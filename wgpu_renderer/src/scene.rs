use jni::objects::JByteBuffer;
use jni::{
    objects::{JList, JObject, JString},
    JNIEnv,
};
use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use wgpu::util::DeviceExt;

mod camera;
mod minecraft_mesh;
pub use camera::*;
pub use minecraft_mesh::*;

use crate::renderer;

pub struct Scene {
    pub camera: Camera,
    pub minecraft_meshes: Vec<MinecraftMesh>,
}
impl Scene {
    fn new() -> Self {
        Self {
            camera: Camera::new(90.0, 0.01, 150.0, 800.0 / 600.0, 0.0, 0.0),
            minecraft_meshes: vec![],
        }
    }

    fn upload_mesh(
        &mut self,
        device: &wgpu::Device,
        env: &JNIEnv,
        mesh: JObject,
        vertices: &JList,
        indices: &JList,
    ) {
        const BUFFER_MAX_INDICES: i64 = 128 * 1024;
        let format = env
            .get_field(mesh, "vertexFormat", "Ljava/lang/String;")
            .unwrap();
        let format = JString::from(format.l().unwrap());
        let format = env.get_string(format).unwrap();
        let format = format.to_str().unwrap();
        let start_vertices = env
            .get_field(mesh, "startVertices", "J")
            .unwrap()
            .j()
            .unwrap();
        let end_vertices = env
            .get_field(mesh, "endVertices", "J")
            .unwrap()
            .j()
            .unwrap();
        let start_indices = env
            .get_field(mesh, "startIndices", "J")
            .unwrap()
            .j()
            .unwrap();
        let end_indices = env.get_field(mesh, "endIndices", "J").unwrap().j().unwrap();

        if start_indices == end_indices {
            return;
        }

        let vertex_format = match format {
            "PositionColorTextureLightNormal" => {
                MinecraftMeshVertexFormat::PositionColorTextureLightNormal
            }
            _ => {
                // println!("Unknown vertex format: {}", format);
                MinecraftMeshVertexFormat::PositionColorTextureLightNormal
            }
        };

        let vertex_buffer =
            if (start_vertices) / BUFFER_MAX_INDICES == (end_vertices) / BUFFER_MAX_INDICES {
                let buffer = vertices
                    .get((start_vertices / BUFFER_MAX_INDICES) as i32)
                    .unwrap()
                    .unwrap();
                let buffer = env
                    .get_direct_buffer_address(JByteBuffer::from(buffer))
                    .unwrap();
                device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                    label: Some("Vertex Buffer"),
                    contents: bytemuck::cast_slice(
                        &buffer[start_vertices as usize..end_vertices as usize],
                    ),
                    usage: wgpu::BufferUsages::VERTEX,
                })
            } else {
                let mut vertices_buf = vec![0; (end_vertices - start_vertices) as usize];
                let mut start = start_vertices;
                let mut cursor = 0;
                loop {
                    let buffer = vertices
                        .get((start / BUFFER_MAX_INDICES) as i32)
                        .unwrap()
                        .unwrap();
                    let buffer = env
                        .get_direct_buffer_address(JByteBuffer::from(buffer))
                        .unwrap();
                    if start / BUFFER_MAX_INDICES == end_vertices / BUFFER_MAX_INDICES {
                        let end = end_vertices - start;
                        let (_left, right) = vertices_buf.split_at_mut(cursor);
                        let (left, _right) = right.split_at_mut((end - start) as usize);
                        left.copy_from_slice(&buffer[start as usize..end as usize]);
                        break;
                    } else {
                        let end = (start / BUFFER_MAX_INDICES + 1) * BUFFER_MAX_INDICES;
                        let (_left, right) = vertices_buf.split_at_mut(cursor);
                        let (left, _right) = right.split_at_mut((end - start) as usize);
                        left.copy_from_slice(&buffer[start as usize..end as usize]);
                        start += BUFFER_MAX_INDICES;
                        cursor += (end - start) as usize
                    }
                }
                device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                    label: Some("Vertex Buffer"),
                    contents: bytemuck::cast_slice(&vertices_buf),
                    usage: wgpu::BufferUsages::VERTEX,
                })
            };

        let index_buffer = if start_indices / BUFFER_MAX_INDICES == end_indices / BUFFER_MAX_INDICES
        {
            let buffer = indices
                .get((start_indices / BUFFER_MAX_INDICES) as i32)
                .unwrap()
                .unwrap();
            let buffer = env
                .get_direct_buffer_address(JByteBuffer::from(buffer))
                .unwrap();
            device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("Index Buffer"),
                contents: bytemuck::cast_slice(
                    &buffer[start_indices as usize..end_indices as usize],
                ),
                usage: wgpu::BufferUsages::INDEX,
            })
        } else {
            let mut indices_buf = vec![0; (end_indices - start_indices) as usize];
            let mut start = start_indices;
            let mut cursor = 0;
            loop {
                let buffer = indices
                    .get((start / BUFFER_MAX_INDICES) as i32)
                    .unwrap()
                    .unwrap();
                let buffer = env
                    .get_direct_buffer_address(JByteBuffer::from(buffer))
                    .unwrap();
                if start / BUFFER_MAX_INDICES == end_indices / BUFFER_MAX_INDICES {
                    let end = start - end_indices;
                    let (_left, right) = indices_buf.split_at_mut(cursor);
                    right.copy_from_slice(&buffer[start as usize..end as usize]);
                    break;
                } else {
                    let end = (start / BUFFER_MAX_INDICES + 1) * BUFFER_MAX_INDICES;
                    let (_left, right) = indices_buf.split_at_mut(cursor);
                    right.copy_from_slice(&buffer[start as usize..end as usize]);
                    start += BUFFER_MAX_INDICES;
                    cursor += (end - start) as usize
                }
            }
            device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
                label: Some("Index Buffer"),
                contents: bytemuck::cast_slice(&indices_buf),
                usage: wgpu::BufferUsages::INDEX,
            })
        };

        self.minecraft_meshes.push(MinecraftMesh::new(
            vertex_format,
            vertex_buffer,
            index_buffer,
            ((end_indices - start_indices) / 4) as u32,
        ))
    }

    fn apply_command(&mut self, renderer: &renderer::Renderer, env: JNIEnv, command: JObject) {
        self.minecraft_meshes = vec![];

        let camera = env
            .get_field(
                command,
                "camera",
                "Lnet/orito_itsuki/prototype_mc_wgpu/rust/WgpuCamera;",
            )
            .unwrap()
            .l()
            .unwrap();
        let fov_y = env
            .get_static_field(camera, "fovY", "F")
            .unwrap()
            .f()
            .unwrap();
        let near = env
            .get_static_field(camera, "near", "F")
            .unwrap()
            .f()
            .unwrap();
        let far = env
            .get_static_field(camera, "far", "F")
            .unwrap()
            .f()
            .unwrap();
        let aspect = env
            .get_static_field(camera, "aspectRatio", "F")
            .unwrap()
            .f()
            .unwrap();
        let pitch = env
            .get_static_field(camera, "pitch", "F")
            .unwrap()
            .f()
            .unwrap();
        let yaw = env
            .get_static_field(camera, "yaw", "F")
            .unwrap()
            .f()
            .unwrap();
        self.camera = Camera::new(fov_y, near, far, aspect, pitch, yaw);

        let minecraft_world = env
            .get_field(
                command,
                "minecraftWorld",
                "Lnet/orito_itsuki/prototype_mc_wgpu/rust/MinecraftWorld;",
            )
            .unwrap()
            .l()
            .unwrap();
        let vertices = env
            .get_static_field(minecraft_world, "vertices", "Ljava/util/ArrayList;")
            .unwrap()
            .l()
            .unwrap();
        let vertices = env.get_list(vertices).unwrap();
        let indices = env
            .get_static_field(minecraft_world, "indices", "Ljava/util/ArrayList;")
            .unwrap()
            .l()
            .unwrap();
        let indices = env.get_list(indices).unwrap();
        let meshes = env
            .get_static_field(minecraft_world, "meshes", "Ljava/util/ArrayList;")
            .unwrap()
            .l()
            .unwrap();
        let meshes = env.get_list(meshes).unwrap();
        for index in 0..meshes.size().unwrap() {
            let mesh = meshes.get(index).unwrap().unwrap();
            self.upload_mesh(renderer.device(), &env, mesh, &vertices, &indices);
        }
    }
}

static SCENE: Lazy<Arc<Mutex<Scene>>> = Lazy::new(|| Arc::new(Mutex::new(Scene::new())));

pub fn apply_command(env: JNIEnv, command: JObject) {
    let mut scene = SCENE.lock().unwrap();
    let renderer = renderer::get_renderer();
    let renderer = renderer.lock().unwrap();
    if let Some(renderer) = renderer.as_ref() {
        scene.apply_command(renderer, env, command);
    }
}

pub fn get_scene() -> Arc<Mutex<Scene>> {
    SCENE.clone()
}
