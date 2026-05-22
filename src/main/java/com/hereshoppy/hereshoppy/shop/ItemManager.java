package com.hereshoppy.hereshoppy.shop;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {
    private final HereShoppyPlugin plugin;
    private final Map<String, ShopItem> items = new HashMap<>();
    private final Map<String, List<String>> categories = new HashMap<>();

    public ItemManager(HereShoppyPlugin plugin) {
        this.plugin = plugin;
        loadItems();
    }

    public void loadItems() {
        items.clear();
        categories.clear();
        File salesDir = new File(plugin.getDataFolder(), "sales");
        if (!salesDir.exists()) {
            salesDir.mkdirs();
        }
        createDefaultSalesFiles(salesDir);

        File[] files = salesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String categoryName = file.getName().replace(".yml", "");
                List<String> categoryItems = new ArrayList<>();
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                for (String key : config.getKeys(false)) {
                    ConfigurationSection mainSection = config.getConfigurationSection(key);
                    if (mainSection != null) {
                        // Check if it's a direct item or a category of materials
                        if (mainSection.contains("material")) {
                            // Direct item
                            loadItem(mainSection, key, categoryItems, file.getName());
                        } else {
                            // Nested materials
                            for (String subKey : mainSection.getKeys(false)) {
                                ConfigurationSection subSection = mainSection.getConfigurationSection(subKey);
                                if (subSection != null) {
                                    loadItem(subSection, subKey, categoryItems, file.getName());
                                }
                            }
                        }
                    }
                }
                categories.put(categoryName, categoryItems);
            }
        }
    }

    private void loadItem(ConfigurationSection section, String key, List<String> categoryItems, String fileName) {
        try {
            String categoryName = fileName.replace(".yml", "");
            String itemKey = categoryName + "." + key;

            Material material = Material.valueOf(section.getString("material", key).toUpperCase());
            int requiredLevel = section.getInt("required_shop_level", 0);
            
            // Calculate buy price: Level 0 -> 1, Level N -> N
            double buyPrice = (requiredLevel == 0) ? 1 : requiredLevel;
            
            String potionType = section.getString("potion_type");
            String enchantType = section.getString("enchant_type");
            int enchantLevel = section.getInt("enchant_level", 1);

            ShopItem item = new ShopItem(itemKey, material, buyPrice, requiredLevel, potionType, enchantType, enchantLevel);
            items.put(itemKey, item);
            categoryItems.add(itemKey);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in " + fileName + ": " + key);
        }
    }

    private void createDefaultSalesFiles(File salesDir) {
        String[] defaultFiles = {
            "weapons_and_shields.yml", "armor.yml", "tools_and_elytra.yml",
            "fruit_and_veg.yml", "meat_and_fish.yml", "wooden_items.yml",
            "metal_items.yml", "ores_and_minerals.yml", "seeds_and_saplings.yml",
            "potions.yml", "building_blocks.yml", "redstone.yml",
            "mob_drops.yml", "other_items.yml", "enchants.yml"
        };
        for (String fileName : defaultFiles) {
            File file = new File(salesDir, fileName);
            if (!file.exists()) {
                try {
                    plugin.saveResource("sales/" + fileName, false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save " + fileName + " from resources, creating empty file.");
                    try {
                        file.createNewFile();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            } else {
                // Safely merge any missing default items from the jar into the existing config
                mergeMissingDefaults(file, "sales/" + fileName);
            }
        }
    }

    private void mergeMissingDefaults(File localFile, String resourcePath) {
        try {
            YamlConfiguration localConfig = YamlConfiguration.loadConfiguration(localFile);
            
            // Load the default resource stream from the jar
            java.io.InputStream resourceStream = plugin.getResource(resourcePath);
            if (resourceStream == null) return;
            
            YamlConfiguration jarConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(resourceStream, java.nio.charset.StandardCharsets.UTF_8)
            );
            
            boolean modified = false;
            // Iterate over all keys (recursively) in the packaged jar resource file
            for (String key : jarConfig.getKeys(true)) {
                if (!localConfig.contains(key)) {
                    // Copy the missing item (and its sub-keys if it's a section)
                    localConfig.set(key, jarConfig.get(key));
                    modified = true;
                }
            }
            
            if (modified) {
                localConfig.save(localFile);
                plugin.getLogger().info("Successfully merged new default items into " + localFile.getName() + " without altering existing entries.");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to safely merge defaults for " + localFile.getName() + ": " + e.getMessage());
        }
    }

    public double calculateBuyPrice(String itemKey, ItemStack itemStack) {
        ShopItem shopItem = items.get(itemKey);
        if (shopItem == null) return -1;

        double basePrice = shopItem.getBuyPrice();
        double enchantBonus = 0;
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantBonus += 0.1 * entry.getValue();
            }
        }
        
        return basePrice * (1 + enchantBonus);
    }

    public double calculateBuyPrice(ItemStack itemStack) {
        if (itemStack.hasItemMeta()) {
            String itemKey = itemStack.getItemMeta().getPersistentDataContainer().get(plugin.getShopItemKey(), PersistentDataType.STRING);
            if (itemKey != null) {
                return calculateBuyPrice(itemKey, itemStack);
            }
        }
        for (ShopItem item : items.values()) {
            if (item.getMaterial() == itemStack.getType()) {
                return calculateBuyPrice(item.getConfigKey(), itemStack);
            }
        }
        return -1;
    }

    public double calculateSellPrice(ItemStack itemStack) {
        if (itemStack.getAmount() < itemStack.getMaxStackSize()) {
            return 0;
        }

        // Check cooldown
        if (getResaleCooldownRemaining(itemStack) > 0) {
            return -1; // Special value to indicate cooldown
        }
        
        // Base sale is 1 Kroin per full stack
        double basePrice = 1.0;
        double enchantBonus = 0;
        
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
                enchantBonus += 0.1 * entry.getValue();
            }
        }
        
        return basePrice * (1 + enchantBonus);
    }

    public long getResaleCooldownRemaining(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (!meta.getPersistentDataContainer().has(plugin.getShopBoughtTimeKey(), PersistentDataType.LONG)) return 0;

        long boughtTime = meta.getPersistentDataContainer().get(plugin.getShopBoughtTimeKey(), PersistentDataType.LONG);
        long cooldownMillis = (long) plugin.getResaleCooldownHours() * 3600000L;
        long elapsed = System.currentTimeMillis() - boughtTime;

        if (elapsed >= cooldownMillis) {
            return 0;
        }
        return cooldownMillis - elapsed;
    }

    public void clearCooldown(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta.getPersistentDataContainer().has(plugin.getShopBoughtTimeKey(), PersistentDataType.LONG)) {
            meta.getPersistentDataContainer().remove(plugin.getShopBoughtTimeKey());
            item.setItemMeta(meta);
        }
    }

    public ShopItem getShopItem(String itemKey) {
        return items.get(itemKey);
    }

    public Map<String, List<String>> getCategories() {
        return categories;
    }

    public Map<String, ShopItem> getAllItems() {
        return items;
    }

    public static class ShopItem {
        private final String configKey;
        private final Material material;
        private final double buyPrice;
        private final int requiredLevel;
        private final String potionType;
        private final String enchantType;
        private final int enchantLevel;

        public ShopItem(String configKey, Material material, double buyPrice, int requiredLevel,
                        String potionType, String enchantType, int enchantLevel) {
            this.configKey = configKey;
            this.material = material;
            this.buyPrice = buyPrice;
            this.requiredLevel = requiredLevel;
            this.potionType = potionType;
            this.enchantType = enchantType;
            this.enchantLevel = enchantLevel;
        }

        public String getConfigKey() {
            return configKey;
        }

        public Material getMaterial() {
            return material;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public String getPotionType() {
            return potionType;
        }

        public String getEnchantType() {
            return enchantType;
        }

        public int getEnchantLevel() {
            return enchantLevel;
        }

        public ItemStack createItemStack(HereShoppyPlugin plugin) {
            ItemStack item = new ItemStack(material);
            if (material.getMaxStackSize() > 1) {
                item.setAmount(material.getMaxStackSize());
            }

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // Write unique configKey to PDC
                meta.getPersistentDataContainer().set(plugin.getShopItemKey(), PersistentDataType.STRING, configKey);

                // Apply Potion Meta if it's a potion
                if (potionType != null && meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                    try {
                        org.bukkit.potion.PotionType type = org.bukkit.potion.PotionType.valueOf(potionType.toUpperCase());
                        potionMeta.setBasePotionType(type);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid potion type: " + potionType + " for item " + configKey);
                    }
                    
                    // Premium custom display name
                    potionMeta.displayName(net.kyori.adventure.text.Component.text(formatPotionName(potionType, material), net.kyori.adventure.text.format.NamedTextColor.AQUA).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }

                // Apply Enchantment Storage Meta if it's an enchanted book
                if (enchantType != null && meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storageMeta) {
                    try {
                        org.bukkit.enchantments.Enchantment enchantment = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(enchantType.toLowerCase()));
                        if (enchantment == null) {
                            enchantment = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchantType.toLowerCase()));
                        }
                        if (enchantment != null) {
                            storageMeta.addStoredEnchant(enchantment, enchantLevel, true);
                        } else {
                            plugin.getLogger().warning("Could not find enchantment: " + enchantType);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error applying enchantment " + enchantType + ": " + e.getMessage());
                    }

                    // Premium custom display name with Roman numerals
                    storageMeta.displayName(net.kyori.adventure.text.Component.text("Enchanted Book (" + formatEnchantName(enchantType) + " " + toRoman(enchantLevel) + ")", net.kyori.adventure.text.format.NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }

                item.setItemMeta(meta);
            }
            return item;
        }

        private String toRoman(int number) {
            return switch (number) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(number);
            };
        }

        private String formatEnchantName(String key) {
            if (key == null) return "";
            return Arrays.stream(key.split("_"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
        }

        private String formatPotionName(String type, Material mat) {
            if (type == null) return "Potion";
            String prefix = "";
            if (mat == Material.SPLASH_POTION) prefix = "Splash ";
            else if (mat == Material.LINGERING_POTION) prefix = "Lingering ";
            
            String cleanType = type.toLowerCase();
            boolean isLong = cleanType.startsWith("long_");
            boolean isStrong = cleanType.startsWith("strong_");
            if (isLong) cleanType = cleanType.substring(5);
            if (isStrong) cleanType = cleanType.substring(7);
            
            String effectName = Arrays.stream(cleanType.split("_"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                    .collect(Collectors.joining(" "));
            
            String suffix = "";
            if (isLong) suffix = " (Extended)";
            if (isStrong) suffix = " II";
            
            return prefix + "Potion of " + effectName + suffix;
        }
    }
}
