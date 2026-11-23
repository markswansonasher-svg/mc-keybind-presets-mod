package cyberdeck.keybindpresets.preset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The player's presets, one file each, in <game folder>/config/keybindpresets/:
 *   presets/*.json   the presets
 *   exports/         where Export writes (share these, or import them on the Deck Panel)
 *   import/          drop a preset file here, then Import in-game
 *   state.json       which preset is on, and which server presets you've already answered
 * Plain Java (no Minecraft) so it can be tested on its own.
 */
public final class PresetStore {
    public static final String DEFAULT = "Default";      // the mods' own keys; never stored
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;

    public PresetStore(Path root) { this.root = root; }

    public Path presetsDir() { return root.resolve("presets"); }
    public Path exportsDir() { return root.resolve("exports"); }
    public Path importDir() { return root.resolve("import"); }

    /** All saved presets, sorted by name. Broken files are skipped. */
    public List<Preset> list() {
        List<Preset> out = new ArrayList<>();
        if (!Files.isDirectory(presetsDir())) return out;
        try (Stream<Path> s = Files.list(presetsDir())) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json"))::iterator) {
                try {
                    out.add(PresetFormat.parse(Files.readString(p, StandardCharsets.UTF_8)));
                } catch (IOException | PresetFormat.BadPresetException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        out.sort(Comparator.comparing(p -> p.name().toLowerCase(Locale.ROOT)));
        return out;
    }

    public Optional<Preset> get(String name) {
        return list().stream().filter(p -> p.name().equalsIgnoreCase(name)).findFirst();
    }

    /** Save (or replace the one with the same name). */
    public void save(Preset p) throws IOException {
        Files.createDirectories(presetsDir());
        Path f = fileFor(p.name());
        Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
        Files.writeString(tmp, PresetFormat.write(p, true), StandardCharsets.UTF_8);
        Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public void delete(String name) throws IOException {
        Files.deleteIfExists(fileFor(name));
        if (name.equalsIgnoreCase(active())) setActive(DEFAULT);
    }

    public void rename(String from, String to) throws IOException {
        Preset p = get(from).orElseThrow(() -> new IOException("no preset called " + from));
        boolean wasActive = from.equalsIgnoreCase(active());
        Files.deleteIfExists(fileFor(from));
        save(p.withName(to));
        if (wasActive) setActive(to);
    }

    /** A name not used yet: "My keys", "My keys 2", ... (Default is taken). */
    public String freeName(String want) {
        String base = clean(want).isEmpty() ? "Preset" : clean(want);
        String n = base;
        for (int i = 2; n.equalsIgnoreCase(DEFAULT) || get(n).isPresent(); i++) n = base + " " + i;
        return n;
    }

    /** Write a preset to exports/ for sharing. Returns the file. */
    public Path export(Preset p) throws IOException {
        Files.createDirectories(exportsDir());
        Path f = exportsDir().resolve(fileName(p.name()));
        Files.writeString(f, PresetFormat.write(p.withSource(""), false), StandardCharsets.UTF_8);
        return f;
    }

    /** Preset files waiting in import/. */
    public List<Path> importable() throws IOException {
        Files.createDirectories(importDir());
        try (Stream<Path> s = Files.list(importDir())) {
            return s.filter(f -> f.toString().toLowerCase(Locale.ROOT).endsWith(".json")).sorted().toList();
        }
    }

    /** Import one file: saved under a free name (never overwrites). Returns the saved preset. */
    public Preset importFile(Path f) throws IOException, PresetFormat.BadPresetException {
        if (Files.size(f) > PresetFormat.MAX_BYTES) throw new PresetFormat.BadPresetException("That preset file is over 512 KB.");
        Preset p = PresetFormat.parse(Files.readString(f, StandardCharsets.UTF_8));
        p = p.withName(freeName(p.name())).withSource("");
        save(p);
        return p;
    }

    // ---- state: active preset + answered server presets
    public String active() { return state().has("active") ? state().get("active").getAsString() : DEFAULT; }

    public void setActive(String name) throws IOException {
        JsonObject s = state();
        s.addProperty("active", name);
        writeState(s);
    }

    /** The server preset fingerprint you last answered (Yes or No) for this server, or "". */
    public String answered(String server) {
        JsonObject s = state();
        return s.has("servers") && s.getAsJsonObject("servers").has(server) ? s.getAsJsonObject("servers").get(server).getAsString() : "";
    }

    public void setAnswered(String server, String hash) throws IOException {
        JsonObject s = state();
        if (!s.has("servers")) s.add("servers", new JsonObject());
        s.getAsJsonObject("servers").addProperty(server, hash);
        writeState(s);
    }

    private JsonObject state() {
        try {
            Path f = root.resolve("state.json");
            if (Files.exists(f)) {
                var e = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                if (e.isJsonObject()) return e.getAsJsonObject();
            }
        } catch (IOException | RuntimeException ignored) {
        }
        return new JsonObject();
    }

    private void writeState(JsonObject s) throws IOException {
        Files.createDirectories(root);
        Files.writeString(root.resolve("state.json"), GSON.toJson(s), StandardCharsets.UTF_8);
    }

    // ---- file names
    private Path fileFor(String name) { return presetsDir().resolve(fileName(name)); }

    /** "My Keys!" -> "my-keys.json"; anything odd becomes "-", so a name can never reach another folder. */
    static String fileName(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (s.isEmpty()) s = "preset";
        if (s.length() > 60) s = s.substring(0, 60);
        return s + ".json";
    }

    /** A name players type: no control characters, trimmed, at most 60 long. */
    public static String clean(String name) {
        return PresetFormat.text(name == null ? "" : name, 60).trim();
    }
}
