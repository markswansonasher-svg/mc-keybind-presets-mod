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

Rules for both sides:

* Unknown fields are ignored (so later versions can add some).
* A key `id` the player's game doesn't have is skipped, not an error. That happens when a mod was taken out.
* Keys not in the file keep whatever the player has.
* The whole file is at most 512 KB.
