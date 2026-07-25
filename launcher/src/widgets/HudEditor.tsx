/* HudEditor — live drag-and-drop layout editor for the in-game
   HUD modules. Writes pinion/hud.json (via core) which the
   Fabric mod reads (PLAN §3.7 / §7). */

import { useEffect, useRef, useState } from "react";
import { core } from "../core/client";
import type { HudConfig, HudModule } from "../core/bindings";
import { Icon } from "../components/Icon";
import { Button, Slider, Toggle } from "../components/ui";
import { useT } from "../i18n";
import { useStore } from "../state/store";

const SNAP = 1;
const clampPct = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

function ModuleChip({ mod, selected }: { mod: HudModule; selected: boolean }) {
  const { t } = useT();
  return (
    <div
      className={`hud-chip ${selected ? "selected" : ""}`}
      style={{ transform: `scale(${mod.scale})` }}
      data-mod={mod.id}
    >
      {mod.id === "fps" && (
        <>
          <b className="num">243</b>
          <span>FPS</span>
        </>
      )}
      {mod.id === "cps" && (
        <>
          <b className="num">7</b>
          <span>CPS</span>
        </>
      )}
      {mod.id === "coords" && (
        <span className="mono hud-chip-coords">128 · 64 · −512</span>
      )}
      {mod.id === "ping" && (
        <>
          <Icon name="signal" size={11} />
          <b className="num">23</b>
          <span>ms</span>
        </>
      )}
      {mod.id === "keystrokes" && (
        <span className="hud-keys">
          <span className="hud-keys-row">
            <i className="hud-key">W</i>
          </span>
          <span className="hud-keys-row">
            <i className="hud-key">A</i>
            <i className="hud-key down">S</i>
            <i className="hud-key">D</i>
          </span>
        </span>
      )}
      {mod.id === "gear" && (
        <span className="hud-gear">
          <i />
          <i />
          <i />
          <i />
        </span>
      )}
      {mod.id === "potion" && (
        <span className="hud-potions">
          <i style={{ "--h": 268 }}>8:12</i>
          <i style={{ "--h": 22 }}>1:34</i>
        </span>
      )}
      <em className="hud-chip-name">{t(`settings.modules.${mod.id}`)}</em>
    </div>
  );
}

export function HudEditor({ instanceId }: { instanceId: string }) {
  const { t } = useT();
  const { toast } = useStore();
  const [cfg, setCfg] = useState<HudConfig | null>(null);
  const [sel, setSel] = useState<string | null>(null);
  const boxRef = useRef<HTMLDivElement>(null);
  const dragId = useRef<string | null>(null);
  const moved = useRef(false);

  useEffect(() => {
    setCfg(null);
    setSel(null);
    void core.hud_get(instanceId).then(setCfg);
  }, [instanceId]);

  const commit = (next: HudConfig) => {
    setCfg(next);
    void core.hud_set(instanceId, next);
  };

  const mutate = (id: string, patch: Partial<HudModule>, persist = true) => {
    if (!cfg) return;
    const next = { modules: cfg.modules.map((m) => (m.id === id ? { ...m, ...patch } : m)) };
    if (persist) commit(next);
    else setCfg(next);
  };

  const onPointerDown = (e: React.PointerEvent, id: string) => {
    e.preventDefault();
    dragId.current = id;
    moved.current = false;
    setSel(id);
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
  };
  const onPointerMove = (e: React.PointerEvent) => {
    const id = dragId.current;
    const box = boxRef.current;
    if (!id || !box || !cfg) return;
    moved.current = true;
    const rect = box.getBoundingClientRect();
    const x = clampPct(((e.clientX - rect.left) / rect.width) * 100, 3, 97);
    const y = clampPct(((e.clientY - rect.top) / rect.height) * 100, 4, 96);
    mutate(id, {
      x: Math.round(x / SNAP) * SNAP,
      y: Math.round(y / SNAP) * SNAP,
    }, false);
  };
  const onPointerUp = () => {
    if (dragId.current && moved.current && cfg) void core.hud_set(instanceId, cfg);
    dragId.current = null;
  };

  const reset = () => {
    const fresh = { modules: DEFAULTS.modules.map((m) => ({ ...m })) };
    commit(fresh);
    toast("success", t("settings.resetDone"));
  };

  const selected = cfg?.modules.find((m) => m.id === sel) ?? null;

  return (
    <div className="hud-editor">
      {/* live preview */}
      <div
        className="hud-stage"
        ref={boxRef}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerLeave={onPointerUp}
      >
        {/* fake scene */}
        <div className="hud-crosshair">+</div>
        <div className="hud-hotbar">
          {Array.from({ length: 9 }, (_, i) => (
            <span key={i} className={i === 0 ? "slot active" : "slot"} />
          ))}
        </div>
        <div className="hud-hearts">
          {Array.from({ length: 10 }, (_, i) => (
            <span key={i} className="heart" />
          ))}
        </div>

        {cfg?.modules
          .filter((m) => m.enabled)
          .map((m) => (
            <div
              key={m.id}
              className="hud-node"
              style={{ left: `${m.x}%`, top: `${m.y}%` }}
              onPointerDown={(e) => onPointerDown(e, m.id)}
            >
              <ModuleChip mod={m} selected={sel === m.id} />
            </div>
          ))}

        <span className="hud-stage-hint">
          <Icon name="info" size={12} /> {t("settings.previewNote")}
        </span>
      </div>

      {/* module list + inspector */}
      <div className="hud-side">
        <ul className="hud-list">
          {cfg?.modules.map((m) => (
            <li key={m.id} className={sel === m.id ? "active" : ""} onClick={() => setSel(m.id)}>
              <span>{t(`settings.modules.${m.id}`)}</span>
              <Toggle checked={m.enabled} onChange={(v) => mutate(m.id, { enabled: v })} />
            </li>
          ))}
        </ul>

        {selected && (
          <div className="hud-inspector">
            <span className="field-label">
              {t("settings.moduleScale")} — <b className="num">{selected.scale.toFixed(2)}×</b>
            </span>
            <Slider
              min={0.6}
              max={1.8}
              step={0.05}
              value={selected.scale}
              onChange={(v) => mutate(selected.id, { scale: v })}
            />
            <div className="hud-pos mono">
              x {selected.x.toFixed(0)}% · y {selected.y.toFixed(0)}%
            </div>
          </div>
        )}

        <Button variant="outline" size="sm" icon="refresh" onClick={reset}>
          {t("settings.resetLayout")}
        </Button>
      </div>
    </div>
  );
}

const DEFAULTS: HudConfig = {
  modules: [
    { id: "fps", enabled: true, x: 4, y: 6, scale: 1 },
    { id: "cps", enabled: true, x: 4, y: 14, scale: 1 },
    { id: "coords", enabled: true, x: 5.5, y: 22, scale: 1 },
    { id: "ping", enabled: true, x: 93, y: 6, scale: 1 },
    { id: "keystrokes", enabled: true, x: 86, y: 74, scale: 1 },
    { id: "gear", enabled: true, x: 93, y: 40, scale: 1 },
    { id: "potion", enabled: false, x: 93, y: 24, scale: 1 },
  ],
};
