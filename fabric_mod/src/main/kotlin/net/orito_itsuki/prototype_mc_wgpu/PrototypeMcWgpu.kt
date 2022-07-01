package net.orito_itsuki.prototype_mc_wgpu

import net.fabricmc.api.ModInitializer
import net.orito_itsuki.prototype_mc_wgpu.rust.WgpuRendererNative
import net.orito_itsuki.prototype_mc_wgpu.rust.WindowSize
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
object PrototypeMcWgpu: ModInitializer {
    const val MOD_ID = "prototype_mc_wgpu"
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        println("Hello Fabric world!")
        WgpuRendererNative.rustNative()
        WgpuRendererNative.initWindow()
    }
}
