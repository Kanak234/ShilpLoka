package shilploka.render.chunk;

import shilploka.render.TextureArray;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkSection;
import shilploka.world.light.WorldLightingAccess;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * WHY:
 * Rendering individual 1x1 voxel cube faces produces excessive vertex and index traffic
 * to the GPU. Greedy meshing sweeps through 2D slices along each coordinate axis (X, Y, Z)
 * and coalesces adjacent coplanar faces sharing identical block textures, sky light,
 * block light, and ambient occlusion into single consolidated rectangular quads.
 *
 * WHAT:
 * 1. 3-axis greedy quad coalescing across dimensions d=0 (X), d=1 (Y), d=2 (Z).
 * 2. 4-corner Smooth Ambient Occlusion (AO) checking 3 adjacent neighbor voxels per corner.
 * 3. Anisotropic quad diagonal flipping to eliminate lighting interpolation artifacts.
 * 4. 10-float vertex packing: Position (X,Y,Z), UV (U,V), Texture Layer, and LightData
 *    (SkyLight, BlockLight, AO, DirectionalShade).
 *
 * USED FOR:
 * Invoked by background worker threads and synchronous edits when generating section meshes.
 *
 * WHERE NEXT:
 * The populated vertex and index buffers are bound to OpenGL VAO/VBO/EBO handles by SectionMesh.
 */
public class GreedyMesher {

    /**
     * Helper mask record storing face identity, lighting, and ambient occlusion
     * to ensure quads only merge when visual properties match identically.
     */
    public static class FaceData {
        public int blockId;
        public float layer;
        public float directionalLight;
        public int skyLight;
        public int blockLight;
        public int ao0, ao1, ao2, ao3;
        public boolean backFace;

        public void set(int blockId, float layer, float dirLight, int skyLight, int blockLight,
                        int ao0, int ao1, int ao2, int ao3, boolean backFace) {
            this.blockId = blockId;
            this.layer = layer;
            this.directionalLight = dirLight;
            this.skyLight = skyLight;
            this.blockLight = blockLight;
            this.ao0 = ao0;
            this.ao1 = ao1;
            this.ao2 = ao2;
            this.ao3 = ao3;
            this.backFace = backFace;
        }

        public void clear() {
            this.blockId = 0;
            this.layer = 0;
            this.directionalLight = 0;
            this.skyLight = 0;
            this.blockLight = 0;
            this.ao0 = 3;
            this.ao1 = 3;
            this.ao2 = 3;
            this.ao3 = 3;
            this.backFace = false;
        }

        public boolean equalsFace(FaceData other) {
            if (other == null) return false;
            return this.blockId == other.blockId &&
                   this.layer == other.layer &&
                   this.skyLight == other.skyLight &&
                   this.blockLight == other.blockLight &&
                   this.ao0 == other.ao0 &&
                   this.ao1 == other.ao1 &&
                   this.ao2 == other.ao2 &&
                   this.ao3 == other.ao3 &&
                   Math.abs(this.directionalLight - other.directionalLight) < 0.01f &&
                   this.backFace == other.backFace;
        }
    }

    /**
     * Backwards-compatible single-section greedy mesher (uses default ambient starlight).
     */
    public static int meshSection(ChunkSection section, FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
        return meshSection(section, null, null, vertexBuffer, indexBuffer);
    }

