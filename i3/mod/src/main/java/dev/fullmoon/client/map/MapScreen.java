package dev.fullmoon.client.map;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dev.fullmoon.client.design.Tokens;
import dev.fullmoon.client.layout.Box;
import dev.fullmoon.client.network.BridgeProtocol;
import dev.fullmoon.client.network.FullmoonChannel;
import dev.fullmoon.client.render.Painter;
import dev.fullmoon.client.render.Rgb;
import dev.fullmoon.client.text.Typeset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import com.mojang.blaze3d.platform.InputConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** North-up map of client-loaded terrain with server-owned route markers. */
public final class MapScreen extends Screen {
    private static final Logger LOG = LoggerFactory.getLogger("Fullmoon/Map");
    private static final int CELL_SIZE = 3;
    private static final int PAN_CELLS = 12;
    private static final int COMPACT_WIDTH = 700;
    private static final int HEADER_HEIGHT = 48;
    private static final int FOOTER_HEIGHT = 24;
    private static final int RAIL_WIDTH = 208;
    private static final int COMPACT_RAIL_WIDTH = 148;
    private static final int ROUTE_ROW_HEIGHT = 34;

    private final Screen parent;
    private MapViewport viewport;
    private TerrainSample terrain;
    private Box content = Box.EMPTY;
    private Box map = Box.EMPTY;
    private Box rail = Box.EMPTY;
    private List<RouteHit> routeHits = List.of();
    private String centredRouteId = "";
    private int ticksUntilRefresh;
    private boolean sampleFailed;

