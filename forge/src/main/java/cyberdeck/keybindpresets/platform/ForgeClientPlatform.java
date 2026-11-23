package cyberdeck.keybindpresets.platform;

import com.mojang.blaze3d.platform.InputConstants;
import cyberdeck.keybindpresets.platform.services.IClientPlatform;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyModifier;

/** Forge keys can need Shift/Ctrl/Alt held as well. */
public class ForgeClientPlatform implements IClientPlatform {

    @Override
    public String modifier(KeyMapping km) { return km.getKeyModifier().name(); }

    @Override
    public void setKey(KeyMapping km, InputConstants.Key key, String modifier) {
        KeyModifier m;
        try {
            m = KeyModifier.valueOf(modifier);
        } catch (IllegalArgumentException e) {
            m = KeyModifier.NONE;
        }
        km.setKeyModifierAndCode(m, key);
    }

    @Override
    public void resetToDefault(KeyMapping km) { km.setToDefault(); }

    @Override
    public boolean isDefault(KeyMapping km) { return km.isDefault(); }
}
