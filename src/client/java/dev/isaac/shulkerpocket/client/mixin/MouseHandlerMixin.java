package dev.isaac.shulkerpocket.client.mixin;

import dev.isaac.shulkerpocket.client.ScrollState;
import dev.isaac.shulkerpocket.client.ShulkerPocketClient;
import dev.isaac.shulkerpocket.network.ScrollPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Unique
    private final ScrollState shulker_pocket$state = new ScrollState();

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void shulker_pocket$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // a GUI is open → leave vanilla scroll alone
        Player player = mc.player;
        if (player == null) return;
        if (!player.isShiftKeyDown()) return;

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.has(DataComponents.CONTAINER)) return;

        long cooldown = ShulkerPocketClient.config.cooldownMs;
        if (!shulker_pocket$state.tryFire(cooldown)) {
            ci.cancel(); // within cooldown: still suppress the vanilla hotbar change
            return;
        }

        byte direction = (byte) (vertical > 0 ? 1 : -1);
        if (ShulkerPocketClient.config.invertScroll) direction = (byte) -direction;

        ClientPlayNetworking.send(new ScrollPayload(direction));
        ci.cancel(); // suppress the vanilla hotbar-slot change for this scroll event
    }
}
