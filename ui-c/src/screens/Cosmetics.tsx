import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Badge, Empty, Segmented } from "../components/ui";
import { PlayerRender } from "../widgets/PlayerRender";
import type { Cosmetic, CosmeticSlot } from "../core/bindings";
import { useStore } from "../state/store";
import { useT } from "../i18n";

function rarityClass(r: string): string {
  return `rarity-${r}`;
}

export function CosmeticsScreen() {
  const { activeAccount, cosmetics, loadout, equip, toast } = useStore();
  const { t } = useT();
  const [slotFilter, setSlotFilter] = useState<CosmeticSlot>("cape");

  const filtered = useMemo(() => cosmetics.filter((c) => c.slot === slotFilter), [cosmetics, slotFilter]);

  const equipItem = (item: Cosmetic) => {
    const isEquipped = loadout?.[item.slot] === item.id;
    void equip(item.slot, isEquipped ? null : item.id);
    if (isEquipped) toast("info", t("cosmetics.unequippedToast", { name: item.name }));
  };

  if (!activeAccount) {
    return (
      <div className="screen-pad">
        <header className="page-head">
          <div>
            <h2>{t("cosmetics.title")}</h2>
          </div>
        </header>
        <Empty icon="sparkles" title={t("accounts.add")} hint={t("cosmetics.perAccount")} />
      </div>
    );
  }

  const slots: CosmeticSlot[] = ["cape", "wings", "trail"];

  return (
    <div className="screen-pad">
      <header className="page-head">
        <div>
          <h2>{t("cosmetics.title")}</h2>
          <p>
            <Icon name="info" size={13} style={{ verticalAlign: "-2px", marginRight: 6 }} />
            {t("cosmetics.clientSide")}
          </p>
        </div>
        <Badge tone="accent">{activeAccount.username}</Badge>
      </header>

      <div className="cos-layout">
        {/* loadout column */}
        <section className="cos-slots">
          <h3 className="cos-col-title">{t("cosmetics.loadout")}</h3>
          {slots.map((slot) => {
            const item = cosmetics.find((c) => c.id === loadout?.[slot]) ?? null;
            return (
              <button
                key={slot}
                className={`cos-slot card ${slotFilter === slot ? "active" : ""} ${item ? rarityClass(item.rarity) : ""}`}
                onClick={() => setSlotFilter(slot)}
              >
                <span className="cos-slot-icon" style={item ? { "--h": item.hue } : undefined}>
                  <Icon name={slot === "cape" ? "layers" : slot === "wings" ? "sparkles" : "zap"} size={17} />
                </span>
                <span className="cos-slot-meta">
                  <em>{t(`cosmetics.${slot}`)}</em>
                  <strong>{item ? item.name : t("cosmetics.empty")}</strong>
                </span>
                {item && (
                  <span
                    className="cos-unequip"
                    role="button"
                    tabIndex={0}
                    title={t("cosmetics.unequip")}
                    onClick={(e) => {
                      e.stopPropagation();
                      void equip(slot, null);
                    }}
                    onKeyDown={(e) => e.key === "Enter" && void equip(slot, null)}
                  >
                    <Icon name="x" size={13} />
                  </span>
                )}
              </button>
            );
          })}

          {/* preview stage */}
          <div className="cos-stage card">
            <div className="cos-stage-grid" />
            <span className="cos-stage-light" />
            <div className="cos-figure">
              <PlayerRender loadout={loadout} catalog={cosmetics} skinHue={activeAccount.skinHue} />
            </div>
            <span className="cos-stage-label mono">client-side render</span>
          </div>
        </section>

        {/* catalog */}
        <section className="cos-catalog">
          <Segmented
            options={slots.map((s) => ({ value: s, label: t(`cosmetics.${s}`) }))}
            value={slotFilter}
            onChange={setSlotFilter}
          />
          <div className="cos-grid stagger">
            {filtered.map((item) => {
              const equipped = loadout?.[item.slot] === item.id;
              return (
                <button
                  key={item.id}
                  className={`cos-item card ${rarityClass(item.rarity)} ${equipped ? "equipped gborder" : ""}`} data-glow
                  onClick={() => equipItem(item)}
                >
                  <span className="cos-swatch" style={{ "--h": item.hue }}>
                    {equipped && (
                      <span className="cos-equipped-mark">
                        <Icon name="check" size={12} strokeWidth={2.4} />
                      </span>
                    )}
                  </span>
                  <span className="cos-item-meta">
                    <strong>{item.name}</strong>
                    <span className="cos-item-desc">{item.desc}</span>
                  </span>
                  <span className="cos-item-foot">
                    <span className={`cos-rarity cos-r-${item.rarity}`}>{t(`cosmetics.rarity.${item.rarity}`)}</span>
                    {equipped && <span className="cos-equipped-label">{t("cosmetics.equipped")}</span>}
                  </span>
                </button>
              );
            })}
          </div>
        </section>
      </div>
    </div>
  );
}
