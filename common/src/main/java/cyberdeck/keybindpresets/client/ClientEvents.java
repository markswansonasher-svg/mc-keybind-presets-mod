package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetFormat;
import cyberdeck.keybindpresets.preset.PresetStore;
import cyberdeck.keybindpresets.server.ServerPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Loader-independent client hooks. Forge and Fabric call these from their own events. */
public final class ClientEvents {
    private static Screen lastScreen;
    private static Pending pending;

    private record Pending(Preset preset, String server, String hash, boolean update) {}

    private ClientEvents() {}

    /** A screen finished building: add the preset button to Key Binds. */
    public static void onScreenInit(Screen screen, Consumer<AbstractWidget> add) {
        if (!(screen instanceof KeyBindsScreen)) return;
        String active = PresetsScreen.displayName(ClientPresets.store().active());
        String label = active.length() > 14 ? active.substring(0, 13) + "…" : active;
        // Top left, 18 high: clear of the screen's title and of the search box Controlling puts at y=22.
        add.accept(Button.builder(ClientPresets.tr("keybindpresets.button.preset", label),
                        b -> Minecraft.getInstance().setScreen(new PresetsScreen(screen)))
                .bounds(4, 2, 130, 18)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(ClientPresets.tr("keybindpresets.button.preset.tooltip")))
                .build());
    }

    /** Every client tick (both loaders). */
    public static void onClientTick(Minecraft mc) {
        Screen now = mc.screen;
        // left Key Binds (not into our own presets screen): keep the changes in the active preset
        if (lastScreen instanceof KeyBindsScreen && !(now instanceof KeyBindsScreen) && !(now instanceof PresetsScreen)) {
            ClientPresets.saveCurrent();
        }
        lastScreen = now;
        if (mc.player == null || mc.level == null) {
            if (mc.getConnection() == null) pending = null;      // left the server before answering
            return;
        }
        if (pending != null && mc.screen == null) {
            Pending p = pending;
            pending = null;
            mc.setScreen(prompt(p));
        }
    }

    /** The server sent its preset (called on the main thread). */
    public static void onServerPreset(byte[] payload) {
        try {
            Preset p = ServerPreset.read(payload);
            String server = serverKey();
            String hash = PresetFormat.hash(p);
            String before = ClientPresets.store().answered(server);
            if (hash.equals(before)) return;                     // already answered this exact preset
            pending = new Pending(p, server, hash, !before.isEmpty());
        } catch (Exception e) {
            Constants.LOG.warn("Ignored a broken keybind preset from the server: {}", e.getMessage());
        }
    }

    private static String serverKey() {
        ServerData d = Minecraft.getInstance().getCurrentServer();
        return d != null && d.ip != null ? d.ip.toLowerCase(java.util.Locale.ROOT) : "this-world";
    }

    private static Screen prompt(Pending p) {
        Component title = ClientPresets.tr(p.update() ? "keybindpresets.prompt.title_changed" : "keybindpresets.prompt.title_new");
        Component msg = ClientPresets.tr("keybindpresets.prompt.message", p.preset().name(), p.preset().keys().size());
        return new ConfirmScreen(yes -> {
            try {
                ClientPresets.store().setAnswered(p.server(), p.hash());
            } catch (java.io.IOException e) {
                Constants.LOG.warn("Couldn't remember the answer: {}", e.getMessage());
            }
            if (yes) ClientPresets.addServerPreset(p.preset(), p.server());
            Minecraft.getInstance().setScreen(null);
        }, title, msg, ClientPresets.tr("keybindpresets.prompt.yes"), ClientPresets.tr("keybindpresets.prompt.no"));
    }

    /** Shown on our screen's preset list: is this the one that's on? */
    static boolean isActive(String name) {
        return ClientPresets.store().active().equalsIgnoreCase(name == null ? PresetStore.DEFAULT : name);
    }
}
