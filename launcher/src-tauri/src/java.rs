/* Real JDK discovery. Candidates come from JAVA_HOME, PATH and the vendor
   install roots people actually end up with on Windows (Adoptium, Microsoft,
   Zulu, Graal, plus Mojang's own bundled runtimes); each is then asked what
   it is with `java -version`, because a path proves nothing. */
use std::{
    collections::BTreeSet,
    path::{Path, PathBuf},
};

use crate::{error::Result, model::JavaRuntime};

/// Minecraft 26.x runs on Java 21+; anything older is listed but never
/// recommended, so the picker can still show a user their old 8.
const MIN_MAJOR: u32 = 21;

pub async fn detect() -> Result<Vec<JavaRuntime>> {
    let mut seen = BTreeSet::new();
    let mut out = Vec::new();

    for exe in candidates().await {
        let Ok(canon) = dunce_canonicalize(&exe) else { continue };
        if !seen.insert(canon.clone()) {
            continue;
        }
        if let Some(rt) = probe(&canon).await {
            out.push(rt);
        }
    }

    out.sort_by(|a, b| {
        major_of(&b.version)
            .cmp(&major_of(&a.version))
            .then_with(|| a.path.cmp(&b.path))
    });
    if let Some(first) = out.iter_mut().find(|r| major_of(&r.version) >= MIN_MAJOR) {
        first.recommended = true;
    }
    Ok(out)
}

/// Every `java` executable worth asking about, before deduplication.
async fn candidates() -> Vec<PathBuf> {
    let exe_name = if cfg!(windows) { "java.exe" } else { "java" };
    let mut found = Vec::new();

    if let Ok(home) = std::env::var("JAVA_HOME") {
        found.push(PathBuf::from(home).join("bin").join(exe_name));
    }

    if let Ok(path) = std::env::var("PATH") {
        for dir in std::env::split_paths(&path) {
            found.push(dir.join(exe_name));
        }
    }

    for root in vendor_roots() {
        let Ok(mut entries) = tokio::fs::read_dir(&root).await else { continue };
        while let Ok(Some(entry)) = entries.next_entry().await {
            let dir = entry.path();
            found.push(dir.join("bin").join(exe_name));
            // Mojang nests one more level: <root>/<component>/<platform>/bin
            if let Ok(mut inner) = tokio::fs::read_dir(&dir).await {
                while let Ok(Some(sub)) = inner.next_entry().await {
                    found.push(sub.path().join("bin").join(exe_name));
                }
            }
        }
    }

    found
}

fn vendor_roots() -> Vec<PathBuf> {
    let mut roots = Vec::new();
    if cfg!(windows) {
        for base in ["ProgramFiles", "ProgramFiles(x86)", "LOCALAPPDATA"] {
            let Ok(base) = std::env::var(base) else { continue };
            let base = PathBuf::from(base);
            roots.push(base.join("Eclipse Adoptium"));
            roots.push(base.join("Java"));
            roots.push(base.join("Microsoft"));
            roots.push(base.join("Zulu"));
            roots.push(base.join("Amazon Corretto"));
            roots.push(base.join("BellSoft"));
            roots.push(base.join("GraalVM"));
            // the official launcher's runtimes, which most players already have
            roots.push(base.join("Packages").join("Microsoft.4297127D64EC6_8wekyb3d8bbwe").join("LocalCache").join("Local").join("runtime"));
        }
        if let Ok(appdata) = std::env::var("APPDATA") {
            roots.push(PathBuf::from(appdata).join(".minecraft").join("runtime"));
        }
    } else {
        roots.push(PathBuf::from("/usr/lib/jvm"));
        roots.push(PathBuf::from("/Library/Java/JavaVirtualMachines"));
    }
    roots
}

async fn probe(exe: &Path) -> Option<JavaRuntime> {
    if !tokio::fs::try_exists(exe).await.unwrap_or(false) {
        return None;
    }
    let mut cmd = tokio::process::Command::new(exe);
    cmd.arg("-XshowSettings:properties").arg("-version");
    #[cfg(windows)]
    {
        // tokio's Command carries creation_flags itself; no CommandExt needed
        const CREATE_NO_WINDOW: u32 = 0x0800_0000;
        cmd.creation_flags(CREATE_NO_WINDOW);
    }
    let out = cmd.output().await.ok()?;
    // `-XshowSettings` and `-version` both report on stderr
    let text = format!(
        "{}{}",
        String::from_utf8_lossy(&out.stderr),
        String::from_utf8_lossy(&out.stdout)
    );

    let version = property(&text, "java.version").or_else(|| quoted_version(&text))?;
    let vendor = property(&text, "java.vendor").unwrap_or_else(|| "unknown".into());
    let arch = property(&text, "os.arch").unwrap_or_else(|| "unknown".into());

    Some(JavaRuntime {
        path: exe.to_string_lossy().replace('/', "\\"),
        version,
        vendor,
        arch,
        recommended: false,
    })
}

fn property(text: &str, key: &str) -> Option<String> {
    text.lines()
        .map(str::trim)
        .find_map(|l| l.strip_prefix(key)?.strip_prefix(" = ").map(str::to_owned))
}

/// Fallback for JVMs that don't answer -XshowSettings: `openjdk version "21.0.5"`.
fn quoted_version(text: &str) -> Option<String> {
    let line = text.lines().find(|l| l.contains(" version \""))?;
    let start = line.find('"')? + 1;
    let end = line[start..].find('"')? + start;
    Some(line[start..end].to_owned())
}

pub fn major_of(version: &str) -> u32 {
    let head = version.split(['.', '-', '+']).next().unwrap_or("0");
    // Java 8 reports as 1.8.0_x
    if head == "1" {
        return version
            .split('.')
            .nth(1)
            .and_then(|s| s.parse().ok())
            .unwrap_or(0);
    }
    head.parse().unwrap_or(0)
}

/// `std::fs::canonicalize` on Windows yields `\\?\` paths that then leak into
/// the UI and into process arguments; keep the plain form.
fn dunce_canonicalize(p: &Path) -> std::io::Result<PathBuf> {
    let c = std::fs::canonicalize(p)?;
    let s = c.to_string_lossy();
    Ok(PathBuf::from(
        s.strip_prefix(r"\\?\").map(str::to_owned).unwrap_or_else(|| s.to_string()),
    ))
}
