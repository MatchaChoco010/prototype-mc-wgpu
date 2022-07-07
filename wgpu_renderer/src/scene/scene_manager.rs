use jni::{objects::JObject, JNIEnv};
use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex, MutexGuard};

use crate::scene::Scene;

pub struct SceneManager {
    render_scene: Arc<Mutex<Option<Scene>>>,
    update_scene: Arc<Mutex<Option<Scene>>>,
}
impl SceneManager {
    fn new() -> Self {
        Self {
            render_scene: Arc::new(Mutex::new(None)),
            update_scene: Arc::new(Mutex::new(None)),
        }
    }

    fn init(&self, device: Arc<wgpu::Device>) {
        self.render_scene
            .lock()
            .unwrap()
            .replace(Scene::new(device.clone()));
        self.update_scene
            .lock()
            .unwrap()
            .replace(Scene::new(device));
    }

    fn submit_command(&self, env: JNIEnv, command: JObject) {
        if let Some(update_scene) = self.update_scene.lock().unwrap().as_mut() {
            update_scene.apply_command(env, command).unwrap();
        }
        if let (Some(update_scene), Some(render_scene)) = (
            self.update_scene.lock().unwrap().as_ref(),
            self.render_scene.lock().unwrap().as_mut(),
        ) {
            *render_scene = update_scene.clone();
        }
    }
}

static SCENE_MANAGER: Lazy<SceneManager> = Lazy::new(|| SceneManager::new());

pub fn init(device: Arc<wgpu::Device>) {
    SCENE_MANAGER.init(device);
}

pub fn submit_command(env: JNIEnv, command: JObject) {
    SCENE_MANAGER.submit_command(env, command);
}

pub fn render_scene<'a>() -> MutexGuard<'a, Option<Scene>> {
    SCENE_MANAGER.render_scene.lock().unwrap()
}