    public MapScreen(Screen parent) {
        super(Component.translatable("fullmoon.map.title"));
        this.parent = parent;
        Entity player = player();
        double x = player == null ? 0.0 : player.getX();
        double z = player == null ? 0.0 : player.getZ();
        viewport = new MapViewport(x, z, 2);
        terrain = blank(1, 1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void init() {
        int edge = width < COMPACT_WIDTH ? Tokens.Space.LOOSE : Tokens.Space.SECTION;
        content = new Box(edge, edge, width - edge * 2, height - edge * 2);
        Box body = Box.between(content.x(), content.y() + HEADER_HEIGHT,
            content.right(), content.bottom() - FOOTER_HEIGHT);
        int railWidth = width < COMPACT_WIDTH ? COMPACT_RAIL_WIDTH : RAIL_WIDTH;
        Box.Split columns = body.splitLeft(body.w() - railWidth - Tokens.Space.SECTION,
            Tokens.Space.SECTION);
        Box mapSlot = columns.head();
        map = new Box(mapSlot.x(), mapSlot.y(),
            Math.max(CELL_SIZE, mapSlot.w() / CELL_SIZE * CELL_SIZE),
            Math.max(CELL_SIZE, mapSlot.h() / CELL_SIZE * CELL_SIZE));
        rail = columns.rest();
        refreshTerrain();
    }

    @Override
    public void tick() {
        if (ticksUntilRefresh <= 0) {
            refreshTerrain();
            ticksUntilRefresh = 20;
        } else {
            ticksUntilRefresh--;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float partialTick) {
        Painter painter = new Painter(gfx);
        painter.blurredStratum();
        painter.fill(0, 0, painter.width(), painter.height(),
            Rgb.alpha(Tokens.Color.SURFACE_VOID, 0.91f));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
            float partialTick) {
        Painter painter = new Painter(gfx);
        header(painter);
        MapCanvas.draw(painter, map, CELL_SIZE, terrain, viewport,
            placedRoutes(), playerPoint(), true);
        rail(painter);
        footer(painter);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_LEFT) {
            pan(-PAN_CELLS, 0);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT) {
            pan(PAN_CELLS, 0);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            pan(0, -PAN_CELLS);
            return true;
        }
        if (key == InputConstants.KEY_DOWN) {
            pan(0, PAN_CELLS);
            return true;
        }
        if (key == InputConstants.KEY_R || key == InputConstants.KEY_HOME) {
            recenter();
            return true;
        }
        if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
            zoom(true, map.midX(), map.midY());
            return true;
        }
        if (key == InputConstants.KEY_MINUS) {
            zoom(false, map.midX(), map.midY());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX,
            double scrollY) {
        if (!map.holds(mouseX, mouseY) || scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        zoom(scrollY > 0.0, (int) mouseX, (int) mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            for (RouteHit hit : routeHits) {
                if (hit.bounds().holds(event.x(), event.y())) {
                    BridgeProtocol.Waypoint route = hit.route();
                    viewport = new MapViewport(route.x(), route.z(), viewport.blocksPerCell());
                    centredRouteId = route.id();
                    refreshTerrain();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void header(Painter painter) {
        int brandY = content.y();
        painter.fill(content.x(), Typeset.capTop(Tokens.Type.DISPLAY, brandY),
            Tokens.Stroke.FOCUS, Typeset.capHeight(Tokens.Type.DISPLAY), Tokens.Color.ACCENT);
        int textX = content.x() + Tokens.Stroke.FOCUS + Tokens.Space.COZY;
        Typeset.draw(painter, Tokens.Type.DISPLAY, "Fullmoon", textX, brandY,
            Tokens.Color.INK_PRIMARY);
        Typeset.draw(painter, Tokens.Type.LABEL, tr("subtitle"), textX,
            brandY + Tokens.Type.DISPLAY.leading(), Tokens.Color.INK_TERTIARY);
        Typeset.drawRight(painter, Tokens.Type.LABEL, tr("authority"), content.right(),
            brandY + Tokens.Space.TIGHT, Tokens.Color.INK_TERTIARY);
        painter.hRule(content.x(), content.y() + HEADER_HEIGHT - Tokens.Space.COZY,
            content.w(), Tokens.Color.LINE_STRONG);
    }

    private void rail(Painter painter) {
        painter.vRule(rail.x() - Tokens.Space.LOOSE, rail.y(), rail.h(),
            Tokens.Color.LINE_HAIRLINE);
        int y = section(painter, tr("position"), rail.y());
        drawFact(painter, tr("centre"), coordinate(viewport.centerX(), viewport.centerZ()), y);
        y += Tokens.Type.BODY.leading() + Tokens.Space.SNUG;
        drawFact(painter, tr("scale"), tr("scale.value", viewport.blocksPerCell()), y);
        y += Tokens.Type.BODY.leading() + Tokens.Space.SECTION;

        List<BridgeProtocol.Waypoint> routes = currentRoutes();
        y = section(painter, tr("routes", routes.size()), y);
        List<RouteHit> hits = new java.util.ArrayList<>();
        int bottom = rail.bottom() - Tokens.Space.GUTTER;
        for (BridgeProtocol.Waypoint route : routes) {
            if (y + ROUTE_ROW_HEIGHT > bottom) {
                break;
            }
            Box row = new Box(rail.x(), y, rail.w(), ROUTE_ROW_HEIGHT);
            drawRoute(painter, row, route);
            hits.add(new RouteHit(row, route));
            y += ROUTE_ROW_HEIGHT;
        }
        routeHits = List.copyOf(hits);
        if (routes.isEmpty()) {
            Typeset.drawWrapped(painter, Tokens.Type.BODY, tr("routes.empty"), rail.x(), y,
                rail.w(), 3, Tokens.Color.INK_TERTIARY);
        }
    }

    private void drawRoute(Painter painter, Box row, BridgeProtocol.Waypoint route) {
        boolean centred = route.id().equals(centredRouteId);
        if (centred) {
            painter.fill(row.x(), row.y(), row.w(), row.h(), Tokens.Color.ACCENT_WASH);
            painter.fill(row.x(), row.y(), Tokens.Stroke.FOCUS, row.h(), Tokens.Color.ACCENT);
        }
        int left = row.x() + (centred ? Tokens.Space.COZY : 0);
        String name = Typeset.fittingPrefix(Tokens.Type.BODY_STRONG, route.name(), row.w());
        Typeset.draw(painter, Tokens.Type.BODY_STRONG, name, left,
            row.y() + Tokens.Space.SNUG, Tokens.Color.INK_PRIMARY);
        Typeset.tabular(painter, Tokens.Type.LABEL, route.x() + "  " + route.z(), left,
            row.y() + Tokens.Space.SNUG + Tokens.Type.BODY.leading(),
            Tokens.Color.INK_TERTIARY);
        painter.hRule(row.x(), row.bottom() - Tokens.Stroke.HAIR, row.w(),
            Tokens.Color.LINE_HAIRLINE);
    }

    private void footer(Painter painter) {
        int y = content.bottom() - FOOTER_HEIGHT + Tokens.Space.COZY;
        painter.hRule(content.x(), content.bottom() - FOOTER_HEIGHT, content.w(),
            Tokens.Color.LINE_HAIRLINE);
        Typeset.draw(painter, Tokens.Type.LABEL, tr("footer.keys"), content.x(), y,
            Tokens.Color.INK_TERTIARY);
        int color = sampleFailed ? Tokens.Color.STATUS_DANGER : Tokens.Color.INK_TERTIARY;
        String status = sampleFailed
            ? tr("status.failed")
            : tr("status.coverage", terrain.snapshot().mappedPercent());
        Typeset.drawRight(painter, Tokens.Type.LABEL, status, content.right(), y, color);
    }

    private int section(Painter painter, String label, int y) {
        painter.fill(rail.x(), y + Tokens.Space.TIGHT, Tokens.Stroke.FOCUS,
            Tokens.Type.LABEL.px(), Tokens.Color.ACCENT);
        Typeset.draw(painter, Tokens.Type.LABEL, label,
            rail.x() + Tokens.Space.COZY, y, Tokens.Color.INK_TERTIARY);
        return y + Tokens.Type.LABEL.leading() + Tokens.Space.COZY;
    }

    private void drawFact(Painter painter, String label, String value, int y) {
        Typeset.draw(painter, Tokens.Type.LABEL, label, rail.x(), y,
            Tokens.Color.INK_TERTIARY);
        Typeset.tabularRight(painter, Tokens.Type.BODY_STRONG, value, rail.right(), y,
            Tokens.Color.INK_PRIMARY);
    }

    private void pan(int columns, int rows) {
        viewport = viewport.panCells(columns, rows);
        centredRouteId = "";
        refreshTerrain();
    }

    private void zoom(boolean in, int mouseX, int mouseY) {
        int column = Math.clamp((mouseX - map.x()) / CELL_SIZE, 0, terrain.snapshot().width() - 1);
        int row = Math.clamp((mouseY - map.y()) / CELL_SIZE, 0, terrain.snapshot().height() - 1);
        viewport = in
            ? viewport.zoomInAt(column, row, terrain.snapshot().width(), terrain.snapshot().height())
            : viewport.zoomOutAt(column, row, terrain.snapshot().width(), terrain.snapshot().height());
        centredRouteId = "";
        refreshTerrain();
    }

    private void recenter() {
        Entity player = player();
        if (player != null) {
            viewport = new MapViewport(player.getX(), player.getZ(), viewport.blocksPerCell());
            centredRouteId = "";
            refreshTerrain();
        }
    }

    private void refreshTerrain() {
        Minecraft client = Minecraft.getInstance();
        int columns = Math.max(1, map.w() / CELL_SIZE);
        int rows = Math.max(1, map.h() / CELL_SIZE);
        if (client.level == null) {
            terrain = blank(columns, rows);
            sampleFailed = false;
            return;
        }
        try {
            terrain = TerrainSampler.sample(client.level, viewport, columns, rows);
            sampleFailed = false;
        } catch (RuntimeException error) {
            terrain = blank(columns, rows);
            sampleFailed = true;
            LOG.error("Failed to sample client-loaded terrain", error);
        }
        ticksUntilRefresh = 20;
    }

    private List<MapMarkers.Placed> placedRoutes() {
        List<MapMarkers.Marker> markers = currentRoutes().stream()
            .map(route -> new MapMarkers.Marker(
                route.id(), route.name(), route.x(), route.z()))
            .toList();
        return MapMarkers.place(markers, viewport,
            terrain.snapshot().width(), terrain.snapshot().height());
    }

    private MapViewport.GridPoint playerPoint() {
        Entity player = player();
        return player == null
            ? new MapViewport.GridPoint(-1.0, -1.0)
            : viewport.project(player.getX(), player.getZ(),
                terrain.snapshot().width(), terrain.snapshot().height());
    }

    private static List<BridgeProtocol.Waypoint> currentRoutes() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return List.of();
        }
        String dimension = client.level.dimension().identifier().toString();
        return FullmoonChannel.waypoints().stream()
            .filter(route -> WorldNames.matches(route.world(), dimension))
            .toList();
    }

    private static Entity player() {
        Minecraft client = Minecraft.getInstance();
        return client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
    }

    private static TerrainSample blank(int width, int height) {
        TerrainSnapshot.Cell cell = TerrainSnapshot.Cell.unmapped(Tokens.Color.SURFACE_SUNKEN);
        TerrainSnapshot snapshot = new TerrainSnapshot(width, height,
            Collections.nCopies(Math.multiplyExact(width, height), cell));
        return TerrainSample.of(snapshot);
    }

    private static String coordinate(double x, double z) {
        return String.format(Locale.ROOT, "%.0f  %.0f", x, z);
    }

    private static String tr(String key, Object... args) {
        return I18n.get("fullmoon.map." + key, args);
    }

    private record RouteHit(Box bounds, BridgeProtocol.Waypoint route) {}
}
