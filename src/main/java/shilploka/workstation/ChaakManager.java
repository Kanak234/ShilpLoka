package shilploka.workstation;

import shilploka.item.ItemRegistry;
import shilploka.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * WHY:
 * Simulates Kumhar Chaak (the rotating Indian potter's wheel).
 * Wet river clay is spun and hand-sculpted into sacred and daily terracotta pottery:
 * Kulhad (chai cup), Handi (cooking pot), Matka (cooling water pot), and Surahi (jug).
 *
 * WHAT:
 * Workstation model holding clay inputs and allowing selection of pottery shapes.
 *
 * USED FOR:
 * Consulted by WorkstationScreen when interacting with a Chaak block.
 */
public class ChaakManager {

    public record PotteryShape(String id, String displayName, int clayRequired, String resultItemId, int resultCount) {}

    public static final List<PotteryShape> SHAPES = new ArrayList<>();

    static {
        SHAPES.add(new PotteryShape("kulhad", "Terracotta Kulhad (कुल्हड़)", 1, "shilploka:kulhad", 2));
        SHAPES.add(new PotteryShape("handi", "Earthen Handi (मिट्टी की हांड़ी)", 2, "shilploka:handi", 1));
        SHAPES.add(new PotteryShape("matka", "Earthen Matka (मिट्टी का मटका)", 3, "shilploka:matka", 1));
        SHAPES.add(new PotteryShape("surahi", "Clay Surahi (मिट्टी की सुराही)", 4, "shilploka:surahi", 1));
    }

    private ItemStack claySlot = ItemStack.EMPTY;
    private ItemStack outputSlot = ItemStack.EMPTY;
    private int selectedShapeIndex = 0;

    public boolean canShape(int shapeIndex) {
        if (shapeIndex < 0 || shapeIndex >= SHAPES.size()) return false;
        PotteryShape shape = SHAPES.get(shapeIndex);

        if (claySlot.isEmpty() || !claySlot.getItem().getId().equals("shilploka:mitti_clay")) {
            return false;
        }
        if (claySlot.getCount() < shape.clayRequired()) {
            return false;
        }

        if (outputSlot.isEmpty()) return true;
        if (!outputSlot.getItem().getId().equals(shape.resultItemId())) return false;
        return outputSlot.getCount() + shape.resultCount() <= outputSlot.getItem().getMaxStackSize();
    }

    public boolean craftShape(int shapeIndex) {
        if (!canShape(shapeIndex)) return false;
        PotteryShape shape = SHAPES.get(shapeIndex);

        claySlot.shrink(shape.clayRequired());
        if (claySlot.isEmpty()) claySlot = ItemStack.EMPTY;

        if (outputSlot.isEmpty()) {
            outputSlot = new ItemStack(ItemRegistry.getItem(shape.resultItemId()), shape.resultCount());
        } else {
            outputSlot.grow(shape.resultCount());
        }
        return true;
    }

    public ItemStack getClaySlot() { return claySlot; }
    public void setClaySlot(ItemStack claySlot) { this.claySlot = (claySlot != null) ? claySlot : ItemStack.EMPTY; }

    public ItemStack getOutputSlot() { return outputSlot; }
    public void setOutputSlot(ItemStack outputSlot) { this.outputSlot = (outputSlot != null) ? outputSlot : ItemStack.EMPTY; }

    public int getSelectedShapeIndex() { return selectedShapeIndex; }
    public void setSelectedShapeIndex(int selectedShapeIndex) { this.selectedShapeIndex = selectedShapeIndex; }
}
