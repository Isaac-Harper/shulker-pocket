# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Fabric mod (Minecraft **26.1.2**, Java **25**) that turns an off-hand shulker box into a
scrollable tool belt: hold sneak and scroll to rotate items between the shulker's 27 slots and
the main hand. Mod ID `shulker_pocket`, package `dev.isaac.shulkerpocket`.

The full design lives in [SPEC.md](SPEC.md) — read it before changing behavior. It is the source
of truth for the swap algorithm, edge cases, and HUD layout.

## Status / how to read the source

**Working end-to-end (M1–M3).** Builds against MC 26.1.2 (`./gradlew build` → the jar), the scroll
payload round-trips, and sneak+scroll cycles through all items in-game (confirmed by the author).
All mapping `// VERIFY:` tags are resolved against the real 26.1 names and removed. The HUD (M4) was
dropped by design. Remaining: M5 — sounds, config polish, packaging.

**26.1 is unobfuscated.** From MC 26.1 the game ships with real Mojang names + parameter names, so
there are no deobfuscation mappings and Fabric dropped Yarn. The build uses the
**`net.fabricmc.fabric-loom`** plugin, which does **not** remap: there is no `mappings` line and
deps use plain `implementation` (never reintroduce `loom.officialMojangMappings()` or
`modImplementation` here). See the mapping-resolution table in [SPEC.md](SPEC.md) for the 26.1
renames (e.g. `ResourceLocation`→`Identifier`, `GuiGraphics`→`GuiGraphicsExtractor`).

## Prerequisites

- **JDK 25** and **Gradle** are installed (Homebrew `openjdk@25`, keg-only; Gradle 9.5.1). Build with
  `JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home ./gradlew build`.
- The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is committed
  and pinned to Gradle 9.4.

## Commands

```sh
./gradlew genSources    # decompile MC sources for navigation (run once after setup)
./gradlew runClient     # launch dev client
./gradlew runServer     # launch dev dedicated server
./gradlew build         # → build/libs/shulker-pocket-0.1.0.jar
```

`./gradlew test` runs the JUnit suite (`src/test`, via `fabric-loader-junit`). `ContainerOpsTest`
exercises the rotating swap against real `ItemStack`s. Note its `@BeforeAll`: after
`Bootstrap.bootStrap()` it binds an empty `DataComponentMap` to every item, because 26.1 binds
per-item components in a registry data-load phase a bare bootstrap skips (otherwise `new ItemStack`
throws "Components not bound yet"). The swap logic doesn't read component contents, so this is safe.

## Architecture

Server-authoritative. The client only detects intent and sends a single packet; the server
re-validates every gate and is the only side that mutates inventory. The mod must be installed on
**both** sides.

**Split source sets** (Loom `splitEnvironmentSourceSets()`): `src/main` is common + server,
`src/client` is client-only. `client` depends on `main`, so client code may import server classes —
but **not** the reverse. Keep anything the server needs in `src/main`.

**The scroll round-trip** (one direction, client → server):

1. `client/mixin/MouseHandlerMixin` injects at `MouseHandler#onScroll` HEAD. It gates on
   screen-closed + sneaking + off-hand-is-a-container + `ScrollState` cooldown, then sends
   `ScrollPayload` and **cancels** the callback to suppress the vanilla hotbar change.
2. `network/ScrollPayload` (a `record` + `StreamCodec`) carries one byte: `-1` / `+1`. Registered
   in `ShulkerPocket#onInitialize` via `PayloadTypeRegistry.playC2S()`.
3. `server/ScrollHandler#receive` re-checks sneaking (anti-cheat — never trusts the client),
   reads the off-hand `ItemContainerContents`, and delegates to `ContainerOps`.
4. `server/ContainerOps#swap` is the pure rotating-swap algorithm. It takes the player's current
   `homeSlot` and returns a `SwapResult` (new main-hand stack + new contents + new home slot) or
   `null` to refuse. `ScrollHandler` holds the per-player `UUID → homeSlot` cursor map, writes the
   result back, and calls `broadcastChanges()`.

**No HUD.** There is no on-screen overlay — dropped by design (the swap is the feedback). Don't
reintroduce one without a server→client cursor sync: the cursor is server-authoritative home-slot
state and cannot be re-derived on the client by content-matching.

**Config** (`client/ClientConfig`) is a plain GSON-serialized POJO at
`config/shulker_pocket.json`, loaded once at client init and written with defaults if missing.
It is client-only; the server does not read it.

## Conventions

- Mixin members are prefixed `shulker_pocket$…` to avoid clashes with the target class.
- The cursor is tracked by **home slot** (the slot the held item came from), persisted per player in
  `ScrollHandler`. Do **not** re-derive it by matching the held item against the contents — that
  collapses the cursor onto the first slot and makes scrolling ping-pong between two items.
- Logical position `occupied.size()` is the "bare hands" stop — the `+1` in the modulo. Preserve it.
