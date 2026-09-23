#version 330 core

// ==============================================================================
// ShilpLoka — 2D UI / Overlay Fragment Shader
// Renders colored rectangular panels, borders, crosshair, and bitmap font glyphs.
// ==============================================================================

in vec2 v_TexCoords;
in vec4 v_Color;

uniform bool u_UseTexture;                     // Flag whether to sample texture or use flat color
uniform sampler2D u_FontTexture;               // Bitmap font texture atlas

out vec4 FragColor;

void main() {
    if (u_UseTexture) {
        vec4 texColor = texture(u_FontTexture, v_TexCoords);
        FragColor = vec4(v_Color.rgb, v_Color.a * texColor.a);
    } else {
        FragColor = v_Color;
    }
}
