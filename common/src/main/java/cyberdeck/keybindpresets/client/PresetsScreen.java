package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetStore;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cyberdeck.keybindpresets.client.ClientPresets.tr;

/**
 * Options > Controls > Key Binds > "Preset" button.
 * Left: every preset. Right: the one you picked - what it is, what switching to it would change, and what you can do
 * with it. Bottom: things that aren't about one preset.
 */
public class PresetsScreen extends Screen {
    private static final int MARGIN = 10, GAP = 8, TOP = 34;
    private static final int PANEL = 0x90000000, TEXT = 0xFFFFFF, DIM = 0xA0A0A0, ON = 0x55FF55;
    private final Screen parent;
    private PresetList list;
    private ChangesList changes;
    private Button switchBtn, renameBtn, deleteBtn, exportBtn;
    private int bottom, leftW, rightX, rightW;

    public PresetsScreen(Screen parent) {
        super(tr("keybindpresets.title"));
        this.parent = parent;
    }

    /** "Default" in the player's language; any other preset by its own name. */
    public static String displayName(String name) {
        if (!PresetStore.DEFAULT.equalsIgnoreCase(name)) return name;
        String t = I18n.get("keybindpresets.default");
        return t.startsWith("keybindpresets.") ? PresetStore.DEFAULT : t;
    }

