use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use tokio::task::block_in_place;
use winit::{
    event::*,
    event_loop::{ControlFlow, EventLoop},
    window::{Window, WindowBuilder},
};

use crate::renderer::Renderer;
use crate::resources;
use crate::scene;

pub struct WindowSize {
    inner_width: u32,
    inner_height: u32,
}
impl WindowSize {
    fn new() -> Self {
        Self {
            inner_width: 0,
            inner_height: 0,
        }
    }

    fn update(&mut self, window: &winit::window::Window) {
        let size = window.inner_size();
        self.inner_width = size.width;
        self.inner_height = size.height;
    }
}

static WINDOW_SIZE: Lazy<Arc<Mutex<WindowSize>>> =
    Lazy::new(|| Arc::new(Mutex::new(WindowSize::new())));

pub fn inner_size() -> (u32, u32) {
    let window_size = WINDOW_SIZE.lock().unwrap();
    (window_size.inner_width, window_size.inner_height)
}

pub fn update_window_size(window: &Window) {
    let mut window_size = WINDOW_SIZE.lock().unwrap();
    window_size.update(window);
}

pub fn init() {
    env_logger::init();

    crate::RUNTIME.spawn({
        async move {
            #[cfg(target_os = "linux")]
            let event_loop: EventLoop<()> =
                winit::platform::unix::EventLoopExtUnix::new_any_thread();
            #[cfg(target_os = "windows")]
            let event_loop: EventLoop<()> =
                winit::platform::windows::EventLoopExtWindows::new_any_thread();

            let window = WindowBuilder::new()
                .with_title("Wgpu Renderer")
                .with_inner_size(winit::dpi::PhysicalSize::new(800, 600))
                .build(&event_loop)
                .unwrap();
            update_window_size(&window);

            let instance = wgpu::Instance::new(wgpu::Backends::all());
            let surface = unsafe { instance.create_surface(&window) };
            let adapter = pollster::block_on(async {
                instance
                    .request_adapter(&wgpu::RequestAdapterOptions {
                        power_preference: wgpu::PowerPreference::default(),
                        compatible_surface: Some(&surface),
                        force_fallback_adapter: false,
                    })
                    .await
                    .unwrap()
            });
            let (device, queue) = pollster::block_on(async {
                adapter
                    .request_device(
                        &wgpu::DeviceDescriptor {
                            features: wgpu::Features::empty(),
                            limits: wgpu::Limits::default(),
                            label: None,
                        },
                        None,
                    )
                    .await
                    .unwrap()
            });
            let device = Arc::new(device);
            let queue = Arc::new(queue);

            let mut renderer =
                Renderer::new(&window, adapter, surface, device.clone(), queue.clone());

            resources::minecraft::texture_manager::init(device.clone(), queue.clone());

            scene::init(device.clone());

            block_in_place(|| {
                event_loop.run(move |event, _, control_flow| {
                    *control_flow = ControlFlow::Poll;
                    match event {
                        Event::WindowEvent {
                            ref event,
                            window_id,
                        } if window_id == window.id() => {
                            // input(event);
                            match event {
                                WindowEvent::CloseRequested => *control_flow = ControlFlow::Exit,
                                WindowEvent::Resized(physical_size) => {
                                    update_window_size(&window);
                                    renderer.resize(*physical_size);
                                }
                                WindowEvent::ScaleFactorChanged { new_inner_size, .. } => {
                                    update_window_size(&window);
                                    renderer.resize(**new_inner_size);
                                }
                                _ => (),
                            }
                        }
                        Event::RedrawRequested(window_id) if window_id == window.id() => {
                            let scene = scene::render_scene();
                            let scene = scene.as_ref().unwrap();
                            match renderer.render(&scene) {
                                Ok(_) => {}
                                Err(wgpu::SurfaceError::Lost) => {
                                    renderer.resize(window.inner_size())
                                }
                                Err(e) => eprintln!("{:?}", e),
                            }
                        }
                        Event::MainEventsCleared => window.request_redraw(),
                        _ => (),
                    }
                });
            })
        }
    });
}
