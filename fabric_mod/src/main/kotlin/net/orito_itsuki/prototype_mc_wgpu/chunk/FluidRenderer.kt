package net.orito_itsuki.prototype_mc_wgpu.chunk

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.LeavesBlock
import net.minecraft.block.TransparentBlock
import net.minecraft.client.color.world.BiomeColors
import net.minecraft.client.render.VertexConsumer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.WorldRenderer
import net.minecraft.fluid.Fluid
import net.minecraft.fluid.FluidState
import net.minecraft.tag.FluidTags
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockRenderView
import net.minecraft.world.BlockView
import net.orito_itsuki.prototype_mc_wgpu.resource.TextureResource

object FluidRenderer {
    private val lavaSprites = arrayOf(
        TextureResource("minecraft", "textures/block/lava_still.png"),
        TextureResource("minecraft", "textures/block/lava_flow.png"),
    )
    private val waterSprites = arrayOf(
        TextureResource("minecraft", "textures/block/water_still.png"),
        TextureResource("minecraft", "textures/block/water_flow.png"),
    )
    private val waterOverlaySprite = TextureResource("minecraft", "block/water_overlay.png")


    private fun isSameFluid(fluidState: FluidState, fluidState2: FluidState): Boolean {
        return fluidState2.fluid.matchesType(fluidState.fluid)
    }

