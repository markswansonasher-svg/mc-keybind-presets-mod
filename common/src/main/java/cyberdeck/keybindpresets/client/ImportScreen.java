package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Pick a preset file from config/keybindpresets/import/ (put files there with "Open import folder"). */
public class ImportScreen extends Screen {
    private static final int SHOW = 8;
    private final PresetsScreen parent;
    private List<Path> files = List.of();
    private int page = 0;

    public ImportScreen(PresetsScreen parent) {
        super(Component.literal("Import a preset"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        try {
            files = ClientPresets.store().importable();
        } catch (IOException e) {
            files = List.of();
        }
        int y = 44;
        for (int i = page * SHOW; i < Math.min(files.size(), (page + 1) * SHOW); i++) {
            Path f = files.get(i);
            String n = f.getFileName().toString();
            addRenderableWidget(Button.builder(Component.literal(n.length() > 40 ? n.substring(0, 39) + "…" : n), b -> importFile(f))
                    .bounds(width / 2 - 120, y, 240, 20).build());
            y += 22;
        }
        if (files.size() > SHOW) {
            addRenderableWidget(Button.builder(Component.literal("More…"), b -> { page = (page + 1) % ((files.size() + SHOW - 1) / SHOW); rebuildWidgets(); })
                    .bounds(width / 2 - 120, y, 240, 20).build());
        }
        int w = 118;
        addRenderableWidget(Button.builder(Component.literal("Open import folder"), b -> PresetsScreen.openFolder(ClientPresets.store().importDir()))
                .bounds(width / 2 - w - 2, height - 28, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose()).bounds(width / 2 + 2, height - 28, w, 20).build());
    }

    private void importFile(Path f) {
        try {
            Preset p = ClientPresets.store().importFile(f);
            ClientPresets.toast("Imported \"" + p.name() + "\"", p.keys().size() + " keys. Switch to it to use it.");
            onClose();
        } catch (PresetFormat.BadPresetException e) {
            ClientPresets.toast("Can't import that file", e.getMessage());
        } catch (IOException e) {
            ClientPresets.toast("Couldn't read that file", e.getMessage());
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal(files.isEmpty()
                ? "No preset files yet. Press \"Open import folder\", put a .json preset there, then come back."
                : "Pick a file (from config/keybindpresets/import):"), width / 2, 26, 0xA0A0A0);
        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public void onClose() {
        parent.refresh();
        minecraft.setScreen(parent);
    }
}
