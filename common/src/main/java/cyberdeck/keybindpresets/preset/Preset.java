package cyberdeck.keybindpresets.preset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A named set of key bindings.
 * source: "" for the player's own presets, "server:<address>" for one a server handed out.
 */
public final class Preset {
    private final String name;
    private final String minecraft;
    private final List<KeyEntry> keys;
    private final String source;

    public Preset(String name, String minecraft, List<KeyEntry> keys, String source) {
        this.name = name;
        this.minecraft = minecraft == null ? "" : minecraft;
        this.keys = Collections.unmodifiableList(new ArrayList<>(keys));
        this.source = source == null ? "" : source;
    }

    public String name() { return name; }
    public String minecraft() { return minecraft; }
    public List<KeyEntry> keys() { return keys; }
    public String source() { return source; }
    public boolean fromServer() { return source.startsWith("server:"); }

    public Preset withName(String newName) { return new Preset(newName, minecraft, keys, source); }
    public Preset withSource(String newSource) { return new Preset(name, minecraft, keys, newSource); }

    /** id -> entry, in file order. */
    public Map<String, KeyEntry> byId() {
        Map<String, KeyEntry> m = new LinkedHashMap<>();
        for (KeyEntry k : keys) m.putIfAbsent(k.id(), k);
        return m;
    }
}
