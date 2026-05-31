package dev.isaac.shulkerpocket.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
        ItemTooltipCallback.EVENT.register(ShulkerPocketClient::appendHint);
    }

    /** Adds a discoverability hint to shulker-box tooltips, naming the current activation key. */
    private static void appendHint(net.minecraft.world.item.ItemStack stack,
                                   net.minecraft.world.item.Item.TooltipContext context,
                                   net.minecraft.world.item.TooltipFlag flag,
                                   java.util.List<Component> lines) {
        if (config == null || !config.showTooltipHint) return;
        if (!stack.has(DataComponents.CONTAINER)) return;
        Component activator = config.useActivationKey
            ? ACTIVATE_KEY.getTranslatedKeyMessage()
            : Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();
        lines.add(Component.translatable("shulker_pocket.tooltip.hint", activator)
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
