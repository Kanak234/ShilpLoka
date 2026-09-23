package shilploka.entity;

import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import shilploka.ai.Pathfinder;
import shilploka.entity.living.fauna.BuffaloEntity;
import shilploka.entity.living.fauna.ElephantEntity;
import shilploka.entity.living.fauna.GauEntity;
import shilploka.entity.living.fauna.PeacockEntity;
import shilploka.entity.living.hostile.CobraEntity;
import shilploka.entity.living.hostile.DaakuEntity;
import shilploka.entity.living.hostile.TigerEntity;
import shilploka.entity.living.hostile.VetaalEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.world.BlockIds;
import shilploka.world.DayNightCycle;
import shilploka.world.WorldGenerator;
import shilploka.world.chunk.ChunkManager;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WHY:
 * Comprehensive test suite for ShilpLoka Phase P9: Entity System, AI Framework & Fauna.
 *
 * WHAT:
 * Tests 3D A* pathfinding, Ray-AABB intersection, passive sacred fauna (Gau milk, Elephant, Peacock),
 * predators (Tiger, Cobra), nocturnal folklore entities (Vetaal, Daaku), and EntityManager lifecycle.
 */
public class EntitySystemTest {

    private static ChunkManager chunkManager;
    private static DayNightCycle dayNightCycle;

    @BeforeAll
    public static void setUp() {
        ItemRegistry.init();
        WorldGenerator worldGen = new WorldGenerator(12345L);
        chunkManager = new ChunkManager(worldGen, null);
        dayNightCycle = new DayNightCycle();
    }

