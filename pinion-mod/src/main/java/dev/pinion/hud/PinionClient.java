package dev.pinion.hud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The in-game half of Pinion. The launcher writes the layout, this reads it. */
public final class PinionClient implements ClientModInitializer {
    public static final String MOD_ID = "pinion";
    public static final Logger LOG = LoggerFactory.getLogger("Pinion");

    public static final ClickCounter LEFT_CLICKS = new ClickCounter();
    public static final ClickCounter RIGHT_CLICKS = new ClickCounter();

    @Override
    public void onInitializeClient() {
        // last in the stack: the readouts sit above the vanilla bars, below chat input
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                PinionHud::render);
        /* Our potion module says the same thing as vanilla's icon strip, in the
           corner the player chose, with the time left on it. Two of them on
           screen is not two readouts, it is one readout and a duplicate — so
           the vanilla one steps aside while ours is on. Wrapping the element
           rather than mixing into Gui keeps this alive across a rebase. */
        HudElementRegistry.replaceElement(
                VanillaHudElements.MOB_EFFECTS,
                vanilla -> (gfx, delta) -> {
                    if (!HudSettings.get("potion").enabled()) {
                        vanilla.extractRenderState(gfx, delta);
                    }
                });

        PinionKeys.register();
        dev.pinion.bridge.FullmoonBridge.register();
        dev.pinion.bridge.CasinoResultCard.registerScreenLayer();
        /* UI rig: with -Dfullmoon.uiRig the panel opens over the title screen
           (PinionKeys.tick does the opening — CLIENT_STARTED is too early, the
           title screen replaces whatever is set then) and the HUD draws on
           preview values. No world, no server. */
        if (Boolean.getBoolean("fullmoon.uiRig")) {
            LOG.info("UI rig active — HUD preview on, panel opens at title");
        }
        LOG.info("Pinion HUD ready");
    }
}
