import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Button, Empty, IconButton } from "../components/ui";
import { useStore } from "../state/store";
import { openExternal } from "../core/client";
import { useT } from "../i18n";
import Skin2D from "../widgets/Skin2D";

const TAG_TONE: Record<string, "accent" | "ok" | "info" | "err" | "dim"> = {
  update: "accent",
  event: "ok",
  dev: "info",
  cosmetic: "err",
};

type Tab = "servers" | "wallet" | "news";

/* "recent" is a promise: the stats and the ledger read the same window, and
   the count line says so when the backend's history is longer than it */
const RECENT_TX = 30;

function fmtWhen(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

/* ping read as a word the palette already owns */
function pingClass(ms: number): string {
  if (ms < 50) return "ping-good";
  if (ms < 120) return "ping-mid";
  return "ping-bad";
}

export function HomeScreen() {
  const {
    news, wallet, walletTxs, servers, serverStatus, pingingServers, refreshServers, addServer,
    versions, modCatalog, activeAccount, instances, selectedInstance, launch, removeServer,
    toast, setScreen, cosmetics, loadout,
  } = useStore();
  const { t } = useT();
  const [tab, setTab] = useState<Tab>("servers");
  const [draft, setDraft] = useState({ name: "", address: "" });

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);
  const memGb = selectedInstance ? Math.round(selectedInstance.memoryMb / 1024) : null;

  const cape = useMemo(() => {
    const id = loadout?.cape;
    return id ? cosmetics.find((c) => c.id === id) ?? null : null;
  }, [loadout, cosmetics]);

  const onlineCount = servers.filter((s) => serverStatus[s.address]?.online === true).length;
  const playersNow = servers.reduce(
    (sum, s) => sum + (serverStatus[s.address]?.online ? serverStatus[s.address].players : 0),
    0,
  );

  /* the "recent window" promise is enforced locally, not trusted from the
     core: the bindings contract does not pin transaction order, so sort by
     timestamp before slicing — ISO-8601 UTC compares lexicographically */
  const recentTxs = useMemo(
    () =>
      [...walletTxs]
        .sort((a, b) => (a.at < b.at ? 1 : a.at > b.at ? -1 : 0))
        .slice(0, RECENT_TX),
    [walletTxs],
  );

  const walletStats = useMemo(() => {
    let income = 0;
    let expense = 0;
    for (const tx of recentTxs) {
      if (tx.delta >= 0) income += tx.delta;
      else expense += Math.abs(tx.delta);
    }
    return { income, expense };
  }, [recentTxs]);

  const txCountLabel =
    walletTxs.length > RECENT_TX
      ? t("home.txWindow", { n: RECENT_TX, total: walletTxs.length })
      : t("home.txCount", { n: walletTxs.length });

  const quickPlay = (address: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  const TABS: Array<{ id: Tab; label: string; count: number | null }> = [
    { id: "servers", label: t("home.tabServers"), count: servers.length },
    { id: "wallet", label: t("home.tabWallet"), count: walletTxs.length },
    { id: "news", label: t("home.tabNews"), count: news.length },
  ];

  return (
    <div className="dash stagger">
      {/* the instrument band: is anything up, in one row of numbers */}
      <section className="dash-status card">
        <div className="dash-stat">
          <div className="dash-stat-label">{t("home.statNetwork")}</div>
          <div className="dash-stat-val">{onlineCount}<small>/ {servers.length}</small></div>
          <div className="dash-stat-sub">{onlineCount > 0 ? t("home.statWorldsOn") : t("home.statAllOff")}</div>
        </div>
        <div className="dash-stat">
          <div className="dash-stat-label">{t("home.statPlayers")}</div>
          <div className="dash-stat-val">{playersNow}</div>
          <div className="dash-stat-sub">{t("home.statPlayersSub")}</div>
        </div>
        <div className="dash-stat">
          <div className="dash-stat-label">{t("home.statBalance")}</div>
          <div className="dash-stat-val">
            {wallet ? wallet.balance.toLocaleString("ko-KR") : "0"}
            <small>{wallet?.currency ?? t("home.walletUnit")}</small>
          </div>
          <div className="dash-stat-sub">
            {wallet ? t("home.statUpdated", { when: fmtWhen(wallet.updatedAt) }) : t("home.offline")}
          </div>
        </div>
        <div className="dash-stat">
          <div className="dash-stat-label">{t("home.statInstance")}</div>
          <div className="dash-stat-val">{target ? target.id : "26.1.2"}</div>
          <div className="dash-stat-sub">
            {selectedInstance?.installed ? t("home.statInstalled") : t("home.statNotInstalled")}
            {memGb !== null && ` · ${memGb} GB`}
          </div>
        </div>
      </section>

      {/* the tab rail: text on a hairline, the active one underlined */}
      <nav className="dash-tabs">
        {TABS.map((x) => (
          <button
            key={x.id}
            className={`dash-tab ${tab === x.id ? "active" : ""}`}
            onClick={() => setTab(x.id)}
          >
            {x.label}
            {x.count !== null && <em className="num">{x.count}</em>}
          </button>
        ))}
        <div className="dash-tabs-right">
          <button className="rail-link" onClick={() => void refreshServers()} disabled={pingingServers}>
            <Icon name="refresh" size={12} className={pingingServers ? "spin" : undefined} />
            <span>{t("home.refresh")}</span>
          </button>
        </div>
      </nav>

      <div className="dash-body">
        <div className="dash-main">
          {/* servers */}
          {tab === "servers" && (
            <div>
              {servers.length === 0 ? (
                <Empty icon="globe" title={t("home.emptyServers")} hint={t("home.emptyServersHint")} />
              ) : (
                <div className="realm-grid">
                  {servers.map((s) => {
                    const st = serverStatus[s.address];
                    const online = st?.online === true;
                    const cap = st?.maxPlayers ?? s.maxPlayers;
                    const curPlayers = st ? st.players : s.players;
                    const curPing = st ? st.pingMs : s.pingMs;
                    return (
                      <article key={s.id} className="realm-card card">
                        <div className="realm-head">
                          <span className="realm-addr">{s.address}</span>
                          <div className="realm-head-right">
                            <Badge tone={online ? "ok" : "dim"}>
                              {online ? t("home.serverOnline") : st ? t("home.serverOffline") : t("home.serverChecking")}
                            </Badge>
                            <IconButton
                              icon="x"
                              label={t("home.removeServer")}
                              onClick={() => void removeServer(s.id)}
                            />
                          </div>
                        </div>
                        <h2 className="realm-title">{s.name}</h2>
                        <p className="realm-motd">{st?.motd || s.motd || s.address}</p>
                        <div className="realm-foot">
                          <div className="realm-metrics">
                            <span>
                              <Icon name="users" size={13} />
                              <b>{curPlayers}</b>&thinsp;/&thinsp;{cap}
                            </span>
                            <span className={online ? pingClass(curPing) : undefined}>
                              <Icon name="signal" size={12} />
                              {online ? `${curPing} ms` : "—"}
                            </span>
                          </div>
                          <Button
                            size="sm"
                            variant="soft"
                            icon="arrowRight"
                            disabled={!installedAny || (st ? !online : false)}
                            onClick={() => quickPlay(s.address)}
                          >
                            {t("home.join")}
                          </Button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}

              <form
                className="realm-add"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (!draft.address.trim()) return;
                  void addServer(draft.name || draft.address, draft.address);
                  setDraft({ name: "", address: "" });
                }}
              >
                <input
                  className="input"
                  value={draft.name}
                  onChange={(e) => setDraft({ ...draft, name: e.target.value })}
                  placeholder={t("home.addNamePh")}
                  spellCheck={false}
                />
                <input
                  className="input mono"
                  value={draft.address}
                  onChange={(e) => setDraft({ ...draft, address: e.target.value })}
                  placeholder={t("home.addAddrPh")}
                  spellCheck={false}
                />
                <Button variant="soft" icon="plus" type="submit" disabled={!draft.address.trim()}>
                  {t("home.addSubmit")}
                </Button>
              </form>
            </div>
          )}

          {/* wallet */}
          {tab === "wallet" && (
            <div className="wallet-panel card">
              <div className="wallet-stats">
                <div className="wallet-stat">
                  <div className="dash-stat-label">{t("home.walletBalance")}</div>
                  <div className="wallet-stat-hero">
                    {wallet ? wallet.balance.toLocaleString("ko-KR") : "0"}
                    <small>{wallet?.currency ?? t("home.walletUnit")}</small>
                  </div>
                  <div className="wallet-stat-sub">
                    {wallet ? t("home.statUpdated", { when: fmtWhen(wallet.updatedAt) }) : t("home.offline")}
                  </div>
                </div>
                <div className="wallet-stat">
                  <div className="dash-stat-label">{t("home.walletIncome")}</div>
                  <div className="wallet-stat-val text-ok">
                    +{walletStats.income.toLocaleString("ko-KR")}
                    <small>{wallet?.currency ?? t("home.walletUnit")}</small>
                  </div>
                  <div className="wallet-stat-sub">{t("home.walletIncomeSub")}</div>
                </div>
                <div className="wallet-stat">
                  <div className="dash-stat-label">{t("home.walletExpense")}</div>
                  <div className="wallet-stat-val text-danger">
                    −{walletStats.expense.toLocaleString("ko-KR")}
                    <small>{wallet?.currency ?? t("home.walletUnit")}</small>
                  </div>
                  <div className="wallet-stat-sub">{t("home.walletExpenseSub")}</div>
                </div>
              </div>

              <div className="tx-head">
                <h2>{t("home.txTitle")}</h2>
                <span className="tx-count num">{txCountLabel}</span>
              </div>
              {recentTxs.length === 0 ? (
                <Empty icon="star" title={t("home.txEmpty")} hint={t("home.txEmptyHint")} />
              ) : (
                <div className="tx-list">
                  {recentTxs.map((tx, idx) => (
                    <div key={tx.at + tx.reason + idx} className="tx-row">
                      <span className={`tx-sign ${tx.delta >= 0 ? "in" : "out"}`}>
                        {tx.delta >= 0 ? "+" : "−"}
                      </span>
                      <div className="tx-detail">
                        <strong>{tx.label}</strong>
                        <span>{tx.reason}</span>
                      </div>
                      <span className="tx-time">{fmtWhen(tx.at)}</span>
                      <div className="tx-amount">
                        <b className={tx.delta >= 0 ? "text-ok" : "text-danger"}>
                          {tx.delta >= 0 ? "+" : "−"}{Math.abs(tx.delta).toLocaleString("ko-KR")} {wallet?.currency ?? t("home.walletUnit")}
                        </b>
                        {tx.balanceAfter !== null && (
                          <span>{tx.balanceAfter.toLocaleString("ko-KR")} {wallet?.currency ?? t("home.walletUnit")}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* news — a ledger, not tiles; rows read, they don't pretend to open */}
          {tab === "news" && (
            news.length === 0 ? (
              <Empty icon="bell" title={t("home.newsEmpty")} hint={t("home.newsEmptyHint")} />
            ) : (
              <div className="news-rows">
                {news.map((n) => {
                  /* bind the url once — the click handler's closure needs a
                     narrowed binding, not a property re-read */
                  const url = n.url;
                  /* a feed item carries its source post — the row opens it.
                     Bundled items stay plain text; a row that points nowhere
                     is a row that lies. */
                  return url ? (
                    <button
                      key={n.id}
                      className="news-row"
                      title={url}
                      onClick={() => void openExternal(url)}
                    >
                      <span className="news-swatch" style={{ "--h": n.hue }} />
                      <div>
                        <div className="news-row-top">
                          <Badge tone={TAG_TONE[n.tag] ?? "dim"}>{n.tag.toUpperCase()}</Badge>
                          {n.featured && <Badge tone="accent">{t("home.featured")}</Badge>}
                          <span className="news-src">DISCORD</span>
                          <span className="news-date num">{n.date}</span>
                        </div>
                        <h2 className="news-title">{n.title}</h2>
                        <p className="news-summary">{n.summary}</p>
                      </div>
                      <span className="news-arrow">
                        <Icon name="external" size={15} />
                      </span>
                    </button>
                  ) : (
                    <article key={n.id} className="news-row">
                      <span className="news-swatch" style={{ "--h": n.hue }} />
                      <div>
                        <div className="news-row-top">
                          <Badge tone={TAG_TONE[n.tag] ?? "dim"}>{n.tag.toUpperCase()}</Badge>
                          {n.featured && <Badge tone="accent">{t("home.featured")}</Badge>}
                          <span className="news-date num">{n.date}</span>
                        </div>
                        <h2 className="news-title">{n.title}</h2>
                        <p className="news-summary">{n.summary}</p>
                      </div>
                    </article>
                  );
                })}
              </div>
            )
          )}
        </div>

        {/* the rail: the player, the purse, the install */}
        <aside className="dash-rail">
          <section className="rail-card card">
            <div className="rail-title">
              <span className="dash-stat-label">{t("home.playerTitle")}</span>
              <button className="rail-link" onClick={() => setScreen("cosmetics")}>
                <span>{t("home.changeLook")}</span>
                <Icon name="arrowRight" size={12} />
              </button>
            </div>
            <div className="rail-player-stage">
              <Skin2D
                skin={activeAccount?.skinUrl ?? "/skins/blackcow.png"}
                cape={cape?.capeUrl ?? null}
                view="front"
                scale={7}
                label={activeAccount?.username}
              />
            </div>
            <div className="rail-player-meta">
              <div className="rail-player-name">
                <strong>{activeAccount?.username ?? t("dock.needsAccount")}</strong>
                <span>{activeAccount ? t(`accounts.source.${activeAccount.source}`) : t("home.offline")}</span>
              </div>
              <Badge tone={cape ? "accent" : "dim"}>{cape ? cape.name : t("home.noCape")}</Badge>
            </div>
          </section>

          <section className="rail-card card">
            <div className="rail-title">
              <span className="dash-stat-label">{t("home.walletTitle")}</span>
            </div>
            <div className="rail-balance">
              {wallet ? wallet.balance.toLocaleString("ko-KR") : "0"}
              <small>{wallet?.currency ?? t("home.walletUnit")}</small>
            </div>
            <div className="rail-actions">
              <Button size="sm" variant="outline" onClick={() => setTab("wallet")}>
                {t("home.walletStats")}
              </Button>
              <Button size="sm" variant="outline" onClick={() => setScreen("cosmetics")}>
                {t("home.walletShop")}
              </Button>
            </div>
          </section>

          <section className="rail-card card">
            <div className="rail-title">
              <span className="dash-stat-label">{t("home.instTitle")}</span>
              <button className="rail-link" onClick={() => setScreen("settings")}>
                <span>{t("home.instManage")}</span>
                <Icon name="arrowRight" size={12} />
              </button>
            </div>
            <dl className="rail-facts">
              <div className="rail-fact">
                <dt>{t("home.instVersion")}</dt>
                <dd>{target ? target.id : "26.1.2"} ({selectedInstance?.loader ?? "fabric"})</dd>
              </div>
              <div className="rail-fact">
                <dt>{t("home.instMemory")}</dt>
                <dd>{memGb ? `${memGb} GB` : "—"}</dd>
              </div>
              {modCatalog && (
                <div className="rail-fact">
                  <dt>{t("home.instMods")}</dt>
                  <dd>{t("home.instModsFmt", { n: modCatalog.mods.length })}</dd>
                </div>
              )}
              <div className="rail-fact">
                <dt>{t("home.instState")}</dt>
                <dd className={selectedInstance?.installed ? "text-ok" : undefined}>
                  {selectedInstance?.installed ? t("home.statInstalled") : t("home.statNotInstalled")}
                </dd>
              </div>
            </dl>
          </section>
        </aside>
      </div>
    </div>
  );
}
