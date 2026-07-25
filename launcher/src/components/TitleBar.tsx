import { Icon } from "./Icon";
import { Logo } from "./Logo";
import { useStore } from "../state/store";

export function TitleBar() {
  const { toast } = useStore();

  const toggleFullscreen = () => {
    if (document.fullscreenElement) void document.exitFullscreen();
    else void document.documentElement.requestFullscreen().catch(() => {});
  };

  return (
    <header className="titlebar">
      <div className="titlebar-left">
        <Logo size={26} />
        <span className="titlebar-tag">v1.0.0 · mock core</span>
      </div>
      <div className="titlebar-controls">
        <button
          className="winbtn"
          aria-label="minimize"
          onClick={() => toast("info", "데스크톱(Tauri) 빌드에서 동작합니다")}
        >
          <Icon name="minus" size={14} strokeWidth={1.5} />
        </button>
        <button className="winbtn" aria-label="maximize" onClick={toggleFullscreen}>
          <Icon name="maximize" size={13} strokeWidth={1.5} />
        </button>
        <button
          className="winbtn winbtn-close"
          aria-label="close"
          onClick={() => toast("info", "데스크톱(Tauri) 빌드에서 동작합니다")}
        >
          <Icon name="x" size={14} strokeWidth={1.5} />
        </button>
      </div>
    </header>
  );
}
