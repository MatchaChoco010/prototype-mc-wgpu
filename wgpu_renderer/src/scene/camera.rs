pub struct Camera {
    pub fov_y: f32,
    pub near: f32,
    pub far: f32,
    pub aspect_ratio: f32,
    pub pitch: f32,
    pub yaw: f32,
}
impl Camera {
    pub fn new(fov_y: f32, near: f32, far: f32, aspect_ratio: f32, pitch: f32, yaw: f32) -> Self {
        Self {
            fov_y,
            near,
            far,
            aspect_ratio,
            pitch,
            yaw,
        }
    }
}
