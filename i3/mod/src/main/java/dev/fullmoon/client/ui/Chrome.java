package dev.fullmoon.client.ui;

import dev.fullmoon.client.design.Tokens;

/**
 * What a state looks like: the ground under a control, the ink on it, and the edge around it.
 *
 * <p>Two voices, and no third. {@link #quiet} is every control on a surface; {@link #loud} is
 * the one action a surface is about, and a screen with two loud controls has not decided what
 * it is for. Both walk the same eight states, so a state cannot exist in one voice and be
 * missing in the other.
 */
public record Chrome(int fill, int ink, int line) {
    /** Ground-level control: a surface can hold as many of these as it needs. */
    public static Chrome quiet(State state) {
        return switch (state) {
            case REST, FOCUS_VISIBLE ->
                new Chrome(Tokens.Color.SURFACE_RAISED, Tokens.Color.INK_PRIMARY, Tokens.Color.LINE_HAIRLINE);
            case HOVER ->
                new Chrome(Tokens.Color.SURFACE_OVERLAY, Tokens.Color.INK_PRIMARY, Tokens.Color.LINE_STRONG);
            case ACTIVE ->
                new Chrome(Tokens.Color.ACCENT_WASH, Tokens.Color.INK_PRIMARY, Tokens.Color.ACCENT);
            case FOCUS ->
                new Chrome(Tokens.Color.SURFACE_RAISED, Tokens.Color.INK_PRIMARY, Tokens.Color.LINE_STRONG);
            case DISABLED ->
                new Chrome(Tokens.Color.SURFACE_SUNKEN, Tokens.Color.INK_DISABLED, Tokens.Color.LINE_HAIRLINE);
            case LOADING ->
                new Chrome(Tokens.Color.SURFACE_RAISED, Tokens.Color.INK_TERTIARY, Tokens.Color.LINE_HAIRLINE);
            case ERROR ->
                new Chrome(Tokens.Color.SURFACE_RAISED, Tokens.Color.INK_PRIMARY, Tokens.Color.STATUS_DANGER);
        };
    }

    /**
     * The one action a surface is about. Held, it darkens to {@code accent.pressed} rather than
     * lifting: a control that gets brighter under the finger reads as an animation, not a press.
     */
    public static Chrome loud(State state) {
        return switch (state) {
            case REST, FOCUS, FOCUS_VISIBLE ->
                new Chrome(Tokens.Color.ACCENT, Tokens.Color.INK_ON_ACCENT, Tokens.Color.ACCENT);
            case HOVER ->
                new Chrome(Tokens.Color.ACCENT, Tokens.Color.INK_ON_ACCENT, Tokens.Color.INK_ON_ACCENT);
            case ACTIVE ->
                new Chrome(Tokens.Color.ACCENT_PRESSED, Tokens.Color.INK_ON_ACCENT, Tokens.Color.ACCENT_PRESSED);
            case DISABLED ->
                new Chrome(Tokens.Color.SURFACE_SUNKEN, Tokens.Color.INK_DISABLED, Tokens.Color.LINE_HAIRLINE);
            case LOADING ->
                new Chrome(Tokens.Color.ACCENT_WASH, Tokens.Color.INK_SECONDARY, Tokens.Color.ACCENT_WASH);
            case ERROR ->
                new Chrome(Tokens.Color.SURFACE_RAISED, Tokens.Color.INK_PRIMARY, Tokens.Color.STATUS_DANGER);
        };
    }
}