    /**
     * WHY:
     * Executes the complete 3-axis greedy meshing pass over a 16³ chunk section with
     * smooth vertex ambient occlusion and cellular lighting.
     *
     * @param section      The target 16³ chunk section.
     * @param column       Parent chunk column for intra-column queries (Y=0..319).
     * @param world        Global lighting access for neighbor boundary queries.
     * @param vertexBuffer Destination native FloatBuffer (10 floats per vertex).
     * @param indexBuffer  Destination native IntBuffer (Triangle indices).
     * @return Total number of consolidated quads produced.
     */
    public static int meshSection(ChunkSection section, ChunkColumn column, WorldLightingAccess world,
                                  FloatBuffer vertexBuffer, IntBuffer indexBuffer) {
        if (section == null || section.isEmpty()) {
            return 0; // Skip empty air sections
        }

        int quadCount = 0;

        // Allocate reusable 2D mask (16x16)
        FaceData[] mask = new FaceData[16 * 16];
        for (int i = 0; i < mask.length; i++) {
            mask[i] = new FaceData();
        }

        // Sweep all three axes: d=0 (X), d=1 (Y), d=2 (Z)
        for (int d = 0; d < 3; d++) {
            int u = (d + 1) % 3;
            int v = (d + 2) % 3;

            int[] x = new int[3];
            int[] q = new int[3];
            q[d] = 1;

            // Unit vectors along u and v axes
            int[] duVec = new int[3]; duVec[u] = 1;
            int[] dvVec = new int[3]; dvVec[v] = 1;

            for (x[d] = -1; x[d] < 16; x[d]++) {
                int maskIndex = 0;

                // 1. Build the 16x16 face mask for current slice
                for (x[v] = 0; x[v] < 16; x[v]++) {
                    for (x[u] = 0; x[u] < 16; x[u]++) {
                        int bCurrent = queryBlock(section, column, world, x[0], x[1], x[2]);
                        int bNext = queryBlock(section, column, world, x[0] + q[0], x[1] + q[1], x[2] + q[2]);

                        boolean curOpaque = BlockIds.isOpaque(bCurrent);
                        boolean nextOpaque = BlockIds.isOpaque(bNext);

                        if (curOpaque == nextOpaque) {
                            mask[maskIndex++].clear();
                        } else if (curOpaque) {
                            // Front face: current voxel extends into adjacent air
                            float layer = mapBlockToLayer(bCurrent);
                            float dirLight = computeDirectionalLight(d, false);
                            int airX = x[0] + q[0];
                            int airY = x[1] + q[1];
                            int airZ = x[2] + q[2];
                            int sky = querySkyLight(section, column, world, airX, airY, airZ);
                            int block = queryBlockLight(section, column, world, airX, airY, airZ);

                            // Calculate 4 corner ambient occlusion values
                            int ao0 = computeCornerAO(section, column, world, airX, airY, airZ, -duVec[0], -duVec[1], -duVec[2], -dvVec[0], -dvVec[1], -dvVec[2]);
                            int ao1 = computeCornerAO(section, column, world, airX, airY, airZ,  duVec[0],  duVec[1],  duVec[2], -dvVec[0], -dvVec[1], -dvVec[2]);
                            int ao2 = computeCornerAO(section, column, world, airX, airY, airZ,  duVec[0],  duVec[1],  duVec[2],  dvVec[0],  dvVec[1],  dvVec[2]);
                            int ao3 = computeCornerAO(section, column, world, airX, airY, airZ, -duVec[0], -duVec[1], -duVec[2],  dvVec[0],  dvVec[1],  dvVec[2]);

                            mask[maskIndex++].set(bCurrent, layer, dirLight, sky, block, ao0, ao1, ao2, ao3, false);
                        } else {
                            // Back face: adjacent voxel extends into current air
                            float layer = mapBlockToLayer(bNext);
                            float dirLight = computeDirectionalLight(d, true);
                            int airX = x[0];
                            int airY = x[1];
                            int airZ = x[2];
                            int sky = querySkyLight(section, column, world, airX, airY, airZ);
                            int block = queryBlockLight(section, column, world, airX, airY, airZ);

                            // Calculate 4 corner ambient occlusion values
                            int ao0 = computeCornerAO(section, column, world, airX, airY, airZ, -duVec[0], -duVec[1], -duVec[2], -dvVec[0], -dvVec[1], -dvVec[2]);
                            int ao1 = computeCornerAO(section, column, world, airX, airY, airZ,  duVec[0],  duVec[1],  duVec[2], -dvVec[0], -dvVec[1], -dvVec[2]);
                            int ao2 = computeCornerAO(section, column, world, airX, airY, airZ,  duVec[0],  duVec[1],  duVec[2],  dvVec[0],  dvVec[1],  dvVec[2]);
                            int ao3 = computeCornerAO(section, column, world, airX, airY, airZ, -duVec[0], -duVec[1], -duVec[2],  dvVec[0],  dvVec[1],  dvVec[2]);

                            mask[maskIndex++].set(bNext, layer, dirLight, sky, block, ao0, ao1, ao2, ao3, true);
                        }
                    }
                }

                // 2. Greedily coalesce matching faces
                maskIndex = 0;
                for (int j = 0; j < 16; j++) {
                    for (int i = 0; i < 16;) {
                        FaceData currentFace = mask[maskIndex];

                        if (currentFace.blockId > 0) {
                            int w;
                            for (w = 1; i + w < 16 && mask[maskIndex + w].equalsFace(currentFace); w++) {}

                            int h;
                            boolean canExpand = true;
                            for (h = 1; j + h < 16; h++) {
                                for (int k = 0; k < w; k++) {
                                    FaceData check = mask[maskIndex + k + h * 16];
                                    if (!check.equalsFace(currentFace)) {
                                        canExpand = false;
                                        break;
                                    }
                                }
                                if (!canExpand) break;
                            }

                            x[u] = i;
                            x[v] = j;

                            int[] du = new int[3]; du[u] = w;
                            int[] dv = new int[3]; dv[v] = h;

                            emitGreedyQuad(vertexBuffer, indexBuffer, quadCount++,
                                    x, du, dv, d, currentFace, w, h);

                            for (int dy = 0; dy < h; dy++) {
                                for (int dx = 0; dx < w; dx++) {
                                    mask[maskIndex + dx + dy * 16].clear();
                                }
                            }

                            i += w;
                            maskIndex += w;
                        } else {
                            i++;
                            maskIndex++;
                        }
                    }
                }
            }
        }

        return quadCount;
    }

