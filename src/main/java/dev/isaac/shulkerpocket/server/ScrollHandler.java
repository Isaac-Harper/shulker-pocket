package dev.isaac.shulkerpocket.server;

import dev.isaac.shulkerpocket.network.ScrollPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Receives {@link ScrollPayload} and performs the authoritative inventory mutation. */
public final class ScrollHandler {
    private ScrollHandler() {}

    /** Per-player cursor: the shulker slot the held pocket item came from (its "home"). */
    private static final Map<UUID, Integer> HOME = new ConcurrentHashMap<>();

    public static void receive(ScrollPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();

        // Anti-cheat: re-verify sneaking on the server. Never trust the client gate alone.
        if (!player.isShiftKeyDown()) return;

        ItemStack offhand = player.getOffhandItem();
        ItemContainerContents contents = offhand.get(DataComponents.CONTAINER);
        if (contents == null) return; // off-hand isn't a container

        ItemStack mainHand = player.getMainHandItem();

        // Refuse if the held item is the off-hand container itself (would nest the pocket into itself).
        if (mainHand == offhand) return;

        UUID id = player.getUUID();
        int home = HOME.getOrDefault(id, ContainerOps.NO_HOME);
        ContainerOps.SwapResult result = ContainerOps.swap(contents, mainHand, home, payload.direction());
        if (result == null) return; // refused — TODO: send a deny-sound packet when config.playSounds

        offhand.set(DataComponents.CONTAINER, result.contents());
        player.setItemInHand(InteractionHand.MAIN_HAND, result.mainHand());
        HOME.put(id, result.homeSlot());
        player.inventoryMenu.broadcastChanges();
    }
}
