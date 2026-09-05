import { useEffect, useRef, useState } from "react";
import { Icon } from "./Icon";
import { SkinFace } from "./ui";
import { useStore } from "../state/store";
import { openExternal } from "../core/client";
import { useT } from "../i18n";

/* downward-opening chrome menu, same dismissal contract as DockMenu */
function TopMenu({
  open,
  setOpen,
  trigger,
  children,
}: {
  open: boolean;
  setOpen: (v: boolean) => void;
  trigger: React.ReactNode;
  children: React.ReactNode;
}) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!open) return;
    const onDown = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") setOpen(false);
    };
    document.addEventListener("mousedown", onDown);
    document.addEventListener("keydown", onKey);
    return () => {
      document.removeEventListener("mousedown", onDown);
      document.removeEventListener("keydown", onKey);
    };
  }, [open, setOpen]);
  return (
    <div className="topmenu" ref={ref}>
      <div onClick={() => setOpen(!open)}>{trigger}</div>
      {open && <div className="topmenu-panel">{children}</div>}
    </div>
  );
}

export function TopBar({ onPalette }: { onPalette: () => void }) {
  const { screen, setScreen, news, accounts, activeAccount, selectAccount, importOfficial } =
    useStore();
  const { t } = useT();
  const [bellOpen, setBellOpen] = useState(false);
  const [accOpen, setAccOpen] = useState(false);
  const [read, setRead] = useState(false);

  const latest = news.slice(0, 3);

  return (
    <header className="topbar">
      <div className="topbar-heading" key={screen}>
        <h1>{t(`nav.${screen}`)}</h1>
        <p>{t(`topbar.sub.${screen}`)}</p>
      </div>

      <div className="topbar-actions">
        <button className="searchbtn" onClick={onPalette}>
          <Icon name="search" size={15} />
          <span>{t("topbar.search")}</span>
          <kbd>Ctrl K</kbd>
        </button>

        <TopMenu
          open={bellOpen}
          setOpen={setBellOpen}
          trigger={
            <button className="bellbtn" aria-label={t("topbar.notifications")}>
              <Icon name="bell" size={17} />
              {!read && latest.length > 0 && <span className="bell-dot" />}
            </button>
          }
        >
          <div className="topmenu-title">{t("topbar.notifications")}</div>
          {latest.length === 0 ? (
            <div className="topmenu-empty">{t("topbar.noNotifications")}</div>
          ) : (
            latest.map((n) => (
              <button
                key={n.id}
                className="topmenu-item"
                onClick={() => {
                  setBellOpen(false);
                  /* a feed item points at its source post; a bundled one
                     lands on the dashboard where the ledger lives */
                  if (n.url) void openExternal(n.url);
                  else setScreen("home");
                }}
              >
                <span className="topmenu-swatch" style={{ "--h": n.hue }} />
                <span className="topmenu-item-text">
                  <strong>{n.title}</strong>
                  <em>{n.url ? "DISCORD" : n.date}</em>
                </span>
              </button>
            ))
          )}
          <button
            className="topmenu-item topmenu-foot"
            onClick={() => {
              setRead(true);
              setBellOpen(false);
            }}
          >
            <Icon name="check" size={13} />
            <span>{t("topbar.markRead")}</span>
          </button>
        </TopMenu>

        <span className="topbar-divider" aria-hidden />

        <TopMenu
          open={accOpen}
          setOpen={setAccOpen}
          trigger={
            <button className="acctchip">
              {activeAccount ? (
                <>
                  <SkinFace hue={activeAccount.skinHue} size={26} />
                  <span className="acctchip-meta">
                    <strong>{activeAccount.username}</strong>
                    <em>{t(`accounts.source.${activeAccount.source}`)}</em>
                  </span>
                </>
              ) : (
                <>
                  <span className="acctchip-none">
                    <Icon name="user" size={14} />
                  </span>
                  <span className="acctchip-meta">
                    <strong>{t("dock.needsAccount")}</strong>
                  </span>
                </>
              )}
              <Icon name="chevronDown" size={13} className="acctchip-caret" />
            </button>
          }
        >
          {accounts.map((a) => (
            <button
              key={a.uuid}
              className={`topmenu-item ${a.uuid === activeAccount?.uuid ? "active" : ""}`}
              onClick={() => {
                void selectAccount(a.uuid);
                setAccOpen(false);
              }}
            >
              <SkinFace hue={a.skinHue} size={22} />
              <span className="topmenu-item-text">
                <strong>{a.username}</strong>
                <em>{t(`accounts.source.${a.source}`)}</em>
              </span>
              {a.uuid === activeAccount?.uuid && <Icon name="check" size={14} />}
            </button>
          ))}
          <button
            className="topmenu-item topmenu-foot"
            onClick={() => {
              setAccOpen(false);
              setScreen("accounts");
            }}
          >
            <Icon name="plus" size={13} />
            <span>{t("accounts.add")}</span>
          </button>
          <button
            className="topmenu-item topmenu-foot"
            onClick={() => {
              setAccOpen(false);
              void importOfficial();
            }}
          >
            <Icon name="download" size={13} />
            <span>{t("accounts.importOfficial")}</span>
          </button>
        </TopMenu>
      </div>
    </header>
  );
}
