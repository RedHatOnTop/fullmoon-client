import { Play, Wifi } from "../lib/icons";
import { servers, worlds } from "../mock/data";

const fmt = (n: number) => n.toLocaleString("en-US");

export default function Servers() {
  return (
    <div className="content fade-in">
      <div className="page-head">
        <div>
          <div className="page-title">Servers</div>
          <div className="page-sub">Quick Play · launch straight into a world</div>
        </div>
        <button className="btn-ghost">+ Add server</button>
      </div>

      <div className="section-label" style={{ marginTop: 0 }}>
        Multiplayer · {servers.length}
      </div>
      <div className="srv-grid">
        {servers.map((s) => {
          const load = Math.min(1, s.players / s.max);
          const pingCls = s.ping < 60 ? "good" : s.ping < 120 ? "mid" : "bad";
          return (
            <div key={s.id} className="card card--hover srv">
              <div
                className="srv__banner"
                style={{
                  background: `linear-gradient(115deg, hsl(${s.hue} 70% 42%), hsl(${s.hue + 40} 65% 28%))`,
                }}
              >
                <span className="srv__fav" style={{ background: `hsl(${s.hue} 78% 58%)` }}>
                  {s.name[0]}
                </span>
                <span className="srv__ver">{s.version}</span>
              </div>
              <div className="srv__body">
                <div className="srv__top">
                  <div>
                    <div className="srv__name">{s.name}</div>
                    <div className="srv__addr">{s.addr}</div>
                  </div>
                  <div className={"srv__ping srv__ping--" + pingCls}>
                    <Wifi size={13} /> {s.ping}ms
                  </div>
                </div>
                <div className="srv__motd">{s.motd}</div>
                <div className="srv__load">
                  <div className="srv__bar">
                    <div className="srv__barfill" style={{ width: `${load * 100}%` }} />
                  </div>
                  <span className="srv__players stat">
                    {fmt(s.players)} <span className="dim">/ {fmt(s.max)}</span>
                  </span>
                </div>
                <button className="srv__join">
                  <Play size={13} /> Join server
                </button>
              </div>
            </div>
          );
        })}
      </div>

      <div className="section-label">Recent worlds</div>
      <div className="world-grid">
        {worlds.map((w) => (
          <div key={w.id} className="card card--hover world">
            <div
              className="world__thumb"
              style={{
                background: `linear-gradient(160deg, hsl(${w.hue} 55% 40%), hsl(${w.hue + 30} 45% 20%))`,
              }}
            >
              <svg viewBox="0 0 120 70" className="world__iso" fill="none">
                {[0, 1, 2].map((i) =>
                  [0, 1, 2].map((j) => {
                    const x = 60 + (i - j) * 16;
                    const y = 30 + (i + j) * 8;
                    return (
                      <g key={`${i}${j}`}>
                        <polygon points={`${x},${y} ${x + 16},${y + 8} ${x},${y + 16} ${x - 16},${y + 8}`} fill={`hsl(${w.hue} 60% 55%)`} />
                        <polygon points={`${x - 16},${y + 8} ${x},${y + 16} ${x},${y + 28} ${x - 16},${y + 20}`} fill={`hsl(${w.hue} 45% 30%)`} />
                        <polygon points={`${x + 16},${y + 8} ${x},${y + 16} ${x},${y + 28} ${x + 16},${y + 20}`} fill={`hsl(${w.hue} 45% 22%)`} />
                      </g>
                    );
                  }),
                )}
              </svg>
            </div>
            <div className="world__meta">
              <div className="world__name">{w.name}</div>
              <div className="world__mode">{w.mode}</div>
              <div className="world__last">Last played {w.last}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
