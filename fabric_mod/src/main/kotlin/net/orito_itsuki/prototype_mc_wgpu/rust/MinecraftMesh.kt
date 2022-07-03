package net.orito_itsuki.prototype_mc_wgpu.rust

class MinecraftMesh(
    val entityType: String,
    val vertexFormat: String,
    val textureName: String,
    var startVertices: Long,
    var endVertices: Long,
    var startIndices: Long,
    var endIndices: Long,
)