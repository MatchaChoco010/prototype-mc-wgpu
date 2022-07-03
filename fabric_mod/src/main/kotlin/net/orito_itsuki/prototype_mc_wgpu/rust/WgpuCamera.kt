package net.orito_itsuki.prototype_mc_wgpu.rust

object WgpuCamera {
    var fovY: Float = Math.toRadians(90.0).toFloat()
    var near: Float = 0.01F
    var far: Float = 150F
    var aspectRatio: Float = 800F / 600F
    var pitch: Float = 0F
    var yaw: Float = 0F
}