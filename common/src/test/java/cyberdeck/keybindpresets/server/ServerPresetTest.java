package cyberdeck.keybindpresets.server;

import cyberdeck.keybindpresets.preset.Preset;
import cyberdeck.keybindpresets.preset.PresetFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.*;

class ServerPresetTest {
    @TempDir Path dir;

    @Test
    void noFileMeansNothingToSend() {
        assertNull(new ServerPreset(dir.resolve("server.json")).payload());
    }

    @Test
    void sendsWhatTheDeckPutThere() throws Exception {
        Path f = dir.resolve("server.json");
        Files.copy(Path.of(System.getProperty("deckSample")), f);
        ServerPreset sp = new ServerPreset(f);
        byte[] b = sp.payload();
        assertNotNull(b);
        assertTrue(b.length < Files.size(f), "sent gzipped");
        Preset p = ServerPreset.read(b);
        assertEquals("Cozy Pack test keys", p.name());
        assertEquals(24, p.keys().size());
    }

    @Test
    void picksUpAChangedFile() throws Exception {
        Path f = dir.resolve("server.json");
        Files.writeString(f, "{\"format\":\"cyberdeck-keys/1\",\"name\":\"One\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"key.keyboard.space\"}]}");
        ServerPreset sp = new ServerPreset(f);
        assertEquals("One", ServerPreset.read(sp.payload()).name());
        Files.writeString(f, "{\"format\":\"cyberdeck-keys/1\",\"name\":\"Two\",\"keys\":[{\"id\":\"key.jump\",\"key\":\"key.keyboard.space\"}]}");
        Files.setLastModifiedTime(f, FileTime.fromMillis(System.currentTimeMillis() + 5000));
        assertEquals("Two", ServerPreset.read(sp.payload()).name());
        Files.writeString(f, "broken");
        Files.setLastModifiedTime(f, FileTime.fromMillis(System.currentTimeMillis() + 10000));
        assertNull(sp.payload(), "a broken file is never sent");
    }

    @Test
    void clientRefusesJunkAndZipBombs() throws Exception {
        assertThrows(Exception.class, () -> ServerPreset.read(new byte[]{1, 2, 3}));
        byte[] bomb = ServerPreset.gzip(new byte[PresetFormat.MAX_BYTES * 4]);     // tiny packet, huge when unpacked
        assertTrue(bomb.length < 10_000);
        assertThrows(PresetFormat.BadPresetException.class, () -> ServerPreset.read(bomb));
        assertThrows(PresetFormat.BadPresetException.class, () -> ServerPreset.read(new byte[ServerPreset.MAX_PACKET + 1]));
    }
}
