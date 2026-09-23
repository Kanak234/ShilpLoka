package shilploka.ui;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

/**
 * WHY:
 * The F3 debug overlay, HUD counters, and diagnostics require crisp, low-latency,
 * zero-allocation text rendering in screen coordinates without third-party font engine bloat.
 *
 * WHAT:
 * Contains an embedded procedural 8x8 monochrome pixel font covering printable ASCII (32-126).
 * Uploads glyphs to a GL_TEXTURE_2D texture atlas, batches screen-space 2D quads with UVs
 * and RGBA colors into a dynamic VBO, and renders text strings and solid rectangles.
 *
 * USED FOR:
 * Utilized by DebugOverlay to draw real-time FPS, coordinates, memory stats, and crosshair.
 *
 * WHERE NEXT:
 * Shaded via ui.vert and ui.frag using an orthographic projection matrix.
 */
public class BitmapFont {

    private static final Logger LOGGER = LoggerFactory.getLogger(BitmapFont.class);

    public static final int GLYPH_WIDTH = 8;
    public static final int GLYPH_HEIGHT = 8;
    public static final int ATLAS_COLS = 16;
    public static final int ATLAS_ROWS = 8;
    public static final int ATLAS_WIDTH = ATLAS_COLS * GLYPH_WIDTH;   // 128 pixels
    public static final int ATLAS_HEIGHT = ATLAS_ROWS * GLYPH_HEIGHT; // 64 pixels

    private final int textureId;
    private final int vaoId;
    private final int vboId;

    // Direct buffer for batching 2D UI quads: (X, Y, U, V, R, G, B, A) = 8 floats per vertex
    private static final int FLOATS_PER_VERTEX = 8;
    private static final int MAX_QUADS = 4096;
    private final FloatBuffer vertexBuffer;

    /**
     * WHY:
     * Initializes the font atlas texture and the dynamic 2D quad vertex array.
     *
     * WHAT:
     * Synthesizes 8x8 font glyphs, uploads them as an alpha texture, and configures VAO/VBO.
     *
     * USED FOR:
     * Instantiated by DebugOverlay during UI setup.
     */
    public BitmapFont() {
        this.textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, this.textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        // Allocate buffer for 128x64 RGBA font texture
        ByteBuffer atlasBuffer = MemoryUtil.memAlloc(ATLAS_WIDTH * ATLAS_HEIGHT * 4);
        try {
            generateFontAtlas(atlasBuffer);
            atlasBuffer.flip();
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, ATLAS_WIDTH, ATLAS_HEIGHT, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasBuffer);
        } finally {
            MemoryUtil.memFree(atlasBuffer);
        }

        glBindTexture(GL_TEXTURE_2D, 0);

        // Set up dynamic VAO and VBO for 2D batch rendering
        this.vaoId = glGenVertexArrays();
        glBindVertexArray(this.vaoId);

