package dev.fullmoon.client.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.fullmoon.client.FullmoonClient;
import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * The design specimen: every token this client owns, drawn by the client's own renderer.
 *
 * <p>It is a working surface, not a demo. Each block is here because something downstream
 * depends on it being right — the type roll proves the five baked providers load and that
 * their leadings stack without collision, the shape rail proves one SDF pipeline covers every
 * solid the UI needs, the colour bands prove the generated constants are the ones on screen,
 * and the figure pair proves tabular digits hold still while proportional ones do not.
 */
public final class SpecimenScreen extends Screen {
    private static final int MAX_CONTENT = 420;
    private static final int LEFT_PERCENT = 62;
    private static final String[] SAMPLES = {"달빛 Fullmoon 0123", "달빛 Fullmoon", "달빛"};

    private int frames;

    public SpecimenScreen() {
        super(Typeset.say(Tokens.Type.TITLE, "디자인 표본"));
    }

    /** The world keeps running behind the blur, so a live frame counter has something to count. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        Painter painter = new Painter(gfx);
        painter.blurredStratum();
        painter.fill(0, 0, painter.width(), painter.height(), Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.82f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        frames++;
        Painter painter = new Painter(gfx);

        int contentW = Math.min(MAX_CONTENT, width - Tokens.Space.SECTION * 2);
        int x = (width - contentW) / 2;
        int top = header(painter, x, Tokens.Space.SECTION, contentW);

        int leftW = contentW * LEFT_PERCENT / 100;
        int rightX = x + leftW + Tokens.Space.GUTTER;
        int rightW = contentW - leftW - Tokens.Space.GUTTER;

        int leftBottom = shapeRail(painter, x, typeRoll(painter, x, top, leftW) + Tokens.Space.SECTION, leftW);
        int rightBottom = figures(painter, rightX, colorBands(painter, rightX, top, rightW) + Tokens.Space.SECTION, rightW);

        painter.vRule(x + leftW + Tokens.Space.GUTTER / 2, top,
            Math.max(leftBottom, rightBottom) - top, Tokens.Color.LINE_HAIRLINE);
        footer(painter, x, contentW, height - Tokens.Space.SECTION - Tokens.Type.LABEL.leading());
    }

    private int header(Painter painter, int x, int y, int w) {
        painter.fill(x, y + Tokens.Space.SNUG, Tokens.Stroke.FOCUS,
            Tokens.Type.DISPLAY.px() - Tokens.Space.BASE, Tokens.Color.ACCENT);
        int textX = x + Tokens.Stroke.FOCUS + Tokens.Space.COZY;

        Typeset.draw(painter, Tokens.Type.DISPLAY, "Fullmoon", textX, y, Tokens.Color.INK_PRIMARY);
        Typeset.draw(painter, Tokens.Type.LABEL, "클라이언트 i3 · 디자인 표본",
            textX, y + Tokens.Type.DISPLAY.leading(), Tokens.Color.INK_TERTIARY);
        Typeset.tabularRight(painter, Tokens.Type.LABEL, painter.width() + " × " + painter.height() + " gui px",
            x + w, y + Tokens.Space.TIGHT, Tokens.Color.INK_TERTIARY);

        int bottom = y + Tokens.Type.DISPLAY.leading() + Tokens.Type.LABEL.leading();
        painter.hRule(x, bottom, w, Tokens.Color.LINE_STRONG);
        return bottom + Tokens.Space.GUTTER;
    }

    /** A section head is a label with an accent tick, never a tag left and a value right. */
    private int sectionHead(Painter painter, String name, int x, int y) {
        painter.fill(x, y + Tokens.Space.TIGHT, Tokens.Stroke.HAIR,
            Tokens.Type.LABEL.px() - Tokens.Space.TIGHT, Tokens.Color.ACCENT);
        Typeset.draw(painter, Tokens.Type.LABEL, name, x + Tokens.Space.BASE, y, Tokens.Color.INK_SECONDARY);
        return y + Tokens.Type.LABEL.leading() + Tokens.Space.COZY;
    }

