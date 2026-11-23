package cyberdeck.keybindpresets.preset;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Key bundles (KeyBind Bundles mod) carried in presets. */
class BundleTest {
    static final String ID = "0f8fad5b-d9cb-469f-a165-70867728950e";

    static final String WITH_BUNDLE = "{\"format\":\"cyberdeck-keys/1\",\"name\":\"B\",\"keys\":["
            + "{\"id\":\"key.keybindbundles.bundle_" + ID + "\",\"key\":\"key.keyboard.g\"}],"
            + "\"bundles\":[{\"id\":\"" + ID.toUpperCase() + "\",\"name\":\"Maps\",\"bookmark\":1,\"entries\":["
            + "{\"key\":\"key.minimap.waypoint\",\"title\":\"Waypoint\",\"icon\":\"minecraft:compass\"},"
            + "{\"key\":\"key.jei.showRecipe\",\"title\":\"Recipe\"}]}]}";

    @Test
    void readsBundlesFromAPreset() throws Exception {
        Preset p = PresetFormat.parse(WITH_BUNDLE);
        assertEquals(1, p.bundles().size());
        Bundle b = p.bundles().get(0);
        assertEquals(ID, b.id(), "ids are kept in lower case, like the mod writes them");
        assertEquals("Maps", b.name());
        assertEquals(1, b.bookmark());
        assertEquals(List.of(new Bundle.Entry("key.minimap.waypoint", "Waypoint", "minecraft:compass"),
                new Bundle.Entry("key.jei.showRecipe", "Recipe", "")), b.entries());
        assertEquals("key.keybindbundles.bundle_" + ID, b.keyId());
        assertTrue(p.byId().containsKey(b.keyId()));
    }

    @Test
    void writeThenReadKeepsBundles() throws Exception {
        Preset p = PresetFormat.parse(WITH_BUNDLE);
        Preset back = PresetFormat.parse(PresetFormat.write(p, false));
        assertEquals(p.bundles(), back.bundles());
        assertEquals(PresetFormat.hash(p), PresetFormat.hash(back));
        assertEquals(p.bundles(), p.withName("X").withSource("server:a").bundles());
    }

    @Test
    void noBundlesMeansNoField() throws Exception {
        Preset p = PresetFormat.parse(PresetFormatTest.sample());
        assertTrue(p.bundles().isEmpty());
        assertFalse(PresetFormat.write(p, false).contains("bundles"));
    }

    @Test
    void hashNoticesBundleChanges() throws Exception {
        Preset p = PresetFormat.parse(WITH_BUNDLE);
        Preset none = new Preset(p.name(), p.minecraft(), p.keys(), "");
        assertNotEquals(PresetFormat.hash(p), PresetFormat.hash(none));
    }

    @Test
    void readsTheModsOwnFile() {
        // keybind_bundles.json as KeyBind Bundles writes it: icons are item stacks
        String file = "[{\"id\":\"" + ID + "\",\"name\":\"Maps\",\"entries\":[{\"key\":\"key.jump\",\"title\":\"Jump\","
                + "\"icon\":{\"id\":\"minecraft:feather\",\"Count\":1}}],\"bookmark\":-1}]";
        List<Bundle> bs = PresetFormat.parseBundles(JsonParser.parseString(file));
        assertEquals(1, bs.size());
        assertEquals("minecraft:feather", bs.get(0).entries().get(0).icon());
        // and back into the mod's shape
        var o = PresetFormat.bundleJson(bs.get(0), true);
        assertEquals("minecraft:feather", o.getAsJsonArray("entries").get(0).getAsJsonObject()
                .getAsJsonObject("icon").get("id").getAsString());
        assertEquals(-1, o.get("bookmark").getAsInt());
    }

    @Test
    void brokenBundlesAreLeftOutNotFatal() throws Exception {
        String json = "{\"format\":\"cyberdeck-keys/1\",\"name\":\"B\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"key.keyboard.space\"}],"
                + "\"bundles\":[{\"id\":\"not-a-uuid\",\"name\":\"x\"},"
                + "{\"id\":\"" + ID + "\",\"name\":\"\\u0000Ok\",\"bookmark\":9,\"entries\":["
                + "{\"key\":\"bad key\",\"title\":\"x\"},{\"key\":\"key.jump\",\"title\":\"J\",\"icon\":\"../../evil\"}]},"
                + "{\"id\":\"" + ID + "\",\"name\":\"twice\"}, 5]}";
        Preset p = PresetFormat.parse(json);
        assertEquals(1, p.bundles().size());
        Bundle b = p.bundles().get(0);
        assertEquals("Ok", b.name());
        assertEquals(1, b.entries().size());
        assertEquals("", b.entries().get(0).icon());
        assertEquals(-1, b.bookmark(), "a bookmark past the end is dropped");
    }
}
