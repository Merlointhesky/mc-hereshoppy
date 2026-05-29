package com.hereshoppy.hereshoppy.gui;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.model.PlayerData;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ShopGUI {

    public static void openMainMenu(Player player) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        Inventory inv = Bukkit.createInventory(new MainMenuHolder(), 27, Component.text("Shop Categories - Level " + data.getShopLevel()));
        
        Map<String, List<String>> categories = plugin.getItemManager().getCategories();
        int slot = 0;
        
        // Add search item at the beginning
        ItemStack searchItem = new ItemStack(Material.ANVIL);
        ItemMeta searchMeta = searchItem.getItemMeta();
        searchMeta.displayName(Component.text("Search Items", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        searchItem.setItemMeta(searchMeta);
        inv.setItem(26, searchItem); // Put search at the end

        List<String> sortedCategories = categories.keySet().stream().sorted().collect(Collectors.toList());
        
        for (String category : sortedCategories) {
            if (slot >= 26) break;
            
            Material icon = getCategoryIcon(category);
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(formatCategoryName(category), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to view " + formatCategoryName(category), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
    }

    public static void openCategoryMenu(Player player, String category, int page) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getShopLevel();
        
        List<String> allKeys = plugin.getItemManager().getCategories().get(category);
        if (allKeys == null) return;

        // Filter available items and preview items
        List<String> available = new ArrayList<>();
        List<String> preview = new ArrayList<>();
        
        int nextTierLevel = Integer.MAX_VALUE;
        for (String key : allKeys) {
            ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(key);
            if (shopItem == null) continue;
            
            if (shopItem.getRequiredLevel() <= currentLevel) {
                available.add(key);
            } else if (shopItem.getRequiredLevel() < nextTierLevel) {
                nextTierLevel = shopItem.getRequiredLevel();
            }
        }

        if (nextTierLevel != Integer.MAX_VALUE) {
            for (String key : allKeys) {
                ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(key);
                if (shopItem != null && shopItem.getRequiredLevel() == nextTierLevel) {
                    preview.add(key);
                }
            }
        }

        Inventory inv = Bukkit.createInventory(new CategoryMenuHolder(category, page), 54, Component.text(formatCategoryName(category)));

        // Items are in slots 0-44 (5 rows)
        // Preview items are in slots 45-53 (bottom row)
        
        int start = page * 45;
        for (int i = 0; i < 45 && (start + i) < available.size(); i++) {
            String key = available.get(start + i);
            inv.setItem(i, createShopItem(player, key, false));
        }

        // Clean restructuing of Row 6 (slots 45-53)
        ItemStack borderPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPane.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(Component.text(" "));
            borderPane.setItemMeta(borderMeta);
        }
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, borderPane.clone());
        }

        // Navigation
        if (page > 0) {
            inv.setItem(45, createNavItem("Previous Page", Material.ARROW));
        }
        if (available.size() > (page + 1) * 45) {
            inv.setItem(53, createNavItem("Next Page", Material.ARROW));
        }
        
        // Randomise/Refresh button
        if (isRandomizableCategory(category)) {
            ItemStack randomiseItem = new ItemStack(Material.SUNFLOWER);
            ItemMeta randMeta = randomiseItem.getItemMeta();
            if (randMeta != null) {
                randMeta.displayName(Component.text("Refresh Enchants", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
                List<Component> randLore = new ArrayList<>();
                randLore.add(Component.text("Click to randomise stock and enchants.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                randLore.add(Component.text("Keybind: Press F or Q to refresh!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                randMeta.lore(randLore);
                randomiseItem.setItemMeta(randMeta);
            }
            inv.setItem(49, randomiseItem);
        }

        // Back button
        inv.setItem(48, createNavItem("Back to Categories", Material.BARRIER));

        // Previews in dedicated slots: 46, 47, 50, 51, 52
        int[] previewSlots = {46, 47, 50, 51, 52};
        if (currentLevel < 100) {
            for (int i = 0; i < previewSlots.length && i < preview.size(); i++) {
                inv.setItem(previewSlots[i], createShopItem(player, preview.get(i), true));
            }
        }

        player.openInventory(inv);
    }

    public static List<ItemManager.ShopItem> getSearchMatches(SearchFilterState state, int currentLevel) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        List<ItemManager.ShopItem> matches = new ArrayList<>();
        if (state.hasAnyActiveFilter()) {
            for (ItemManager.ShopItem shopItem : plugin.getItemManager().getAllItems().values()) {
                // Apply category filter
                if (state.getCategory() != null) {
                    String cat = findCategoryForKey(shopItem.getConfigKey());
                    if (!cat.equalsIgnoreCase(state.getCategory())) continue;
                }

                // Apply starting letter filter
                if (state.getLetter() != null) {
                    String name = formatCategoryItemName(shopItem);
                    if (name == null || name.isEmpty() || !name.substring(0, 1).equalsIgnoreCase(state.getLetter())) {
                        continue;
                    }
                }

                // Apply level range filter
                if (!state.getLevelRange().equals("ALL")) {
                    int req = shopItem.getRequiredLevel();
                    boolean matchesLevel = switch (state.getLevelRange()) {
                        case "1-20" -> req >= 1 && req <= 20;
                        case "21-50" -> req >= 21 && req <= 50;
                        case "51-80" -> req >= 51 && req <= 80;
                        case "81-100" -> req >= 81 && req <= 100;
                        default -> true;
                    };
                    if (!matchesLevel) continue;
                }

                // Apply availability filter
                if (!state.getAvailability().equals("ALL")) {
                    boolean canBuy = shopItem.getRequiredLevel() <= currentLevel;
                    if (state.getAvailability().equals("PURCHASABLE") && !canBuy) continue;
                    if (state.getAvailability().equals("LOCKED") && canBuy) continue;
                }

                // Apply text query filter
                if (state.getQuery() != null) {
                    String q = state.getQuery().toLowerCase();
                    String itemKey = shopItem.getConfigKey().toLowerCase();
                    boolean match = itemKey.contains(q) || 
                                    shopItem.getMaterial().name().toLowerCase().contains(q);
                    
                    if (!match && shopItem.getPotionType() != null) {
                        match = shopItem.getPotionType().toLowerCase().contains(q);
                    }
                    if (!match && shopItem.getEnchantType() != null) {
                        match = shopItem.getEnchantType().toLowerCase().contains(q);
                    }
                    if (!match) continue;
                }

                matches.add(shopItem);
            }

            // Sort matches: purchasable first, then by required level (ascending), then by config key
            matches.sort((a, b) -> {
                boolean aCanBuy = a.getRequiredLevel() <= currentLevel;
                boolean bCanBuy = b.getRequiredLevel() <= currentLevel;
                if (aCanBuy != bCanBuy) {
                    return aCanBuy ? -1 : 1;
                }
                int levelComp = Integer.compare(a.getRequiredLevel(), b.getRequiredLevel());
                if (levelComp != 0) return levelComp;
                return a.getConfigKey().compareTo(b.getConfigKey());
            });
        }
        return matches;
    }

    public static void openSearchMenu(Player player, SearchFilterState state) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int currentLevel = data.getShopLevel();
        
        List<ItemManager.ShopItem> matches = getSearchMatches(state, currentLevel);

        Inventory inv = Bukkit.createInventory(new SearchMenuHolder(state), 54, Component.text("Shop Search & Filter"));
        
        if (!state.hasAnyActiveFilter()) {
            ItemStack guideBook = new ItemStack(Material.BOOK);
            ItemMeta guideMeta = guideBook.getItemMeta();
            if (guideMeta != null) {
                guideMeta.displayName(Component.text("Shop Search & Filter Dashboard", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                List<Component> guideLore = new ArrayList<>();
                guideLore.add(Component.text("Select a filter below to search the shop.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                guideLore.add(Component.text("Click the controls at the bottom to start!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                guideMeta.lore(guideLore);
                guideBook.setItemMeta(guideMeta);
            }
            inv.setItem(22, guideBook);
        } else {
            int start = state.getPage() * 45;
            for (int i = 0; i < 45 && (start + i) < matches.size(); i++) {
                ItemManager.ShopItem shopItem = matches.get(start + i);
                inv.setItem(i, createShopItem(player, shopItem.getConfigKey(), shopItem.getRequiredLevel() > currentLevel));
            }
        }
        
        // Slot 45: Name Search (Compass)
        ItemStack nameSearch = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = nameSearch.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("🔍 Name Search", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> searchLore = new ArrayList<>();
            searchLore.add(Component.text("Current: " + (state.getQuery() != null ? state.getQuery() : "None"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            searchLore.add(Component.text("Click to search by word in chat", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            searchMeta.lore(searchLore);
            nameSearch.setItemMeta(searchMeta);
        }
        inv.setItem(45, nameSearch);

        // Slot 46: Starting Letter (Paper)
        ItemStack letterSearch = new ItemStack(Material.PAPER);
        ItemMeta letterMeta = letterSearch.getItemMeta();
        if (letterMeta != null) {
            letterMeta.displayName(Component.text("🔠 Starting Letter Filter", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> letterLore = new ArrayList<>();
            letterLore.add(Component.text("Current: " + (state.getLetter() != null ? state.getLetter().toUpperCase() : "None"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            letterLore.add(Component.text("Click to select starting letter", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            letterMeta.lore(letterLore);
            letterSearch.setItemMeta(letterMeta);
        }
        inv.setItem(46, letterSearch);

        // Slot 47: Category Filter (Chest)
        ItemStack catSearch = new ItemStack(Material.CHEST);
        ItemMeta catMeta = catSearch.getItemMeta();
        if (catMeta != null) {
            catMeta.displayName(Component.text("📦 Category Filter", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> catLore = new ArrayList<>();
            catLore.add(Component.text("Current: " + (state.getCategory() != null ? formatCategoryName(state.getCategory()) : "All Categories"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            catLore.add(Component.text("Click to cycle categories", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            catMeta.lore(catLore);
            catSearch.setItemMeta(catMeta);
        }
        inv.setItem(47, catSearch);

        // Slot 48: Level Filter (Experience Bottle)
        ItemStack levelSearch = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta levelMeta = levelSearch.getItemMeta();
        if (levelMeta != null) {
            levelMeta.displayName(Component.text("🧪 Level Range Filter", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> levelLore = new ArrayList<>();
            levelLore.add(Component.text("Current: " + state.getLevelRange(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            levelLore.add(Component.text("Click to cycle level ranges", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            levelMeta.lore(levelLore);
            levelSearch.setItemMeta(levelMeta);
        }
        inv.setItem(48, levelSearch);

        // Slot 49: Availability Filter (Lever)
        ItemStack availSearch = new ItemStack(Material.LEVER);
        ItemMeta availMeta = availSearch.getItemMeta();
        if (availMeta != null) {
            availMeta.displayName(Component.text("⚙️ Availability Filter", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            List<Component> availLore = new ArrayList<>();
            availLore.add(Component.text("Current: " + state.getAvailability(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            availLore.add(Component.text("Click to cycle availability", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            availMeta.lore(availLore);
            availSearch.setItemMeta(availMeta);
        }
        inv.setItem(49, availSearch);

        // Slot 50: Reset Filters (Redstone Dust)
        ItemStack resetSearch = new ItemStack(Material.REDSTONE);
        ItemMeta resetMeta = resetSearch.getItemMeta();
        if (resetMeta != null) {
            resetMeta.displayName(Component.text("❌ Reset Filters", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            List<Component> resetLore = new ArrayList<>();
            resetLore.add(Component.text("Click to clear all active filters", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            resetMeta.lore(resetLore);
            resetSearch.setItemMeta(resetMeta);
        }
        inv.setItem(50, resetSearch);

        // Slot 51: Previous Page (Arrow)
        if (state.getPage() > 0) {
            inv.setItem(51, createNavItem("Previous Page", Material.ARROW));
        }

        // Slot 52: Next Page (Arrow)
        if (matches.size() > (state.getPage() + 1) * 45) {
            inv.setItem(52, createNavItem("Next Page", Material.ARROW));
        }

        // Slot 53: Go Back (Barrier)
        inv.setItem(53, createNavItem("Back to Categories", Material.BARRIER));

        player.openInventory(inv);
    }

    public static void openAlphabetMenu(Player player, SearchFilterState state) {
        Inventory inv = Bukkit.createInventory(new AlphabetMenuHolder(state), 27, Component.text("Select Starting Letter"));
        
        char letter = 'A';
        for (int i = 0; i < 26; i++) {
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Letter: " + letter, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
                paper.setItemMeta(meta);
            }
            inv.setItem(i, paper);
            letter++;
        }
        
        ItemStack clear = new ItemStack(Material.BARRIER);
        ItemMeta clearMeta = clear.getItemMeta();
        if (clearMeta != null) {
            clearMeta.displayName(Component.text("Clear Starting Letter Filter", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            clear.setItemMeta(clearMeta);
        }
        inv.setItem(26, clear);
        
        player.openInventory(inv);
    }

    private static String formatCategoryItemName(ItemManager.ShopItem shopItem) {
        String key = shopItem.getConfigKey();
        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            key = parts[parts.length - 1];
        }
        return key.replaceAll("_", " ");
    }

    private static ItemStack createShopItem(Player player, String itemKey, boolean isPreview) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(itemKey);
        if (shopItem == null) return new ItemStack(Material.BARRIER);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        
        ItemStack item = shopItem.createItemStack(plugin);
        
        // Add random enchants if it's a randomizable category
        if (!isPreview && isRandomizableCategory(findCategoryForKey(itemKey))) {
            addRandomEnchantments(item, data.getShopLevel());
        }
        
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        
        if (isPreview) {
            lore.add(Component.text("LOCKED - Available at level " + shopItem.getRequiredLevel(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            double price = plugin.getItemManager().calculateBuyPrice(itemKey, item);
            lore.add(Component.text("Price: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.format("%.2f", price) + " Kroins", NamedTextColor.YELLOW)));
            lore.add(Component.text("Click to purchase stack", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String findCategoryForKey(String itemKey) {
        if (itemKey != null && itemKey.contains(".")) {
            return itemKey.split("\\.")[0];
        }
        return "other";
    }

    private static void addRandomEnchantments(ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return; // Safety check in case the item doesn't support metadata
        
        Random random = new Random();
        
        // Filter out items it can't enchant AND curses
        List<Enchantment> possible = Arrays.stream(Enchantment.values())
                .filter(e -> e.canEnchantItem(item))
                .filter(e -> !e.isCursed()) // This filters out the curses
                .collect(Collectors.toList());
        
        if (possible.isEmpty()) return;
        
        int numEnchants = random.nextInt(Math.min(3, level / 20 + 1)) + 1;
        
        for (int i = 0; i < numEnchants; i++) {
            // Break out of the loop if we run out of possible enchantments
            if (possible.isEmpty()) break; 
            
            int randomIndex = random.nextInt(possible.size());
            Enchantment ench = possible.get(randomIndex);
            
            int max = Math.min(ench.getMaxLevel(), level / 20 + 1);
            int enchLevel = random.nextInt(Math.max(1, max)) + 1; // Ensure bound is positive
            
            meta.addEnchant(ench, enchLevel, true);
            
            // Remove the applied enchantment so the loop doesn't select it again
            possible.remove(randomIndex);
        }
        
        item.setItemMeta(meta);
    }

    private static ItemStack createNavItem(String name, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private static String formatCategoryName(String key) {
        return Arrays.stream(key.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static Material getCategoryIcon(String category) {
        return switch (category) {
            case "weapons_and_shields" -> Material.DIAMOND_SWORD;
            case "armor" -> Material.DIAMOND_CHESTPLATE;
            case "tools_and_elytra" -> Material.NETHERITE_PICKAXE;
            case "fruit_and_veg" -> Material.APPLE;
            case "meat_and_fish" -> Material.COOKED_BEEF;
            case "wooden_items" -> Material.OAK_LOG;
            case "metal_items" -> Material.IRON_INGOT;
            case "ores_and_minerals" -> Material.DIAMOND;
            case "seeds_and_saplings" -> Material.OAK_SAPLING;
            case "potions" -> Material.POTION;
            case "enchants" -> Material.ENCHANTED_BOOK;
            case "building_blocks" -> Material.BRICKS;
            case "redstone" -> Material.REDSTONE;
            case "mob_drops" -> Material.ROTTEN_FLESH;
            case "dyes" -> Material.ORANGE_DYE;
            case "materials" -> Material.CLAY_BALL;
            default -> Material.CHEST;
        };
    }

    public static boolean isRandomizableCategory(String category) {
        return category.equals("weapons_and_shields") || category.equals("armor") || category.equals("tools_and_elytra");
    }

    public static class MainMenuHolder implements InventoryHolder {
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public static class CategoryMenuHolder implements InventoryHolder {
        private final String category;
        private final int page;
        public CategoryMenuHolder(String category, int page) { this.category = category; this.page = page; }
        public String getCategory() { return category; }
        public int getPage() { return page; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public static class SearchFilterState {
        private String query = null;
        private String letter = null;
        private String category = null;
        private String levelRange = "ALL";
        private String availability = "ALL";
        private int page = 0;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; this.page = 0; }

        public String getLetter() { return letter; }
        public void setLetter(String letter) { this.letter = letter; this.page = 0; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; this.page = 0; }

        public String getLevelRange() { return levelRange; }
        public void setLevelRange(String levelRange) { this.levelRange = levelRange; this.page = 0; }

        public String getAvailability() { return availability; }
        public void setAvailability(String availability) { this.availability = availability; this.page = 0; }

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public boolean hasAnyActiveFilter() {
            return query != null || letter != null || category != null || !levelRange.equals("ALL") || !availability.equals("ALL");
        }

        public void reset() {
            query = null;
            letter = null;
            category = null;
            levelRange = "ALL";
            availability = "ALL";
            page = 0;
        }
    }

    public static class SearchMenuHolder implements InventoryHolder {
        private final SearchFilterState state;
        public SearchMenuHolder(SearchFilterState state) { this.state = state; }
        public SearchFilterState getState() { return state; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }

    public static class AlphabetMenuHolder implements InventoryHolder {
        private final SearchFilterState state;
        public AlphabetMenuHolder(SearchFilterState state) { this.state = state; }
        public SearchFilterState getState() { return state; }
        @Override public @NotNull Inventory getInventory() { return null; }
    }
}