    private int typeRoll(Painter painter, int x, int y, int w) {
        int cursor = sectionHead(painter, "타입 스케일", x, y);
        int nameCol = nameColumn();
        int metricW = Typeset.tabularWidth(Tokens.Type.LABEL, "22/28") + Tokens.Space.LOOSE;
        int sampleW = w - nameCol - metricW;

        for (Map.Entry<String, Tokens.Type.Role> entry : Tokens.Type.ROLL) {
            Tokens.Type.Role role = entry.getValue();
            int rowH = role.leading() + Tokens.Space.SNUG;
            // Name, sample and metric share the sample's baseline, so a row reads as one line
            // however large the face in it is.
            int textY = Typeset.centred(role, cursor, rowH);

            Typeset.draw(painter, Tokens.Type.LABEL, entry.getKey(), x, textY, Tokens.Color.INK_TERTIARY);

            // The column is clipped, not the row: the game hangs every face off one 9 px line
            // box, so a 22 px sample legitimately draws taller than the band it is measured in,
            // and cutting its ascenders would misrepresent the thing the row exists to show.
            painter.pushClip(x + nameCol, 0, sampleW, painter.height());
            Typeset.draw(painter, role, sample(role, sampleW), x + nameCol, textY, Tokens.Color.INK_PRIMARY);
            painter.popClip();

            Typeset.tabularRight(painter, Tokens.Type.LABEL, role.px() + "/" + role.leading(),
                x + w, textY, Tokens.Color.INK_TERTIARY);

            cursor += rowH;
            painter.hRule(x, cursor, w, Tokens.Color.LINE_HAIRLINE);
            cursor += Tokens.Space.SNUG;
        }
        return cursor;
    }

    /**
     * The longest sample the role can set inside the column. A foundry shows a display face on
     * fewer characters for the same reason: the row exists to show the shape of the glyphs, and
     * a face cut off mid-stroke shows nothing. The clip stays as the guarantee.
     */
    private static String sample(Tokens.Type.Role role, int w) {
        for (String candidate : SAMPLES) {
            if (Typeset.width(role, candidate) <= w) {
                return candidate;
            }
        }
        return SAMPLES[SAMPLES.length - 1];
    }

    /** The name column is as wide as the longest role name, so no sample can be pushed into it. */
    private static int nameColumn() {
        int widest = 0;
        for (Map.Entry<String, Tokens.Type.Role> entry : Tokens.Type.ROLL) {
            widest = Math.max(widest, Typeset.width(Tokens.Type.LABEL, entry.getKey()));
        }
        return widest + Tokens.Space.LOOSE;
    }

    /** Six chips, one pipeline: the fill, the three radii, the inset stroke, the ring and dot. */
    private int shapeRail(Painter painter, int x, int y, int w) {
        int cursor = sectionHead(painter, "형상 · 하나의 SDF 파이프라인", x, y);
        String[] captions = {"none", "sm", "md", "lg", "1px", "ring"};
        int gap = Tokens.Space.COZY;
        int cellW = (w - gap * (captions.length - 1)) / captions.length;
        int cellH = 22;

        for (int i = 0; i < captions.length; i++) {
            int cx = x + i * (cellW + gap);
            switch (i) {
                case 0 -> painter.fill(cx, cursor, cellW, cellH, Tokens.Radius.NONE, Tokens.Color.SURFACE_RAISED);
                case 1 -> painter.fill(cx, cursor, cellW, cellH, Tokens.Radius.SM, Tokens.Color.SURFACE_RAISED);
                case 2 -> painter.fill(cx, cursor, cellW, cellH, Tokens.Radius.MD, Tokens.Color.SURFACE_RAISED);
                case 3 -> painter.fill(cx, cursor, cellW, cellH, Tokens.Radius.LG, Tokens.Color.ACCENT_WASH);
                case 4 -> painter.border(cx, cursor, cellW, cellH, Tokens.Radius.MD,
                    Tokens.Stroke.HAIR, Tokens.Color.LINE_STRONG);
                default -> {
                    float mid = cursor + cellH / 2.0f;
                    painter.ring(cx + cellW / 2.0f, mid, cellH / 2.0f - 1.0f, Tokens.Stroke.FOCUS, Tokens.Color.ACCENT);
                    painter.dot(cx + cellW / 2.0f, mid, Tokens.Space.SNUG, Tokens.Color.ACCENT);
                }
            }
            Typeset.drawCentered(painter, Tokens.Type.LABEL, captions[i],
                cx + cellW / 2, cursor + cellH + Tokens.Space.SNUG, Tokens.Color.INK_TERTIARY);
        }
        return cursor + cellH + Tokens.Space.SNUG + Tokens.Type.LABEL.leading();
    }