    /**
     * Computes Ambient Occlusion level (0 to 3) for a quad corner.
     */
    public static int computeCornerAO(ChunkSection section, ChunkColumn column, WorldLightingAccess world,
                                      int ax, int ay, int az,
                                      int duX, int duY, int duZ,
                                      int dvX, int dvY, int dvZ) {
        int b1 = queryBlock(section, column, world, ax + duX, ay + duY, az + duZ);
        int b2 = queryBlock(section, column, world, ax + dvX, ay + dvY, az + dvZ);
        int bc = queryBlock(section, column, world, ax + duX + dvX, ay + duY + dvY, az + duZ + dvZ);

        boolean s1 = BlockIds.isOpaque(b1);
        boolean s2 = BlockIds.isOpaque(b2);
        boolean sc = BlockIds.isOpaque(bc);

        if (s1 && s2) {
            return 0; // Fully occluded concave corner
        }
        return 3 - (s1 ? 1 : 0) - (s2 ? 1 : 0) - (sc ? 1 : 0);
    }

    /**
     * Appends 4 vertices (10 floats each) and 6 indices to native buffers.
     */
    private static void emitGreedyQuad(FloatBuffer vBuf, IntBuffer iBuf, int quadIndex,
                                       int[] x, int[] du, int[] dv, int d,
                                       FaceData face, int w, int h) {
        float x0, y0, z0;
        float x1, y1, z1;
        float x2, y2, z2;
        float x3, y3, z3;

        int dCoord = x[d] + (face.backFace ? 1 : 1);

        if (d == 0) { // X-axis (u=Y, v=Z)
            x0 = dCoord; y0 = x[1];         z0 = x[2];
            x1 = dCoord; y1 = x[1] + du[1]; z1 = x[2];
            x2 = dCoord; y2 = x[1] + du[1]; z2 = x[2] + dv[2];
            x3 = dCoord; y3 = x[1];         z3 = x[2] + dv[2];
        } else if (d == 1) { // Y-axis (u=Z, v=X)
            x0 = x[0];         y0 = dCoord; z0 = x[2];
            x1 = x[0];         y1 = dCoord; z1 = x[2] + du[2];
            x2 = x[0] + dv[0]; y2 = dCoord; z2 = x[2] + du[2];
            x3 = x[0] + dv[0]; y3 = dCoord; z3 = x[2];
        } else { // Z-axis (u=X, v=Y)
            x0 = x[0];         y0 = x[1];         z0 = dCoord;
            x1 = x[0] + du[0]; y1 = x[1];         z1 = dCoord;
            x2 = x[0] + du[0]; y2 = x[1] + dv[1]; z2 = dCoord;
            x3 = x[0];         y3 = x[1] + dv[1]; z3 = dCoord;
        }

        float layer = face.layer;
        float sky = (float) face.skyLight;
        float block = (float) face.blockLight;
        float dir = face.directionalLight;

        int ao0 = face.ao0;
        int ao1 = face.ao1;
        int ao2 = face.ao2;
        int ao3 = face.ao3;

        int base = quadIndex * 4;

        if (!face.backFace) {
            // Front face vertices (CCW)
            vBuf.put(x0).put(y0).put(z0).put(0.0f).put((float) h).put(layer).put(sky).put(block).put((float) ao0).put(dir);
            vBuf.put(x1).put(y1).put(z1).put((float) w).put((float) h).put(layer).put(sky).put(block).put((float) ao1).put(dir);
            vBuf.put(x2).put(y2).put(z2).put((float) w).put(0.0f).put(layer).put(sky).put(block).put((float) ao2).put(dir);
            vBuf.put(x3).put(y3).put(z3).put(0.0f).put(0.0f).put(layer).put(sky).put(block).put((float) ao3).put(dir);

            // Anisotropic diagonal flip
            if (ao0 + ao2 < ao1 + ao3) {
                iBuf.put(base).put(base + 1).put(base + 3);
                iBuf.put(base + 1).put(base + 2).put(base + 3);
            } else {
                iBuf.put(base).put(base + 1).put(base + 2);
                iBuf.put(base + 2).put(base + 3).put(base);
            }
        } else {
            // Back face vertices (CW reversed)
            vBuf.put(x3).put(y3).put(z3).put(0.0f).put(0.0f).put(layer).put(sky).put(block).put((float) ao3).put(dir);
            vBuf.put(x2).put(y2).put(z2).put((float) w).put(0.0f).put(layer).put(sky).put(block).put((float) ao2).put(dir);
            vBuf.put(x1).put(y1).put(z1).put((float) w).put((float) h).put(layer).put(sky).put(block).put((float) ao1).put(dir);
            vBuf.put(x0).put(y0).put(z0).put(0.0f).put((float) h).put(layer).put(sky).put(block).put((float) ao0).put(dir);

            // Anisotropic diagonal flip
            if (ao3 + ao1 < ao2 + ao0) {
                iBuf.put(base).put(base + 1).put(base + 3);
                iBuf.put(base + 1).put(base + 2).put(base + 3);
            } else {
                iBuf.put(base).put(base + 1).put(base + 2);
                iBuf.put(base + 2).put(base + 3).put(base);
            }
        }
    }

