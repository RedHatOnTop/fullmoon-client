import { Pinion, Min, Max, Close } from "../lib/icons";
import { version } from "../mock/data";

export default function TitleBar() {
  return (
    <header className="titlebar">
      <div className="brand">
        <span className="brand__glyph">
          <Pinion size={22} />
        </span>
        <span className="brand__word">PINION</span>
      </div>
      <span className="brand__tag">v{version.mc}</span>
      <div className="titlebar__spacer" />
      <button className="wbtn" aria-label="minimize">
        <Min size={15} />
      </button>
      <button className="wbtn" aria-label="maximize">
        <Max size={13} />
      </button>
      <button className="wbtn wbtn--close" aria-label="close">
        <Close size={15} />
      </button>
    </header>
  );
}
