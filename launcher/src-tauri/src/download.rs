/* The download engine. Three rules it never breaks:

   - a file that is already on disk with the right SHA1 is never fetched again,
     so a second install is seconds, not minutes;
   - bytes land in a temp file and are only renamed into place after the hash
     checks out, so an interrupted run can never leave a corrupt jar that
     looks installed;
   - progress is reported from the bytes actually written, not from a count of
     finished files, because 3000 tiny assets and one 38 MB client jar are not
     the same kind of work. */
use std::{
    path::{Path, PathBuf},
    sync::{
        atomic::{AtomicU64, Ordering},
        Arc,
    },
};

use futures::{stream, StreamExt};
use sha1::{Digest, Sha1};
use tokio::io::AsyncWriteExt;

use crate::error::{Error, Result};

#[derive(Debug, Clone)]
pub struct Item {
    pub url: String,
    pub path: PathBuf,
    pub sha1: Option<String>,
    pub size: u64,
}

/// Live totals for one install. Cloned into every task; the UI polls it.
#[derive(Debug, Default)]
pub struct Progress {
    pub done_bytes: AtomicU64,
    pub total_bytes: AtomicU64,
    pub done_files: AtomicU64,
    pub total_files: AtomicU64,
}

impl Progress {
    pub fn fraction(&self) -> f64 {
        let total = self.total_bytes.load(Ordering::Relaxed);
        if total == 0 {
            return 0.0;
        }
        (self.done_bytes.load(Ordering::Relaxed) as f64 / total as f64).clamp(0.0, 1.0)
    }
}

pub async fn sha1_of(path: &Path) -> Option<String> {
    let bytes = tokio::fs::read(path).await.ok()?;
    let mut h = Sha1::new();
    h.update(&bytes);
    Some(hex::encode(h.finalize()))
}

/// True when the file is present and matches. Without a hash we can only
/// check that something is there, which is what the metadata gives us.
pub async fn already_good(item: &Item) -> bool {
    let Ok(meta) = tokio::fs::metadata(&item.path).await else {
        return false;
    };
    if !meta.is_file() {
        return false;
    }
    match &item.sha1 {
        Some(want) => sha1_of(&item.path).await.as_deref() == Some(want.as_str()),
        None => item.size == 0 || meta.len() == item.size,
    }
}

async fn fetch_one(client: &reqwest::Client, item: &Item, progress: &Progress) -> Result<()> {
    if let Some(parent) = item.path.parent() {
        tokio::fs::create_dir_all(parent).await?;
    }

    let tmp = item.path.with_extension("part");
    let res = client.get(&item.url).send().await?;
    if !res.status().is_success() {
        return Err(Error::Invalid(format!("{} → HTTP {}", item.url, res.status())));
    }

    let mut file = tokio::fs::File::create(&tmp).await?;
    let mut hasher = Sha1::new();
    let mut stream = res.bytes_stream();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk?;
        hasher.update(&chunk);
        file.write_all(&chunk).await?;
        progress.done_bytes.fetch_add(chunk.len() as u64, Ordering::Relaxed);
    }
    file.flush().await?;
    drop(file);

    if let Some(want) = &item.sha1 {
        let got = hex::encode(hasher.finalize());
        if &got != want {
            let _ = tokio::fs::remove_file(&tmp).await;
            return Err(Error::Invalid(format!(
                "checksum mismatch for {}: expected {want}, got {got}",
                item.path.display()
            )));
        }
    }

    let _ = tokio::fs::remove_file(&item.path).await;
    tokio::fs::rename(&tmp, &item.path).await?;
    Ok(())
}

/// Fetch everything missing, `concurrency` at a time. Returns the number of
/// files actually transferred; the rest were already on disk and verified.
pub async fn fetch_all(
    client: &reqwest::Client,
    items: Vec<Item>,
    concurrency: usize,
    progress: Arc<Progress>,
) -> Result<usize> {
    progress.total_files.fetch_add(items.len() as u64, Ordering::Relaxed);

    let client = client.clone();
    let results: Vec<Result<bool>> = stream::iter(items)
        .map(|item| {
            let client = client.clone();
            let progress = Arc::clone(&progress);
            async move {
                if already_good(&item).await {
                    // count it as complete so the bar reflects total work, not just new work
                    progress.done_bytes.fetch_add(item.size, Ordering::Relaxed);
                    progress.done_files.fetch_add(1, Ordering::Relaxed);
                    return Ok(false);
                }
                let out = fetch_one(&client, &item, &progress).await;
                progress.done_files.fetch_add(1, Ordering::Relaxed);
                out.map(|_| true)
            }
        })
        .buffer_unordered(concurrency.clamp(1, 32))
        .collect()
        .await;

    let mut fetched = 0;
    for r in results {
        if r? {
            fetched += 1;
        }
    }
    Ok(fetched)
}

/// Sum of everything the caller is about to ask for — the denominator of the
/// progress bar has to be known before the first byte moves.
pub fn total_size(items: &[Item]) -> u64 {
    items.iter().map(|i| i.size).sum()
}

#[cfg(test)]
mod tests {
    use super::*;
    use sha1::{Digest, Sha1};

    #[tokio::test]
    async fn sha1_of_calculates_correct_hash() {
        let temp_dir = std::env::temp_dir().join("fullmoon_test_sha1");
        let _ = tokio::fs::create_dir_all(&temp_dir).await;
        let test_file = temp_dir.join("test_sha1.txt");
        let content = b"fullmoon client verification test";
        tokio::fs::write(&test_file, content).await.unwrap();

        let mut hasher = Sha1::new();
        hasher.update(content);
        let expected = hex::encode(hasher.finalize());

        let result = sha1_of(&test_file).await;
        assert_eq!(result, Some(expected));

        let _ = tokio::fs::remove_dir_all(&temp_dir).await;
    }

    #[tokio::test]
    async fn already_good_rejects_corrupt_sha1() {
        let temp_dir = std::env::temp_dir().join("fullmoon_test_corrupt");
        let _ = tokio::fs::create_dir_all(&temp_dir).await;
        let test_file = temp_dir.join("corrupt_asset.jar");
        tokio::fs::write(&test_file, b"corrupted bytes").await.unwrap();

        let item = Item {
            url: "https://example.com/asset.jar".to_string(),
            path: test_file.clone(),
            sha1: Some("0123456789abcdef0123456789abcdef01234567".to_string()),
            size: 15,
        };

        assert!(!already_good(&item).await, "corrupted asset with mismatched sha1 must be rejected");

        let _ = tokio::fs::remove_dir_all(&temp_dir).await;
    }

    #[tokio::test]
    async fn already_good_accepts_matching_sha1() {
        let temp_dir = std::env::temp_dir().join("fullmoon_test_valid");
        let _ = tokio::fs::create_dir_all(&temp_dir).await;
        let test_file = temp_dir.join("valid_asset.jar");
        let content = b"valid asset payload";
        tokio::fs::write(&test_file, content).await.unwrap();

        let mut hasher = Sha1::new();
        hasher.update(content);
        let expected_sha1 = hex::encode(hasher.finalize());

        let item = Item {
            url: "https://example.com/asset.jar".to_string(),
            path: test_file.clone(),
            sha1: Some(expected_sha1),
            size: content.len() as u64,
        };

        assert!(already_good(&item).await, "valid asset with matching sha1 must be accepted");

        let _ = tokio::fs::remove_dir_all(&temp_dir).await;
    }
}
