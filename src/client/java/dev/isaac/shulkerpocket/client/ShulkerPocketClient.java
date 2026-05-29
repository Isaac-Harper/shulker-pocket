package dev.isaac.shulkerpocket.client;

import net.fabricmc.api.ClientModInitializer;

/** Client entrypoint. Loads config; scroll detection lives in {@code MouseHandlerMixin}. */
public class ShulkerPocketClient implements ClientModInitializer {
    public static ClientConfig config;

    @Override
    public void onInitializeClient() {
        config = ClientConfig.load();
    }
}
