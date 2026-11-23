package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Optional;

/** Switching presets without ever losing the keys you had. */
public final class ClientPresets {
    private static PresetStore store;

    private ClientPresets() {}

    public static PresetStore store() {
        if (store == null) store = new PresetStore(Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("keybindpresets"));
        return store;
    }

    /**
     * Before keys change: keep what you have now.
     * A preset is on -> it's updated with your current keys.
     * Default is on but you've changed keys -> they're saved as "My keys" (or "My keys 2"...).
     */
    public static void saveCurrent() {
        try {
            PresetStore s = store();
            String a = s.active();
            if (PresetStore.DEFAULT.equalsIgnoreCase(a)) {
                if (!KeyApplier.allDefault()) {
                    String n = s.freeName("My keys");
                    s.save(KeyApplier.current(n, ""));
                    s.setActive(n);
                    toast("Your keys were saved", "as the preset \"" + n + "\".");
                }
                return;
            }
            Optional<Preset> old = s.get(a);
            if (old.isPresent() && KeyApplier.matches(old.get())) return;
            s.save(KeyApplier.current(old.map(Preset::name).orElse(a), old.map(Preset::source).orElse("")));
        } catch (IOException e) {
            Constants.LOG.error("Couldn't save the current keys", e);
        }
    }

    public static void switchTo(String name) {
        saveCurrent();
        try {
            PresetStore s = store();
            if (PresetStore.DEFAULT.equalsIgnoreCase(name)) {
                KeyApplier.applyDefaults();
                s.setActive(PresetStore.DEFAULT);
            } else {
                Preset p = s.get(name).orElseThrow(() -> new IOException("no preset " + name));
                KeyApplier.apply(p);
                s.setActive(p.name());
            }
        } catch (IOException e) {
            Constants.LOG.error("Couldn't switch preset", e);
            toast("Couldn't switch preset", e.getMessage());
        }
    }

    /** Yes to a server's preset: add it (replacing that server's older one) and switch to it. */
    public static void addServerPreset(Preset p, String server) {
        saveCurrent();
        try {
            PresetStore s = store();
            String src = "server:" + server;
            String name = s.list().stream().filter(x -> src.equals(x.source())).map(Preset::name).findFirst()
                    .orElseGet(() -> s.freeName(p.name()));
            Preset mine = p.withName(name).withSource(src);
            s.save(mine);
            int n = KeyApplier.apply(mine);
            s.setActive(name);
            toast("Switched to \"" + name + "\"", n + " keys set. Switch back any time in Key Binds.");
        } catch (IOException e) {
            Constants.LOG.error("Couldn't add the server preset", e);
            toast("Couldn't add the server preset", e.getMessage());
        }
    }

    public static void toast(String title, String msg) {
        SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT,
                Component.literal(title), msg == null ? null : Component.literal(msg));
    }
}
