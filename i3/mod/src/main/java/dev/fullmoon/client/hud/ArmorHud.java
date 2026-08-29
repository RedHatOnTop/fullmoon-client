package dev.fullmoon.client.hud;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;

/** Equipped armor durability and status readout. */
public final class ArmorHud extends BaseHudElement {
    private static final int SLOT_COUNT = 4;
    private static final int SLOT_W = 32;
    private static final int GAP = Tokens.Space.TIGHT;
    private static final int TOTAL_W = SLOT_COUNT * SLOT_W + (SLOT_COUNT - 1) * GAP;

    private static final String[] LABELS = { "HLM", "CHS", "LEG", "BOT" };
    private static final String[] DEMO_VALS = { "98%", "84%", "100%", "72%" };

    public ArmorHud() {
        super("armor", "방어구 상태", "플레이어", false, Anchor.BOTTOM_LEFT, 12, 16);
    }

    @Override
    public int measureWidth(Minecraft client) {
        return TOTAL_W;
    }

    @Override
    public int measureHeight(Minecraft client) {
        return CHIP_HEIGHT;
    }

    @Override
    public void draw(Painter painter, Box bounds, Minecraft client, boolean isEditor) {
        int x = bounds.x();
        int y = bounds.y();

        for (int i = 0; i < SLOT_COUNT; i++) {
            int slotX = x + i * (SLOT_W + GAP);
            Box slotBox = new Box(slotX, y, SLOT_W, bounds.h());

            painter.fill(slotBox.x(), slotBox.y(), slotBox.w(), slotBox.h(),
                Tokens.Radius.SM, Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.76f));
            painter.border(slotBox.x(), slotBox.y(), slotBox.w(), slotBox.h(),
                Tokens.Radius.SM, Tokens.Stroke.HAIR, Tokens.Color.LINE_HAIRLINE);

            String val = DEMO_VALS[i];
            int textW = Typeset.width(Tokens.Type.BODY_STRONG, val);
            int textX = slotBox.x() + (slotBox.w() - textW) / 2;
            int textY = slotBox.y() + PADDING_V;

            Typeset.tabular(painter, Tokens.Type.BODY_STRONG, val, textX, textY, Tokens.Color.INK_PRIMARY);
        }
    }
}
