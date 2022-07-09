package net.orito_itsuki.prototype_mc_wgpu.chunk

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.client.render.Camera
import net.minecraft.client.render.chunk.ChunkRendererRegionBuilder
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.orito_itsuki.prototype_mc_wgpu.resource.Resource
import java.nio.ByteBuffer
import kotlin.math.floor

object RenderChunksManager {
    private const val minY = -64
    private const val maxY = 320

    private val chunkRendererRegionBuilder = ChunkRendererRegionBuilder()

    private val activeChunks = mutableMapOf<ChunkId, ChunkUpdater>()

    var loadDistance = 4
    var unloadDistance = 6

    suspend fun update(world: World, camera: Camera): RenderChunks {
        // カメラ位置の取得
        val camPos = camera.blockPos

        // activeChunksの更新
        val cameraChunk = ChunkId(floor(camPos.x.toFloat() / 16F).toInt(), floor(camPos.z.toFloat() / 16F).toInt())
        for (entry in activeChunks) {
            val c = entry.key
            if (c.x < cameraChunk.x - unloadDistance + 1 || c.x > cameraChunk.x + unloadDistance - 1 ||
                c.z < cameraChunk.z - unloadDistance + 1 || c.z > cameraChunk.z + unloadDistance - 1) {
                activeChunks.remove(c)
            }
        }
        for (x in (-loadDistance + 1) until loadDistance) {
            for (z in (-loadDistance + 1) until loadDistance) {
                val c = ChunkId(x, z)
                if (!activeChunks.containsKey(c)) {
                    activeChunks[c] = ChunkUpdater(x, z)
                }
            }
        }

        runBlocking {
            // syncの領域をrunする
            val list = mutableListOf<Deferred<Unit>>()
            for (entry in activeChunks) {
                if (entry.value.needUpdate()) {
                    list.add(async { entry.value.update(world) })
                }
            }
            list.awaitAll()
            // asyncの領域をスケジュールする
            for (entry in activeChunks) {
                if (!entry.value.needUpdate()) {
                    async { entry.value.update(world) }
                }
            }
        }

        // sync/asyncの完了したやつを集める & 更新済みフラグを折る
        val renderChunks = mutableListOf<RenderChunk>()
        for (entry in activeChunks) {
            val chunk = entry.value.takeUpdatedChunk()
            if (chunk != null) {
                renderChunks.add(chunk)
            }
        }

        // RenderChunksとして返す
        val activeChunks = activeChunks.keys.toList()
        return RenderChunks(activeChunks, renderChunks)
    }

    class ChunkId(val x: Int, val z: Int)

    class ChunkUpdater(private val x: Int, private val z: Int) {
        private val mutex = Mutex()
        private var renderChunk: RenderChunk? = null

        fun needUpdate(): Boolean {
            return true
        }

        suspend fun update(world: World) {
            // chunk取得
            val chunk = world.getChunk(x, z)
            val originX = x * 16
            val originY = 0
            val originZ = z * 16

            // region作成
            val region = chunkRendererRegionBuilder.build(
                world,
                BlockPos(originX - 1, minY, originZ - 1),
                BlockPos(originX + 16, maxY, originZ + 16),
                1
            )

            // vertexConsumerProvider作成
            val vertexConsumerProvider = VertexConsumerProvider()

            // renderする
            RenderChunkConstructor.construct(vertexConsumerProvider, originX, originZ, chunk, region)

            // メッシュ情報をvertexConsumerProviderから取得する
            val vertices = vertexConsumerProvider.takeVertices()
            val indices = vertexConsumerProvider.takeIndices()
            val meshes = vertexConsumerProvider.takeMeshes()
            val resources = vertexConsumerProvider.takeResources()

            // mutex取ってUpdatedChunkを更新
            mutex.withLock {
                renderChunk = RenderChunk(originX, originZ, resources, vertices, indices, meshes)
            }
        }

        suspend fun takeUpdatedChunk(): RenderChunk? {
            if (renderChunk != null) {
                mutex.withLock {
                    val uc = renderChunk
                    renderChunk = null
                    return uc
                }
            } else {
                return null
            }
        }
    }

    class RenderChunk(
        val originX: Int,
        val originZ: Int,
        val resources: List<Resource>,
        val vertices: List<ByteBuffer>,
        val indices: List<ByteBuffer>,
        val meshes: List<VertexConsumerProvider.MinecraftBlockMesh>,
    )

    class RenderChunks(
        val activeChunks: List<ChunkId>,
        val renderChunksList: List<RenderChunk>,
    )
}