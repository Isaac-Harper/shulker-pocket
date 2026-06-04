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

        // Note: the ResourceLocation -> Identifier rename (1.21.11) and the further pre-1.21
        // constructor-vs-factory difference are handled with local //? conditionals in ScrollPayload,
        // since Identifier appears in only that one file.

        // Fabric API + client renames that landed at the 26.1 unobfuscation boundary.
        string(current.parsed < "26.1") {
            replace("serverboundPlay", "playC2S")
            replace("keymapping.v1", "keybinding.v1")
            replace("KeyMappingHelper", "KeyBindingHelper")
            replace("registerKeyMapping", "registerKeyBinding")
        }

        // The typed KeyMapping.Category enum was introduced at 1.21.9; older versions take a String
        // translation key as the key-category argument. (Verified: 1.21.8 needs String, 1.21.10 needs
        // the enum.) The 3-arg CycleButton.booleanBuilder is handled by a local //? in ConfigScreen
        // (introduced at 1.21.11).
        string(current.parsed < "1.21.9") {
            replace("KeyMapping.Category.INVENTORY", "\"key.categories.inventory\"")
        }
    }
}
