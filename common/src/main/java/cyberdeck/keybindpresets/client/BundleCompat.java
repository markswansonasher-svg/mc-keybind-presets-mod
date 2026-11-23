package cyberdeck.keybindpresets.client;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.platform.Services;
import cyberdeck.keybindpresets.preset.Bundle;
import cyberdeck.keybindpresets.preset.PresetFormat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Works with the KeyBind Bundles mod (matyrobbrt, Forge) when it's installed, without needing it:
 * - its bundles are saved into presets with your keys, and
 * - switching to a preset adds the bundles it brings that you don't have yet (yours are never changed or removed).
 * Everything goes through reflection and the mod's own file, so nothing breaks when it isn't there.
 */
public final class BundleCompat {
    public static final String MOD_ID = "keybindbundles";
    private static final String PKG = "com.matyrobbrt.keybindbundles.";
    private static Method nameOverride;
    private static boolean nameOverrideLooked;

    private BundleCompat() {}

    public static boolean present() {
        try {
            return Services.PLATFORM.isModLoaded(MOD_ID);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The mod keeps its bundles in <game folder>/keybind_bundles.json and rewrites it on every change. */
    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("keybind_bundles.json");
    }

    /** The bundles you have now (empty when the mod isn't installed). */
    public static List<Bundle> current() {
        if (!present()) return List.of();
        try {
            Path f = file();
            if (!Files.isRegularFile(f) || Files.size(f) > PresetFormat.MAX_BYTES) return List.of();
            return PresetFormat.parseBundles(JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)));
        } catch (Exception e) {
            Constants.LOG.warn("Couldn't read KeyBind Bundles' bundles: {}", e.getMessage());
            return List.of();
        }
    }

    /** Bundles in the list that you don't have yet. */
    public static List<Bundle> missing(List<Bundle> want) {
        if (want.isEmpty() || !present()) return List.of();
        Set<String> have = new HashSet<>();
        for (Bundle b : current()) have.add(b.id());
        return want.stream().filter(b -> !have.contains(b.id())).toList();
    }

    /** Add the bundles you don't have yet (so their keys exist before the preset's keys are set). Returns how many. */
    public static int addMissing(List<Bundle> want) {
        List<Bundle> add = missing(want);
        if (add.isEmpty()) return 0;
        int n = 0;
        try {
            Class<?> bundleClass = Class.forName(PKG + "KeyBindBundle");
            Codec<?> codec = (Codec<?>) bundleClass.getField("CODEC").get(null);
            Method addMethod = Class.forName(PKG + "KeyBindBundleManager").getMethod("add", bundleClass);
            for (Bundle b : add) {
                Object made = codec.parse(JsonOps.INSTANCE, PresetFormat.bundleJson(b, true)).result().orElse(null);
                if (made == null) {
                    Constants.LOG.warn("KeyBind Bundles didn't accept the bundle \"{}\"", b.name());
                    continue;
                }
                addMethod.invoke(null, made);
                n++;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Constants.LOG.warn("Couldn't add key bundles (is KeyBind Bundles a version this mod knows?): {}", e.toString());
        }
        return n;
    }

    /**
     * A bundle's key has no translation; the mod names it itself ("Bundle: Maps"). Returns that name, or null
     * for every other key.
     */
    public static String label(KeyMapping km) {
        if (!km.getName().startsWith(Bundle.KEY_PREFIX)) return null;
        try {
            if (!nameOverrideLooked) {
                nameOverrideLooked = true;
                nameOverride = KeyMapping.class.getMethod("kbb$getNameOverride");
            }
            if (nameOverride != null && nameOverride.invoke(km) instanceof Component c) return c.getString();
        } catch (ReflectiveOperationException | RuntimeException e) {
            nameOverride = null;
        }
        return null;
    }
}
