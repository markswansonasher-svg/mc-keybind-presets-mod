package cyberdeck.keybindpresets.preset;

/** One key binding in a preset (see docs/keybind-preset-format.md in the cyberdeck project). */
public record KeyEntry(String id, String key, String modifier, String def, String label, String category) {

    public static final String UNBOUND = "key.keyboard.unknown";

    public KeyEntry withKey(String newKey, String newModifier) {
        return new KeyEntry(id, newKey, newModifier, def, label, category);
    }
}
