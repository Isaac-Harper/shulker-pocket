package dev.isaac.shulkerpocket.gametest;

import dev.isaac.shulkerpocket.network.ScrollPayload;
import dev.isaac.shulkerpocket.server.ScrollHandler;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * In-world integration test for the scroll swap. Unlike {@code ContainerOpsTest} (which exercises the
 * pure algorithm), this boots a real dedicated-server world, gives a real {@link ServerPlayer} an
 * off-hand shulker box, and drives the swap through the actual {@link ScrollHandler#receive} path,
 * so it covers the server-authoritative pieces the unit test can't reach: the sneak re-check, reading
 * and writing the {@code CONTAINER} data component on a live held item, {@code setItemInHand}, and the
 * per-player home-slot cursor persisting across steps.
 */
public class ScrollGameTest {

    private static final int SLOTS = 27;

    /**
     * Sneak + scroll cycles the whole shulker: three items come out one at a time, then the "bare
     * hands" stop, and every item is returned to the box with none lost or duplicated.
     */
    @GameTest
    public void sneakScrollCyclesShulkerContents(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        // Off-hand shulker box holding STONE, DIRT, OAK_PLANKS in slots 0..2 (rest empty).
        List<ItemStack> slots = new ArrayList<>();
        slots.add(new ItemStack(Items.STONE));
        slots.add(new ItemStack(Items.DIRT));
        slots.add(new ItemStack(Items.OAK_PLANKS));
        while (slots.size() < SLOTS) slots.add(ItemStack.EMPTY);

        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        player.setItemInHand(InteractionHand.OFF_HAND, box);

        // Sneak is the default activation and the server re-verifies it, so hold it down.
        player.setShiftKeyDown(true);

        ServerPlayNetworking.Context ctx = contextFor(player, helper.getLevel().getServer());

        // Bare hand -> STONE -> DIRT -> OAK_PLANKS as we scroll up through the box.
        scrollUp(player, ctx);
        helper.assertTrue(player.getMainHandItem().is(Items.STONE),
            "first sneak+scroll should pull STONE into the main hand");

        scrollUp(player, ctx);
        helper.assertTrue(player.getMainHandItem().is(Items.DIRT),
            "second step should advance to DIRT");

        scrollUp(player, ctx);
        helper.assertTrue(player.getMainHandItem().is(Items.OAK_PLANKS),
            "third step should advance to OAK_PLANKS");

        // One more step lands on the "bare hands" stop: hand empties and all three items are back.
        scrollUp(player, ctx);
        helper.assertTrue(player.getMainHandItem().isEmpty(),
            "the step after the last item should return to bare hands");

        ItemContainerContents contents = player.getOffhandItem().get(DataComponents.CONTAINER);
        helper.assertValueEqual(nonEmptyCount(contents), 3,
            "all three items must be back in the box after a full cycle (none lost or duplicated)");

        helper.succeed();
    }

    private static void scrollUp(ServerPlayer player, ServerPlayNetworking.Context ctx) {
        // direction +1 (up), allowEmpty=true (bare-hands stop allowed), playSounds=false,
        // requireSneak=true (default sneak activation the server re-checks).
        ScrollHandler.receive(new ScrollPayload((byte) 1, true, false, true), ctx);
    }

    private static int nonEmptyCount(ItemContainerContents contents) {
        NonNullList<ItemStack> out = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        contents.copyInto(out);
        int n = 0;
        for (ItemStack s : out) {
            if (!s.isEmpty()) n++;
        }
        return n;
    }

    /**
     * The handler only ever reads {@link ServerPlayNetworking.Context#player()}; the real player is
     * doing all the work. server() is wired for completeness and responseSender() is unused (the
     * handler never replies, it only mutates inventory and optionally plays a sound).
     */
    private static ServerPlayNetworking.Context contextFor(ServerPlayer player, MinecraftServer server) {
        return new ServerPlayNetworking.Context() {
            @Override
            public MinecraftServer server() {
                return server;
            }

            @Override
            public ServerPlayer player() {
                return player;
            }

            @Override
            public PacketSender responseSender() {
                return null;
            }
        };
    }
}
