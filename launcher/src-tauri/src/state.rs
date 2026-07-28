use std::{
    collections::{HashMap, VecDeque},
    sync::Arc,
};

use tokio::sync::{oneshot, Mutex};

use crate::{
    auth::DeviceFlow,
    model::{Account, GameState, GameStateValue, Instance, LogEvent, Settings},
    paths, store,
};

pub struct AppState {
    pub http: reqwest::Client,
    pub settings: Mutex<Settings>,
    pub instances: Mutex<Vec<Instance>>,
    pub active_account: Mutex<Option<String>>,
    /// Device-code sign-ins in flight. In memory only — a code that outlives
    /// the launcher is a code the user has to request again anyway.
    pub device_flows: Mutex<HashMap<String, DeviceFlow>>,
    /// Shared with the process watcher, which updates it from its own task.
    pub game: Arc<Mutex<GameState>>,
    /// What the game has said so far. Events alone are not enough: a console
    /// opened after launch would be blank until the game happened to speak,
    /// and a running client can stay quiet for minutes.
    pub log: Arc<Mutex<VecDeque<LogEvent>>>,
    /// Dropping this sender is harmless; sending on it kills the game.
    pub kill: Mutex<Option<oneshot::Sender<()>>>,
}

impl AppState {
    pub async fn load() -> Self {
        let settings: Settings = store::read_or(&paths::settings_file(), Settings::default).await;
        let mut instances: Vec<Instance> =
            store::read_or(&paths::instances_file(), Vec::new).await;
        // an install that was in flight when the launcher died is not in flight now
        for inst in &mut instances {
            inst.installing = None;
        }

        let accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;

        let http = reqwest::Client::builder()
            .user_agent(concat!(env!("PINION_NAME"), "/", env!("CARGO_PKG_VERSION")))
            .connect_timeout(std::time::Duration::from_secs(15))
            .build()
            .expect("http client");

        Self {
            http,
            settings: Mutex::new(settings),
            instances: Mutex::new(instances),
            active_account: Mutex::new(accounts.first().map(|a| a.uuid.clone())),
            device_flows: Mutex::new(HashMap::new()),
            game: Arc::new(Mutex::new(GameState {
                state: GameStateValue::Idle,
                session_id: None,
                instance_id: None,
                server: None,
                started_at: None,
                exit_code: None,
            })),
            log: Arc::new(Mutex::new(VecDeque::new())),
            kill: Mutex::new(None),
        }
    }
}
