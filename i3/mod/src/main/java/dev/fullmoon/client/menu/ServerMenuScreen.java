package dev.fullmoon.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.network.FullmoonChannel;
import dev.fullmoon.client.network.MenuProtocol;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;
import dev.fullmoon.client.ui.SurfaceScreen;
import dev.fullmoon.client.ui.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ServerMenuScreen extends SurfaceScreen {
    private static final int MAX_CONTENT = 720;
    private static final int HEADER_HEIGHT = 48;
    private static final int DETAIL_HEIGHT = 58;
    private static final int FOOTER_HEIGHT = 22;
    private static final long REQUEST_TIMEOUT_MILLIS = 5_000;

    private final Screen parent;
    private final MenuProtocol.Open menu;
    private final List<ServerMenuTile> tiles;

    private Box content = Box.EMPTY;
    private Box grid = Box.EMPTY;
    private Box detail = Box.EMPTY;
    private long requestedAt;
    private boolean closingFromServer;

    public ServerMenuScreen(Screen parent, MenuProtocol.Open menu) {
        super(Component.literal(menu.title()));
        this.parent = parent;
        this.menu = menu;
        this.tiles = createTiles(menu.items());
        this.tiles.forEach(surface::add);
    }

    public String menuId() {
        return menu.id();
    }

    public Screen parentScreen() {
        return parent;
    }

    public ServerMenuScreen refreshed(MenuProtocol.Open next) {
        return new ServerMenuScreen(parent, next);
    }

    public void closeFromServer() {
        closingFromServer = true;
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (!closingFromServer) {
            FullmoonChannel.closeMenu(menu.id(), menu.revision());
        }
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void init() {
        int edge = height < 360 ? Tokens.Space.LOOSE : Tokens.Space.SECTION;
        int frameWidth = Math.min(MAX_CONTENT, width - edge * 2);
        content = new Box((width - frameWidth) / 2, edge, frameWidth, height - edge * 2);

        int footerY = content.bottom() - FOOTER_HEIGHT;
        int detailY = footerY - DETAIL_HEIGHT;
        int gridTop = content.y() + HEADER_HEIGHT;
        grid = Box.between(content.x(), gridTop, content.right(),
            detailY - Tokens.Space.COZY);
        detail = Box.between(content.x(), detailY, content.right(), footerY);

        ServerMenuLayout layout = ServerMenuLayout.fit(grid, menu.rows());
        for (ServerMenuTile tile : tiles) {
            tile.place(layout.slot(tile.item().slot()));
        }
    }

    @Override
    public void tick() {
        if (requestedAt > 0 && System.currentTimeMillis() - requestedAt >= REQUEST_TIMEOUT_MILLIS) {
            requestedAt = 0;
            tiles.forEach(tile -> tile.busy(false));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float partialTick) {
        Painter painter = new Painter(gfx);
        painter.blurredStratum();
        painter.fill(0, 0, painter.width(), painter.height(),
            Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.86f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float partialTick) {
        surface.hover(mouseX, mouseY);
        Painter painter = new Painter(gfx);
        header(painter);
        surface.draw(painter);
        details(painter);
        footer(painter);
    }

    private void header(Painter painter) {
        int y = content.y();
        painter.fill(content.x(), Typeset.capTop(Tokens.Type.DISPLAY, y),
            Tokens.Stroke.FOCUS, Typeset.capHeight(Tokens.Type.DISPLAY), Tokens.Color.ACCENT);
        int left = content.x() + Tokens.Stroke.FOCUS + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.DISPLAY, menu.title(), left, y,
            Tokens.Color.INK_PRIMARY);
        Typeset.drawRight(painter, Tokens.Type.LABEL, "SERVER MENU  " + menu.rows() + "×9",
            content.right(), y + Tokens.Space.TIGHT, Tokens.Color.INK_TERTIARY);
        painter.hRule(content.x(), content.y() + HEADER_HEIGHT - Tokens.Space.SNUG,
            content.w(), Tokens.Color.LINE_STRONG);
    }

    private void details(Painter painter) {
        painter.hRule(detail.x(), detail.y(), detail.w(), Tokens.Color.LINE_HAIRLINE);
        MenuProtocol.Item item = currentItem();
        int y = detail.y() + Tokens.Space.COZY;
        if (item == null) {
            Typeset.draw(painter, Tokens.Type.BODY, "항목을 가리키면 설명을 볼 수 있습니다.",
                detail.x(), y, Tokens.Color.INK_TERTIARY);
            return;
        }
        Typeset.draw(painter, Tokens.Type.BODY_STRONG, item.label(), detail.x(), y,
            Tokens.Color.INK_PRIMARY);
        String copy = String.join(" · ", item.details());
        if (!copy.isBlank()) {
            Typeset.drawWrapped(painter, Tokens.Type.BODY, copy, detail.x(),
                y + Tokens.Type.BODY_STRONG.leading() + Tokens.Space.SNUG,
                detail.w() * 3 / 4, 2, Tokens.Color.INK_SECONDARY);
        }
        String action = actionHint(item);
        if (!action.isEmpty()) {
            Typeset.drawRight(painter, Tokens.Type.LABEL, action, detail.right(), y,
                Tokens.Color.ACCENT);
        }
    }

    private void footer(Painter painter) {
        int y = content.bottom() - FOOTER_HEIGHT + Tokens.Space.SNUG;
        Typeset.draw(painter, Tokens.Type.LABEL, "Tab 이동  ·  Enter 선택  ·  Esc 닫기",
            content.x(), y, Tokens.Color.INK_TERTIARY);
        String status = requestedAt > 0 ? "서버 응답 대기" : "서버 권한 검증";
        Typeset.drawRight(painter, Tokens.Type.LABEL, status, content.right(), y,
            requestedAt > 0 ? Tokens.Color.STATUS_WARN : Tokens.Color.STATUS_LIVE);
    }

    private List<ServerMenuTile> createTiles(List<MenuProtocol.Item> items) {
        List<ServerMenuTile> created = new ArrayList<>(items.size());
        items.stream().sorted(Comparator.comparingInt(MenuProtocol.Item::slot)).forEach(item ->
            created.add(new ServerMenuTile(item, () -> request(item))));
        return List.copyOf(created);
    }

    private MenuProtocol.Item currentItem() {
        Widget widget = surface.hovered() != null ? surface.hovered() : surface.held();
        return widget instanceof ServerMenuTile tile ? tile.item() : null;
    }

    private void request(MenuProtocol.Item item) {
        if (requestedAt > 0 || item.actions().isEmpty()) {
            return;
        }
        MenuProtocol.Click click = requestedClick(item);
        if (FullmoonChannel.requestMenuAction(menu.id(), menu.revision(), item.slot(), click)) {
            requestedAt = System.currentTimeMillis();
            tiles.forEach(tile -> tile.busy(true));
        }
    }

    private static MenuProtocol.Click requestedClick(MenuProtocol.Item item) {
        if (Minecraft.getInstance().hasShiftDown()
                && item.actions().contains(MenuProtocol.Click.SHIFT_LEFT)) {
            return MenuProtocol.Click.SHIFT_LEFT;
        }
        return item.actions().contains(MenuProtocol.Click.LEFT)
            ? MenuProtocol.Click.LEFT : item.actions().getFirst();
    }

    private static String actionHint(MenuProtocol.Item item) {
        boolean left = item.actions().contains(MenuProtocol.Click.LEFT);
        boolean shift = item.actions().contains(MenuProtocol.Click.SHIFT_LEFT);
        if (left && shift) {
            return "클릭  ·  Shift+클릭 보조 동작";
        }
        if (left) {
            return "클릭하여 실행";
        }
        if (shift) {
            return "Shift+클릭하여 실행";
        }
        return "읽기 전용";
    }
}
