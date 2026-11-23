package cyberdeck.keybindpresets.preset;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PresetFormatTest {

    static String sample() throws Exception {
        return Files.readString(Path.of(System.getProperty("deckSample")));
    }

    @Test
    void readsTheDecksSampleExport() throws Exception {
        Preset p = PresetFormat.parse(sample());
        assertEquals("Cozy Pack test keys", p.name());
        assertEquals("1.20.1", p.minecraft());
        assertEquals(24, p.keys().size());
        KeyEntry jei = p.byId().get("key.jei.showRecipe");
        assertEquals("key.keyboard.r", jei.key());
        assertEquals("SHIFT", jei.modifier());
        assertEquals("Show Recipe", jei.label());
        assertEquals("NONE", p.byId().get("key.jump").modifier());
        assertEquals(KeyEntry.UNBOUND, p.byId().get("key.minimap.waypoint").key());
    }

    @Test
    void writeThenReadGivesTheSamePreset() throws Exception {
        Preset p = PresetFormat.parse(sample());
        Preset back = PresetFormat.parse(PresetFormat.write(p, false));
        assertEquals(p.name(), back.name());
        assertEquals(p.keys(), back.keys());
        assertEquals(PresetFormat.hash(p), PresetFormat.hash(back));
    }

    @Test
    void sourceIsOnlyWrittenWhenAsked() throws Exception {
        Preset p = PresetFormat.parse(sample()).withSource("server:deck.example");
        assertTrue(PresetFormat.write(p, true).contains("server:deck.example"));
        assertFalse(PresetFormat.write(p, false).contains("server:"));
    }

    @Test
    void hashChangesWhenAKeyChanges() throws Exception {
        Preset p = PresetFormat.parse(sample());
        java.util.List<KeyEntry> keys = new java.util.ArrayList<>(p.keys());
        keys.set(0, keys.get(0).withKey("key.keyboard.h", "NONE"));
        assertNotEquals(PresetFormat.hash(p), PresetFormat.hash(new Preset(p.name(), p.minecraft(), keys, "")));
        assertNotEquals(PresetFormat.hash(p), PresetFormat.hash(p.withName("Other")));
        assertEquals(PresetFormat.hash(p), PresetFormat.hash(p.withSource("server:x")), "where it came from doesn't matter");
    }

    @Test
    void oddValuesAreCleanedNotTrusted() throws Exception {
        Preset p = PresetFormat.parse("{\"format\":\"cyberdeck-keys/1\",\"name\":\"A\\u0000B\",\"keys\":["
                + "{\"id\":\"key.jump\",\"key\":\"key.keyboard.space\",\"modifier\":\"hyper\",\"label\":\"x\\u001b[31m\"},"
                + "{\"id\":\"key.jump\",\"key\":\"key.keyboard.z\"}]}");
        assertEquals("AB", p.name());
        assertEquals(1, p.keys().size(), "a key listed twice: the first counts");
        assertEquals("NONE", p.keys().get(0).modifier());
        assertFalse(p.keys().get(0).label().contains("\u001b"));
    }

    @Test
    void refusesWhatIsntAPreset() {
        String[] bad = {
                "not json", "[1,2]", "{\"format\":\"other/1\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"key.keyboard.space\"}]}",
                "{\"format\":\"cyberdeck-keys/1\",\"keys\":[]}",
                "{\"format\":\"cyberdeck-keys/1\",\"keys\":[{\"id\":\"key jump\",\"key\":\"key.keyboard.space\"}]}",
                "{\"format\":\"cyberdeck-keys/1\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"space\"}]}",
                "{\"format\":\"cyberdeck-keys/1\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"key.keyboard.SPACE;x\"}]}",
                "x".repeat(PresetFormat.MAX_BYTES + 1)};
        for (String b : bad) assertThrows(PresetFormat.BadPresetException.class, () -> PresetFormat.parse(b), b.length() > 60 ? "huge" : b);
    }
}
