package cyberdeck.keybindpresets.server;

import cyberdeck.keybindpresets.Constants;
import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The server's preset: <server folder>/config/keypresets/server.json (the cyberdeck puts it there).
 * Sent to players (gzipped JSON) when they join. Plain Java, no Minecraft.
 */
public final class ServerPreset {
    public static final String CHANNEL = Constants.MOD_ID + ":server_preset";
    public static final int MAX_PACKET = 900 * 1024;           // under Minecraft's 1 MB custom-packet limit
    private final Path file;
    private long lastModified = -1;
    private byte[] cached;

    public ServerPreset(Path file) { this.file = file; }

    public static ServerPreset standard() { return new ServerPreset(Path.of("config", "keypresets", "server.json")); }

    /** Bytes to send to a joining player, or null if there's no (usable) preset. Re-read when the file changes. */
    public synchronized byte[] payload() {
        try {
            if (!Files.isRegularFile(file)) { cached = null; lastModified = -1; return null; }
            long m = Files.getLastModifiedTime(file).toMillis();
            if (m != lastModified) {
                lastModified = m;
                cached = null;
                if (Files.size(file) > PresetFormat.MAX_BYTES) {
                    Constants.LOG.warn("Keybind preset {} is over 512 KB - not sent", file);
                } else {
                    Preset p = PresetFormat.parse(Files.readString(file, StandardCharsets.UTF_8));
                    byte[] b = gzip(PresetFormat.write(p, false).getBytes(StandardCharsets.UTF_8));
                    if (b.length <= MAX_PACKET) cached = b;
                    else Constants.LOG.warn("Keybind preset {} is too big to send", file);
                    Constants.LOG.info("Keybind preset \"{}\" ({} keys) will be offered to players", p.name(), p.keys().size());
                }
            }
        } catch (IOException | PresetFormat.BadPresetException e) {
            Constants.LOG.warn("Keybind preset {} not usable: {}", file, e.getMessage());
            cached = null;
        }
        return cached;
    }

    /** What a client does with the bytes. */
    public static Preset read(byte[] payload) throws IOException, PresetFormat.BadPresetException {
        if (payload == null || payload.length > MAX_PACKET) throw new PresetFormat.BadPresetException("server preset too big");
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            byte[] json = in.readNBytes(PresetFormat.MAX_BYTES + 1);       // never unpack more than a preset may be
            if (json.length > PresetFormat.MAX_BYTES) throw new PresetFormat.BadPresetException("server preset too big");
            return PresetFormat.parse(new String(json, StandardCharsets.UTF_8));
        }
    }

    static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream z = new GZIPOutputStream(out)) { z.write(data); }
        return out.toByteArray();
    }
}
