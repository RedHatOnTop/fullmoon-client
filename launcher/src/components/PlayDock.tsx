import { useEffect, useRef, useState, type ReactNode } from "react";
import { Icon } from "./Icon";
import { SkinFace } from "./ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";

/* upward-opening dock menu with outside-click dismissal */
function DockMenu({
  trigger,
  children,
  open,
  setOpen,
  align = "left",
}: {
  trigger: ReactNode;
  children: (close: () => void) => ReactNode;
  open: boolean;
  setOpen: (v: boolean) => void;
  align?: "left" | "right";
}) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    return () => document.removeEventListener("mousedown", onDown);
  }, [open, setOpen]);

  return (
    <div className="dockmenu" ref={ref}>
      <div onClick={() => setOpen(!open)}>{trigger}</div>
      {open && (
        <div className={`dockmenu-panel dockmenu-${align}`} onClick={() => setOpen(false)}>
          {children(() => setOpen(false))}
        </div>
      )}
    </div>
  );
}

export function PlayDock() {
  const {
    accounts, activeAccount, selectAccount,
    selectedInstance,
    installInstance, launch, game, setScreen, setOverlayHidden,
  } = useStore();
  const { t } = useT();
  const [accOpen, setAccOpen] = useState(false);

  const installing = selectedInstance?.installing ?? null;
  const sessionIsMine = game.sessionId && game.instanceId === selectedInstance?.id;
  const starting = game.state === "starting" && sessionIsMine;
  const running = game.state === "running" && sessionIsMine;

  /* decide the big button */
  let playContent: ReactNode;
  let playClass = "playbtn";
  let playAction: () => void = () => {};

  if (!activeAccount) {
    playContent = (
      <>
        <Icon name="user" size={18} />
        <span>{t("dock.needsAccount")}</span>
      </>
    );
    playClass += " playbtn-warn";
    playAction = () => setScreen("accounts");
  } else if (!selectedInstance) {
    /* the core provisions the managed instance itself; until then there is
       nothing to choose and nothing to click */
    playContent = (
      <>
        <span className="spinner spinner-light" />
        <span>{t("dock.preparing")}</span>
      </>
    );
    playClass += " playbtn-busy";
  } else if (installing) {
    playContent = (
      <>
        <div className="playbtn-progress">
          <span className="playbtn-stage">{t(`dock.stage.${installing.stage}`)}</span>
          <span className="playbtn-pct num">{Math.floor(installing.pct)}%</span>
        </div>
        <span className="playbtn-bar" style={{ width: `${installing.pct}%` }} />
      </>
    );
    playClass += " playbtn-busy";
  } else if (!selectedInstance.installed) {
    playContent = (
      <>
        <Icon name="download" size={18} />
        <span>{t("dock.install")}</span>
      </>
    );
    playAction = () => void installInstance(selectedInstance.id);
  } else if (starting) {
    playContent = (
      <>
        <span className="spinner spinner-light" />
        <span>{t("dock.launching")}</span>
      </>
    );
    playClass += " playbtn-busy";
    playAction = () => setOverlayHidden(null);
  } else if (running) {
    playContent = (
      <>
        <span className="live-dot" />
        <span>{t("dock.running")}</span>
        <Icon name="terminal" size={16} />
      </>
    );
    playClass += " playbtn-running";
    /* re-show the live surface — the console screen is gone, the overlay is it */
    playAction = () => setOverlayHidden(null);
  } else {
    playContent = (
      <>
        <span className="playbtn-orb">
          <Icon name="play" size={13} strokeWidth={2.6} />
        </span>
        <span className="playbtn-word">{t("dock.play")}</span>
      </>
    );
    playClass += " playbtn-go";
    playAction = () => void launch(selectedInstance.id);
  }

  return (
    <footer className="dock">
      <div className="dock-left">
        <DockMenu
          open={accOpen}
          setOpen={setAccOpen}
          trigger={
            <button className="dock-chip" title={t("dock.selectAccount")}>
              {activeAccount ? (
                <>
                  <SkinFace hue={activeAccount.skinHue} size={26} />
                  <span className="dock-chip-label">{activeAccount.username}</span>
                </>
              ) : (
                <>
                  <span className="dock-chip-none"><Icon name="user" size={14} /></span>
                  <span className="dock-chip-label dim">{t("dock.needsAccount")}</span>
                </>
              )}
              <Icon name="chevronDown" size={13} className="dock-chip-caret" />
            </button>
          }
        >
          {() => (
            <>
              {accounts.map((a) => (
                <button
                  key={a.uuid}
                  className={`dockmenu-item ${a.uuid === activeAccount?.uuid ? "active" : ""}`}
                  onClick={(e) => {
                    e.stopPropagation();
                    void selectAccount(a.uuid);
                    setAccOpen(false);
                  }}
                >
                  <SkinFace hue={a.skinHue} size={22} />
                  <span>{a.username}</span>
                  {a.uuid === activeAccount?.uuid && <Icon name="check" size={14} />}
                </button>
              ))}
              <button className="dockmenu-item dockmenu-add" onClick={() => setScreen("accounts")}>
                <Icon name="plus" size={14} />
                <span>{t("accounts.add")}</span>
              </button>
            </>
          )}
        </DockMenu>

        {/* the instance is not a choice — one managed install, shown as state.
            Repair lives in Settings; there is no picker and no "+ new". */}
        <div className="dock-chip dock-chip-static" title={t("dock.selectInstance")}>
          {selectedInstance ? (
            <>
              <span className="dock-chip-cube" style={{ "--h": selectedInstance.iconHue }}>
                <Icon name="layers" size={13} />
              </span>
              <span className="dock-chip-label">
                {selectedInstance.name}
                <em className="num">{selectedInstance.versionId}</em>
              </span>
            </>
          ) : (
            <>
              <span className="dock-chip-none"><Icon name="layers" size={14} /></span>
              <span className="dock-chip-label dim">{t("dock.preparing")}</span>
            </>
          )}
        </div>
      </div>

      <span className="dock-divider" aria-hidden />

      <button className={playClass} onClick={playAction}>
        {playContent}
      </button>
    </footer>
  );
}
