use std::sync::Arc;
use tokio::task::block_in_place;
use tokio::{runtime::Runtime, sync::mpsc::unbounded_channel};
use winit::{
    event::*,
    event_loop::{ControlFlow, EventLoop},
    window::{Window, WindowBuilder},
};

use crate::event::{self, MinecraftEvent};
use crate::window_size;

struct Renderer {
    window: Window,
    surface: wgpu::Surface,
    device: wgpu::Device,
    queue: wgpu::Queue,
    config: wgpu::SurfaceConfiguration,
    size: winit::dpi::PhysicalSize<u32>,
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

        Self {
            window,
            surface,
            device,
            queue,
            config,
            size,
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
        let output = self.surface.get_current_texture()?;
        let view = output
            .texture
            .create_view(&wgpu::TextureViewDescriptor::default());

        let mut encoder = self
            .device
            .create_command_encoder(&wgpu::CommandEncoderDescriptor {
                label: Some("Render Encoder"),
            });

        {
            let _render_pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
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
        }

        // submit will accept anything that implements IntoIter
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

    fn process_event(&mut self, event: MinecraftEvent) {
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
}

pub fn init() {
    env_logger::init();

    let (tx, mut rx) = unbounded_channel();
    event::set_tx(tx.clone());

    let runtime = Arc::new(Runtime::new().unwrap());
    runtime.spawn({
        let runtime = runtime.clone();
        async move {
            #[cfg(target_os = "linux")]
            let event_loop: EventLoop<()> =
                winit::platform::unix::EventLoopExtUnix::new_any_thread();
            #[cfg(target_os = "windows")]
            let event_loop: EventLoop<()> =
                winit::platform::windows::EventLoopExtWindows::new_any_thread();

            let window = WindowBuilder::new()
                .with_title("Wgpu Renderer")
                .build(&event_loop)
                .unwrap();

            let mut state = pollster::block_on(Renderer::new(window));

            runtime.spawn(async move {
                loop {
                    let event = rx.recv().await.unwrap();
                    state.process_event(event);
                }
            });

            block_in_place(|| {
                event_loop.run(move |event, _, control_flow| {
                    *control_flow = ControlFlow::Wait;
                    match event {
                        Event::WindowEvent { ref event, .. } => match event {
                            WindowEvent::CloseRequested => *control_flow = ControlFlow::Exit,
                            _ => (),
                        },
                        _ => (),
                    }
                    event::send(MinecraftEvent::WinitEvent(event.to_static()));
                });
            })
        }
    });
}
