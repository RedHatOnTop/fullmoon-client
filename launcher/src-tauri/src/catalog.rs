/* The bundled catalogue: the mods we ship, the cosmetics we drew, the news
   we wrote and the starter server list. Compiled in rather than fetched —
   these are product data, not user data, and the launcher must render on a
   plane with no network. */
use std::sync::OnceLock;

use serde::Deserialize;

use crate::model::{Cosmetic, HudConfig, Mod, NewsItem, ServerEntry};

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Catalog {
    pub mods: Vec<Mod>,
    pub cosmetics: Vec<Cosmetic>,
    pub news: Vec<NewsItem>,
    pub servers: Vec<ServerEntry>,
    pub default_hud: HudConfig,
}

pub fn get() -> &'static Catalog {
    static CATALOG: OnceLock<Catalog> = OnceLock::new();
    CATALOG.get_or_init(|| {
        serde_json::from_str(include_str!("../resources/catalog.json"))
            .expect("bundled catalog.json is malformed")
    })
}
