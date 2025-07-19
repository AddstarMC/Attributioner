# Attributioner

Attributioner is a Paper plugin that manages player attributes. It applies attribute modifiers when players enter or leave configured WorldGuard regions and also supports manually assigned attributes for integration with other plugins or systems.

## Features

- Apply attribute modifiers based on WorldGuard regions.
- Attributes added by regions use namespaced keys in the format `attributioner-<region>` to avoid conflicts.
- Manual attribute management API via `AttributeManager` for use by other plugins.
- `/attributioner reload` command reloads the configuration and reapplies region modifiers for online players.
- `/attributioner regions` lists all regions and their configured attributes for quick debugging.
- `/attributioner debug` toggles debug logging on or off.
- `/attributioner info <player>` shows all custom attribute modifiers applied to the given player.

## Building

This plugin uses Gradle. Run:

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

Each key under `regions` corresponds to a WorldGuard region by ID. When a player is within that region, the listed attribute modifiers are applied. Attributes are removed when the player leaves the region or when the plugin reloads.

## Usage

1. Install WorldGuard and RegionEvents on your Paper server.
2. Place the built Attributioner JAR in the `plugins` directory and start the server.
3. Edit the generated `config.yml` to define your regions and attribute modifiers.
4. Use `/attributioner reload` to reload the configuration without restarting the server.
5. Use `/attributioner regions` to list all configured region attributes.
6. Use `/attributioner debug` to toggle debug logging.
7. Use `/attributioner info <player>` to view custom modifiers currently applied to a player.

Attributes added programmatically or by regions share the same key prefix (`attributioner-<region>`), ensuring they can be safely removed when needed.
