package net.orito_itsuki.prototype_mc_wgpu.mixins_impl.client

import net.minecraft.block.entity.BlockEntity
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.math.Vec3d
import net.orito_itsuki.prototype_mc_wgpu.rust.MinecraftWorld
import net.orito_itsuki.prototype_mc_wgpu.rust.WgpuCamera
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.atan2

object WorldRendererMixinImpl {
    fun onRenderer(
        self: WorldRenderer,
        matrixStack: MatrixStack,
        f: Float,
        l: Long,
        bl: Boolean,
        camera: Camera,
        gameRenderer: GameRenderer,
        lightmapTextureManager: LightmapTextureManager,
        matrix4f: Matrix4f,
        ci: CallbackInfo,
    ) {
//        PrototypeMcWgpu.LOGGER.info("start my render")
        MinecraftWorld.resetIndices()

//        WgpuCamera.fovY = atan2(camera.projection.topLeft.y, camera.projection.topLeft.z).toFloat()
        WgpuCamera.fovY = Math.toRadians(90.0).toFloat()
        WgpuCamera.near = 0.01F
//        WgpuCamera.far = camera.projection.bottomRight.z.toFloat()
        WgpuCamera.far = 150F
//        WgpuCamera.aspectRatio = ((camera.projection.bottomRight.x - camera.projection.bottomLeft.x) / (camera.projection.topLeft.y - camera.projection.bottomLeft.y)).toFloat()
        WgpuCamera.aspectRatio = 800F / 600F
        WgpuCamera.yaw = Math.toRadians(camera.yaw.toDouble()).toFloat()
        WgpuCamera.pitch = Math.toRadians(camera.pitch.toDouble()).toFloat()

        val vec3d: Vec3d = camera.pos
        val d = vec3d.getX()
        val e = vec3d.getY()
        val g = vec3d.getZ()
        for (chunkInfo in self.chunkInfos) {
            val list = chunkInfo.chunk.getData().blockEntities
            if (list.isEmpty()) continue
            for (blockEntity in list) {
                val blockPos2 = blockEntity.pos
                var vertexConsumerProvider = MinecraftWorld.getVertexConsumerProvider(blockEntity.type.toString())
                matrixStack.push()
                matrixStack.translate(
                    blockPos2.x.toDouble() - d,
                    blockPos2.y.toDouble() - e,
                    blockPos2.z.toDouble() - g
                )
//                val sortedSet = self.blockBreakingProgressions.get(blockPos2.asLong())
//                if (sortedSet != null && !sortedSet.isEmpty() && (sortedSet.last() as BlockBreakingInfo).stage.also {
//                        m = it
//                    } >= 0) {
//                    val entry: MatrixStack.Entry = matrixStack.peek()
//                    val vertexConsumer = OverlayVertexConsumer(
//                        self.bufferBuilders.getEffectVertexConsumers().getBuffer(
//                            ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS[m]
//                        ), entry.positionMatrix, entry.normalMatrix
//                    )
//                    vertexConsumerProvider = label@ VertexConsumerProvider { renderLayer: RenderLayer ->
//                        val vertexConsumer2: VertexConsumer = immediate.getBuffer(renderLayer)
//                        if (renderLayer.hasCrumbling()) {
//                            return@label VertexConsumers.union(vertexConsumer, vertexConsumer2)
//                        }
//                        vertexConsumer2
//                    }
//                }
                self.blockEntityRenderDispatcher.render<BlockEntity>(
                    blockEntity,
                    f,
                    matrixStack,
                    vertexConsumerProvider
                )
                matrixStack.pop()
            }
        }

        MinecraftWorld.clearBuffers()
    }
}