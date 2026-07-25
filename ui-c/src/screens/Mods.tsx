import { useEffect, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Empty, Toggle } from "../components/ui";
import { core } from "../core/client";
import type { InstalledMod } from "../core/bindings";
import { useStore } from "../state/store";
import { useT } from "../i18n";

const KIND_BADGE: Record<string, { tone: "accent" | "ok" | "warn" | "dim"; labelKey: string }> = {
  hud: { tone: "accent", labelKey: "mods.hud" },
  perf: { tone: "ok", labelKey: "mods.perf" },
  lib: { tone: "dim", labelKey: "mods.lib" },
};

const GLYPH_HUE: Record<string, number> = {
  "pinion-hud": 216,
  sodium: 152,
  lithium: 268,
  "fabric-api": 26,
};

export function ModsScreen() {
  const { instances, selectedInstanceId, selectedInstance, selectInstance, toast } = useStore();
  const { t } = useT();
  const [mods, setMods] = useState<InstalledMod[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!selectedInstanceId) return;
    setLoading(true);
    core
      .mods_list(selectedInstanceId)
      .then(setMods)
      .finally(() => setLoading(false));
  }, [selectedInstanceId]);

  const toggle = async (modId: string, enabled: boolean, name: string) => {
    if (!selectedInstanceId) return;
    setMods((m) => m.map((x) => (x.id === modId ? { ...x, enabled } : x)));
    await core.mod_toggle(selectedInstanceId, modId, enabled);
    toast("info", t(enabled ? "mods.toggleOn" : "mods.toggleOff", { name }));
  };

  const onCount = mods.filter((m) => m.enabled).length;

  return (
    <div className="screen-pad">
      <header className="page-head">
        <div>
          <h2>{t("mods.title")}</h2>
          <p>{t("mods.subtitle")}</p>
        </div>
        {mods.length > 0 && <Badge tone="accent">{t("mods.enabledFmt", { on: onCount, total: mods.length })}</Badge>}
      </header>

      {/* instance switcher */}
      <div className="mod-instances">
        {instances.map((i) => (
          <button
            key={i.id}
            className={`mod-inst-chip ${i.id === selectedInstanceId ? "active" : ""}`}
            onClick={() => selectInstance(i.id)}
          >
            <span className="dock-chip-cube sm" style={{ "--h": i.iconHue }}>
              <Icon name="layers" size={11} />
            </span>
            {i.name}
          </button>
        ))}
      </div>

      {!selectedInstance ? (
        <Empty icon="puzzle" title={t("mods.title")} hint={t("mods.bundleNote")} />
      ) : (
        <div className="mod-list stagger">
          {(loading ? [] : mods).map((m) => (
            <article key={m.id} className={`mod-row card ${m.ours ? "mod-ours gborder" : ""} ${!m.enabled ? "mod-off" : ""}`} data-glow>
              <span className="mod-glyph" style={{ "--h": GLYPH_HUE[m.id] ?? 210 }}>
                {m.name.slice(0, 1)}
              </span>
              <div className="mod-meta">
                <div className="mod-title">
                  <strong>{m.name}</strong>
                  <span className="mod-ver num">v{m.version}</span>
                  <Badge tone={KIND_BADGE[m.kind].tone}>{t(KIND_BADGE[m.kind].labelKey)}</Badge>
                  {m.ours && (
                    <Badge tone="accent">
                      <Icon name="sparkles" size={10} strokeWidth={2} /> {t("mods.ours")}
                    </Badge>
                  )}
                  {!m.compatible && <Badge tone="err">{t("mods.incompatible")}</Badge>}
                </div>
                <p className="mod-desc">{m.description}</p>
              </div>
              <Toggle checked={m.enabled} onChange={(v) => void toggle(m.id, v, m.name)} disabled={!m.compatible} />
            </article>
          ))}
          {loading && (
            <>
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="mod-row card mod-skeleton" style={{ animationDelay: `${i * 70}ms` }} />
              ))}
            </>
          )}
          <p className="mod-note">
            <Icon name="info" size={13} /> {t("mods.bundleNote")}
          </p>
        </div>
      )}
    </div>
  );
}
