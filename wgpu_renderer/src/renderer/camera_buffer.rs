use crevice::std140::AsStd140;
use wgpu::util::DeviceExt;

use crate::scene::Camera;

#[derive(AsStd140)]
struct CameraUniform {
    view_proj: mint::ColumnMatrix4<f32>,
}
impl CameraUniform {
    fn new() -> Self {
        Self {
            view_proj: vek::Mat4::identity().into(),
        }
    }
}

pub struct CameraBuffer {
    uniform: CameraUniform,
    buffer: wgpu::Buffer,
    _bind_group: wgpu::BindGroup,
    bind_group_layout: wgpu::BindGroupLayout,
}
impl CameraBuffer {
    pub fn new(device: &wgpu::Device) -> Self {
        let uniform = CameraUniform::new();

        let buffer = device.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("camera_buffer"),
            contents: uniform.as_std140().as_bytes(),
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
        });

        let bind_group_layout = device.create_bind_group_layout(&wgpu::BindGroupLayoutDescriptor {
            entries: &[wgpu::BindGroupLayoutEntry {
                binding: 0,
                visibility: wgpu::ShaderStages::VERTEX,
                ty: wgpu::BindingType::Buffer {
                    ty: wgpu::BufferBindingType::Uniform,
                    has_dynamic_offset: false,
                    min_binding_size: None,
                },
                count: None,
            }],
            label: Some("camera_bind_group_layout"),
        });
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            layout: &bind_group_layout,
            entries: &[wgpu::BindGroupEntry {
                binding: 0,
                resource: buffer.as_entire_binding(),
            }],
            label: Some("camera_bind_group"),
        });
        Self {
            uniform,
            buffer,
            _bind_group: bind_group,
            bind_group_layout,
        }
    }

    pub fn update_view_proj(&mut self, queue: &wgpu::Queue, camera: &Camera) {
        // self.uniform.view_proj = (vek::Mat4::perspective_rh_zo(
        //     camera.fov_y,
        //     camera.aspect_ratio,
        //     camera.near,
        //     camera.far,
        // ) * vek::Mat4::rotation_y(camera.yaw)
        //     * vek::Mat4::rotation_x(camera.pitch))
        self.uniform.view_proj = vek::Mat4::perspective_rh_zo(
            camera.fov_y,
            camera.aspect_ratio,
            camera.near,
            camera.far,
        )
        .into();
        queue.write_buffer(&self.buffer, 0, self.uniform.as_std140().as_bytes());
    }

    pub fn bind_group(&self) -> &wgpu::BindGroup {
        &self._bind_group
    }

    pub fn bind_group_layout(&self) -> &wgpu::BindGroupLayout {
        &self.bind_group_layout
    }
}
