package dev.pinion.hud.mixin;

import dev.pinion.hud.PinionClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void pinion$countClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }
        switch (info.button()) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> PinionClient.LEFT_CLICKS.hit();
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> PinionClient.RIGHT_CLICKS.hit();
            default -> {
            }
        }
    }
}
