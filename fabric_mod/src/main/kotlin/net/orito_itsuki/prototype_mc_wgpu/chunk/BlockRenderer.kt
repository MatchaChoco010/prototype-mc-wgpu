package net.orito_itsuki.prototype_mc_wgpu.chunk

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.WorldRenderer
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.crash.CrashException
import net.minecraft.util.crash.CrashReport
import net.minecraft.util.crash.CrashReportSection
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.random.Random
import net.minecraft.world.BlockRenderView
import java.util.*

object BlockRenderer {
    fun render(
        blockRenderView: BlockRenderView,
        bakedModel: BakedModel,
        blockState: BlockState,
        blockPos: BlockPos,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        bl: Boolean,
        random: Random,
        l: Long,
        i: Int
    ) {
        val bl2 =
            MinecraftClient.isAmbientOcclusionEnabled() && (blockState.luminance == 0) && bakedModel.useAmbientOcclusion()
        val vec3d = blockState.getModelOffset(blockRenderView, blockPos)
        matrixStack.translate(vec3d.x, vec3d.y, vec3d.z)
        try {
            if (bl2) {
                this.renderSmooth(
                    blockRenderView,
                    bakedModel,
                    blockState,
                    blockPos,
                    matrixStack,
                    vertexConsumer,
                    bl,
                    random,
                    l,
                    i
                )
            } else {
                this.renderFlat(
                    blockRenderView,
                    bakedModel,
                    blockState,
                    blockPos,
                    matrixStack,
                    vertexConsumer,
                    bl,
                    random,
                    l,
                    i
                )
            }
        } catch (throwable: Throwable) {
            val crashReport = CrashReport.create(throwable, "Tesselating block model")
            val crashReportSection = crashReport.addElement("Block model being tesselated")
            CrashReportSection.addBlockInfo(crashReportSection, blockRenderView, blockPos, blockState)
            crashReportSection.add("Using AO", bl2)
            throw CrashException(crashReport)
        }
    }

    fun renderSmooth(
        blockRenderView: BlockRenderView,
        bakedModel: BakedModel,
        blockState: BlockState,
        blockPos: BlockPos,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        bl: Boolean,
        random: Random,
        l: Long,
        i: Int
    ) {
        val fs = FloatArray(DIRECTIONS.size * 2)
        val bitSet = BitSet(3)
        val ambientOcclusionCalculator = AmbientOcclusionCalculator()
        val mutable = blockPos.mutableCopy()
        for (direction in DIRECTIONS) {
            random.setSeed(l)
            val list = bakedModel.getQuads(blockState, direction, random)
            if (list.isEmpty()) continue
            mutable[blockPos] = direction
            if (bl && !Block.shouldDrawSide(blockState, blockRenderView, blockPos, direction, mutable)) continue
            renderQuadsSmooth(
                blockRenderView,
                blockState,
                blockPos,
                matrixStack,
                vertexConsumer,
                list,
                fs,
                bitSet,
                ambientOcclusionCalculator,
                i
            )
        }
        random.setSeed(l)
        val list2 = bakedModel.getQuads(blockState, null, random)
        if (!list2.isEmpty()) {
            renderQuadsSmooth(
                blockRenderView,
                blockState,
                blockPos,
                matrixStack,
                vertexConsumer,
                list2,
                fs,
                bitSet,
                ambientOcclusionCalculator,
                i
            )
        }
    }

    fun renderFlat(
        blockRenderView: BlockRenderView,
        bakedModel: BakedModel,
        blockState: BlockState,
        blockPos: BlockPos,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        bl: Boolean,
        random: Random,
        l: Long,
        i: Int
    ) {
        val bitSet = BitSet(3)
        val mutable = blockPos.mutableCopy()
        for (direction in DIRECTIONS) {
            random.setSeed(l)
            val list = bakedModel.getQuads(blockState, direction, random)
            if (list.isEmpty()) continue
            mutable[blockPos] = direction
            if (bl && !Block.shouldDrawSide(blockState, blockRenderView, blockPos, direction, mutable)) continue
            val j = WorldRenderer.getLightmapCoordinates(blockRenderView, blockState, mutable)
            renderQuadsFlat(
                blockRenderView,
                blockState,
                blockPos,
                j,
                i,
                false,
                matrixStack,
                vertexConsumer,
                list,
                bitSet
            )
        }
        random.setSeed(l)
        val list2 = bakedModel.getQuads(blockState, null, random)
        if (!list2.isEmpty()) {
            renderQuadsFlat(
                blockRenderView,
                blockState,
                blockPos,
                -1,
                i,
                true,
                matrixStack,
                vertexConsumer,
                list2,
                bitSet
            )
        }
    }

