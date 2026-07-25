import { useState } from "react";
import { Search, Cube } from "../lib/icons";
import { mods as seed, type Mod } from "../mock/data";

const filters = ["All", "Enabled", "Performance", "Core"] as const;

export default function Mods() {
  const [list, setList] = useState<Mod[]>(seed);
  const [q, setQ] = useState("");
  const [f, setF] = useState<(typeof filters)[number]>("All");

  const toggle = (id: string) =>
    setList((l) => l.map((m) => (m.id === id ? { ...m, on: !m.on } : m)));

  const shown = list.filter((m) => {
    if (q && !m.name.toLowerCase().includes(q.toLowerCase())) return false;
    if (f === "Enabled") return m.on;
    if (f === "Performance") return m.tag === "perf";
    if (f === "Core") return m.tag === "core";
    return true;
  });

  return (
    <div className="content fade-in">
      <div className="page-head">
        <div>
          <div className="page-title">Mods</div>
          <div className="page-sub">
            {list.filter((m) => m.on).length} of {list.length} enabled · Fabric 0.17.2
          </div>
        </div>
        <button className="btn-ghost">
          <Cube size={13} style={{ verticalAlign: -2, marginRight: 6 }} />
          Browse Modrinth
        </button>
      </div>

      <div className="toolbar">
        <div className="search">
          <Search size={16} />
          <input placeholder="Search mods…" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        {filters.map((x) => (
          <button
            key={x}
            className={"filterchip" + (f === x ? " filterchip--on" : "")}
            onClick={() => setF(x)}
          >
            {x}
          </button>
        ))}
      </div>

      <div className="mod-grid">
        {shown.map((m) => (
          <div key={m.id} className={"card modcard" + (m.on ? " modcard--on" : "")}>
            <div
              className="modcard__ic"
              style={{ background: `linear-gradient(135deg, hsl(${m.hue} 78% 58%), hsl(${m.hue + 40} 72% 48%))` }}
            >
              {m.letter}
            </div>
            <div className="modcard__body">
              <div className="modcard__name">
                {m.name}
                {m.tag === "perf" && <span className="badge badge--perf">PERF</span>}
                {m.tag === "core" && <span className="badge badge--core">PINION</span>}
              </div>
              <div className="modcard__desc">{m.desc}</div>
              <div className="modcard__foot">
                <span className="modcard__ver">v{m.ver}</span>
                <span className="modcard__dot">·</span>
                <span className="modcard__dl">{m.dl === "first-party" ? "first-party" : `${m.dl} downloads`}</span>
              </div>
            </div>
            <button
              className={"toggle" + (m.on ? " toggle--on" : "")}
              onClick={() => toggle(m.id)}
              aria-label={m.on ? "disable" : "enable"}
            >
              <span className="toggle__knob" />
            </button>
          </div>
        ))}
      </div>
    </div>
  );
}
