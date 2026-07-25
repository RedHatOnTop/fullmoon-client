use tokio::sync::Mutex;

use crate::{
    model::{Account, GameState, GameStateValue, Instance, Settings},
    paths, store,
};

pub struct AppState {
    pub http: reqwest::Client,
    pub settings: Mutex<Settings>,
    pub instances: Mutex<Vec<Instance>>,
    pub active_account: Mutex<Option<String>>,
    pub game: Mutex<GameState>,
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
            game: Mutex::new(GameState {
                state: GameStateValue::Idle,
                session_id: None,
                instance_id: None,
                server: None,
                started_at: None,
                exit_code: None,
            }),
        }
    }
}
