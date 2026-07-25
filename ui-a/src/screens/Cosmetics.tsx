import { useState } from "react";
import Skin3D from "../components/Skin3D";
import { cosmetics as seed, rarityColor, type Cosmetic } from "../mock/data";

const LOCKED = new Set(["c7", "c8"]);
const BLURB: Record<string, string> = {
  c1: "Shifting polar lights trail behind every step. Season-1 founder reward.",
  c2: "A soft cirrus drift with a woven silver hem.",
  c3: "Molten wings that ember and cool as you sprint.",
  c4: "Deep-current weave that ripples underwater.",
  c5: "Living moss-cloak that sways in the wind.",
  c6: "Volcanic glass with a razor obsidian edge.",
  c7: "Radiant solar crown — Mythic tier, event-locked.",
  c8: "Frost-laced wingspan that leaves a rime trail.",
};

export default function Cosmetics() {
  const [list, setList] = useState<Cosmetic[]>(seed);
  const [walk, setWalk] = useState(false);
  const equipped = list.find((c) => c.on) ?? null;
  const owned = list.length - LOCKED.size;

  const equip = (id: string) => {
    if (LOCKED.has(id)) return;
    setList((l) => l.map((c) => ({ ...c, on: c.id === id ? !c.on : false })));
  };

  return (
    <div className="content fade-in">
      <div className="page-head">
        <div>
          <div className="page-title">Cosmetics</div>
          <div className="page-sub">Client-side · rendered on your player in-game</div>
        </div>
        <div className="seg">
          <button className={!walk ? "on" : ""} onClick={() => setWalk(false)}>
            Idle
          </button>
          <button className={walk ? "on" : ""} onClick={() => setWalk(true)}>
            Walk
          </button>
        </div>
      </div>

      <div className="cos-layout">
        <div className="cos-left">
          <div className="card player-stage">
            <div className="stage-glow" />
            <Skin3D cape={equipped ? `/capes/${equipped.id}.png` : null} walk={walk} width={230} height={310} />
          </div>
          <div className="card cos-detail">
            {equipped ? (
              <>
                <div className="cos-detail__top">
                  <div className="cos-detail__name">{equipped.name}</div>
                  <span
                    className="cos-detail__rarity"
                    style={{ color: rarityColor(equipped.rarity), borderColor: rarityColor(equipped.rarity) }}
                  >
                    {equipped.rarity}
                  </span>
                </div>
                <div className="cos-detail__blurb">{BLURB[equipped.id]}</div>
                <div className="cos-detail__stats">
                  <div>
                    <div className="idcard__k">Slot</div>
                    <div className="idcard__v" style={{ fontSize: 15 }}>
                      Cape
                    </div>
                  </div>
                  <div>
                    <div className="idcard__k">Status</div>
                    <div className="idcard__v" style={{ fontSize: 15, color: "var(--good)" }}>
                      Equipped
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <div className="cos-detail__blurb">Select a cape to preview it on your player.</div>
            )}
          </div>
        </div>

        <div>
          <div className="section-label" style={{ marginTop: 0, display: "flex", justifyContent: "space-between" }}>
            <span>Cape vault</span>
            <span className="stat" style={{ color: "var(--text-mute)" }}>
              owned {owned} / {list.length}
            </span>
          </div>
          <div className="cos-grid">
            {list.map((c) => {
              const locked = LOCKED.has(c.id);
              return (
                <div
                  key={c.id}
                  className={"cos" + (c.on ? " cos--on" : "") + (locked ? " cos--locked" : "")}
                  onClick={() => equip(c.id)}
                >
                  <div
                    className="cos__art"
                    style={{
                      background: `radial-gradient(120% 90% at 50% 0%, hsl(${c.hue} 80% 62%), hsl(${c.hue + 30} 60% 30%))`,
                    }}
                  >
                    <svg width="40" height="50" viewBox="0 0 42 52" fill="none">
                      <path d="M12 3 Q4 28 8 47 L21 51 L34 47 Q38 28 30 3 Z" fill="rgba(255,255,255,.92)" />
                      <path d="M21 8 L26 24 L21 22 L16 24 Z" fill={`hsl(${c.hue} 70% 55%)`} />
                    </svg>
                    {locked && <div className="cos__lock">🔒</div>}
                  </div>
                  <div className="cos__meta">
                    <div className="cos__name">{c.name}</div>
                    <div className="cos__rarity" style={{ color: rarityColor(c.rarity) }}>
                      {locked ? "Locked" : c.rarity}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
