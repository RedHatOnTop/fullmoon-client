package dev.fullmoon.client;

import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.SpecimenScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * Client entrypoint. No mixins: every hook here is a public Fabric API surface, and the one
 * place this client needs something vanilla keeps to itself — the GUI render state — is opened
 * by a single access-widener line instead of an injected method.
 */
public final class FullmoonClient implements ClientModInitializer {
    public static final String NAMESPACE = "fullmoon";

    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(Identifier.fromNamespaceAndPath(NAMESPACE, "client"));

    private static final KeyMapping OPEN_SPECIMEN =
        new KeyMapping("key.fullmoon.specimen", InputConstants.Type.KEYSYM, InputConstants.KEY_F6, CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_SPECIMEN);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SPECIMEN.consumeClick()) {
                client.setScreen(new SpecimenScreen());
            }
        });

        // The game only drains KeyMapping clicks while no screen is open, so a binding that
        // opens a surface would be dead on the title screen and in every menu — exactly where
        // a client's own screens are reached from. This is the same binding, read directly.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key) -> {
                if (!(current instanceof SpecimenScreen) && bound(OPEN_SPECIMEN, key)) {
                    client.setScreen(new SpecimenScreen());
                }
            }));

        // Font metrics are memoised, and a resource pack can replace a provider under us. The
        // ordering against FONTS is what makes this run after the atlases are rebuilt, not
        // before, when it would re-measure the glyphs it just threw away.
        Identifier typeset = Identifier.fromNamespaceAndPath(NAMESPACE, "typeset");
        ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        loader.registerReloadListener(typeset,
            (ResourceManagerReloadListener) manager -> Typeset.invalidate());
        loader.addListenerOrdering(ResourceReloaderKeys.Client.FONTS, typeset);
    }

    private static boolean bound(KeyMapping mapping, KeyEvent event) {
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == event.key();
    }

    /** The key the specimen is bound to, so a surface can name its own binding. */
    public static String specimenKey() {
        return KeyMappingHelper.getBoundKeyOf(OPEN_SPECIMEN).getDisplayName().getString();
    }
}
