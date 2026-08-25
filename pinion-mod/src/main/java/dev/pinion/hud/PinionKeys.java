package dev.pinion.hud;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** The three client keys, and the state each of them owns.
 *
 *  Zoom holds a vanilla option at a value the player did not choose, so it
 *  remembers what was there before and puts it back — leaving the world must
 *  not leave someone stuck at 30 degrees. Fullbright is only a flag here; the
 *  light itself is {@link dev.pinion.hud.mixin.LightmapMixin}. */
public final class PinionKeys {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PinionClient.MOD_ID, "keys"));

    private static KeyMapping settings;
    private static KeyMapping zoom;
    private static KeyMapping fullbright;
    private static KeyMapping warp;
    /** UI rig one-shot: the auto-open fires exactly once per session. */
    private static boolean rigOpened;

    /** fov the player picked, held while the zoom key is down */
    private static int savedFov;
    private static double savedSensitivity;
    private static boolean zooming;

    private PinionKeys() {
    }

    public static void register() {
        settings = bind("key.pinion.settings", GLFW.GLFW_KEY_RIGHT_SHIFT);
        zoom = bind("key.pinion.zoom", GLFW.GLFW_KEY_C);
        fullbright = bind("key.pinion.fullbright", GLFW.GLFW_KEY_B);
        warp = bind("key.pinion.warp", GLFW.GLFW_KEY_K);

        ClientTickEvents.END_CLIENT_TICK.register(PinionKeys::tick);
    }

    private static KeyMapping bind(String translation, int code) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translation, InputConstants.Type.KEYSYM, code, CATEGORY));
    }

    private static void tick(Minecraft mc) {
        /* UI rig (-Dfullmoon.uiRig): the panel opens on its own at the title
           screen and the keys keep working without a world, so the screens can
           be reviewed before any server exists. */
        boolean rig = PinionHud.uiRig();
        if (rig && !rigOpened && mc.player == null
                && mc.screen instanceof net.minecraft.client.gui.screens.TitleScreen) {
            // open over the TITLE SCREEN specifically: earlier in boot the
            // screen is null or a splash, and the title assignment would
            // replace whatever we set then
            rigOpened = true;
            mc.setScreen(new PinionSettingsScreen(null));
        }
        if (mc.player == null && !rig) {
            // leaving a world drops the holds; the options are the player's again
            releaseZoom(mc);
            return;
        }

        while (settings.consumeClick()) {
            if (mc.screen == null) {
                mc.setScreen(new PinionSettingsScreen(null));
            }
        }
        while (fullbright.consumeClick()) {
            toggleFullbright();
        }
        while (warp.consumeClick()) {
            if ((mc.player != null || rig) && mc.screen == null) {
                mc.setScreen(new dev.pinion.bridge.WarpScreen(null));
            }
        }

        boolean want = zoom.isDown() && mc.screen == null && mc.player != null;
        if (want && !zooming) {
            savedFov = mc.options.fov().get();
            savedSensitivity = mc.options.sensitivity().get();
            mc.options.fov().set(ClientSettings.zoomFov());
            /* aim has to stay usable at 4x: without scaling the sensitivity the
               same wrist movement sweeps four times as far across the target */
            mc.options.sensitivity().set(savedSensitivity * ClientSettings.zoomSensitivity());
            zooming = true;
        } else if (!want && zooming) {
            releaseZoom(mc);
        }
    }

    private static void releaseZoom(Minecraft mc) {
        if (!zooming) {
            return;
        }
        mc.options.fov().set(savedFov);
        mc.options.sensitivity().set(savedSensitivity);
        zooming = false;
    }

    static void toggleFullbright() {
        ClientSettings.setFullbright(!ClientSettings.fullbright());
        PinionClient.LOG.info("fullbright {}", ClientSettings.fullbright() ? "on" : "off");
    }

    public static boolean isFullbright() {
        return ClientSettings.fullbright();
    }

    public static boolean isZooming() {
        return zooming;
    }

    /** The panel's Keys page shows what these are actually bound to, which is
     *  whatever vanilla Controls last said — not the defaults registered here. */
    static KeyMapping settingsKey() {
        return settings;
    }

    static KeyMapping zoomKey() {
        return zoom;
    }

    static KeyMapping fullbrightKey() {
        return fullbright;
    }

    static KeyMapping warpKey() {
        return warp;
    }
}