    private int colorBands(Painter painter, int x, int y, int w) {
        int cursor = sectionHead(painter, "색 · OKLCH 토큰", x, y);
        Map<String, List<Integer>> families = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : Tokens.COLOR_ROLL) {
            int dot = entry.getKey().indexOf('.');
            String family = dot < 0 ? entry.getKey() : entry.getKey().substring(0, dot);
            families.computeIfAbsent(family, key -> new ArrayList<>()).add(entry.getValue());
        }

        int bandH = 14;
        int nameCol = 0;
        for (String family : families.keySet()) {
            nameCol = Math.max(nameCol, Typeset.width(Tokens.Type.LABEL, family));
        }
        nameCol += Tokens.Space.COZY;

        for (Map.Entry<String, List<Integer>> family : families.entrySet()) {
            Typeset.draw(painter, Tokens.Type.LABEL, family.getKey(), x,
                Typeset.centred(Tokens.Type.LABEL, cursor, bandH), Tokens.Color.INK_TERTIARY);

            List<Integer> swatches = family.getValue();
            int gap = Tokens.Space.TIGHT;
            int bandsW = w - nameCol;
            int cellW = (bandsW - gap * (swatches.size() - 1)) / swatches.size();
            for (int i = 0; i < swatches.size(); i++) {
                int sx = x + nameCol + i * (cellW + gap);
                painter.fill(sx, cursor, cellW, bandH, Tokens.Radius.SM, swatches.get(i));
                // The darkest surfaces would otherwise be invisible against the scrim.
                painter.border(sx, cursor, cellW, bandH, Tokens.Radius.SM,
                    Tokens.Stroke.HAIR, Tokens.Color.LINE_HAIRLINE);
            }
            cursor += bandH + Tokens.Space.COZY;
        }
        return cursor;
    }

    /** The same live value set twice. The right column is the one that jitters. */
    private int figures(Painter painter, int x, int y, int w) {
        int cursor = sectionHead(painter, "숫자 · 고정폭", x, y);
        String live = String.format("%06d", frames);
        int half = w / 2;

        Typeset.draw(painter, Tokens.Type.LABEL, "고정폭", x, cursor, Tokens.Color.INK_TERTIARY);
        Typeset.draw(painter, Tokens.Type.LABEL, "비례", x + half, cursor, Tokens.Color.INK_TERTIARY);
        cursor += Tokens.Type.LABEL.leading() + Tokens.Space.TIGHT;

        Typeset.tabular(painter, Tokens.Type.BODY_STRONG, live, x, cursor, Tokens.Color.ACCENT);
        Typeset.draw(painter, Tokens.Type.BODY_STRONG, live, x + half, cursor, Tokens.Color.INK_SECONDARY);
        return cursor + Tokens.Type.BODY_STRONG.leading();
    }

    private void footer(Painter painter, int x, int w, int y) {
        painter.hRule(x, y, w, Tokens.Color.LINE_HAIRLINE);
        int textY = y + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.LABEL,
            "Esc 닫기 · " + FullmoonClient.specimenKey() + " 다시 열기",
            x, textY, Tokens.Color.INK_TERTIARY);
        Typeset.tabularRight(painter, Tokens.Type.LABEL,
            "색 " + Tokens.COLOR_ROLL.size() + " · 타입 " + Tokens.Type.ROLL.size(),
            x + w, textY, Tokens.Color.INK_TERTIARY);
    }
}