    private fun renderQuadsSmooth(
        blockRenderView: BlockRenderView,
        blockState: BlockState,
        blockPos: BlockPos,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        list: List<BakedQuad>,
        fs: FloatArray,
        bitSet: BitSet,
        ambientOcclusionCalculator: AmbientOcclusionCalculator,
        i: Int
    ) {
        for (bakedQuad in list) {
            getQuadDimensions(blockRenderView, blockState, blockPos, bakedQuad.vertexData, bakedQuad.face, fs, bitSet)
            ambientOcclusionCalculator.apply(
                blockRenderView,
                blockState,
                blockPos,
                bakedQuad.face,
                fs,
                bitSet,
                bakedQuad.hasShade()
            )
            renderQuad(
                blockRenderView, blockState, blockPos, vertexConsumer, matrixStack.peek(), bakedQuad,
                ambientOcclusionCalculator.brightness[0],
                ambientOcclusionCalculator.brightness[1],
                ambientOcclusionCalculator.brightness[2],
                ambientOcclusionCalculator.brightness[3],
                ambientOcclusionCalculator.light[0],
                ambientOcclusionCalculator.light[1],
                ambientOcclusionCalculator.light[2], ambientOcclusionCalculator.light[3], i
            )
        }
    }

    private fun renderQuad(
        blockRenderView: BlockRenderView,
        blockState: BlockState,
        blockPos: BlockPos,
        vertexConsumer: VertexConsumer,
        entry: MatrixStack.Entry,
        bakedQuad: BakedQuad,
        f: Float,
        g: Float,
        h: Float,
        i: Float,
        j: Int,
        k: Int,
        l: Int,
        m: Int,
        n: Int
    ) {
        val r: Float
        val q: Float
        val p: Float
        if (bakedQuad.hasColor()) {
            val o: Int = this.colors.getColor(blockState, blockRenderView, blockPos, bakedQuad.colorIndex)
            p = (o shr 16 and 0xFF).toFloat() / 255.0f
            q = (o shr 8 and 0xFF).toFloat() / 255.0f
            r = (o and 0xFF).toFloat() / 255.0f
        } else {
            p = 1.0f
            q = 1.0f
            r = 1.0f
        }
        vertexConsumer.quad(entry, bakedQuad, floatArrayOf(f, g, h, i), p, q, r, intArrayOf(j, k, l, m), n, true)
    }

    private fun getQuadDimensions(
        blockRenderView: BlockRenderView,
        blockState: BlockState,
        blockPos: BlockPos,
        `is`: IntArray,
        direction: Direction,
        fs: FloatArray?,
        bitSet: BitSet
    ) {
        var m: Float
        var l: Int
        var f = 32.0f
        var g = 32.0f
        var h = 32.0f
        var i = -32.0f
        var j = -32.0f
        var k = -32.0f
        l = 0
        while (l < 4) {
            m = java.lang.Float.intBitsToFloat(`is`[l * 8])
            val n = java.lang.Float.intBitsToFloat(`is`[l * 8 + 1])
            val o = java.lang.Float.intBitsToFloat(`is`[l * 8 + 2])
            f = Math.min(f, m)
            g = Math.min(g, n)
            h = Math.min(h, o)
            i = Math.max(i, m)
            j = Math.max(j, n)
            k = Math.max(k, o)
            ++l
        }
        if (fs != null) {
            fs[Direction.WEST.id] = f
            fs[Direction.EAST.id] = i
            fs[Direction.DOWN.id] = g
            fs[Direction.UP.id] = j
            fs[Direction.NORTH.id] = h
            fs[Direction.SOUTH.id] = k
            l = DIRECTIONS.size
            fs[Direction.WEST.id + l] = 1.0f - f
            fs[Direction.EAST.id + l] = 1.0f - i
            fs[Direction.DOWN.id + l] = 1.0f - g
            fs[Direction.UP.id + l] = 1.0f - j
            fs[Direction.NORTH.id + l] = 1.0f - h
            fs[Direction.SOUTH.id + l] = 1.0f - k
        }
        val p = 1.0E-4f
        m = 0.9999f
        when (direction) {
            Direction.DOWN -> {
                bitSet[1] = f >= 1.0E-4f || h >= 1.0E-4f || i <= 0.9999f || k <= 0.9999f
                bitSet[0] = g == j && (g < 1.0E-4f || blockState.isFullCube(blockRenderView, blockPos))
            }
            Direction.UP -> {
                bitSet[1] = f >= 1.0E-4f || h >= 1.0E-4f || i <= 0.9999f || k <= 0.9999f
                bitSet[0] = g == j && (j > 0.9999f || blockState.isFullCube(blockRenderView, blockPos))
            }
            Direction.NORTH -> {
                bitSet[1] = f >= 1.0E-4f || g >= 1.0E-4f || i <= 0.9999f || j <= 0.9999f
                bitSet[0] = h == k && (h < 1.0E-4f || blockState.isFullCube(blockRenderView, blockPos))
            }
            Direction.SOUTH -> {
                bitSet[1] = f >= 1.0E-4f || g >= 1.0E-4f || i <= 0.9999f || j <= 0.9999f
                bitSet[0] = h == k && (k > 0.9999f || blockState.isFullCube(blockRenderView, blockPos))
            }
            Direction.WEST -> {
                bitSet[1] = g >= 1.0E-4f || h >= 1.0E-4f || j <= 0.9999f || k <= 0.9999f
                bitSet[0] = f == i && (f < 1.0E-4f || blockState.isFullCube(blockRenderView, blockPos))
            }
            Direction.EAST -> {
                bitSet[1] = g >= 1.0E-4f || h >= 1.0E-4f || j <= 0.9999f || k <= 0.9999f
                bitSet[0] = f == i && (i > 0.9999f || blockState.isFullCube(blockRenderView, blockPos))
            }
        }
    }

