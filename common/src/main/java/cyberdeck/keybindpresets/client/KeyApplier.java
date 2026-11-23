package cyberdeck.keybindpresets.client;

import com.mojang.blaze3d.platform.InputConstants;
import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.platform.ClientServices;
import cyberdeck.keybindpresets.preset.KeyEntry;
import cyberdeck.keybindpresets.preset.Preset;
import net.minecraft.SharedConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads the game's key bindings into a preset, and puts a preset's keys into the game. */
public final class KeyApplier {
    private KeyApplier() {}

    private static KeyMapping[] mappings() { return Minecraft.getInstance().options.keyMappings; }

    private static String mcVersion() { return SharedConstants.getCurrentVersion().getName(); }

    /** Every key the game has right now (all mods included), with labels and defaults. */
    public static Preset current(String name, String source) {
        List<KeyEntry> keys = new ArrayList<>();
        for (KeyMapping km : mappings()) {
            keys.add(new KeyEntry(km.getName(), km.saveString(), ClientServices.PLATFORM.modifier(km),
                    km.getDefaultKey().getName(), I18n.get(km.getName()), km.getCategory()));
        }
        return new Preset(name, mcVersion(), keys, source);
    }

    /** The mods' own keys ("Default"), for exporting. */
    public static Preset defaults() {
        List<KeyEntry> keys = new ArrayList<>();
        for (KeyMapping km : mappings()) {
            String d = km.getDefaultKey().getName();
            keys.add(new KeyEntry(km.getName(), d, "NONE", d, I18n.get(km.getName()), km.getCategory()));
        }
        return new Preset("Default", mcVersion(), keys, "");
    }

    public static boolean allDefault() {
        for (KeyMapping km : mappings()) if (!ClientServices.PLATFORM.isDefault(km)) return false;
        return true;
    }

    /** True if the game's keys already match this preset (nothing to save). */
    public static boolean matches(Preset p) {
        Map<String, KeyEntry> want = p.byId();
        for (KeyMapping km : mappings()) {
            KeyEntry e = want.get(km.getName());
            if (e != null && (!e.key().equals(km.saveString()) || !e.modifier().equals(ClientServices.PLATFORM.modifier(km)))) return false;
        }
        return true;
    }

    /**
     * Put a preset's keys into the game and save options.txt. Keys the preset doesn't list keep what they
     * have; keys from mods this game doesn't have are skipped. Returns how many keys were set.
     */
    public static int apply(Preset p) {
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
}
