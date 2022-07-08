package net.orito_itsuki.prototype_mc_wgpu.rust

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object WgpuCommandMailbox {
    private val mutex = Mutex()

    private var isMailboxEmpty = true

    val active = WgpuCommand()
    private val mailbox = WgpuCommand()
    private val back = WgpuCommand()

    fun swapActive() {
        runBlocking {
            mutex.withLock {
                // mailboxにあればactiveを上書きして、isMailboxEmptyをtrueにする
                // mailboxになければ何もせず返す
                // backの読み込みをキックして、backが60fpsのためにsuspendしていたら読み込みを割り込ませる
            }
        }
    }

    private suspend fun swapMailbox() {
        mutex.withLock {
            // mailboxをbackと入れ替えして、isMailboxEmptyをfalseにする
        }
    }

    fun run() {
        runBlocking {
            launch {
                // 60fpsもしくはswapActiveのキックを待つ
                // チャンクを集めてきて
                // 同期的に更新
                // 非同期更新をキック
                // 更新されているチャンクをコマンドに詰める
                swapMailbox()
            }
        }
    }
}