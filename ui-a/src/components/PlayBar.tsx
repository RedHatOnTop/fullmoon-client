import { Play, Chevron, Cube, Bolt } from "../lib/icons";
import { account, version } from "../mock/data";

export default function PlayBar({ busy, onPlay }: { busy: boolean; onPlay: () => void }) {
  return (
    <footer className="playbar">
      <button className="selectchip">
        <span className="selectchip__ic">
          <Cube size={17} />
        </span>
        <span>
          <div className="selectchip__k">Instance</div>
          <div className="selectchip__v">
            Main · {version.mc} <Chevron size={13} />
          </div>
        </span>
      </button>

      <div className="chip chip--accent">
        <Bolt size={13} /> {version.loader} {version.fabric}
      </div>

      <div className="playbar__spacer" />

      <div className="acct">
        <div style={{ textAlign: "right" }}>
          <div className="acct__name">{account.name}</div>
          <div className="acct__state">
            <span className="chip__dot" style={{ width: 6, height: 6 }} /> {account.state}
          </div>
        </div>
        <img className="avatar" src={account.avatar} alt="" />
      </div>

      <button className="play" onClick={onPlay} disabled={busy}>
        <Play size={19} />
        <span className="play__stack">
          <span>{busy ? "LAUNCHING…" : "PLAY"}</span>
          <span className="play__sub">{busy ? "see console" : `${version.loader} · ${version.mc}`}</span>
        </span>
      </button>
    </footer>
  );
}
