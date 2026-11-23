package cyberdeck.keybindpresets.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads and writes preset files, format "cyberdeck-keys/1".
 * Same rules as the Deck Panel (cyberdeck pi5/decklib.py clean_preset) - keep them in step.
 */
public final class PresetFormat {
    public static final String FORMAT = "cyberdeck-keys/1";
    public static final int MAX_KEYS = 3000;
    public static final int MAX_BYTES = 512 * 1024;
    static final Pattern KEY_ID = Pattern.compile("^[A-Za-z0-9_.:/-]{1,120}$");
    static final Pattern KEY_NAME = Pattern.compile("^key\\.(keyboard|mouse)\\.[a-z0-9.]{1,30}$");
    static final Pattern MC_VERSION = Pattern.compile("^\\d+\\.\\d+(\\.\\d+)?$");
    static final Set<String> MODIFIERS = Set.of("NONE", "SHIFT", "CONTROL", "ALT");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private PresetFormat() {}

    /** A file that isn't a usable preset. The message is for players. */
    public static final class BadPresetException extends Exception {
        public BadPresetException(String msg) { super(msg); }
    }

    public static Preset parse(String json) throws BadPresetException {
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new BadPresetException("That preset file is over 512 KB.");
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (RuntimeException e) {
            throw new BadPresetException("That isn't a keybind preset file (it's not JSON).");
        }
        if (!root.isJsonObject() || !FORMAT.equals(str(root.getAsJsonObject(), "format")))
            throw new BadPresetException("That isn't a keybind preset file (format " + FORMAT + ").");
        JsonObject o = root.getAsJsonObject();
        if (!o.has("keys") || !o.get("keys").isJsonArray() || o.getAsJsonArray("keys").isEmpty())
            throw new BadPresetException("That preset has no keys in it.");
        JsonArray arr = o.getAsJsonArray("keys");
        if (arr.size() > MAX_KEYS) throw new BadPresetException("That preset has more than " + MAX_KEYS + " keys.");
        List<KeyEntry> keys = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JsonElement e : arr) {
            if (!e.isJsonObject()) throw new BadPresetException("A key in that preset is broken.");
            JsonObject k = e.getAsJsonObject();
            String id = str(k, "id"), key = str(k, "key");
            if (!KEY_ID.matcher(id).matches()) throw new BadPresetException("Odd key name in that preset: " + cut(id, 40));
            if (!KEY_NAME.matcher(key).matches()) throw new BadPresetException("Odd key for " + id + ": " + cut(key, 40));
            if (!seen.add(id)) continue;                         // listed twice: the first one counts
            String mod = str(k, "modifier").toUpperCase(Locale.ROOT);
            if (!MODIFIERS.contains(mod)) mod = "NONE";
            String def = str(k, "default");
            keys.add(new KeyEntry(id, key, mod, KEY_NAME.matcher(def).matches() ? def : "",
                    text(str(k, "label"), 120), text(str(k, "category"), 120)));
        }
        String name = text(str(o, "name"), 60).trim();
        String mc = str(o, "minecraft");
        return new Preset(name.isEmpty() ? "Server preset" : name, MC_VERSION.matcher(mc).matches() ? mc : "",
                keys, text(str(o, "source"), 200));
    }

    /** JSON text for a preset. withSource: include our own "source" field (not for exports or the server). */
    public static String write(Preset p, boolean withSource) {
        JsonObject o = new JsonObject();
        o.addProperty("format", FORMAT);
        o.addProperty("name", p.name());
        if (!p.minecraft().isEmpty()) o.addProperty("minecraft", p.minecraft());
        if (withSource && !p.source().isEmpty()) o.addProperty("source", p.source());
        JsonArray arr = new JsonArray();
        for (KeyEntry k : p.keys()) {
            JsonObject e = new JsonObject();
            e.addProperty("id", k.id());
            e.addProperty("key", k.key());
            if (!"NONE".equals(k.modifier())) e.addProperty("modifier", k.modifier());
            if (!k.def().isEmpty()) e.addProperty("default", k.def());
            if (!k.label().isEmpty()) e.addProperty("label", k.label());
            if (!k.category().isEmpty()) e.addProperty("category", k.category());
            arr.add(e);
        }
        o.add("keys", arr);
        return GSON.toJson(o);
    }

    /** Fingerprint of what a preset does (name + keys), to notice when a server's preset changed. */
    public static String hash(Preset p) {
        StringBuilder sb = new StringBuilder(p.name()).append('\n');
        for (KeyEntry k : p.keys()) sb.append(k.id()).append('=').append(k.key()).append('+').append(k.modifier()).append('\n');
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : "";
    }

    /** Display text from a file: no control characters, at most n long. */
    static String text(String s, int n) {
        String t = s.replaceAll("[\\x00-\\x1f\\x7f]", "");
        return t.length() > n ? t.substring(0, n) : t;
    }

    private static String cut(String s, int n) { return text(s, n); }
}
