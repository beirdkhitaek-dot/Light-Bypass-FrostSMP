package lastfmhud.mixin;

import lastfmhud.HudRenderer;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void lastfmhud$drag(long window, MouseInput input, int action, CallbackInfo ci) {
        if (input.button() != 0) return;
        if (HudRenderer.mouseButton(action)) ci.cancel();
    }
}
