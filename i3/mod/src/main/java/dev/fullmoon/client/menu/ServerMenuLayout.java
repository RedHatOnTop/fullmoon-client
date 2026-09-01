package dev.fullmoon.client.menu;

import dev.fullmoon.client.layout.Box;

public record ServerMenuLayout(Box body, int rows, int gap, int cellWidth, int cellHeight) {
    private static final int COLUMNS = 9;
    private static final int PREFERRED_GAP = 4;

    public ServerMenuLayout {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("rows must be between 1 and 6");
        }
    }

    public static ServerMenuLayout fit(Box body, int rows) {
        if (body.w() <= 0 || body.h() <= 0) {
            throw new IllegalArgumentException("body must have positive dimensions");
        }
        int gap = Math.min(PREFERRED_GAP,
            Math.min(body.w() / (COLUMNS * 2), body.h() / (rows * 2)));
        int cellWidth = Math.max(1, (body.w() - gap * (COLUMNS - 1)) / COLUMNS);
        int cellHeight = Math.max(1, (body.h() - gap * (rows - 1)) / rows);
        return new ServerMenuLayout(body, rows, gap, cellWidth, cellHeight);
    }

    public int columns() {
        return COLUMNS;
    }

    public Box slot(int slot) {
        if (slot < 0 || slot >= rows * COLUMNS) {
            throw new IllegalArgumentException("slot is outside the menu");
        }
        int column = slot % COLUMNS;
        int row = slot / COLUMNS;
        return new Box(
            body.x() + column * (cellWidth + gap),
            body.y() + row * (cellHeight + gap),
            cellWidth,
            cellHeight);
    }
}