    private fun isSideCovered(
        blockView: BlockView,
        direction: Direction,
        f: Float,
        blockPos: BlockPos,
        blockState: BlockState
    ): Boolean {
        if (blockState.isOpaque) {
            val voxelShape = VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, f.toDouble(), 1.0)
            val voxelShape2 = blockState.getCullingShape(blockView, blockPos)
            return VoxelShapes.isSideCovered(voxelShape, voxelShape2, direction)
        }
        return false
    }

    private fun isSideCovered(
        blockView: BlockView,
        blockPos: BlockPos,
        direction: Direction,
        f: Float,
        blockState: BlockState
    ): Boolean {
        return FluidRenderer.isSideCovered(blockView, direction, f, blockPos.offset(direction), blockState)
    }

    private fun isOppositeSideCovered(
        blockView: BlockView,
        blockPos: BlockPos,
        blockState: BlockState,
        direction: Direction
    ): Boolean {
        return FluidRenderer.isSideCovered(blockView, direction.opposite, 1.0f, blockPos, blockState)
    }

    private fun shouldRenderSide(
        blockRenderView: BlockRenderView,
        blockPos: BlockPos,
        fluidState: FluidState,
        blockState: BlockState,
        direction: Direction,
        fluidState2: FluidState
    ): Boolean {
        return !FluidRenderer.isOppositeSideCovered(
            blockRenderView,
            blockPos,
            blockState,
            direction
        ) && !FluidRenderer.isSameFluid(fluidState, fluidState2)
    }

    fun render(
        vertexConsumerProvider: VertexConsumerProvider,
        drawMode: VertexFormat.DrawMode,
        vertexFormat: VertexFormat,
        blockRenderView: BlockRenderView,
        blockPos: BlockPos,
        blockState: BlockState,
        fluidState: FluidState
    ) {
        var ap: Float
        var ao: Float
        var ag: Float
        var af: Float
        var ae: Float
        var ad: Float
        var ac: Float
        var ab: Float
        var z: Float
        val y: Float
        var r: Float
        var q: Float
        var p: Float
        var o: Float
        val bl = fluidState.isIn(FluidTags.LAVA)
        val sprites = if (bl) lavaSprites else waterSprites
        val i = if (bl) 0xFFFFFF else BiomeColors.getWaterColor(blockRenderView, blockPos)
        val f = (i shr 16 and 0xFF).toFloat() / 255.0f
        val g = (i shr 8 and 0xFF).toFloat() / 255.0f
        val h = (i and 0xFF).toFloat() / 255.0f
        val blockState2 = blockRenderView.getBlockState(blockPos.offset(Direction.DOWN))
        val fluidState2 = blockState2.fluidState
        val blockState3 = blockRenderView.getBlockState(blockPos.offset(Direction.UP))
        val fluidState3 = blockState3.fluidState
        val blockState4 = blockRenderView.getBlockState(blockPos.offset(Direction.NORTH))
        val fluidState4 = blockState4.fluidState
        val blockState5 = blockRenderView.getBlockState(blockPos.offset(Direction.SOUTH))
        val fluidState5 = blockState5.fluidState
        val blockState6 = blockRenderView.getBlockState(blockPos.offset(Direction.WEST))
        val fluidState6 = blockState6.fluidState
        val blockState7 = blockRenderView.getBlockState(blockPos.offset(Direction.EAST))
        val fluidState7 = blockState7.fluidState
        val bl2 = !FluidRenderer.isSameFluid(fluidState, fluidState3)
        val bl3 = FluidRenderer.shouldRenderSide(
            blockRenderView,
            blockPos,
            fluidState,
            blockState,
            Direction.DOWN,
            fluidState2
        ) && !FluidRenderer.isSideCovered(
            blockRenderView as BlockView,
            blockPos,
            Direction.DOWN,
            0.8888889f,
            blockState2
        )
        val bl4 = FluidRenderer.shouldRenderSide(
            blockRenderView,
            blockPos,
            fluidState,
            blockState,
            Direction.NORTH,
            fluidState4
        )
        val bl5 = FluidRenderer.shouldRenderSide(
            blockRenderView,
            blockPos,
            fluidState,
            blockState,
            Direction.SOUTH,
            fluidState5
        )
        val bl6 = FluidRenderer.shouldRenderSide(
            blockRenderView,
            blockPos,
            fluidState,
            blockState,
            Direction.WEST,
            fluidState6
        )
        val bl7 = FluidRenderer.shouldRenderSide(
            blockRenderView,
            blockPos,
            fluidState,
            blockState,
            Direction.EAST,
            fluidState7
        )
        if (!(bl2 || bl3 || bl7 || bl6 || bl4 || bl5)) {
            return
        }
        val j = blockRenderView.getBrightness(Direction.DOWN, true)
        val k = blockRenderView.getBrightness(Direction.UP, true)
        val l = blockRenderView.getBrightness(Direction.NORTH, true)
        val m = blockRenderView.getBrightness(Direction.WEST, true)
        val fluid = fluidState.fluid
        val n: Float = this.getFluidHeight(blockRenderView, fluid, blockPos, blockState, fluidState)
        if (n >= 1.0f) {
            o = 1.0f
            p = 1.0f
            q = 1.0f
            r = 1.0f
        } else {
            val s: Float = this.getFluidHeight(blockRenderView, fluid, blockPos.north(), blockState4, fluidState4)
            val t: Float = this.getFluidHeight(blockRenderView, fluid, blockPos.south(), blockState5, fluidState5)
            val u: Float = this.getFluidHeight(blockRenderView, fluid, blockPos.east(), blockState7, fluidState7)
            val v: Float = this.getFluidHeight(blockRenderView, fluid, blockPos.west(), blockState6, fluidState6)
            o = this.calculateFluidHeight(
                blockRenderView,
                fluid,
                n,
                s,
                u,
                blockPos.offset(Direction.NORTH).offset(Direction.EAST)
            )
            p = this.calculateFluidHeight(
                blockRenderView,
                fluid,
                n,
                s,
                v,
                blockPos.offset(Direction.NORTH).offset(Direction.WEST)
            )
            q = this.calculateFluidHeight(
                blockRenderView,
                fluid,
                n,
                t,
                u,
                blockPos.offset(Direction.SOUTH).offset(Direction.EAST)
            )
            r = this.calculateFluidHeight(
                blockRenderView,
                fluid,
                n,
                t,
                v,
                blockPos.offset(Direction.SOUTH).offset(Direction.WEST)
            )
        }
        val d = (blockPos.x and 0xF).toDouble()
        val e = (blockPos.y and 0xF).toDouble()
        val w = (blockPos.z and 0xF).toDouble()
        val x = 0.001f
        y = if (bl3) 0.001f else 0.0f
        val f2 = y
        if (bl2 && !FluidRenderer.isSideCovered(
                blockRenderView as BlockView, blockPos, Direction.UP, Math.min(
                    Math.min(p, r), Math.min(q, o)
                ), blockState3
            )
        ) {
            val vertexConsumer: VertexConsumer
            var ak: Float
            var aj: Float
            var ai: Float
            var ah: Float
            var aa: Float
            p -= 0.001f
            r -= 0.001f
            q -= 0.001f
            o -= 0.001f
            val vec3d = fluidState.getVelocity(blockRenderView, blockPos)
            if (vec3d.x == 0.0 && vec3d.z == 0.0) {
                vertexConsumer = vertexConsumerProvider.createMinecraftBlockMesh(drawMode, vertexFormat, sprites[0], true)
                z = 0.0f / 16f
                aa = 0.0f / 16f
                ab = z
                ac = 16.0f / 16f
                ad = 16.0f / 16f
                ae = ac
                af = ad
                ag = aa
            } else {
                vertexConsumer = vertexConsumerProvider.createMinecraftBlockMesh(drawMode, vertexFormat, sprites[1], true)
                ah = MathHelper.atan2(vec3d.z, vec3d.x).toFloat() - 1.5707964f
                ai = MathHelper.sin(ah) * 0.25f
                aj = MathHelper.cos(ah) * 0.25f
                ak = (8.0f) / 16f
                z = (8.0f + (-aj - ai) * 16.0f) / 16f
                aa = (8.0f + (-aj + ai) * 16.0f) / 16f
                ab = (8.0f + (-aj + ai) * 16.0f) / 16f
                ac = (8.0f + (aj + ai) * 16.0f) / 16f
                ad = (8.0f + (aj + ai) * 16.0f) / 16f
                ae = (8.0f + (aj - ai) * 16.0f) / 16f
                af = (8.0f + (aj - ai) * 16.0f) / 16f
                ag = (8.0f + (-aj - ai) * 16.0f) / 16f
            }
            val al = (z + ab + ad + af) / 4.0f
            ah = (aa + ac + ae + ag) / 4.0f
//            ai = sprites[0].width.toFloat() / (sprites[0].maxU - sprites[0].minU)
//            aj = sprites[0].height.toFloat() / (sprites[0].maxV - sprites[0].minV)
            ai = 1.0f
            aj = 1.0f
            ak = 4.0f / Math.max(aj, ai)
            z = MathHelper.lerp(ak, z, al)
            ab = MathHelper.lerp(ak, ab, al)
            ad = MathHelper.lerp(ak, ad, al)
            af = MathHelper.lerp(ak, af, al)
            aa = MathHelper.lerp(ak, aa, ah)
            ac = MathHelper.lerp(ak, ac, ah)
            ae = MathHelper.lerp(ak, ae, ah)
            ag = MathHelper.lerp(ak, ag, ah)
            val am: Int = this.getLight(blockRenderView, blockPos)
            val an = k * f
            ao = k * g
            ap = k * h
            this.vertex(vertexConsumer, d + 0.0, e + p.toDouble(), w + 0.0, an, ao, ap, z, aa, am)
            this.vertex(vertexConsumer, d + 0.0, e + r.toDouble(), w + 1.0, an, ao, ap, ab, ac, am)
            this.vertex(vertexConsumer, d + 1.0, e + q.toDouble(), w + 1.0, an, ao, ap, ad, ae, am)
            this.vertex(vertexConsumer, d + 1.0, e + o.toDouble(), w + 0.0, an, ao, ap, af, ag, am)
            if (fluidState.method_15756(blockRenderView, blockPos.up())) {
                this.vertex(vertexConsumer, d + 0.0, e + p.toDouble(), w + 0.0, an, ao, ap, z, aa, am)
                this.vertex(vertexConsumer, d + 1.0, e + o.toDouble(), w + 0.0, an, ao, ap, af, ag, am)
                this.vertex(vertexConsumer, d + 1.0, e + q.toDouble(), w + 1.0, an, ao, ap, ad, ae, am)
                this.vertex(vertexConsumer, d + 0.0, e + r.toDouble(), w + 1.0, an, ao, ap, ab, ac, am)
            }
        }
        if (bl3) {
            val vertexConsumer = vertexConsumerProvider.createMinecraftBlockMesh(drawMode, vertexFormat, sprites[0], true)
            z = 0.0f
            ab = 1.0f
            ad = 0.0f
            af = 1.0f
            val aq: Int = this.getLight(blockRenderView, blockPos.down())
            ac = j * f
            ae = j * g
            ag = j * h
            this.vertex(vertexConsumer, d, e + y.toDouble(), w + 1.0, ac, ae, ag, z, af, aq)
            this.vertex(vertexConsumer, d, e + y.toDouble(), w, ac, ae, ag, z, ad, aq)
            this.vertex(vertexConsumer, d + 1.0, e + y.toDouble(), w, ac, ae, ag, ab, ad, aq)
            this.vertex(vertexConsumer, d + 1.0, e + y.toDouble(), w + 1.0, ac, ae, ag, ab, af, aq)
        }
        val ar: Int = this.getLight(blockRenderView, blockPos)
        for (direction in Direction.Type.HORIZONTAL) {
            var block: Block?
            var av: Double
            var au: Double
            var at: Double
            var `as`: Double
            var aa: Float
            if (!when (direction) {
                    Direction.NORTH -> {
                        af = p
                        aa = o
                        `as` = d
                        at = d + 1.0
                        au = w + 0.001
                        av = w + 0.001
                        bl4
                    }
                    Direction.SOUTH -> {
                        af = q
                        aa = r
                        `as` = d + 1.0
                        at = d
                        au = w + 1.0 - 0.001
                        av = w + 1.0 - 0.001
                        bl5
                    }
                    Direction.WEST -> {
                        af = r
                        aa = p
                        `as` = d + 0.001
                        at = d + 0.001
                        au = w + 1.0
                        av = w
                        bl6
                    }
                    else -> {
                        af = o
                        aa = q
                        `as` = d + 1.0 - 0.001
                        at = d + 1.0 - 0.001
                        au = w
                        av = w + 1.0
                        bl7
                    }
                } || FluidRenderer.isSideCovered(
                    blockRenderView as BlockView,
                    blockPos,
                    direction,
                    Math.max(af, aa),
                    blockRenderView.getBlockState(blockPos.offset(direction))
                )
            ) continue
            val blockPos2 = blockPos.offset(direction)
            var sprite2 = sprites[1]
            if (!bl && (blockRenderView.getBlockState(blockPos2).block.also {
                    block = it
                } is TransparentBlock || block is LeavesBlock)) {
                sprite2 = this.waterOverlaySprite
            }
            val vertexConsumer = vertexConsumerProvider.createMinecraftBlockMesh(drawMode, vertexFormat, sprite2, true)
            ao = 0.0f / 16f
            ap = 8.0f / 16f
            val aw = ((1.0f - af) * 16.0f * 0.5f) / 16f
            val ax = ((1.0f - aa) * 16.0f * 0.5f) / 16f
            val ay = 8.0f / 16f
            val az = if (direction.axis === Direction.Axis.Z) l else m
            val ba = k * az * f
            val bb = k * az * g
            val bc = k * az * h
            this.vertex(vertexConsumer, `as`, e + af.toDouble(), au, ba, bb, bc, ao, aw, ar)
            this.vertex(vertexConsumer, at, e + aa.toDouble(), av, ba, bb, bc, ap, ax, ar)
            this.vertex(vertexConsumer, at, e + y.toDouble(), av, ba, bb, bc, ap, ay, ar)
            this.vertex(vertexConsumer, `as`, e + y.toDouble(), au, ba, bb, bc, ao, ay, ar)
            if (sprite2 === this.waterOverlaySprite) continue
            this.vertex(vertexConsumer, `as`, e + y.toDouble(), au, ba, bb, bc, ao, ay, ar)
            this.vertex(vertexConsumer, at, e + y.toDouble(), av, ba, bb, bc, ap, ay, ar)
            this.vertex(vertexConsumer, at, e + aa.toDouble(), av, ba, bb, bc, ap, ax, ar)
            this.vertex(vertexConsumer, `as`, e + af.toDouble(), au, ba, bb, bc, ao, aw, ar)
        }
    }

    private fun calculateFluidHeight(
        blockRenderView: BlockRenderView,
        fluid: Fluid,
        f: Float,
        g: Float,
        h: Float,
        blockPos: BlockPos
    ): Float {
        if (h >= 1.0f || g >= 1.0f) {
            return 1.0f
        }
        val fs = FloatArray(2)
        if (h > 0.0f || g > 0.0f) {
            val i = this.getFluidHeight(blockRenderView, fluid, blockPos)
            if (i >= 1.0f) {
                return 1.0f
            }
            addHeight(fs, i)
        }
        addHeight(fs, f)
        addHeight(fs, h)
        addHeight(fs, g)
        return fs[0] / fs[1]
    }

    private fun addHeight(fs: FloatArray, f: Float) {
        if (f >= 0.8f) {
            fs[0] = fs[0] + f * 10.0f
            fs[1] = fs[1] + 10.0f
        } else if (f >= 0.0f) {
            fs[0] = fs[0] + f
            fs[1] = fs[1] + 1.0f
        }
    }

    private fun getFluidHeight(blockRenderView: BlockRenderView, fluid: Fluid, blockPos: BlockPos): Float {
        val blockState = blockRenderView.getBlockState(blockPos)
        return this.getFluidHeight(blockRenderView, fluid, blockPos, blockState, blockState.fluidState)
    }

    private fun getFluidHeight(
        blockRenderView: BlockRenderView,
        fluid: Fluid,
        blockPos: BlockPos,
        blockState: BlockState,
        fluidState: FluidState
    ): Float {
        if (fluid.matchesType(fluidState.fluid)) {
            val blockState2 = blockRenderView.getBlockState(blockPos.up())
            return if (fluid.matchesType(blockState2.fluidState.fluid)) {
                1.0f
            } else fluidState.height
        }
        return if (!blockState.material.isSolid) {
            0.0f
        } else -1.0f
    }

    private fun vertex(
        vertexConsumer: VertexConsumer,
        d: Double,
        e: Double,
        f: Double,
        g: Float,
        h: Float,
        i: Float,
        j: Float,
        k: Float,
        l: Int
    ) {
        vertexConsumer.vertex(d, e, f).color(g, h, i, 1.0f).texture(j, k).light(l).normal(0.0f, 1.0f, 0.0f).next()
    }

    private fun getLight(blockRenderView: BlockRenderView, blockPos: BlockPos): Int {
        val i = WorldRenderer.getLightmapCoordinates(blockRenderView, blockPos)
        val j = WorldRenderer.getLightmapCoordinates(blockRenderView, blockPos.up())
        val k = i and 0xFF
        val l = j and 0xFF
        val m = i shr 16 and 0xFF
        val n = j shr 16 and 0xFF
        return (if (k > l) k else l) or ((if (m > n) m else n) shl 16)
    }
}