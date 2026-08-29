package dev.fullmoon.client.ui;

import net.minecraft.client.Minecraft;

/**
 * Where a control's cut, copy and paste go.
 *
 * <p>One interface for one implementation, because {@link #game} needs a running Minecraft: a
 * caret that reached for the game's clipboard directly could only be tested by starting the game,
 * and the arithmetic of what a paste does to a selection is the part worth testing.
 */
public interface Clipboard {
    String get();

    void put(String text);

    /** The game's clipboard, which is the system one. */
    static Clipboard game() {
        return new Clipboard() {
            @Override
            public String get() {
                return Minecraft.getInstance().keyboardHandler.getClipboard();
            }

            @Override
            public void put(String text) {
                Minecraft.getInstance().keyboardHandler.setClipboard(text);
            }
        };
    }

    /** A clipboard that holds one string and no game. */
    static Clipboard scratch() {
        return new Clipboard() {
            private String held = "";

            @Override
            public String get() {
                return held;
            }

            @Override
            public void put(String text) {
                held = text;
            }
        };
    }
}
