# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Fabric mod (Minecraft **26.1.2**, Java **25**) that turns an off-hand shulker box into a
scrollable tool belt: hold sneak and scroll to rotate items between the shulker's 27 slots and
the main hand. Mod ID `shulker_pocket`, package `dev.isaac.shulkerpocket`.

The full design lives in [SPEC.md](SPEC.md) — read it before changing behavior. It is the source
of truth for the swap algorithm, edge cases, and HUD layout.

## Status / how to read the source

**M1 reached: the skeleton compiles** against MC 26.1.2 (`./gradlew build` → the jar). All mapping
`// VERIFY:` tags have been resolved against the real 26.1 names and removed; only one non-mapping
VERIFY remains (the HUD pixel offset in `PocketHudOverlay`, to be tuned in-game). Runtime
milestones M2–M4 are still unverified — nothing has been launched in a dev client/server yet.

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

There is no test source set yet; `ContainerOps` is written as pure logic specifically so a JUnit
suite can be added against it without a running client.

## Architecture

Server-authoritative. The client only detects intent and sends a single packet; the server
re-validates every gate and is the only side that mutates inventory. The mod must be installed on
**both** sides.

**Split source sets** (Loom `splitEnvironmentSourceSets()`): `src/main` is common + server,
`src/client` is client-only. `client` depends on `main`, so client code may import server classes
(e.g. `PocketHudOverlay` reads `ContainerOps.SLOTS`) — but **not** the reverse. Keep anything the
server needs in `src/main`.

**The scroll round-trip** (one direction, client → server):

1. `client/mixin/MouseHandlerMixin` injects at `MouseHandler#onScroll` HEAD. It gates on
   screen-closed + sneaking + off-hand-is-a-container + `ScrollState` cooldown, then sends
   `ScrollPayload` and **cancels** the callback to suppress the vanilla hotbar change.
2. `network/ScrollPayload` (a `record` + `StreamCodec`) carries one byte: `-1` / `+1`. Registered
   in `ShulkerPocket#onInitialize` via `PayloadTypeRegistry.playC2S()`.
3. `server/ScrollHandler#receive` re-checks sneaking (anti-cheat — never trusts the client),
   reads the off-hand `ItemContainerContents`, and delegates to `ContainerOps`.
4. `server/ContainerOps#swap` is the pure rotating-swap algorithm. It returns a `SwapResult`
   (new main-hand stack + new contents) or `null` to refuse. `ScrollHandler` writes the result
   back and calls `broadcastChanges()`.

**HUD** is rendered entirely client-side: `client/PocketHudOverlay` is attached after the hotbar
layer in `ShulkerPocketClient` via `HudLayerRegistrationCallback`. It re-derives the same cursor
the server uses; there is no state sync for it.

**Config** (`client/ClientConfig`) is a plain GSON-serialized POJO at
`config/shulker_pocket.json`, loaded once at client init and written with defaults if missing.
It is client-only; the server does not read it.

## Conventions

- Mixin members are prefixed `shulker_pocket$…` to avoid clashes with the target class.
- The cursor model treats logical position `occupied.size()` as the "bare hands" slot — the
  `+1` in the modulo. Preserve this when editing `ContainerOps`; the HUD and swap must agree.
