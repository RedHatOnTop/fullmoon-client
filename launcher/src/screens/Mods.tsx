import { useEffect, useMemo, useState } from "react";
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

type Filter = "all" | "fav" | "hud" | "perf" | "lib";

export function ModsScreen() {
  const { instances, selectedInstanceId, selectedInstance, selectInstance, toast } = useStore();
  const { t } = useT();
  const [mods, setMods] = useState<InstalledMod[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>("all");
  const [query, setQuery] = useState("");

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

  const star = async (modId: string, favorite: boolean) => {
    if (!selectedInstanceId) return;
    setMods((m) => m.map((x) => (x.id === modId ? { ...x, favorite } : x)));
    await core.mod_favorite(selectedInstanceId, modId, favorite);
  };

  const onCount = mods.filter((m) => m.enabled).length;
  const favCount = mods.filter((m) => m.favorite).length;

  const shown = useMemo(() => {
    const q = query.trim().toLowerCase();
    return mods
      .filter((m) => (filter === "all" ? true : filter === "fav" ? m.favorite : m.kind === filter))
      .filter((m) => !q || m.name.toLowerCase().includes(q) || m.description.toLowerCase().includes(q));
  }, [mods, filter, query]);

  const tabs: Array<{ id: Filter; label: string; count: number }> = [
    { id: "all", label: t("mods.filterAll"), count: mods.length },
    { id: "fav", label: t("mods.filterFav"), count: favCount },
    { id: "hud", label: t("mods.hud"), count: mods.filter((m) => m.kind === "hud").length },
    { id: "perf", label: t("mods.perf"), count: mods.filter((m) => m.kind === "perf").length },
    { id: "lib", label: t("mods.lib"), count: mods.filter((m) => m.kind === "lib").length },
  ];

  return (
    <div className="screen-pad">
      {/* instance switcher — mods are per instance, so the target comes first */}
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
        {mods.length > 0 && (
          <span className="mod-count num">{t("mods.enabledFmt", { on: onCount, total: mods.length })}</span>
        )}
      </div>

      {!selectedInstance ? (
        <Empty icon="puzzle" title={t("mods.title")} hint={t("mods.bundleNote")} />
      ) : (
        <>
          <div className="mod-bar">
            <div className="mod-tabs">
              {tabs.map((x) => (
                <button
                  key={x.id}
                  className={`mod-tab ${filter === x.id ? "active" : ""}`}
                  onClick={() => setFilter(x.id)}
                >
                  {x.id === "fav" && <Icon name="star" size={12} strokeWidth={2} />}
                  {x.label}
                  <em className="num">{x.count}</em>
                </button>
              ))}
            </div>
            <label className="mod-search">
              <Icon name="search" size={14} />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder={t("mods.searchPlaceholder")}
                spellCheck={false}
              />
              {query && (
                <button className="mod-search-clear" onClick={() => setQuery("")} aria-label={t("common.cancel")}>
                  <Icon name="x" size={13} />
                </button>
              )}
            </label>
          </div>

          {shown.length === 0 && !loading ? (
            <Empty icon="search" title={t("mods.noMatch")} hint={t("mods.noMatchHint")} />
          ) : (
            <div className="mod-grid stagger">
              {(loading ? [] : shown).map((m) => (
                <article
                  key={m.id}
                  className={`mod-card card ${m.ours ? "mod-ours" : ""} ${!m.enabled ? "mod-off" : ""}`}
                >
                  <div className="mod-card-head">
                    <span className="mod-glyph" style={{ "--h": GLYPH_HUE[m.id] ?? 210 }}>
                      {m.name.slice(0, 1)}
                    </span>
                    <div className="mod-card-title">
                      <strong>{m.name}</strong>
                      <span className="mod-ver num">v{m.version}</span>
                    </div>
                    <button
                      className={`mod-fav ${m.favorite ? "on" : ""}`}
                      onClick={() => void star(m.id, !m.favorite)}
                      aria-pressed={m.favorite}
                      title={t(m.favorite ? "mods.unfavorite" : "mods.favorite")}
                    >
                      <Icon name="star" size={15} strokeWidth={1.8} />
                    </button>
                  </div>

                  <p className="mod-desc">{m.description}</p>

                  <div className="mod-card-foot">
                    <div className="mod-tags">
                      <Badge tone={KIND_BADGE[m.kind].tone}>{t(KIND_BADGE[m.kind].labelKey)}</Badge>
                      {m.ours && (
                        <Badge tone="accent">
                          <Icon name="gear" size={10} strokeWidth={2} /> {t("mods.ours")}
                        </Badge>
                      )}
                      {!m.compatible && <Badge tone="err">{t("mods.incompatible")}</Badge>}
                    </div>
                    <Toggle checked={m.enabled} onChange={(v) => void toggle(m.id, v, m.name)} disabled={!m.compatible} />
                  </div>
                </article>
              ))}
              {loading &&
                [0, 1, 2, 3].map((i) => (
                  <div key={i} className="mod-card card mod-skeleton" style={{ animationDelay: `${i * 70}ms` }} />
                ))}
            </div>
          )}

          <p className="mod-note">
            <Icon name="info" size={13} /> {t("mods.bundleNote")}
          </p>
        </>
      )}
    </div>
  );
}