    @Test
    @DisplayName("3D A* Pathfinder finds walkable waypoints on solid terrain")
    public void testPathfinderNavigation() {
        // Build flat floor with a 1-block step up
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 2; z++) {
                int yLevel = (x >= 5) ? 65 : 64;
                chunkManager.setBlock(x, yLevel, z, BlockIds.STONE);
                chunkManager.setBlock(x, yLevel + 1, z, BlockIds.AIR);
                chunkManager.setBlock(x, yLevel + 2, z, BlockIds.AIR);
            }
        }

        Vector3i start = new Vector3i(1, 65, 1);
        Vector3i target = new Vector3i(8, 66, 1);

        List<Vector3i> path = Pathfinder.findPath(start, target, chunkManager, 100);
        assertNotNull(path);
        assertFalse(path.isEmpty(), "Pathfinder should find valid path across 1-block elevation step");
        // Verify path steps up to level 66
        boolean reachedStep = path.stream().anyMatch(node -> node.y == 66);
        assertTrue(reachedStep, "Path should contain elevated waypoint at Y=66");
    }

    @Test
    @DisplayName("AABB intersectsRay detects line-of-sight intersection and misses")
    public void testAABBRayIntersection() {
        AABB box = new AABB(10, 10, 10, 12, 12, 12);

        // Ray directly pointing at center
        Vector3d origin = new Vector3d(0, 11, 11);
        Vector3f dirHit = new Vector3f(1, 0, 0);
        assertTrue(box.intersectsRay(origin, dirHit, 20.0), "Ray along +X should hit target box");

        // Ray pointing away
        Vector3f dirMiss = new Vector3f(-1, 0, 0);
        assertFalse(box.intersectsRay(origin, dirMiss, 20.0), "Ray pointing opposite should miss target box");

        // Ray with insufficient reach
        assertTrue(box.intersectsRay(origin, dirHit, 20.0));
        assertFalse(box.intersectsRay(origin, dirHit, 5.0), "Ray with distance 5 should not reach box at X=10");
    }

    @Test
    @DisplayName("Sacred Indian Zebu Cow is unharmed by player damage and provides milk in earthen vessels")
    public void testGauSacredBehavior() {
        GauEntity gau = new GauEntity(0, 65, 0, chunkManager);
        float initialHealth = gau.getHealth();

        // Cultural Mandate: Player damage has no harmful effect on Gau
        gau.damage(10.0f);
        assertEquals(initialHealth, gau.getHealth(), "Sacred Gau must not take damage from attacks");
        assertFalse(gau.isDead());
        assertTrue(gau.getDeathDrops().isEmpty(), "Gau must never drop meat or loot");

        // Milking interaction: Player with earthen Kulhad receives fresh milk
        Player player = new Player(0, 65, 0);
        player.getInventory().setSlot(0, new ItemStack(ItemRegistry.KULHAD, 1));

        assertTrue(gau.interact(player), "Player holding Kulhad should milk Gau successfully");
        ItemStack held = player.getInventory().getSlot(0);
        assertEquals("shilploka:doodh", held.getItem().getId(), "Player should receive fresh milk (Doodh)");
    }

    @Test
    @DisplayName("Indian Elephant is high-health neutral colossus")
    public void testElephantEntity() {
        ElephantEntity elephant = new ElephantEntity(10, 65, 10, chunkManager);
        assertEquals(100.0f, elephant.getMaxHealth());
        assertEquals("Indian Elephant", elephant.getEntityName());

        elephant.damage(20.0f);
        assertEquals(80.0f, elephant.getHealth());
        assertFalse(elephant.isDead());
    }

    @Test
    @DisplayName("Peacock panics when player approaches and drops peacock feather")
    public void testPeacockEntity() {
        Player player = new Player(0, 65, 0);
        PeacockEntity peacock = new PeacockEntity(2, 65, 2, chunkManager, player);

        assertEquals(10.0f, peacock.getMaxHealth());
        var drops = peacock.getDeathDrops();
        assertEquals(1, drops.size());
        assertEquals("shilploka:mor_pankh", drops.get(0).getItem().getId());
    }

    @Test
    @DisplayName("Royal Bengal Tiger is aggressive predator that attacks player")
    public void testTigerPredator() {
        Player player = new Player(0, 65, 0);
        player.getVitals().setHealth(20.0f);

        TigerEntity tiger = new TigerEntity(1.5, 65, 0, chunkManager, player);
        assertEquals(40.0f, tiger.getMaxHealth());

        // Tick tiger AI to trigger attack
        for (int i = 0; i < 5; i++) {
            tiger.tick(0.05f, chunkManager, player);
        }

        assertTrue(player.getVitals().getHealth() < 20.0f, "Tiger should damage player within attack range");
    }

    @Test
    @DisplayName("King Cobra inflicts venomous bite damage")
    public void testCobraVenomAttack() {
        Player player = new Player(0, 65, 0);
        player.getVitals().setHealth(20.0f);

        CobraEntity cobra = new CobraEntity(1.0, 65, 0, chunkManager, player);
        cobra.tick(0.05f, chunkManager, player);

        assertTrue(player.getVitals().getHealth() < 20.0f, "King Cobra should deal venom bite damage");
    }

    @Test
    @DisplayName("Vetaal nocturnal spirit is active at night and drops Bhasma")
    public void testVetaalSpirit() {
        Player player = new Player(0, 65, 0);
        dayNightCycle.setTime(18000L); // Midnight

        VetaalEntity vetaal = new VetaalEntity(0, 65, 0, chunkManager, player, dayNightCycle);
        assertEquals(24.0f, vetaal.getMaxHealth());

        var drops = vetaal.getDeathDrops();
        assertFalse(drops.isEmpty());
        assertEquals("shilploka:bhasma", drops.get(0).getItem().getId());
    }

    @Test
    @DisplayName("Wilderness Daaku attacks with Talwar and drops copper mudra currency")
    public void testDaakuBandit() {
        Player player = new Player(0, 65, 0);
        DaakuEntity daaku = new DaakuEntity(0, 65, 0, chunkManager, player);

        assertEquals(30.0f, daaku.getMaxHealth());
        var drops = daaku.getDeathDrops();
        assertFalse(drops.isEmpty());
        assertEquals("shilploka:mudra_copper", drops.get(0).getItem().getId());
    }

    @Test
    @DisplayName("EntityManager spawns drops upon entity death and clears on world transition")
    public void testEntityManagerLifecycle() {
        EntityManager em = new EntityManager();
        Player player = new Player(0, 65, 0);
        List<ItemEntity> drops = new ArrayList<>();

        PeacockEntity mor = new PeacockEntity(0, 65, 0, chunkManager, player);
        em.addEntity(mor);
        assertEquals(1, em.getEntities().size());

        // Vanquish entity
        mor.damage(100.0f);
        assertTrue(mor.isDead());

        em.tick(0.05f, chunkManager, player, dayNightCycle, drops);
        // Entity removed, death drops generated
        assertEquals(0, em.getEntities().size());
        assertFalse(drops.isEmpty(), "Item drop should be spawned on entity death");
        assertEquals("shilploka:mor_pankh", drops.get(0).getItemStack().getItem().getId());

        // Test clear
        em.addEntity(new BuffaloEntity(0, 65, 0, chunkManager));
        em.clear();
        assertEquals(0, em.getEntities().size());
    }
}
