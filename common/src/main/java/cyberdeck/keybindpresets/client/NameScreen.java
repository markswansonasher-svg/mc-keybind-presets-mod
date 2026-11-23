package cyberdeck.keybindpresets.client;

import cyberdeck.keybindpresets.preset.PresetStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** Type a preset name. */
public class NameScreen extends Screen {
    private final PresetsScreen parent;
    private final String start;
    private final Consumer<String> done;
    private EditBox box;
    private Button ok;

    public NameScreen(PresetsScreen parent, String title, String start, Consumer<String> done) {
        super(Component.literal(title));
        this.parent = parent;
        this.start = start;
        this.done = done;
    }

    @Override
    protected void init() {
        box = new EditBox(font, width / 2 - 100, height / 2 - 20, 200, 20, Component.literal("Name"));
        box.setMaxLength(60);
        box.setValue(start);
        box.setResponder(s -> ok.active = !PresetStore.clean(s).isEmpty() && !PresetStore.DEFAULT.equalsIgnoreCase(PresetStore.clean(s)));
        addRenderableWidget(box);
        ok = addRenderableWidget(Button.builder(Component.literal("OK"), b -> finish()).bounds(width / 2 - 100, height / 2 + 10, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> onClose()).bounds(width / 2 + 2, height / 2 + 10, 98, 20).build());
        setInitialFocus(box);
    }

    private void finish() {
        String n = PresetStore.clean(box.getValue());
        if (n.isEmpty() || PresetStore.DEFAULT.equalsIgnoreCase(n)) return;
        done.accept(n);
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) { finish(); return true; }      // Enter / keypad Enter
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, height / 2 - 40, 0xFFFFFF);
        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}
