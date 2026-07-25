/* Brand values are generated, never asserted. One source (`brand.json`) is
   read here and emitted as compile-time constants, so a rename is a one-line
   edit plus a rebuild — not a hunt through Cargo.toml, tauri.conf.json and a
   pair of matching string literals (PLAN §0). */
use std::{env, fs, path::PathBuf};

fn main() {
    let brand_path = PathBuf::from(env::var("CARGO_MANIFEST_DIR").unwrap())
        .join("..")
        .join("..")
        .join("brand.json");
    println!("cargo:rerun-if-changed={}", brand_path.display());

    let raw = fs::read_to_string(&brand_path)
        .unwrap_or_else(|e| panic!("brand.json unreadable at {}: {e}", brand_path.display()));
    let brand: serde_json::Value = serde_json::from_str(&raw).expect("brand.json is not valid JSON");

    for key in ["name", "slug", "bin", "appId", "scheme", "tagline", "accent"] {
        let value = brand
            .get(key)
            .and_then(|v| v.as_str())
            .unwrap_or_else(|| panic!("brand.json missing string field `{key}`"));
        println!("cargo:rustc-env=PINION_{}={value}", key.to_uppercase());
    }

    tauri_build::build()
}
