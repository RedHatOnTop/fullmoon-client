package dev.pinion.hud.mixin;

import dev.pinion.hud.Cosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** The client-side cape.
 *
 *  Client-side means exactly that: the patch is applied to the local player's
 *  render state only, so nobody on the server sees a cape the server never
 *  agreed to. The cape layer draws whatever texture the skin carries, so
 *  swapping it here needs no second layer and keeps vanilla's cape physics. */
@Mixin(AvatarRenderer.class)
public class AvatarRenderMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("RETURN"))
    private void pinion$cape(Avatar entity, AvatarRenderState state, float delta, CallbackInfo ci) {
        Cosmetics.poll();
        ClientAsset.ResourceTexture cape = Cosmetics.cape();
        if (cape == null || entity != Minecraft.getInstance().player) {
            return;
        }
        state.skin = state.skin.with(PlayerSkin.Patch.create(
                Optional.empty(), Optional.of(cape), Optional.empty(), Optional.empty()));
    }
}
