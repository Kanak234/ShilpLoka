#version 330 core

// ==============================================================================
// ShilpLoka — Voxel Chunk Fragment Shader
// Samples 2D texture array slice without bleeding and applies combined lighting,
// ambient occlusion, directional shading, and atmospheric distance fog.
// ==============================================================================

in vec2 v_TexCoords;
in float v_LayerIndex;
in vec3 v_Lighting; // x: Combined Light, y: AO factor, z: Directional
in vec3 v_WorldPos;

uniform sampler2DArray u_TextureArray;         // 16x16 Texture Array holding all block faces
uniform vec3 u_CameraPos;                      // Player eye position for fog calculation
uniform vec3 u_FogColor;                       // Dynamic atmospheric fog color from DayNightCycle
uniform float u_FogNear;                       // Fog onset distance
uniform float u_FogFar;                        // Maximum fog distance

out vec4 FragColor;

void main() {
    // Sample texture layer corresponding to the block face
    vec4 texColor = texture(u_TextureArray, vec3(v_TexCoords, v_LayerIndex));

    // Alpha cutoff for transparent cutouts (leaves, glass)
    if (texColor.a < 0.1) {
        discard;
    }

    // Multiply block texture by combined light * ambient occlusion * directional face shading
    float netBrightness = v_Lighting.x * v_Lighting.y * v_Lighting.z;
    vec3 litColor = texColor.rgb * netBrightness;

    // Atmospheric distance fog blending
    float dist = distance(v_WorldPos, u_CameraPos);
    float fogFactor = clamp((dist - u_FogNear) / max(0.001, (u_FogFar - u_FogNear)), 0.0, 1.0);
    vec3 finalColor = mix(litColor, u_FogColor, fogFactor);

    FragColor = vec4(finalColor, texColor.a);
}
