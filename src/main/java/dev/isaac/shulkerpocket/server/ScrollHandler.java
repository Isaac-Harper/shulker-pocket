package dev.isaac.shulkerpocket.server;

import dev.isaac.shulkerpocket.network.ScrollPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

    /** Drop a player's cursor when they disconnect (see {@code ShulkerPocket#onInitialize}). */
    public static void forget(UUID id) {
        HOME.remove(id);
    }

    public static void receive(ScrollPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();

        // Anti-cheat: re-verify sneaking on the server for the default (sneak) activation. A custom
        // activation key is client-only and not synced, so the server can't observe it, so in that
        // mode the client clears requireSneak and we trust the gated packet (the swap only
        // rearranges the player's own inventory, so there's nothing to exploit).
        if (payload.requireSneak() && !player.isShiftKeyDown()) return;

        ItemStack offhand = player.getOffhandItem();
        ItemContainerContents contents = offhand.get(DataComponents.CONTAINER);
        if (contents == null) return; // off-hand isn't a container

        ItemStack mainHand = player.getMainHandItem();

        // Refuse if the held item is the off-hand container itself (would nest the pocket into itself).
        if (mainHand == offhand) return;

        UUID id = player.getUUID();
        int home = HOME.getOrDefault(id, ContainerOps.NO_HOME);
        ContainerOps.SwapResult result =
            ContainerOps.swap(contents, mainHand, home, payload.direction(), payload.allowEmpty());
        if (result == null) {
            if (payload.playSounds()) playSound(player, SoundEvents.DISPENSER_FAIL, 0.25f, 1.0f);
            return; // refused, nothing mutated
        }

        offhand.set(DataComponents.CONTAINER, result.contents());
        player.setItemInHand(InteractionHand.MAIN_HAND, result.mainHand());
        HOME.put(id, result.homeSlot());
        player.inventoryMenu.broadcastChanges();

        if (payload.playSounds()) {
            // Taking an item out vs. parking it back: borrow the bundle insert/remove blips.
            boolean tookItem = !result.mainHand().isEmpty();
            playSound(player, tookItem ? SoundEvents.BUNDLE_REMOVE_ONE : SoundEvents.BUNDLE_INSERT,
                0.3f, tookItem ? 1.4f : 1.2f);
        }
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
            sound, SoundSource.PLAYERS, volume, pitch);
    }
}
