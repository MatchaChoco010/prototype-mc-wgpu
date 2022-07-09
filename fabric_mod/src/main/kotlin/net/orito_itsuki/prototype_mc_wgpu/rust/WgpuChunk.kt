package net.orito_itsuki.prototype_mc_wgpu.rust

import net.orito_itsuki.prototype_mc_wgpu.resource.Resource

class WgpuChunk(
    val origin: ChunkOrigin,
    val resources: java.util.ArrayList<Resource>,
    val minecraftMeshes: java.util.ArrayList<WgpuMinecraftMesh>,
) {
    class ChunkId(val x: Int, val z: Int)
    class ChunkOrigin(val x: Int, val y: Int, val z: Int)
}
