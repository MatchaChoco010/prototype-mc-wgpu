package net.orito_itsuki.prototype_mc_wgpu.chunk

import net.minecraft.block.BlockRenderType
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.*
import net.minecraft.client.render.chunk.ChunkRendererRegion
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.fluid.FluidState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.random.Random
import net.minecraft.world.chunk.WorldChunk

object RenderChunkConstructor {
    private const val minY = -64
    private const val maxY = 320

    private val models = MinecraftClient.getInstance().bakedModelManager.blockModels

    fun construct(
        vertexConsumerProvider: VertexConsumerProvider,
        originX: Int,
        originZ: Int,
        chunk: WorldChunk,
        region: ChunkRendererRegion?,
    ) {
        if (region == null) return
        val random = Random.create()
        val blockEntities = mutableListOf<BlockEntity>()
        for (blockPos in BlockPos.iterate(originX, minY, originZ, originX + 15, maxY, originZ + 15)) {
            val blockEntity = chunk.getBlockEntity(blockPos)
            if (blockEntity != null) {
                blockEntities.add(blockEntity)
            }
            val blockState = chunk.getBlockState(blockPos)
            val fluidState = blockState.fluidState
            if (!fluidState.isEmpty) {
                constructFluid(vertexConsumerProvider, region, blockPos, blockState, fluidState, originX, originZ)
            }
            if (blockState.renderType == BlockRenderType.INVISIBLE) continue
            val renderLayer = RenderLayers.getBlockLayer(blockState)
            constructBlock(vertexConsumerProvider, region, blockPos, blockState, renderLayer, originX, originZ, random)
        }
    }

    private fun constructFluid(
        vertexConsumerProvider: VertexConsumerProvider,
        region: ChunkRendererRegion,
        blockPos: BlockPos,
        blockState: BlockState,
        fluidState: FluidState,
        originX: Int,
        originZ: Int,
    ) {
        val drawMode = VertexFormat.DrawMode.QUADS
        val vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        FluidRenderer.render(
            vertexConsumerProvider,
            drawMode,
            vertexFormat,
            region,
            BlockPos(blockPos.x - originX, blockPos.y - minY, blockPos.z - originZ),
            blockState,
            fluidState,
        )
    }

    private fun constructBlock(
        vertexConsumerProvider: VertexConsumerProvider,
        region: ChunkRendererRegion,
        blockPos: BlockPos,
        blockState: BlockState,
        renderLayer: RenderLayer,
        originX: Int,
        originZ: Int,
        random: Random,
    ) {
        val matrixStack = MatrixStack()
        matrixStack.translate(-originX.toDouble(), -minY.toDouble(), -originZ.toDouble())
        val drawMode = VertexFormat.DrawMode.QUADS
        val vertexFormat = VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL
        val texture =
        val transparent = blockState.isTranslucent(region, blockPos)
        if (blockState.renderType == BlockRenderType.MODEL) {
            BlockRenderer.render(
                region,
                models.getModel(blockState),
                blockState,
                blockPos,
                matrixStack,
                vertexConsumerProvider.createMinecraftBlockMesh(drawMode, vertexFormat, texture, transparent),
                true,
                random,
                blockState.getRenderingSeed(blockPos),
                OverlayTexture.DEFAULT_UV,
            );
        }
    }
}