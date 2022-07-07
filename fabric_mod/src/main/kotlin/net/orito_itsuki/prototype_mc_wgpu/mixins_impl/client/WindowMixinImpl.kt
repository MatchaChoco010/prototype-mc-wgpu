package net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client

import net.minecraft.client.util.Window
import net.orito_itsuki.prototype_mc_wgpu.PrototypeMcWgpu
import net.orito_itsuki.prototype_mc_wgpu.rust.*
import org.lwjgl.glfw.GLFW
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
import java.net.URLDecoder

object WindowMixinImpl {
    private var width = 0
    private var height = 0

    fun onSwapBuffers(window: Window, ci: CallbackInfo) {
        val ws = WgpuRendererNative.getWindowSize() as WindowSize
        width = ws.innerWidth
        height = ws.innerHeight
        GLFW.glfwSetWindowSize(window.handle, width, height)

        WgpuRendererNative.draw(WgpuDrawCommand(MinecraftWorld, WgpuCamera, FileExtractor.isFinishFileExtract))
    }

    fun onGetWidth(ci: CallbackInfoReturnable<Int>) {
        ci.returnValue = width
    }

    fun onGetHeight(ci: CallbackInfoReturnable<Int>) {
        ci.returnValue = height
    }

    fun onGetScaledWidth(window: Window, ci: CallbackInfoReturnable<Int>) {
        val w = (width.toDouble() / window.scaleFactor).toInt()
        val scaledWidth = if (width.toDouble() / window.scaleFactor > w.toDouble()) w + 1 else w
        ci.returnValue = scaledWidth
    }

    fun onGetScaledHeight(window: Window, ci: CallbackInfoReturnable<Int>) {
        val h = (height.toDouble() / window.scaleFactor).toInt()
        val scaledHeight = if (height.toDouble() / window.scaleFactor > h.toDouble()) h + 1 else h
        ci.returnValue = scaledHeight
    }
}