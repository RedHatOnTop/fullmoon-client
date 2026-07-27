/* The bundled catalogue: the mods we ship, the cosmetics we drew, the news
   we wrote and the starter server list. Compiled in rather than fetched —
   these are product data, not user data, and the launcher must render on a
   plane with no network. */
use std::{collections::BTreeMap, sync::OnceLock};

use serde::Deserialize;

use crate::{
    model::{Cosmetic, HudConfig, Mod, NewsItem, ServerEntry},
    mods::ModSource,
};

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Catalog {
    pub mods: Vec<Mod>,
    /// Where each listed mod actually comes from. Not part of the wire
    /// contract — the UI has no business knowing a maven coordinate.
    pub mod_sources: BTreeMap<String, ModSource>,
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
