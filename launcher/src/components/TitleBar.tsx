import { useEffect, useMemo, useState } from "react";
import { Icon } from "./Icon";
import { FeatherGlyph } from "./Logo";
import { useStore } from "../state/store";
import { isRealCore } from "../core/client";
import { useT } from "../i18n";
import BRAND from "../brand";

/* In the shell the buttons drive the real window; in the browser they fall
   back to fullscreen so the same chrome stays usable in vite dev. */
async function win() {
  const { getCurrentWindow } = await import("@tauri-apps/api/window");
  return getCurrentWindow();
}

declare const __APP_VERSION__: string;
const APP_VERSION = typeof __APP_VERSION__ !== "undefined" ? __APP_VERSION__ : "dev";

function fmtUptime(ms: number): string {
  const total = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`;
}

/* The masthead. The brand sits on the sidebar's column with no rule between
   them — the chrome reads as one building, not a strip glued on top. The sky
   to the right is drag; the only thing allowed to live there is a live
   session, because when the overlay is hidden this line is the one place
   that still says the game is running. */
export function TitleBar() {
  const { game, setOverlayHidden, toast } = useStore();
  const { t } = useT();
  const [maximized, setMaximized] = useState(false);
  const [tick, setTick] = useState(() => Date.now());
  const live = game.state === "starting" || game.state === "running";

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

  /* a clock nobody watches is an interval nobody needed — tick only live */
  useEffect(() => {
    if (!live) return;
    const id = window.setInterval(() => setTick(Date.now()), 1000);
    return () => window.clearInterval(id);
  }, [live]);

  const uptime = useMemo(() => {
    if (!live || game.startedAt === null) return null;
    return fmtUptime(tick - game.startedAt);
  }, [live, tick, game.startedAt]);

  const minimize = () => {
    /* the browser shell has no window to minimize — say so rather than
       swallowing the click */
    if (!isRealCore) return toast("info", t("titlebar.desktopOnly"));
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
    if (!isRealCore) return toast("info", t("titlebar.desktopOnly"));
    void win().then((w) => w.close());
  };

  return (
    <header className="titlebar" data-tauri-drag-region>
      <div className="titlebar-brand" data-tauri-drag-region>
        <span className="titlebar-mark">
          <FeatherGlyph size={16} />
        </span>
        <span className="titlebar-word">{BRAND.name.toUpperCase()}</span>
        <span
          className="titlebar-ver num"
          title={isRealCore ? "rust core / tauri" : "ui-standalone / vite — mock core"}
        >
          v{APP_VERSION}
        </span>
      </div>

      <div className="titlebar-sky" data-tauri-drag-region>
        {live && (
          <button
            className="titlebar-session"
            onClick={() => setOverlayHidden(null)}
            aria-label={t("launchov.console")}
            title={t("launchov.console")}
          >
            <span className={`live-dot ${game.state === "starting" ? "pending" : ""}`} />
            <span className="titlebar-session-state">
              {game.state === "running" ? t("dock.running") : t("dock.launching")}
            </span>
            {uptime && <span className="titlebar-session-uptime num">{uptime}</span>}
            <Icon name="terminal" size={13} />
          </button>
        )}
      </div>

      <div className="titlebar-controls">
        <button className="winbtn" aria-label="minimize" onClick={minimize}>
          <Icon name="minus" size={15} strokeWidth={1.5} />
        </button>
        <button className="winbtn" aria-label="maximize" onClick={toggleMax}>
          <Icon name={maximized ? "restore" : "maximize"} size={14} strokeWidth={1.5} />
        </button>
        <button className="winbtn winbtn-close" aria-label="close" onClick={close}>
          <Icon name="x" size={15} strokeWidth={1.5} />
        </button>
      </div>
    </header>
  );
}
