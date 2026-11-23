# Keybind Presets (Minecraft 1.20.1, Forge + Fabric)

Adds **presets** to Minecraft's Key Binds screen: save whole sets of keys, switch between them, export and import them. A server can offer its own preset, and players with the mod are asked **"Add and switch to the server preset?"** when they join.

It works with the cyberdeck: you make a pack's preset in-game, export it, import it on the Deck Panel (Modpacks > Keybind preset), and the deck puts it on the server.

**Download:** [Keybind Presets 1.0.0 on the Releases page](https://github.com/markswansonasher-svg/apache-4800-cyberdeck/releases/tag/keybind-presets-v1.0.0) (one jar for Forge, one for Fabric). Put it in your game's `mods` folder.

## For players

* **Options > Controls > Key Binds**: there's a new **Preset: … ▾** button at the top left. It opens the Presets screen:
  * **Switch to**: use that preset's keys.
  * **Save keys as…**: save the keys you have now as a new preset.
  * **Rename…**, **Delete**
  * **Export**: writes the preset to `config/keybindpresets/exports/` and opens that folder. Share the file, or import it on the Deck Panel.
  * **Import…**: put a preset file in `config/keybindpresets/import/` (**Open import folder** opens it), then pick it.
* **Default** is always there: every mod's own keys.
* **Your keys are never lost.** If you change keys while a preset is on, that preset is updated when you leave Key Binds. If you change them while Default is on, they're saved as a new preset called "My keys".
* **Joining a server with a preset**: you're asked once. **Add and switch** saves your own keys first, then adds the server's preset and switches to it. **No thanks** isn't asked again unless the server changes its preset. Switch back any time in Key Binds.
* Keys a preset doesn't list keep what you have. Keys for mods you don't have are skipped.
* On Forge, a key can need Shift, Ctrl or Alt held too. Fabric doesn't have that, so it's ignored there.

## For servers

Put the preset at `config/keypresets/server.json` in the server's folder (the cyberdeck does this for you). The format is in the cyberdeck's `docs/keybind-preset-format.md`. The file is re-read when it changes, so no restart is needed.

The mod is **optional on both sides**:
* players without it can still join a server that has it, and
* players with it can join servers without it.

## Building it

You need Java 17 (Eclipse Temurin). In this folder:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.21.10002-hotspot'
.\gradlew.bat build
```

The mods are in `fabric\build\libs\keybindpresets-fabric-1.20.1-<version>.jar` and `forge\build\libs\keybindpresets-forge-1.20.1-<version>.jar`. Ignore the `-sources` and `-javadoc` jars.

* `.\gradlew.bat :fabric:runClient` (or `:forge:runClient`) starts a test Minecraft with the mod. It uses the `run` folder, and `run\config\keypresets\server.json` makes single-player worlds offer that preset, which is handy for trying the prompt. (Not run on the personal laptop.)
* `.\gradlew.bat :common:test` runs the unit tests: the preset format, preset storage and the server's preset file. They also read the cyberdeck's `tests\presets\sample-export.json`, so the mod and the Deck Panel stay in step.

**Fabric needs Fabric API** (almost every Fabric pack has it). Forge needs nothing extra.

## Getting it into your packs

A CurseForge modpack can only include mods that are on CurseForge. To ship this mod inside your packs, upload both jars to CurseForge as a project (free account). Until then, players add the jar to their game's `mods` folder by hand.

## Code layout

* `common/`: shared code, used by both loaders.
  * `preset/`: the format, storage and file names. Plain Java, unit-tested.
  * `server/ServerPreset.java`: reads `server.json` and packs it for sending.
  * `client/`: the Presets screen, the join prompt, and applying keys.
* `fabric/` and `forge/`: each loader's networking, events and Shift/Ctrl/Alt handling.

Built from the MultiLoader-Template by Jared (MIT licence, see LICENSE).
