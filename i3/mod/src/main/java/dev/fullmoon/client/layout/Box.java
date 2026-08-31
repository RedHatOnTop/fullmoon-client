package dev.fullmoon.client.layout;

/**
 * An integer rect in GUI px, and the only arithmetic in this client that divides space.
 *
 * <p>Every method returns a new box. A layout pass then reads as the shape it produces, and a
 * widget can never move the box that placed it — which is what keeps the drawing code free of
 * the running x/y cursors that make a screen impossible to re-order.
 */
public record Box(int x, int y, int w, int h) {
    public static final Box EMPTY = new Box(0, 0, 0, 0);

    /** A box from its edges. A reversed pair collapses to zero instead of going negative. */
    public static Box between(int left, int top, int right, int bottom) {
        return new Box(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    public int right() {
        return x + w;
    }

    public int bottom() {
        return y + h;
    }

    public int midX() {
        return x + w / 2;
    }

    public int midY() {
        return y + h / 2;
    }

    public boolean empty() {
        return w <= 0 || h <= 0;
    }

    public Box inset(int all) {
        return inset(all, all);
    }

    public Box inset(int dx, int dy) {
        return between(x + dx, y + dy, right() - dx, bottom() - dy);
    }

    public Box insets(int left, int top, int right, int bottom) {
        return between(x + left, y + top, right() - right, bottom() - bottom);
    }

    public Box at(int nx, int ny) {
        return new Box(nx, ny, w, h);
    }

    /** Mouse coordinates are doubles, and the right and bottom edges belong to the next box. */
    public boolean holds(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    /** Whether two boxes share a pixel. An empty box shares none, including with itself. */
    public boolean overlaps(Box other) {
        return !empty() && !other.empty()
            && x < other.right() && other.x < right()
            && y < other.bottom() && other.y < bottom();
    }

    /** The leading {@code width} of this box, and what is left of it past a {@code gap}. */
    public Split splitLeft(int width, int gap) {
        int cut = Math.clamp(width, 0, w);
        return new Split(new Box(x, y, cut, h), between(x + cut + gap, y, right(), bottom()));
    }

    /** The leading {@code height} of this box, and what is left of it past a {@code gap}. */
    public Split splitTop(int height, int gap) {
        int cut = Math.clamp(height, 0, h);
        return new Split(new Box(x, y, w, cut), between(x, y + cut + gap, right(), bottom()));
    }

    /**
     * The {@code index}-th of {@code count} equal columns. The division remainder is spent on
     * the leading columns one px at a time rather than left as a ragged edge on the last one.
     */
    public Box col(int index, int count, int gap) {
        int span = w - gap * (count - 1);
        int cell = span / count;
        int spare = span - cell * count;
        int before = index * cell + Math.min(index, spare) + index * gap;
        return new Box(x + before, y, cell + (index < spare ? 1 : 0), h);
    }

    /** The {@code index}-th of {@code count} equal rows, sharing the remainder as {@link #col}. */
    public Box row(int index, int count, int gap) {
        Box t = new Box(y, x, h, w).col(index, count, gap);
        return new Box(t.y(), t.x(), t.h(), t.w());
    }

    /** A box of {@code cw} × {@code ch} centred in this one. */
    public Box centred(int cw, int ch) {
        return new Box(x + (w - cw) / 2, y + (h - ch) / 2, cw, ch);
    }

    /** The head of a division and the tail left over. */
    public record Split(Box head, Box rest) {}
}
