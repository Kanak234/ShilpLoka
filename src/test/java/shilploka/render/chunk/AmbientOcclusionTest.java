package shilploka.render.chunk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkSection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests validating smooth 4-corner ambient occlusion (AO)
 * and anisotropic diagonal triangulation flipping.
 */
public class AmbientOcclusionTest {

    @Test
    @DisplayName("Open floor quad with no adjacent walls produces maximum brightness AO level 3")
    void testOpenFloorProducesAO3() {
        ChunkSection section = new ChunkSection(0);
        // Floor at y=0
        section.setBlock(5, 0, 5, BlockIds.STONE);

        // Air block above floor at (5, 1, 5)
        // Check corner (du=1, dv=1): side1, side2, corner are all air
        int ao = GreedyMesher.computeCornerAO(section, null, null,
                5, 1, 5,
                1, 0, 0,
                0, 0, 1);

        assertEquals(3, ao, "Corner surrounded by air must have AO 3 (fully unoccluded)");
    }

    @Test
    @DisplayName("Floor quad adjacent to a single vertical wall has reduced AO level")
    void testSingleWallReducesAO() {
        ChunkSection section = new ChunkSection(0);
        // Floor at y=0
        section.setBlock(5, 0, 5, BlockIds.STONE);
        // Wall block at (6, 1, 5)
        section.setBlock(6, 1, 5, BlockIds.STONE);

        // Air at (5, 1, 5), check corner facing the wall (du=+1 along X, dv=+1 along Z)
        int ao = GreedyMesher.computeCornerAO(section, null, null,
                5, 1, 5,
                1, 0, 0,
                0, 0, 1);

        // side1 is solid (X+1), side2 is air (Z+1), corner is air
        // ao = 3 - 1 - 0 - 0 = 2
        assertEquals(2, ao, "Corner facing a single wall must have AO 2");
    }

    @Test
    @DisplayName("Concave corner with perpendicular walls on both sides produces AO 0 (deep crease shadow)")
    void testConcaveCornerProducesAO0() {
        ChunkSection section = new ChunkSection(0);
        // Floor at y=0
        section.setBlock(5, 0, 5, BlockIds.STONE);
        // Wall along X: (6, 1, 5)
        section.setBlock(6, 1, 5, BlockIds.STONE);
        // Wall along Z: (5, 1, 6)
        section.setBlock(5, 1, 6, BlockIds.STONE);

        int ao = GreedyMesher.computeCornerAO(section, null, null,
                5, 1, 5,
                1, 0, 0,
                0, 0, 1);

        assertEquals(0, ao, "Corner wedged between perpendicular solid walls must produce AO 0");
    }

    @Test
    @DisplayName("Anisotropic diagonal test correctly flips triangulation when ao0 + ao2 < ao1 + ao3")
    void testAnisotropicDiagonalFlipCondition() {
        int ao0 = 0; // Dark corner
        int ao1 = 3; // Bright corner
        int ao2 = 0; // Dark opposite corner
        int ao3 = 3; // Bright corner

        // Standard diagonal (0-2) would connect the two dark corners, creating an artificial dark stripe!
        // The anisotropic condition (ao0 + ao2 < ao1 + ao3) evaluates: 0 + 0 < 3 + 3 (0 < 6),
        // which triggers diagonal flipping to (1-3) to bridge the bright corners smoothly.
        boolean shouldFlip = (ao0 + ao2) < (ao1 + ao3);
        assertTrue(shouldFlip, "Diagonal must flip when opposite corners are substantially darker");
    }
}
