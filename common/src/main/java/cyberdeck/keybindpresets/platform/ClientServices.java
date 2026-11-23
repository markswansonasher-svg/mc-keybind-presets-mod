package cyberdeck.keybindpresets.platform;

import cyberdeck.keybindpresets.platform.services.IClientPlatform;

/** Client-only services. Kept apart from Services so a dedicated server never loads client classes. */
public final class ClientServices {
    public static final IClientPlatform PLATFORM = Services.load(IClientPlatform.class);

    private ClientServices() {}
}
