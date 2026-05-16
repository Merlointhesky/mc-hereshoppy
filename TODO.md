# Plugin Specification: Hereshoppy

## 1. Overview
**Name:** Hereshoppy
**API Version:** Paper 1.20+
**Description:** A comprehensive economy, shop progression, and item generation plugin. It serves as the central "Bank" API, completely replacing `heremobby`'s economy. It features a tiered, categorized item shop with Anvil search, a dynamic enchantment engine, a staggered quadratic leveling system to gatekeep high-tier items, and allows players to sell items via "Shipping Bin" signs.

## 2. Transition from 'heremobby'
The existing `heremobby` plugin must be refactored:
*   Remove all internal economy, balance tracking, and shop GUI logic from `heremobby`.
*   Make `heremobby` depend on `hereshoppy`.
*   When a mob/boss is killed, `heremobby` will calculate the reward and simply call `HereshoppyAPI.addKroins(playerUUID, amount)`.

## 3. Core Architecture & Data Models
Centralize player data persistently (e.g., `plugins/Hereshoppy/data/players.yml` or SQLite).

**Player Data Object:**
*   `uuid` (String/UUID)
*   `balance` (Integer/Double) - Current spendable Kroins.
*   `shop_xp` (Integer) - Total Kroins spent over a lifetime. 1 Kroin spent = 1 XP.
*   `shop_level` (Integer) - Calculated dynamically based on `shop_xp`.
*   `lifetime_earned` (Integer/Double) - Total Kroins generated from all sources.

## 4. The Economy & Leveling API
Create a public API class `HereshoppyAPI` for internal use and external plugins:
*   `getKroins(UUID uuid)`
*   `addKroins(UUID uuid, int amount)` (Adds to `balance` AND increments `lifetime_earned`)
*   `removeKroins(UUID uuid, int amount)` (Deducts from `balance`)
*   `addShopXp(UUID uuid, int amount)` (Increments `shop_xp` and triggers a level-up check/sound if a new level is reached).

## 5. Extensive File Configuration (`plugins/Hereshoppy/sales/`)
All prices and shop tiers are categorized into specific files inside a `sales/` folder.
*Properties per item:* `material`, `sell_price`, `buy_price`, `required_shop_level`.

**Required Files:**
*   `weapons_and_shields.yml`
*   `armor.yml`
*   `tools_and_elytra.yml`
*   `fruit_and_veg.yml`
*   `meat_and_fish.yml`
*   `wooden_items.yml` 
*   `metal_items.yml` 
*   `ores_and_minerals.yml` 
*   `seeds_and_saplings.yml`
*   `potions.yml`
*   `building_blocks.yml` 
*   `redstone.yml`
*   `mob_drops.yml`
*   `other_items.yml`

## 6. The Leveling System (Quadratic Progression)
The plugin must calculate a player's `shop_level` dynamically based on their lifetime `shop_xp`. The cost scales linearly (+10 XP per level), meaning the total XP required follows a quadratic curve. 

**Implementation Math:**
Use this exact derivation to calculate levels without needing a massive config file:
```java
public class LevelingMath {
    public static int calculateLevel(int totalXp) {
        if (totalXp < 20) return 0;
        // Derived from TotalXP = 5N^2 + 15N
        double level = (-15.0 + Math.sqrt(225.0 + (20.0 * totalXp))) / 10.0;
        return (int) Math.floor(level);
    }
}