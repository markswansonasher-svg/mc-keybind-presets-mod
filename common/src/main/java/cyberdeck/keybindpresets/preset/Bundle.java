package cyberdeck.keybindpresets.preset;

import java.util.List;

/**
 * A key bundle from the KeyBind Bundles mod (matyrobbrt): one key that opens a radial menu of other keys.
 * Presets can carry them (the optional "bundles" field), so a server's preset can hand out bundles too.
 *
 * @param id       the bundle's UUID (lower case). Its key in the game is "key.keybindbundles.bundle_" + id.
 * @param name     shown in the game
 * @param entries  the keys in the radial menu
 * @param bookmark index of the entry a quick tap uses, or -1
 */
public record Bundle(String id, String name, List<Entry> entries, int bookmark) {

    public static final String KEY_PREFIX = "key.keybindbundles.bundle_";

    public Bundle {
        entries = List.copyOf(entries);
    }

    /** The id of this bundle's own key, as it appears in a preset's "keys". */
    public String keyId() { return KEY_PREFIX + id; }

    /**
     * @param key   the key mapping the entry presses, e.g. "key.jei.showRecipe"
     * @param title shown in the radial menu
     * @param icon  an item id for the menu icon, e.g. "minecraft:compass", or ""
     */
    public record Entry(String key, String title, String icon) {}
}