    private static int queryBlock(ChunkSection section, ChunkColumn column, WorldLightingAccess world,
                                  int lx, int ly, int lz) {
        if (lx >= 0 && lx < 16 && ly >= 0 && ly < 16 && lz >= 0 && lz < 16) {
            return section.getBlock(lx, ly, lz);
        }
        if (column != null) {
            int wy = (section.getSectionY() << 4) + ly;
            if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16 && wy >= 0 && wy < ChunkColumn.CHUNK_HEIGHT) {
                return column.getBlock(lx, wy, lz);
            }
            if (world != null) {
                int wx = (column.getChunkX() << 4) + lx;
                int wz = (column.getChunkZ() << 4) + lz;
                return world.getBlock(wx, wy, wz);
            }
        }
        return 0;
    }

    private static int querySkyLight(ChunkSection section, ChunkColumn column, WorldLightingAccess world,
                                     int lx, int ly, int lz) {
        if (lx >= 0 && lx < 16 && ly >= 0 && ly < 16 && lz >= 0 && lz < 16) {
            return section.getSkyLight(lx, ly, lz);
        }
        if (column != null) {
            int wy = (section.getSectionY() << 4) + ly;
            if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                return column.getSkyLight(lx, wy, lz);
            }
            if (world != null) {
                int wx = (column.getChunkX() << 4) + lx;
                int wz = (column.getChunkZ() << 4) + lz;
                return world.getSkyLight(wx, wy, wz);
            }
        }
        return 15;
    }

    private static int queryBlockLight(ChunkSection section, ChunkColumn column, WorldLightingAccess world,
                                       int lx, int ly, int lz) {
        if (lx >= 0 && lx < 16 && ly >= 0 && ly < 16 && lz >= 0 && lz < 16) {
            return section.getBlockLight(lx, ly, lz);
        }
        if (column != null) {
            int wy = (section.getSectionY() << 4) + ly;
            if (lx >= 0 && lx < 16 && lz >= 0 && lz < 16) {
                return column.getBlockLight(lx, wy, lz);
            }
            if (world != null) {
                int wx = (column.getChunkX() << 4) + lx;
                int wz = (column.getChunkZ() << 4) + lz;
                return world.getBlockLight(wx, wy, wz);
            }
        }
        return 0;
    }

    private static float computeDirectionalLight(int axis, boolean backFace) {
        if (axis == 1) { // Y axis
            return backFace ? 0.5f : 1.0f; // Top=1.0, Bottom=0.5
        } else if (axis == 2) { // Z axis
            return 0.8f; // North / South
        } else { // X axis
            return 0.65f; // East / West
        }
    }

    private static float mapBlockToLayer(int blockId) {
        return switch (blockId) {
            case BlockIds.STONE -> TextureArray.LAYER_STONE;
            case BlockIds.GRANITE -> TextureArray.LAYER_GRANITE;
            case BlockIds.RED_SANDSTONE -> TextureArray.LAYER_RED_SANDSTONE;
            case BlockIds.ALLUVIAL_SOIL -> TextureArray.LAYER_ALLUVIAL_SOIL;
            case BlockIds.YELLOW_SANDSTONE -> TextureArray.LAYER_YELLOW_SANDSTONE;
            case BlockIds.DECCAN_BASALT -> TextureArray.LAYER_DECCAN_BASALT;
            case BlockIds.FIRED_BRICK -> TextureArray.LAYER_FIRED_BRICK;
            case BlockIds.DIYA -> TextureArray.LAYER_DIYA;
            case BlockIds.SAL_LOG -> TextureArray.LAYER_SAL_LOG_SIDE;
            case BlockIds.SAL_PLANKS -> TextureArray.LAYER_SAL_PLANKS;
            case BlockIds.WATER -> TextureArray.LAYER_WATER;
            case BlockIds.THAR_SAND -> TextureArray.LAYER_THAR_SAND;
            case BlockIds.HIMALAYAN_SNOW -> TextureArray.LAYER_HIMALAYAN_SNOW;
            case BlockIds.SAL_LEAVES -> TextureArray.LAYER_SAL_LEAVES;
            case BlockIds.DEODAR_LEAVES -> TextureArray.LAYER_DEODAR_LEAVES;
            case BlockIds.KHEJRI_LEAVES -> TextureArray.LAYER_KHEJRI_LEAVES;
            case BlockIds.KOYLA_ORE -> TextureArray.LAYER_KOYLA_ORE;
            case BlockIds.TAAMBA_ORE -> TextureArray.LAYER_TAAMBA_ORE;
            case BlockIds.LOHA_ORE -> TextureArray.LAYER_LOHA_ORE;
            case BlockIds.RAJAVARTA_ORE -> TextureArray.LAYER_RAJAVARTA_ORE;
            case BlockIds.HIRA_ORE -> TextureArray.LAYER_HIRA_ORE;
            case BlockIds.LAVA -> TextureArray.LAYER_LAVA;
            case BlockIds.BAJRI -> TextureArray.LAYER_BAJRI;
            case BlockIds.WATER_FLOWING -> TextureArray.LAYER_WATER_FLOWING;
            case BlockIds.DEODAR_LOG -> TextureArray.LAYER_DEODAR_LOG;
            case BlockIds.KHEJRI_LOG -> TextureArray.LAYER_KHEJRI_LOG;
            case BlockIds.SONA_ORE -> TextureArray.LAYER_SONA_ORE;
            case BlockIds.KARYASHALA -> TextureArray.LAYER_KARYASHALA;
            case BlockIds.PAKKI_BHATTI -> TextureArray.LAYER_PAKKI_BHATTI;
            case BlockIds.SMELTING_BHATTI -> TextureArray.LAYER_SMELTING_BHATTI;
            case BlockIds.CHAAK -> TextureArray.LAYER_CHAAK;
            case BlockIds.KARGAH -> TextureArray.LAYER_KARGAH;
            case BlockIds.CHAKKI -> TextureArray.LAYER_CHAKKI;
            case BlockIds.NIHAI -> TextureArray.LAYER_NIHAI;
            case BlockIds.KAANCH -> TextureArray.LAYER_KAANCH;
            case BlockIds.REGUR_SOIL -> TextureArray.LAYER_REGUR_SOIL;
            case BlockIds.TEAK_LOG -> TextureArray.LAYER_TEAK_LOG;
            case BlockIds.TEAK_LEAVES -> TextureArray.LAYER_TEAK_LEAVES;
            case BlockIds.NAMAK_CRUST -> TextureArray.LAYER_NAMAK_CRUST;
            case BlockIds.MANGROVE_LOG -> TextureArray.LAYER_MANGROVE_LOG;
            case BlockIds.MANGROVE_LEAVES -> TextureArray.LAYER_MANGROVE_LEAVES;
            case BlockIds.BAANS -> TextureArray.LAYER_BAANS;
            case BlockIds.SAMUDRA_SAND -> TextureArray.LAYER_SAMUDRA_SAND;
            case BlockIds.CARVED_STONE -> TextureArray.LAYER_CARVED_STONE;
            case BlockIds.ANCIENT_CHEST -> TextureArray.LAYER_ANCIENT_CHEST;
            case BlockIds.ASHOKA_CHAKRA -> TextureArray.LAYER_ASHOKA_CHAKRA;
            case BlockIds.SHILP_MANDAP -> TextureArray.LAYER_SHILP_MANDAP;
            case BlockIds.TILLED_SOIL -> TextureArray.LAYER_TILLED_SOIL;
            case BlockIds.DHAN_CROP -> TextureArray.LAYER_DHAN_CROP;
            case BlockIds.GANNA_CROP -> TextureArray.LAYER_GANNA_CROP;
            case BlockIds.KAPAAS_CROP -> TextureArray.LAYER_KAPAAS_CROP;
            case BlockIds.PAATAAL_AGNI_ROCK -> TextureArray.LAYER_PAATAAL_AGNI_ROCK;
            case BlockIds.PAATAAL_LAVA_GEL -> TextureArray.LAYER_PAATAAL_LAVA_GEL;
            case BlockIds.AKASH_MARBLE -> TextureArray.LAYER_AKASH_MARBLE;
            case BlockIds.AKASH_CRYSTAL -> TextureArray.LAYER_AKASH_CRYSTAL;
            case BlockIds.DIMENSIONAL_PORTAL -> TextureArray.LAYER_DIMENSIONAL_PORTAL;
            default -> TextureArray.LAYER_STONE;
        };
    }
}
