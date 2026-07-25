import { useEffect, useRef, useState } from "react";
import { Play, Close, Bolt } from "../lib/icons";

type Ev = { at: number; pct?: number; stage?: string; log?: string };

const SCRIPT: Ev[] = [
  { at: 0, pct: 4, stage: "Verifying files", log: "[info] Pinion 0.1.0 — launching Main · 26.1.2" },
  { at: 250, log: "[auth] session valid — BlackCow (8b1d…f42a)" },
  { at: 550, pct: 16, stage: "Downloading libraries", log: "[dl] libraries 0/142" },
  { at: 1050, pct: 33, log: "[dl] libraries 142/142 ok" },
  { at: 1350, pct: 44, stage: "Fetching assets", log: "[dl] assets 0/3021" },
  { at: 2150, pct: 64, log: "[dl] assets 3021/3021 ok" },
  { at: 2450, pct: 73, stage: "Installing Fabric 0.17.2", log: "[fabric] intermediary + loader ready" },
  { at: 2800, pct: 81, log: "[mods] 4 enabled · Sodium · Lithium · Pinion HUD · Mod Menu" },
  { at: 3150, pct: 87, stage: "Starting Minecraft", log: "[game] Setting user: BlackCow" },
  { at: 3550, pct: 93, log: "[game] OpenGL 4.6 · Sodium 0.6.3 · 335 fps" },
  { at: 3900, pct: 98, log: "[game] Sound engine started" },
  { at: 4250, pct: 100, stage: "Running", log: "[game] Started in 4.19s" },
];

const logClass = (line: string) => {
  const tag = line.slice(0, line.indexOf("]") + 1);
  if (tag === "[auth]") return "lg lg--accent";
  if (tag === "[dl]") return "lg lg--blue";
  if (tag === "[fabric]" || tag === "[mods]") return "lg lg--violet";
  if (tag === "[game]") return "lg lg--green";
  return "lg lg--dim";
};

export default function LaunchOverlay({ onClose }: { onClose: () => void }) {
  const [pct, setPct] = useState(0);
  const [stage, setStage] = useState("Verifying files");
  const [logs, setLogs] = useState<string[]>([]);
  const [done, setDone] = useState(false);
  const consoleRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const start = performance.now();
    let idx = 0;
    const timers: number[] = [];
    SCRIPT.forEach((ev) => {
      const t = window.setTimeout(() => {
        if (ev.pct !== undefined) setPct(ev.pct);
        if (ev.stage) setStage(ev.stage);
        if (ev.log) setLogs((l) => [...l, ev.log!]);
        idx++;
        if (idx === SCRIPT.length) setDone(true);
      }, ev.at);
      timers.push(t);
    });
    void start;
    return () => timers.forEach(clearTimeout);
  }, []);

  useEffect(() => {
    consoleRef.current?.scrollTo({ top: consoleRef.current.scrollHeight });
  }, [logs]);

  return (
    <div className="overlay">
      <div className="launch card">
        <div className="launch__head">
          <div className={"launch__badge" + (done ? " launch__badge--ok" : "")}>
            {done ? <Bolt size={18} /> : <Play size={16} />}
          </div>
          <div style={{ flex: 1 }}>
            <div className="launch__title">{done ? "Minecraft is running" : "Launching Minecraft"}</div>
            <div className="launch__sub">
              Main · Fabric 26.1.2 {done ? "· PID 24188" : `· ${stage}`}
            </div>
          </div>
          <button className="wbtn" onClick={onClose} aria-label="close">
            <Close size={16} />
          </button>
        </div>

        <div className="launch__bar">
          <div
            className={"launch__fill" + (done ? " launch__fill--ok" : "")}
            style={{ width: `${pct}%` }}
          />
        </div>
        <div className="launch__meta">
          <span>{done ? "Ready — window handed to the OS" : stage}</span>
          <span className="stat">{pct}%</span>
        </div>

        <div className="console" ref={consoleRef}>
          {logs.map((l, i) => (
            <div key={i} className={logClass(l)}>
              {l}
            </div>
          ))}
          {!done && <div className="lg lg--dim caret">▍</div>}
        </div>

        {done && (
          <div className="launch__actions">
            <button className="btn-ghost" onClick={onClose}>
              Hide
            </button>
            <button className="btn-danger" onClick={onClose}>
              Force quit
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
