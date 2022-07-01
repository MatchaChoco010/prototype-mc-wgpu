package net.orito_itsuki.prototype_mc_wgpu.rust

import fr.stardustenterprises.yanl.NativeLoader

object WgpuRendererNative {
    init {
        val nativeLoader = NativeLoader.Builder().build()
        nativeLoader.loadLibrary("wgpu_renderer")
    }

    external fun rustNative()

    external fun initWindow()

    external fun draw()

    external fun getWindowSize(): Any
}