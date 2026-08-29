package dev.fullmoon.client;

import java.util.List;

import dev.fullmoon.client.settings.SettingsScreen;
import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.DevScreen;

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

    /**
     * A key and the page it opens. One binding per page, in the rail's own order, so a key that
     * reached a page the rail could not is a key that would have to be added here to exist.
     */
    private record Binding(KeyMapping mapping, DevScreen.Page page) {}

    private static final List<Binding> BINDINGS = List.of(
        new Binding(key("specimen", InputConstants.KEY_F6), DevScreen.Page.SPECIMEN),
        new Binding(key("kit", InputConstants.KEY_F7), DevScreen.Page.KIT),
        new Binding(key("list", InputConstants.KEY_F8), DevScreen.Page.LIST));

    private static final KeyMapping SETTINGS = key("settings", InputConstants.KEY_F9);

    @Override
    public void onInitializeClient() {
        for (Binding binding : BINDINGS) {
            KeyMappingHelper.registerKeyMapping(binding.mapping());
        }
        KeyMappingHelper.registerKeyMapping(SETTINGS);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SETTINGS.consumeClick()) {
                client.setScreen(new SettingsScreen(client.screen));
            }
            for (Binding binding : BINDINGS) {
                while (binding.mapping().consumeClick()) {
                    client.setScreen(DevScreen.open(binding.page()));
                }
            }
        });

        // The game only drains KeyMapping clicks while no screen is open, so a binding that
        // opens a surface would be dead on the title screen and in every menu — exactly where
        // a client's own screens are reached from. These are the same bindings, read directly.
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
            ScreenKeyboardEvents.afterKeyPress(screen).register((current, key) -> {
                if (bound(SETTINGS, key)) {
                    if (current instanceof SettingsScreen settings) {
                        settings.onClose();
                    } else {
                        client.setScreen(new SettingsScreen(current));
                    }
                    return;
                }
                for (Binding binding : BINDINGS) {
                    // Re-opening a page on top of itself throws its state away, and on this route
                    // the binding fires while that very page holds the keyboard.
                    boolean up = current instanceof DevScreen dev && dev.page() == binding.page();
                    if (!up && bound(binding.mapping(), key)) {
                        client.setScreen(DevScreen.open(binding.page()));
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

    /** The key a page is bound to, so a surface can name the binding that reaches it. */
    public static String pageKey(DevScreen.Page page) {
        for (Binding binding : BINDINGS) {
            if (binding.page() == page) {
                return KeyMappingHelper.getBoundKeyOf(binding.mapping()).getDisplayName().getString();
            }
        }
        // Unreachable while every page has a binding, which is what the loop above is checking.
        return "";
    }
}
