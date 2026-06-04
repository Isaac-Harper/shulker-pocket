# Changelog

## 0.2.2

- Open-sourced under the MIT License; the GitHub repository is now public.
- README restored its build badge, demo GIFs, and a download/build-from-source section now that
  the repo links resolve publicly.
- No gameplay or behavior changes since 0.2.1.

## 0.2.1

- Project page polish: README badges (Modrinth, Minecraft, Fabric, build), a **Why** section, and
  clear install steps. The README doubles as the Modrinth project page.
- Added demo GIFs showing the sneak + scroll swap in action.
- No gameplay or behavior changes since 0.2.0.

## 0.2.0

- **In-game config screen via Mod Menu.** When [Mod Menu](https://modrinth.com/mod/modmenu) is
  installed, Shulker Pocket gains a Config button that opens a screen for all options, with no need to
  edit the JSON by hand. Mod Menu is an optional dependency; the mod still runs without it.
- **Custom activation key.** Optionally activate with a rebindable key (Options → Controls →
  Inventory) instead of sneak. Toggle `useActivationKey` / the "Activation" option.
- **Tooltip hint.** Shulker boxes now show a "<key> + scroll in off-hand to cycle" hint
  (naming your current activation key); toggle with `showTooltipHint`.
- Config screen now has per-option tooltips and a **Reset to defaults** button.
- All config-screen labels are now localizable (`lang/en_us.json`).
- Higher-resolution (512×512) mod icon.

## 0.1.0

- Initial alpha for Minecraft 26.1.2. Sneak + scroll cycles an off-hand shulker box's contents
  through the main hand (server-authoritative swap, per-player home-slot cursor).
