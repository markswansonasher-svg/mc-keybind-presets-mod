package cyberdeck.keybindpresets.platform.services;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

/** The bits of key handling that differ between Forge (Shift/Ctrl/Alt modifiers) and Fabric (none). Client only. */
public interface IClientPlatform {

    /** "NONE", "SHIFT", "CONTROL" or "ALT". */
    String modifier(KeyMapping km);

    void setKey(KeyMapping km, InputConstants.Key key, String modifier);

    void resetToDefault(KeyMapping km);

    boolean isDefault(KeyMapping km);

    /** Whether keys can need Shift/Ctrl/Alt held too (Forge yes, Fabric no). */
    boolean hasModifiers();
}
