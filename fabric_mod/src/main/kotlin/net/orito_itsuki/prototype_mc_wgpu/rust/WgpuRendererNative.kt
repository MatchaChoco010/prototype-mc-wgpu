package net.orito_itsuki.prototype_mc_wgpu.rust

import fr.stardustenterprises.yanl.NativeLoader

object WgpuRendererNative {
    init {
        val nativeLoader = NativeLoader.Builder().build()
        nativeLoader.loadLibrary("wgpu_renderer")
    }

    external fun initWindow()

    external fun draw(command: WgpuDrawCommand)

    external fun getWindowSize(): Any
}