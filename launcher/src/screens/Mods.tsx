import { useEffect, useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, Empty, Toggle } from "../components/ui";
import { core, errText } from "../core/client";
import type { InstalledMod, ShaderStatus } from "../core/bindings";
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
  iris: 188,
  lithium: 268,
  "fabric-api": 26,
};

type Filter = "all" | "fav" | "hud" | "perf" | "lib";

function ShaderEasy({
  instanceId,
  vanilla,
  onChanged,
}: {
  instanceId: string;
  vanilla: boolean;
  onChanged: () => void;
}) {
  const { toast } = useStore();
  const { t } = useT();
  const [status, setStatus] = useState<ShaderStatus | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let live = true;
    setStatus(null);
    setBusy(false);
    void core
      .shaders_status(instanceId)
      .then((s) => {
        if (live) setStatus(s);
      })
      .catch((e) => {
        if (live) toast("error", errText(e));
      });
    return () => {
      live = false;
    };
  }, [instanceId, toast]);

  const run = async (action: "install" | "on" | "off") => {
    setBusy(true);
    try {
      const next =
        action === "install"
          ? await core.shaders_install(instanceId)
          : await core.shaders_set_enabled(instanceId, action === "on");
      setStatus(next);
      onChanged();
      toast(
        "success",
        t(action === "install" ? "mods.shaderInstalled" : action === "on" ? "mods.shaderEnabled" : "mods.shaderDisabled"),
      );
    } catch (e) {
      toast("error", errText(e));
    } finally {
      setBusy(false);
    }
  };

  const titleId = "shader-easy-title";

  return (
    <section className="shader-easy" aria-labelledby={titleId}>
      <span className="shader-easy-icon" aria-hidden="true">
        <Icon name="sun" size={20} />
      </span>
      <div className="shader-easy-copy">
        <span className="shader-easy-kicker">{t("mods.shaderKicker")}</span>
        <h2 id={titleId}>{t("mods.shaderTitle")}</h2>
        <p>{t("mods.shaderDesc")}</p>
        {status?.ready && status.packName && (
          <span className="shader-easy-ready">{t("mods.shaderReady", { name: status.packName })}</span>
        )}
      </div>
      <div className="shader-easy-actions">
        {vanilla ? (
          <p className="shader-easy-hint">{t("mods.shaderNeedFabric")}</p>
        ) : !status ? (
          <Button variant="primary" loading disabled>
            {t("mods.shaderInstall")}
          </Button>
        ) : status.ready ? (
          <Button
            variant={status.enabled ? "outline" : "primary"}
            loading={busy}
            aria-pressed={status.enabled}
            onClick={() => void run(status.enabled ? "off" : "on")}
          >
            {status.enabled ? t("mods.shaderOff") : t("mods.shaderOn")}
          </Button>
        ) : (
          <Button variant="primary" icon="download" loading={busy} onClick={() => void run("install")}>
            {t("mods.shaderInstall")}
          </Button>
        )}
      </div>
    </section>
  );
}

export function ModsScreen() {
  const { instances, selectedInstanceId, selectedInstance, selectInstance, toast, setScreen } = useStore();
  const { t } = useT();
  const [mods, setMods] = useState<InstalledMod[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<Filter>("all");
  const [query, setQuery] = useState("");
  const [shaderTick, setShaderTick] = useState(0);

  useEffect(() => {
    if (!selectedInstanceId) return;
    let live = true;
    setLoading(true);
    core
      .mods_list(selectedInstanceId)
      .then((list) => {
        if (live) setMods(list);
      })
      .catch((e) => {
        if (live) toast("error", errText(e));
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [selectedInstanceId, shaderTick, toast]);

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

  const vanilla = selectedInstance?.loader === "vanilla";
  const onCount = mods.filter((m) => m.enabled).length;
  const favCount = mods.filter((m) => m.favorite).length;
  const badCount = mods.filter((m) => !m.compatible).length;

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
          {vanilla && (
            <div className="mod-vanilla">
              <Icon name="info" size={15} />
              <div>
                <strong>{t("mods.vanillaTitle")}</strong>
                <span>{t("mods.vanillaHint")}</span>
              </div>
            </div>
          )}

          {selectedInstanceId && (
            <ShaderEasy
              key={selectedInstanceId}
              instanceId={selectedInstanceId}
              vanilla={vanilla}
              onChanged={() => setShaderTick((n) => n + 1)}
            />
          )}

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

          <div className="mod-layout">
            <div className="mod-main">
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
                      {/* the version of the jar that is actually there, not the
                          one the catalogue was written against */}
                      <span className="mod-ver num" title={m.file ?? undefined}>
                        v{m.version}
                      </span>
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
                      {m.compatible && !m.installed && <Badge tone="dim">{t("mods.notInstalled")}</Badge>}
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
            </div>

            <aside className="mod-side">
              <h3 className="mod-side-title">{t("mods.sideTitle")}</h3>

              <div className="mod-side-block">
                <div className="mod-mix" role="img" aria-label={t("mods.sideMix")}>
                  {(["hud", "perf", "lib"] as const).map((k) => {
                    const n = mods.filter((m) => m.kind === k && m.enabled).length;
                    return n > 0 ? (
                      <span key={k} className={`mod-mix-seg mod-mix-${k}`} style={{ flexGrow: n }} />
                    ) : null;
                  })}
                </div>
                <ul className="mod-legend">
                  {(["hud", "perf", "lib"] as const).map((k) => (
                    <li key={k}>
                      <span className={`mod-mix-dot mod-mix-${k}`} />
                      {t(`mods.${k}`)}
                      <em className="num">{mods.filter((m) => m.kind === k && m.enabled).length}</em>
                    </li>
                  ))}
                </ul>
              </div>

              <dl className="mod-side-facts">
                <div>
                  <dt>{t("mods.sideLoader")}</dt>
                  <dd>
                    {selectedInstance.loader.toUpperCase()} <span className="num">{selectedInstance.versionId}</span>
                  </dd>
                </div>
                <div>
                  <dt>{t("mods.sideCompat")}</dt>
                  <dd className={badCount > 0 ? "mod-side-bad" : "mod-side-ok"}>
                    {badCount > 0 ? t("mods.conflictN", { n: badCount }) : t("mods.noConflict")}
                  </dd>
                </div>
              </dl>

              <Button variant="outline" icon="gamepad" onClick={() => setScreen("settings", "hud")}>
                {t("mods.hudEdit")}
              </Button>
            </aside>
          </div>
        </>
      )}
    </div>
  );
}
