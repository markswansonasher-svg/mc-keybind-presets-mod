package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetFormat;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static cyberdeck.keybindpresets.client.ClientPresets.tr;

/** Pick a preset file from config/keybindpresets/import/ (put files there with "Open import folder"). */
public class ImportScreen extends Screen {
    private static final int TOP = 34, TEXT = 0xFFFFFF, DIM = 0xA0A0A0, BAD = 0xFF6060;
    private final PresetsScreen parent;
    private FileList list;
    private Button importBtn;
    private int listW, listX, bottom;

    public ImportScreen(PresetsScreen parent) {
        super(tr("keybindpresets.import.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        bottom = height - 36;
        listW = Math.min(360, width - 20);
        listX = width / 2 - listW / 2;
        list = new FileList(minecraft, listW, height, TOP, bottom, 24);
        list.setLeftPos(listX);
        addRenderableWidget(list);
        int w = Math.min(100, (width - 32) / 4), x0 = width / 2 - (4 * w + 12) / 2, y = height - 28;
        importBtn = addRenderableWidget(Button.builder(tr("keybindpresets.import.action"), b -> importSelected())
                .bounds(x0, y, w, 20).build());
        addRenderableWidget(Button.builder(tr("keybindpresets.import.refresh"), b -> list.scan())
                .bounds(x0 + (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(tr("keybindpresets.import.open"), b -> PresetsScreen.openFolder(ClientPresets.store().importDir()))
                .bounds(x0 + 2 * (w + 4), y, w, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> onClose()).bounds(x0 + 3 * (w + 4), y, w, 20).build());
        list.scan();
    }

    private void updateButtons() {
        if (importBtn != null) importBtn.active = list.getSelected() != null && list.getSelected().preset != null;
    }

    private void importSelected() {
        FileList.Entry e = list.getSelected();
        if (e == null || e.preset == null) return;
        try {
            Preset p = ClientPresets.store().importFile(e.file);
            ClientPresets.toast(tr("keybindpresets.toast.imported", p.name()), tr("keybindpresets.toast.imported_msg", p.keys().size()));
            onClose();
        } catch (PresetFormat.BadPresetException ex) {
            ClientPresets.toastError("keybindpresets.toast.import_bad", ex);
        } catch (IOException ex) {
            ClientPresets.toastError("keybindpresets.toast.import_failed", ex);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.fill(listX, TOP, listX + listW, bottom, 0x90000000);
        super.render(g, mouseX, mouseY, pt);
        g.drawCenteredString(font, title, width / 2, 8, TEXT);
        g.drawCenteredString(font, tr("keybindpresets.import.pick"), width / 2, 20, DIM);
        if (list.children().isEmpty()) {
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(tr("keybindpresets.import.empty"), listW - 20);
            int y = TOP + 12;
            for (var line : lines) {
                g.drawCenteredString(font, line, width / 2, y, DIM);
                y += 11;
            }
        }
    }

    @Override
    public void onClose() {
        parent.refresh();
        minecraft.setScreen(parent);
    }

    class FileList extends ObjectSelectionList<FileList.Entry> {
        FileList(Minecraft mc, int w, int h, int top, int bottom, int itemHeight) {
            super(mc, w, h, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        /** Look at what's in the import folder again. */
        void scan() {
            Path keep = getSelected() != null ? getSelected().file : null;
            clearEntries();
            setScrollAmount(0);
            List<Path> files;
            try {
                files = ClientPresets.store().importable();
            } catch (IOException e) {
                files = List.of();
            }
            for (Path f : files) addEntry(new Entry(f));
            for (Entry e : children()) if (e.file.equals(keep)) setSelected(e);
            if (getSelected() == null) for (Entry e : children()) if (e.preset != null) { setSelected(e); break; }
            updateButtons();
        }

        @Override
        public void setSelected(Entry e) {
            super.setSelected(e);
            updateButtons();
        }

        @Override
        public int getRowWidth() { return width - 12; }

        @Override
        protected int getScrollbarPosition() { return x1 - 6; }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            final Path file;
            final Preset preset;          // null if it can't be imported
            final String problem;
            private long lastClick;

            Entry(Path f) {
                file = f;
                Preset p = null;
                String why = "";
                try {
                    if (Files.size(f) > PresetFormat.MAX_BYTES) throw new PresetFormat.BadPresetException("That preset file is over 512 KB.");
                    p = PresetFormat.parse(Files.readString(f, StandardCharsets.UTF_8));
                } catch (PresetFormat.BadPresetException | IOException | RuntimeException e) {
                    why = String.valueOf(e.getMessage());
                }
                preset = p;
                problem = why;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int w, int h, int mouseX, int mouseY, boolean hovered, float pt) {
                g.drawString(font, font.plainSubstrByWidth(file.getFileName().toString(), w - 8), left + 3, top + 2, preset != null ? TEXT : DIM);
                Component info = preset != null ? tr("keybindpresets.import.row", preset.name(), preset.keys().size())
                        : tr("keybindpresets.import.row_bad", problem);
                g.drawString(font, font.plainSubstrByWidth(info.getString(), w - 8), left + 3, top + 12, preset != null ? DIM : BAD);
            }

            @Override
            public boolean mouseClicked(double x, double y, int button) {
                long now = Util.getMillis();
                boolean twice = FileList.this.getSelected() == this && now - lastClick < 300;
                lastClick = now;
                FileList.this.setSelected(this);
                if (twice) importSelected();                 // double-click imports
                return true;
            }

            @Override
            public Component getNarration() { return Component.literal(file.getFileName().toString()); }
        }
    }
}
