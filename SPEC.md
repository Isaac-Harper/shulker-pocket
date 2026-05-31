# Shulker Pocket — Spec

A Fabric mod that turns an off-hand shulker box into a live, scrollable tool belt.
Sneak + scroll cycles items through the main hand.

> **Status:** working end-to-end (M1–M3). Builds against MC 26.1.2, scroll round-trips,
> and sneak+scroll cycles through all items in-game. The HUD (M4) was dropped by design.
> Remaining: M5 — sounds, config polish, and packaging for release.

## Target

| Component       | Version                       |
| --------------- | ----------------------------- |
| Minecraft       | 26.1.2 (Hermitcraft S11)      |
| Fabric Loader   | 0.19.2                        |
| Fabric API      | 0.149.1+26.1.2                |
| Fabric Loom     | `net.fabricmc.fabric-loom` 1.16-SNAPSHOT |
| Mappings        | None — MC 26.1 ships unobfuscated (real Mojang names) |
| Java            | 25                            |
| Gradle          | 9.4+                          |
| IntelliJ IDEA   | 2025.3+ (mixin tooling)       |

Single-version, no Stonecutter / cross-loader. Port forward when Hermitcraft does.

> **26.1 is unobfuscated.** From MC 26.1 onward the game jars carry real Mojang names and
> parameter names, so there are no deobfuscation mappings (and Fabric dropped Yarn). The build
> uses the **`net.fabricmc.fabric-loom`** plugin, which does **not** remap — there is no
> `mappings` line and dependencies use plain `implementation` (not `modImplementation`).

## Identity

- Mod ID: `shulker_pocket`
- Package: `dev.isaac.shulkerpocket`
- Installation: required on **both** client and server (inventory mutation is server-authoritative).

## Trigger

All three must hold:

1. Off-hand stack carries a `minecraft:container` data component.
2. Player is sneaking (server-verified).
3. Player scrolls the mouse wheel.

When triggered, the vanilla hotbar-slot change is suppressed for that scroll event.

## Behavior — rotating swap (home-slot cursor)

The cursor is tracked by **home slot** — the shulker slot the currently-held item came from —
persisted per player on the server (`ScrollHandler` keeps a `UUID → slot` map). It is **not**
derived by matching the held item against the contents: a held item is no longer in the box, so
content-matching collapses the cursor onto the first slot and makes scrolling ping-pong between two
items. The home slot survives the item leaving the box.

