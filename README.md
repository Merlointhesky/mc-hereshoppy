# HereShoppy

A comprehensive economy, shop progression, and item generation plugin for [Paper](https://papermc.io).

## Features

- **Centralized Economy**: A robust Bank API (`HereshoppyAPI`) used by other plugins like **HereMobby**.
- **Shop Progression**: A quadratic leveling system ($TotalXP = 5N^2 + 15N$) that gates access to higher-tier items.
- **Dynamic Shop Pricing**: Buy prices scale with the required shop level. Enchantments increase item value by 10% per level.
- **Shipping Bin**: A virtual sales chest accessed via signs labeled `[Here Sell!]`. Sell any item for 1 Kroin per stack (plus enchant bonuses).
- **Categorized Sales**: Items and prices are defined in YAML files within the `sales/` directory.

## Requirements

- Paper `1.21+` *(plugin `api-version: '1.21'`; built against Paper `1.21.4` API)*
- Java `21`

## Setup & Usage

### Shipping Bin (Sales Chest)
The Shipping Bin allows players to sell items in bulk for a flat rate.

#### Virtual Shipping Bin
1. Place a **Sign** on any block.
2. Write `[Here Sell!]` on the **first line** (front or back).
3. **Right-click** the sign to open a virtual GUI.
4. Place items inside and **close the inventory** to receive payment.

#### Physical Shipping Bin (Automated)
1. Place a **Sign** on a **Chest, Barrel, or other Container**.
2. Write `[Here Sell!]` on the **first line**.
3. Any items placed in this container (by players, hoppers, or other plugins like **HereCroppy**) will be sold automatically every 5 seconds.
4. **Ownership**: The player who placed the sign will receive the payment.

**Pricing**: 1 Kroin per stack/slot + 10% bonus per enchantment level.

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
