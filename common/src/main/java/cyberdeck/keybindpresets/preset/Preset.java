package cyberdeck.keybindpresets.preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named set of key bindings.
 * source: "" for the player's own presets, "server:<address>" for one a server handed out.
 * bundles: key bundles for the KeyBind Bundles mod that come with the preset (often none).
 */
public final class Preset {
    private final String name;
    private final String minecraft;
    private final List<KeyEntry> keys;
    private final String source;
    private final List<Bundle> bundles;

    public Preset(String name, String minecraft, List<KeyEntry> keys, String source) {
        this(name, minecraft, keys, source, List.of());
    }

    public Preset(String name, String minecraft, List<KeyEntry> keys, String source, List<Bundle> bundles) {
        this.name = name;
        this.minecraft = minecraft == null ? "" : minecraft;
        this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
        this.source = source == null ? "" : source;
        this.bundles = bundles == null ? List.of() : List.copyOf(bundles);
    }

    public String name() { return name; }
    public String minecraft() { return minecraft; }
    public List<KeyEntry> keys() { return keys; }
    public String source() { return source; }
    public List<Bundle> bundles() { return bundles; }
    public boolean fromServer() { return source.startsWith("server:"); }

    /** The server address for a preset a server handed out, or "". */
    public String server() { return fromServer() ? source.substring("server:".length()) : ""; }

    public Preset withName(String newName) { return new Preset(newName, minecraft, keys, source, bundles); }
    public Preset withSource(String newSource) { return new Preset(name, minecraft, keys, newSource, bundles); }

    /** id -> entry, in file order. */
    public Map<String, KeyEntry> byId() {
        Map<String, KeyEntry> m = new LinkedHashMap<>();
        for (KeyEntry k : keys) m.putIfAbsent(k.id(), k);
        return m;
    }
}
