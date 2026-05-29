package dev.isaac.shulkerpocket;

import dev.isaac.shulkerpocket.network.ScrollPayload;
import dev.isaac.shulkerpocket.server.ScrollHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
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

        // Forget a player's cursor when they leave so the per-player map doesn't grow unbounded.
        ServerPlayConnectionEvents.DISCONNECT.register(
            (handler, server) -> ScrollHandler.forget(handler.player.getUUID()));

        LOGGER.info("Shulker Pocket initialized");
    }
}
