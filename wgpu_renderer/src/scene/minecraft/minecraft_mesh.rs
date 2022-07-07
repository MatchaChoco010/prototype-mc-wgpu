use std::sync::Arc;

use crate::resources::Identifier;

#[derive(Debug, PartialEq, Eq, Clone, Copy)]
pub enum MinecraftMeshVertexFormat {
    PositionColorTextureLightNormal,
}

#[derive(Debug, Clone)]
pub struct MinecraftMesh {
    pub vertex_format: MinecraftMeshVertexFormat,
    pub vertex_buffer: Arc<wgpu::Buffer>,
    pub index_buffer: Arc<wgpu::Buffer>,
    pub num_indices: u32,
    pub texture_id: Identifier,
}
impl MinecraftMesh {
    pub fn new(
        vertex_format: MinecraftMeshVertexFormat,
        vertex_buffer: Arc<wgpu::Buffer>,
        index_buffer: Arc<wgpu::Buffer>,
        num_indices: u32,
        texture_id: Identifier,
    ) -> Self {
        Self {
            vertex_format,
            vertex_buffer,
            index_buffer,
            num_indices,
            texture_id,
        }
    }
}
