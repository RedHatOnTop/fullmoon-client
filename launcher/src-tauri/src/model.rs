/* The wire types. These mirror `launcher/src/core/bindings.ts` field for
   field — that file is the contract, and the UI is written against it. */
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Account {
    pub uuid: String,
    pub username: String,
    pub skin_hue: u16,
    pub skin_url: Option<String>,
    pub source: String,
    pub capes: Vec<String>,
    /// Only a Microsoft account carries a session; an offline profile has none
    /// and the game is told so rather than handed a fake-looking token.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub access_token: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub xuid: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct VersionSummary {
    pub id: String,
    #[serde(rename = "type")]
    pub kind: String,
    pub release_time: String,
    pub is_target: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum InstallStage {
    Manifest,
    Libraries,
    Assets,
    Jre,
    Fabric,
    Mods,
    Done,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallProgress {
    pub stage: InstallStage,
    pub pct: f32,
}

/// `game://log`
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct LogEvent {
    pub session_id: String,
    pub level: String,
    pub line: String,
}

/// `game://state`
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct StateEvent {
    pub session_id: String,
    pub state: GameStateValue,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub exit_code: Option<i32>,
}

/// `install://stage` — the same shape the UI's bindings declare.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstallStageEvent {
    pub instance_id: String,
    pub stage: InstallStage,
    pub pct: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Instance {
    pub id: String,
    pub name: String,
    pub version_id: String,
    pub loader: String,
    pub installed: bool,
    pub installing: Option<InstallProgress>,
    pub memory_mb: u32,
    pub icon_hue: u16,
    pub created_at: String,
    pub last_played_at: Option<String>,
    pub quick_play_server: Option<String>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstanceSpec {
    pub name: String,
    pub version_id: String,
    pub loader: String,
    pub memory_mb: Option<u32>,
    pub icon_hue: Option<u16>,
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct InstancePatch {
    pub name: Option<String>,
    pub memory_mb: Option<u32>,
    /// `Some(None)` clears the server, `None` leaves it alone.
    #[serde(default, deserialize_with = "double_option")]
    pub quick_play_server: Option<Option<String>>,
}

fn double_option<'de, D, T>(de: D) -> Result<Option<Option<T>>, D::Error>
where
    D: serde::Deserializer<'de>,
    T: Deserialize<'de>,
{
    Deserialize::deserialize(de).map(Some)
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Mod {
    pub id: String,
    pub name: String,
    pub version: String,
    pub description: String,
    pub kind: String,
    pub ours: bool,
    pub compatible: bool,
    pub note: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ModCatalog {
    pub mods: Vec<Mod>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct InstalledMod {
    #[serde(flatten)]
    pub base: Mod,
    pub enabled: bool,
    pub favorite: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum GameStateValue {
    Idle,
    Starting,
    Running,
    Crashed,
    Closed,
}

impl GameStateValue {
    /// A process exists — a second launch would fight the first for the
    /// instance directory.
    pub fn is_live(self) -> bool {
        matches!(self, Self::Starting | Self::Running)
    }
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GameState {
    pub state: GameStateValue,
    pub session_id: Option<String>,
    pub instance_id: Option<String>,
    pub server: Option<String>,
    pub started_at: Option<i64>,
    pub exit_code: Option<i32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Cosmetic {
    pub id: String,
    pub slot: String,
    pub name: String,
    pub rarity: String,
    pub hue: u16,
    pub desc: String,
    pub cape_url: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HudModule {
    pub id: String,
    pub enabled: bool,
    pub x: f32,
    pub y: f32,
    pub scale: f32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct HudConfig {
    pub modules: Vec<HudModule>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Settings {
    pub java_path: Option<String>,
    pub java_args: String,
    pub memory_mb: u32,
    pub concurrency: u32,
    pub theme: String,
    pub accent: String,
    pub language: String,
    pub telemetry: bool,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            java_path: None,
            java_args: "-XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200".into(),
            memory_mb: 4096,
            concurrency: 8,
            theme: "dark".into(),
            accent: env!("PINION_ACCENT").into(),
            language: "ko".into(),
            telemetry: false,
        }
    }
}

#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SettingsPatch {
    pub java_path: Option<Option<String>>,
    pub java_args: Option<String>,
    pub memory_mb: Option<u32>,
    pub concurrency: Option<u32>,
    pub theme: Option<String>,
    pub accent: Option<String>,
    pub language: Option<String>,
    pub telemetry: Option<bool>,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct JavaRuntime {
    pub path: String,
    pub version: String,
    pub vendor: String,
    pub arch: String,
    pub recommended: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct NewsItem {
    pub id: String,
    pub tag: String,
    pub title: String,
    pub summary: String,
    pub date: String,
    pub hue: u16,
    pub featured: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ServerEntry {
    pub id: String,
    pub name: String,
    pub address: String,
    pub motd: String,
    pub players: u32,
    pub max_players: u32,
    pub ping_ms: u32,
    pub hue: u16,
}
