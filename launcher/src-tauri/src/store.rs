/* JSON-file persistence. Writes go through a temp file + rename so a crash
   mid-save can't leave a half-written settings.json that bricks the launcher
   on next start. */
use std::path::Path;

use serde::{de::DeserializeOwned, Serialize};

use crate::{error::Result, paths};

pub async fn read_or<T: DeserializeOwned>(file: &Path, fallback: impl FnOnce() -> T) -> T {
    match tokio::fs::read(file).await {
        Ok(bytes) => serde_json::from_slice(&bytes).unwrap_or_else(|_| fallback()),
        Err(_) => fallback(),
    }
}

pub async fn write<T: Serialize>(file: &Path, value: &T) -> Result<()> {
    if let Some(parent) = file.parent() {
        paths::ensure_dir(parent).await?;
    }
    let bytes = serde_json::to_vec_pretty(value)?;
    let tmp = file.with_extension("json.tmp");
    tokio::fs::write(&tmp, &bytes).await?;
    // Windows rename fails if the destination exists
    let _ = tokio::fs::remove_file(file).await;
    tokio::fs::rename(&tmp, file).await?;
    Ok(())
}
