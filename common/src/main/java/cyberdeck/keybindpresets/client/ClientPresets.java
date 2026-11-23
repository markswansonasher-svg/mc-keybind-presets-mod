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
                    String n = s.freeName(defaultName());
                    s.save(KeyApplier.current(n, ""));
                    s.setActive(n);
                    toast(tr("keybindpresets.toast.keys_saved"), tr("keybindpresets.toast.keys_saved_msg", n));
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
            toastError("keybindpresets.toast.switch_failed", e);
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
            toast(tr("keybindpresets.toast.switched", name), tr("keybindpresets.toast.switched_keys", n));
        } catch (IOException e) {
            Constants.LOG.error("Couldn't add the server preset", e);
            toastError("keybindpresets.toast.server_failed", e);
        }
    }

    public static void toast(Component title, Component msg) {
        SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), SystemToast.SystemToastIds.TUTORIAL_HINT, title, msg);
    }

    /** A failure toast: the title in the player's language, the error as it came. */
    public static void toastError(String titleKey, Exception e) {
        toast(tr(titleKey), e.getMessage() == null ? null : Component.literal(e.getMessage()));
    }

    /** Text from the language file (assets/keybindpresets/lang/en_us.json and friends). */
    public static Component tr(String key, Object... args) {
        return Component.translatable(key, args);
    }

    /** Name for keys saved from Default: "My keys" (in the player's language), never a broken one. */
    public static String defaultName() {
        String n = PresetStore.clean(net.minecraft.client.resources.language.I18n.get("keybindpresets.save_as.default_name"));
        return n.isEmpty() || n.startsWith("keybindpresets.") ? "My keys" : n;
    }
}
