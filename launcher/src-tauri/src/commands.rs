/* The IPC surface. One command per entry in `bindings.ts`, and every one of
   them backed by real work — a command that cannot do its job says why, in a
   sentence, rather than answering with a convincing fake. */
use std::collections::HashMap;

use serde::Deserialize;
use tauri::{AppHandle, State};

use crate::{
    catalog,
    error::{Error, Result},
    install, java,
    meta,
    model::*,
    mods, paths, store,
    state::AppState,
};

// ── settings ──────────────────────────────────────────────────

#[tauri::command]
pub async fn settings_get(state: State<'_, AppState>) -> Result<Settings> {
    // first run has no java pinned; adopt the best runtime on the box and keep it
    let missing = state.settings.lock().await.java_path.is_none();
    if missing {
        if let Some(best) = java::detect()
            .await
            .ok()
            .and_then(|rs| rs.into_iter().find(|r| r.recommended).or(None))
        {
            let mut guard = state.settings.lock().await;
            guard.java_path = Some(best.path);
            let snapshot = guard.clone();
            drop(guard);
            store::write(&paths::settings_file(), &snapshot).await?;
        }
    }
    Ok(state.settings.lock().await.clone())
}

#[tauri::command]
pub async fn settings_set(state: State<'_, AppState>, patch: SettingsPatch) -> Result<Settings> {
    let mut guard = state.settings.lock().await;
    if let Some(v) = patch.java_path {
        guard.java_path = v;
    }
    if let Some(v) = patch.java_args {
        guard.java_args = v;
    }
    if let Some(v) = patch.memory_mb {
        guard.memory_mb = v.clamp(1024, 32768);
    }
    if let Some(v) = patch.concurrency {
        guard.concurrency = v.clamp(1, 32);
    }
    if let Some(v) = patch.theme {
        guard.theme = v;
    }
    if let Some(v) = patch.accent {
        guard.accent = v;
    }
    if let Some(v) = patch.language {
        guard.language = v;
    }
    if let Some(v) = patch.telemetry {
        guard.telemetry = v;
    }
    store::write(&paths::settings_file(), &*guard).await?;
    Ok(guard.clone())
}

#[tauri::command]
pub async fn java_detect() -> Result<Vec<JavaRuntime>> {
    java::detect().await
}

// ── versions / instances ──────────────────────────────────────

#[tauri::command]
pub async fn versions_manifest(state: State<'_, AppState>) -> Result<Vec<VersionSummary>> {
    let m = meta::manifest(&state.http).await?;
    Ok(meta::summaries(&m))
}

#[tauri::command]
pub async fn instances_list(state: State<'_, AppState>) -> Result<Vec<Instance>> {
    Ok(state.instances.lock().await.clone())
}

#[tauri::command]
pub async fn instance_create(state: State<'_, AppState>, spec: InstanceSpec) -> Result<Instance> {
    if spec.name.trim().is_empty() {
        return Err(Error::Invalid("instance name is empty".into()));
    }
    let manifest = meta::manifest(&state.http).await?;
    if meta::find(&manifest, &spec.version_id).is_none() {
        return Err(Error::NotFound(format!("version {}", spec.version_id)));
    }

    let default_mem = state.settings.lock().await.memory_mb;
    let inst = Instance {
        id: format!("inst-{}", uuid::Uuid::new_v4().simple()),
        name: spec.name.trim().to_owned(),
        version_id: spec.version_id,
        loader: spec.loader,
        installed: false,
        installing: None,
        memory_mb: spec.memory_mb.unwrap_or(default_mem),
        icon_hue: spec.icon_hue.unwrap_or(214),
        created_at: now_iso(),
        last_played_at: None,
        quick_play_server: None,
    };

    paths::ensure_dir(&paths::instance_minecraft_dir(&inst.id)).await?;
    paths::ensure_dir(&paths::instance_mods_dir(&inst.id)).await?;

    let mut list = state.instances.lock().await;
    list.push(inst.clone());
    store::write(&paths::instances_file(), &*list).await?;
    Ok(inst)
}

#[tauri::command]
pub async fn instance_update(
    state: State<'_, AppState>,
    id: String,
    patch: InstancePatch,
) -> Result<Instance> {
    let mut list = state.instances.lock().await;
    let inst = list
        .iter_mut()
        .find(|i| i.id == id)
        .ok_or_else(|| Error::NotFound(format!("instance {id}")))?;
    if let Some(name) = patch.name {
        inst.name = name;
    }
    if let Some(mem) = patch.memory_mb {
        inst.memory_mb = mem.clamp(1024, 32768);
    }
    if let Some(server) = patch.quick_play_server {
        inst.quick_play_server = server;
    }
    let updated = inst.clone();
    store::write(&paths::instances_file(), &*list).await?;
    Ok(updated)
}

