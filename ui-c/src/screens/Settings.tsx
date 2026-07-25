import { useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, Segmented, Slider, Toggle } from "../components/ui";
import { core } from "../core/client";
import type { JavaRuntime } from "../core/bindings";
import { HudEditor } from "../widgets/HudEditor";
import { useStore } from "../state/store";
import { useT } from "../i18n";
import brand from "../brand";

const ACCENTS = ["#6ea8ff", "#b07cff", "#3ddc97", "#ffb454", "#ff6470", "#22d3ee"];

function JavaSection() {
  const { settings, patchSettings } = useStore();
  const { t } = useT();
  const [runtimes, setRuntimes] = useState<JavaRuntime[]>([]);
  const [scanning, setScanning] = useState(false);

  const scan = async () => {
    setScanning(true);
    setRuntimes(await core.java_detect());
    setScanning(false);
  };
  if (runtimes.length === 0 && !scanning) void scan();

  return (
    <section className="set-section" id="set-java">
      <div className="set-section-head">
        <h3>{t("settings.java")}</h3>
        <Button size="sm" variant="outline" icon="refresh" loading={scanning} onClick={() => void scan()}>
          {t("settings.rescan")}
        </Button>
      </div>
      <div className="java-list">
        {runtimes.map((j) => (
          <button
            key={j.path}
            className={`java-card card ${settings?.javaPath === j.path ? "active gborder" : ""}`} data-glow
            onClick={() => void patchSettings({ javaPath: j.path })}
          >
            <span className="java-icon">
              <Icon name="cpu" size={17} />
            </span>
            <span className="java-meta">
              <strong className="num">
                {j.version} <em className="java-arch">{j.arch}</em>
              </strong>
              <span className="java-path mono">{j.path}</span>
              <span className="java-vendor">{j.vendor}</span>
            </span>
            {j.recommended && <Badge tone="ok">{t("settings.recommended")}</Badge>}
            {settings?.javaPath === j.path && <Icon name="check" size={16} className="java-check" />}
          </button>
        ))}
      </div>
      <div className="field" style={{ marginTop: 14 }}>
        <label className="field-label">{t("settings.javaArgs")}</label>
        <input
          className="input mono"
          value={settings?.javaArgs ?? ""}
          onChange={(e) => void patchSettings({ javaArgs: e.target.value })}
          spellCheck={false}
        />
      </div>
    </section>
  );
}

