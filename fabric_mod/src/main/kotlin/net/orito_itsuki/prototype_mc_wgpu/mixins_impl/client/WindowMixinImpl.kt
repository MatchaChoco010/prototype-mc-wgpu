package net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client

import net.orito_itsuki.prototype_mc_wgpu.rust.WgpuRendererNative
import net.orito_itsuki.prototype_mc_wgpu.rust.WindowSize
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object WindowMixinImpl {
    fun onSwapBuffers(ci: CallbackInfo) {
        WgpuRendererNative.draw()
        val ws = WgpuRendererNative.getWindowSize() as WindowSize
        println("window size: ${ws.innerWidth}, ${ws.innerHeight}")
    }
}