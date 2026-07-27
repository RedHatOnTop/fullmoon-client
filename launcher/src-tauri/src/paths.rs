/* Every path the launcher touches is derived here from one root, so an
   instance can never write outside its own directory (MultiMC-style
   isolation, PLAN §1) and a rebrand moves the whole tree at once. */
use std::path::{Path, PathBuf};

pub const BRAND_NAME: &str = env!("PINION_NAME");
pub const BRAND_SLUG: &str = env!("PINION_SLUG");

/// `%APPDATA%/Pinion` on Windows, `~/.local/share/pinion` elsewhere.
pub fn root() -> PathBuf {
    let base = if cfg!(windows) {
        dirs::data_dir()
    } else {
        dirs::data_local_dir()
    };
    let dir = base
        .unwrap_or_else(|| PathBuf::from("."))
        .join(if cfg!(windows) { BRAND_NAME } else { BRAND_SLUG });
    dir
}

pub fn settings_file() -> PathBuf {
    root().join("settings.json")
}

pub fn instances_file() -> PathBuf {
    root().join("instances.json")
}

pub fn accounts_file() -> PathBuf {
    root().join("accounts.json")
}

/// Session tokens, kept apart from the profiles so nothing the UI reads can
/// ever carry one.
pub fn sessions_file() -> PathBuf {
    root().join("sessions.json")
}

pub fn servers_file() -> PathBuf {
    root().join("servers.json")
}

pub fn cosmetics_file() -> PathBuf {
    root().join("cosmetics.json")
}

/// Shared across instances: version metadata, libraries, assets, JREs.
pub fn shared() -> PathBuf {
    root().join("shared")
}

pub fn versions_dir() -> PathBuf {
    shared().join("versions")
}

pub fn libraries_dir() -> PathBuf {
    shared().join("libraries")
}

pub fn assets_dir() -> PathBuf {
    shared().join("assets")
}

pub fn instances_dir() -> PathBuf {
    root().join("instances")
}

/// The game directory for one instance — its own saves, configs and mods.
pub fn instance_dir(id: &str) -> PathBuf {
    instances_dir().join(sanitize(id))
}

pub fn instance_minecraft_dir(id: &str) -> PathBuf {
    instance_dir(id).join("minecraft")
}

pub fn instance_mods_dir(id: &str) -> PathBuf {
    instance_minecraft_dir(id).join("mods")
}

/// Written by the launcher, read by the Pinion mod at runtime.
pub fn instance_hud_file(id: &str) -> PathBuf {
    instance_minecraft_dir(id).join("pinion").join("hud.json")
}

pub fn instance_state_file(id: &str) -> PathBuf {
    instance_dir(id).join("instance.json")
}

/// The launcher's own shipped files — the Pinion mod jar lives here. An
/// installed build has them beside the binary; `tauri dev` runs from the crate,
/// where they are still only in the source tree.
pub fn resources(app: &tauri::AppHandle) -> PathBuf {
    use tauri::Manager;
    if let Ok(dir) = app.path().resource_dir() {
        let nested = dir.join("resources");
        if nested.is_dir() {
            return nested;
        }
        if dir.join("mods").is_dir() {
            return dir;
        }
    }
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).join("resources")
}

/// Ids come from the UI, so they never get to walk out of the tree.
fn sanitize(id: &str) -> String {
    id.chars()
        .map(|c| match c {
            'a'..='z' | 'A'..='Z' | '0'..='9' | '-' | '_' => c,
            _ => '_',
        })
        .collect()
}

pub async fn ensure_dir(p: &Path) -> std::io::Result<()> {
    tokio::fs::create_dir_all(p).await
}
