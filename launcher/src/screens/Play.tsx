import { useMemo } from "react";
import { Icon } from "../components/Icon";
import { useStore } from "../state/store";
import { useT } from "../i18n";
import BRAND from "../brand";
import Skin3D from "../widgets/Skin3D";
import { HomeScreen } from "./Home";

export function PlayScreen() {
  const {
    servers, serverStatus, versions, cosmetics, loadout,
    activeAccount, instances, selectedInstance, launch, toast, setScreen, news,
  } = useStore();
  const { t } = useT();

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);

  const cape = useMemo(() => {
    const id = loadout?.cape;
    return id ? cosmetics.find((c) => c.id === id) ?? null : null;
  }, [loadout, cosmetics]);

  const featuredNews = useMemo(() => news.find((n) => n.featured) ?? news[0] ?? null, [news]);

  const quickPlay = (address?: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  return (
    <div className="play-screen-wrapper stagger">
      {/* ── 1. Focus Hero Stage (Play Now) ── */}
      <div className="play-screen">
        {/* Center Hero Avatar & Big Launchpad Stage */}
        <div className="play-center-stage">
          {/* Floating Player Nametag */}
          <div className="play-player-nametag">
            <span className="player-name-text">{activeAccount?.username ?? "BlackCow"}</span>
          </div>

          {/* 3D Character Avatar Centerpiece */}
          <div className="play-avatar-stage">
            <Skin3D
              skin={activeAccount?.skinUrl ?? "/skins/blackcow.png"}
              cape={cape?.capeUrl ?? null}
              width={320}
              height={360}
              zoom={1.08}
            />
          </div>

          {/* Big Central Launch Action Group */}
          <div className="play-action-cluster">
            <button
              className="massive-play-button"
              disabled={!installedAny}
              onClick={() => quickPlay()}
            >
              <div className="play-btn-glow" />
              <div className="play-btn-content">
                <Icon name="play" size={24} strokeWidth={2.4} />
                <div className="play-btn-text">
                  <span className="main-word">PLAY NOW</span>
                  <span className="sub-instance-tag">
                    {selectedInstance ? selectedInstance.name : `${BRAND.name} 26.1.2`}
                  </span>
                </div>
              </div>
            </button>

            <button
              className="play-quick-config-btn"
              title="설정 및 인스턴스 옵션"
              onClick={() => setScreen("settings")}
            >
              <Icon name="gear" size={20} />
            </button>
          </div>
        </div>

        {/* Bottom Deck: Quick Servers & Featured News */}
        <div className="play-bottom-deck">
          {/* Left: Server List Strip */}
          <div className="play-servers-section">
            <div className="section-head-bar">
              <span className="section-title-label">공식 서버</span>
              <button className="view-more-link" onClick={() => setScreen("dashboard")}>
                <span>대시보드에서 관리</span>
                <Icon name="arrowRight" size={12} />
              </button>
            </div>

            <div className="play-servers-grid">
              {servers.slice(0, 3).map((s) => {
                const st = serverStatus[s.address];
                const online = st?.online === true;
                const cap = st?.maxPlayers ?? s.maxPlayers;
                const curPlayers = st ? st.players : s.players;

                return (
                  <div key={s.id} className="play-server-card card-hover" onClick={() => quickPlay(s.address)}>
                    <div className="server-icon-box" style={{ "--h": s.hue }}>
                      <Icon name="server" size={16} />
                    </div>
                    <div className="server-info-col">
                      <strong className="server-name">{s.name}</strong>
                      <span className="server-motd">{st?.motd || s.motd || s.address}</span>
                    </div>
                    <div className="server-right-col">
                      <span className="server-players num">
                        {online ? `${curPlayers}/${cap}` : "접속 가능"}
                      </span>
                      <button
                        className="server-fast-join-btn"
                        disabled={!installedAny}
                        onClick={(e) => {
                          e.stopPropagation();
                          quickPlay(s.address);
                        }}
                      >
                        <Icon name="zap" size={12} />
                        <span>입장</span>
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Right: Featured Banner Card */}
          {featuredNews && (
            <div className="play-featured-card card-hover" onClick={() => setScreen("dashboard")}>
              <div className="featured-card-top">
                <span className="featured-badge">주요 소식</span>
                <span className="featured-date num">{featuredNews.date}</span>
              </div>
              <h4 className="featured-title">{featuredNews.title}</h4>
              <p className="featured-summary">{featuredNews.summary}</p>
              <div className="featured-card-footer">
                <span>상세 보기 및 패치노트</span>
                <Icon name="arrowRight" size={14} />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* ── 2. Natural Scroll Down: Full Dashboard ── */}
      <div className="play-scroll-divider">
        <div className="divider-line" />
        <div className="scroll-indicator-pill">
          <Icon name="chevronDown" size={14} />
          <span>대시보드 및 상세 내역</span>
        </div>
        <div className="divider-line" />
      </div>

      <div className="play-dashboard-section">
        <HomeScreen />
      </div>
    </div>
  );
}
