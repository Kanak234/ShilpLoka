#version 330 core

// ==============================================================================
// ShilpLoka — Voxel Chunk Vertex Shader
// Transforms 3D voxel quad vertices and passes texture array layer, UVs,
// cellular light levels (sky + block light), ambient occlusion, and directional
// lighting to the fragment shader.
// ==============================================================================

// Input vertex attributes
layout (location = 0) in vec3 a_Position;      // Local voxel vertex position (X, Y, Z)
layout (location = 1) in vec2 a_TexCoords;     // Face UV texture coordinates (0.0 - 1.0)
layout (location = 2) in float a_LayerIndex;   // Layer index in GL_TEXTURE_2D_ARRAY
layout (location = 3) in vec4 a_LightData;     // x: SkyLight, y: BlockLight, z: AmbientOcclusion, w: DirectionalShade

// Uniforms
uniform mat4 u_ViewProjection;                 // Combined View * Projection matrix
uniform vec3 u_ChunkOffset;                    // World translation offset for chunk (X, Y, Z)
uniform float u_SunlightFactor;                // Celestial time-of-day sky multiplier (0.15 to 1.0)

// Outputs to fragment shader
out vec2 v_TexCoords;
out float v_LayerIndex;
out vec3 v_Lighting;                          // x: Combined Light, y: AO factor, z: Directional
out vec3 v_WorldPos;

void main() {
    v_TexCoords = a_TexCoords;
    v_LayerIndex = a_LayerIndex;

    // Attenuate Sky Light by celestial time of day (0.15 at night to 1.0 at noon)
    float effectiveSky = (a_LightData.x / 15.0) * u_SunlightFactor;
    float block = a_LightData.y / 15.0;

    // Combined illumination clamped with minimum ambient starlight (0.08)
    float combinedLight = clamp(max(effectiveSky, block), 0.08, 1.0);

    // Smooth ambient occlusion curve: 0 -> 0.45, 1 -> 0.65, 2 -> 0.82, 3 -> 1.0
    float aoFactor = 0.45 + 0.55 * (a_LightData.z / 3.0);

    v_Lighting = vec3(combinedLight, aoFactor, a_LightData.w);

    vec4 worldPos = vec4(a_Position + u_ChunkOffset, 1.0);
    v_WorldPos = worldPos.xyz;
    gl_Position = u_ViewProjection * worldPos;
}
