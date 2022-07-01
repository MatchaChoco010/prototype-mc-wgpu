use once_cell::sync::Lazy;
use std::sync::{Arc, Mutex};
use winit::window::Window;

pub struct WindowSize {
    inner_width: u32,
    inner_height: u32,
    scale_factor: f64,
    scaled_inner_width: u32,
    scaled_inner_height: u32,
}
impl WindowSize {
    fn new() -> Self {
        Self {
            inner_width: 0,
            inner_height: 0,
            scale_factor: 1.0,
            scaled_inner_width: 0,
            scaled_inner_height: 0,
        }
    }

    fn update(&mut self, window: &winit::window::Window) {
        let size = window.inner_size();
        let scale_factor = window.scale_factor();
        let scaled_inner_width = (size.width as f64 / scale_factor) as u32;
        let scaled_inner_width = if (scaled_inner_width as f64) < (size.width as f64 / scale_factor)
        {
            scaled_inner_width + 1
        } else {
            scaled_inner_width
        };
        let scaled_inner_height = (size.height as f64 / scale_factor) as u32;
        let scaled_inner_height =
            if (scaled_inner_height as f64) < (size.height as f64 / scale_factor) {
                scaled_inner_height + 1
            } else {
                scaled_inner_height
            };

        if self.inner_width != size.width || self.inner_height != size.height {
            self.inner_width = size.width;
            self.inner_height = size.height;
            self.scale_factor = scale_factor;
            self.scaled_inner_width = scaled_inner_width;
            self.scaled_inner_height = scaled_inner_height;
        }
    }

    pub fn inner_width(&self) -> u32 {
        self.inner_width
    }

    pub fn inner_height(&self) -> u32 {
        self.inner_height
    }

    pub fn scaled_inner_width(&self) -> u32 {
        self.scaled_inner_width
    }

    pub fn scaled_inner_height(&self) -> u32 {
        self.scaled_inner_height
    }
}

static WINDOW_SIZE: Lazy<Arc<Mutex<WindowSize>>> =
    Lazy::new(|| Arc::new(Mutex::new(WindowSize::new())));

pub fn inner_size() -> (u32, u32) {
    let window_size = WINDOW_SIZE.lock().unwrap();
    (window_size.inner_width(), window_size.inner_height())
}

pub fn scaled_inner_size() -> (u32, u32) {
    let window_size = WINDOW_SIZE.lock().unwrap();
    (
        window_size.scaled_inner_width(),
        window_size.scaled_inner_height(),
    )
}

pub fn scale_factor() -> f64 {
    let window_size = WINDOW_SIZE.lock().unwrap();
    window_size.scale_factor
}

pub fn update_window_size(window: &Window) {
    let mut window_size = WINDOW_SIZE.lock().unwrap();
    window_size.update(window);
}