1. Read the off-hand stack's `ItemContainerContents` into 27 slots.
2. Return the held item to a slot so `slots` is the complete picture: its known home if still empty,
   else the first free slot. A foreign item with no free slot → **refuse** (don't destroy).
3. Build a sorted list of non-empty slot indices (`occupied`).
4. Cursor = index of the home slot within `occupied`, or `occupied.size()` (the bare-hands stop) when
   not holding a pocket item.
5. Scroll direction → step `±1`; next = `floorMod(cursor + step, occupied.size + 1)`. The `+1` is the
   bare-hands stop so you can scroll back to empty hands.
6. Resolve:
   - Bare-hands stop → held item stays parked at its home, hand goes empty, home = none.
   - Occupied slot → take that item into the hand, leaving the slot empty; that slot is the new home.
7. Write new `ItemContainerContents` back to off-hand, set the main hand, store the new home slot, sync.

## Edge cases

| Case                                       | Behavior                              |
| ------------------------------------------ | ------------------------------------- |
| Empty shulker + empty main hand            | No-op, soft deny sound.               |
| Full shulker + foreign item in hand        | Refuse swap, deny sound. Don't destroy. |
| Main-hand item *is* the off-hand shulker   | Refuse.                               |
| Screen open while scrolling                | Client gate `mc.screen == null`.      |
| Scroll fires repeatedly                    | 50 ms client cooldown.                |
| Nested shulker                             | Treated as a regular item. No recursion. |

## HUD overlay

None — dropped by design. The swap itself is the feedback, and the player already knows what's in
their box. (An earlier version attached an element after the vanilla hotbar; it was removed because
the cursor is now server-authoritative home-slot state with no client sync, and the display added
clutter without earning its complexity.)

## Networking

One C2S payload. Server is authoritative.

```
ScrollPayload {
  direction: byte,      // -1 or +1
  allowEmpty: boolean,  // client config: may the cursor land on bare hands?
  playSounds: boolean   // client config: should the server play feedback sounds?
}
```

The two booleans ride along because config is client-only — the server reads no config file, so the
preferences it needs to honour travel with the action. Identifier: `shulker_pocket:scroll`

### Server handler flow

```
on receive ScrollPayload(direction):
  player = handler.player
  if !player.isShiftKeyDown(): return            // anti-cheat
  offhand = player.getOffhandItem()
  contents = offhand.get(DataComponents.CONTAINER)
  if contents == null: return                    // not a container

  mainHand = player.getMainHandItem()
  home = HOME.getOrDefault(player.uuid, NO_HOME)             // per-player cursor
  result = swap(contents, mainHand, home, direction)         // null → refuse, mutate nothing
  if result == null: return

  offhand.set(DataComponents.CONTAINER, result.contents)
  player.setItemInHand(InteractionHand.MAIN_HAND, result.mainHand)
  HOME.put(player.uuid, result.homeSlot)                     // remember the new cursor
  player.inventoryMenu.broadcastChanges()
```

### Client interceptor flow (Mixin into `MouseHandler#onScroll` @ HEAD)

```
if mc.screen != null: return                     // GUI open
if !player.isShiftKeyDown(): return
if !offhand.has(DataComponents.CONTAINER): return
if !state.tryFire(cooldownMs): ci.cancel(); return
direction = vertical > 0 ? +1 : -1
if config.invertScroll: direction = -direction
ClientPlayNetworking.send(new ScrollPayload(direction))
ci.cancel()                                      // suppress vanilla hotbar change
```

## Config — `config/shulker_pocket.json`

```json
{
  "invertScroll": false,
  "cooldownMs": 250,
  "allowEmptyPosition": true,
  "playSounds": true,
  "useActivationKey": false
}
```

Editable in-game when **Mod Menu** is installed (an optional dependency): the `modmenu`
entrypoint (`client/ModMenuIntegration`) hands Mod Menu a vanilla-widget `ConfigScreen` that edits
a working copy and commits to `ClientConfig` + disk only on *Done*. Without Mod Menu, edit the JSON
by hand. The screen uses plain renderable widgets (`CycleButton`, `AbstractSliderButton`,
`StringWidget`) — no custom drawing — so it sidesteps the 26.1 render-extraction API.

## Mapping resolution (done at first compile against 26.1.2)

All symbols below are confirmed compiling. The 26.1 names differ from the older Mojang
mappings the skeleton was first written against; the resolved names are:

- [x] `MouseHandler#onScroll(JDD)V` — still exists (private; mixin target OK)
- [x] `net.minecraft.core.component.DataComponents.CONTAINER` — unchanged
- [x] `net.minecraft.world.item.component.ItemContainerContents` — unchanged
- [x] `ItemContainerContents.fromItems(List<ItemStack>)` / `copyInto(NonNullList<ItemStack>)` — unchanged
- [x] `Player.getMainHandItem() / getOffhandItem() / setItemInHand() / isShiftKeyDown()` — unchanged
- [x] `ResourceLocation` → **`net.minecraft.resources.Identifier`** (`fromNamespaceAndPath` unchanged)
- [x] `PayloadTypeRegistry.playC2S()` → **`serverboundPlay()`**

All `// VERIFY:` tags have been removed from the source.

## Milestones

- [x] **M1 — Skeleton compiles.** `./gradlew build` produces the jar against 26.1.2.
- [x] **M2 — Payload round-trips.** Client sends `ScrollPayload`, server receives.
- [x] **M3 — Swap works.** Sneak+scroll cycles through all items end-to-end (home-slot cursor).
- [x] **M4 — ~~HUD~~.** Dropped by design — no on-screen overlay.
- [~] **M5 — Config + sounds + polish.** Done: wired config, subtle swap/deny sounds, logout cleanup,
  unit tests, mod metadata + icon + LICENSE. Remaining: real icon art and the actual Modrinth upload.
