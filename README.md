# Silent UI

Custom menus, HUDs and in-world screens for **Paper 1.21.11+**, with no client mods.
You design a UI in the browser editor, push or export it, and the plugin draws it in game.

| UI type | How it's drawn | How players use it |
|---|---|---|
| **Screen** | Display entities on a plane locked in front of the player | The crosshair is the cursor: look at a button and click. Keys (WASD, Space, Shift, Q, F, scroll, 1–9) fire events. |
| **HUD** | Bossbar text built from resource-pack glyphs, positioned to the pixel | Always on screen. Placeholders refresh automatically. |
| **World** | Display entities placed in the world | Anyone can walk up, look at it and click. |

Features:

- **Real Minecraft assets.** The plugin downloads the official client jar for your server's version from Mojang once (about 30 MB, cached) and the editor gets every item as a proper inventory icon (3D blocks, flat items, tints), every vanilla texture as a sprite, and real player skins as 3D heads and full bodies.
- **Elements:** rectangles, text, buttons, sprites, text inputs (in-game dialog box), progress bars, 3D items (any item, item models, enchant glint, or the viewer's own held item and armour), 3D player heads, and full 3D **player models** (1.21.9+ mannequins) that spin or rotate when you scroll over them.
- **Tabs:** tab sets inside a menu. Tab buttons switch pages on their own, with an active style.
- **Presets:** a library of ready-made pieces with scripts already wired (tab bar, confirm dialog, item shop, player card, toggle, search box, toast…). Save your own from any selection; they are stored on the server so every admin has them.
- **Commands & functions:** custom commands with typed arguments (player, number, choice, text…), tab completion and usage messages, which run reusable script functions.
- **In-game editor:** `/sui edit <ui>` edits the UI inside Minecraft.
- Text with MiniMessage colours and placeholders (`{player}`, `{var:x}`, `{arg:x}`, `%papi%`), hover effects, keyframe animations with easing (interpolated client-side), and a node-based script editor.

## Install

1. Download `SilentUI-x.y.z.jar` from the [latest release](../../releases/tag/latest) (or build it, see below) and drop it in `plugins/`.
2. Start the server once. Then open `plugins/SilentUI/config.yml` and set `web.public-host` to your server's public IP or domain, and open the `web.port` (default **8163**, TCP) on your firewall or host panel. Players' clients download the resource pack from there.
3. `/sui reload`.

Four example UIs are installed on first start:

- `/shop [blocks|tools|food]` opens **example_shop**: tabs, real items, a `buy` function and a typed command.
- `/menu` opens **example_menu** (a profile menu with 6 tiles).
- **example_hud** shows automatically on join.
- `/sui world place example_board` puts the welcome board in front of you.

Already using ItemsAdder, Nexo, Oraxen or your own pack? Set `resource-pack.send-on-join: false` and merge `plugins/SilentUI/output/SilentUI-pack.zip` into your pack. Everything Silent UI adds lives under the `silentui` namespace, except two transparent bossbar textures.

## The editor

- Run `/sui editor` in game. It links to the editor that the plugin hosts (`http://<public-host>:8163/editor`).
- **Push to server** needs the token in `config.yml → web.upload-token`. It saves the UI and reloads it immediately.
- **Export** gives you the JSON. Put it in `plugins/SilentUI/uis/` and run `/sui reload`.

When the editor is opened from your server it connects automatically (the chip in the top bar shows **Server · MC 1.21.x**). That unlocks **Assets** (items, textures, skins) and server-side presets.

Shortcuts: `V R T B I N G E H M` tools · `A` timeline · `K` scripts · `P` preview · `Ctrl+Z` undo · `Alt+drag` duplicate · `?` help.

## In-game editor

`/sui edit <ui>` opens the UI in front of you with a toolbar underneath.

| Do this | To |
|---|---|
| Click an element | Select it |
| Click it again (or Space) | Pick it up, it follows your crosshair, click to drop |
| Right click | Edit all its properties in a dialog |
| W A S D | Nudge 1 pixel |
| Scroll | Resize |
| Q / F | Delete / duplicate |
| Shift | Cancel a move, deselect, or exit |

The toolbar has Move, Size, Up/Down (layer order), Copy, Delete, Edit, Add, Page (cycle tab pages), Undo, Save and Exit. **Save** writes the file and reloads it for everyone. Scripts and animations don't run while editing.

## Tabs

Make a tab set in the editor (Layers → **+ Tab set**). Elements on a page have the group `tabset/page`; tab buttons have **Tab button** set to `tabset/page` and an active fill/text colour. Clicking a tab button switches pages with no scripting. Scripts can also use **Switch tab**, **Show/Hide/Toggle group**.

## Commands & functions

Open **Commands & functions** in the editor:

- A command has a name, aliases, an optional permission, and arguments. Argument types are `word`, `text` (rest of the line), `number`, `integer`, `player`, `choice` and `boolean`, and each can be optional with a default. Players get tab completion and a usage message when they get it wrong.
- A command can open the UI and then run a **function**: a chain that starts at a *When function is called* node. *Call function* passes values like `item=minecraft:diamond, price=100`, readable as `{item}` and `{price}`.
- Command arguments are `{arg:name}`.
- A file with type **Logic** draws nothing and only holds commands and functions. Other UIs can call its functions by setting *In UI* on the Call function node.

## Commands

| Command | |
|---|---|
| `/sui open <ui> [player]` | Open a screen UI or show a HUD |
| `/sui close [player]` | Close the open screen UI |
| `/sui hud <show\|hide\|toggle> <ui> [player]` | Control HUDs |
| `/sui world place <ui> [name]` · `remove <name>` · `list` | World UIs |
| `/sui anim <animation> [player]` | Play an animation on the open UI |
| `/sui var <player> <name> <value>` | Set a script variable |
| `/sui reload` · `list` · `pack [player]` · `editor` | Admin |

UIs can also register their own commands (UI settings → Command, or a *When command is used* node).

Permissions: `silentui.admin`, `silentui.open.<id>` or `silentui.open.*`, `silentui.open.others`, and an optional permission per UI.

## Scripts

Events: page opens / closes, clicked (left/right), cursor enters / leaves, every N seconds, key pressed, text typed, command used, function called.
Elements: show / hide / toggle, set text / colour / sprite / progress / item / head, move, rotate 3D, play / stop animation, switch tab, show / hide / toggle group, ask for text.
Player & server: open / close UI, run command (as player or console), give item (name, lore, item model), message, broadcast, action bar, title, sound, screen fade.
Logic: wait, set / add variable, call function, if variable, if compare, if permission, random chance (each *if* has True / False outputs).

Item elements accept `{hand}`, `{offhand}`, `{helmet}`, `{chestplate}`, `{leggings}` and `{boots}` to show what the viewer is holding or wearing.

Text typed by players is stripped of `< > { } % & \ /` before it reaches scripts. Still, don't feed `{text}` into console commands.

## Build (IntelliJ IDEA)

1. **File → Open…** and pick this folder (or `pom.xml`). IntelliJ imports it as a Maven project. Use JDK 21.
2. Run **Build plugin jar** from the run configurations dropdown, or `mvn package` in a terminal.
3. The jar is at `target/SilentUI-1.0.0.jar`.

Every push to `main` is built by GitHub Actions, and the jar is published to the `latest` release.

## Limits worth knowing

- Player models need Minecraft 1.21.9+ (mannequins). Older servers show a head instead. Dialog popups (in-game editor, text inputs) need 1.21.6+; otherwise chat is used.
- Assets need the server to reach `piston-meta.mojang.com` once. If your host blocks that, put `client-<version>.jar` into `plugins/SilentUI/cache/` yourself, or set `assets.enabled: false`.
- HUD positions are relative to the top centre of the screen at 1 GUI pixel per canvas pixel. HUD text is ASCII only. Items and heads, rotation, scale, opacity and hover don't apply to HUDs.
- Screen UIs move with the crosshair, so the client draws the cursor with zero delay. Clicks and hover are resolved on the server, about one tick behind.
- Bedrock players (Geyser) can't load Java resource packs, so they won't see sprites.
- If something sits 1–2 pixels off on your client, tune `calibration.*` in `config.yml`.
