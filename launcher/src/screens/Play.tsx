import { useMemo, type ReactNode } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button } from "../components/ui";
import { useStore } from "../state/store";
import { openExternal } from "../core/client";
import { useT } from "../i18n";
import Skin2D from "../widgets/Skin2D";

/* the moon rabbit — the brand's one wink, surfaced only on launch intent.
   Drawn as soft blocks, coloured from the token ramp (see .rabbit-*). */
function Rabbit() {
  return (
    <span className="play-rabbit" aria-hidden="true">
      <svg viewBox="0 0 48 38" className="rabbit-svg">
        <rect x="10" y="2" width="7" height="17" rx="3.5" className="rabbit-white" />
        <rect x="12" y="6" width="3" height="11" rx="1.5" className="rabbit-pink" />
        <rect x="31" y="2" width="7" height="17" rx="3.5" className="rabbit-white" />
        <rect x="33" y="6" width="3" height="11" rx="1.5" className="rabbit-pink" />
        <rect x="8" y="14" width="32" height="24" rx="10" className="rabbit-white" />
        <circle cx="12" cy="27" r="2.5" className="rabbit-blush" />
        <circle cx="36" cy="27" r="2.5" className="rabbit-blush" />
        <circle cx="17" cy="23" r="2.5" className="rabbit-eye" />
        <circle cx="16.2" cy="22.2" r="0.8" className="rabbit-sparkle" />
        <circle cx="31" cy="23" r="2.5" className="rabbit-eye" />
        <circle cx="30.2" cy="22.2" r="0.8" className="rabbit-sparkle" />
        <polygon points="24,25.5 22.5,24 25.5,24" className="rabbit-pink" />
        <path d="M22 27 Q24 28.5 26 27" className="rabbit-mouth" />
        <rect x="11" y="32" width="8" height="5" rx="2.5" className="rabbit-white" />
        <rect x="29" y="32" width="8" height="5" rx="2.5" className="rabbit-white" />
      </svg>
    </span>
  );
}

