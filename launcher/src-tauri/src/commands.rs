/* The IPC surface. One command per entry in `bindings.ts`; the ones with no
   core behind them yet return Error::Unimplemented so the UI shows a real
   failure instead of a convincing fake. */
use std::collections::HashMap;

use serde::{Deserialize, Serialize};
use tauri::State;

use crate::{
    catalog,
    error::{Error, Result},
    java,
    meta,
    model::*,
    paths, store,
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

#[tauri::command]
pub async fn instance_install(_state: State<'_, AppState>, _id: String) -> Result<String> {
    Err(Error::Unimplemented("instance_install (download engine lands next)"))
}

// ── mods ──────────────────────────────────────────────────────

#[derive(Debug, Default, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstanceModState {
    #[serde(default)]
    pub disabled: Vec<String>,
    #[serde(default)]
    pub favorites: Vec<String>,
}

async fn mod_state(id: &str) -> InstanceModState {
    store::read_or(&paths::instance_state_file(id), InstanceModState::default).await
}

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
    let st = mod_state(&instance_id).await;
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

#[tauri::command]
pub async fn mod_toggle(instance_id: String, mod_id: String, enabled: bool) -> Result<()> {
    let mut st = mod_state(&instance_id).await;
    st.disabled.retain(|x| *x != mod_id);
    if !enabled {
        st.disabled.push(mod_id);
    }
    store::write(&paths::instance_state_file(&instance_id), &st).await
}

#[tauri::command]
pub async fn mod_favorite(instance_id: String, mod_id: String, favorite: bool) -> Result<()> {
    let mut st = mod_state(&instance_id).await;
    st.favorites.retain(|x| *x != mod_id);
    if favorite {
        st.favorites.push(mod_id);
    }
    store::write(&paths::instance_state_file(&instance_id), &st).await
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
    let mut active = state.active_account.lock().await;
    if active.as_deref() == Some(uuid.as_str()) {
        *active = accounts.first().map(|a| a.uuid.clone());
    }
    Ok(())
}

#[tauri::command]
pub async fn auth_begin_device_code() -> Result<serde_json::Value> {
    Err(Error::Unimplemented("auth_begin_device_code (Microsoft OAuth lands with launch)"))
}

#[tauri::command]
pub async fn auth_poll(_session: String) -> Result<serde_json::Value> {
    Err(Error::Unimplemented("auth_poll"))
}

#[tauri::command]
pub async fn auth_login_authcode() -> Result<Account> {
    Err(Error::Unimplemented("auth_login_authcode"))
}

#[tauri::command]
pub async fn auth_import_official() -> Result<Vec<Account>> {
    Err(Error::Unimplemented("auth_import_official"))
}

#[tauri::command]
pub async fn auth_refresh(_uuid: String) -> Result<Account> {
    Err(Error::Unimplemented("auth_refresh"))
}

// ── launch ────────────────────────────────────────────────────

#[tauri::command]
pub async fn launch(_instance_id: String, _opts: Option<serde_json::Value>) -> Result<String> {
    Err(Error::Unimplemented("launch (needs the install engine and auth first)"))
}

#[tauri::command]
pub async fn launch_quickplay(_instance_id: String, _server: String) -> Result<String> {
    Err(Error::Unimplemented("launch_quickplay"))
}

#[tauri::command]
pub async fn game_kill(_session_id: String) -> Result<()> {
    Err(Error::Unimplemented("game_kill"))
}

#[tauri::command]
pub async fn game_status(state: State<'_, AppState>) -> Result<GameState> {
    Ok(state.game.lock().await.clone())
}

fn now_iso() -> String {
    time::OffsetDateTime::now_utc()
        .format(&time::format_description::well_known::Rfc3339)
        .unwrap_or_else(|_| "1970-01-01T00:00:00Z".into())
}
