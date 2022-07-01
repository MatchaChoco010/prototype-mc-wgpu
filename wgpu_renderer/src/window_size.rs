use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use winit::window::Window;

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

    pub fn inner_width(&self) -> u32 {
        self.inner_width
    }

    pub fn inner_height(&self) -> u32 {
        self.inner_height
    }
}

static WINDOW_SIZE: Lazy<Arc<Mutex<WindowSize>>> =
    Lazy::new(|| Arc::new(Mutex::new(WindowSize::new())));

pub fn inner_size() -> (u32, u32) {
    let window_size = WINDOW_SIZE.lock().unwrap();
    (window_size.inner_width(), window_size.inner_height())
}

pub fn update_window_size(window: &Window) {
    let mut window_size = WINDOW_SIZE.lock().unwrap();
    window_size.update(window);
}
