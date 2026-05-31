# Shulker Pocket

A Fabric mod that turns an off-hand shulker box into a live, scrollable tool belt.
**Sneak + scroll** cycles items between the shulker and your main hand — no inventory screen, no hotbar juggling.

- **MC 26.1.2** · Fabric Loader 0.19.2 · Fabric API 0.149.1+26.1.2 · Java 25 · Loom `net.fabricmc.fabric-loom` 1.16-SNAPSHOT
- Mod ID `shulker_pocket` · required on **both** client and server (inventory mutation is server-authoritative)

## How it works

1. Put any shulker box (or other `minecraft:container` item) in your off-hand.
2. Hold **sneak** (or a key you bind under Options → Controls → Inventory) and **scroll** the mouse wheel.
3. The held item rotates through the shulker's contents; scroll past the end to reach bare hands.

There's no custom overlay — vanilla already shows the held item's name above the hotbar as it
changes, and you know what's in your box.

## Build & run

```sh
./gradlew genSources    # decompiled MC sources for navigation
./gradlew runClient     # dev client
./gradlew runServer     # dev dedicated server
./gradlew test          # ContainerOps unit tests
./gradlew build         # outputs build/libs/shulker-pocket-0.2.0.jar
```

The jar goes in `mods/` on **both** client and server.

## Config

Install **[Mod Menu](https://modrinth.com/mod/modmenu)** (optional) to edit everything in-game:
its Config button on the Shulker Pocket row opens a settings screen. Otherwise, edit
`config/shulker_pocket.json` (written on first launch) by hand.

| Key                       | Default | Meaning                                          |
| ------------------------- | ------- | ------------------------------------------------ |
| `invertScroll`            | `false` | Flip scroll direction.                           |
| `cooldownMs`              | `250`   | Min ms between scroll fires (debounce).          |
| `allowEmptyPosition`      | `true`  | Let the cursor land on "bare hands".             |
| `playSounds`              | `true`  | Play subtle swap / deny sounds.                  |
| `useActivationKey`        | `false` | Activate with a bindable key instead of sneak.   |

See [SPEC.md](SPEC.md) for the full design, edge cases, and the `// VERIFY:` checklist.
