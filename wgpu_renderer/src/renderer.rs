use std::sync::Arc;
use winit::window::Window;

use crate::resources::minecraft::texture_manager::MINECRAFT_TEXTURE_MANAGER;
use crate::scene::{MinecraftMeshVertexFormat, Scene};

mod camera_buffer;
mod render_pipeline;
mod vertex;
use camera_buffer::CameraBuffer;

pub struct Renderer {
    surface: wgpu::Surface,
    device: Arc<wgpu::Device>,
    queue: Arc<wgpu::Queue>,
    config: wgpu::SurfaceConfiguration,
    size: winit::dpi::PhysicalSize<u32>,
    camera: CameraBuffer,
}
impl Renderer {
    pub fn new(
        window: &Window,
        adapter: wgpu::Adapter,
        surface: wgpu::Surface,
        device: Arc<wgpu::Device>,
        queue: Arc<wgpu::Queue>,
    ) -> Self {
        let size = window.inner_size();

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface.get_supported_formats(&adapter)[0],
            width: size.width,
            height: size.height,
            present_mode: wgpu::PresentMode::Fifo,
        };
        surface.configure(&device, &config);

        let camera = CameraBuffer::new(&device);

        Self {
            surface,
            device,
            queue,
            config,
            size,
            camera,
        }
    }

    pub fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size = new_size;
            self.config.width = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
        }
    }

    pub fn render(&mut self, scene: &Scene) -> Result<(), wgpu::SurfaceError> {
        let output = self.surface.get_current_texture()?;
        let view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        self.camera.update_view_proj(&self.queue, &scene.camera);

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("Render Encoder"),
            });

        let mut texture_bind_groups = vec![];
        let texture_bind_group_layout =
            self.device
                .create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
                    entries: &[
                        wgpu::BindGroupLayoutEntry {
                            binding: 0,
                            visibility: wgpu::ShaderStages::FRAGMENT,
                            ty: wgpu::BindingType::Texture {
                                multisampled: false,
                                view_dimension: wgpu::TextureViewDimension::D2,
                                sample_type: wgpu::TextureSampleType::Float { filterable: true },
                            },
                            count: None,
                        },
                        wgpu::BindGroupLayoutEntry {
                            binding: 1,
                            visibility: wgpu::ShaderStages::FRAGMENT,
                            ty: wgpu::BindingType::Sampler(wgpu::SamplerBindingType::Filtering),
                            count: None,
                        },
                    ],
                    label: Some("texture_bind_group_layout"),
                });

        let render_pipeline = render_pipeline::create_position_color_texture_light_normal(
            &self.device,
            self.config.format,
            &texture_bind_group_layout,
            &self.camera.bind_group_layout(),
        );

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Render Pass"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: &view,
                    resolve_target: None,
                    ops: wgpu::Operations {
                        load: wgpu::LoadOp::Clear(wgpu::Color {
                            r: 0.1,
                            g: 0.2,
                            b: 0.3,
                            a: 1.0,
                        }),
                        store: true,
                    },
                })],
                depth_stencil_attachment: None,
            });
            render_pass.set_bind_group(1, self.camera.bind_group(), &[]);

            let texture_manager = MINECRAFT_TEXTURE_MANAGER.lock().unwrap();
            let texture_manager = texture_manager.as_ref().unwrap();

            for mesh in &scene.minecraft_meshes {
                let texture = texture_manager.get_texture(mesh.texture_id.clone());
                let texture_bind_group =
                    self.device.create_bind_group(&wgpu::BindGroupDescriptor {
                        layout: &texture_bind_group_layout,
                        entries: &[
                            wgpu::BindGroupEntry {
                                binding: 0,
                                resource: wgpu::BindingResource::TextureView(&texture.view),
                            },
                            wgpu::BindGroupEntry {
                                binding: 1,
                                resource: wgpu::BindingResource::Sampler(&texture.sampler),
                            },
                        ],
                        label: Some(&format!("{} texture_bind_group", mesh.texture_id.0)),
                    });
                texture_bind_groups.push(texture_bind_group);
            }

            for (i, mesh) in scene.minecraft_meshes.iter().enumerate() {
                match mesh.vertex_format {
                    MinecraftMeshVertexFormat::PositionColorTextureLightNormal => {
                        render_pass.set_pipeline(&render_pipeline);
                        render_pass.set_bind_group(0, &texture_bind_groups[i], &[]);
                        render_pass.set_vertex_buffer(0, mesh.vertex_buffer.slice(..));
                        render_pass.set_index_buffer(
                            mesh.index_buffer.slice(..),
                            wgpu::IndexFormat::Uint32,
                        );

                        render_pass.draw_indexed(0..mesh.num_indices, 0, 0..1);
                    }
                }
            }
        }

        self.queue.submit(std::iter::once(encoder.finish()));
        output.present();

        Ok(())
    }
}
