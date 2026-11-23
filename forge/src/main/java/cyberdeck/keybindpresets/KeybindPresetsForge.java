package cyberdeck.keybindpresets;

import cyberdeck.keybindpresets.server.ServerPreset;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Forge, both sides. The mod is optional: players without it can join, and it works on a server without it. */
@Mod(Constants.MOD_ID)
public class KeybindPresetsForge {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"), () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
    private static final ServerPreset PRESET = ServerPreset.standard();

    /** The server's preset, gzipped JSON (see ServerPreset). */
    public record PresetMessage(byte[] data) {}

    public KeybindPresetsForge() {
        CHANNEL.messageBuilder(PresetMessage.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((m, buf) -> buf.writeByteArray(m.data()))
                .decoder(buf -> new PresetMessage(buf.readByteArray(ServerPreset.MAX_PACKET)))
                .consumerMainThread((m, ctx) -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClient.onServerPreset(m.data())))
                .add();
        MinecraftForge.EVENT_BUS.addListener(this::onLogin);
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClient.init();
    }

    private void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!CHANNEL.isRemotePresent(sp.connection.connection)) return;      // player doesn't have the mod
        byte[] b = PRESET.payload();
        if (b != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new PresetMessage(b));
    }
}
