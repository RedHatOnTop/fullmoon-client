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

    /** vanilla's own floor; anything under it the option rejects outright,
     *  logs "Illegal option value" and snaps back to the player's setting */
    private static final int ZOOM_FOV = 30;

    /** fov the player picked, held while the zoom key is down */
    private static int savedFov;
    private static double savedSensitivity;
    private static boolean zooming;

    private static boolean bright;

    private PinionKeys() {
    }

    public static void register() {
        settings = bind("key.pinion.settings", GLFW.GLFW_KEY_RIGHT_SHIFT);
        zoom = bind("key.pinion.zoom", GLFW.GLFW_KEY_C);
        fullbright = bind("key.pinion.fullbright", GLFW.GLFW_KEY_B);

        ClientTickEvents.END_CLIENT_TICK.register(PinionKeys::tick);
    }

    private static KeyMapping bind(String translation, int code) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translation, InputConstants.Type.KEYSYM, code, CATEGORY));
    }

    private static void tick(Minecraft mc) {
        if (mc.player == null) {
            // leaving a world drops the holds; the options are the player's again
            releaseZoom(mc);
            return;
        }

        while (settings.consumeClick()) {
            mc.setScreen(new PinionSettingsScreen(null));
        }
        while (fullbright.consumeClick()) {
            toggleFullbright();
        }

        boolean want = zoom.isDown() && mc.screen == null;
        if (want && !zooming) {
            savedFov = mc.options.fov().get();
            savedSensitivity = mc.options.sensitivity().get();
            mc.options.fov().set(ZOOM_FOV);
            /* aim has to stay usable at 4x: without scaling the sensitivity the
               same wrist movement sweeps four times as far across the target */
            mc.options.sensitivity().set(savedSensitivity * 0.35);
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

    private static void toggleFullbright() {
        bright = !bright;
        PinionClient.LOG.info("fullbright {}", bright ? "on" : "off");
    }

    public static boolean isFullbright() {
        return bright;
    }

    public static boolean isZooming() {
        return zooming;
    }
}
