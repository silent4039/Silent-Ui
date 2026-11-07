# Silent UI

Custom menus, HUDs and in-world screens for **Paper 1.21.11+**, with no client mods.
You design a UI in the browser editor, push or export it, and the plugin draws it in game.

| UI type | How it's drawn | How players use it |
|---|---|---|
| **Screen** | Display entities on a plane locked in front of the player | The crosshair is the cursor: look at a button and click. Keys (WASD, Space, Shift, Q, F, scroll, 1–9) fire events. |
| **HUD** | Bossbar text built from resource-pack glyphs, positioned to the pixel | Always on screen. Placeholders refresh automatically. |
| **World** | Display entities placed in the world | Anyone can walk up, look at it and click. |

Features: layers, sprites (upload PNGs or generate rounded panels, circles, bars and pixel icons), text with MiniMessage colours and placeholders (`{player}`, `{var:x}`, `%papi%`), buttons, text inputs, progress bars, items, player heads, hover effects, keyframe animations with easing (client-side interpolated), and a node-based script editor.

## Install

1. Download `SilentUI-x.y.z.jar` from the [latest release](../../releases/tag/latest) (or build it, see below) and drop it in `plugins/`.
2. Start the server once. Then open `plugins/SilentUI/config.yml` and set `web.public-host` to your server's public IP or domain, and open the `web.port` (default **8163**, TCP) on your firewall or host panel. Players' clients download the resource pack from there.
3. `/sui reload`.

Three example UIs are installed on first start:

- `/menu` opens **example_menu** (a profile menu with 6 tiles).
- **example_hud** shows automatically on join.
- `/sui world place example_board` puts the welcome board in front of you.

Already using ItemsAdder, Nexo, Oraxen or your own pack? Set `resource-pack.send-on-join: false` and merge `plugins/SilentUI/output/SilentUI-pack.zip` into your pack. Everything Silent UI adds lives under the `silentui` namespace, except two transparent bossbar textures.

## The editor

- Run `/sui editor` in game. It links to the editor that the plugin hosts (`http://<public-host>:8163/editor`).
- **Push to server** needs the token in `config.yml → web.upload-token`. It saves the UI and reloads it immediately.
- **Export** gives you the JSON. Put it in `plugins/SilentUI/uis/` and run `/sui reload`.

Shortcuts: `V R T B I N G E H` tools · `A` timeline · `K` scripts · `P` preview · `Ctrl+Z` undo · `Alt+drag` duplicate · `?` help.

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

Events: page opens / closes, clicked (left/right), cursor enters / leaves, every N seconds, key pressed, text typed, command used.
Actions: show / hide / toggle, set text / colour / sprite / progress, move, play / stop animation, open / close UI, ask for text, run command (as player or console), message, broadcast, action bar, title, sound, screen fade, set / add variable, wait.
Logic: if variable, if compare, if permission, random chance (each has True / False outputs).

Text typed by players is stripped of `< > { } % & \ /` before it reaches scripts. Still, don't feed `{text}` into console commands.

## Build (IntelliJ IDEA)

1. **File → Open…** and pick this folder (or `pom.xml`). IntelliJ imports it as a Maven project. Use JDK 21.
2. Run **Build plugin jar** from the run configurations dropdown, or `mvn package` in a terminal.
3. The jar is at `target/SilentUI-1.0.0.jar`.

Every push to `main` is built by GitHub Actions, and the jar is published to the `latest` release.

## Limits worth knowing

- HUD positions are relative to the top centre of the screen at 1 GUI pixel per canvas pixel. HUD text is ASCII only. Items and heads, rotation, scale, opacity and hover don't apply to HUDs.
- Screen UIs move with the crosshair, so the client draws the cursor with zero delay. Clicks and hover are resolved on the server, about one tick behind.
- Bedrock players (Geyser) can't load Java resource packs, so they won't see sprites.
- If something sits 1–2 pixels off on your client, tune `calibration.*` in `config.yml`.
