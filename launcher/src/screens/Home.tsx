import { useMemo } from "react";
import { Icon } from "../components/Icon";
import { Badge, IconButton } from "../components/ui";
import { useStore } from "../state/store";
import { useT } from "../i18n";
import BRAND from "../brand";
import Skin3D from "../widgets/Skin3D";
import VoxelIsland from "../widgets/VoxelIsland";

/* four tags, four corners of the wheel — a warm accent puts update, dev and
   cosmetic in the same family unless dev is pushed to the cool side */
const TAG_TONE: Record<string, "accent" | "ok" | "warn" | "err" | "info" | "dim"> = {
  update: "accent",
  event: "ok",
  dev: "info",
  cosmetic: "err",
};

export function HomeScreen() {
  const {
    news, servers, versions, settings, modCatalog, cosmetics, loadout,
    activeAccount, instances, selectedInstance, launch, removeServer, toast, setScreen,
  } = useStore();
  const { t } = useT();

  const target = useMemo(() => versions.find((v) => v.isTarget), [versions]);
  const installedAny = instances.some((i) => i.installed);
  const memGb = selectedInstance ? Math.round(selectedInstance.memoryMb / 1024) : null;
  const javaLabel = settings?.javaPath?.split("\\").pop() ?? "—";

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
    <div className="home stagger">
      {/* ── flight-deck hero: the brand voice, not a news slot ── */}
      <section className="fhero card">
        <div className="fhero-copy">
          <div className="fhero-eyebrow">
            <span className="live-dot" />
            READY TO LAUNCH
            {target && <em className="num">{target.id}</em>}
          </div>
          <h2 className="fhero-title">
            {t("home.heroTitle1")}
            <br />
            <strong>{t("home.heroTitle2")}</strong>
          </h2>
          <div className="fhero-meta">
            <span>
              <Icon name="shield" size={13} />
              {t("home.metaVerified")}
            </span>
            {modCatalog && (
              <span>
                <Icon name="puzzle" size={13} />
                {t("home.metaMods", { n: modCatalog.mods.length })}
              </span>
            )}
            {memGb !== null && (
              <span>
                <Icon name="ram" size={13} />
                {t("home.metaRam", { gb: memGb })}
              </span>
            )}
          </div>
        </div>
        <div className="fhero-art" aria-hidden>
          <VoxelIsland className="fhero-island" />
        </div>
        <div className="fhero-build">
          <span>{BRAND.name.toUpperCase()} BUILD</span>
          <b className="num">1.0.0</b>
          <small className="num">{target ? `${target.id} target` : "…"}</small>
        </div>
      </section>

      {servers.length > 0 && (
        <div className="quickbar">
          <span className="quickbar-label">{t("home.quickJoin")}</span>
          {servers.slice(0, 4).map((s) => (
            <button
              key={s.id}
              className="quickbar-chip"
              disabled={!installedAny}
              onClick={() => quickPlay(s.address)}
              title={s.address}
            >
              <span className="quickbar-swatch" style={{ "--h": s.hue }} />
              {s.name}
              <span className="quickbar-ping">{s.pingMs}ms</span>
            </button>
          ))}
        </div>
      )}

      <div className="home-grid">
        {/* ── news list ── */}
        <section>
          <div className="section-head">
            <h3>{t("home.newsTitle")}</h3>
          </div>
          <div className="news-list">
            {news.map((n) => (
              <article key={n.id} className="news-row card-hover">
                <span className="news-swatch" style={{ "--h": n.hue }} />
                <div className="news-meta">
                  <div className="news-top">
                    <Badge tone={TAG_TONE[n.tag] ?? "dim"}>{n.tag.toUpperCase()}</Badge>
                    {n.featured && <Badge tone="dim">{t("home.featured")}</Badge>}
                    <span className="news-date num">{n.date}</span>
                  </div>
                  <h4>{n.title}</h4>
                  <p>{n.summary}</p>
                </div>
                <Icon name="arrowRight" size={15} className="news-arrow" />
              </article>
            ))}
          </div>
        </section>

        {/* ── right rail: the player, then the machine ── */}
        <aside className="home-rail">
          <section className="player-card card">
            <div className="player-stage">
              <Skin3D
                skin={activeAccount?.skinUrl ?? "/skins/blackcow.png"}
                cape={cape?.capeUrl ?? null}
                width={276}
                height={300}
                zoom={0.92}
              />
            </div>
            <div className="player-meta">
              <div className="player-name">
                <strong>{activeAccount?.username ?? "—"}</strong>
                <span>{cape ? t("home.capeOn", { name: cape.name }) : t("home.noCape")}</span>
              </div>
              <IconButton icon="feather" label={t("home.changeLook")} onClick={() => setScreen("cosmetics")} />
            </div>
          </section>

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
            <article key={s.id} className="server-card card-hover">
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
