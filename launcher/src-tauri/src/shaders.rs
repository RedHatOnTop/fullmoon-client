/* One-click shaders. Iris + Sodium + a Complementary pack, then Iris's
   properties file so the next launch actually uses the pack. The mods
   catalogue keeps Iris default-off; this path is what turns it on. */
use std::collections::BTreeMap;
use std::sync::Arc;

use crate::{
    catalog, download,
    error::{Error, Result},
    model::ShaderStatus,
    mods, paths,
};

const IRIS: &str = "iris";
const SODIUM: &str = "sodium";
const PACK_KEY: &str = "shaderPack";
const ENABLE_KEY: &str = "enableShaders";

pub fn recommended_pack() -> Result<&'static crate::model::ShaderPack> {
    catalog::get()
        .shader_packs
        .first()
        .ok_or_else(|| Error::Invalid("no shader pack is catalogued".into()))
}

pub async fn status(instance_id: &str) -> ShaderStatus {
    let st = mods::state(instance_id).await;
    let present = mods::installed(instance_id).await;
    let iris = present.contains_key(IRIS) && !st.disabled.contains(&IRIS.to_string());
    let sodium = present.contains_key(SODIUM) && !st.disabled.contains(&SODIUM.to_string());
    let props = read_properties(&paths::instance_iris_properties(instance_id)).await;
    let pack_file = props.get(PACK_KEY).filter(|s| !s.is_empty()).cloned();
    let pack_on_disk = match &pack_file {
        Some(file) => tokio::fs::metadata(paths::instance_shaderpacks_dir(instance_id).join(file))
            .await
            .is_ok(),
        None => false,
    };
    let enabled = iris
        && sodium
        && pack_on_disk
        && props.get(ENABLE_KEY).map(|s| s == "true").unwrap_or(false);
    let pack_name = pack_file.as_ref().and_then(|file| {
        catalog::get()
            .shader_packs
            .iter()
            .find(|p| file.starts_with(&p.name.replace(' ', "")) || file.contains(&p.id))
            .map(|p| p.name.clone())
            .or_else(|| Some(file.trim_end_matches(".zip").to_string()))
    });
    ShaderStatus {
        ready: iris && sodium && pack_on_disk,
        enabled,
        pack_file,
        pack_name,
        iris,
        sodium,
    }
}

/// Install Iris, Sodium, and the recommended pack, then point Iris at it.
pub async fn install_easy(
    client: &reqwest::Client,
    resources: &std::path::Path,
    instance_id: &str,
    game: &str,
    loader: &str,
    concurrency: usize,
) -> Result<ShaderStatus> {
    if loader != "fabric" {
        return Err(Error::Invalid(
            "shaders need a Fabric instance — pick or create one first".into(),
        ));
    }
    let pack = recommended_pack()?;

    let mut st = mods::state(instance_id).await;
    st.disabled.retain(|id| id != IRIS && id != SODIUM);
    mods::save_state(instance_id, &st).await?;
    mods::apply(
        client,
        resources,
        instance_id,
        game,
        loader,
        concurrency,
    )
    .await?;

    let resolved = mods::modrinth_with(client, &pack.project, game, &["iris"])
        .await?
        .ok_or_else(|| {
            Error::Invalid(format!(
                "no {} build for Minecraft {game}",
                pack.name
            ))
        })?;
    let file_name = pack_filename(&resolved.file)?;
    let dir = paths::instance_shaderpacks_dir(instance_id);
    paths::ensure_dir(&dir).await?;
    let target = dir.join(&file_name);
    if let Some(url) = resolved.url {
        download::fetch_all(
            client,
            vec![download::Item {
                url,
                path: target.clone(),
                sha1: resolved.sha1,
                size: resolved.size,
            }],
            concurrency,
            Arc::new(download::Progress::default()),
        )
        .await?;
    }
    write_iris_properties(&paths::instance_iris_properties(instance_id), &file_name, true)
        .await?;
    let out = status(instance_id).await;
    if !out.ready {
        return Err(Error::Invalid(
            "Iris, Sodium, or the shader pack did not land — try again".into(),
        ));
    }
    Ok(out)
}