#[tauri::command]
pub async fn instance_delete(state: State<'_, AppState>, id: String) -> Result<()> {
    let mut list = state.instances.lock().await;
    let before = list.len();
    list.retain(|i| i.id != id);
    if list.len() == before {
        return Err(Error::NotFound(format!("instance {id}")));
    }
    store::write(&paths::instances_file(), &*list).await?;
    drop(list);
    // the game directory holds saves, so it goes only after the index is clean
    let dir = paths::instance_dir(&id);
    if tokio::fs::try_exists(&dir).await.unwrap_or(false) {
        tokio::fs::remove_dir_all(&dir).await?;
    }
    Ok(())
}

/// Runs the install to completion before answering. The UI follows along on
/// `install://stage`; the returned id is what a later cancel would name.
#[tauri::command]
pub async fn instance_install(
    app: AppHandle,
    state: State<'_, AppState>,
    id: String,
) -> Result<String> {
    let (game, loader) = {
        let instances = state.instances.lock().await;
        let inst = instances
            .iter()
            .find(|i| i.id == id)
            .ok_or_else(|| Error::NotFound(format!("instance {id}")))?;
        (inst.version_id.clone(), inst.loader.clone())
    };
    let concurrency = state.settings.lock().await.concurrency as usize;

    set_installing(
        &state,
        &id,
        Some(InstallProgress { stage: InstallStage::Manifest, pct: 0.0 }),
    )
    .await?;

    let outcome = install::run(&app, &state.http, &id, &game, &loader, concurrency).await;

    match outcome {
        Ok(_) => {
            let mut instances = state.instances.lock().await;
            if let Some(inst) = instances.iter_mut().find(|i| i.id == id) {
                inst.installed = true;
                inst.installing = None;
            }
            let snapshot = instances.clone();
            drop(instances);
            store::write(&paths::instances_file(), &snapshot).await?;
            Ok(id)
        }
        Err(e) => {
            // a failed install leaves the instance exactly as un-installed as it was
            set_installing(&state, &id, None).await?;
            Err(e)
        }
    }
}

async fn set_installing(
    state: &State<'_, AppState>,
    id: &str,
    progress: Option<InstallProgress>,
) -> Result<()> {
    let mut instances = state.instances.lock().await;
    if let Some(inst) = instances.iter_mut().find(|i| i.id == id) {
        inst.installing = progress;
    }
    let snapshot = instances.clone();
    drop(instances);
    store::write(&paths::instances_file(), &snapshot).await
}

// ── mods ──────────────────────────────────────────────────────

#[tauri::command]
pub async fn mods_available() -> Result<ModCatalog> {
    Ok(ModCatalog {
        mods: catalog::get().mods.clone(),
    })
}

#[tauri::command]
pub async fn mods_list(state: State<'_, AppState>, instance_id: String) -> Result<Vec<InstalledMod>> {
    let known = state.instances.lock().await.iter().any(|i| i.id == instance_id);
    if !known {
        return Err(Error::NotFound(format!("instance {instance_id}")));
    }
    let st = mods::state(&instance_id).await;
    Ok(catalog::get()
        .mods
        .iter()
        .map(|m| InstalledMod {
            enabled: !st.disabled.contains(&m.id),
            favorite: st.favorites.contains(&m.id),
            base: m.clone(),
        })
        .collect())
}

/// Flipping a mod moves the jar, not just a flag — the next launch has to see
/// the change without a reinstall.
#[tauri::command]
pub async fn mod_toggle(
    app: AppHandle,
    state: State<'_, AppState>,
    instance_id: String,
    mod_id: String,
    enabled: bool,
) -> Result<()> {
    let (game, loader) = {
        let instances = state.instances.lock().await;
        let inst = instances
            .iter()
            .find(|i| i.id == instance_id)
            .ok_or_else(|| Error::NotFound(format!("instance {instance_id}")))?;
        (inst.version_id.clone(), inst.loader.clone())
    };
    let concurrency = state.settings.lock().await.concurrency as usize;

    let mut st = mods::state(&instance_id).await;
    st.disabled.retain(|x| *x != mod_id);
    if !enabled {
        st.disabled.push(mod_id);
    }
    mods::save_state(&instance_id, &st).await?;

    mods::apply(
        &state.http,
        &paths::resources(&app),
        &instance_id,
        &game,
        &loader,
        concurrency,
    )
    .await
}