    @Override
    protected void init() {
        ClientPresets.saveCurrent();                     // the active preset is up to date before we show it
        bottom = height - 36;
        int avail = width - 2 * MARGIN - GAP;
        leftW = Math.max(120, Math.min(220, avail * 42 / 100));
        rightX = MARGIN + leftW + GAP;
        rightW = width - MARGIN - rightX;

        list = new PresetList(minecraft, leftW, height, TOP, bottom, 24);
        list.setLeftPos(MARGIN);
        addRenderableWidget(list);

        // right panel: the picked preset
        int bx = rightX + 4, bw = rightW - 8;
        switchBtn = addRenderableWidget(Button.builder(tr("keybindpresets.action.switch"), b -> switchSelected())
                .bounds(bx, TOP + 30, bw, 20).build());
        int third = (bw - 8) / 3;
        renameBtn = addRenderableWidget(Button.builder(tr("keybindpresets.action.rename"), b -> renameSelected())
                .bounds(bx, TOP + 54, third, 20).build());
        deleteBtn = addRenderableWidget(Button.builder(tr("keybindpresets.action.delete"), b -> deleteSelected())
                .bounds(bx + third + 4, TOP + 54, third, 20).build());
        exportBtn = addRenderableWidget(Button.builder(tr("keybindpresets.action.export"), b -> exportSelected())
                .bounds(bx + 2 * (third + 4), TOP + 54, bw - 2 * (third + 4), 20).build());
        changes = new ChangesList(minecraft, rightW, height, TOP + 80, bottom, 11);
        changes.setLeftPos(rightX);
        addRenderableWidget(changes);

        // bottom row: not about one preset
        int w = Math.min(100, (width - 2 * MARGIN - 12) / 4), x0 = width / 2 - (4 * w + 12) / 2, y = height - 28;
        addRenderableWidget(Button.builder(tr("keybindpresets.action.save_as"), b -> minecraft.setScreen(new NameScreen(this,
                tr("keybindpresets.save_as.title"), ClientPresets.store().freeName(ClientPresets.defaultName()), this::saveAs)))
                .bounds(x0, y, w, 20).build());
        addRenderableWidget(Button.builder(tr("keybindpresets.action.import"), b -> minecraft.setScreen(new ImportScreen(this)))
                .bounds(x0 + (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(tr("keybindpresets.action.open_folder"), b -> openFolder(ClientPresets.store().exportsDir()))
                .bounds(x0 + 2 * (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(x0 + 3 * (w + 4), y, w, 20).build());
        refresh();
    }

    void refresh() {
        String keep = list.getSelected() != null ? list.getSelected().name : ClientPresets.store().active();
        list.fill(keep);
        updateDetails();
    }

    /** The picked preset changed (or the presets did): buttons and the "what changes" list. */
    private void updateDetails() {
        PresetList.Entry e = list.getSelected();
        boolean any = e != null, isDefault = any && e.preset == null, on = any && ClientEvents.isActive(e.name);
        switchBtn.active = any && !on;
        renameBtn.active = any && !isDefault;
        deleteBtn.active = any && !isDefault;
        exportBtn.active = any;
        changes.show(e == null ? null : e, on);
    }

    private void switchSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null || ClientEvents.isActive(e.name)) return;
        ClientPresets.switchTo(e.name);
        ClientPresets.toast(tr("keybindpresets.toast.switched", displayName(e.name)), null);
        refresh();
    }

    private void saveAs(String name) {
        try {
            String n = ClientPresets.store().freeName(name);
            ClientPresets.store().save(KeyApplier.current(n, ""));
            ClientPresets.store().setActive(n);
            ClientPresets.toast(tr("keybindpresets.toast.saved", n), tr("keybindpresets.toast.saved_msg"));
        } catch (IOException e) {
            ClientPresets.toastError("keybindpresets.toast.save_failed", e);
        }
    }

    private void renameSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null || e.preset == null) return;
        String from = e.name;
        minecraft.setScreen(new NameScreen(this, tr("keybindpresets.rename.title", from), from, to -> {
            try {
                if (!to.equalsIgnoreCase(from)) ClientPresets.store().rename(from, ClientPresets.store().freeName(to));
            } catch (IOException ex) {
                ClientPresets.toastError("keybindpresets.toast.rename_failed", ex);
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
                    ClientPresets.toastError("keybindpresets.toast.delete_failed", ex);
                }
            }
            minecraft.setScreen(this);
        }, tr("keybindpresets.delete.title", e.name), tr(on ? "keybindpresets.delete.active" : "keybindpresets.delete.other")));
    }

    private void exportSelected() {
        PresetList.Entry e = list.getSelected();
        if (e == null) return;
        try {
            Preset p = e.preset != null ? e.preset : KeyApplier.defaults();
            // the one that's on: export exactly what you have now, every key the game knows
            if (ClientEvents.isActive(e.name) && e.preset != null) p = KeyApplier.current(e.name, "");
            Path f = ClientPresets.store().export(p);
            ClientPresets.toast(tr("keybindpresets.toast.exported", displayName(p.name())),
                    Component.literal("config/keybindpresets/exports/" + f.getFileName()));
            openFolder(f.getParent());
        } catch (IOException ex) {
            ClientPresets.toastError("keybindpresets.toast.export_failed", ex);
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
        g.fill(MARGIN, TOP, MARGIN + leftW, bottom, PANEL);
        g.fill(rightX, TOP, rightX + rightW, bottom, PANEL);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, TEXT);
        g.drawCenteredString(font, tr("keybindpresets.subtitle"), width / 2, 20, DIM);

        PresetList.Entry e = list.getSelected();
        int x = rightX + 6, maxW = rightW - 12;
        if (e == null) {
            g.drawString(font, tr("keybindpresets.details.pick"), x, TOP + 8, DIM);
            return;
        }
        boolean on = ClientEvents.isActive(e.name);
        String name = font.plainSubstrByWidth(displayName(e.name), maxW - (on ? font.width(" ●") : 0));
        g.drawString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), x, TOP + 6, on ? ON : TEXT);
        Component info = e.preset == null ? tr("keybindpresets.details.default")
                : e.preset.fromServer() ? tr("keybindpresets.details.server", e.preset.server())
                : tr("keybindpresets.details.mine");
        g.drawString(font, font.plainSubstrByWidth(info.getString(), maxW), x, TOP + 18, DIM);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ------------------------------------------------------------------ left: the presets
    class PresetList extends ObjectSelectionList<PresetList.Entry> {
        PresetList(Minecraft mc, int w, int h, int top, int bottom, int itemHeight) {
            super(mc, w, h, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void fill(String select) {
            clearEntries();
            addEntry(new Entry(null));
            List<Preset> all = ClientPresets.store().list();
            for (Preset p : all) addEntry(new Entry(p));
            for (Entry e : children()) if (e.name.equalsIgnoreCase(select)) setSelected(e);
            if (getSelected() != null) ensureVisible(getSelected());
        }

        @Override
        public void setSelected(Entry e) {
            super.setSelected(e);
            if (changes != null) updateDetails();
        }

        @Override
        public int getRowWidth() { return width - 12; }

        @Override
        protected int getScrollbarPosition() { return x1 - 6; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            final Preset preset;            // null = Default
            final String name;
            private long lastClick;

            Entry(Preset p) {
                preset = p;
                name = p == null ? PresetStore.DEFAULT : p.name();
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float pt) {
                boolean on = ClientEvents.isActive(name);
                String badge = on ? tr("keybindpresets.list.on").getString() : "";
                int badgeW = on ? font.width(badge) + 6 : 0;
                g.drawString(font, font.plainSubstrByWidth(displayName(name), w - 8 - badgeW), left + 3, top + 2, on ? ON : TEXT);
                if (on) {
                    int bx = left + w - 4 - badgeW;
                    g.fill(bx, top + 1, bx + badgeW, top + 11, 0xFF2E7D32);
                    g.drawString(font, badge, bx + 3, top + 2, TEXT, false);
                }
                Component info = preset == null ? tr("keybindpresets.list.default_info")
                        : preset.fromServer() ? tr("keybindpresets.list.keys_server", preset.keys().size(), preset.server())
                        : !preset.bundles().isEmpty() ? tr("keybindpresets.list.keys_bundles", preset.keys().size(), preset.bundles().size())
                        : tr("keybindpresets.list.keys", preset.keys().size());
                g.drawString(font, font.plainSubstrByWidth(info.getString(), w - 8), left + 3, top + 12, DIM);
            }

            @Override
            public boolean mouseClicked(double x, double y, int button) {
                long now = Util.getMillis();
                boolean twice = PresetList.this.getSelected() == this && now - lastClick < 300;
                lastClick = now;
                PresetList.this.setSelected(this);
                if (twice) switchSelected();                 // double-click switches
                return true;
            }

            @Override
            public Component getNarration() { return Component.literal(displayName(name)); }
        }
    }

    // ------------------------------------------------------------------ right: what switching would change
    class ChangesList extends ObjectSelectionList<ChangesList.Row> {
        ChangesList(Minecraft mc, int w, int h, int top, int bottom, int itemHeight) {
            super(mc, w, h, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void show(PresetList.Entry e, boolean on) {
            clearEntries();
            setScrollAmount(0);
            if (e == null) return;
            if (on) {
                addEntry(new Row(tr("keybindpresets.details.on"), ON));
                return;
            }
            KeyApplier.Preview p = KeyApplier.preview(e.preset);
            if (p.newBundles() > 0) addEntry(new Row(tr("keybindpresets.details.bundles", p.newBundles()), 0x55FFFF));
            if (p.missing() > 0) addEntry(new Row(tr("keybindpresets.details.missing", p.missing()), DIM));
            addEntry(new Row(p.changes().isEmpty() ? tr("keybindpresets.details.no_changes")
                    : tr("keybindpresets.details.changes", p.changes().size()), TEXT));
            for (KeyApplier.Change c : p.changes()) addEntry(new Row(c));
        }

        @Override
        public int getRowWidth() { return width - 12; }

        @Override
        protected int getScrollbarPosition() { return x1 - 6; }

        class Row extends ObjectSelectionList.Entry<Row> {
            private final Component text;
            private final int color;
            private final KeyApplier.Change change;

            Row(Component text, int color) {
                this.text = text;
                this.color = color;
                this.change = null;
            }

            Row(KeyApplier.Change c) {
                this.text = Component.literal(c.label());
                this.color = 0xE0E0E0;
                this.change = c;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float pt) {
                if (change == null) {
                    g.drawString(font, font.plainSubstrByWidth(text.getString(), w - 4), left + 2, top + 1, color);
                    return;
                }
                Component keys = Component.empty()
                        .append(change.from().copy().withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" → ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(change.to().copy().withStyle(ChatFormatting.YELLOW));
                int kw = font.width(keys);
                int kx = left + w - 2 - kw;
                int labelW = Math.max(20, kx - left - 8);
                String label = text.getString();
                if (font.width(label) > labelW) label = font.plainSubstrByWidth(label, labelW - font.width("…")) + "…";
                g.drawString(font, label, left + 6, top + 1, color);
                g.drawString(font, keys, Math.max(kx, left + 6 + labelW + 4), top + 1, 0xFFFFFF);
            }

            @Override
            public boolean mouseClicked(double x, double y, int button) { return false; }      // just a list to read

            @Override
            public Component getNarration() {
                return change == null ? text : Component.empty().append(text).append(": ").append(change.from())
                        .append(" → ").append(change.to());
            }
        }
    }
}
