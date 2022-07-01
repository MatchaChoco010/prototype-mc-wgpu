use jni::objects::{JObject, JValue};
use jni::sys::{jdouble, jint};
use jni::JNIEnv;

mod event;
mod renderer;
mod window_size;
use event::*;

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_rustNative(
    _env: JNIEnv,
) {
    println!("Hello from Rust!");
}

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_initWindow(
    _env: JNIEnv,
) {
    renderer::init();
}

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_draw(
    _env: JNIEnv,
) {
    event::send(MinecraftEvent::Draw);
}

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_getWindowSize(
    env: JNIEnv,
) -> JObject {
    let (w, h) = window_size::inner_size();
    let (sw, sh) = window_size::scaled_inner_size();
    let s = window_size::scale_factor();
    let class = env
        .find_class("net/orito_itsuki/prototype_mc_wgpu/rust/WindowSize")
        .unwrap();
    let obj = env
        .new_object(
            class,
            "(IIIID)V",
            &[
                JValue::Int(w as jint),
                JValue::Int(h as jint),
                JValue::Int(sw as jint),
                JValue::Int(sh as jint),
                JValue::Double(s as jdouble),
            ],
        )
        .unwrap();
    obj
}
