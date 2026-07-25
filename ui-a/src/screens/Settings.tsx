import { useState, type CSSProperties } from "react";
import { Gear, Cube, Bolt, Folder } from "../lib/icons";

function Toggle({ on, set }: { on: boolean; set: (v: boolean) => void }) {
  return (
    <button className={"toggle" + (on ? " toggle--on" : "")} onClick={() => set(!on)}>
      <span className="toggle__knob" />
    </button>
  );
}

function Seg<T extends string>({ opts, val, set }: { opts: T[]; val: T; set: (v: T) => void }) {
  return (
    <div className="seg">
      {opts.map((o) => (
        <button key={o} className={val === o ? "on" : ""} onClick={() => set(o)}>
          {o}
        </button>
      ))}
    </div>
  );
}

const HUD_MODULES = [
  ["fps", "FPS counter"],
  ["cps", "CPS meter"],
  ["coords", "Coordinates"],
  ["keystrokes", "Keystrokes"],
  ["armor", "Armor status"],
  ["potions", "Potion effects"],
  ["ping", "Ping display"],
] as const;

function GamePreview({ hud, ram, win, res }: { hud: Record<string, boolean>; ram: number; win: string; res: string }) {
  return (
    <div className="preview card">
      <div className="section-label" style={{ margin: "0 0 12px" }}>
        In-game preview
      </div>
      <div className="gameframe">
        <div className="gameframe__sky" />
        <div className="gameframe__ground" />
        {hud.fps && <div className="ovl ovl--tl stat">312 fps</div>}
        {hud.coords && <div className="ovl ovl--tl2 stat">128 / 64 / -40</div>}
        {hud.ping && (
          <div className="ovl ovl--tr stat">
            24<span style={{ opacity: 0.6 }}>ms</span>
          </div>
        )}
        {hud.potions && (
          <div className="ovl ovl--tr2">
            <span className="pot" style={{ background: "#c084fc" }} />
            <span className="pot" style={{ background: "#56d98a" }} />
          </div>
        )}
        {hud.keystrokes && (
          <div className="ovl ovl--bl">
            <div className="pk pk--w">W</div>
            <div className="pk">A</div>
            <div className="pk">S</div>
            <div className="pk">D</div>
          </div>
        )}
        {hud.cps && <div className="ovl ovl--bl2 stat">7 cps</div>}
        {hud.armor && (
          <div className="ovl ovl--br">
            <span className="arm" />
            <span className="arm" />
            <span className="arm" />
            <span className="arm" />
          </div>
        )}
      </div>
      <div className="preview__chips">
        <span className="chip">{res}</span>
        <span className="chip">{win}</span>
        <span className="chip chip--accent">
          <Bolt size={12} /> {ram}.0 GB
        </span>
      </div>
    </div>
  );
}

export default function Settings() {
  const [ram, setRam] = useState(6);
  const [win, setWin] = useState<"Windowed" | "Borderless" | "Fullscreen">("Borderless");
  const [res, setRes] = useState<"1080p" | "1440p" | "Native">("Native");
  const [hud, setHud] = useState<Record<string, boolean>>({
    fps: true,
    cps: true,
    coords: true,
    keystrokes: true,
    armor: false,
    potions: true,
    ping: true,
  });
  const ramPct = ((ram - 2) / (16 - 2)) * 100;

  return (
    <div className="content fade-in">
      <div className="page-head">
        <div>
          <div className="page-title">Settings</div>
          <div className="page-sub">Java 25 detected · dev.pinion.launcher</div>
        </div>
      </div>

      <div className="set-layout">
        <div>
          <div className="card set-group">
            <div className="set-group__title">
              <Bolt size={16} /> Java &amp; Performance
            </div>
            <div className="set-row">
              <div>
                <div className="set-row__k">Java runtime</div>
                <div className="set-row__d">Auto-provisioned for 26.1.2</div>
              </div>
              <div className="set-row__ctl">
                <div className="pathpick">
                  <Folder size={14} />
                  <span>…/jdk-25.0.3/bin/java.exe</span>
                </div>
                <button className="btn-ghost">Change</button>
              </div>
            </div>
            <div className="set-row">
              <div>
                <div className="set-row__k">Allocated memory</div>
                <div className="set-row__d">Heap size handed to the JVM</div>
              </div>
              <div className="set-row__ctl">
                <input
                  className="range"
                  type="range"
                  min={2}
                  max={16}
                  value={ram}
                  style={{ "--fill": `${ramPct}%` } as CSSProperties}
                  onChange={(e) => setRam(+e.target.value)}
                />
                <span className="rangeval">{ram}.0 GB</span>
              </div>
            </div>
          </div>

          <div className="card set-group">
            <div className="set-group__title">
              <Cube size={16} /> Game window
            </div>
            <div className="set-row">
              <div className="set-row__k">Window mode</div>
              <div className="set-row__ctl">
                <Seg opts={["Windowed", "Borderless", "Fullscreen"]} val={win} set={setWin} />
              </div>
            </div>
            <div className="set-row">
              <div className="set-row__k">Resolution</div>
              <div className="set-row__ctl">
                <Seg opts={["1080p", "1440p", "Native"]} val={res} set={setRes} />
              </div>
            </div>
          </div>

          <div className="card set-group">
            <div className="set-group__title">
              <Gear size={16} /> Pinion HUD modules
            </div>
            {HUD_MODULES.map(([k, label]) => (
              <div className="set-row" key={k}>
                <div className="set-row__k">{label}</div>
                <div className="set-row__ctl">
                  <Toggle on={hud[k]} set={(v) => setHud((h) => ({ ...h, [k]: v }))} />
                </div>
              </div>
            ))}
          </div>
        </div>

        <GamePreview hud={hud} ram={ram} win={win} res={res} />
      </div>
    </div>
  );
}
