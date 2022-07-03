use std::sync::Arc;
use tokio::task::block_in_place;
use tokio::{runtime::Runtime, sync::mpsc::unbounded_channel};
use winit::{
    event::*,
    event_loop::{ControlFlow, EventLoop},
    window::WindowBuilder,
};

use crate::event::{self, MinecraftEvent};
use crate::renderer;

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
                .with_inner_size(winit::dpi::PhysicalSize::new(800, 600))
                .build(&event_loop)
                .unwrap();

            pollster::block_on(renderer::new(window));

            runtime.spawn(async move {
                loop {
                    let event = rx.recv().await.unwrap();
                    let renderer = renderer::get_renderer();
                    let mut renderer = renderer.lock().unwrap();
                    let renderer = renderer.as_mut().unwrap();
                    renderer.process_event(event);
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
