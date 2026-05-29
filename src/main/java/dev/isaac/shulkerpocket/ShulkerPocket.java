package dev.isaac.shulkerpocket;

import dev.isaac.shulkerpocket.network.ScrollPayload;
import dev.isaac.shulkerpocket.server.ScrollHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint. Registers the C2S payload and the authoritative server receiver. */
public class ShulkerPocket implements ModInitializer {
    public static final String MOD_ID = "shulker_pocket";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(ScrollPayload.ID, ScrollPayload.CODEC);

        // Server is authoritative: it re-checks every gate before mutating inventory.
        ServerPlayNetworking.registerGlobalReceiver(ScrollPayload.ID, ScrollHandler::receive);

        LOGGER.info("Shulker Pocket initialized");
    }
}
