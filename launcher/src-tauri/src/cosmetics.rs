/* The launcher half of the cosmetics contract.
 *
 * The loadout lives once, per account, in the launcher's own store; a copy goes
 * inside the instance because that is the only directory a mod can find without
 * being told where the launcher keeps its files. The copy is rewritten on every
 * equip and again at launch.
 *
 * Nothing reads it yet: the client we bundle draws no capes, wings or trails, so
 * equipping changes the launcher's own preview and nothing in the world. */
use std::collections::HashMap;

use crate::{error::Result, paths, store};

pub type Loadout = HashMap<String, Option<String>>;
pub type LoadoutMap = HashMap<String, Loadout>;

pub const SLOTS: [&str; 3] = ["cape", "wings", "trail"];

pub fn empty() -> Loadout {
    SLOTS.into_iter().map(|s| (s.to_owned(), None)).collect()
}

pub async fn loadout(uuid: &str) -> Loadout {
    let all: LoadoutMap = store::read_or(&paths::cosmetics_file(), LoadoutMap::new).await;
    all.get(uuid).cloned().unwrap_or_else(empty)
}

/// Put `uuid`'s loadout where the mod running in `instance_id` will find it.
pub async fn materialize(instance_id: &str, uuid: &str) -> Result<()> {
    let loadout = loadout(uuid).await;
    store::write(&paths::instance_cosmetics_file(instance_id), &loadout).await
}
