/* The patchnote feed lives on the network, like the version manifest does:
   fetched fresh, cached on disk, and the bundled catalogue is the last resort
   — never the first choice. The feed is published by the Discord bot whenever
   a patchnote goes out on 📋║풀문패치노트 (contract: docs/patchnotes-feed.md);
   every item carries the message link so the launcher can hand the player to
   the source instead of a dead end. */
use serde::Deserialize;

use crate::{model::NewsItem, paths, store};

pub const FEED_URL: &str = "https://play.fullmoon.ink/feeds/patchnotes.json";

/// A launcher must not stall its boot on an announcement. Three seconds and
/// we fall back — the news panel is read often and written rarely.
const FETCH_TIMEOUT: std::time::Duration = std::time::Duration::from_secs(3);

#[derive(Debug, Deserialize)]
struct Feed {
    #[serde(default)]
    news: Vec<NewsItem>,
}

fn cache_file() -> std::path::PathBuf {
    paths::shared().join("feeds").join("patchnotes.json")
}

async fn read_cache() -> Vec<NewsItem> {
    let feed: Option<Feed> = store::read_or(&cache_file(), || None).await;
    feed.map(|f| f.news).unwrap_or_default()
}

async fn write_cache(items: &[NewsItem]) {
    let file = cache_file();
    if let Some(dir) = file.parent() {
        let _ = paths::ensure_dir(dir).await;
    }
    let body = serde_json::json!({ "news": items });
    let _ = tokio::fs::write(&file, body.to_string()).await;
}

async fn fetch_remote(client: &reqwest::Client) -> Option<Vec<NewsItem>> {
    let res = client.get(FEED_URL).timeout(FETCH_TIMEOUT).send().await.ok()?;
    if !res.status().is_success() {
        return None;
    }
    let f: Feed = res.json().await.ok()?;
    if f.news.is_empty() {
        return None;
    }
    write_cache(&f.news).await;
    Some(f.news)
}

/// Discord feed first, bundled catalogue underneath, deduped by id. Never
/// fails: a dead network degrades to the cache, a missing cache degrades to
/// the bundled news. An empty panel would read as "nothing ever happens" —
/// the one thing a launcher must not say when it merely cannot reach the
/// feed.
pub async fn feed(client: &reqwest::Client) -> Vec<NewsItem> {
    let bundled = crate::catalog::get().news.clone();
    let remote = match fetch_remote(client).await {
        Some(items) => Some(items),
        None => {
            let cached = read_cache().await;
            (!cached.is_empty()).then_some(cached)
        }
    };

    let mut items = remote.unwrap_or_default();
    let mut seen: std::collections::HashSet<String> =
        items.iter().map(|n| n.id.clone()).collect();
    for n in bundled {
        if seen.insert(n.id.clone()) {
            items.push(n);
        }
    }
    items
}
