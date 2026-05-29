package dev.isaac.shulkerpocket.network;

import dev.isaac.shulkerpocket.ShulkerPocket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** One C2S payload carrying scroll direction: -1 (down) or +1 (up). */
public record ScrollPayload(byte direction) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScrollPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ShulkerPocket.MOD_ID, "scroll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BYTE, ScrollPayload::direction,
            ScrollPayload::new
        );

    @Override
    public CustomPacketPayload.Type<ScrollPayload> type() {
        return ID;
    }
}
