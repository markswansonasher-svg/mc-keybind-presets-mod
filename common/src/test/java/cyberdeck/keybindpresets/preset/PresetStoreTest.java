package cyberdeck.keybindpresets.preset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PresetStoreTest {
    @TempDir Path dir;

    static Preset preset(String name, String key) {
        return new Preset(name, "1.20.1", List.of(new KeyEntry("key.jump", key, "NONE", "key.keyboard.space", "Jump", "key.categories.movement")), "");
    }

    @Test
    void saveListGetDelete() throws Exception {
        PresetStore s = new PresetStore(dir);
        assertTrue(s.list().isEmpty());
        assertEquals(PresetStore.DEFAULT, s.active());
        s.save(preset("Building", "key.keyboard.b"));
        s.save(preset("PvP", "key.keyboard.p"));
        assertEquals(List.of("Building", "PvP"), s.list().stream().map(Preset::name).toList());
        assertEquals("key.keyboard.b", s.get("building").orElseThrow().keys().get(0).key(), "names are case-insensitive");
        s.save(preset("Building", "key.keyboard.c"));
        assertEquals(2, s.list().size(), "same name replaces");
        s.setActive("PvP");
        s.delete("pvp");
        assertEquals(PresetStore.DEFAULT, s.active(), "deleting the active one -> Default");
    }

    @Test
    void freeNamesNeverClash() throws Exception {
        PresetStore s = new PresetStore(dir);
        assertEquals("My keys", s.freeName("My keys"));
        s.save(preset("My keys", "key.keyboard.a"));
        assertEquals("My keys 2", s.freeName("My keys"));
        assertEquals("Default 2", s.freeName("Default"), "Default is never taken by a real preset");
        assertEquals("Preset", s.freeName("\u0000\u0001"));
    }

    @Test
    void renameKeepsItActive() throws Exception {
        PresetStore s = new PresetStore(dir);
        s.save(preset("Old", "key.keyboard.a"));
        s.setActive("Old");
        s.rename("Old", "New");
        assertTrue(s.get("New").isPresent());
        assertTrue(s.get("Old").isEmpty());
        assertEquals("New", s.active());
    }

    @Test
    void fileNamesCantEscapeTheFolder() {
        assertEquals("my-keys.json", PresetStore.fileName("My Keys!"));
        assertEquals("etc-passwd.json", PresetStore.fileName("../../etc/passwd"));
        assertEquals("c-windows.json", PresetStore.fileName("C:\\Windows"));
        assertEquals("preset.json", PresetStore.fileName("..."));
    }

    @Test
    void exportAndImport() throws Exception {
        PresetStore s = new PresetStore(dir);
        Path f = s.export(preset("Building", "key.keyboard.b").withSource("server:x"));
        String json = Files.readString(f);
        assertFalse(json.contains("server:"), "exports don't say where a preset came from");
        Files.createDirectories(s.importDir());
        Files.copy(f, s.importDir().resolve("building.json"));
        Files.writeString(s.importDir().resolve("junk.json"), "nope");
        Files.writeString(s.importDir().resolve("readme.txt"), "not a preset");
        assertEquals(2, s.importable().size(), "only .json files are offered");
        s.save(preset("Building", "key.keyboard.z"));
        Preset in = s.importFile(s.importDir().resolve("building.json"));
        assertEquals("Building 2", in.name(), "import never overwrites");
        assertThrows(PresetFormat.BadPresetException.class, () -> s.importFile(s.importDir().resolve("junk.json")));
    }

    @Test
    void remembersServerAnswers() throws Exception {
        PresetStore s = new PresetStore(dir);
        assertEquals("", s.answered("deck.example"));
        s.setAnswered("deck.example", "abc");
        s.setActive("X");
        assertEquals("abc", new PresetStore(dir).answered("deck.example"));
        assertEquals("X", new PresetStore(dir).active());
    }

    @Test
    void brokenFilesAreSkipped() throws Exception {
        PresetStore s = new PresetStore(dir);
        s.save(preset("Good", "key.keyboard.g"));
        Files.writeString(s.presetsDir().resolve("bad.json"), "{broken");
        Files.writeString(dir.resolve("state.json"), "{broken");
        assertEquals(1, s.list().size());
        assertEquals(PresetStore.DEFAULT, s.active());
    }
}
