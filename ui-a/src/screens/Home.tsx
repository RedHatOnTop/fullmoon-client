import { useEffect, useState } from "react";
import { Bolt, Wifi, Play } from "../lib/icons";
import { news, servers, version } from "../mock/data";
import VoxelIsland from "../components/VoxelIsland";
import Skin3D from "../components/Skin3D";

function HudPreview() {
  const [fps, setFps] = useState(312);
  const [cps, setCps] = useState(7);
  const [active, setActive] = useState<string>("w");
  useEffect(() => {
    const seq = ["w", "wa", "w", "wd", "a", "w"];
    let i = 0;
    const t = setInterval(() => {
      setFps(300 + Math.floor(Math.random() * 40));
      setCps(5 + Math.floor(Math.random() * 6));
      i = (i + 1) % seq.length;
      setActive(seq[i]);
    }, 700);
    return () => clearInterval(t);
  }, []);
  const key = (k: string, cls = "") => (
    <div className={"key " + cls + (active.includes(k) ? " key--down" : "")}>{k.toUpperCase()}</div>
  );
  return (
    <div className="card hud">
      <div className="section-label" style={{ margin: 0 }}>
        In-game HUD · live preview
      </div>
      <div className="hud__row">
        <div className="hud__stat">
          <div className="hud__k">FPS</div>
          <div className="hud__v stat">{fps}</div>
        </div>
        <div className="hud__stat">
          <div className="hud__k">CPS</div>
          <div className="hud__v stat">
            {cps}
            <small>/s</small>
          </div>
        </div>
        <div className="hud__stat keys-wrap">
          <div className="hud__k" style={{ marginBottom: 8 }}>
            Keys
          </div>
          <div className="keys">
            {key("w", "key--w")}
            {key("a")}
            {key("s")}
            {key("d")}
          </div>
        </div>
      </div>
    </div>
  );
}

function IdentityCard() {
  return (
    <div className="card idcard">
      <div className="idcard__stage">
        <Skin3D width={132} height={210} walk cape="/capes/c1.png" zoom={0.9} />
      </div>
      <div className="idcard__info">
        <div className="idcard__name">BlackCow</div>
        <span className="chip chip--accent" style={{ marginTop: 8 }}>
          Aurora · Legendary
        </span>
        <div className="idcard__stats">
          <div>
            <div className="idcard__k">Hours</div>
            <div className="idcard__v stat">2,418</div>
          </div>
          <div>
            <div className="idcard__k">Since</div>
            <div className="idcard__v stat">2019</div>
          </div>
        </div>
      </div>
    </div>
  );
}

const pingBars = (ping: number) => {
  const cls = ping < 60 ? "" : ping < 120 ? "ping--mid" : "ping--bad";
  const heights = [7, 11, 15, 18];
  const lit = ping < 60 ? 4 : ping < 120 ? 3 : 2;
  return (
    <div className={"ping " + cls}>
      {heights.map((h, i) => (
        <i key={i} style={{ height: h, opacity: i < lit ? 1 : 0.22 }} />
      ))}
    </div>
  );
};

export default function Home() {
  return (
    <div className="content fade-in">
      <div className="page-head">
        <div>
          <div className="page-title">Welcome back, BlackCow</div>
          <div className="page-sub">Everything's synced. Feathers up.</div>
        </div>
        <span className="chip">
          <span className="chip__dot" /> All systems ready
        </span>
      </div>

      <section className="hero">
        <div className="hero__shimmer" />
        <VoxelIsland className="hero__voxel" />
        <div className="hero__content">
          <div className="hero__kicker">Featured · Update</div>
          <h1 className="hero__title">
            Minecraft {version.mc}
            <br />
            is ready to fly.
          </h1>
          <div className="hero__meta">
            <span className="chip chip--accent">
              <Bolt size={13} /> Sodium rebuilt
            </span>
            <span className="chip">
              {version.loader} {version.fabric}
            </span>
            <span className="chip">
              <Play size={12} /> Quick Play
            </span>
          </div>
        </div>
      </section>

      <div className="grid-2">
        <div>
          <div className="section-label">Latest</div>
          <div className="news">
            {news.map((n) => (
              <div key={n.id} className="card card--hover newscard">
                <div
                  className="newscard__thumb"
                  style={{ background: `linear-gradient(135deg, hsl(${n.hue} 80% 62%), hsl(${n.hue + 40} 75% 55%))` }}
                >
                  {n.tag}
                </div>
                <div>
                  <div className="newscard__title">{n.title}</div>
                  <div className="newscard__body">{n.body}</div>
                </div>
                <div className="newscard__date">{n.date}</div>
              </div>
            ))}
          </div>
        </div>
        <div className="client-col">
          <div className="section-label">Profile</div>
          <IdentityCard />
          <HudPreview />
        </div>
      </div>

      <div className="section-label">
        <Wifi size={13} style={{ verticalAlign: -2, marginRight: 6 }} />
        Favorite servers
      </div>
      <div className="servers-grid">
        {servers.map((s) => (
          <div key={s.id} className="card card--hover server">
            <div
              className="server__ico"
              style={{ background: `linear-gradient(135deg, hsl(${s.hue} 78% 60%), hsl(${s.hue + 35} 72% 52%))` }}
            >
              {s.name[0]}
            </div>
            <div style={{ minWidth: 0 }}>
              <div className="server__name">{s.name}</div>
              <div className="server__addr">{s.addr}</div>
            </div>
            <div className="server__ping">
              <span className="stat" style={{ fontSize: 12, color: "var(--text-mute)" }}>
                {s.ping}ms
              </span>
              {pingBars(s.ping)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
