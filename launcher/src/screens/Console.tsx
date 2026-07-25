import { useEffect, useMemo, useRef, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, ConfirmModal, Empty, Toggle } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";

function fmtUptime(startedAt: number, nowMs: number): string {
  const s = Math.max(0, Math.floor((nowMs - startedAt) / 1000));
  const m = Math.floor(s / 60);
  const h = Math.floor(m / 60);
  if (h > 0) return `${h}:${String(m % 60).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
  return `${String(m).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

export function ConsoleScreen() {
  const { game, logs, instances, killGame, launch, toast } = useStore();
  const { t } = useT();
  const [autoscroll, setAutoscroll] = useState(true);
  const [filter, setFilter] = useState("");
  const [confirmKill, setConfirmKill] = useState(false);
  const [nowMs, setNowMs] = useState(Date.now());
  const boxRef = useRef<HTMLDivElement>(null);

  const active = game.state === "running" || game.state === "starting";
  const inst = instances.find((i) => i.id === game.instanceId) ?? null;

  /* uptime ticker */
  useEffect(() => {
    if (!active) return;
    const iv = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(iv);
  }, [active]);

  /* stick-to-bottom */
  useEffect(() => {
    const box = boxRef.current;
    if (box && autoscroll) box.scrollTop = box.scrollHeight;
  }, [logs, autoscroll]);

  const visible = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return logs;
    return logs.filter((l) => l.line.toLowerCase().includes(q) || l.level.toLowerCase().includes(q));
  }, [logs, filter]);

  if (!game.sessionId) {
    return (
      <div className="screen-pad console-empty">
        <Empty icon="terminal" title={t("console.idle")} hint={t("console.idleHint")} />
      </div>
    );
  }

  const copyAll = () => {
    const text = logs.map((l) => `[${l.ts}] ${l.line}`).join("\n");
    void navigator.clipboard?.writeText(text);
    toast("info", t("common.copied"));
  };

  const stateTone =
    game.state === "running" ? "ok" : game.state === "starting" ? "warn" : game.state === "crashed" ? "err" : "dim";

  return (
    <div className="screen-pad console">
      <header className="console-head card">
        <div className="console-head-left">
          <Badge tone={stateTone}>
            {t(`console.state.${game.state}`)}
          </Badge>
          {inst && (
            <span className="console-inst">
              <strong>{inst.name}</strong>
              <em className="num">{inst.versionId} · fabric</em>
            </span>
          )}
        </div>
        <div className="console-stats">
          {game.startedAt && (
            <span className="console-stat">
              <Icon name="clock" size={13} />
              <b className="num">{fmtUptime(game.startedAt, nowMs)}</b>
            </span>
          )}
          {game.exitCode !== null && (
            <span className="console-stat">
              {t("console.exitCode")} <b className="num">{game.exitCode}</b>
            </span>
          )}
          <span className="console-stat mono console-session">
            {t("console.session")} {game.sessionId.slice(0, 8)}
          </span>
        </div>
        <div className="console-actions">
          <div className="console-filter">
            <Icon name="search" size={13} />
            <input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder={t("console.filter")} />
          </div>
          <label className="console-auto">
            <span>{t("console.autoscroll")}</span>
            <Toggle checked={autoscroll} onChange={setAutoscroll} />
          </label>
          <Button size="sm" variant="ghost" icon="copy" onClick={copyAll}>
            {t("console.copyAll")}
          </Button>
          {active ? (
            <Button size="sm" variant="danger" icon="stop" onClick={() => setConfirmKill(true)}>
              {t("console.kill")}
            </Button>
          ) : (
            inst && (
              <Button size="sm" variant="soft" icon="refresh" onClick={() => void launch(inst.id, game.server ?? undefined)}>
                {t("console.relaunch")}
              </Button>
            )
          )}
        </div>
      </header>

      <div className="console-body card" ref={boxRef}>
        {visible.map((l) => (
          <div key={l.id} className={`log-line log-${l.level.toLowerCase()}`}>
            <span className="log-ts num">{l.ts}</span>
            <span className="log-text">{l.line}</span>
          </div>
        ))}
        {visible.length === 0 && <div className="log-empty">{t("console.filter")}</div>}
      </div>

      <ConfirmModal
        open={confirmKill}
        onClose={() => setConfirmKill(false)}
        onConfirm={() => void killGame()}
        title={t("console.kill")}
        body={t("console.killConfirm")}
        confirmLabel={t("console.kill")}
      />
    </div>
  );
}
