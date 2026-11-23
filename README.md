# Keybind Presets (Minecraft 1.20.1, Forge + Fabric)

Adds **presets** to Minecraft's Key Binds screen: save whole sets of keys, switch between them, export and import them. A server can offer its own preset, and players with the mod are asked **"Add and switch to the server preset?"** when they join.

**Download:** [the latest release](https://github.com/markswansonasher-svg/mc-keybind-presets-mod/releases/latest) - one jar for Forge, one for Fabric. Put it in your game's `mods` folder.

It was made for the [Apache 4800 cyberdeck](https://github.com/markswansonasher-svg/apache-4800-cyberdeck): you make a modpack's preset in-game, export it, import it on the deck's panel, and the deck puts it on the server. Any server can offer one, though - see [For servers](#for-servers).

## For players

* **Options > Controls > Key Binds**: there's a new **Preset: … ▾** button at the top left. It opens the Presets screen:
  * **Switch to**: use that preset's keys.
  * **Save keys as…**: save the keys you have now as a new preset.
  * **Rename…**, **Delete**
  * **Export**: writes the preset to `config/keybindpresets/exports/` and opens that folder. Share the file, or put it on a server.
  * **Import…**: put a preset file in `config/keybindpresets/import/` (**Open import folder** opens it), then pick it.
* **Default** is always there: every mod's own keys.
* **Your keys are never lost.** If you change keys while a preset is on, that preset is updated when you leave Key Binds. If you change them while Default is on, they're saved as a new preset called "My keys".
* **Joining a server with a preset**: you're asked once. **Add and switch** saves your own keys first, then adds the server's preset and switches to it. **No thanks** isn't asked again unless the server changes its preset. Switch back any time in Key Binds.
* Keys a preset doesn't list keep what you have. Keys for mods you don't have are skipped.
* On Forge, a key can need Shift, Ctrl or Alt held too. Fabric doesn't have that, so it's ignored there.

### The Presets screen

Your presets are on the left. Pick one and the right side shows **what switching to it would change**: every key
that would move (old key → new key), how many keys are for mods you don't have, and any key bundles it adds.
Double-click a preset to switch to it.

### Works with other keybind mods

* **[Controlling](https://www.curseforge.com/minecraft/mc-mods/controlling)**: its search box and the Preset button sit side by side.
* **[KeyBind Bundles](https://www.curseforge.com/minecraft/mc-mods/keybind-bundles)** (Forge): your bundles are saved in your presets and exported with them. Switching to a preset (or a server's preset) adds the bundles it brings that you don't have yet; yours are never changed or removed.

### Languages

All the mod's text is in `assets/keybindpresets/lang/en_us.json`. To translate it, copy that file to your language's
code (for example `de_de.json`), translate the right-hand side, and send it as a pull request.

**Fabric needs Fabric API** (almost every Fabric pack has it). Forge needs nothing extra.

## For servers

Install the mod on the server too, and put the preset at `config/keypresets/server.json` in the server's folder (an exported preset file works as-is). The format is in [docs/keybind-preset-format.md](docs/keybind-preset-format.md). The file is re-read when it changes, so no restart is needed.

The mod is **optional on both sides**:
* players without it can still join a server that has it, and
* players with it can join servers without it.

## Building it

Every push is built and tested by GitHub Actions (the **Actions** tab); the jars are attached to each run.

To build it yourself you need Java 17 (Eclipse Temurin). In this folder:

```powershell
.\gradlew.bat build
```

The mods are in `fabric\build\libs\keybindpresets-fabric-1.20.1-<version>.jar` and `forge\build\libs\keybindpresets-forge-1.20.1-<version>.jar`. Ignore the `-sources` and `-javadoc` jars.

* `.\gradlew.bat :fabric:runClient` (or `:forge:runClient`) starts a test Minecraft with the mod. It uses the `run` folder; put a preset at `run\config\keypresets\server.json` to make single-player worlds offer it, which is handy for trying the prompt.
* `.\gradlew.bat :common:test` runs the unit tests: the preset format, preset storage and the server's preset file. They read `docs/sample-export.json`, which is the same sample file the cyberdeck's tests use, so the mod and the deck's panel stay in step. Keep the two copies identical.

## Getting it into modpacks

A CurseForge modpack can only include mods that are on CurseForge. Until this mod is uploaded there, players add the jar to their game's `mods` folder by hand.

## Code layout

* `common/`: shared code, used by both loaders.
  * `preset/`: the format, storage and file names. Plain Java, unit-tested.
  * `server/ServerPreset.java`: reads `server.json` and packs it for sending.
  * `client/`: the Presets screen, the join prompt, and applying keys.
* `fabric/` and `forge/`: each loader's networking, events and Shift/Ctrl/Alt handling.

Started from jaredlll08's [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).

## Licence

MIT - see [LICENSE](LICENSE).
