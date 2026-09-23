package shilploka.ui;

import org.joml.Matrix4f;
import shilploka.render.Shader;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * WHY:
 * The Itihaas Granth (Codex of Bharat) serves as the player's illustrated historical and
 * cultural compendium. Chronicles ancient Indian architecture, metallurgy secrets,
 * Ayurvedic botany, wildlife ecology, and mythological folklore.
 *
 * WHAT:
 * Dual-pane interactive book UI opened via the 'G' key.
 * Left pane contains categories and historical entries; right pane renders detailed lore,
 * architectural guidance, and crafting notes.
 *
 * USED FOR:
 * Opened and closed in Game.java. Headless-safe for automated unit testing.
 */
public class CodexScreen {

    public record CodexEntry(String id, String title, String era, String category, List<String> lines) {}

    private boolean open = false;
    private int selectedCategoryIndex = 0;
    private int selectedEntryIndex = 0;
    private final List<String> categories = new ArrayList<>();
    private final List<CodexEntry> allEntries = new ArrayList<>();
    private final Matrix4f orthoMatrix = new Matrix4f();

    public CodexScreen() {
        initCategoriesAndEntries();
    }

    private void initCategoriesAndEntries() {
        categories.add("1. VASTU KALA (Architecture)");
        categories.add("2. DHATU VIDYA (Metallurgy)");
        categories.add("3. VANASPATI (Flora & Herbs)");
        categories.add("4. PRANI JAGAT (Fauna)");
        categories.add("5. LOKA KATHA (Folklore & Myth)");

        // 1. Architecture entries
        allEntries.add(new CodexEntry(
                "harappan_bath",
                "Harappan Great Bath & Granary",
                "Sindhu-Saraswati Era (~2600 BCE)",
                categories.get(0),
                List.of(
                        "The Great Bath of Mohenjo-Daro is the earliest public water tank",
                        "in ancient world history. Built using precision-fired clay bricks",
                        "laid in English bond and sealed with natural bitumen pitch to create",
                        "an impervious waterproof seal.",
                        "",
                        "Structure: 14x14 courtyard with colonnaded brick cloisters,",
                        "recessed central bath with north and south stairs, and granary vaults.",
                        "Loot: Harappan Pashupati seals, Mudra coins, and terracotta pottery."
                )
        ));

        allEntries.add(new CodexEntry(
                "buddhist_stupa",
                "Great Buddhist Stupa",
                "Mauryan Empire (~3rd Century BCE)",
                categories.get(0),
                List.of(
                        "The Great Stupa at Sanchi embodies monumental sacred stone masonry.",
                        "Consists of a hemispherical sandstone dome (Anda) sheltering sacred",
                        "relics, surrounded by a circumambulatory path (Pradakshina Path).",
                        "",
                        "Structure: Square Medhi plinth, crowned with a square balcony",
                        "(Harmika) and three-tiered umbrella spire (Chhatra).",
                        "Loot: Maurya edict inscriptions, palm leaf manuscripts, and Mudras."
                )
        ));

        allEntries.add(new CodexEntry(
                "chola_stepwell",
                "Ancient Stepwell (Baoli / Kalyani)",
                "Chola & Solanki Eras (~10th Century CE)",
                categories.get(0),
                List.of(
                        "Masterpieces of subterranean hydro-engineering across arid regions.",
                        "Reaches deep into the earth to tap groundwater aquifers through",
                        "geometric stepped stairwells and ornate carved pillar galleries.",
                        "",
                        "Structure: 12x12 subterranean pit descending through tiered terraces",
                        "with pristine water cistern reservoir at the base.",
                        "Loot: Copper plate grants (Tamrapatra), Kauri barter shells, bronze."
                )
        ));

        allEntries.add(new CodexEntry(
                "hill_fort",
                "Maratha-Rajput Hill Fort (Giri Durg)",
                "Medieval Bharat (~12th-17th Century CE)",
                categories.get(0),
                List.of(
                        "Formidable defensive hill forts built atop rugged mountain scarps.",
                        "Utilized natural basalt and sandstone ramparts, angled watchtower",
                        "bastions, and crenellated battlements.",
                        "",
                        "Structure: 14x14 fortified enclosure with corner round bastions,",
                        "defensive arrow slits, timber beam floors, and central armory.",
                        "Loot: Wootz crucible steel ingots, Talwar blades, and gold mudras."
                )
        ));

        allEntries.add(new CodexEntry(
                "ashoka_sthambha",
                "Monolithic Ashokan Edict Pillar",
                "Mauryan Era (~250 BCE)",
                categories.get(0),
                List.of(
                        "Monolithic polished Chunar sandstone pillars erected by Emperor",
                        "Ashoka to propagate moral Dhamma edicts across the subcontinent.",
                        "",
                        "Structure: 11-block tall carved pillar shaft with inverted bell lotus",
                        "capital, abacus, and crowning Dharma Chakra wheel.",
                        "Loot: Ashokan Dhamma edicts, silver mudra coins, and sacred peacock feathers."
                )
        ));

        // 2. Metallurgy entries
        allEntries.add(new CodexEntry(
                "wootz_steel",
                "Wootz Crucible Steel (Ukku / Hinduwani)",
                "Ancient Tamilakam & Telangana (~300 BCE)",
                categories.get(1),
                List.of(
                        "Ancient India pioneered ultra-high carbon crucible steel celebrated",
                        "across the ancient world for its wavy Damascene patterns and sharp edge.",
                        "Synthesized in closed clay crucibles by firing pure iron ore with",
                        "carbon-rich biomass and coal in the Smelting Bhatti.",
                        "",
                        "Crafting: Combine Loha Ingot + Koyla inside Smelting Bhatti.",
                        "Properties: Exceptional hardness (2500 uses) and +9.0 weapon damage."
                )
        ));

        allEntries.add(new CodexEntry(
                "kansa_bronze",
                "Ancient Kansa Alloy & Lost-Wax Casting",
                "Sindhu-Saraswati Civilization (~2500 BCE)",
                categories.get(1),
                List.of(
                        "Bell metal bronze alloy composed of copper and tin/zinc.",
                        "Used in lost-wax casting (Dhokra) to craft sacred idols and durable tools.",
                        "",
                        "Crafting: Smelt Taamba Ore + Taamba Ingot in the Smelting Bhatti.",
                        "Durability: 350 uses, surpassing copper and stone."
                )
        ));

        // 3. Flora & Herbs entries
        allEntries.add(new CodexEntry(
                "sal_tree",
                "Sal Vriksha (Shorea robusta)",
                "Ganga Maidan & Central Indian Woodlands",
                categories.get(2),
                List.of(
                        "Dense, resinous hardwood revered since the Vedic period.",
                        "Provides the primary structural timber for Indian architectural beams,",
                        "charpais, bullock carts, and temple mandapas.",
                        "",
                        "Usage: Harvest with Kulhadi to craft Sal Planks, charcoal, and sticks."
                )
        ));

        allEntries.add(new CodexEntry(
                "deodar_cedar",
                "Deodar Vriksha (Devadaru / Tree of the Gods)",
                "Himalayan Shikhar Highlands",
                categories.get(2),
                List.of(
                        "Towering cedar endemic to high altitude Himalayan ridges.",
                        "Renowned for fragrant aromatic wood and resistance to mountain frost.",
                        "",
                        "Usage: Yields rot-resistant aromatic timber for mountain shrines."
                )
        ));

        // 4. Fauna entries
        allEntries.add(new CodexEntry(
                "pavitra_gau",
                "Pavitra Gau (Sacred Zebu Cow)",
                "Plains & Villages across Bharat",
                categories.get(3),
                List.of(
                        "The humped Bos indicus is sacred and culturally venerated across Bharat.",
                        "In ShilpLoka, the Gau is completely immune to player attacks and",
                        "cannot be harmed. Milked gently using earthen Kulhad or Matka.",
                        "",
                        "Drops: Zero meat drops. Provides nourishing Gau Doodh."
                )
        ));

        allEntries.add(new CodexEntry(
                "royal_tiger",
                "Royal Bengal Bagh (Panthera tigris)",
                "Sundarban & Sahyadri Deep Jungles",
                categories.get(3),
                List.of(
                        "Apex predator of the Indian subcontinent.",
                        "Stalks swiftly through dense mangroves and teak forests. Possesses",
                        "high agility and a lethal pouncing strike dealing 8.0 hearts damage.",
                        "",
                        "Combat: Engage with Talwar and Kansa/Loha armor; keep torch lit."
                )
        ));

        // 5. Folklore entries
        allEntries.add(new CodexEntry(
                "vetaal_spirit",
                "Vetaal (Nocturnal Spirit)",
                "Ravines, Ruins & Wilderness",
                categories.get(4),
                List.of(
                        "Enigmatic spirit dwelling in ancient ruins and cemetery grounds.",
                        "Active solely between midnight and dawn. Bursts into flames upon",
                        "contact with early morning sunlight.",
                        "",
                        "Drops: Sacred Bhasma Ash used in Ayurvedic potions and Kaushal altars."
                )
        ));

        allEntries.add(new CodexEntry(
                "andhakaar_naag",
                "Andhakaar Naag (Subterranean World Serpent)",
                "Paataal Volcanic Depths",
                categories.get(4),
                List.of(
                        "Legendary multi-segmented subterranean serpent guarding the",
                        "basalt core of the underworld dimension.",
                        "",
                        "Defeat Reward: Yields the legendary Vajra Core to unlock celestial portals."
                )
        ));
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle() {
        this.open = !this.open;
    }

    public List<String> getCategories() {
        return categories;
    }

    public List<CodexEntry> getAllEntries() {
        return allEntries;
    }

    public List<CodexEntry> getEntriesForCategory(String category) {
        List<CodexEntry> filtered = new ArrayList<>();
        for (CodexEntry entry : allEntries) {
            if (entry.category().equals(category)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public CodexEntry getActiveEntry() {
        List<CodexEntry> currentCategoryEntries = getEntriesForCategory(categories.get(selectedCategoryIndex));
        if (currentCategoryEntries.isEmpty()) return null;
        int clamped = Math.max(0, Math.min(selectedEntryIndex, currentCategoryEntries.size() - 1));
        return currentCategoryEntries.get(clamped);
    }

    public void nextCategory() {
        selectedCategoryIndex = (selectedCategoryIndex + 1) % categories.size();
        selectedEntryIndex = 0;
    }

    public void previousCategory() {
        selectedCategoryIndex = (selectedCategoryIndex - 1 + categories.size()) % categories.size();
        selectedEntryIndex = 0;
    }

    public void nextEntry() {
        List<CodexEntry> cur = getEntriesForCategory(categories.get(selectedCategoryIndex));
        if (!cur.isEmpty()) {
            selectedEntryIndex = (selectedEntryIndex + 1) % cur.size();
        }
    }

    public void previousEntry() {
        List<CodexEntry> cur = getEntriesForCategory(categories.get(selectedCategoryIndex));
        if (!cur.isEmpty()) {
            selectedEntryIndex = (selectedEntryIndex - 1 + cur.size()) % cur.size();
        }
    }

    public void handleKeyPress(int key) {
        if (!open) return;

        if (key == GLFW_KEY_G || key == GLFW_KEY_ESCAPE) {
            this.open = false;
        } else if (key == GLFW_KEY_LEFT || key == GLFW_KEY_A) {
            previousCategory();
        } else if (key == GLFW_KEY_RIGHT || key == GLFW_KEY_D) {
            nextCategory();
        } else if (key == GLFW_KEY_UP || key == GLFW_KEY_W) {
            previousEntry();
        } else if (key == GLFW_KEY_DOWN || key == GLFW_KEY_S) {
            nextEntry();
        } else if (key >= GLFW_KEY_1 && key <= GLFW_KEY_5) {
            selectedCategoryIndex = key - GLFW_KEY_1;
            selectedEntryIndex = 0;
        }
    }

    /**
     * Renders the Itihaas Granth dual-pane codex on screen.
     */
    public void render(Shader uiShader, BitmapFont font, int windowWidth, int windowHeight) {
        if (!open) return;

        orthoMatrix.identity().ortho(0, windowWidth, windowHeight, 0, -1, 1);

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        uiShader.bind();
        uiShader.setMat4("u_Projection", orthoMatrix);

        // Book parchment background (centered 820 x 520)
        float bookW = Math.min(840, windowWidth - 40);
        float bookH = Math.min(540, windowHeight - 40);
        float bookX = (windowWidth - bookW) / 2.0f;
        float bookY = (windowHeight - bookH) / 2.0f;
        float leftW = 280;
        float rightX = bookX + leftW + 20;
        float rightW = bookW - leftW - 30;

        // --- 1. RENDER FLAT RECTANGLES ---
        uiShader.setBoolean("u_UseTexture", false);

        // Dim backdrop
        font.drawRect(0, 0, windowWidth, windowHeight, 0.0f, 0.0f, 0.0f, 0.65f);

        // Parchment & Terracotta frame
        font.drawRect(bookX - 4, bookY - 4, bookX + bookW + 4, bookY + bookH + 4, 0.45f, 0.22f, 0.12f, 1.0f);
        font.drawRect(bookX, bookY, bookX + bookW, bookY + bookH, 0.94f, 0.88f, 0.74f, 1.0f);

        // Header bar
        font.drawRect(bookX, bookY, bookX + bookW, bookY + 36, 0.32f, 0.15f, 0.08f, 1.0f);

        // Left pane panel
        font.drawRect(bookX + 10, bookY + 45, bookX + 10 + leftW, bookY + bookH - 10, 0.88f, 0.81f, 0.67f, 1.0f);

        // Active category highlight
        float cy = bookY + 55;
        for (int i = 0; i < categories.size(); i++) {
            if (i == selectedCategoryIndex) {
                font.drawRect(bookX + 15, cy - 2, bookX + leftW + 5, cy + 14, 0.45f, 0.22f, 0.12f, 0.85f);
            }
            cy += 18;
        }

        // Horizontal divider in left pane
        font.drawRect(bookX + 15, cy + 4, bookX + leftW + 5, cy + 6, 0.6f, 0.5f, 0.4f, 1.0f);
        cy += 28;

        // Active entry highlight in left pane
        List<CodexEntry> curEntries = getEntriesForCategory(categories.get(selectedCategoryIndex));
        for (int j = 0; j < curEntries.size(); j++) {
            if (j == selectedEntryIndex) {
                font.drawRect(bookX + 15, cy - 2, bookX + leftW + 5, cy + 14, 0.2f, 0.4f, 0.25f, 0.9f);
            }
            cy += 18;
        }

        // Right pane divider
        font.drawRect(rightX, bookY + 95, rightX + rightW - 10, bookY + 97, 0.6f, 0.4f, 0.2f, 1.0f);

        font.flush();

        // --- 2. RENDER TEXT ---
        uiShader.setBoolean("u_UseTexture", true);
        font.bindTexture();

        // Header text
        font.drawString("=== ITIHAAS GRANTH (CODEX OF SHILPLOKA) ===", bookX + 20, bookY + 12, 1.5f, 1.0f, 0.84f, 0.0f, 1.0f, true);
        font.drawString("[ESC/G: Close  |  A/D: Category  |  W/S: Entry  |  1-5: Jump]", bookX + bookW - 440, bookY + 14, 1.0f, 0.9f, 0.9f, 0.9f, 1.0f, false);

        // Left pane categories
        cy = bookY + 55;
        for (int i = 0; i < categories.size(); i++) {
            boolean activeCat = (i == selectedCategoryIndex);
            if (activeCat) {
                font.drawString("> " + categories.get(i), bookX + 18, cy, 1.0f, 1.0f, 0.9f, 0.2f, 1.0f, true);
            } else {
                font.drawString("  " + categories.get(i), bookX + 18, cy, 1.0f, 0.3f, 0.2f, 0.1f, 1.0f, false);
            }
            cy += 18;
        }

        cy += 12;
        font.drawString("--- CHAPTER TOPICS ---", bookX + 20, cy, 1.0f, 0.4f, 0.25f, 0.15f, 1.0f, false);
        cy += 16;

        // Current Category Entries
        for (int j = 0; j < curEntries.size(); j++) {
            CodexEntry e = curEntries.get(j);
            boolean activeEntry = (j == selectedEntryIndex);
            if (activeEntry) {
                font.drawString(" * " + e.title(), bookX + 18, cy, 1.0f, 1.0f, 1.0f, 0.7f, 1.0f, true);
            } else {
                font.drawString("   " + e.title(), bookX + 18, cy, 1.0f, 0.25f, 0.18f, 0.12f, 1.0f, false);
            }
            cy += 18;
        }

        // Right Pane: Active Entry Details
        CodexEntry active = getActiveEntry();
        if (active != null) {
            font.drawString(active.title(), rightX, bookY + 55, 1.6f, 0.45f, 0.15f, 0.05f, 1.0f, true);
            font.drawString("Era: " + active.era(), rightX, bookY + 80, 1.0f, 0.6f, 0.3f, 0.1f, 1.0f, false);

            float lineY = bookY + 110;
            for (String line : active.lines()) {
                font.drawString(line, rightX, lineY, 1.0f, 0.18f, 0.14f, 0.10f, 1.0f, false);
                lineY += 18;
            }
        }

        font.flush();
        font.unbindTexture();
        uiShader.unbind();

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
}
