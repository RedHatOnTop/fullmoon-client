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

/* What the mod can actually draw. A slot that reaches the game is equippable;
   the rest stay browsable and say so, because a loadout the game ignores is a
   promise the client does not keep. */
const RENDERS: Record<CosmeticSlot, boolean> = { cape: true, wings: false, trail: false };

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
    if (!RENDERS[item.slot]) {
      toast("info", t("cosmetics.notYetToast", { slot: t(`cosmetics.${item.slot}`) }));
      return;
    }
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
                  <strong className={RENDERS[slot] ? "" : "cos-slot-soon"}>
                    {RENDERS[slot] ? (item ? item.name : t("cosmetics.empty")) : t("cosmetics.notYet")}
                  </strong>
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
              {/* three-quarters from behind: a cape stage that spins the cape
                  out of view half the time is a stage that shows nothing */}
              <Skin3D
                skin={activeAccount.skinUrl ?? "/skins/blackcow.png"}
                cape={equippedCape?.capeUrl ?? null}
                walk={walk}
                rotate={false}
                angle={Math.PI * 0.86}
                width={246}
                height={344}
                zoom={0.95}
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
            <span className="cos-stage-drag mono">{t("cosmetics.dragHint")}</span>
            <span className="cos-stage-label mono">클라이언트 실시간 렌더링</span>
          </div>
        </section>

        {/* catalog */}
        <section className="cos-catalog">
          <Segmented
            options={slots.map((s) => ({ value: s, label: t(`cosmetics.${s}`) }))}
            value={slotFilter}
            onChange={setSlotFilter}
          />
          {!RENDERS[slotFilter] && (
            <div className="cos-soon">
              <Icon name="info" size={15} />
              <div>
                <strong>{t("cosmetics.notYetTitle", { slot: t(`cosmetics.${slotFilter}`) })}</strong>
                <span>{t("cosmetics.notYetHint")}</span>
              </div>
            </div>
          )}
          <div className="cos-grid stagger">
            {filtered.map((item) => {
              const equipped = loadout?.[item.slot] === item.id;
              const renders = RENDERS[item.slot];
              return (
                <button
                  key={item.id}
                  className={`cos-item card ${rarityClass(item.rarity)} ${equipped ? "equipped" : ""} ${renders ? "" : "unbuilt"}`}
                  onClick={() => equipItem(item)}
                >
                  <span className="cos-swatch" style={{ "--h": item.hue }}>
                    {item.capeUrl ? (
                      <span className="cos-cape" style={{ backgroundImage: `url(${item.capeUrl})` }} />
                    ) : (
                      <span className="cos-glyph">
                        <Icon name={item.slot === "wings" ? "feather" : "zap"} size={30} strokeWidth={1.3} />
                      </span>
                    )}
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
                    {!renders && <span className="cos-soon-label">{t("cosmetics.notYet")}</span>}
                    {equipped && renders && <span className="cos-equipped-label">{t("cosmetics.equipped")}</span>}
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
