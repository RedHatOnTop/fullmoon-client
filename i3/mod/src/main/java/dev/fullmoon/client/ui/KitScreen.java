package dev.fullmoon.client.ui;

import java.util.List;
import java.util.Locale;

import dev.fullmoon.client.FullmoonClient;
import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * The widget kit: every state of every control this client has, and the proof that eight states
 * are eight states rather than eight screenshots of the one the mouse happened to be in.
 *
 * <p>The matrix and the live band below it go through the same {@code draw(Painter, State)}. The
 * only difference is where the state comes from — the matrix hands one in, the band asks the
 * surface — so a cell that lied about a state would have to lie in the band as well.
 *
 * <p>The band is where the routing shows. 적용 puts itself in flight, 취소 takes it back out, and
 * the switch enables and disables 적용, which makes LOADING and DISABLED reachable with the mouse
 * instead of only tabulated. A control in flight keeps its focus ring and stops answering: both
 * halves of that are visible here.
 */
public final class KitScreen extends Screen {
    private static final int MAX_CONTENT = 520;
    private static final int CELL_GAP = Tokens.Space.SNUG;
    private static final int ROW_H = Button.HEIGHT + Tokens.Space.COZY;
    private static final int CAPTION_LINES = captionLines();

    /** A specimen this wide takes the whole cell it lands in. */
    private static final int FILL = 0;

    private static final Runnable INERT = () -> {};
    private static final Toggle.Switched UNTHROWN = on -> {};

    /** One line of the matrix: what it is called, what draws it, and how big that draws. */
    private record Row(String name, Widget widget, int w, int h) {}

    private final Surface surface = new Surface();
    private final Button apply = new Button(Voice.LOUD, "적용", this::inFlight);
    private final Button cancel = new Button(Voice.QUIET, "취소", this::landed);
    private final Toggle arm = new Toggle("적용 허용", true, this::armed);

    private final List<Row> rows = List.of(
        new Row("버튼 · quiet", new Button(Voice.QUIET, "저장", INERT), FILL, Button.HEIGHT),
        new Row("버튼 · loud", new Button(Voice.LOUD, "저장", INERT), FILL, Button.HEIGHT),
        new Row("스위치 · 꺼짐", new Toggle("", false, UNTHROWN), Toggle.TRACK_W, Toggle.TRACK_H),
        new Row("스위치 · 켜짐", new Toggle("", true, UNTHROWN), Toggle.TRACK_W, Toggle.TRACK_H));

    private Box content = Box.EMPTY;
    private int spine;
    private int matrixTop;
    private int liveTop;

    public KitScreen() {
        super(Typeset.say(Tokens.Type.TITLE, "위젯 키트"));
        // Registration order is Tab order, and this one runs left to right across the band.
        surface.add(arm);
        surface.add(cancel);
        surface.add(apply);
    }

