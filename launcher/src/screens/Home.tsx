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

export function HomeScreen() {
  const {
    news, wallet, walletTxs, servers, serverStatus, pingingServers, refreshServers, addServer,
    versions, settings, modCatalog, cosmetics, loadout,
    activeAccount, instances, selectedInstance, launch, removeServer, toast, setScreen,
  } = useStore();
  const { t } = useT();
  const [draft, setDraft] = useState({ name: "", address: "" });
  const [activeTab, setActiveTab] = useState<"servers" | "wallet" | "news">("servers");

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);
  const memGb = selectedInstance ? Math.round(selectedInstance.memoryMb / 1024) : null;

  const cape = useMemo(() => {
    const id = loadout?.cape;
    return id ? cosmetics.find((c) => c.id === id) ?? null : null;
  }, [loadout, cosmetics]);

  // Wallet income/expense analytics computed from real transactions
  const walletStats = useMemo(() => {
    let income = 0;
    let expense = 0;
    for (const tx of walletTxs) {
      if (tx.delta >= 0) income += tx.delta;
      else expense += Math.abs(tx.delta);
    }
    return { income, expense };
  }, [walletTxs]);

  const quickPlay = (address: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  const primaryServer = servers[0] ?? null;
  const primaryStatus = primaryServer ? serverStatus[primaryServer.address] : null;

  return (
    <div className="home game-home-layout stagger">
      {/* ── 1. Grand Cinematic Hero Banner ── */}
      <section className="game-hero-banner card">
        <div className="game-hero-vignette" />
        <div className="game-hero-glow" />

        <div className="game-hero-body">
          <div className="hero-eyebrow">
            <span>OFFICIAL CLIENT</span>
            <span className="divider">/</span>
            <span>FABRIC 26.1.2</span>
          </div>

          <h1 className="game-hero-headline">
            백악의 만월궁,<br />
            <strong>새로운 달빛 모험의 시작.</strong>
          </h1>

          <p className="game-hero-desc">
            Fabric 26.1.2 기반의 고성능 최적화 번들과 만월 인게임 HUD, 실시간 재화 연동 시스템이 탑재된 공식 클라이언트입니다.
          </p>

          <div className="game-hero-actions">
            {primaryServer ? (
              <button
                className="game-launch-btn primary"
                disabled={!installedAny}
                onClick={() => quickPlay(primaryServer.address)}
              >
                <Icon name="play" size={18} />
                <span>{primaryServer.name} 즉시 입장</span>
              </button>
            ) : (
              <button
                className="game-launch-btn primary"
                disabled={!installedAny || !selectedInstance}
                onClick={() => selectedInstance && void launch(selectedInstance.id)}
              >
                <Icon name="play" size={18} />
                <span>게임 시작</span>
              </button>
            )}

            {servers[1] && (
              <button
                className="game-launch-btn secondary"
                disabled={!installedAny}
                onClick={() => quickPlay(servers[1].address)}
              >
                <Icon name="gamepad" size={16} />
                <span>{servers[1].name} 접속</span>
              </button>
            )}
          </div>

          <div className="game-hero-specs">
            <div className="spec-chip">
              <Icon name="shield" size={13} />
              <span>무결성 SHA1 검증</span>
            </div>
            {modCatalog && (
              <div className="spec-chip">
                <Icon name="puzzle" size={13} />
                <span>{modCatalog.mods.length}개 번들 모드 탑재</span>
              </div>
            )}
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
            <span>{BRAND.name.toUpperCase()} CLIENT</span>
            <b className="num">1.0.0</b>
            <small className="num">{target ? `${target.id} target` : "26.1.2"}</small>
          </div>
        </div>
      </section>

      {/* ── 2. Functional Feature Navigation Tabs ── */}
      <div className="game-section-nav">
        <div className="game-nav-buttons">
          <button
            className={`game-nav-tab ${activeTab === "servers" ? "active" : ""}`}
            onClick={() => setActiveTab("servers")}
          >
            <Icon name="globe" size={16} />
            <span>서버 및 월드 접속</span>
            <span className="tab-count num">{servers.length}</span>
          </button>
          <button
            className={`game-nav-tab ${activeTab === "wallet" ? "active" : ""}`}
            onClick={() => setActiveTab("wallet")}
          >
            <Icon name="star" size={16} />
            <span>재화 통계 및 내역</span>
            <span className="tab-badge">{wallet ? `${wallet.balance.toLocaleString("ko-KR")}원` : "모니터링"}</span>
          </button>
          <button
            className={`game-nav-tab ${activeTab === "news" ? "active" : ""}`}
            onClick={() => setActiveTab("news")}
          >
            <Icon name="bell" size={16} />
            <span>새 소식 및 패치노트</span>
            <span className="tab-count num">{news.length}</span>
          </button>
        </div>

        <div className="game-nav-extra">
          <button
            className="game-refresh-btn"
            onClick={() => void refreshServers()}
            disabled={pingingServers}
          >
            <Icon name="refresh" size={14} className={pingingServers ? "spin" : undefined} />
            <span>서버 핑 새로고침</span>
          </button>
        </div>
      </div>

      {/* ── 3. Main Dynamic Content Grid ── */}
      <div className="game-main-content">
        <div className="game-primary-deck">
          {/* TAB 1: Real Servers Grid with Add/Remove and Live Status */}
          {activeTab === "servers" && (
            <div className="servers-management-zone">
              <div className="realms-grid">
                {servers.map((s) => {
                  const st = serverStatus[s.address];
                  const online = st?.online === true;
                  const cap = st?.maxPlayers ?? s.maxPlayers;
                  const curPlayers = st ? st.players : s.players;
                  const curPing = st ? st.pingMs : s.pingMs;

                  return (
                    <article key={s.id} className="realm-card card-hover">
                      <div className="realm-header">
                        <span className="realm-category" style={{ color: "var(--accent)" }}>
                          {s.address}
                        </span>
                        <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
                          <span
                            className="realm-badge"
                            style={{
                              borderColor: online ? "rgba(104, 211, 145, 0.4)" : "rgba(255, 255, 255, 0.15)",
                              color: online ? "#68D391" : "var(--text-3)",
                            }}
                          >
                            {online ? "온라인" : (st ? "오프라인" : "대기 중")}
                          </span>
                          <IconButton
                            icon="x"
                            label={t("home.removeServer")}
                            onClick={() => void removeServer(s.id)}
                          />
                        </div>
                      </div>

                      <h3 className="realm-title">{s.name}</h3>
                      <p className="realm-desc">{st?.motd || s.motd || s.address}</p>

                      <div className="realm-footer">
                        <div className="realm-metrics">
                          <span className="realm-players num">
                            <Icon name="users" size={13} />
                            <b>{curPlayers}</b> / {cap}
                          </span>
                          <span className="realm-ping num">
                            <Icon name="signal" size={12} />
                            {online ? `${curPing} ms` : "—"}
                          </span>
                        </div>

                        <button
                          className="realm-join-btn"
                          disabled={!installedAny || (st && !online)}
                          onClick={() => quickPlay(s.address)}
                        >
                          <Icon name="zap" size={14} />
                          <span>입장</span>
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>

              {/* Add Server Form */}
              <form
                className="server-add-bar"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!draft.address.trim()) return;
                  void addServer(draft.name || draft.address, draft.address);
                  setDraft({ name: "", address: "" });
                }}
              >
                <input
                  className="game-input"
                  value={draft.name}
                  onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                  placeholder="서버 이름 (예: 친구 서버)"
                  spellCheck={false}
                />
                <input
                  className="game-input mono"
                  value={draft.address}
                  onChange={(e) => setDraft({ ...draft, address: e.target.value })}
                  placeholder="서버 주소 (예: play.myserver.net)"
                  spellCheck={false}
                />
                <button className="game-add-btn" type="submit" disabled={!draft.address.trim()}>
                  <Icon name="plus" size={14} />
                  <span>서버 등록</span>
                </button>
              </form>
            </div>
          )}

          {/* TAB 2: Real Wallet Statistics & Transaction Ledger */}
          {activeTab === "wallet" && (
            <div className="wallet-analytics-panel card">
              <div className="wallet-stats-overview">
                <div className="stat-box main-balance">
                  <span className="stat-label">보유 잔액</span>
                  <div className="stat-value-hero num">
                    {wallet ? wallet.balance.toLocaleString("ko-KR") : "0"}<small className="unit-label">{wallet?.currency ?? "원"}</small>
                  </div>
                  <small className="stat-sub num">
                    마지막 갱신: {wallet ? fmtWhen(wallet.updatedAt) : "오프라인"}
                  </small>
                </div>

                <div className="stat-box income">
                  <span className="stat-label">최근 총 획득량</span>
                  <div className="stat-value num text-ok">
                    +{walletStats.income.toLocaleString("ko-KR")}원
                  </div>
                  <small className="stat-sub">출석, 알바, 이벤트 보상 합산</small>
                </div>

                <div className="stat-box expense">
                  <span className="stat-label">최근 총 사용량</span>
                  <div className="stat-value num text-danger">
                    −{walletStats.expense.toLocaleString("ko-KR")}원
                  </div>
                  <small className="stat-sub">상점 아이템 및 코스메틱 구매</small>
                </div>
              </div>

              <div className="wallet-tx-history">
                <div className="tx-head">
                  <h4>실시간 거래 및 보상 내역</h4>
                  <span className="tx-count num">총 {walletTxs.length}건</span>
                </div>

                <div className="tx-list-table">
                  {walletTxs.map((tx, idx) => (
                    <div key={tx.at + tx.reason + idx} className="tx-item-row">
                      <div className="tx-icon-cell">
                        <span className={`tx-icon-badge ${tx.delta >= 0 ? "in" : "out"}`}>
                          {tx.delta >= 0 ? "+" : "−"}
                        </span>
                      </div>
                      <div className="tx-detail-cell">
                        <strong>{tx.label}</strong>
                        <span className="tx-reason-code mono">{tx.reason}</span>
                      </div>
                      <div className="tx-time-cell num">
                        {fmtWhen(tx.at)}
                      </div>
                      <div className="tx-amount-cell">
                        <span className={`tx-amount-text num ${tx.delta >= 0 ? "text-ok" : "text-danger"}`}>
                          {tx.delta >= 0 ? "+" : "−"}{Math.abs(tx.delta).toLocaleString("ko-KR")} {wallet?.currency ?? "원"}
                        </span>
                        {tx.balanceAfter !== null && (
                          <span className="tx-bal-after num">잔액 {tx.balanceAfter.toLocaleString("ko-KR")}원</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: Real News & Announcements Feed */}
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
        </div>

        {/* ── 4. Right Side Station: 3D Dressroom + Wallet + Instance Info ── */}
        <aside className="game-side-station">
          {/* Real 3D Avatar Stage */}
          <section className="avatar-stage-card card">
            <div className="card-top-title">
              <span className="stage-tag">3D DRESSROOM</span>
              <button className="stage-jump-btn" onClick={() => setScreen("cosmetics")}>
                <Icon name="feather" size={13} />
                <span>장착 변경</span>
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
                <span className="player-title">
                  {activeAccount ? t(`accounts.source.${activeAccount.source}`) : "계정 필요"}
                </span>
              </div>
              <div className="avatar-cape-badge">
                <span className="badge-text">{cape ? cape.name : "망토 미착용"}</span>
              </div>
            </div>
          </section>

          {/* Real Wallet Summary Card */}
          <section className="moon-wallet-card card">
            <div className="card-top-title">
              <span className="stage-tag">보유 잔액</span>
              <span className="wallet-currency num">{wallet?.currency ?? "원"}</span>
            </div>

            <div className="wallet-hero-amount">
              <span className="amount-val num">{wallet ? wallet.balance.toLocaleString("ko-KR") : "0"}</span>
              <span className="coin-unit">{wallet?.currency ?? "원"}</span>
            </div>

            <div className="wallet-quick-actions">
              <button className="wallet-act-btn" onClick={() => setActiveTab("wallet")}>
                <Icon name="star" size={13} />
                <span>거래 통계</span>
              </button>
              <button className="wallet-act-btn" onClick={() => setScreen("cosmetics")}>
                <Icon name="feather" size={13} />
                <span>코스메틱 상점</span>
              </button>
            </div>
          </section>

          {/* Active Instance Specs Card */}
          <section className="instance-summary-card card">
            <div className="card-top-title">
              <span className="stage-tag">클라이언트 인스턴스</span>
              <button className="stage-jump-btn" onClick={() => setScreen("settings")}>
                <Icon name="gear" size={13} />
                <span>설정</span>
              </button>
            </div>

            <div className="instance-info-rows">
              <div className="inst-row">
                <span className="inst-label">타겟 버전</span>
                <span className="inst-val num">{target ? target.id : "26.1.2"} ({selectedInstance?.loader ?? "fabric"})</span>
              </div>
              <div className="inst-row">
                <span className="inst-label">할당 메모리</span>
                <span className="inst-val num">{memGb ? `${memGb} GB` : "4 GB"}</span>
              </div>
              {modCatalog && (
                <div className="inst-row">
                  <span className="inst-label">탑재 모드</span>
                  <span className="inst-val num">{modCatalog.mods.length}개 ({modCatalog.mods.map(m => m.name).join(", ")})</span>
                </div>
              )}
              <div className="inst-row">
                <span className="inst-label">설치 상태</span>
                <span className="inst-val text-ok num">
                  {selectedInstance?.installed ? "정상 설치됨" : "설치 필요"}
                </span>
              </div>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}
