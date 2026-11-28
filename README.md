# Lo Client 1.6.3 (Fabric, Minecraft 1.21.11)

Cosmetic only: reads your own game state and draws on your own screen. Server-safe.
No resource packs: everything (textures, fonts) is built into the mod jar.

## Features
- HUD: FPS, XYZ, facing, ping, clock, biome, light, CPS, sprint/sneak, potion timers,
  armor + held-item durability, compass strip, WASD/space keystrokes
- HUD layout editor (Settings > HUD > Edit layout, or your own key): drag anything anywhere
  (HUD boxes can also be dragged while chat is open)
- Theme colors with a color wheel, brightness slider, hex color codes and presets
- Main menu (always on) with a starfield, a smooth glowing logo, smooth text and round buttons; round gray buttons, sliders and
  scrollbars in every menu
- Multiplayer screen: servers as round cards in a grid, with search, refresh, add/edit/delete,
  direct connection, MOTD, ping bars and each server's own icon
- Settings screen with two styles (Settings > Style > Menu style, or Modules tab):
  Classic (tabs) or Dropdown (collapsible sections). The search box finds any setting or module
- Fonts (Settings > Style > Font): Xuong (bitmap font by DrBartt), Outfit, Jura, Tektur,
  Pixelify Sans, Instrument Sans (SIL OFL). Changes apply instantly to all text
- Cherry blossom (sakura) background on the loading screens (Settings > Style)
- Soft hover sounds on buttons and modules, and a Lo Client click sound instead of the vanilla
  one (Settings > Style, both can be turned off)
- Fullbright (raises the Brightness setting while on; your saved Brightness is untouched)
- Buddy: a small mob that follows you (client-side, only you see it). Pick any living mob, name
  it and set its size in Settings > Buddy
- Crosshair styles, zoom, freelook (third person so you see your whole character), freecam (fly the
  camera around while your character stands still), third-person distance
- Sky tint, cloud tint, star field boost, block outline color, particle thinning
- Waypoints (/wp add|remove|list, or bind a key), 3 profile slots, Mod Menu integration

## Keys and modules
Options > Controls only has "Open settings" (Right Shift). Everything else is a module: open
Lo Client settings > Modules to switch each one on or off and give it its own key (click the key
box, press a key; Backspace clears, Esc cancels). No module has a key by default.

## Notes
- Item glint color and a recolorable vanilla button theme needed resource packs, so they were removed.
- Fonts, Buddy, fullbright, star boost, block outline color and sky/cloud tint hook into Minecraft
  internals. If a future version changes them, that one feature quietly does nothing.
- Old versions wrote "Lo Client Pack" and "Lo Client Glint" into the resourcepacks folder; the mod
  deletes those two folders on startup.

## Build
JDK 21. IntelliJ IDEA import, or `gradle build`. Jar: build/libs/Lo Client-1.6.3.jar
(not the -sources jar). Needs Fabric Loader + Fabric API for 1.21.11.