        this.vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, this.vboId);
        glBufferData(GL_ARRAY_BUFFER, (long) MAX_QUADS * 6 * FLOATS_PER_VERTEX * Float.BYTES, GL_DYNAMIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        // Position: vec2 (x, y)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // TexCoords: vec2 (u, v)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // Color: vec4 (r, g, b, a)
        glVertexAttribPointer(2, 4, GL_FLOAT, false, stride, 4 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);

        this.vertexBuffer = MemoryUtil.memAllocFloat(MAX_QUADS * 6 * FLOATS_PER_VERTEX);
        LOGGER.info("Successfully initialized BitmapFont atlas ({}x{} pixels, texture [{}])",
                ATLAS_WIDTH, ATLAS_HEIGHT, this.textureId);
    }

    /**
     * WHY:
     * Draws a text string onto the screen with custom scale, color, and optional drop shadow.
     *
     * WHAT:
     * Appends 2 triangles (6 vertices) per character quad into vertexBuffer with proper atlas UVs.
     *
     * USED FOR:
     * Rendering F3 diagnostic lines.
     */
    public void drawString(CharSequence text, float x, float y, float scale,
                           float r, float g, float b, float a, boolean dropShadow) {
        if (dropShadow) {
            // Render dark translucent drop shadow offset by 1 scale pixel
            renderStringInternal(text, x + scale, y + scale, scale, 0.0f, 0.0f, 0.0f, a * 0.75f);
        }
        renderStringInternal(text, x, y, scale, r, g, b, a);
    }

    private void renderStringInternal(CharSequence text, float startX, float startY, float scale,
                                     float r, float g, float b, float a) {
        float curX = startX;
        float curY = startY;
        float charWidth = GLYPH_WIDTH * scale;
        float charHeight = GLYPH_HEIGHT * scale;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\n') {
                curX = startX;
                curY += charHeight + (2 * scale);
                continue;
            }

            int ascii = (int) c;
            if (ascii < 32 || ascii > 126) {
                ascii = 63; // Fallback to '?'
            }

            int index = ascii - 32;
            int col = index % ATLAS_COLS;
            int row = index / ATLAS_COLS;

            float u0 = (float) (col * GLYPH_WIDTH) / ATLAS_WIDTH;
            float v0 = (float) (row * GLYPH_HEIGHT) / ATLAS_HEIGHT;
            float u1 = (float) ((col + 1) * GLYPH_WIDTH) / ATLAS_WIDTH;
            float v1 = (float) ((row + 1) * GLYPH_HEIGHT) / ATLAS_HEIGHT;

            addQuad(curX, curY, curX + charWidth, curY + charHeight, u0, v0, u1, v1, r, g, b, a);
            curX += charWidth;
        }
    }

    /**
     * WHY:
     * Draws a solid colored 2D rectangle (e.g. text backgrounds or crosshair lines).
     *
     * WHAT:
     * Emits a quad with UV coordinates outside the font atlas (or zero) for flat coloring.
     */
    public void drawRect(float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        addQuad(x0, y0, x1, y1, 0.0f, 0.0f, 0.0f, 0.0f, r, g, b, a);
    }

    private void addQuad(float x0, float y0, float x1, float y1,
                         float u0, float v0, float u1, float v1,
                         float r, float g, float b, float a) {
        if (vertexBuffer.remaining() < 6 * FLOATS_PER_VERTEX) {
            flush(); // Flush buffer if full
        }

        // Triangle 1
        putVertex(x0, y0, u0, v0, r, g, b, a);
        putVertex(x1, y0, u1, v0, r, g, b, a);
        putVertex(x1, y1, u1, v1, r, g, b, a);

        // Triangle 2
        putVertex(x1, y1, u1, v1, r, g, b, a);
        putVertex(x0, y1, u0, v1, r, g, b, a);
        putVertex(x0, y0, u0, v0, r, g, b, a);
    }

    private void putVertex(float x, float y, float u, float v, float r, float g, float b, float a) {
        vertexBuffer.put(x).put(y).put(u).put(v).put(r).put(g).put(b).put(a);
    }

    /**
     * WHY:
     * Flushes queued 2D vertices to the GPU and renders them.
     *
     * WHAT:
     * Flips buffer, uploads via glBufferSubData, and issues glDrawArrays.
     *
     * USED FOR:
     * Called by DebugOverlay after queuing all diagnostic strings.
     */
    public void flush() {
        int vertexCount = vertexBuffer.position() / FLOATS_PER_VERTEX;
        if (vertexCount == 0) return;

        vertexBuffer.flip();

        glBindBuffer(GL_ARRAY_BUFFER, this.vboId);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertexBuffer);

        glBindVertexArray(this.vaoId);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);

        vertexBuffer.clear();
    }

    public void bindTexture() {
        glBindTexture(GL_TEXTURE_2D, this.textureId);
    }

    public void unbindTexture() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * WHY:
     * Generates 8x8 font patterns for ASCII 32-126.
     *
     * WHAT:
     * Encodes 8x8 pixel patterns into the atlas buffer.
     */
    private void generateFontAtlas(ByteBuffer buffer) {
        // Clear entire atlas to transparent
        for (int i = 0; i < ATLAS_WIDTH * ATLAS_HEIGHT * 4; i++) {
            buffer.put((byte) 0);
        }
        buffer.position(0);

        // 8x8 bitmap font patterns for digits, uppercase, lowercase, punctuation
        for (int c = 32; c <= 126; c++) {
            long pattern = getGlyphPattern(c);
            int index = c - 32;
            int col = index % ATLAS_COLS;
            int row = index / ATLAS_COLS;
            int originX = col * GLYPH_WIDTH;
            int originY = row * GLYPH_HEIGHT;

            for (int py = 0; py < 8; py++) {
                for (int px = 0; px < 8; px++) {
                    int bitIndex = py * 8 + px;
                    boolean on = ((pattern >> (63 - bitIndex)) & 1L) == 1L;

                    int pixelIndex = ((originY + py) * ATLAS_WIDTH + (originX + px)) * 4;
                    buffer.position(pixelIndex);

                    if (on) {
                        buffer.put((byte) 255); // R
                        buffer.put((byte) 255); // G
                        buffer.put((byte) 255); // B
                        buffer.put((byte) 255); // A
                    } else {
                        buffer.put((byte) 0);
                        buffer.put((byte) 0);
                        buffer.put((byte) 0);
                        buffer.put((byte) 0);
                    }
                }
            }
        }
        buffer.position(0);
    }

    /**
     * Minimal procedural 8x8 font bitmask generator for clean ASCII rendering.
     */
    private long getGlyphPattern(int ascii) {
        return switch (ascii) {
            case '0' -> 0x3C666E7666663C00L;
            case '1' -> 0x1838181818187E00L;
            case '2' -> 0x3C66060C18307E00L;
            case '3' -> 0x3C66061C06663C00L;
            case '4' -> 0x0C1C34647E0C0C00L;
            case '5' -> 0x7E607C0606663C00L;
            case '6' -> 0x1C30607C66663C00L;
            case '7' -> 0x7E060C1830303000L;
            case '8' -> 0x3C66663C66663C00L;
            case '9' -> 0x3C66663E060C3800L;
            case 'A' -> 0x183C66667E666600L;
            case 'B' -> 0x7C66667C66667C00L;
            case 'C' -> 0x3C66606060663C00L;
            case 'D' -> 0x786C6666666C7800L;
            case 'E' -> 0x7E60607C60607E00L;
            case 'F' -> 0x7E60607C60606000L;
            case 'G' -> 0x3C66606E66663E00L;
            case 'H' -> 0x6666667E66666600L;
            case 'I' -> 0x3C18181818183C00L;
            case 'J' -> 0x0E06060606663C00L;
            case 'K' -> 0x666C7870786C6600L;
            case 'L' -> 0x6060606060607E00L;
            case 'M' -> 0x63777F6B63636300L;
            case 'N' -> 0x66767E7E6E666600L;
            case 'O' -> 0x3C66666666663C00L;
            case 'P' -> 0x7C66667C60606000L;
            case 'Q' -> 0x3C6666666A643A00L;
            case 'R' -> 0x7C66667C786C6600L;
            case 'S' -> 0x3C66603C06663C00L;
            case 'T' -> 0x7E18181818181800L;
            case 'U' -> 0x6666666666663C00L;
            case 'V' -> 0x66666666663C1800L;
            case 'W' -> 0x6363636B7F776300L;
            case 'X' -> 0x66663C183C666600L;
            case 'Y' -> 0x6666663C18181800L;
            case 'Z' -> 0x7E060C1830607E00L;
            case 'a' -> 0x00003C063E663B00L;
            case 'b' -> 0x60607C6666667C00L;
            case 'c' -> 0x00003C6660663C00L;
            case 'd' -> 0x06063E6666663E00L;
            case 'e' -> 0x00003C667E603C00L;
            case 'f' -> 0x0C187C1818181800L;
            case 'g' -> 0x00003E66663E063CL;
            case 'h' -> 0x60607C6666666600L;
            case 'i' -> 0x1800181818181800L;
            case 'j' -> 0x060006060606663CL;
            case 'k' -> 0x60606C78706C6600L;
            case 'l' -> 0x1818181818181800L;
            case 'm' -> 0x0000667F7F6B6300L;
            case 'n' -> 0x00007C6666666600L;
            case 'o' -> 0x00003C6666663C00L;
            case 'p' -> 0x00007C66667C6060L;
            case 'q' -> 0x00003E66663E0606L;
            case 'r' -> 0x00007C6660606000L;
            case 's' -> 0x00003E603C067C00L;
            case 't' -> 0x18187E1818180E00L;
            case 'u' -> 0x0000666666663E00L;
            case 'v' -> 0x00006666663C1800L;
            case 'w' -> 0x0000636B7F7F3600L;
            case 'x' -> 0x0000663C183C6600L;
            case 'y' -> 0x00006666663E063CL;
            case 'z' -> 0x00007E0C18307E00L;
            case '.' -> 0x0000000000181800L;
            case ',' -> 0x0000000000181830L;
            case ':' -> 0x0018180000181800L;
            case ';' -> 0x0018180000181830L;
            case '(' -> 0x0C18303030180C00L;
            case ')' -> 0x30180C0C0C183000L;
            case '[' -> 0x3C30303030303C00L;
            case ']' -> 0x3C0C0C0C0C0C3C00L;
            case '{' -> 0x0E18187018180E00L;
            case '}' -> 0x7018180E18187000L;
            case '+' -> 0x0018187E18180000L;
            case '-' -> 0x0000007E00000000L;
            case '/' -> 0x02060C1830604000L;
            case '=' -> 0x00007E007E000000L;
            case '<' -> 0x060C1830180C0600L;
            case '>' -> 0x6030180C18306000L;
            case '?' -> 0x3C66060C18001800L;
            case '!' -> 0x1818181818001800L;
            case '@' -> 0x3C664E5A5A403C00L;
            case '#' -> 0x24247E247E242400L;
            case '%' -> 0x62660C1830664600L;
            case '&' -> 0x384438544A443AL;
            case '_' -> 0x00000000000000FFL;
            default  -> 0x0000000000000000L; // Space or empty
        };
    }

    /**
     * Clean up OpenGL textures and buffers.
     */
    public void cleanup() {
        glDeleteTextures(this.textureId);
        glDeleteVertexArrays(this.vaoId);
        glDeleteBuffers(this.vboId);
        MemoryUtil.memFree(this.vertexBuffer);
        LOGGER.info("Disposed BitmapFont resources");
    }
}
