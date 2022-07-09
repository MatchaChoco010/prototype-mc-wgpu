package net.orito_itsuki.prototype_mc_wgpu.rust

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.client.render.Camera
import net.minecraft.world.World
import net.orito_itsuki.prototype_mc_wgpu.chunk.RenderChunksManager
import java.util.concurrent.TimeUnit
import kotlin.system.measureNanoTime

object WgpuCommandMailbox {
    private val mutex = Mutex()

    private var isMailboxEmpty = true

    var active = WgpuCommand()
    private var mailbox = WgpuCommand()
    private var back = WgpuCommand()

    private var channel: Channel<Unit>? = null

    fun swapActive() {
        runBlocking {
            mutex.withLock {
                // mailboxにコマンドがあれば入れ替えして、isMailboxEmptyをtrueにする
                if (!isMailboxEmpty) {
                    val cmd = active
                    active = mailbox
                    mailbox = cmd
                    isMailboxEmpty = true
                    channel?.send(Unit)
                }
            }
        }
    }

    private suspend fun swapMailbox() {
        mutex.withLock {
            // mailboxをbackと入れ替えして、isMailboxEmptyをfalseにする
            val cmd = mailbox
            mailbox = back
            back = cmd
            isMailboxEmpty = false
        }
    }

    private suspend fun waitSwapActive() {
        channel = Channel<Unit>()
        channel!!.receive()
    }

    suspend fun run(world: World, camera: Camera) = withContext(Dispatchers.IO) {
        var waitTime: Long = 0
        while (isActive) {
            // 60fpsもしくはswapActiveのキックを待つ
            val jobWaitSwapActive = launch { waitSwapActive() }
            val jobWaitTime = launch {
                withContext(Dispatchers.IO) {
                    TimeUnit.NANOSECONDS.sleep(waitTime)
                }
            }
            select {
                jobWaitSwapActive.onJoin { jobWaitTime.cancel() }
                jobWaitTime.onJoin { jobWaitSwapActive.cancel() }
            }

            val ns = measureNanoTime {
                val renderChunks = RenderChunksManager.update(world, camera)
                back.clear()
                back.loadRenderChunks(renderChunks)
                swapMailbox()
            }
            waitTime = maxOf((1_000_000_000 / 60) - ns, 0)
        }
    }
}