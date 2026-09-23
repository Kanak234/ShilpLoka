#version 330 core

// ==============================================================================
// ShilpLoka — 2D UI / Overlay Vertex Shader
// Renders screen-space orthogonal 2D quads for HUD, F3 debug text, and crosshair.
// ==============================================================================

layout (location = 0) in vec2 a_Position;      // Screen coordinate (pixels: 0,0 top-left)
layout (location = 1) in vec2 a_TexCoords;     // Texture coordinate (0.0 - 1.0)
layout (location = 2) in vec4 a_Color;         // Vertex tint / font color (RGBA)

uniform mat4 u_OrthoProjection;                // Orthographic projection matching screen resolution

out vec2 v_TexCoords;
out vec4 v_Color;

void main() {
    v_TexCoords = a_TexCoords;
    v_Color = a_Color;
    gl_Position = u_OrthoProjection * vec4(a_Position, 0.0, 1.0);
}