    /** The world keeps running behind the blur: a dev surface is not a pause menu. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        int w = Math.min(MAX_CONTENT, width - Tokens.Space.SECTION * 2);
        int left = (width - w) / 2;
        content = Box.between(left, Tokens.Space.SECTION, left + w, DevChrome.footerY(height));

        matrixTop = content.y() + DevChrome.headerHeight();
        spine = spine();
        liveTop = bandTop() + rows.size() * ROW_H + Tokens.Space.GUTTER;

        int line = liveTop + DevChrome.sectionHeadHeight();
        int applyW = apply.measure();
        int cancelW = cancel.measure();
        apply.place(new Box(content.right() - applyW, line, applyW, Button.HEIGHT));
        cancel.place(new Box(content.right() - applyW - Tokens.Space.COZY - cancelW, line,
            cancelW, Button.HEIGHT));
        arm.place(new Box(content.x(), line, arm.measure(), Toggle.HEIGHT));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        Painter painter = new Painter(gfx);
        painter.blurredStratum();
        painter.fill(0, 0, painter.width(), painter.height(),
            Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.82f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // Read the hover every frame rather than only on a move event: the pointer can sit still
        // while the layout under it changes, and then a stale hover is a lie about the surface.
        surface.hover(mouseX, mouseY);

        Painter painter = new Painter(gfx);
        DevChrome.header(painter, content.x(), content.y(), content.w(), "클라이언트 i3 · 위젯 키트");
        matrix(painter);
        band(painter);
        DevChrome.footer(painter, content.x(), DevChrome.footerY(height), content.w(),
            "Esc 닫기 · " + FullmoonClient.kitKey() + " 다시 열기 · "
                + FullmoonClient.specimenKey() + " 표본",
            readout());
    }

    private void matrix(Painter painter) {
        int captions = DevChrome.sectionHead(painter, "상태 매트릭스 · 여덟 칸 전부",
            content.x(), matrixTop);
        int top = bandTop();
        int bottom = top + rows.size() * ROW_H;
        Box grid = Box.between(content.x() + spine, captions, content.right(), bottom);
        int columns = State.values().length;

        for (State state : State.values()) {
            Box cell = grid.col(state.ordinal(), columns, CELL_GAP);
            String[] caption = caption(state);
            for (int i = 0; i < caption.length; i++) {
                Typeset.drawCentered(painter, Tokens.Type.LABEL, caption[i], cell.midX(),
                    captions + i * Tokens.Type.LABEL.leading(), Tokens.Color.INK_TERTIARY);
            }
        }
        painter.hRule(content.x(), top - Tokens.Space.TIGHT, content.w(), Tokens.Color.LINE_STRONG);
        painter.vRule(content.x() + spine - Tokens.Space.LOOSE, captions, bottom - captions,
            Tokens.Color.LINE_HAIRLINE);

        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowTop = top + i * ROW_H;
            Typeset.draw(painter, Tokens.Type.LABEL, row.name(), content.x(),
                Typeset.centred(Tokens.Type.LABEL, rowTop, ROW_H), Tokens.Color.INK_TERTIARY);

            for (State state : State.values()) {
                Box cell = grid.col(state.ordinal(), columns, CELL_GAP);
                int cw = row.w() == FILL ? cell.w() - Tokens.Space.COZY : row.w();
                // A specimen is placed and drawn in the same breath, eight times a row. It is not
                // on the surface, so nothing hit-tests the box it was left in.
                row.widget().place(new Box(cell.midX() - cw / 2,
                    rowTop + (ROW_H - row.h()) / 2, cw, row.h()));
                row.widget().draw(painter, state);
            }
            painter.hRule(content.x(), rowTop + ROW_H, content.w(), Tokens.Color.LINE_HAIRLINE);
        }
    }

    private void band(Painter painter) {
        DevChrome.sectionHead(painter, "라이브 · 적용은 요청을 띄우고 취소는 되돌린다",
            content.x(), liveTop);
        for (Widget widget : surface.widgets()) {
            widget.draw(painter, surface.state(widget));
        }
    }

    private String readout() {
        Widget held = surface.held();
        return held == null
            ? "포커스 없음 · Tab"
            : "포커스 " + held.label() + " · " + String.join(" ", caption(surface.state(held)));
    }

    /** The y the matrix's first row starts at: the section head, then the caption lines. */
    private int bandTop() {
        return matrixTop + DevChrome.sectionHeadHeight()
            + CAPTION_LINES * Tokens.Type.LABEL.leading() + Tokens.Space.SNUG;
    }

    private int spine() {
        int widest = 0;
        for (Row row : rows) {
            widest = Math.max(widest, Typeset.width(Tokens.Type.LABEL, row.name()));
        }
        return widest + Tokens.Space.SECTION;
    }

    /** The state's own name. {@code focus_visible} is two words, so it is two lines of caption. */
    private static String[] caption(State state) {
        return state.name().toLowerCase(Locale.ROOT).split("_");
    }

    private static int captionLines() {
        int most = 1;
        for (State state : State.values()) {
            most = Math.max(most, caption(state).length);
        }
        return most;
    }

    private void inFlight() {
        apply.busy(true);
    }

    private void landed() {
        apply.busy(false);
    }

    private void armed(boolean on) {
        apply.enabled(on);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        surface.pointer(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        return surface.press(event.x(), event.y()) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return surface.release(event.x(), event.y()) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (surface.captured() == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        surface.pointer(event.x(), event.y());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return surface.scroll(mouseX, mouseY, scrollY)
            || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return surface.key(event.key(), event.hasShiftDown()) || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return surface.type(event.codepoint()) || super.charTyped(event);
    }
}
