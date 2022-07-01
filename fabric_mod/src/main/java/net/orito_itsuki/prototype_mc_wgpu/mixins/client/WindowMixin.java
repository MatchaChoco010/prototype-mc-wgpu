package net.orito_itsuki.prototype_mc_wgpu.mixins.client;

import net.minecraft.client.util.Window;
import net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client.WindowMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(at = @At("TAIL"), method = "swapBuffers")
    private void onSwapBuffers(CallbackInfo ci) {
        WindowMixinImpl.INSTANCE.onSwapBuffers(ci);
    }
}
