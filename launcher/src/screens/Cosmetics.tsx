import { useMemo, useState } from "react";
import { Icon } from "../components/Icon";
import { Empty, Segmented } from "../components/ui";
import Skin3D from "../widgets/Skin3D";
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
  const [walk, setWalk] = useState(false);

  const filtered = useMemo(() => cosmetics.filter((c) => c.slot === slotFilter), [cosmetics, slotFilter]);
  const equippedCape = useMemo(
    () => cosmetics.find((c) => c.id === loadout?.cape) ?? null,
    [cosmetics, loadout],
  );

  const equipItem = (item: Cosmetic) => {
    const isEquipped = loadout?.[item.slot] === item.id;
    void equip(item.slot, isEquipped ? null : item.id);
    if (isEquipped) toast("info", t("cosmetics.unequippedToast", { name: item.name }));
  };

  if (!activeAccount) {
    return (
      <div className="screen-pad">
        <Empty icon="feather" title={t("accounts.add")} hint={t("cosmetics.perAccount")} />
      </div>
    );
  }

  const slots: CosmeticSlot[] = ["cape", "wings", "trail"];

  return (
    <div className="screen-pad">
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
                  <Icon name={slot === "cape" ? "layers" : slot === "wings" ? "feather" : "zap"} size={17} />
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

          {/* preview stage — the real skinview3d viewer, same one the HUD ships */}
          <div className="cos-stage card">
            <div className="cos-figure">
              <Skin3D
                skin={activeAccount.skinUrl ?? "/skins/blackcow.png"}
                cape={equippedCape?.capeUrl ?? null}
                walk={walk}
                width={218}
                height={280}
                zoom={0.85}
              />
            </div>
            <div className="cos-stage-anim">
              <Segmented
                options={[
                  { value: "idle", label: t("cosmetics.idle") },
                  { value: "walk", label: t("cosmetics.walk") },
                ]}
                value={walk ? "walk" : "idle"}
                onChange={(v) => setWalk(v === "walk")}
              />
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
                  className={`cos-item card ${rarityClass(item.rarity)} ${equipped ? "equipped" : ""}`}
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
