# HereShoppy

A comprehensive economy, shop progression, and item generation plugin for [Paper](https://papermc.io).

## Features

- **Centralized Economy**: A robust Bank API (`HereshoppyAPI`) used by other plugins like **HereMobby**.
- **Shop Progression**: A quadratic leveling system ($TotalXP = 5N^2 + 15N$) that gates access to higher-tier items.
- **Dynamic Shop Pricing**: Buy prices scale with the required shop level. Enchantments increase item value by 10% per level.
- **Shipping Bin**: Sell items in bulk using signs labeled `[Shipping Bin]`. Earn 1 Kroin per stack (plus enchant bonuses).
- **Categorized Sales**: Items and prices are defined in YAML files within the `sales/` directory.

## Requirements

- Paper `1.21+` *(plugin `api-version: '1.21'`; built against Paper `1.21.4` API)*
- Java `21`

## Commands & Permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/hereshoppy shop` | Open the tiered shop GUI | `hereshoppy.use` |
| `/hereshoppy info` | View economy stats and leaderboards | `hereshoppy.use` |
| `/hereshoppy reload` | Reload configuration files | `hereshoppy.admin` |

## Configuration

### Sales Data (`sales/`)
Organize items into categories (e.g., `weapons_and_shields.yml`). 
Example entry:
```yaml
DIAMOND_SWORD:
  material: DIAMOND_SWORD
  required_shop_level: 20
```

### Player Data (`data/players.yml`)
Stores balances, XP, and levels for all players.

## API for Developers
Plugins can interact with the economy via `com.hereshoppy.hereshoppy.api.HereshoppyAPI`:
- `getKroins(UUID uuid)`
- `addKroins(UUID uuid, double amount)`
- `removeKroins(UUID uuid, double amount)`
- `getLevel(UUID uuid)`

## License
Licensed under [GNU GPLv3](LICENSE).
