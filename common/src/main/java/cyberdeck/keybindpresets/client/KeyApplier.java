package cyberdeck.keybindpresets.client;

import com.mojang.blaze3d.platform.InputConstants;
import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.platform.ClientServices;
import cyberdeck.keybindpresets.preset.Bundle;
import cyberdeck.keybindpresets.preset.KeyEntry;
import cyberdeck.keybindpresets.preset.Preset;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reads the game's key bindings into a preset, and puts a preset's keys into the game. */
public final class KeyApplier {
    private KeyApplier() {}

    private static KeyMapping[] mappings() { return Minecraft.getInstance().options.keyMappings; }

    private static String mcVersion() { return SharedConstants.getCurrentVersion().getName(); }

    /** The name players see for a key binding ("Jump", "Show Recipe", "Bundle: Maps"). */
    public static String label(KeyMapping km) {
        String bundle = BundleCompat.label(km);
        return bundle != null ? bundle : I18n.get(km.getName());
    }

    /** Every key the game has right now (all mods included), with labels and defaults, plus any key bundles. */
    public static Preset current(String name, String source) {
        List<KeyEntry> keys = new ArrayList<>();
        for (KeyMapping km : mappings()) {
            keys.add(new KeyEntry(km.getName(), km.saveString(), ClientServices.PLATFORM.modifier(km),
                    km.getDefaultKey().getName(), label(km), km.getCategory()));
        }
        return new Preset(name, mcVersion(), keys, source, BundleCompat.current());
    }

    /** The mods' own keys ("Default"), for exporting. */
    public static Preset defaults() {
        List<KeyEntry> keys = new ArrayList<>();
        for (KeyMapping km : mappings()) {
            String d = km.getDefaultKey().getName();
            keys.add(new KeyEntry(km.getName(), d, "NONE", d, label(km), km.getCategory()));
        }
        return new Preset("Default", mcVersion(), keys, "");
    }

    public static boolean allDefault() {
        for (KeyMapping km : mappings()) if (!ClientServices.PLATFORM.isDefault(km)) return false;
        return true;
    }

    /** True if the game's keys (and key bundles) already match this preset (nothing to save). */
    public static boolean matches(Preset p) {
        Map<String, KeyEntry> want = p.byId();
        for (KeyMapping km : mappings()) {
            KeyEntry e = want.get(km.getName());
            if (e != null && (!e.key().equals(km.saveString()) || !e.modifier().equals(ClientServices.PLATFORM.modifier(km)))) return false;
        }
        if (BundleCompat.present() && !BundleCompat.current().equals(p.bundles())) return false;
        return true;
    }

    /**
     * Put a preset's keys into the game and save options.txt. Key bundles it brings are added first (when
     * KeyBind Bundles is installed). Keys the preset doesn't list keep what they have; keys from mods this
     * game doesn't have are skipped. Returns how many keys were set.
     */
    public static int apply(Preset p) {
        BundleCompat.addMissing(p.bundles());
        Map<String, KeyEntry> want = p.byId();
        int n = 0;
        for (KeyMapping km : mappings()) {
            KeyEntry e = want.get(km.getName());
            if (e == null) continue;
            InputConstants.Key key;
            try {
                key = InputConstants.getKey(e.key());
            } catch (RuntimeException ex) {
                Constants.LOG.warn("Preset {}: unknown key {} for {}", p.name(), e.key(), e.id());
                continue;
            }
            ClientServices.PLATFORM.setKey(km, key, e.modifier());
            n++;
        }
        save();
        return n;
    }

    public static void applyDefaults() {
        for (KeyMapping km : mappings()) ClientServices.PLATFORM.resetToDefault(km);
        save();
    }

    private static void save() {
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
    }

    // ------------------------------------------------------------------ preview, for the Presets screen

    /** One key that switching would change. */
    public record Change(String label, Component from, Component to) {}

    /**
     * What switching to a preset would do.
     * changes: keys that would change; missing: keys for mods this game doesn't have; newBundles: key bundles it adds.
     */
    public record Preview(List<Change> changes, int missing, int newBundles) {}

    /** What switching to this preset (null = Default) would change, compared with the keys you have now. */
    public static Preview preview(Preset p) {
        List<Change> changes = new ArrayList<>();
        Set<String> inGame = new HashSet<>();
        Map<String, KeyEntry> want = p == null ? Map.of() : p.byId();
        for (KeyMapping km : mappings()) {
            inGame.add(km.getName());
            String toKey, toMod;
            if (p == null) {
                if (ClientServices.PLATFORM.isDefault(km)) continue;
                toKey = km.getDefaultKey().getName();
                toMod = "NONE";
            } else {
                KeyEntry e = want.get(km.getName());
                if (e == null) continue;
                toKey = e.key();
                toMod = e.modifier();
            }
            String fromMod = ClientServices.PLATFORM.modifier(km);
            if (toKey.equals(km.saveString()) && (toMod.equals(fromMod) || !ClientServices.PLATFORM.hasModifiers())) continue;
            changes.add(new Change(label(km), keyName(km.saveString(), fromMod), keyName(toKey, toMod)));
        }
        int missing = 0, newBundles = 0;
        if (p != null) {
            List<Bundle> adds = BundleCompat.missing(p.bundles());
            newBundles = adds.size();
            Set<String> comingWithBundles = new HashSet<>();
            for (Bundle b : adds) comingWithBundles.add(b.keyId());
            for (String id : want.keySet()) if (!inGame.contains(id) && !comingWithBundles.contains(id)) missing++;
        }
        return new Preview(changes, missing, newBundles);
    }

    /** "R", "Shift + R", "Not bound" - in the player's language. */
    public static Component keyName(String key, String modifier) {
        Component k;
        try {
            k = InputConstants.getKey(key).getDisplayName();
        } catch (RuntimeException e) {
            k = Component.literal(key);
        }
        if (!ClientServices.PLATFORM.hasModifiers() || modifier == null || "NONE".equals(modifier)
                || InputConstants.UNKNOWN.getName().equals(key)) return k;
        return Component.translatable("keybindpresets.modifier." + modifier.toLowerCase(java.util.Locale.ROOT), k);
    }
}
