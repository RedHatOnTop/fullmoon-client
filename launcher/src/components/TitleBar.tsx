import { useEffect, useState } from "react";
import { Icon } from "./Icon";
import { Logo } from "./Logo";
import { useStore } from "../state/store";
import { isRealCore } from "../core/client";

/* In the shell the buttons drive the real window; in the browser they fall
   back to fullscreen so the same chrome stays usable in vite dev. */
async function win() {
  const { getCurrentWindow } = await import("@tauri-apps/api/window");
  return getCurrentWindow();
}

export function TitleBar() {
  const { toast } = useStore();
  const [maximized, setMaximized] = useState(false);

  useEffect(() => {
    if (!isRealCore) return;
    let off: (() => void) | null = null;
    void (async () => {
      const w = await win();
      setMaximized(await w.isMaximized());
      off = await w.onResized(async () => setMaximized(await w.isMaximized()));
    })();
    return () => off?.();
  }, []);

  const minimize = () => {
    if (!isRealCore) return toast("info", "데스크톱(Tauri) 빌드에서 동작합니다");
    void win().then((w) => w.minimize());
  };

  const toggleMax = () => {
    if (!isRealCore) {
      if (document.fullscreenElement) void document.exitFullscreen();
      else void document.documentElement.requestFullscreen().catch(() => {});
      return;
    }
    void win().then((w) => w.toggleMaximize());
  };

  const close = () => {
    if (!isRealCore) return toast("info", "데스크톱(Tauri) 빌드에서 동작합니다");
    void win().then((w) => w.close());
  };

  return (
    <header className="titlebar" data-tauri-drag-region>
      <div className="titlebar-left" data-tauri-drag-region>
        <Logo size={26} />
        <span className="titlebar-tag">v1.0.0 · {isRealCore ? "core" : "mock core"}</span>
      </div>
      <div className="titlebar-controls">
        <button className="winbtn" aria-label="minimize" onClick={minimize}>
          <Icon name="minus" size={14} strokeWidth={1.5} />
        </button>
        <button className="winbtn" aria-label="maximize" onClick={toggleMax}>
          <Icon name={maximized ? "restore" : "maximize"} size={13} strokeWidth={1.5} />
        </button>
        <button className="winbtn winbtn-close" aria-label="close" onClick={close}>
          <Icon name="x" size={14} strokeWidth={1.5} />
        </button>
      </div>
    </header>
  );
}
