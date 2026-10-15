# Attributioner

Attributioner is a Paper plugin that manages player attributes. It applies attribute modifiers when players enter or leave configured WorldGuard regions and also supports manually assigned attributes for integration with other plugins or systems.

## Features

- Apply attribute modifiers based on WorldGuard regions.
- Attributes added by regions use namespaced keys in the format `attributioner-<region>` to avoid conflicts.
- Manual attribute management API via `AttributeManager` for use by other plugins.
- `/attributioner reload` command reloads the configuration and clears all Attributioner modifiers from online players. Region modifiers are applied again the next time a player enters the region.
- `/attributioner regions` lists all regions and their configured attributes for quick debugging.
- `/attributioner debug` toggles debug logging on or off.
- `/attributioner info <player>` shows all custom attribute modifiers applied to the given player.
- `/attributioner timed ...` applies a modifier directly to a player for a set number of seconds (see [Timed modifiers](#timed-modifiers)).
- `/attributioner apply ...` and `/attributioner remove ...` add and remove modifiers on a player by hand (see [Manual modifiers](#manual-modifiers)).
- Automatically monitors online players every second to detect WorldGuard region changes.

## Commands

| Command | Description |
|---|---|
| `/attributioner regions` | Lists all configured regions and their attributes. |
| `/attributioner debug` | Toggles debug logging. |
| `/attributioner info <player>` | Shows all `attributioner-*` modifiers on a player. |
| `/attributioner reload` | Reloads `config.yml`, cancels all timed modifiers and removes every `attributioner-*` modifier from online players. |
| `/attributioner timed <player> <attribute> <amount> <operation> <seconds>` | Applies a modifier to a player and removes it after `<seconds>`. |
| `/attributioner apply <player> <attribute> <amount> <operation>` | Applies a modifier to a player until it is removed. |
| `/attributioner remove <player> <attribute\|all>` | Removes the manual and timed modifiers for one attribute, or all of them, from a player. |

Running `/attributioner` with no arguments, or with an unknown subcommand, lists every subcommand and its syntax. All commands can be run from the console and over RCON. Tab completion is available for subcommands, player names, attributes and operations.

## Permissions

| Permission | Description |
|---|---|
| `attributioner.admin` | Required for every `/attributioner` subcommand and its tab completion. |

`attributioner.admin` defaults to op. The console and RCON have all permissions.

## Timed modifiers

```
/attributioner timed <player> <attribute> <amount> <operation> <seconds>
/attributioner timed add5tar scale -0.5 ADD_SCALAR 20
```

- `<player>` must be online and is matched by exact name.
- `<attribute>` is looked up in Paper's attribute registry and accepts either `scale` or `minecraft:scale`.
- `<amount>` is any finite number.
- `<operation>` is one of `ADD_NUMBER`, `ADD_SCALAR` or `MULTIPLY_SCALAR_1`.
- `<seconds>` is a whole number greater than 0.

The modifier uses the key `attributioner-timed:<attribute>` (for example `attributioner-timed:scale`) and is removed by a scheduled task once the time is up. Running the command again for the same player and attribute replaces the existing modifier and restarts the timer, so repeated triggers extend the effect rather than stacking. Different attributes have independent timers.

Pending timers are cancelled when the player quits, on `/attributioner reload`, when the plugin is disabled, and by `/attributioner remove`. Because the key is under the `attributioner-` prefix, the existing join, quit and reload cleanup also removes it, so a crash part way through a timer can't leave a player modified after they rejoin.

## Manual modifiers

```
/attributioner apply <player> <attribute> <amount> <operation>
/attributioner apply add5tar scale 1 ADD_SCALAR
/attributioner remove <player> <attribute|all>
/attributioner remove add5tar scale
```

`apply` takes the same arguments as `timed`, without `<seconds>`. The modifier uses the key `attributioner-manual:<attribute>` and stays until it is removed. Applying the same attribute again replaces the existing manual modifier rather than stacking.

`remove` removes the player's manual and timed modifiers for the given attribute and cancels any pending timer for it. `all` does the same for every attribute. Region modifiers are never touched by `remove`, since they are managed by the region tracking. Tab completion for `remove` only suggests attributes the player currently has manual or timed modifiers on.

Manual and timed modifiers have separate keys, so a manual and a timed modifier on the same attribute both apply at once. Manual modifiers are not persistent: like every `attributioner-*` modifier, they are removed when the player quits or joins and on `/attributioner reload`.

## Building

This plugin uses Gradle and targets Java 25. The build always compiles with a JDK 25 toolchain, whichever JDK runs Gradle, so a JDK 25 must be installed. The server must also run on Java 25 or newer. Run:

```bash
./gradlew build
```

The resulting JAR will be placed in `build/libs/`.

## Configuration

Example `config.yml`:

```yaml
regions:
  low_gravity_zone:
    gravity:
      amount: -0.05
      operation: ADD_NUMBER
  fast_zone:
    movement_speed:
      amount: 0.1
      operation: MULTIPLY_SCALAR_1
```

Each key under `regions` corresponds to a WorldGuard region by ID. Attribute names are looked up in Paper's attribute registry, so `gravity` and `minecraft:gravity` both work. Invalid names are logged and skipped. When a player is within that region, the listed attribute modifiers are applied. Attributes are removed when the player leaves the region or when the plugin reloads.

## Usage

1. Install WorldGuard on your Paper server.
2. Place the built Attributioner JAR in the `plugins` directory and start the server.
3. Edit the generated `config.yml` to define your regions and attribute modifiers.
4. Use `/attributioner reload` to reload the configuration without restarting the server.
5. Use `/attributioner regions` to list all configured region attributes.
6. Use `/attributioner debug` to toggle debug logging.
7. Use `/attributioner info <player>` to view custom modifiers currently applied to a player.
8. Use `/attributioner timed <player> <attribute> <amount> <operation> <seconds>` to apply a temporary modifier, e.g. from a stream bot over RCON.
9. Use `/attributioner apply` and `/attributioner remove` to add or remove a modifier by hand.

Attributes added by regions use the key prefix `attributioner-<region>` timed modifiers use `attributioner-timed` and manual modifiers use `attributioner-manual`, so anything under `attributioner-` can be safely removed when needed.
