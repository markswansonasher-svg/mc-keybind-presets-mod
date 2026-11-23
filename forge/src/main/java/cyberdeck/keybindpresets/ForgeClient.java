package cyberdeck.keybindpresets;

import cyberdeck.keybindpresets.client.ClientEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Forge, player's game only (never loaded on a dedicated server). */
public final class ForgeClient {
    private ForgeClient() {}

    static void init() {
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post e) -> ClientEvents.onScreenInit(e.getScreen(), e::addListener));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) ClientEvents.onClientTick(Minecraft.getInstance());
        });
    }

    static void onServerPreset(byte[] data) {
        ClientEvents.onServerPreset(data);
    }
}
