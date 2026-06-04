package dev.isaac.shulkerpocket.server;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for the rotating swap. The key regression here is {@link #cyclesThroughAllItems}:
 * an earlier version tracked the cursor by matching the held item against the contents, which made
 * scrolling ping-pong between two items. These lock in the home-slot cursor behavior.
 */
class ContainerOpsTest {

    @BeforeAll
    static void bootstrap() {
        // Register vanilla items so the registry is populated.
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        //? if >=26.1 {
        // 26.1 binds per-item data components during a registry data-load phase that a bare bootstrap
        // doesn't run, so `new ItemStack(item)` throws "Components not bound yet". ContainerOps only
        // cares about item identity + isSameItemSameComponents (never component contents), so binding
        // an empty map to every item is enough to exercise the real ItemStack/ItemContainerContents paths.
        // Pre-26.1 the normal bootstrap binds components, so this loop is not needed there.
        for (Item item : BuiltInRegistries.ITEM) {
            Holder.Reference<Item> holder = item.builtInRegistryHolder();
            if (!holder.areComponentsBound()) {
                holder.bindComponents(DataComponentMap.EMPTY);
            }
        }
        //?}
    }

    private record State(ItemStack hand, ItemContainerContents contents, int home) {}

    /** Build container contents from items placed at slots 0,1,2,… (padded to 27 with empties). */
    private static ItemContainerContents box(Item... itemsInOrder) {
        List<ItemStack> list = new ArrayList<>();
        for (Item it : itemsInOrder) list.add(it == null ? ItemStack.EMPTY : new ItemStack(it));
        while (list.size() < ContainerOps.SLOTS) list.add(ItemStack.EMPTY);
        return ItemContainerContents.fromItems(list);
    }

    /** Apply one scroll step; returns the new state, or null if the swap was refused. */
    private static State step(State s, int dir, boolean allowEmpty) {
        ContainerOps.SwapResult r =
            ContainerOps.swap(s.contents(), s.hand(), s.home(), (byte) dir, allowEmpty);
        return r == null ? null : new State(r.mainHand(), r.contents(), r.homeSlot());
    }

    private static int countNonEmpty(ItemContainerContents c) {
        NonNullList<ItemStack> slots = NonNullList.withSize(ContainerOps.SLOTS, ItemStack.EMPTY);
        c.copyInto(slots);
        int n = 0;
        for (ItemStack st : slots) {
            if (!st.isEmpty()) n++;
        }
        return n;
    }

    @Test
    void cyclesThroughAllItems() {
        State s = new State(ItemStack.EMPTY,
            box(Items.STONE, Items.DIRT, Items.OAK_PLANKS), ContainerOps.NO_HOME);

        s = step(s, +1, true);
        assertSame(Items.STONE, s.hand().getItem());
        s = step(s, +1, true);
        assertSame(Items.DIRT, s.hand().getItem());
        s = step(s, +1, true);
        assertSame(Items.OAK_PLANKS, s.hand().getItem());
        s = step(s, +1, true);
        assertTrue(s.hand().isEmpty(), "should reach bare hands after the last item");
        s = step(s, +1, true);
        assertSame(Items.STONE, s.hand().getItem(), "should wrap back to the first item");
    }

    @Test
    void reverseVisitsLastItemFirst() {
        State s = new State(ItemStack.EMPTY,
            box(Items.STONE, Items.DIRT, Items.OAK_PLANKS), ContainerOps.NO_HOME);
        s = step(s, -1, true);
        assertSame(Items.OAK_PLANKS, s.hand().getItem());
    }

    @Test
    void everyItemPreservedAcrossAFullCycle() {
        State s = new State(ItemStack.EMPTY,
            box(Items.STONE, Items.DIRT, Items.OAK_PLANKS), ContainerOps.NO_HOME);
        for (int i = 0; i < 4; i++) {
            s = step(s, +1, true); // 3 items + bare hands → back to start
        }
        assertTrue(s.hand().isEmpty());
        assertEquals(3, countNonEmpty(s.contents()), "no item lost or duplicated over a full cycle");
    }

    @Test
    void handlesDuplicateItemTypes() {
        // Two identical stacks must not collapse the cursor (the old content-matching bug).
        State s = new State(ItemStack.EMPTY,
            box(Items.STONE, Items.STONE, Items.DIRT), ContainerOps.NO_HOME);
        s = step(s, +1, true);
        assertSame(Items.STONE, s.hand().getItem());
        s = step(s, +1, true);
        assertSame(Items.STONE, s.hand().getItem());
        s = step(s, +1, true);
        assertSame(Items.DIRT, s.hand().getItem(), "must advance past the duplicate, not stick");
    }

    @Test
    void emptyBoxEmptyHandIsRefused() {
        State s = new State(ItemStack.EMPTY, box(), ContainerOps.NO_HOME);
        assertNull(step(s, +1, true));
    }

    @Test
    void foreignItemIntoFullBoxIsRefused() {
        Item[] full = new Item[ContainerOps.SLOTS];
        Arrays.fill(full, Items.STONE);
        State s = new State(new ItemStack(Items.DIAMOND), box(full), ContainerOps.NO_HOME);
        assertNull(step(s, +1, true), "no room to park the foreign item → refuse, don't destroy");
    }

    @Test
    void allowEmptyFalseNeverGoesToBareHands() {
        State s = new State(ItemStack.EMPTY,
            box(Items.STONE, Items.DIRT), ContainerOps.NO_HOME);
        for (int i = 0; i < 6; i++) {
            s = step(s, +1, false);
            assertFalse(s.hand().isEmpty(), "with allowEmpty=false the hand should always hold an item");
        }
    }
}
