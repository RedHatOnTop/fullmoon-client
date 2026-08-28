package dev.fullmoon.client.layout;

/**
 * A cursor down a column of bands. A surface hands it the area it owns and asks for bands in
 * the order they are drawn; the cursor is the only thing that has to know where the last one
 * ended.
 *
 * <p>A band is never clamped to the area. Clamping would silently move content that does not
 * fit, and a surface that overflows should be caught by {@link #overflows} and re-laid out, not
 * quietly folded into its own footer.
 */
public final class Stack {
    private final Box area;
    private final int gap;
    private int used;
    private boolean started;

    public Stack(Box area, int gap) {
        this.area = area;
        this.gap = gap;
    }

    /** The next band of {@code height}, placed under the last one. */
    public Box next(int height) {
        if (started) {
            used += gap;
        }
        started = true;
        Box band = new Box(area.x(), area.y() + used, area.w(), height);
        used += height;
        return band;
    }

    /** Skips {@code height} without handing out a band — a gap the design asks for by name. */
    public void skip(int height) {
        used += height;
    }

    /** The area not yet handed out. */
    public Box rest() {
        return Box.between(area.x(), area.y() + used, area.right(), area.bottom());
    }

    /** Where the next band would start. */
    public int cursor() {
        return area.y() + used;
    }

    /** How much of the area the bands have taken, gaps included. */
    public int used() {
        return used;
    }

    public boolean overflows() {
        return used > area.h();
    }
}
