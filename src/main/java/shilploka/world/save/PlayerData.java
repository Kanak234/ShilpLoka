package shilploka.world.save;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * WHY:
 * Persists player coordinates, orientation, flight state, selected hotbar slot,
 * survival vitals, and inventory to `saves/<world_folder>/playerdata/player.json`.
 *
 * WHAT:
 * Jackson-serializable POJO conforming to ShilpLoka DATA_MODELS specification §7.2.
 * Includes position [x, y, z], rotation [yaw, pitch], dimension, survival stats,
 * and inventory item records.
 *
 * USED FOR:
 * Loaded during Game.init() to position the player and restored during save cycles.
 *
 * WHERE NEXT:
 * Serialized atomically to disk by SaveManager.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerData {

    @JsonProperty("uuid")
    private String uuid = UUID.randomUUID().toString();

    @JsonProperty("position")
    private double[] position = new double[]{8.0, 68.0, 8.0};

    @JsonProperty("rotation")
    private float[] rotation = new float[]{-90.0f, 0.0f};

    @JsonProperty("dimension")
    private String dimension = "shilploka:bhoolok";

    @JsonProperty("flying")
    private boolean flying = true;

    @JsonProperty("selectedHotbarSlot")
    private int selectedHotbarSlot = 0;

    @JsonProperty("survivalStats")
    private SurvivalStats survivalStats = new SurvivalStats();

    @JsonProperty("inventory")
    private List<InventoryItem> inventory = new ArrayList<>();

    public PlayerData() {}

    public PlayerData(double x, double y, double z, float yaw, float pitch, boolean flying, int hotbarSlot) {
        this.position = new double[]{x, y, z};
        this.rotation = new float[]{yaw, pitch};
        this.flying = flying;
        this.selectedHotbarSlot = hotbarSlot;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SurvivalStats {
        @JsonProperty("health")
        public float health = 20.0f; // 10 Kamal (Lotus) icons

        @JsonProperty("bhookh")
        public int bhookh = 20; // 10 Roti icons

        @JsonProperty("saturation")
        public float saturation = 5.0f;

        @JsonProperty("exhaustion")
        public float exhaustion = 0.0f;

        @JsonProperty("airBubbles")
        public int airBubbles = 300;

        @JsonProperty("anubhavLevel")
        public int anubhavLevel = 0;

        @JsonProperty("anubhavPoints")
        public int anubhavPoints = 0;

        public SurvivalStats() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class InventoryItem {
        @JsonProperty("slot")
        public int slot;

        @JsonProperty("item")
        public String item;

        @JsonProperty("count")
        public int count = 1;

        @JsonProperty("damage")
        public int damage = 0;

        public InventoryItem() {}

        public InventoryItem(int slot, String item, int count, int damage) {
            this.slot = slot;
            this.item = item;
            this.count = count;
            this.damage = damage;
        }
    }

    // Getters and Setters
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public double[] getPosition() { return position; }
    public void setPosition(double[] position) { this.position = position; }

    public float[] getRotation() { return rotation; }
    public void setRotation(float[] rotation) { this.rotation = rotation; }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }

    public boolean isFlying() { return flying; }
    public void setFlying(boolean flying) { this.flying = flying; }

    public int getSelectedHotbarSlot() { return selectedHotbarSlot; }
    public void setSelectedHotbarSlot(int selectedHotbarSlot) { this.selectedHotbarSlot = selectedHotbarSlot; }

    public SurvivalStats getSurvivalStats() { return survivalStats; }
    public void setSurvivalStats(SurvivalStats survivalStats) { this.survivalStats = survivalStats; }

    public List<InventoryItem> getInventory() { return inventory; }
    public void setInventory(List<InventoryItem> inventory) { this.inventory = inventory; }
}
