package dev.fullmoon.client.hud;

import dev.fullmoon.client.FullmoonClient;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Renders active HUD modules onto the in-game HUD layer. */
public final class HudOverlay {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(FullmoonClient.NAMESPACE, "hud_overlay");

    private HudOverlay() {}

    public static void init() {
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(ID, HudOverlay::render);
    }

    private static void render(GuiGraphicsExtractor gfx, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui || client.screen != null) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();

        Painter painter = new Painter(gfx);
        HudElementRegistry.getInstance().poll(System.currentTimeMillis());
        for (HudElement elem : HudElementRegistry.getInstance().elements()) {
            if (elem.enabled()) {
                Box bounds = elem.computeBounds(width, height, client);
                elem.draw(painter, bounds, client, false);
            }
        }
        ServerNoticeOverlay.draw(painter, System.currentTimeMillis());
    }
}