export function SettingsScreen() {
  const { settings, patchSettings, selectedInstance, toast } = useStore();
  const { t, setLang } = useT();
  const [tab, setTab] = useState<"java" | "perf" | "look" | "hud" | "privacy" | "about">("java");

  if (!settings) return null;

  const tabs = [
    { id: "java" as const, icon: "cpu" as const, label: t("settings.java") },
    { id: "perf" as const, icon: "ram" as const, label: t("settings.perf") },
    { id: "look" as const, icon: "palette" as const, label: t("settings.appearance") },
    { id: "hud" as const, icon: "gamepad" as const, label: t("settings.hudSection") },
    { id: "privacy" as const, icon: "shield" as const, label: t("settings.privacy") },
    { id: "about" as const, icon: "info" as const, label: t("settings.about") },
  ];

  return (
    <div className="screen-pad">
      <header className="page-head">
        <div>
          <h2>{t("settings.title")}</h2>
        </div>
      </header>

      <div className="set-layout">
        <nav className="set-nav">
          {tabs.map((x) => (
            <button key={x.id} className={tab === x.id ? "active" : ""} onClick={() => setTab(x.id)}>
              <Icon name={x.icon} size={16} />
              {x.label}
            </button>
          ))}
        </nav>

        <div className="set-body screen-enter" key={tab}>
          {tab === "java" && <JavaSection />}

          {tab === "perf" && (
            <section className="set-section">
              <div className="set-section-head">
                <h3>{t("settings.perf")}</h3>
              </div>
              <div className="field">
                <label className="field-label">
                  {t("settings.memory")} — <b className="num">{(settings.memoryMb / 1024).toFixed(1)} GB</b>
                </label>
                <Slider
                  min={2048}
                  max={16384}
                  step={512}
                  value={settings.memoryMb}
                  onChange={(v) => void patchSettings({ memoryMb: v })}
                  marks={[2048, 4096, 6144, 8192, 12288, 16384]}
                  format={(v) => `${v / 1024}G`}
                />
                <p className="set-hint">{t("settings.memoryDesc")}</p>
              </div>
              <div className="field">
                <label className="field-label">
                  {t("settings.concurrency")} — <b className="num">{settings.concurrency}</b>
                </label>
                <Slider
                  min={1}
                  max={16}
                  value={settings.concurrency}
                  onChange={(v) => void patchSettings({ concurrency: v })}
                  marks={[1, 4, 8, 12, 16]}
                />
                <p className="set-hint">{t("settings.concurrencyDesc")}</p>
              </div>
            </section>
          )}

          {tab === "look" && (
            <section className="set-section">
              <div className="set-section-head">
                <h3>{t("settings.appearance")}</h3>
              </div>
              <div className="field">
                <label className="field-label">{t("settings.theme")}</label>
                <Segmented
                  options={[
                    { value: "dark", label: t("settings.dark") },
                    { value: "amoled", label: t("settings.amoled") },
                  ]}
                  value={settings.theme}
                  onChange={(v) => void patchSettings({ theme: v })}
                />
              </div>
              <div className="field">
                <label className="field-label">{t("settings.accent")}</label>
                <div className="accent-row">
                  {ACCENTS.map((c) => (
                    <button
                      key={c}
                      className={`accent-dot ${settings.accent === c ? "active" : ""}`}
                      style={{ background: c }}
                      onClick={() => void patchSettings({ accent: c })}
                      title={c}
                    />
                  ))}
                </div>
                <p className="set-hint">{t("settings.accentDesc")}</p>
              </div>
              <div className="field">
                <label className="field-label">{t("settings.language")}</label>
                <Segmented
                  options={[
                    { value: "ko", label: "한국어" },
                    { value: "en", label: "English" },
                  ]}
                  value={settings.language}
                  onChange={(v) => {
                    void patchSettings({ language: v });
                    setLang(v);
                  }}
                />
              </div>
            </section>
          )}

          {tab === "hud" && (
            <section className="set-section">
              <div className="set-section-head">
                <h3>{t("settings.hudSection")}</h3>
                {selectedInstance && <Badge tone="accent">{selectedInstance.name}</Badge>}
              </div>
              <p className="set-hint" style={{ marginBottom: 14 }}>{t("settings.hudDesc")}</p>
              {selectedInstance ? (
                <HudEditor key={selectedInstance.id} instanceId={selectedInstance.id} />
              ) : (
                <p className="set-hint">{t("dock.selectInstance")}</p>
              )}
            </section>
          )}

          {tab === "privacy" && (
            <section className="set-section">
              <div className="set-section-head">
                <h3>{t("settings.privacy")}</h3>
              </div>
              <div className="set-row">
                <div>
                  <strong>{t("settings.telemetry")}</strong>
                  <p className="set-hint">{t("settings.telemetryDesc")}</p>
                </div>
                <Toggle
                  checked={settings.telemetry}
                  onChange={(v) => {
                    void patchSettings({ telemetry: v });
                    toast("info", t("settings.saved"));
                  }}
                />
              </div>
            </section>
          )}

          {tab === "about" && (
            <section className="set-section about">
              <div className="about-mark">
                <span className="logo-tile" style={{
                  width: 64, height: 64, borderRadius: 19, display: "grid", placeItems: "center",
                  background: "linear-gradient(150deg, var(--accent), var(--accent-dim))",
                  color: "var(--on-accent)",
                  boxShadow: "0 10px 30px -8px var(--accent-glow)",
                }}>
                  <Icon name="sparkles" size={30} />
                </span>
              </div>
              <h3 className="about-name">PINION</h3>
              <p className="about-tagline">{brand.tagline}</p>
              <div className="about-rows">
                <div><span>{t("settings.version")}</span><b className="num">1.0.0</b></div>
                <div><span>{t("settings.build")}</span><b className="mono">ui-standalone / vite</b></div>
                <div><span>MC</span><b className="num">26.1.2</b></div>
              </div>
              <p className="set-hint">{t("settings.aboutMock")}</p>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
