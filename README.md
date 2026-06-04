# Shulker Pocket

A Fabric mod that turns an off-hand shulker box into a live, scrollable tool belt.
**Sneak + scroll** cycles items between the shulker and your main hand: no inventory screen, no
hotbar juggling.

[![Modrinth](https://img.shields.io/modrinth/v/shulker-pocket?logo=modrinth&color=00AF5C&label=Modrinth)](https://modrinth.com/mod/shulker-pocket)
[![Downloads](https://img.shields.io/modrinth/dt/shulker-pocket?logo=modrinth&color=00AF5C&label=downloads)](https://modrinth.com/mod/shulker-pocket)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?logo=minecraft)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
[![Build](https://github.com/Isaac-Harper/shulker-pocket/actions/workflows/build.yml/badge.svg)](https://github.com/Isaac-Harper/shulker-pocket/actions/workflows/build.yml)

![Cycling through tools in the off-hand shulker](https://raw.githubusercontent.com/Isaac-Harper/shulker-pocket/main/TOOL.gif)
![Cycling through a shulker of plants while building](https://raw.githubusercontent.com/Isaac-Harper/shulker-pocket/main/PLANTS.gif)

## Why

Carrying a stack of tools means a cluttered hotbar or constant trips to your inventory. Shulker
Pocket lets a single off-hand shulker box *be* your tool belt: spin the wheel to bring the next
item to hand, instantly, without ever opening a screen. Your hotbar stays free for what matters.

## How it works

1. Put any shulker box (or other `minecraft:container` item) in your off-hand.
2. Hold **sneak** (or a key you bind under Options → Controls → Inventory) and **scroll** the mouse wheel.
3. The held item rotates through the shulker's contents; scroll past the end to reach bare hands.

There's no custom overlay. Vanilla already shows the held item's name above the hotbar as it
changes, and you know what's in your box.

## Install

Requires **[Fabric Loader](https://fabricmc.net/use/installer/)** and
**[Fabric API](https://modrinth.com/mod/fabric-api)** on Minecraft **26.1.2**. Must be installed on
**both** the client and the server.

**[Download on Modrinth](https://modrinth.com/mod/shulker-pocket)** or grab a jar from
**[GitHub Releases](https://github.com/Isaac-Harper/shulker-pocket/releases)**.

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
| `showTooltipHint`         | `true`  | Show a usage hint on shulker-box tooltips.       |

## Building from source

Requires **JDK 25**. Minecraft 26.1 ships unobfuscated, so the build uses the
`net.fabricmc.fabric-loom` plugin with no remap step.

```sh
./gradlew build      # -> build/libs/shulker-pocket-<version>.jar
./gradlew runClient  # launch a dev client
./gradlew test       # run the JUnit suite
```

The design and swap algorithm are documented in [SPEC.md](SPEC.md).

## License

Released under the [MIT License](LICENSE).
