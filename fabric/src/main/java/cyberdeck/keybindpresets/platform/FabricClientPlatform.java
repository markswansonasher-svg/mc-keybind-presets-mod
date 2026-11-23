package cyberdeck.keybindpresets.platform;

import com.mojang.blaze3d.platform.InputConstants;
import cyberdeck.keybindpresets.platform.services.IClientPlatform;
import net.minecraft.client.KeyMapping;

/** Fabric has no Shift/Ctrl/Alt key modifiers: those are ignored. */
public class FabricClientPlatform implements IClientPlatform {

    @Override
    public String modifier(KeyMapping km) { return "NONE"; }

    @Override
    public void setKey(KeyMapping km, InputConstants.Key key, String modifier) { km.setKey(key); }

    @Override
    public void resetToDefault(KeyMapping km) { km.setKey(km.getDefaultKey()); }

    @Override
    public boolean isDefault(KeyMapping km) { return km.isDefault(); }
}
