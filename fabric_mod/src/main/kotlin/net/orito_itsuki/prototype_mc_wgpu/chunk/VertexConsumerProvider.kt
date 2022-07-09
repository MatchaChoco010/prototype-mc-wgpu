package net.orito_itsuki.prototype_mc_wgpu.chunk

import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.orito_itsuki.prototype_mc_wgpu.resource.Resource
import net.orito_itsuki.prototype_mc_wgpu.resource.TextureResource
import java.nio.ByteBuffer
import java.nio.ByteOrder

private const val BYTE_BUFFER_AMOUNT = 128 * 1024

class VertexConsumerProvider {
    private val vertices = mutableListOf<ByteBuffer>()
    private val indices = mutableListOf<ByteBuffer>()
    var verticesCurrentBytes: Long = 0
    var indicesCurrentBytes: Long = 0
    private val meshes = mutableListOf<MinecraftBlockMesh>()
    private val resources = mutableListOf<Resource>()

    private fun addFloatToVertices(f: Float) {
        verticesCurrentBytes += 4
        if (vertices.size < verticesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocate(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            vertices.add(buffer)
        }
        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putFloat(f)
    }

    private fun addIntToVertices(i: Int) {
        verticesCurrentBytes += 4
        if (vertices.size < verticesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            vertices.add(buffer)
        }
        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(i)
    }

    private fun addIntToIndices(i: Int) {
        indicesCurrentBytes += 4
        if (indices.size < indicesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            indices.add(buffer)
        }
        indices[(indicesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(i)
    }

    fun createMinecraftBlockMesh(drawMode: VertexFormat.DrawMode, vertexFormat: VertexFormat, texture: TextureResource, transparent: Boolean): VertexConsumer {
        resources.add(texture)
        val mesh = MinecraftBlockMesh(texture, transparent)
        meshes.add(mesh)
        return VertexConsumer(this, mesh, drawMode, vertexFormat)
    }

    fun takeVertices(): List<ByteBuffer> {
        for (b in vertices) {
            b.flip()
        }
        return vertices
    }

    fun takeIndices(): List<ByteBuffer> {
        for (b in indices) {
            b.flip()
        }
        return indices
    }

    fun takeMeshes(): List<MinecraftBlockMesh> {
        return meshes
    }

    fun takeResources(): List<Resource> {
        return resources
    }

    class VertexConsumer(
        private val provider: VertexConsumerProvider,
        private val mesh: MinecraftBlockMesh,
        private val drawMode: VertexFormat.DrawMode,
        private val vertexFormat: VertexFormat,
        private var vertexCount: Int = 0,
    ): net.minecraft.client.render.VertexConsumer {

        init {
            mesh.startVertices = provider.verticesCurrentBytes
            mesh.startIndices = provider.indicesCurrentBytes
            mesh.endVertices = provider.verticesCurrentBytes
            mesh.endIndices = provider.indicesCurrentBytes
        }

        private var vd: Double? = null
        private var ve: Double? = null
        private var vf: Double? = null
        private var ci: Int? = null
        private var cj: Int? = null
        private var ck: Int? = null
        private var tf: Float? = null
        private var tg: Float? = null
        private var oi: Int? = null
        private var oj: Int? = null
        private var li: Int? = null
        private var lj: Int? = null
        private var nf: Float? = null
        private var ng: Float? = null
        private var nh: Float? = null
        private var fci: Int? = null
        private var fcj: Int? = null
        private var fck: Int? = null
        private var fcl: Int? = null

        override fun vertex(d: Double, e: Double, f: Double): VertexConsumer {
            vd = d
            ve = e
            vf = f
            return this
        }

        override fun color(i: Int, j: Int, k: Int, l: Int): VertexConsumer {
            ci = i
            cj = j
            ck = k
            return this
        }

        override fun texture(f: Float, g: Float): VertexConsumer {
            tf = f
            tg = g
            return this
        }

        override fun overlay(i: Int, j: Int): VertexConsumer {
            oi = i
            oj = j
            return this
        }

        override fun light(i: Int, j: Int): VertexConsumer {
            li = i
            lj = j
            return this
        }

        override fun normal(f: Float, g: Float, h: Float): VertexConsumer {
            nf = f
            ng = g
            nh = h
            return this
        }

        override fun fixedColor(i: Int, j: Int, k: Int, l: Int) {
            // 調査
            fci = i
            fcj = j
            fck = k
            fcl = l
        }

        override fun unfixColor() {
            // 調査
        }

        override fun next() {
            when(vertexFormat to drawMode) {
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL to VertexFormat.DrawMode.QUADS -> {
                    if (vd == null || ve == null || vf == null || ci == null || cj == null || ck == null ||
                        tf == null || tg == null || li == null || lj == null || nf == null || ng == null || nh == null) {
                        throw Exception("Not all vertex infos are provided")
                    }

                    provider.addFloatToVertices(vd!!.toFloat())
                    provider.addFloatToVertices(ve!!.toFloat())
                    provider.addFloatToVertices(vf!!.toFloat())
                    provider.addFloatToVertices(ci!!.toFloat() / 255F)
                    provider.addFloatToVertices(cj!!.toFloat() / 255F)
                    provider.addFloatToVertices(ck!!.toFloat() / 255F)
                    provider.addFloatToVertices(tf!!)
                    provider.addFloatToVertices(tg!!)
                    provider.addIntToVertices(li!!)
                    provider.addIntToVertices(lj!!)
                    provider.addFloatToVertices(nf!!)
                    provider.addFloatToVertices(ng!!)
                    provider.addFloatToVertices(nh!!)

                    vd = null
                    ve = null
                    vf = null
                    ci = null
                    cj = null
                    ck = null
                    tf = null
                    tg = null
                    li = null
                    lj = null
                    nf = null
                    ng = null
                    nh = null

                    if (vertexCount % 4 == 3) { // Quad
                        provider.addIntToIndices(vertexCount - 3)
                        provider.addIntToIndices(vertexCount - 2)
                        provider.addIntToIndices(vertexCount - 1)
                        provider.addIntToIndices(vertexCount - 1)
                        provider.addIntToIndices(vertexCount - 0)
                        provider.addIntToIndices(vertexCount - 3)
                    }
                }
                else -> {
                    provider.addFloatToVertices(vd!!.toFloat())
                    provider.addFloatToVertices(ve!!.toFloat())
                    provider.addFloatToVertices(vf!!.toFloat())
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)
                    provider.addIntToVertices(0)
                    provider.addIntToVertices(0)
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)
                    provider.addFloatToVertices(0F)


                    if (vertexCount % 4 == 3) { // Quad
                        provider.addIntToIndices(vertexCount - 3)
                        provider.addIntToIndices(vertexCount - 2)
                        provider.addIntToIndices(vertexCount - 1)
                        provider.addIntToIndices(vertexCount - 1)
                        provider.addIntToIndices(vertexCount - 0)
                        provider.addIntToIndices(vertexCount - 3)
                    }
                }
            }

            vertexCount += 1
            mesh.endVertices = provider.verticesCurrentBytes
            mesh.endIndices = provider.indicesCurrentBytes
        }
    }

    class MinecraftBlockMesh(
        val texture: TextureResource,
        val transparent: Boolean,
        var startVertices: Long = 0,
        var startIndices: Long = 0,
        var endVertices: Long = 0,
        var endIndices: Long = 0,
    )
}