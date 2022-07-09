package net.orito_itsuki.prototype_mc_wgpu.rust

import net.orito_itsuki.prototype_mc_wgpu.resource.TextureResource

class WgpuMinecraftMesh(
    val textureName: TextureResource,
    val transparent: Boolean,
    var startVertices: Long,
    var endVertices: Long,
    var startIndices: Long,
    var endIndices: Long,
)