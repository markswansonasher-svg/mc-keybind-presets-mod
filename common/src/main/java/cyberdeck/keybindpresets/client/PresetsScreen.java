package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetStore;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Options > Controls > Key Binds > "Preset" button: every preset, and what you can do with them. */
public class PresetsScreen extends Screen {
    private final Screen parent;
    private PresetList list;
    private Button switchBtn, renameBtn, deleteBtn, exportBtn;

    public PresetsScreen(Screen parent) {
        super(Component.literal("Keybind Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientPresets.saveCurrent();                     // the active preset is up to date before we show it
        list = new PresetList(minecraft, width, height, 32, height - 58, 20);
        addRenderableWidget(list);
        int w = 98, x0 = width / 2 - (4 * w + 3 * 4) / 2;
        int y1 = height - 54, y2 = height - 28;
        switchBtn = addRenderableWidget(Button.builder(Component.literal("Switch to"), b -> switchSelected()).bounds(x0, y1, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Save keys as…"), b -> minecraft.setScreen(new NameScreen(this,
                "Save your current keys as a new preset", ClientPresets.store().freeName("My keys"), this::saveAs))).bounds(x0 + (w + 4), y1, w, 20).build());
        renameBtn = addRenderableWidget(Button.builder(Component.literal("Rename…"), b -> renameSelected()).bounds(x0 + 2 * (w + 4), y1, w, 20).build());
        deleteBtn = addRenderableWidget(Button.builder(Component.literal("Delete"), b -> deleteSelected()).bounds(x0 + 3 * (w + 4), y1, w, 20).build());
        exportBtn = addRenderableWidget(Button.builder(Component.literal("Export"), b -> exportSelected()).bounds(x0, y2, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Import…"), b -> minecraft.setScreen(new ImportScreen(this))).bounds(x0 + (w + 4), y2, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Open folder"), b -> openFolder(ClientPresets.store().exportsDir())).bounds(x0 + 2 * (w + 4), y2, w, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose()).bounds(x0 + 3 * (w + 4), y2, w, 20).build());
        refresh();
    }

    void refresh() {
        String keep = list.getSelected() != null ? list.getSelected().name : ClientPresets.store().active();
        list.fill(keep);
        updateButtons();
    }

    private void updateButtons() {
        PresetList.Entry e = list.getSelected();
        boolean any = e != null, isDefault = any && e.preset == null;
        switchBtn.active = any && !ClientEvents.isActive(e.name);
        renameBtn.active = any && !isDefault;
        deleteBtn.active = any && !isDefault;
        exportBtn.active = any;
    }

    private void switchSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null) return;
        ClientPresets.switchTo(e.name);
        ClientPresets.toast("Switched to \"" + e.name + "\"", null);
        refresh();
    }

    private void saveAs(String name) {
        try {
            String n = ClientPresets.store().freeName(name);
            ClientPresets.store().save(KeyApplier.current(n, ""));
            ClientPresets.store().setActive(n);
            ClientPresets.toast("Saved \"" + n + "\"", "It's the active preset now.");
        } catch (IOException e) {
            ClientPresets.toast("Couldn't save the preset", e.getMessage());
        }
    }

    private void renameSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null || e.preset == null) return;
        String from = e.name;
        minecraft.setScreen(new NameScreen(this, "Rename \"" + from + "\"", from, to -> {
            try {
                if (!to.equalsIgnoreCase(from)) ClientPresets.store().rename(from, ClientPresets.store().freeName(to));
            } catch (IOException ex) {
                ClientPresets.toast("Couldn't rename it", ex.getMessage());
            }
        }));
    }

    private void deleteSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null || e.preset == null) return;
        boolean on = ClientEvents.isActive(e.name);
        minecraft.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                try {
                    ClientPresets.store().delete(e.name);
                } catch (IOException ex) {
                    ClientPresets.toast("Couldn't delete it", ex.getMessage());
                }
            }
            minecraft.setScreen(this);
        }, Component.literal("Delete \"" + e.name + "\"?"),
                Component.literal(on ? "It's the preset that's on. Your keys stay as they are; \"Default\" becomes the active preset."
                        : "This can't be undone. Export it first if you might want it back.")));
    }

    private void exportSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null) return;
        try {
            Preset p = e.preset != null ? e.preset : KeyApplier.defaults();
            // the one that's on: export exactly what you have now, every key the game knows
            if (ClientEvents.isActive(e.name) && e.preset != null) p = KeyApplier.current(e.name, "");
            Path f = ClientPresets.store().export(p);
            ClientPresets.toast("Exported \"" + p.name() + "\"", "config/keybindpresets/exports/" + f.getFileName());
            openFolder(f.getParent());
        } catch (IOException ex) {
            ClientPresets.toast("Couldn't export it", ex.getMessage());
        }
    }

    static void openFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            Util.getPlatform().openFile(dir.toFile());
        } catch (IOException e) {
            Constants.LOG.warn("Couldn't open {}", dir);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        g.drawCenteredString(font, Component.literal("Keys you change in Key Binds are saved into the preset that's on."), width / 2, 20, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ------------------------------------------------------------------ list
    class PresetList extends ObjectSelectionList<PresetList.Entry> {
        PresetList(Minecraft mc, int w, int h, int top, int bottom, int itemHeight) {
            super(mc, w, h, top, bottom, itemHeight);
        }

        void fill(String select) {
            clearEntries();
            addEntry(new Entry(null));
            List<Preset> all = ClientPresets.store().list();
            for (Preset p : all) addEntry(new Entry(p));
            for (Entry e : children()) if (e.name.equalsIgnoreCase(select)) setSelected(e);
        }

        @Override
        public void setSelected(Entry e) {
            super.setSelected(e);
            if (switchBtn != null) updateButtons();
        }

        @Override
        public int getRowWidth() { return Math.min(340, width - 20); }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            final Preset preset;            // null = Default
            final String name;

            Entry(Preset p) {
                preset = p;
                name = p == null ? PresetStore.DEFAULT : p.name();
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float pt) {
                boolean on = ClientEvents.isActive(name);
                g.drawString(font, (on ? "● " : "   ") + name, left + 4, top + 5, on ? 0x55FF55 : 0xFFFFFF);
                String info = preset == null ? "the mods' own keys"
                        : (preset.fromServer() ? "from " + preset.source().substring(7) + " · " : "") + preset.keys().size() + " keys";
                g.drawString(font, info, left + w - 6 - font.width(info), top + 5, 0xA0A0A0);
            }

            @Override
            public boolean mouseClicked(double x, double y, int button) {
                PresetList.this.setSelected(this);
                return true;
            }

            @Override
            public Component getNarration() { return Component.literal(name); }
        }
    }
}