#[tauri::command]
pub async fn mod_favorite(instance_id: String, mod_id: String, favorite: bool) -> Result<()> {
    let mut st = mods::state(&instance_id).await;
    st.favorites.retain(|x| *x != mod_id);
    if favorite {
        st.favorites.push(mod_id);
    }
    mods::save_state(&instance_id, &st).await
}

// ── cosmetics / hud ───────────────────────────────────────────

type LoadoutMap = HashMap<String, HashMap<String, Option<String>>>;

#[tauri::command]
pub async fn cosmetics_catalog() -> Result<Vec<Cosmetic>> {
    Ok(catalog::get().cosmetics.clone())
}

#[tauri::command]
pub async fn cosmetics_equipped(uuid: String) -> Result<HashMap<String, Option<String>>> {
    let all: LoadoutMap = store::read_or(&paths::cosmetics_file(), LoadoutMap::new).await;
    Ok(all.get(&uuid).cloned().unwrap_or_else(empty_loadout))
}

#[tauri::command]
pub async fn cosmetics_equip(uuid: String, slot: String, item_id: Option<String>) -> Result<()> {
    if !["cape", "wings", "trail"].contains(&slot.as_str()) {
        return Err(Error::Invalid(format!("unknown cosmetic slot {slot}")));
    }
    if let Some(id) = &item_id {
        let item = catalog::get().cosmetics.iter().find(|c| c.id == *id);
        match item {
            Some(c) if c.slot == slot => {}
            Some(c) => {
                return Err(Error::Invalid(format!(
                    "{id} is a {} cosmetic, not {slot}",
                    c.slot
                )))
            }
            None => return Err(Error::NotFound(format!("cosmetic {id}"))),
        }
    }
    let mut all: LoadoutMap = store::read_or(&paths::cosmetics_file(), LoadoutMap::new).await;
    all.entry(uuid).or_insert_with(empty_loadout).insert(slot, item_id);
    store::write(&paths::cosmetics_file(), &all).await
}

fn empty_loadout() -> HashMap<String, Option<String>> {
    ["cape", "wings", "trail"]
        .into_iter()
        .map(|s| (s.to_owned(), None))
        .collect()
}

#[tauri::command]
pub async fn hud_get(instance_id: String) -> Result<HudConfig> {
    Ok(store::read_or(&paths::instance_hud_file(&instance_id), || {
        catalog::get().default_hud.clone()
    })
    .await)
}

#[tauri::command]
pub async fn hud_set(instance_id: String, cfg: HudConfig) -> Result<()> {
    // this file is the contract with the in-game mod, not launcher scratch
    store::write(&paths::instance_hud_file(&instance_id), &cfg).await
}

// ── home ──────────────────────────────────────────────────────

#[tauri::command]
pub async fn news_feed() -> Result<Vec<NewsItem>> {
    Ok(catalog::get().news.clone())
}

#[tauri::command]
pub async fn servers_list() -> Result<Vec<ServerEntry>> {
    let file = paths::servers_file();
    if tokio::fs::try_exists(&file).await.unwrap_or(false) {
        Ok(store::read_or(&file, || catalog::get().servers.clone()).await)
    } else {
        Ok(catalog::get().servers.clone())
    }
}

#[tauri::command]
pub async fn servers_save(list: Vec<ServerEntry>) -> Result<()> {
    store::write(&paths::servers_file(), &list).await
}

// ── auth ──────────────────────────────────────────────────────

#[tauri::command]
pub async fn auth_list() -> Result<Vec<Account>> {
    Ok(store::read_or(&paths::accounts_file(), Vec::new).await)
}

/// The UI needs the active uuid synchronously after `auth_list`; it fetches
/// both in one round and caches the result.
#[tauri::command]
pub async fn auth_active(state: State<'_, AppState>) -> Result<Option<String>> {
    Ok(state.active_account.lock().await.clone())
}

#[tauri::command]
pub async fn auth_select(state: State<'_, AppState>, uuid: String) -> Result<()> {
    let accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;
    if !accounts.iter().any(|a| a.uuid == uuid) {
        return Err(Error::NotFound(format!("account {uuid}")));
    }
    *state.active_account.lock().await = Some(uuid);
    Ok(())
}

