package dev.isaac.shulkerpocket.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Client entrypoint. Loads config; scroll detection lives in {@code MouseHandlerMixin}. */
public class ShulkerPocketClient implements ClientModInitializer {
    public static ClientConfig config;

    /**
     * Optional alternative to sneak for activating the scroll (see {@link ClientConfig#useActivationKey}).
     * Rebindable in vanilla Options → Controls; defaults to Left Alt.
     */
    public static final KeyMapping ACTIVATE_KEY = new KeyMapping(
        "key.shulker_pocket.activate",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT,
        KeyMapping.Category.INVENTORY);

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load();
        KeyMappingHelper.registerKeyMapping(ACTIVATE_KEY);
    }
}
