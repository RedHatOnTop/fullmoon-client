import { useEffect, useRef } from "react";
import { Icon } from "../components/Icon";
import { Button, ProgressBar } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";
import type { LogLevel } from "../core/bindings";

const LEVEL_CLASS: Record<LogLevel, string> = {
  PINION: "lov-accent",
  ERROR: "lov-err",
  WARN: "lov-warn",
  INFO: "lov-dim",
  DEBUG: "lov-faint",
};

/* Cinematic launch surface over the live game://log stream.
   No synthesized percentages — starting shows an indeterminate
   sweep, the moment of truth is game://state. */
export function LaunchOverlay({ onHide }: { onHide: () => void }) {
  const { game, logs, instances, killGame } = useStore();
  const { t } = useT();
  const tailRef = useRef<HTMLDivElement>(null);

  const inst = instances.find((i) => i.id === game.instanceId) ?? null;
  const running = game.state === "running";
  const tail = logs.slice(-9);

  useEffect(() => {
    tailRef.current?.scrollTo({ top: tailRef.current.scrollHeight });
  }, [logs.length]);

  return (
    <div className="lov-backdrop">
      <div className="lov card">
        <header className="lov-head">
          <span className={`lov-badge ${running ? "ok" : ""}`}>
            {running ? <Icon name="zap" size={17} /> : <Icon name="play" size={15} />}
          </span>
          <div className="lov-title">
            <strong>{running ? t("launchov.running") : t("launchov.starting")}</strong>
            <span>
              {inst ? `${inst.name} · ${inst.versionId}` : "…"}
              {game.server ? ` → ${game.server}` : ""}
            </span>
          </div>
          <button className="iconbtn" title={t("launchov.hide")} onClick={onHide}>
            <Icon name="x" size={16} />
          </button>
        </header>

        {!running && <ProgressBar pct={0} indeterminate />}
        {running && (
          <div className="lov-runline">
            {t("launchov.handoff")}
          </div>
        )}

        <div className="lov-console mono" ref={tailRef}>
          {tail.map((l) => (
            <p key={l.id} className={LEVEL_CLASS[l.level] ?? "lov-dim"}>
              <em className="num">{l.ts}</em> {l.line}
            </p>
          ))}
          {!running && <p className="lov-faint lov-caret">▍</p>}
        </div>

        <footer className="lov-actions">
          <div className="lov-actions-right">
            <Button variant="soft" size="sm" onClick={onHide}>
              {t("launchov.hide")}
            </Button>
            <Button variant="danger" size="sm" icon="stop" onClick={() => void killGame()}>
              {t("launchov.kill")}
            </Button>
          </div>
        </footer>
      </div>
    </div>
  );
}
