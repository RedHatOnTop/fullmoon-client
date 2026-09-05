/* The launcher reads the player's wallet; it never writes the ledger.
   economy-api is read-only by construction (2026-07-12) — the only writers
   are the Discord bot and the MC plugin — and this module is a client of its
   reads, keyed by the active player's Minecraft identity.

   Credentials live in a config file next to settings.json, never in the
   binary — a read key shipped in a public executable is a key published to
   every player:

     economy.json: { "baseUrl": "https://api.fullmoon.ink/economy",
                     "token": "<read-only client key>" }

   A missing config is a normal state, not an error worth a boot toast: the UI
   already treats a null wallet as "no economy link yet". The claim (write)
   path is designed, not built — see docs/economy-claim-design.md. */
use serde::Deserialize;

use crate::{
    error::{Error, Result},
    model::{Account, WalletInfo, WalletTx},
    paths, store,
    state::AppState,
};

#[derive(Debug, Clone, Deserialize)]
struct EconomyConfig {
    #[serde(rename = "baseUrl")]
    base_url: String,
    token: String,
}

#[derive(Debug, Deserialize)]
struct ByMcResponse {
    wallet: WalletInfo,
    #[serde(default)]
    transactions: Vec<WalletTx>,
}

async fn config() -> Result<EconomyConfig> {
    let file = paths::root().join("economy.json");
    let cfg: Option<EconomyConfig> = store::read_or(&file, || None).await;
    cfg.ok_or_else(|| {
        Error::Invalid(
            "economy.json is missing — the wallet read needs { baseUrl, token }".into(),
        )
    })
}

/// The wallet belongs to the active player, so the launcher resolves its own
/// active account rather than asking the UI who is logged in.
async fn active_username(state: &AppState) -> Result<String> {
    let uuid = state
        .active_account
        .lock()
        .await
        .clone()
        .ok_or_else(|| Error::Invalid("no active account".into()))?;
    let accounts: Vec<Account> = store::read_or(&paths::accounts_file(), Vec::new).await;
    accounts
        .iter()
        .find(|a| a.uuid == uuid)
        .map(|a| a.username.clone())
        .ok_or_else(|| Error::Invalid("active account no longer exists".into()))
}

async fn fetch_by_mc(state: &AppState) -> Result<ByMcResponse> {
    let cfg = config().await?;
    let username = active_username(state).await?;
    if !username
        .chars()
        .all(|c| c.is_ascii_alphanumeric() || c == '_')
    {
        return Err(Error::Invalid("malformed minecraft username".into()));
    }
    let url = format!(
        "{}/v1/accounts/by-mc/{}",
        cfg.base_url.trim_end_matches('/'),
        username
    );
    let res = state
        .http
        .get(url)
        .bearer_auth(&cfg.token)
        .timeout(std::time::Duration::from_secs(5))
        .send()
        .await?;
    // an unlinked username is a normal state, not a server error
    if res.status().as_u16() == 404 {
        return Err(Error::NotFound("no linked economy account".into()));
    }
    if !res.status().is_success() {
        return Err(Error::Invalid(format!(
            "economy-api returned HTTP {}",
            res.status()
        )));
    }
    let text = res.text().await?;
    Ok(serde_json::from_str(&text)?)
}

pub async fn wallet(state: &AppState) -> Result<WalletInfo> {
    Ok(fetch_by_mc(state).await?.wallet)
}

pub async fn transactions(state: &AppState) -> Result<Vec<WalletTx>> {
    Ok(fetch_by_mc(state).await?.transactions)
}
