package net.orito_itsuki.prototype_mc_wgpu.rust

import net.orito_itsuki.prototype_mc_wgpu.chunk.RenderChunksManager
import java.nio.ByteBuffer

private const val BYTE_BUFFER_AMOUNT = 128 * 1024
private const val minY = -64

class WgpuCommand {
    // Minecraft vertex data
    var vertices = java.util.ArrayList<ByteBuffer>()
    var indices = java.util.ArrayList<ByteBuffer>()
    var verticesCurrentBytes: Long = 0
    var indicesCurrentBytes: Long = 0

    // Chunk data
    var activeChunks = java.util.ArrayList<WgpuChunk.ChunkId>()
    var chunks = java.util.ArrayList<WgpuChunk>()

    // flip済みのsrcをverticesに追加する
    private fun appendVertices(src: ByteBuffer) {
        verticesCurrentBytes += src.remaining()

        val last = vertices.lastOrNull()
        if (last != null) {
            if (last.remaining() >= src.remaining()) {
                last.put(src)
            } else {
                val splitIndex = last.remaining()
                val srcLength = src.remaining()
                last.put(src.slice(0, splitIndex))
                vertices.add(ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT))
                vertices.last().put(src.slice(splitIndex, srcLength))
            }
        } else {
            vertices.add(ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT))
            vertices.last().put(src)
        }
    }

    // flip済みのsrcをindicesに追加する
    private fun appendIndices(src: ByteBuffer) {
        indicesCurrentBytes += src.remaining()

        val last = indices.lastOrNull()
        if (last != null) {
            if (last.remaining() >= src.remaining()) {
                last.put(src)
            } else {
                val splitIndex = last.remaining()
                val srcLength = src.remaining()
                last.put(src.slice(0, splitIndex))
                indices.add(ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT))
                indices.last().put(src.slice(splitIndex, srcLength))
            }
        } else {
            indices.add(ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT))
            indices.last().put(src)
        }
    }

    fun clear() {
        verticesCurrentBytes = 0
        indicesCurrentBytes = 0
        activeChunks = java.util.ArrayList<WgpuChunk.ChunkId>()
        chunks = java.util.ArrayList<WgpuChunk>()
    }

    fun loadRenderChunks(renderChunks: RenderChunksManager.RenderChunks) {
        // active chunks
        for (ac in renderChunks.activeChunks) {
            activeChunks.add(WgpuChunk.ChunkId(ac.x, ac.z))
        }

        // chunks
        for (chunk in renderChunks.renderChunksList) {
            val origin = WgpuChunk.ChunkOrigin(chunk.originX, minY, chunk.originZ)
            val resources = java.util.ArrayList(chunk.resources)
            val meshes = chunk.meshes
            val vbs = chunk.vertices
            val ibs = chunk.indices

            val ms = java.util.ArrayList<WgpuMinecraftMesh>()
            val offsetVertices = verticesCurrentBytes
            val offsetIndices = indicesCurrentBytes
            for (mesh in meshes) {
                val startVertices = mesh.startVertices + offsetVertices
                val startIndices = mesh.startIndices + offsetIndices
                val endVertices = mesh.endVertices + offsetVertices
                val endIndices = mesh.endIndices + offsetIndices
                ms.add(WgpuMinecraftMesh(mesh.texture, mesh.transparent, startVertices, startIndices, endVertices, endIndices))
            }

            for (vb in vbs) {
                appendVertices(vb)
            }
            for (ib in ibs) {
                appendIndices(ib)
            }

            chunks.add(WgpuChunk(origin, resources, ms))
        }
    }
}