fn pack_filename(name: &str) -> Result<String> {
    let base = std::path::Path::new(name)
        .file_name()
        .and_then(|s| s.to_str())
        .unwrap_or("");
    if base.is_empty() || base != name {
        return Err(Error::Invalid("shader pack file name is not a plain file".into()));
    }
    Ok(base.to_string())
}

pub async fn set_enabled(instance_id: &str, enabled: bool) -> Result<ShaderStatus> {
    let current = status(instance_id).await;
    let Some(file) = current.pack_file else {
        return Err(Error::Invalid(
            "no shader pack is installed — install shaders first".into(),
        ));
    };
    write_iris_properties(&paths::instance_iris_properties(instance_id), &file, enabled).await?;
    Ok(status(instance_id).await)
}

async fn read_properties(path: &std::path::Path) -> BTreeMap<String, String> {
    let Ok(raw) = tokio::fs::read_to_string(path).await else {
        return BTreeMap::new();
    };
    parse_properties(&raw)
}

fn parse_properties(raw: &str) -> BTreeMap<String, String> {
    let mut out = BTreeMap::new();
    for line in raw.lines() {
        let line = line.trim();
        if line.is_empty() || line.starts_with('#') || line.starts_with('!') {
            continue;
        }
        if let Some((k, v)) = line.split_once('=') {
            out.insert(k.trim().to_string(), v.trim().to_string());
        }
    }
    out
}

fn render_properties(map: &BTreeMap<String, String>) -> String {
    let mut body = String::from("# written by the Fullmoon launcher\n");
    for (k, v) in map {
        body.push_str(k);
        body.push('=');
        body.push_str(v);
        body.push('\n');
    }
    body
}

async fn write_iris_properties(
    path: &std::path::Path,
    pack_file: &str,
    enabled: bool,
) -> Result<()> {
    if let Some(parent) = path.parent() {
        paths::ensure_dir(parent).await?;
    }
    let mut map = read_properties(path).await;
    map.insert(PACK_KEY.into(), pack_file.to_string());
    map.insert(ENABLE_KEY.into(), if enabled { "true" } else { "false" }.into());
    tokio::fs::write(path, render_properties(&map)).await?;
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn properties_round_trip_keeps_unknown_keys() {
        let raw = "colorSpace=SRGB\nshaderPack=old.zip\nenableShaders=false\n";
        let mut map = parse_properties(raw);
        map.insert(PACK_KEY.into(), "ComplementaryReimagined_r5.8.1.zip".into());
        map.insert(ENABLE_KEY.into(), "true".into());
        let out = render_properties(&map);
        assert!(out.contains("colorSpace=SRGB"));
        assert!(out.contains("shaderPack=ComplementaryReimagined_r5.8.1.zip"));
        assert!(out.contains("enableShaders=true"));
        assert!(!out.contains("old.zip"));
    }

    #[test]
    fn comments_and_blanks_are_ignored() {
        let map = parse_properties("# header\n\nshaderPack=a.zip\n!bang\n");
        assert_eq!(map.get("shaderPack").unwrap(), "a.zip");
        assert_eq!(map.len(), 1);
    }

    #[test]
    fn the_catalogue_has_a_recommended_pack_and_iris() {
        let pack = recommended_pack().unwrap();
        assert_eq!(pack.id, "complementary-reimagined");
        assert!(catalog::get().mods.iter().any(|m| m.id == "iris" && !m.default_enabled));
        assert!(catalog::get().mod_sources.contains_key("iris"));
    }

    #[test]
    fn pack_filenames_must_be_plain_files() {
        assert_eq!(
            pack_filename("ComplementaryReimagined_r5.8.1.zip").unwrap(),
            "ComplementaryReimagined_r5.8.1.zip"
        );
        assert!(pack_filename("../evil.zip").is_err());
        assert!(pack_filename("packs/evil.zip").is_err());
    }
}
