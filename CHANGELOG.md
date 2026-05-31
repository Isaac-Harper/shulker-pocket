# Changelog

## 0.2.0

- **In-game config screen via Mod Menu.** When [Mod Menu](https://modrinth.com/mod/modmenu) is
  installed, Shulker Pocket gains a Config button that opens a screen for all options — no need to
  edit the JSON by hand. Mod Menu is an optional dependency; the mod still runs without it.
- **Custom activation key.** Optionally activate with a rebindable key (Options → Controls →
  Inventory) instead of sneak — toggle `useActivationKey` / the "Activation" option.
- **Tooltip hint.** Shulker boxes now show a "<key> + scroll in off-hand to cycle" hint
  (naming your current activation key); toggle with `showTooltipHint`.
- Config screen now has per-option tooltips and a **Reset to defaults** button.
- All config-screen labels are now localizable (`lang/en_us.json`).
- Higher-resolution (512×512) mod icon.

## 0.1.0

- Initial alpha for Minecraft 26.1.2. Sneak + scroll cycles an off-hand shulker box's contents
  through the main hand (server-authoritative swap, per-player home-slot cursor).
