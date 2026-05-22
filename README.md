# HereShoppy

A comprehensive economy, shop progression, and item generation plugin for [Paper](https://papermc.io).

## Features

- **Centralized Economy**: A robust Bank API (`HereshoppyAPI`) used by other plugins like **HereMobby**.
- **Shop Progression**: A quadratic leveling system ($TotalXP = 5N^2 + 15N$) that gates access to higher-tier items.
- **Dynamic Shop Pricing**: Buy prices scale with the required shop level. Enchantments increase item value by 10% per level.
- **Interactive Search & Filter Dashboard**: Click the Search anvil to open a comprehensive 54-slot Chest GUI dashboard. Filter items dynamically by name query (via private chat session), starting letter (A-Z selector drawer), category, required level range, or availability. Matches are sorted dynamically (purchasable items first, locked preview items after) for a premium, zero-experience-cost search!
- **Shipping Bin**: A virtual sales chest accessed via signs labeled `[Here Sell!]`. Sell any item for 1 Kroin per stack (plus enchant bonuses).
- **Categorized Sales**: Items and prices are defined in YAML files within the `sales/` directory.
- **Preview Gated Items**: View items above your current level right inside their categories as previews. Search results also list all matches, sorting purchasable items first and locked preview items immediately after.

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
| `/hereshoppy shop` (or `/hs shop`, `/shop`) | Open the tiered shop GUI | `hereshoppy.use` |
| `/hereshoppy info` (or `/hs info`) | View economy stats and leaderboards | `hereshoppy.use` |
| `/hereshoppy reload` (or `/hs reload`) | Reload configuration files | `hereshoppy.admin` |

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

## Recent Changes (v1.2.3)

- **Modern Items Catalog Synchronization** — Synchronized the shop catalog with the latest Minecraft features (1.21 Tricky Trials & 26.1 "Tiny Takeover" visual/QoL drop), introducing **Wind Charge** (`WIND_CHARGE`) to the mob drops category and the **Golden Dandelion** (`GOLDEN_DANDELION`) age-halting flower to the flower category.
- **Robust Auto-Merging Configurations** — Built-in automated synchronization detects missing default items on plugin startup and safely merges them into existing server configuration files, maintaining custom economy balances.

## Previous Changes (v1.2.2)

- **Search & Filter Dashboard GUI** — Replaced the virtual Anvil search GUI with a comprehensive, highly interactive 54-slot Chest GUI.
- **Dynamic Multi-Criteria Filters** — Implemented concurrent filtering using Compass (Private Chat Name Search), Paper (A-Z starting letter selector drawer), Chest (Category cycle), Exp Bottle (Level range cycle: 1-20, 21-50, 51-80, 81-100), and Lever (Availability cycle: Purchasable vs Locked).
- **Private Chat Capture Session** — Captures player name queries privately in chat, cancelling the chat event to prevent broadcasting, then seamlessly re-opens the dashboard.
- **Session-Based State Management** — Utilizes a non-persistent session-based state tracker associated with the inventory holder, preventing server memory leaks.

## Older Changes (v1.2.1)

- **Anvil Search GUI Integration** — Replaced the legacy chat-based search prompt with a custom virtual Anvil typing GUI. Typed search terms are captured natively and processed with zero experience cost to the player.
- **Deep Recursive Merging** — Replaced the flat key merge utility with a recursive, deep-merge system. Newly introduced default items and property fields are merged seamlessly into existing server `.yml` configurations without overwriting admin custom edits or being skipped.
- **Smart Search Matching & Preview Sorting** — Restructured search results to return matches across keys, materials, potions, and enchants. Matches now sort purchasable items first, followed by locked preview items, providing clear, immersive visual feedback.

- **PersistentDataContainer unique key refactor** — Solved a Spigot limitation where multiple items sharing the same Material type (like potions and books) overwrite each other in the shop registry. Tagged GUI items with a NamespacedKey `"shop_item_key"` for robust click-to-purchase resolution.
- **Massive Potion Catalog** — Added an exhaustive selection of potion types (standard 3m, long 8m, strong II) including 1.21 Trial Chamber effects (Infested & Oozing at level 80; Weaving & Wind Charged at level 90) under the single combined `potions` category.
- **Enchanted Books Section** — Added a combined single enchanted book category ranging from basic level 20 utility enchants to level 99 Mace trial enchants (*Wind Burst III*), presented in the GUI with premium Roman numeral display names.
- **Exclusions** — Explicitly kept dragon eggs and music discs out of the shop to preserve exploration incentive.

## Version

Current release: **`1.2.3`**

## License
Licensed under [GNU GPLv3](LICENSE).
