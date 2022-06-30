package net.orito_itsuki.prototype_mc_wgpu

import net.fabricmc.api.ModInitializer

@Suppress("UNUSED")
object PrototypeMcWgpu: ModInitializer {
    override fun onInitialize() {
        println("Hello Fabric world!")
        WgpuRendererNative.rustNative()
    }
}
