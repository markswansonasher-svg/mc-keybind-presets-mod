package cyberdeck.keybindpresets;

import cyberdeck.keybindpresets.client.ClientEvents;
import cyberdeck.keybindpresets.server.ServerPreset;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

/** Fabric, player's game: the Key Binds button, the join prompt, receiving the server's preset. */
public class KeybindPresetsFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(KeybindPresetsFabric.CHANNEL, (client, handler, buf, sender) -> {
            byte[] b = buf.readByteArray(ServerPreset.MAX_PACKET);
            client.execute(() -> ClientEvents.onServerPreset(b));
        });
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                ClientEvents.onScreenInit(screen, widget -> Screens.getButtons(screen).add(widget)));
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
    }
}
