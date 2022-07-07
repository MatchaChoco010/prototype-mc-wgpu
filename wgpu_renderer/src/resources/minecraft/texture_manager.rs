use once_cell::sync::Lazy;
use std::collections::HashMap;
use std::sync::{Arc, Mutex};

use crate::resources::{Identifier, Texture};

enum TextureContent {
    Loading,
    Content(Texture),
}

pub struct MinecraftTextureManager {
    device: Arc<wgpu::Device>,
    queue: Arc<wgpu::Queue>,
    textures: HashMap<Identifier, TextureContent>,
    loading_texture: Texture,
}
impl MinecraftTextureManager {
    pub fn get_texture(&self, id: Identifier) -> &Texture {
        if let Some(texture) = self.textures.get(&id) {
            match texture {
                TextureContent::Loading => &self.loading_texture,
                TextureContent::Content(texture) => texture,
            }
        } else {
            panic!("Not start loading texture: {:?}", id);
        }
    }
}

pub static MINECRAFT_TEXTURE_MANAGER: Lazy<Arc<Mutex<Option<MinecraftTextureManager>>>> =
    Lazy::new(|| Arc::new(Mutex::new(None)));

pub fn init(device: Arc<wgpu::Device>, queue: Arc<wgpu::Queue>) {
    let textures = HashMap::new();
    let loading_texture = Texture::loading_texture(device.clone(), queue.clone());
    MINECRAFT_TEXTURE_MANAGER
        .lock()
        .unwrap()
        .replace(MinecraftTextureManager {
            device: device.clone(),
            queue: queue.clone(),
            textures,
            loading_texture,
        });
}

pub fn start_loading_texture(id: Identifier) {
    crate::RUNTIME.spawn(async {
        let (device, queue) = {
            let mut manager = MINECRAFT_TEXTURE_MANAGER.lock().unwrap();
            let manager = manager.as_mut().unwrap();
            if manager.textures.contains_key(&id) {
                return;
            }
            manager.textures.insert(id.clone(), TextureContent::Loading);
            (manager.device.clone(), manager.queue.clone())
        };

        let texture = Texture::load_texture(device.clone(), queue.clone(), id.clone())
            .await
            .unwrap_or(Texture::missing_texture(device, queue));

        {
            let mut manager = MINECRAFT_TEXTURE_MANAGER.lock().unwrap();
            let manager = manager.as_mut().unwrap();
            manager
                .textures
                .insert(id, TextureContent::Content(texture));
        }
    });
}
