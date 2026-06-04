plugins {
    id("dev.kikugie.stonecutter")
}

// Active = the version your IDE compiles and the state src/ is left in. Keep it on current MC so
// day-to-day development is in the current dialect; older versions are generated from it.
stonecutter active "26.1.2"

stonecutter parameters {
    replacements {
        // The committed source is written in the CURRENT (26.1) dialect. These rules downgrade the
        // handful of renamed symbols for older Minecraft. Each rule is centralized here, so the
        // source files themselves never carry old-version code.

        // Mojang renamed ResourceLocation -> Identifier at 1.21.11 (package net.minecraft.resources
        // is unchanged, so a bare token swap is correct).
        string(current.parsed < "1.21.11") {
            replace("Identifier", "ResourceLocation")
        }

        // Fabric API + client renames that landed at the 26.1 unobfuscation boundary.
        string(current.parsed < "26.1") {
            replace("serverboundPlay", "playC2S")
            replace("keymapping.v1", "keybinding.v1")
            replace("KeyMappingHelper", "KeyBindingHelper")
            replace("registerKeyMapping", "registerKeyBinding")
            // Typed key category -> the older String category argument. NOTE: the real boundary for
            // KeyMapping.Category is somewhere in the 1.21.x line; narrow this when middle nodes are
            // added (it is correct for the 1.21.1 + 26.1.2 proof-of-concept pair).
            replace("KeyMapping.Category.INVENTORY", "\"key.categories.inventory\"")
        }
    }
}
