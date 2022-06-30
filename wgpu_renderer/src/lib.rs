use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_WgpuRendererNative_rustNative(
    _env: JNIEnv,
) {
    println!("Hello from Rust!");
}
