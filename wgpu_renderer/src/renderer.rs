use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use winit::{event::*, window::Window};

use crate::event::MinecraftEvent;
use crate::scene::{self, MinecraftMeshVertexFormat};
use crate::window_size;

mod camera_buffer;
mod render_pipeline;
mod vertex;
use camera_buffer::CameraBuffer;

pub struct Renderer {
    window: Window,
    surface: wgpu::Surface,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: winit::dpi::PhysicalSize<u32>,
    render_pipeline: wgpu::RenderPipeline,
    camera: CameraBuffer,
}
impl Renderer {
    async fn new(window: Window) -> Self {
        let size = window.inner_size();

        let instance = wgpu::Instance::new(wgpu::Backends::all());
        let surface = unsafe { instance.create_surface(&window) };
        let adapter = instance
            .request_adapter(&wgpu::RequestAdapterOptions {
                power_preference: wgpu::PowerPreference::default(),
                compatible_surface: Some(&surface),
                force_fallback_adapter: false,
            })
            .await
            .unwrap();

        let (device, queue) = adapter
            .request_device(
                &wgpu::DeviceDescriptor {
                    features: wgpu::Features::empty(),
                    limits: wgpu::Limits::default(),
                    label: None,
                },
                None,
            )
            .await
            .unwrap();

        let config = wgpu::SurfaceConfiguration {
            usage: wgpu::TextureUsages::RENDER_ATTACHMENT,
            format: surface.get_preferred_format(&adapter).unwrap(),
            width: size.width,
            height: size.height,
            present_mode: wgpu::PresentMode::Fifo,
        };
        surface.configure(&device, &config);

        let camera = CameraBuffer::new(&device);

        let render_pipeline = render_pipeline::create_position_color_texture_light_normal(
            &device,
            config.format,
            camera.bind_group_layout(),
        );

        Self {
            window,
            surface,
            device,
            queue,
            config,
            size,
            render_pipeline,
            camera,
        }
    }

    fn resize(&mut self, new_size: winit::dpi::PhysicalSize<u32>) {
        if new_size.width > 0 && new_size.height > 0 {
            self.size = new_size;
            self.config.width = new_size.width;
            self.config.height = new_size.height;
            self.surface.configure(&self.device, &self.config);
        }
    }

    fn input(&mut self, _event: &WindowEvent) {
        //
    }

    fn update(&mut self) {
        //
    }

    fn render(&mut self) -> Result<(), wgpu::SurfaceError> {
        let scene = scene::get_scene();
        let scene = scene.lock().unwrap();

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

        {
            let mut render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("Render Pass"),
                color_attachments: &[wgpu::RenderPassColorAttachment {
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
                }],
                depth_stencil_attachment: None,
            });
            render_pass.set_bind_group(0, self.camera.bind_group(), &[]);

            for mesh in &scene.minecraft_meshes {
                match mesh.vertex_format {
                    MinecraftMeshVertexFormat::PositionColorTextureLightNormal => {
                        render_pass.set_pipeline(&self.render_pipeline);
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

    fn process_winit_event(&mut self, event: Event<'static, ()>) {
        match event {
            Event::WindowEvent {
                ref event,
                window_id,
            } if window_id == self.window.id() => {
                self.input(event);
                match event {
                    WindowEvent::Resized(physical_size) => {
                        window_size::update_window_size(&self.window);
                        self.resize(*physical_size);
                    }
                    WindowEvent::ScaleFactorChanged { new_inner_size, .. } => {
                        window_size::update_window_size(&self.window);
                        self.resize(**new_inner_size);
                    }
                    _ => (),
                }
            }
            Event::RedrawRequested(window_id) if window_id == self.window.id() => {
                self.update();
                match self.render() {
                    Ok(_) => {}
                    Err(wgpu::SurfaceError::Lost) => self.resize(self.size),
                    Err(e) => eprintln!("{:?}", e),
                }
            }
            _ => (),
        }
    }

    pub fn process_event(&mut self, event: MinecraftEvent) {
        match event {
            MinecraftEvent::Draw => {
                self.window.request_redraw();
            }
            MinecraftEvent::WinitEvent(event) => {
                if let Some(event) = event {
                    self.process_winit_event(event);
                }
            }
        }
    }

    pub fn device(&self) -> &wgpu::Device {
        &self.device
    }
}

static RENDERER: Lazy<Arc<Mutex<Option<Renderer>>>> = Lazy::new(|| Arc::new(Mutex::new(None)));

pub async fn new(window: Window) {
    let renderer = Renderer::new(window).await;
    *RENDERER.lock().unwrap() = Some(renderer);
}

pub fn get_renderer() -> Arc<Mutex<Option<Renderer>>> {
    RENDERER.clone()
}
