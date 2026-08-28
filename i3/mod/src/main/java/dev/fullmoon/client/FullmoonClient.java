package dev.fullmoon.client;

import java.util.List;
import java.util.function.Supplier;

import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.KitScreen;
import dev.fullmoon.client.ui.SpecimenScreen;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
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

    private static final KeyMapping OPEN_SPECIMEN = key("specimen", InputConstants.KEY_F6);
    private static final KeyMapping OPEN_KIT = key("kit", InputConstants.KEY_F7);

    /**
     * A key, what it opens, and how to tell the surface is already up. The class is carried
     * separately from the supplier because re-opening a screen on top of itself throws its state
     * away, and on this route the binding fires while that screen has the keyboard.
     */
    private record Binding(KeyMapping mapping, Class<? extends Screen> screen, Supplier<Screen> open) {}

    private static final List<Binding> BINDINGS = List.of(
        new Binding(OPEN_SPECIMEN, SpecimenScreen.class, SpecimenScreen::new),
        new Binding(OPEN_KIT, KitScreen.class, KitScreen::new));

    @Override
    public void onInitializeClient() {
        for (Binding binding : BINDINGS) {
            KeyMappingHelper.registerKeyMapping(binding.mapping());
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (Binding binding : BINDINGS) {
                while (binding.mapping().consumeClick()) {
                    client.setScreen(binding.open().get());
                }
            }
        });

        // The game only drains KeyMapping clicks while no screen is open, so a binding that
        // opens a surface would be dead on the title screen and in every menu — exactly where
        // a client's own screens are reached from. These are the same bindings, read directly.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key) -> {
                for (Binding binding : BINDINGS) {
                    if (!binding.screen().isInstance(current) && bound(binding.mapping(), key)) {
                        client.setScreen(binding.open().get());
                        return;
                    }
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

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.fullmoon." + name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }

    private static boolean bound(KeyMapping mapping, KeyEvent event) {
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        return key.getType() == InputConstants.Type.KEYSYM && key.getValue() == event.key();
    }

    /** The key the specimen is bound to, so a surface can name its own binding. */
    public static String specimenKey() {
        return bindingName(OPEN_SPECIMEN);
    }

    /** The key the widget kit is bound to. */
    public static String kitKey() {
        return bindingName(OPEN_KIT);
    }

    private static String bindingName(KeyMapping mapping) {
        return KeyMappingHelper.getBoundKeyOf(mapping).getDisplayName().getString();
    }
}
