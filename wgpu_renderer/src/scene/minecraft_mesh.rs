#[derive(Debug, PartialEq, Eq)]
pub enum MinecraftMeshVertexFormat {
    PositionColorTextureLightNormal,
}

pub struct MinecraftMesh {
    pub vertex_format: MinecraftMeshVertexFormat,
    pub vertex_buffer: wgpu::Buffer,
    pub index_buffer: wgpu::Buffer,
    pub num_indices: u32,
}
impl MinecraftMesh {
    pub fn new(
        vertex_format: MinecraftMeshVertexFormat,
        vertex_buffer: wgpu::Buffer,
        index_buffer: wgpu::Buffer,
        num_indices: u32,
    ) -> Self {
        Self {
            vertex_format,
            vertex_buffer,
            index_buffer,
            num_indices,
        }
    }
}
