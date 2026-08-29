import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, IconButton } from "../components/ui";
import { useStore } from "../state/store";
import { isRealCore } from "../core/client";
import { useT } from "../i18n";
import BRAND from "../brand";
import Skin3D from "../widgets/Skin3D";
import Moonrise from "../widgets/Moonrise";

const TAG_TONE: Record<string, "accent" | "ok" | "warn" | "err" | "info" | "dim"> = {
  update: "accent",
  event: "ok",
  dev: "info",
  cosmetic: "err",
};

function fmtWhen(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

interface Realm {
  id: string;
  name: string;
  category: string;
  desc: string;
  players: number;
  maxPlayers: number;
  ping: number;
  badge: string;
  accent: string;
  address: string;
}

const REALMS: Realm[] = [
  {
    id: "lobby",
    name: "만월의 정원 (메인 로비)",
    category: "LOBBY & PLAZA",
    desc: "백악의 만월궁, 초승달 연못, 유저 광장이 펼쳐지는 만월 네트워크의 심장부.",
    players: 312,
    maxPlayers: 500,
    ping: 8,
    badge: "시즌 1 진행 중",
    accent: "#F5D06E",
    address: "lobby.fullmoon.dev",
  },
  {
    id: "survival",
    name: "생야생 서바이벌 (시즌 1)",
    category: "HARDCORE SURVIVAL",
    desc: "순수 바닐라 기반의 경제, 건축, 모험이 결합된 하드코어 생야생 월드.",
    players: 98,
    maxPlayers: 200,
    ping: 12,
    badge: "HOT",
    accent: "#68D391",
    address: "survival.fullmoon.dev",
  },
  {
    id: "pvp",
    name: "달빛 아레나 (PvP 랭크전)",
    category: "COMPETITIVE ARENA",
    desc: "1v1 듀얼, 팀 데스매치, 레이팅 랭크 시스템이 적용된 실시간 전투 경기장.",
    players: 18,
    maxPlayers: 64,
    ping: 9,
    badge: "랭크 오픈",
    accent: "#F6AD55",
    address: "pvp.fullmoon.dev",
  },
  {
    id: "parkour",
    name: "천공의 달빛 점프맵",
    category: "PARKOUR REALM",
    desc: "초심자부터 랭커까지 도전하는 100단계 천공 파쿠르와 타임어택 챌린지.",
    players: 12,
    maxPlayers: 50,
    ping: 10,
    badge: "타임어택",
    accent: "#63B3ED",
    address: "parkour.fullmoon.dev",
  },
];

export function HomeScreen() {
  const {
    news, wallet, walletTxs, servers, serverStatus, pingingServers, refreshServers, addServer,
    versions, settings, modCatalog, cosmetics, loadout,
    activeAccount, instances, selectedInstance, launch, removeServer, toast, setScreen,
  } = useStore();
  const { t } = useT();
  const [draft, setDraft] = useState({ name: "", address: "" });
  const [activeTab, setActiveTab] = useState<"realms" | "news" | "pass">("realms");

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);
  const memGb = selectedInstance ? Math.round(selectedInstance.memoryMb / 1024) : null;

  const cape = useMemo(() => {
    const id = loadout?.cape;
    return id ? cosmetics.find((c) => c.id === id) ?? null : null;
  }, [loadout, cosmetics]);

  const quickPlay = (address: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  return (
    <div className="home game-home-layout stagger">
      {/* ── 1. Grand Cinematic Hero Launchpad ── */}
      <section className="game-hero-banner card">
        <div className="game-hero-vignette" />
        <div className="game-hero-glow" />

        <div className="game-hero-body">
          <div className="game-hero-header">
            <div className="game-season-pill">
              <span className="live-pulse-dot" />
              <span className="season-tag">SEASON 1</span>
              <span className="season-title">만월의 정원 (Garden of Fullmoon)</span>
            </div>
            <div className="game-target-badge">
              <span className="net-ping">● 428명 온라인</span>
              <span className="divider">·</span>
              <span className="net-ms">11 ms</span>
            </div>
          </div>

          <h1 className="game-hero-headline">
            백악의 만월궁,<br />
            <strong>새로운 달빛 모험의 시작.</strong>
          </h1>

          <p className="game-hero-desc">
            히메지 양식 5층 천수각과 만월 광장이 펼쳐지는 풀문 네트워크의 공식 26.1.2 클라이언트.
            단일 클릭으로 로비에 접속하고 나만의 여정을 시작하세요.
          </p>

          <div className="game-hero-actions">
            <button
              className="game-launch-btn primary"
              disabled={!installedAny}
              onClick={() => quickPlay("lobby.fullmoon.dev")}
            >
              <Icon name="play" size={18} />
              <span>만월의 정원 즉시 입장</span>
            </button>
            <button
              className="game-launch-btn secondary"
              disabled={!installedAny}
              onClick={() => quickPlay("survival.fullmoon.dev")}
            >
              <Icon name="gamepad" size={16} />
              <span>생야생 서바이벌 바로가기</span>
            </button>
          </div>

          <div className="game-hero-specs">
            <div className="spec-chip">
              <Icon name="shield" size={13} />
              <span>보안 무결성 검증됨</span>
            </div>
            <div className="spec-chip">
              <Icon name="puzzle" size={13} />
              <span>고성능 최적화 번들 (Sodium + Lithium)</span>
            </div>
            {memGb !== null && (
              <div className="spec-chip">
                <Icon name="ram" size={13} />
                <span>메모리 {memGb} GB 할당</span>
              </div>
            )}
          </div>
        </div>

        <div className="game-hero-visual" aria-hidden="true">
          <Moonrise className="game-hero-moon" />
          <div className="game-hero-brand-tag">
            <span>FULLMOON CLIENT</span>
            <b className="num">1.0.0</b>
            <small>v26.1.2 Core</small>
          </div>
        </div>
      </section>

      {/* ── 2. Navigation Tabs (Realms / News / Battle Pass) ── */}
      <div className="game-section-nav">
        <div className="game-nav-buttons">
          <button
            className={`game-nav-tab ${activeTab === "realms" ? "active" : ""}`}
            onClick={() => setActiveTab("realms")}
          >
            <Icon name="globe" size={16} />
            <span>서버 월드 탐색 (Realms)</span>
            <span className="tab-count num">{REALMS.length}</span>
          </button>
          <button
            className={`game-nav-tab ${activeTab === "news" ? "active" : ""}`}
            onClick={() => setActiveTab("news")}
          >
            <Icon name="bell" size={16} />
            <span>새 소식 및 이벤트</span>
            <span className="tab-count num">{news.length}</span>
          </button>
          <button
            className={`game-nav-tab ${activeTab === "pass" ? "active" : ""}`}
            onClick={() => setActiveTab("pass")}
          >
            <Icon name="star" size={16} />
            <span>시즌 패스 & 퀘스트</span>
            <span className="tab-badge">LV 14</span>
          </button>
        </div>

        <div className="game-nav-extra">
          <button
            className="game-refresh-btn"
            onClick={() => void refreshServers()}
            disabled={pingingServers}
          >
            <Icon name="refresh" size={14} className={pingingServers ? "spin" : undefined} />
            <span>네트워크 상태 동기화</span>
          </button>
        </div>
      </div>

      {/* ── 3. Main Dynamic Content Grid ── */}
      <div className="game-main-content">
        <div className="game-primary-deck">
          {activeTab === "realms" && (
            <div className="realms-grid">
              {REALMS.map((realm) => (
                <div key={realm.id} className="realm-card card-hover">
                  <div className="realm-header">
                    <span className="realm-category" style={{ color: realm.accent }}>
                      {realm.category}
                    </span>
                    <span className="realm-badge" style={{ borderColor: `${realm.accent}40`, color: realm.accent }}>
                      {realm.badge}
                    </span>
                  </div>
                  <h3 className="realm-title">{realm.name}</h3>
                  <p className="realm-desc">{realm.desc}</p>

                  <div className="realm-footer">
                    <div className="realm-metrics">
                      <span className="realm-players num">
                        <Icon name="users" size={13} />
                        <b>{realm.players}</b> / {realm.maxPlayers}
                      </span>
                      <span className="realm-ping num">
                        <Icon name="signal" size={12} />
                        {realm.ping} ms
                      </span>
                    </div>

                    <button
                      className="realm-join-btn"
                      disabled={!installedAny}
                      onClick={() => quickPlay(realm.address)}
                    >
                      <Icon name="zap" size={14} />
                      <span>입장</span>
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {activeTab === "news" && (
            <div className="news-feed-list">
              {news.map((n) => (
                <article key={n.id} className="game-news-card card-hover">
                  <div className="news-thumb-strip" style={{ "--h": n.hue }} />
                  <div className="news-content-box">
                    <div className="news-top-bar">
                      <Badge tone={TAG_TONE[n.tag] ?? "dim"}>{n.tag.toUpperCase()}</Badge>
                      {n.featured && <span className="featured-chip">주요 공지</span>}
                      <span className="news-date num">{n.date}</span>
                    </div>
                    <h4 className="news-heading">{n.title}</h4>
                    <p className="news-summary-text">{n.summary}</p>
                  </div>
                  <div className="news-action-cell">
                    <Icon name="arrowRight" size={16} />
                  </div>
                </article>
              ))}
            </div>
          )}

          {activeTab === "pass" && (
            <div className="season-pass-panel card">
              <div className="pass-banner">
                <div className="pass-title-group">
                  <span className="pass-eyebrow">SEASON 1 BATTLE PASS</span>
                  <h2>만월의 축복 (Fullmoon Blessing)</h2>
                  <p>일일 퀘스트와 월드 탐험을 통해 달빛 조각과 한정판 코스메틱을 획득하세요.</p>
                </div>
                <div className="pass-level-pill">
                  <span className="lv-label">현재 레벨</span>
                  <span className="lv-number num">14</span>
                </div>
              </div>

              <div className="pass-progress-zone">
                <div className="pass-bar-header">
                  <span>다음 보상까지 (LV 15: 영롱한 달빛 망토)</span>
                  <span className="num">2,850 / 5,000 XP (57%)</span>
                </div>
                <div className="pass-progress-track">
                  <div className="pass-progress-fill" style={{ width: "57%" }} />
                </div>
              </div>

              <div className="daily-quests-deck">
                <h4>오늘의 달빛 퀘스트</h4>
                <div className="quest-list">
                  <div className="quest-row completed">
                    <div className="quest-check">✓</div>
                    <div className="quest-info">
                      <strong>로비 분수대에서 소원 빌기</strong>
                      <span>로비 중앙 분수대에서 달빛 조각 1개 투척</span>
                    </div>
                    <span className="quest-reward num">+500 XP · 1,000 코인</span>
                  </div>

                  <div className="quest-row in-progress">
                    <div className="quest-progress-ring">50%</div>
                    <div className="quest-info">
                      <strong>생야생 광석 50개 채굴</strong>
                      <span>다이아몬드, 금, 철 광석 채굴 (25 / 50)</span>
                    </div>
                    <span className="quest-reward num">+800 XP · 2,500 코인</span>
                  </div>

                  <div className="quest-row">
                    <div className="quest-progress-ring">0%</div>
                    <div className="quest-info">
                      <strong>아레나 듀얼 1회 승리</strong>
                      <span>달빛 아레나 1v1 매치 완료</span>
                    </div>
                    <span className="quest-reward num">+1,200 XP · 한정 칭호</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* ── 4. Right Side Station (3D Avatar Studio + Wallet + Discord) ── */}
        <aside className="game-side-station">
          {/* 3D Avatar Fitting Room Card */}
          <section className="avatar-stage-card card">
            <div className="card-top-title">
              <span className="stage-tag">3D DRESSROOM</span>
              <button className="stage-jump-btn" onClick={() => setScreen("cosmetics")}>
                <Icon name="feather" size={13} />
                <span>드레스룸</span>
              </button>
            </div>

            <div className="avatar-3d-stage">
              <Skin3D
                skin={activeAccount?.skinUrl ?? "/skins/blackcow.png"}
                cape={cape?.capeUrl ?? null}
                width={280}
                height={260}
                zoom={0.94}
              />
            </div>

            <div className="avatar-profile-footer">
              <div className="avatar-name-box">
                <strong>{activeAccount?.username ?? "미로그인"}</strong>
                <span className="player-title">달빛 수호자 (Lv. 42)</span>
              </div>
              <div className="avatar-cape-badge">
                <span className="badge-text">{cape ? cape.name : "망토 미착용"}</span>
              </div>
            </div>
          </section>

          {/* Moon Wallet & Economy Card */}
          <section className="moon-wallet-card card">
            <div className="card-top-title">
              <span className="stage-tag">재화 & 혜택</span>
              <span className="wallet-currency num">MOON COIN</span>
            </div>

            <div className="wallet-hero-amount">
              <span className="coin-symbol">🌙</span>
              <span className="amount-val num">{wallet ? wallet.balance.toLocaleString("ko-KR") : "128,450"}</span>
              <span className="coin-unit">코인</span>
            </div>

            <div className="wallet-quick-actions">
              <button className="wallet-act-btn" onClick={() => setScreen("cosmetics")}>
                <Icon name="star" size={13} />
                <span>상점 가기</span>
              </button>
              <button className="wallet-act-btn" onClick={() => toast("info", "출석 체크 보상이 지급되었습니다 (+1,000 코인)")}>
                <Icon name="check" size={13} />
                <span>일일 출석</span>
              </button>
            </div>
          </section>

          {/* Discord & Community Hub */}
          <section className="community-hub-card card">
            <div className="comm-head">
              <Icon name="users" size={16} />
              <strong>풀문 공식 디스코드</strong>
            </div>
            <p className="comm-desc">공식 디스코드에서 패치노트, 이벤트, 커뮤니티 파티를 확인하세요.</p>
            <a
              href="https://discord.gg/fullmoon"
              target="_blank"
              rel="noreferrer"
              className="discord-join-link"
            >
              <span>디스코드 커뮤니티 참여</span>
              <Icon name="arrowRight" size={13} />
            </a>
          </section>
        </aside>
      </div>
    </div>
  );
}
