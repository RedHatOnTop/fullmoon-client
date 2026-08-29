package dev.fullmoon.client.hud;

import dev.fullmoon.client.layout.Box;

/** Screen anchor for resolution-independent HUD placement. */
public enum Anchor {
    TOP_LEFT(0, 0, 0.0f, 0.0f, "좌상단", "Top Left"),
    TOP_CENTER(1, 0, 0.5f, 0.0f, "상단 중앙", "Top Center"),
    TOP_RIGHT(2, 0, 1.0f, 0.0f, "우상단", "Top Right"),
    CENTER_LEFT(0, 1, 0.0f, 0.5f, "좌측 중앙", "Center Left"),
    CENTER(1, 1, 0.5f, 0.5f, "화면 중앙", "Center"),
    CENTER_RIGHT(2, 1, 1.0f, 0.5f, "우측 중앙", "Center Right"),
    BOTTOM_LEFT(0, 2, 0.0f, 1.0f, "좌하단", "Bottom Left"),
    BOTTOM_CENTER(1, 2, 0.5f, 1.0f, "하단 중앙", "Bottom Center"),
    BOTTOM_RIGHT(2, 2, 1.0f, 1.0f, "우하단", "Bottom Right");

    private final int col;
    private final int row;
    private final float u;
    private final float v;
    private final String labelKo;
    private final String labelEn;

    Anchor(int col, int row, float u, float v, String labelKo, String labelEn) {
        this.col = col;
        this.row = row;
        this.u = u;
        this.v = v;
        this.labelKo = labelKo;
        this.labelEn = labelEn;
    }

    public int col() {
        return col;
    }

    public int row() {
        return row;
    }

    public float u() {
        return u;
    }

    public float v() {
        return v;
    }

    public String label() {
        return labelKo;
    }

    public int computeX(int screenW, int elementW, int offsetX) {
        if (u == 0.0f) {
            return offsetX;
        } else if (u == 0.5f) {
            return (screenW - elementW) / 2 + offsetX;
        } else {
            return screenW - elementW - offsetX;
        }
    }

    public int computeY(int screenH, int elementH, int offsetY) {
        if (v == 0.0f) {
            return offsetY;
        } else if (v == 0.5f) {
            return (screenH - elementH) / 2 + offsetY;
        } else {
            return screenH - elementH - offsetY;
        }
    }

    public int computeOffsetX(int screenW, int elementW, int screenX) {
        if (u == 0.0f) {
            return screenX;
        } else if (u == 0.5f) {
            return screenX - (screenW - elementW) / 2;
        } else {
            return screenW - elementW - screenX;
        }
    }

    public int computeOffsetY(int screenH, int elementH, int screenY) {
        if (v == 0.0f) {
            return screenY;
        } else if (v == 0.5f) {
            return screenY - (screenH - elementH) / 2;
        } else {
            return screenH - elementH - screenY;
        }
    }

    public Box place(int screenW, int screenH, int elementW, int elementH, int offsetX, int offsetY) {
        int x = computeX(screenW, elementW, offsetX);
        int y = computeY(screenH, elementH, offsetY);
        return new Box(x, y, elementW, elementH);
    }

    public static Anchor fromGrid(int col, int row) {
        col = Math.clamp(col, 0, 2);
        row = Math.clamp(row, 0, 2);
        return values()[row * 3 + col];
    }

    public static Anchor nearest(int screenW, int screenH, int elementW, int elementH, int screenX, int screenY) {
        float centerX = screenX + elementW * 0.5f;
        float centerY = screenY + elementH * 0.5f;

        float relX = centerX / (float) Math.max(1, screenW);
        float relY = centerY / (float) Math.max(1, screenH);

        int col = relX < 0.333f ? 0 : (relX < 0.666f ? 1 : 2);
        int row = relY < 0.333f ? 0 : (relY < 0.666f ? 1 : 2);

        return fromGrid(col, row);
    }
}
