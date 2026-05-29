package dev.isaac.shulkerpocket.server;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure swap logic (spec "rotating swap"). No player/world references so it stays unit-testable.
 * Returns {@code null} to refuse a swap (caller plays the deny sound and mutates nothing).
 *
 * <p>The cursor is tracked by {@code homeSlot} — the slot the currently-held item was taken from —
 * <em>not</em> by matching the held item against the contents. Content-matching fails the moment an
 * item leaves the box (it's now in the hand, not the shulker), which collapses the cursor onto the
 * first slot and makes scrolling ping-pong between two items. Persisting the home slot lets us walk
 * cleanly through every item.
 */
public final class ContainerOps {
    public static final int SLOTS = 27;

    /** Sentinel: not holding a pocket item (empty hand, or the cursor is on the bare-hands stop). */
    public static final int NO_HOME = -1;

    private ContainerOps() {}

    /**
     * New state after one scroll step.
     *
     * @param mainHand new main-hand stack
     * @param contents new shulker contents
     * @param homeSlot  slot the new main-hand item was taken from (its "home"), or {@link #NO_HOME}
     *                  for the bare-hands stop. Persist this per player and feed it back into the
     *                  next {@link #swap} call so the cursor survives items leaving the box.
     */
    public record SwapResult(ItemStack mainHand, ItemContainerContents contents, int homeSlot) {}

    public static SwapResult swap(ItemContainerContents contents, ItemStack mainHand, int homeSlot,
                                  byte direction, boolean allowEmpty) {
        // 1. Materialize the 27 slots.
        NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        contents.copyInto(slots);

        // 2. Return the held item to a slot so `slots` is the complete inventory to rotate over.
        //    Prefer its known home; if that's stale (contents changed) or the item is foreign,
        //    fall back to the first free slot. A foreign item with no room → refuse, don't destroy.
        if (!mainHand.isEmpty()) {
            boolean validHome = homeSlot >= 0 && homeSlot < SLOTS && slots.get(homeSlot).isEmpty();
            int target = validHome ? homeSlot : firstFreeSlot(slots);
            if (target == NO_HOME) return null; // full shulker + foreign item in hand → refuse
            slots.set(target, mainHand);
            homeSlot = target;
        } else {
            homeSlot = NO_HOME;
        }

        // 3. Sorted indices of non-empty slots.
        List<Integer> occupied = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            if (!slots.get(i).isEmpty()) occupied.add(i);
        }
        if (occupied.isEmpty()) return null; // empty shulker + empty hand → no-op (soft deny)

        // 4. Cursor: where the held item now sits, else the bare-hands stop. When the bare-hands stop
        //    is disabled, an empty cursor sits at -1 so the first step lands on a real item.
        int cursor;
        if (homeSlot != NO_HOME) {
            cursor = occupied.indexOf(homeSlot);
        } else {
            cursor = allowEmpty ? occupied.size() : -1;
        }

        // 5. Step and wrap. With the bare-hands stop, the range is [0, occupied.size()] inclusive (the
        //    extra slot is "bare hands"); without it, [0, occupied.size()) — you always hold an item.
        int step = direction > 0 ? 1 : -1;
        int positions = occupied.size() + (allowEmpty ? 1 : 0);
        int next = Math.floorMod(cursor + step, positions);

        // 6. Resolve the new position.
        if (allowEmpty && next == occupied.size()) {
            // Bare-hands stop: the held item stays parked at its home, hand goes empty.
            return new SwapResult(ItemStack.EMPTY, ItemContainerContents.fromItems(slots), NO_HOME);
        }
        int slot = occupied.get(next);
        ItemStack picked = slots.get(slot);
        slots.set(slot, ItemStack.EMPTY); // take it out → this slot becomes the new home hole
        return new SwapResult(picked, ItemContainerContents.fromItems(slots), slot);
    }

    private static int firstFreeSlot(NonNullList<ItemStack> slots) {
        for (int i = 0; i < SLOTS; i++) {
            if (slots.get(i).isEmpty()) return i;
        }
        return NO_HOME;
    }
}
