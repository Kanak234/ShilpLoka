package shilploka.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * WHY:
 * Central internationalization (I18n) subsystem providing dynamic bilingual
 * localization (English & Devanagari Hindi) across all menus, HUDs, dialogs,
 * codex entries, and error prompts.
 *
 * WHAT:
 * - Loads JSON dictionaries from assets/shilploka/lang/{lang}.json.
 * - Always maintains English as a reliable fallback dictionary.
 * - Supports parameterized string formatting: Localization.get("key", arg1, ...).
 */
public class Localization {

    private static final Logger LOGGER = LoggerFactory.getLogger(Localization.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static final String DEFAULT_LANG = "en";
    public static final String HINDI_LANG = "hi";

    private static String currentLanguage = DEFAULT_LANG;
    private static final Map<String, String> CURRENT_STRINGS = new HashMap<>();
    private static final Map<String, String> FALLBACK_STRINGS = new HashMap<>();

    static {
        // Load default English fallback dictionary upon initialization
        loadDictionary(DEFAULT_LANG, FALLBACK_STRINGS);
        loadDictionary(DEFAULT_LANG, CURRENT_STRINGS);
    }

    private static void loadDictionary(String langCode, Map<String, String> targetMap) {
        targetMap.clear();
        String resourcePath = "/assets/shilploka/lang/" + langCode + ".json";
        try (InputStream in = Localization.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Map<String, String> parsed = MAPPER.readValue(in, new TypeReference<>() {});
                targetMap.putAll(parsed);
                LOGGER.info("Loaded {} localization strings for language [{}] from classpath", targetMap.size(), langCode);
                return;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load language [{}] from classpath: {}", langCode, e.getMessage());
        }

        // Secondary fallback to filesystem path
        try {
            Path diskPath = Paths.get("assets", "lang", langCode + ".json");
            if (Files.exists(diskPath)) {
                try (InputStream in = Files.newInputStream(diskPath)) {
                    Map<String, String> parsed = MAPPER.readValue(in, new TypeReference<>() {});
                    targetMap.putAll(parsed);
                    LOGGER.info("Loaded {} localization strings for language [{}] from disk", targetMap.size(), langCode);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load language [{}] from disk: {}", langCode, e.getMessage());
        }
    }

    public static synchronized void setLanguage(String langCode) {
        if (langCode == null || langCode.isBlank()) return;
        currentLanguage = langCode.toLowerCase();
        loadDictionary(currentLanguage, CURRENT_STRINGS);
    }

    public static String getLanguage() {
        return currentLanguage;
    }

    public static boolean isHindi() {
        return HINDI_LANG.equalsIgnoreCase(currentLanguage);
    }

    /**
     * Resolves localized string by key with optional format arguments.
     */
    public static String get(String key, Object... args) {
        if (key == null) return "";
        String text = CURRENT_STRINGS.get(key);
        if (text == null) {
            text = FALLBACK_STRINGS.get(key);
        }
        if (text == null) {
            return key; // Key fallback if completely absent
        }

        if (args != null && args.length > 0) {
            try {
                return String.format(text, args);
            } catch (Exception e) {
                return text;
            }
        }
        return text;
    }
}