export function PlayScreen() {
  const {
    servers, serverStatus, versions, cosmetics, loadout, modCatalog,
    activeAccount, accounts, instances, selectedInstance, installInstance, launch,
    game, setOverlayHidden, toast, setScreen, news,
  } = useStore();
  const { t } = useT();

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);

  const cape = useMemo(() => {
    const id = loadout?.cape;
    return id ? cosmetics.find((c) => c.id === id) ?? null : null;
  }, [loadout, cosmetics]);

  const featuredNews = useMemo(() => news.find((n) => n.featured) ?? news[0] ?? null, [news]);
  /* bound once: the click handler closes over a narrowed binding */
  const featuredUrl = featuredNews?.url ?? null;

  const installing = selectedInstance?.installing ?? null;
  const sessionIsMine = game.sessionId !== null && game.instanceId === selectedInstance?.id;
  const starting = game.state === "starting" && sessionIsMine;
  const running = game.state === "running" && sessionIsMine;

  const quickPlay = (address?: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  /* the one button on the stage mirrors the dock's state machine — the dock
     hides on this screen, so the word has to be honest here */
  let btnContent: ReactNode;
  let btnClass = "play-btn";
  let btnAction: () => void = () => {};
  let btnDisabled = false;

  if (!activeAccount) {
    btnContent = (
      <>
        <Icon name="user" size={20} />
        <span className="play-btn-word">
          <span>{t("dock.needsAccount")}</span>
          <span className="play-btn-sub">{t("nav.accounts")}</span>
        </span>
      </>
    );
    btnAction = () => setScreen("accounts");
  } else if (!selectedInstance) {
    btnContent = (
      <>
        <span className="spinner" />
        <span className="play-btn-word"><span>{t("dock.preparing")}</span></span>
      </>
    );
    btnClass += " busy";
    btnDisabled = true;
  } else if (installing) {
    btnContent = (
      <>
        <span className="play-btn-word">
          <span>{t(`dock.stage.${installing.stage}`)}</span>
          <span className="play-btn-sub num">{Math.floor(installing.pct)}%</span>
        </span>
        <span className="play-btn-bar" style={{ width: `${installing.pct}%` }} />
      </>
    );
    btnClass += " busy";
    btnDisabled = true;
  } else if (!selectedInstance.installed) {
    btnContent = (
      <>
        <Icon name="download" size={20} />
        <span className="play-btn-word">
          <span>{t("dock.install")}</span>
          <span className="play-btn-sub">{selectedInstance.name}</span>
        </span>
      </>
    );
    btnAction = () => void installInstance(selectedInstance.id);
  } else if (starting || running) {
    btnContent = (
      <>
        {starting ? <span className="spinner" /> : <Icon name="terminal" size={18} />}
        <span className="play-btn-word">
          <span>{starting ? t("dock.launching") : t("dock.running")}</span>
          <span className="play-btn-sub">{selectedInstance.name}</span>
        </span>
      </>
    );
    btnAction = () => setOverlayHidden(null);
  } else {
    btnContent = (
      <>
        <Icon name="play" size={22} strokeWidth={2.4} />
        <span className="play-btn-word">
          <span>{t("play.launch")}</span>
          <span className="play-btn-sub">{selectedInstance.name}</span>
        </span>
      </>
    );
    btnAction = () => quickPlay();
    btnDisabled = !installedAny;
  }

  return (
    <div className="play-wrap stagger">
      {/* the stage: word on the left, the player on the right — nothing is
          centred, the launch button is the only loud thing */}
      <section className="play-stage">
        <div className="play-lede">
          <p className="play-eyebrow">
            {t("play.eyebrow", { version: target?.id ?? "26.1.2" })}
          </p>
          <p className="play-title">
            {t("play.titleA")}
            <br />
            <strong>{t("play.titleB")}</strong>
          </p>
          <p className="play-sub">{t("play.sub")}</p>

          <div className="play-actions">
            <span className="play-btn-box">
              <Rabbit />
              <button
                className={btnClass}
                disabled={btnDisabled}
                title={btnDisabled && !installing && !selectedInstance?.installed ? t("play.notReady") : undefined}
                onClick={btnAction}
              >
                {btnContent}
              </button>
            </span>
            <Button variant="soft" size="lg" icon="gear" aria-label={t("play.configure")} onClick={() => setScreen("settings")} />
          </div>

          <div className="play-meta">
            <span>
              <Icon name="user" size={13} />
              {activeAccount ? activeAccount.username : t("dock.needsAccount")}
              {accounts.length > 1 && ` +${accounts.length - 1}`}
            </span>
            {selectedInstance && (
              <span>
                <Icon name="layers" size={13} />
                {selectedInstance.versionId} · {selectedInstance.loader}
              </span>
            )}
            {selectedInstance && (
              <span>
                <Icon name="ram" size={13} />
                {Math.round(selectedInstance.memoryMb / 1024)} GB
              </span>
            )}
            {modCatalog && (
              <span>
                <Icon name="puzzle" size={13} />
                {t("home.instModsFmt", { n: modCatalog.mods.length })}
              </span>
            )}
          </div>
        </div>

        <div className="play-figure">
          {activeAccount && (
            <span className="play-tag">
              <span className="play-tag-dot" />
              {activeAccount.username}
            </span>
          )}
          <Skin2D
            skin={activeAccount?.skinUrl ?? "/skins/blackcow.png"}
            cape={cape?.capeUrl ?? null}
            view="front"
            scale={11}
            label={activeAccount?.username}
          />
        </div>
      </section>

      {/* the deck: worlds on the left, the one story worth reading on the right */}
      <section className="play-deck">
        <div className="play-servers card">
          <div className="play-servers-head">
            <span className="dash-stat-label">{t("play.worldsTitle")}</span>
            <button className="rail-link" onClick={() => setScreen("dashboard")}>
              <span>{t("play.manage")}</span>
              <Icon name="arrowRight" size={12} />
            </button>
          </div>
          <div className="play-server-rows">
            {servers.map((s) => {
              const st = serverStatus[s.address];
              const online = st?.online === true;
              const cap = st?.maxPlayers ?? s.maxPlayers;
              const curPlayers = st ? st.players : s.players;
              return (
                <button
                  key={s.id}
                  className="play-server-row"
                  disabled={!installedAny || (st ? !online : false)}
                  onClick={() => quickPlay(s.address)}
                >
                  <span className={`play-server-dot ${online ? "on" : ""}`} />
                  <span className="play-server-name">
                    <strong>{s.name}</strong>
                    <span>{st?.motd || s.motd || s.address}</span>
                  </span>
                  <span className="play-server-num num">
                    {online ? t("play.playersOnline", { cur: curPlayers, max: cap }) : t("home.serverOffline")}
                  </span>
                  <span className="play-server-join">
                    <Icon name="arrowRight" size={15} />
                  </span>
                </button>
              );
            })}
          </div>
        </div>

        {featuredNews && (
          <button
            className="play-featured card"
            onClick={() => (featuredUrl ? void openExternal(featuredUrl) : setScreen("dashboard"))}
          >
            <div className="play-featured-top">
              <Badge tone="accent">{t("home.featured")}</Badge>
              {featuredUrl && <span className="news-src">DISCORD</span>}
              <span className="play-featured-date num">{featuredNews.date}</span>
            </div>
            <span className="play-featured-title">{featuredNews.title}</span>
            <p className="play-featured-summary">{featuredNews.summary}</p>
            <div className="play-featured-foot">
              <span>{t("play.readMore")}</span>
              <Icon name="arrowRight" size={13} />
            </div>
          </button>
        )}
      </section>
    </div>
  );
}
