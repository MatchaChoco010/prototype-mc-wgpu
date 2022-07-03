use jni::objects::{JClass, JObject, JValue};
use jni::sys::jint;
use jni::JNIEnv;

mod event;
mod renderer;
mod scene;
mod window;
mod window_size;
use event::*;

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_initWindow(
    _env: JNIEnv,
) {
    window::init();
}

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_draw(
    env: JNIEnv,
    _class: JClass,
    command: JObject,
) {
    scene::apply_command(env, command);
    event::send(MinecraftEvent::Draw);
}

#[no_mangle]
pub extern "system" fn Java_net_orito_1itsuki_prototype_1mc_1wgpu_rust_WgpuRendererNative_getWindowSize(
    env: JNIEnv,
) -> JObject {
    let (w, h) = window_size::inner_size();
    let class = env
        .find_class("net/orito_itsuki/prototype_mc_wgpu/rust/WindowSize")
        .unwrap();
    let obj = env
        .new_object(
            class,
            "(II)V",
            &[JValue::Int(w as jint), JValue::Int(h as jint)],
        )
        .unwrap();
    obj
}