    private fun renderQuadsFlat(
        blockRenderView: BlockRenderView,
        blockState: BlockState,
        blockPos: BlockPos,
        i: Int,
        j: Int,
        bl: Boolean,
        matrixStack: MatrixStack,
        vertexConsumer: VertexConsumer,
        list: List<BakedQuad>,
        bitSet: BitSet
    ) {
        var i = i
        for (bakedQuad in list) {
            if (bl) {
                getQuadDimensions(
                    blockRenderView,
                    blockState,
                    blockPos,
                    bakedQuad.vertexData,
                    bakedQuad.face,
                    null,
                    bitSet
                )
                val blockPos2 = if (bitSet[0]) blockPos.offset(bakedQuad.face) else blockPos
                i = WorldRenderer.getLightmapCoordinates(blockRenderView, blockState, blockPos2)
            }
            val f = blockRenderView.getBrightness(bakedQuad.face, bakedQuad.hasShade())
            renderQuad(
                blockRenderView,
                blockState,
                blockPos,
                vertexConsumer,
                matrixStack.peek(),
                bakedQuad,
                f,
                f,
                f,
                f,
                i,
                i,
                i,
                i,
                j
            )
        }
    }

    fun render(
        entry: MatrixStack.Entry,
        vertexConsumer: VertexConsumer,
        blockState: BlockState,
        bakedModel: BakedModel,
        f: Float,
        g: Float,
        h: Float,
        i: Int,
        j: Int
    ) {
        val random = Random.create()
        val l = 42L
        for (direction in DIRECTIONS) {
            random.setSeed(42L)
            renderQuads(
                entry,
                vertexConsumer,
                f,
                g,
                h,
                bakedModel.getQuads(blockState, direction, random),
                i,
                j
            )
        }
        random.setSeed(42L)
        renderQuads(
            entry,
            vertexConsumer,
            f,
            g,
            h,
            bakedModel.getQuads(blockState, null, random),
            i,
            j
        )
    }

    private fun renderQuads(
        entry: MatrixStack.Entry,
        vertexConsumer: VertexConsumer,
        f: Float,
        g: Float,
        h: Float,
        list: List<BakedQuad>,
        i: Int,
        j: Int
    ) {
        for (bakedQuad in list) {
            var m: Float
            var l: Float
            var k: Float
            if (bakedQuad.hasColor()) {
                k = MathHelper.clamp(f, 0.0f, 1.0f)
                l = MathHelper.clamp(g, 0.0f, 1.0f)
                m = MathHelper.clamp(h, 0.0f, 1.0f)
            } else {
                k = 1.0f
                l = 1.0f
                m = 1.0f
            }
            vertexConsumer.quad(entry, bakedQuad, k, l, m, i, j)
        }
    }
}