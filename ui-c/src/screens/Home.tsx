import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, IconButton } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";

const TAG_TONE: Record<string, "accent" | "ok" | "warn" | "err" | "dim"> = {
  update: "accent",
  event: "ok",
  dev: "warn",
  cosmetic: "err",
};

export function HomeScreen() {
  const { news, servers, versions, settings, accounts, instances, selectedInstance, launch, removeServer, toast } = useStore();
  const { t } = useT();
  const [featIdx, setFeatIdx] = useState(0);

  const featured = news[featIdx % Math.max(1, news.length)];
  const rest = news.filter((n) => n.id !== featured?.id);
  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);

  const quickPlay = (address: string) => {
    const inst =
      (selectedInstance?.installed ? selectedInstance : null) ?? instances.find((i) => i.installed);
    if (!inst) {
      toast("error", t("toast.launchFail", { reason: "no installed instance" }));
      return;
    }
    void launch(inst.id, address);
  };

  const javaLabel = settings?.javaPath?.split("\\").pop() ?? "—";

  return (
    <div className="home stagger">
      {/* ── featured news hero ── */}
      {featured && (
        <section className="hero card" key={featured.id}>
          <div className="hero-bg" style={{ "--h": featured.hue }}>
            <span className="hero-index num">{String((featIdx % news.length) + 1).padStart(2, "0")}</span>
            <span className="hero-ring r1" />
            <span className="hero-ring r2" />
            <span className="hero-beam" />
            <span className="hero-dot d1" />
            <span className="hero-dot d2" />
            <span className="hero-dot d3" />
          </div>
          <div className="hero-content">
            <div className="hero-tags">
              <Badge tone={TAG_TONE[featured.tag] ?? "dim"}>{featured.tag.toUpperCase()}</Badge>
              <span className="hero-date">{featured.date}</span>
            </div>
            <h2 className="hero-title">{featured.title}</h2>
            <p className="hero-summary">{featured.summary}</p>
            <div className="hero-nav">
              <IconButton
                icon="chevronLeft"
                label="prev"
                onClick={() => setFeatIdx((i) => (i - 1 + news.length) % news.length)}
              />
              <span className="hero-count num">
                {(featIdx % news.length) + 1} / {news.length}
              </span>
              <IconButton icon="chevronRight" label="next" onClick={() => setFeatIdx((i) => (i + 1) % news.length)} />
            </div>
          </div>
        </section>
      )}

      <div className="home-grid">
        {/* ── news list ── */}
        <section>
          <div className="section-head">
            <h3>{t("home.newsTitle")}</h3>
          </div>
          <div className="news-list">
            {rest.map((n) => (
              <article key={n.id} className="news-row card-hover" data-glow>
                <span className="news-swatch" style={{ "--h": n.hue }} />
                <div className="news-meta">
                  <div className="news-top">
                    <Badge tone={TAG_TONE[n.tag] ?? "dim"}>{n.tag.toUpperCase()}</Badge>
                    <span className="news-date">{n.date}</span>
                  </div>
                  <h4>{n.title}</h4>
                  <p>{n.summary}</p>
                </div>
                <Icon name="arrowRight" size={15} className="news-arrow" />
              </article>
            ))}
          </div>
        </section>

        {/* ── right rail ── */}
        <aside className="home-rail">
          <section className="status-card card">
            <div className="section-head">
              <h3>{t("home.statusTitle")}</h3>
              <span className="live-dot" />
            </div>
            <ul className="status-rows">
              <li>
                <span>{t("home.coreOk")}</span>
                <Badge tone="ok">OK · {t("home.coreMock")}</Badge>
              </li>
              <li>
                <span>{t("home.targetMc")}</span>
                <strong className="num">{target?.id ?? "…"}</strong>
              </li>
              <li>
                <span>{t("home.javaLabel")}</span>
                <strong className="mono status-java">{javaLabel}</strong>
              </li>
              <li>
                <span>{t("home.accountsLabel")}</span>
                <strong className="num">{accounts.length}</strong>
              </li>
            </ul>
          </section>
        </aside>
      </div>

      {/* ── favorite servers ── */}
      <section>
        <div className="section-head">
          <h3>{t("home.serversTitle")}</h3>
          <span className="section-sub num">{servers.length}</span>
        </div>
        <div className="server-grid">
          {servers.map((s) => (
            <article key={s.id} className="server-card card-hover" data-glow>
              <div className="server-head">
                <span className="server-orb" style={{ "--h": s.hue }}>
                  <Icon name="server" size={15} />
                </span>
                <div className="server-title">
                  <strong>{s.name}</strong>
                  <span className="mono">{s.address}</span>
                </div>
                <IconButton
                  icon="x"
                  label={t("home.removeServer")}
                  onClick={() => void removeServer(s.id)}
                />
              </div>
              <p className="server-motd">{s.motd}</p>
              <div className="server-stats">
                <div className="server-players">
                  <div className="pbar pbar-sm server-pbar">
                    <span className="pbar-fill" style={{ width: `${(s.players / s.maxPlayers) * 100}%` }} />
                  </div>
                  <span className="num">{s.players.toLocaleString()}/{s.maxPlayers.toLocaleString()}</span>
                </div>
                <span className={`server-ping num ${s.pingMs < 40 ? "ping-good" : s.pingMs < 80 ? "ping-mid" : "ping-bad"}`}>
                  <Icon name="signal" size={12} />
                  {s.pingMs}ms
                </span>
              </div>
              <button
                className="server-join"
                disabled={!installedAny}
                onClick={() => quickPlay(s.address)}
              >
                <Icon name="zap" size={14} />
                <span>{t("home.join")}</span>
              </button>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
