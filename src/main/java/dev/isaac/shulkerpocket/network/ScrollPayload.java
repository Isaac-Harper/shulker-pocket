package dev.isaac.shulkerpocket.network;

import dev.isaac.shulkerpocket.ShulkerPocket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >=1.21.11 {
import net.minecraft.resources.Identifier;
//?} else {
/*import net.minecraft.resources.ResourceLocation;*/
//?}

/**
 * One C2S payload for a scroll step. Carries the direction plus the two client-side preferences the
 * server needs to honour (config is client-only, so the relevant bits ride along with the action):
 *
 * @param direction     -1 (down) or +1 (up)
 * @param allowEmpty     whether the cursor may land on the "bare hands" stop
 * @param playSounds     whether the server should play feedback sounds for this player
 * @param requireSneak   whether the server should re-verify sneaking (true for sneak activation;
 *                       false when a client-only key triggered it, which the server can't observe)
 */
public record ScrollPayload(byte direction, boolean allowEmpty, boolean playSounds, boolean requireSneak)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScrollPayload> ID =
        //? if >=1.21.11 {
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(ShulkerPocket.MOD_ID, "scroll"));
        //?} elif >=1.21 {
        /*new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ShulkerPocket.MOD_ID, "scroll"));*/
        //?} else {
        /*new CustomPacketPayload.Type<>(new ResourceLocation(ShulkerPocket.MOD_ID, "scroll"));*/
        //?}

    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollPayload> CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BYTE, ScrollPayload::direction,
            ByteBufCodecs.BOOL, ScrollPayload::allowEmpty,
            ByteBufCodecs.BOOL, ScrollPayload::playSounds,
            ByteBufCodecs.BOOL, ScrollPayload::requireSneak,
            ScrollPayload::new
        );

    @Override
    public CustomPacketPayload.Type<ScrollPayload> type() {
        return ID;
    }
}
