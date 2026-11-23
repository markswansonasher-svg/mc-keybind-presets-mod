package cyberdeck.keybindpresets;

import cyberdeck.keybindpresets.server.ServerPreset;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Fabric, both sides: when a player who has this mod joins, send them the server's preset (if there is one). */
public class KeybindPresetsFabric implements ModInitializer {
    public static final ResourceLocation CHANNEL = new ResourceLocation(ServerPreset.CHANNEL);
    private static final ServerPreset PRESET = ServerPreset.standard();

    @Override
    public void onInitialize() {
        // fires when the joining client says which channels it listens on - so only players with the mod get it
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> {
            if (!channels.contains(CHANNEL)) return;
            byte[] b = PRESET.payload();
            if (b == null) return;
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeByteArray(b);
            ServerPlayNetworking.send(handler.player, CHANNEL, buf);
        });
    }
}
