package dev.fullmoon.client.hud;

import java.util.Objects;

import dev.fullmoon.client.layout.Box;

/** Screen anchor for resolution-independent HUD placement. */
public enum Anchor {
    TOP_LEFT(0.0f, 0.0f),
    TOP_CENTER(0.5f, 0.0f),
    TOP_RIGHT(1.0f, 0.0f),
    CENTER_LEFT(0.0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1.0f, 0.5f),
    BOTTOM_LEFT(0.0f, 1.0f),
    BOTTOM_CENTER(0.5f, 1.0f),
    BOTTOM_RIGHT(1.0f, 1.0f);

    private final float u;
    private final float v;

    Anchor(float u, float v) {
        this.u = u;
        this.v = v;
    }

    public float u() {
        return u;
    }

    public float v() {
        return v;
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

    public static Anchor nearest(int screenW, int screenH, int elementW, int elementH, int screenX, int screenY) {
        float centerX = screenX + elementW * 0.5f;
        float centerY = screenY + elementH * 0.5f;

        float relX = centerX / (float) Math.max(1, screenW);
        float relY = centerY / (float) Math.max(1, screenH);

        int col = relX < 0.333f ? 0 : (relX < 0.666f ? 1 : 2);
        int row = relY < 0.333f ? 0 : (relY < 0.666f ? 1 : 2);

        return values()[row * 3 + col];
    }
}
