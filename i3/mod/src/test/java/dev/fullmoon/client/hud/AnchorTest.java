package dev.fullmoon.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import dev.fullmoon.client.layout.Box;

class AnchorTest {

    @Test
    void topLeftAnchoring() {
        Anchor a = Anchor.TOP_LEFT;
        Box b = a.place(960, 540, 100, 40, 12, 16);
        assertEquals(12, b.x());
        assertEquals(16, b.y());
        assertEquals(100, b.w());
        assertEquals(40, b.h());

        assertEquals(12, a.computeOffsetX(960, 100, b.x()));
        assertEquals(16, a.computeOffsetY(540, 40, b.y()));
    }

    @Test
    void topRightAnchoring() {
        Anchor a = Anchor.TOP_RIGHT;
        Box b = a.place(960, 540, 100, 40, 12, 16);
        assertEquals(960 - 100 - 12, b.x());
        assertEquals(16, b.y());

        assertEquals(12, a.computeOffsetX(960, 100, b.x()));
        assertEquals(16, a.computeOffsetY(540, 40, b.y()));
    }

    @Test
    void bottomCenterAnchoring() {
        Anchor a = Anchor.BOTTOM_CENTER;
        Box b = a.place(960, 540, 200, 50, 0, 20);
        assertEquals((960 - 200) / 2, b.x());
        assertEquals(540 - 50 - 20, b.y());

        assertEquals(0, a.computeOffsetX(960, 200, b.x()));
        assertEquals(20, a.computeOffsetY(540, 50, b.y()));
    }

    @Test
    void resolutionChangeKeepsDistanceFromAnchor() {
        Anchor a = Anchor.BOTTOM_RIGHT;
        int elemW = 120;
        int elemH = 30;
        int offsetX = 16;
        int offsetY = 16;

        // Screen 1: 960x540
        Box b1 = a.place(960, 540, elemW, elemH, offsetX, offsetY);
        assertEquals(960 - 120 - 16, b1.x());
        assertEquals(540 - 30 - 16, b1.y());

        // Screen 2: 1920x1080 (resolution doubled)
        Box b2 = a.place(1920, 1080, elemW, elemH, offsetX, offsetY);
        assertEquals(1920 - 120 - 16, b2.x());
        assertEquals(1080 - 30 - 16, b2.y());

        // Relative offset from right and bottom edges remains exactly 16px!
        assertEquals(16, 1920 - b2.right());
        assertEquals(16, 1080 - b2.bottom());
    }

    @Test
    void everyAnchorInvertsItsOwnPlacement() {
        // The launcher's editor drags in screen pixels and stores an offset; the pair has to be
        // exact in both directions or a layout moves every time it is opened.
        for (Anchor a : Anchor.values()) {
            Box b = a.place(960, 540, 100, 40, 13, 17);
            assertEquals(13, a.computeOffsetX(960, 100, b.x()), a.name());
            assertEquals(17, a.computeOffsetY(540, 40, b.y()), a.name());
        }
    }

    @Test
    void theCentreRowAndColumnSplitWhatIsLeftOverEvenly() {
        Anchor a = Anchor.CENTER;
        Box b = a.place(961, 541, 100, 40, 0, 0);
        assertEquals((961 - 100) / 2, b.x());
        assertEquals((541 - 40) / 2, b.y());

        assertEquals(0, a.computeOffsetX(961, 100, b.x()));
        assertEquals(0, a.computeOffsetY(541, 40, b.y()));
    }

    @Test
    void aGridCellNamesItsAnchorAndOffTheGridIsTheNearestCorner() {
        assertEquals(Anchor.CENTER_RIGHT, Anchor.fromGrid(2, 1));
        assertEquals(Anchor.TOP_LEFT, Anchor.fromGrid(-4, -1));
        assertEquals(Anchor.BOTTOM_RIGHT, Anchor.fromGrid(9, 9));
    }

    @Test
    void anAnchorReportsTheCellAndTheFractionsItWasBuiltFrom() {
        Anchor a = Anchor.BOTTOM_CENTER;
        assertEquals(1, a.col());
        assertEquals(2, a.row());
        assertEquals(0.5f, a.u());
        assertEquals(1.0f, a.v());
        assertEquals("하단 중앙", a.label());
        assertEquals(a, Anchor.valueOf("BOTTOM_CENTER"));
    }

    @Test
    void aScreenWithNoSizeStillNamesAnAnchor() {
        // computeBounds runs before the window has reported itself at least once.
        assertEquals(Anchor.BOTTOM_RIGHT, Anchor.nearest(0, 0, 100, 30, 0, 0));
    }

    @Test
    void nearestAnchorDetection() {
        assertEquals(Anchor.TOP_LEFT, Anchor.nearest(960, 540, 100, 30, 20, 20));
        assertEquals(Anchor.TOP_RIGHT, Anchor.nearest(960, 540, 100, 30, 800, 20));
        assertEquals(Anchor.BOTTOM_CENTER, Anchor.nearest(960, 540, 100, 30, 430, 500));
        assertEquals(Anchor.CENTER, Anchor.nearest(960, 540, 100, 30, 430, 250));
    }
}
