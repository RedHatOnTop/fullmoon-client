package dev.fullmoon.client.hud;

/**
 * The grid both HUD editors snap a drag to.
 *
 * <p>It is one class because the launcher's editor and the in-game one write the same
 * {@code config/fullmoon/hud.json}: rounding to different steps would shift a layout the first time
 * it was opened in the other surface, and the file gives no way to tell that apart from an edit.
 */
public final class HudGrid {
    /** GUI px. Four is the smallest step that still reads as alignment rather than as jitter. */
    public static final int DEFAULT_STEP = 4;

    /** A step off disk, made usable — zero or less would divide by nothing. */
    public static int sanitize(int step) {
        return step > 0 ? step : DEFAULT_STEP;
    }

    /**
     * Rounds a screen coordinate to the nearest multiple of the step, halves upward. That is
     * {@code Math.round}'s own rule here and in the launcher's JavaScript, which is why the two
     * agree on a coordinate that lands exactly between two grid lines.
     */
    public static int snap(int raw, int step) {
        int s = sanitize(step);
        return Math.round(raw / (float) s) * s;
    }

    private HudGrid() {}
}
