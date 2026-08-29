import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, IconButton } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";

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

export function DashboardScreen() {
  const {
    news, wallet, walletTxs, servers, serverStatus, pingingServers, refreshServers, addServer,
    removeServer, instances, selectedInstance, launch, toast, setScreen,
  } = useStore();
  const { t } = useT();
  const [draft, setDraft] = useState({ name: "", address: "" });
  const [activeTab, setActiveTab] = useState<"wallet" | "servers" | "news">("wallet");

  const installedAny = instances.some((i) => i.installed);

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

  return (
    <div className="dashboard-screen stagger">
      {/* ── 1. Top Category Tabs ── */}
      <div className="game-section-nav">
        <div className="game-nav-buttons">
          <button
            className={`game-nav-tab ${activeTab === "wallet" ? "active" : ""}`}
            onClick={() => setActiveTab("wallet")}
          >
            <Icon name="star" size={16} />
            <span>재화 통계 및 거래 내역</span>
            <span className="tab-badge">{wallet ? `${wallet.balance.toLocaleString("ko-KR")}원` : "모니터링"}</span>
          </button>

          <button
            className={`game-nav-tab ${activeTab === "servers" ? "active" : ""}`}
            onClick={() => setActiveTab("servers")}
          >
            <Icon name="globe" size={16} />
            <span>서버 관리 및 네트워크</span>
            <span className="tab-count num">{servers.length}</span>
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
          {activeTab === "servers" && (
            <button
              className="game-refresh-btn"
              onClick={() => void refreshServers()}
              disabled={pingingServers}
            >
              <Icon name="refresh" size={14} className={pingingServers ? "spin" : undefined} />
              <span>서버 핑 새로고침</span>
            </button>
          )}
        </div>
      </div>

      {/* ── 2. Content Body ── */}
      <div className="dashboard-main-content">
        {/* TAB 1: Real Wallet Statistics & Transaction Ledger */}
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

        {/* TAB 2: Real Servers Management */}
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
    </div>
  );
}
