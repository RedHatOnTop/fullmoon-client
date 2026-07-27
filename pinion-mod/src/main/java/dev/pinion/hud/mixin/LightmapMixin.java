package dev.pinion.hud.mixin;

import dev.pinion.hud.PinionKeys;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fullbright, done where the light actually comes from.
 *
 *  The brightness slider is not a way in: `gamma` only feeds the shader's
 *  BrightnessFactor, which blends the lit colour toward a curve, and the option
 *  rejects anything outside 0..1 anyway. Ambient is the floor every other term
 *  adds on top of, so white ambient is a lit world with the sky, block and
 *  darkness terms left alone. */
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void pinion$fullbright(LightmapRenderState state, float delta, CallbackInfo ci) {
        if (!PinionKeys.isFullbright()) {
            return;
        }
        state.ambientColor = LightmapRenderStateExtractor.WHITE;
        // the Darkness effect subtracts after ambient is added; it would win
        state.darknessEffectScale = 0.0f;
    }
}
