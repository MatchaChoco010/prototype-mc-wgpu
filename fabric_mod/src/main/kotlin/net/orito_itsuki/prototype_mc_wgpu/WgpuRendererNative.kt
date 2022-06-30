package net.orito_itsuki.prototype_mc_wgpu

import fr.stardustenterprises.yanl.NativeLoader

object WgpuRendererNative {
    init {
        val nativeLoader = NativeLoader.Builder().build()
        nativeLoader.loadLibrary("wgpu_renderer")
    }

    external fun rustNative()
}