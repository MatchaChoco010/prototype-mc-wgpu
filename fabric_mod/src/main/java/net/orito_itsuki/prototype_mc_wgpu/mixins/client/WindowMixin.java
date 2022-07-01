package net.orito_itsuki.prototype_mc_wgpu.mixins.client;

import net.minecraft.client.util.Window;
import net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client.WindowMixinImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(at = @At("TAIL"), method = "swapBuffers")
    private void onSwapBuffers(CallbackInfo ci) {
        WindowMixinImpl.INSTANCE.onSwapBuffers((Window)(Object)this, ci);
    }

    @Inject(at = @At("HEAD"), method = "getWidth", cancellable = true)
    private void onGetWidth(CallbackInfoReturnable<Integer> ci) {
        WindowMixinImpl.INSTANCE.onGetWidth(ci);
    }

    @Inject(at = @At("HEAD"), method = "getHeight", cancellable = true)
    private void onGetHeight(CallbackInfoReturnable<Integer> ci) {
        WindowMixinImpl.INSTANCE.onGetHeight(ci);
    }

    @Inject(at = @At("HEAD"), method = "getScaledWidth", cancellable = true)
    private void onGetScaledWidth(CallbackInfoReturnable<Integer> ci) {
        WindowMixinImpl.INSTANCE.onGetScaledWidth((Window)(Object)this, ci);
    }

    @Inject(at = @At("HEAD"), method = "getScaledHeight", cancellable = true)
    private void onGetScaledHeight(CallbackInfoReturnable<Integer> ci) {
        WindowMixinImpl.INSTANCE.onGetScaledHeight((Window)(Object)this, ci);
    }
}