#[tauri::command]
pub async fn auth_remove(state: State<'_, AppState>, uuid: String) -> Result<()> {
    let mut accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;
    accounts.retain(|a| a.uuid != uuid);
    store::write(&paths::accounts_file(), &accounts).await?;
    // removing an account has to remove its session too, or the next add of the
    // same profile silently inherits a dead token
    crate::auth::forget_session(&uuid).await?;
    let mut active = state.active_account.lock().await;
    if active.as_deref() == Some(uuid.as_str()) {
        *active = accounts.first().map(|a| a.uuid.clone());
    }
    Ok(())
}

/// An offline profile: no session, no entitlement, singleplayer and LAN only.
/// The uuid is derived the way every launcher derives it — from the name —
/// so worlds keep their player data across launchers.
#[tauri::command]
pub async fn auth_add_offline(state: State<'_, AppState>, username: String) -> Result<Account> {
    let name = username.trim();
    if name.is_empty() || name.len() > 16 || !name.chars().all(|c| c.is_ascii_alphanumeric() || c == '_')
    {
        return Err(Error::Invalid(
            "a Minecraft name is 1-16 characters of letters, digits or underscore".into(),
        ));
    }

    let uuid = uuid::Uuid::new_v3(
        &uuid::Uuid::NAMESPACE_OID,
        format!("OfflinePlayer:{name}").as_bytes(),
    );
    let account = Account {
        uuid: uuid.to_string(),
        username: name.to_string(),
        skin_hue: (uuid.as_u128() % 360) as u16,
        skin_url: None,
        source: "offline".into(),
        capes: Vec::new(),
    };

    let mut accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;
    if accounts.iter().any(|a| a.uuid == account.uuid) {
        return Err(Error::Invalid(format!("{name} is already added")));
    }
    accounts.push(account.clone());
    store::write(&paths::accounts_file(), &accounts).await?;

    let mut active = state.active_account.lock().await;
    if active.is_none() {
        *active = Some(account.uuid.clone());
    }
    Ok(account)
}

#[tauri::command]
pub async fn auth_begin_device_code(
    state: State<'_, AppState>,
) -> Result<crate::auth::DeviceCodePrompt> {
    let (prompt, flow) = crate::auth::begin_device_code(&state.http).await?;
    state
        .device_flows
        .lock()
        .await
        .insert(prompt.session.clone(), flow);
    Ok(prompt)
}

/// One poll of the device flow. `pending` is the UI's cue to ask again; every
/// other answer ends the flow, so the session is dropped with it.
#[tauri::command]
pub async fn auth_poll(state: State<'_, AppState>, session: String) -> Result<serde_json::Value> {
    let mut flow = state
        .device_flows
        .lock()
        .await
        .get(&session)
        .cloned()
        .ok_or_else(|| Error::NotFound(format!("sign-in session {session}")))?;

    let outcome = crate::auth::poll_device(&state.http, &mut flow).await;
    let mut flows = state.device_flows.lock().await;
    match outcome {
        // keep the pacing Microsoft just told us about
        Ok(None) => {
            flows.insert(session.clone(), flow);
        }
        _ => {
            flows.remove(&session);
        }
    }
    drop(flows);

    match outcome {
        Ok(None) => Ok(serde_json::json!({ "state": "pending" })),
        Ok(Some(account)) => {
            select_if_first(&state, &account.uuid).await;
            Ok(serde_json::json!({ "state": "done", "account": account }))
        }
        Err(e) => Ok(serde_json::json!({ "state": "error", "message": e.to_string() })),
    }
}

#[tauri::command]
pub async fn auth_login_authcode(state: State<'_, AppState>) -> Result<Account> {
    let account = crate::auth::login_authcode(&state.http).await?;
    select_if_first(&state, &account.uuid).await;
    Ok(account)
}

#[tauri::command]
pub async fn auth_import_official(state: State<'_, AppState>) -> Result<Vec<Account>> {
    let added = crate::auth::import_official().await?;
    if let Some(first) = added.first() {
        select_if_first(&state, &first.uuid).await;
    }
    Ok(added)
}

#[tauri::command]
pub async fn auth_refresh(state: State<'_, AppState>, uuid: String) -> Result<Account> {
    crate::auth::refresh(&state.http, &uuid).await
}

async fn select_if_first(state: &State<'_, AppState>, uuid: &str) {
    let mut active = state.active_account.lock().await;
    if active.is_none() {
        *active = Some(uuid.to_string());
    }
}

