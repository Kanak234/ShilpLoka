package shilploka.world.dimension;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shilploka.entity.Player;
import shilploka.world.BlockIds;
import shilploka.world.chunk.ChunkColumn;
import shilploka.world.chunk.ChunkManager;
import shilploka.worldgen.dimension.AkashDweepGenerator;
import shilploka.worldgen.dimension.PaataalGenerator;

/**
 * WHY:
 * Governs multi-dimensional travel across Bharatiya realms (Prithvi, Paataal, Akash Dweep).
 * Coordinates chunk generation delegates and manages player dimensional teleportation.
 *
 * WHAT:
 * Tracks active DimensionType, manages portal gateway triggers, coordinate translations,
 * and delegates chunk generation according to the active realm.
 */
public class DimensionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DimensionManager.class);

    private final long seed;
    private DimensionType currentDimension = DimensionType.PRITHVI;

    private final PaataalGenerator paataalGen;
    private final AkashDweepGenerator akashDweepGen;

    // Portal teleportation cooldown (prevent rapid toggling)
    private float portalCooldown = 0.0f;

    public DimensionManager(long seed) {
        this.seed = seed;
        this.paataalGen = new PaataalGenerator(seed);
        this.akashDweepGen = new AkashDweepGenerator(seed);
    }

    public DimensionType getCurrentDimension() {
        return currentDimension;
    }

    public void setCurrentDimension(DimensionType dimension) {
        if (dimension != null && dimension != this.currentDimension) {
            LOGGER.info("Transitioning realm from [{}] to [{}]", this.currentDimension.getEnglishName(), dimension.getEnglishName());
            this.currentDimension = dimension;
        }
    }

    public PaataalGenerator getPaataalGenerator() {
        return paataalGen;
    }

    public AkashDweepGenerator getAkashDweepGenerator() {
        return akashDweepGen;
    }

    /**
     * Updates cooldown timers.
     */
    public void tick(float dt) {
        if (portalCooldown > 0.0f) {
            portalCooldown = Math.max(0.0f, portalCooldown - dt);
        }
    }

    /**
     * Checks if the player is currently stepping inside a Dimensional Portal block.
     * If detected and cooldown expired, advances to the destination realm.
     *
     * @return true if teleportation occurred
     */
    public boolean checkPortalCollision(Player player, ChunkManager chunkManager) {
        if (player == null || chunkManager == null || portalCooldown > 0.0f) {
            return false;
        }

        var box = player.getBoundingBox();
        int minX = (int) Math.floor(box.minX());
        int maxX = (int) Math.floor(box.maxX());
        int minY = (int) Math.floor(box.minY());
        int maxY = (int) Math.floor(box.maxY());
        int minZ = (int) Math.floor(box.minZ());
        int maxZ = (int) Math.floor(box.maxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (chunkManager.getBlock(x, y, z) == BlockIds.DIMENSIONAL_PORTAL) {
                        // Portal entered! Compute destination realm
                        DimensionType nextRealm = switch (currentDimension) {
                            case PRITHVI -> DimensionType.PAATAAL;
                            case PAATAAL -> DimensionType.AKASH_DWEEP;
                            case AKASH_DWEEP -> DimensionType.PRITHVI;
                        };
                        teleportPlayer(player, nextRealm);
                        portalCooldown = 3.0f; // 3 second cooldown
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Safely teleports the player into the destination dimension at an authentic arrival shrine.
     */
    public void teleportPlayer(Player player, DimensionType targetRealm) {
        setCurrentDimension(targetRealm);
        switch (targetRealm) {
            case PRITHVI -> {
                player.setPosition(0.5, 70.0, 0.5);
                player.getVelocity().set(0, 0, 0);
            }
            case PAATAAL -> {
                // Altar platform in Paataal
                player.setPosition(7.5, 43.0, 7.5);
                player.getVelocity().set(0, 0, 0);
            }
            case AKASH_DWEEP -> {
                // Celestial shrine in Akash Dweep
                player.setPosition(7.5, 98.0, 7.5);
                player.getVelocity().set(0, 0, 0);
            }
        }
        LOGGER.info("Player transported to {} at {}", targetRealm.getEnglishName(), player.getPosition());
    }

    /**
     * Spawns the Andhakaar Naag boss in the Paataal arena if not already present.
     */
    public void spawnBossInPaataal(shilploka.entity.EntityManager entityManager, ChunkManager chunkManager, Player player) {
        if (entityManager == null || chunkManager == null || player == null) return;
        boolean bossExists = entityManager.getEntities().stream()
                .anyMatch(e -> e instanceof shilploka.entity.living.boss.AndhakaarNaagEntity && !e.isDead());
        if (!bossExists) {
            shilploka.entity.living.boss.AndhakaarNaagEntity boss =
                    new shilploka.entity.living.boss.AndhakaarNaagEntity(7.5, 43.0, 15.0, chunkManager, player);
            entityManager.addEntity(boss);
            LOGGER.info("Summoned Paataal guardian boss: Andhakaar Naag at (7.5, 43.0, 15.0)");
        }
    }

    /**
     * Populates a ChunkColumn tailored to the active realm.
     */
    public void populateChunk(ChunkColumn col, shilploka.world.WorldGenerator overworldGen) {
        if (col == null) return;
        switch (currentDimension) {
            case PAATAAL -> paataalGen.generateColumn(col);
            case AKASH_DWEEP -> akashDweepGen.generateColumn(col);
            default -> {}
        }
    }
}
