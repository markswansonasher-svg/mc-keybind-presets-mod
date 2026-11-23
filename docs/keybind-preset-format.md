# Keybind preset file (format `cyberdeck-keys/1`)

One JSON file = one set of key bindings for one modpack. The same file is:

* **exported** by the keybind mod in-game (all keys the pack has, with what you set),
* **imported and edited** on the Deck Panel (Modpacks > Keybind preset),
* **put on the server** by the deck at `<server folder>/config/keypresets/server.json`,
* **sent to players** by the keybind mod on the server when they join. On their first join it asks
  "Add and switch to the server preset?". Yes adds it to their preset list and switches to it.

The deck and the mod (phase 5: Forge + Fabric, Minecraft 1.20.1) must both follow this page.

## Example

```json
{
  "format": "cyberdeck-keys/1",
  "name": "Cozy Pack",
  "minecraft": "1.20.1",
  "keys": [
    {"id": "key.jump",           "key": "key.keyboard.space", "modifier": "NONE",
     "default": "key.keyboard.space", "label": "Jump", "category": "key.categories.movement"},
    {"id": "key.jei.showRecipe", "key": "key.keyboard.r",     "modifier": "SHIFT",
     "default": "key.keyboard.r", "label": "Show Recipe", "category": "jei.key.category"}
  ]
}
```

## Fields

| Field | Required | What it is |
|---|---|---|
| `format` | yes | Always `cyberdeck-keys/1`. A future change gets a new number. |
| `name` | yes | Shown to players in the join prompt and their preset list. 1-60 characters. |
| `minecraft` | no | Minecraft version it was made on, e.g. `1.20.1`. |
| `keys` | yes | The bindings. At most 3000. |
| `keys[].id` | yes | The key mapping's name, exactly as in `options.txt` without the `key_` start: `key.jump`, `key.jei.showRecipe`. Letters, digits and `_ . : / -`, up to 120. |
| `keys[].key` | yes | The key or mouse button, as Minecraft names it: `key.keyboard.r`, `key.keyboard.left.shift`, `key.mouse.right`, `key.mouse.4`. Unbound is `key.keyboard.unknown`. |
| `keys[].modifier` | no | `NONE` (default), `SHIFT`, `CONTROL` or `ALT`. Forge supports it; on Fabric it's ignored. |
| `keys[].default` | no | The mod's own default key, so "reset" works on the panel. |
| `keys[].label` | no | Human name ("Show Recipe"), up to 120 characters. Display only. |
| `keys[].category` | no | Minecraft category name, for grouping on the panel. Display only. |
| `bundles` | no | Key bundles for the [KeyBind Bundles](https://www.curseforge.com/minecraft/mc-mods/keybind-bundles) mod (Forge). See below. |

### Key bundles (optional)

KeyBind Bundles turns one key into a radial menu of other keys. A preset can carry those bundles, so a server can hand
them out with its keys. The mod saves them when it saves your keys (if KeyBind Bundles is installed) and, when you switch
to a preset, adds the bundles you don't have yet. Bundles you already have are never changed or removed.

```json
"bundles": [
  {"id": "0f8fad5b-d9cb-469f-a165-70867728950e", "name": "Maps", "bookmark": 0,
   "entries": [
     {"key": "key.minimap.waypoint", "title": "Waypoint", "icon": "minecraft:compass"},
     {"key": "key.jei.showRecipe",   "title": "Recipe"}
   ]}
]
```

| Field | Required | What it is |
|---|---|---|
| `bundles[].id` | yes | The bundle's UUID. Its own key is `key.keybindbundles.bundle_<id>` in `keys`. |
| `bundles[].name` | no | Shown in the game, up to 60 characters. |
| `bundles[].entries` | no | Up to 100. `key` is a key `id` (same rule as `keys[].id`), `title` up to 120 characters, `icon` an item id like `minecraft:compass`. |
| `bundles[].bookmark` | no | Which entry (counting from 0) a quick tap uses; -1 or missing = none. |

At most 100 bundles. A broken bundle or entry is left out instead of refusing the file.

Rules for both sides:

* Unknown fields are ignored (so later versions can add some).
* A key `id` the player's game doesn't have is skipped, not an error. That happens when a mod was taken out.
* Keys not in the file keep whatever the player has.
* The whole file is at most 512 KB.