// ── launch ────────────────────────────────────────────────────

#[derive(Debug, Default, Deserialize)]
pub struct LaunchOpts {
    pub server: Option<String>,
}

async fn start_game(
    app: AppHandle,
    state: State<'_, AppState>,
    instance_id: String,
    server: Option<String>,
) -> Result<String> {
    if state.game.lock().await.state.is_live() {
        return Err(Error::Invalid("a game is already running".into()));
    }

    let inst = {
        let instances = state.instances.lock().await;
        instances
            .iter()
            .find(|i| i.id == instance_id)
            .cloned()
            .ok_or_else(|| Error::NotFound(format!("instance {instance_id}")))?
    };
    if !inst.installed {
        return Err(Error::Invalid(format!("{} is not installed yet", inst.name)));
    }

    let account = {
        let accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;
        let active = state.active_account.lock().await.clone();
        active
            .and_then(|uuid| accounts.iter().find(|a| a.uuid == uuid).cloned())
            .or_else(|| accounts.first().cloned())
            .ok_or_else(|| Error::Invalid("no account is selected".into()))?
    };

    let settings = state.settings.lock().await.clone();
    let version: crate::version::VersionJson = {
        let file = install::resolved_file(&instance_id);
        let bytes = tokio::fs::read(&file).await.map_err(|_| {
            Error::Invalid(format!("{} has no resolved version — reinstall it", inst.name))
        })?;
        serde_json::from_slice(&bytes)?
    };

    // a stale token is renewed here, where the failure can still be explained,
    // rather than inside a game that only says "failed to log in"
    let session = crate::auth::session_for_launch(&state.http, &account).await?;

    let server = server.or_else(|| inst.quick_play_server.clone());
    let plan = crate::launch::plan(
        &version,
        &inst,
        &settings,
        &account,
        session.as_ref(),
        server.as_deref(),
    )?;

    let session_id = format!("run-{}", uuid::Uuid::new_v4().simple());

    {
        let mut game = state.game.lock().await;
        *game = GameState {
            state: GameStateValue::Starting,
            session_id: Some(session_id.clone()),
            instance_id: Some(instance_id.clone()),
            server: server.clone(),
            started_at: Some(now_millis()),
            exit_code: None,
        };
    }

    let kill = crate::launch::spawn(
        &app,
        std::sync::Arc::clone(&state.game),
        plan,
        session_id.clone(),
    )?;
    *state.kill.lock().await = Some(kill);

    // last played is only true once the process actually exists
    {
        let mut instances = state.instances.lock().await;
        if let Some(i) = instances.iter_mut().find(|i| i.id == instance_id) {
            i.last_played_at = Some(now_iso());
        }
        let snapshot = instances.clone();
        drop(instances);
        store::write(&paths::instances_file(), &snapshot).await?;
    }

    Ok(session_id)
}

#[tauri::command]
pub async fn launch(
    app: AppHandle,
    state: State<'_, AppState>,
    instance_id: String,
    opts: Option<LaunchOpts>,
) -> Result<String> {
    let server = opts.and_then(|o| o.server);
    start_game(app, state, instance_id, server).await
}

#[tauri::command]
pub async fn launch_quickplay(
    app: AppHandle,
    state: State<'_, AppState>,
    instance_id: String,
    server: String,
) -> Result<String> {
    start_game(app, state, instance_id, Some(server)).await
}

#[tauri::command]
pub async fn game_kill(state: State<'_, AppState>, session_id: String) -> Result<()> {
    let current = state.game.lock().await.session_id.clone();
    if current.as_deref() != Some(session_id.as_str()) {
        return Err(Error::NotFound(format!("session {session_id}")));
    }
    match state.kill.lock().await.take() {
        Some(tx) => {
            let _ = tx.send(());
            Ok(())
        }
        None => Err(Error::NotFound(format!("process for session {session_id}"))),
    }
}

#[tauri::command]
pub async fn game_status(state: State<'_, AppState>) -> Result<GameState> {
    Ok(state.game.lock().await.clone())
}

fn now_millis() -> i64 {
    (time::OffsetDateTime::now_utc().unix_timestamp_nanos() / 1_000_000) as i64
}

fn now_iso() -> String {
    time::OffsetDateTime::now_utc()
        .format(&time::format_description::well_known::Rfc3339)
        .unwrap_or_else(|_| "1970-01-01T00:00:00Z".into())
}
