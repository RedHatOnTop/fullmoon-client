package dev.fullmoon.client.map;

import java.util.List;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

import org.joml.Matrix3x2fc;

/** One GUI render state for every mapped run in a terrain raster. */
public record TerrainRenderState(
    Matrix3x2fc pose,
    int x,
    int y,
    int cellSize,
    List<TerrainSnapshot.Run> runs,
    ScreenRectangle scissorArea,
    ScreenRectangle bounds
) implements GuiElementRenderState {
    public TerrainRenderState(
            Matrix3x2fc pose, int x, int y, int cellSize,
            List<TerrainSnapshot.Run> runs, int width, int height,
            ScreenRectangle scissorArea) {
        this(pose, x, y, cellSize, List.copyOf(runs), scissorArea,
            bounds(x, y, cellSize, width, height, pose, scissorArea));
    }

    @Override
    public void buildVertices(VertexConsumer consumer) {
        for (TerrainSnapshot.Run run : runs) {
            if (run.mapped()) {
                emit(consumer, run);
            }
        }
    }

    private void emit(VertexConsumer consumer, TerrainSnapshot.Run run) {
        int x0 = x + run.column() * cellSize;
        int y0 = y + run.row() * cellSize;
        int x1 = x0 + run.length() * cellSize;
        int y1 = y0 + cellSize;
        consumer.addVertexWith2DPose(pose, x0, y0).setColor(run.color());
        consumer.addVertexWith2DPose(pose, x0, y1).setColor(run.color());
        consumer.addVertexWith2DPose(pose, x1, y1).setColor(run.color());
        consumer.addVertexWith2DPose(pose, x1, y0).setColor(run.color());
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    private static ScreenRectangle bounds(int x, int y, int cellSize, int width, int height,
            Matrix3x2fc pose, ScreenRectangle scissor) {
        ScreenRectangle area = new ScreenRectangle(
            x, y, width * cellSize, height * cellSize).transformMaxBounds(pose);
        return scissor == null ? area : scissor.intersection(area);
    }
}
