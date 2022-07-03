package net.orito_itsuki.prototype_mc_wgpu.rust

import net.minecraft.client.render.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

object MinecraftWorld {
    private const val BYTE_BUFFER_AMOUNT = 128 * 1024

    var vertices = java.util.ArrayList<ByteBuffer>()
    var indices = java.util.ArrayList<ByteBuffer>()
    private var verticesFloat = java.util.ArrayList<FloatBuffer>()
    var verticesInt = java.util.ArrayList<IntBuffer>()
    var indicesInt = java.util.ArrayList<IntBuffer>()
    var verticesCurrentBytes: Long = 0
    var indicesCurrentBytes: Long = 0

    var meshes = java.util.ArrayList<MinecraftMesh>()

    private fun addFloatToVertices(f: Float) {
        verticesCurrentBytes += 4
        if (vertices.size < verticesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            println(buffer.limit())
            vertices.add(buffer)
//            verticesFloat.add(buffer.asFloatBuffer())
//            verticesInt.add(buffer.asIntBuffer())
        }
//        verticesFloat[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].put((verticesCurrentBytes % BYTE_BUFFER_AMOUNT / 4).toInt(), f)
//        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putFloat(f)
//        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putFloat(7F)
        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putFloat(f)
//        println("${vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].position()}/${vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].limit()}")
    }

    private fun addIntToVertices(i: Int) {
        verticesCurrentBytes += 4
        if (vertices.size < verticesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            vertices.add(buffer)
//            verticesFloat.add(buffer.asFloatBuffer())
//            verticesInt.add(buffer.asIntBuffer())
        }
//        verticesInt[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].put((verticesCurrentBytes % BYTE_BUFFER_AMOUNT / 4).toInt(), i)
//        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(i)
//        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(7)
        vertices[(verticesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(i)
    }

    private fun addIntToIndices(i: Int) {
        indicesCurrentBytes += 4
        if (indices.size < indicesCurrentBytes / BYTE_BUFFER_AMOUNT + 1) {
            val buffer = ByteBuffer.allocateDirect(BYTE_BUFFER_AMOUNT).order(ByteOrder.LITTLE_ENDIAN)
            indices.add(buffer)
//            indicesInt.add(buffer.asIntBuffer())
        }
//        indicesInt[(indicesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].put((indicesCurrentBytes % BYTE_BUFFER_AMOUNT / 4).toInt(), i)
//        indices[(indicesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putChar(i.toChar())
        indices[(indicesCurrentBytes / BYTE_BUFFER_AMOUNT).toInt()].putInt(i)
    }

    fun resetIndices() {
        verticesCurrentBytes = 0
        indicesCurrentBytes = 0
        meshes = java.util.ArrayList<MinecraftMesh>()
    }

    fun clearBuffers() {
        for (buffer in vertices) {
            buffer.clear()
        }
        for (buffer in indices) {
            buffer.clear()
        }
    }

    fun getVertexConsumerProvider(entityType: String): VertexConsumerProvider {
//        PrototypeMcWgpu.LOGGER.info(entityType)
        return RustVertexConsumerProvider(entityType)
    }

    class RustVertexConsumerProvider(private val entityType: String): VertexConsumerProvider {
        override fun getBuffer(renderLayer: RenderLayer): VertexConsumer {
            val texture = (renderLayer as RenderLayer.MultiPhase).phases.texture
            val mesh = MinecraftMesh(entityType, texture.id.get().toString(), renderLayer.vertexFormat.toString(), 0, 0, 0, 0)
            meshes.add(mesh)
            return RustVertexConsumer(renderLayer.vertexFormat, renderLayer.drawMode, mesh)
        }
    }

    class RustVertexConsumer(
        private val vertexFormat: VertexFormat,
        private val drawMode: VertexFormat.DrawMode,
        private val mesh: MinecraftMesh,
    ): VertexConsumer {

        init {
            mesh.startVertices = MinecraftWorld.verticesCurrentBytes
            mesh.startIndices = MinecraftWorld.indicesCurrentBytes
            mesh.endVertices = MinecraftWorld.verticesCurrentBytes
            mesh.endIndices = MinecraftWorld.indicesCurrentBytes
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
        private var count = 0

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

        override fun next() {
            when(vertexFormat to drawMode) {
                VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL to VertexFormat.DrawMode.QUADS -> {
                    addFloatToVertices(vd!!.toFloat())
                    addFloatToVertices(ve!!.toFloat())
                    addFloatToVertices(vf!!.toFloat())
                    addFloatToVertices(ci!!.toFloat() / 255F)
                    addFloatToVertices(cj!!.toFloat() / 255F)
                    addFloatToVertices(ck!!.toFloat() / 255F)
                    addFloatToVertices(tf!!)
                    addFloatToVertices(tg!!)
                    addIntToVertices(li!!)
                    addIntToVertices(lj!!)
                    addFloatToVertices(nf!!)
                    addFloatToVertices(ng!!)
                    addFloatToVertices(nh!!)
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

                    if (count % 4 == 3) { // Quad
                        addIntToIndices(count - 3)
                        addIntToIndices(count - 2)
                        addIntToIndices(count - 1)
                        addIntToIndices(count - 1)
                        addIntToIndices(count - 0)
                        addIntToIndices(count - 3)
                    }
                }
                else -> {
//                    PrototypeMcWgpu.LOGGER.error("Unknown vertex format or draw mode")


                    addFloatToVertices(vd!!.toFloat())
                    addFloatToVertices(ve!!.toFloat())
                    addFloatToVertices(vf!!.toFloat())
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)
                    addIntToVertices(0)
                    addIntToVertices(0)
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)
                    addFloatToVertices(0F)


                    if (count % 4 == 3) { // Quad
                        addIntToIndices(count - 3)
                        addIntToIndices(count - 2)
                        addIntToIndices(count - 1)
                        addIntToIndices(count - 1)
                        addIntToIndices(count - 0)
                        addIntToIndices(count - 3)
                    }
                }
            }
            count++

            mesh.endVertices = verticesCurrentBytes
            mesh.endIndices = indicesCurrentBytes
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
    }
}