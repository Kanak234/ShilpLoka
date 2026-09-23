package shilploka.entity.living.npc;

import shilploka.ai.PanicGoal;
import shilploka.ai.WanderGoal;
import shilploka.entity.AABB;
import shilploka.entity.living.LivingEntity;
import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;
import shilploka.trade.TradeOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * WHY:
 * Represents the peaceful inhabitants of ancient Indian villages (Gramwasi).
 * Each villager practices one of 9 authentic professions, possesses an era-accurate barter catalog,
 * follows daily work-panchayat schedules, and flees to safety during nocturnal Daaku raids.
 *
 * WHAT:
 * Extends LivingEntity with full swept-AABB collision, AI goals, and trade mechanics.
 *
 * USED FOR:
 * Spawned in Gaon villages and traded with via BarterScreen.
 */
public class GramwasiEntity extends LivingEntity {

    public enum ScheduleState {
        WORKING,
        GATHERING_AT_CHAUPAL,
        SLEEPING
    }

    private final VillagerProfession profession;
    private final List<TradeOffer> tradeOffers = new ArrayList<>();
    private ScheduleState scheduleState = ScheduleState.WORKING;
    private int villageCenterBlockX;
    private int villageCenterBlockZ;

    public GramwasiEntity(double x, double y, double z, VillagerProfession profession) {
        super(x, y, z, 0.6f, 1.8f, 20.0f);
        this.profession = profession;
        this.villageCenterBlockX = (int) x;
        this.villageCenterBlockZ = (int) z;

        initTradeOffers();
    }

    public void initGoals(shilploka.world.chunk.ChunkManager chunkManager, shilploka.entity.Player player) {
        if (chunkManager != null) {
            addGoal(new WanderGoal(this, chunkManager));
            if (player != null) {
                addGoal(new PanicGoal(this, player, chunkManager, 10.0f));
            }
        }
    }

    @Override
    public String getEntityName() {
        return "Gramwasi " + profession.name();
    }

    @Override
    public String getHindiName() {
        return profession.getDisplayName();
    }

    @Override
    public List<ItemStack> getDeathDrops() {
        List<ItemStack> drops = new ArrayList<>();
        Random rand = new Random();
        drops.add(new ItemStack(ItemRegistry.KAURI, 2 + rand.nextInt(4)));
        if (rand.nextBoolean()) {
            drops.add(new ItemStack(ItemRegistry.MUDRA_COPPER, 1 + rand.nextInt(2)));
        }
        return drops;
    }

    public VillagerProfession getProfession() {
        return profession;
    }

    public List<TradeOffer> getTradeOffers() {
        return tradeOffers;
    }

    public ScheduleState getScheduleState() {
        return scheduleState;
    }

    public void setScheduleState(ScheduleState state) {
        this.scheduleState = state;
    }

    public void setVillageCenter(int cx, int cz) {
        this.villageCenterBlockX = cx;
        this.villageCenterBlockZ = cz;
    }

    private void initTradeOffers() {
        switch (profession) {
            case KISAN -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.CHAWAL, 12), new ItemStack(ItemRegistry.MUDRA_COPPER, 2), 12));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 2), new ItemStack(ItemRegistry.ROTI, 4), 16));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 3), new ItemStack(ItemRegistry.AAM, 3), 10));
            }
            case KUMHAR -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MITTI_CLAY, 6), new ItemStack(ItemRegistry.MUDRA_COPPER, 2), 16));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 2), new ItemStack(ItemRegistry.KULHAD, 3), 12));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 4), new ItemStack(ItemRegistry.MATKA, 1), 8));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 5), new ItemStack(ItemRegistry.SURAHI, 1), 8));
            }
            case LOHAR -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.TAAMBA_INGOT, 4), new ItemStack(ItemRegistry.MUDRA_COPPER, 4), 10));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.LOHA_INGOT, 3), new ItemStack(ItemRegistry.MUDRA_COPPER, 6), 8));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 8), new ItemStack(ItemRegistry.BRONZE_KUDAAL, 1), 5));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 14), new ItemStack(ItemRegistry.LOHA_TALWAR, 1), 4));
            }
            case BUNKAR -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.KAPAAS, 8), new ItemStack(ItemRegistry.MUDRA_COPPER, 2), 16));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 3), new ItemStack(ItemRegistry.SOOT, 4), 12));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 6), new ItemStack(ItemRegistry.KHADI_VASTRA, 2), 8));
            }
            case VAIDYA -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.HALDI_RHIZOME, 6), new ItemStack(ItemRegistry.MUDRA_COPPER, 3), 12));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 3), new ItemStack(ItemRegistry.HALDI_POWDER, 2), 10));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 8), new ItemStack(ItemRegistry.BHASMA, 3), 6));
            }
            case SUNAR -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 16), new ItemStack(ItemRegistry.MUDRA_SILVER, 1), 32));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_SILVER, 16), new ItemStack(ItemRegistry.MUDRA_GOLD, 1), 16));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_SILVER, 4), new ItemStack(ItemRegistry.SONA_INGOT, 1), 6));
            }
            case MACHHUARA -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.KAURI, 6), new ItemStack(ItemRegistry.MUDRA_COPPER, 1), 30));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 2), new ItemStack(ItemRegistry.KAURI, 10), 20));
            }
            case SHILPI -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 3), new ItemStack(ItemRegistry.getItem("shilploka:fired_brick"), 8), 16));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_COPPER, 6), new ItemStack(ItemRegistry.getItem("shilploka:carved_stone"), 4), 12));
            }
            case SARPANCH -> {
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_SILVER, 2), new ItemStack(ItemRegistry.TALPANDULIPI, 1), 4));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_GOLD, 1), new ItemStack(ItemRegistry.MAURYA_EDICT, 1), 2));
                tradeOffers.add(new TradeOffer(new ItemStack(ItemRegistry.MUDRA_GOLD, 2), new ItemStack(ItemRegistry.HARAPPAN_SEAL, 1), 1));
            }
        }
    }
}
