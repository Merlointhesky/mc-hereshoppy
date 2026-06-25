package com.hereshoppy.hereshoppy.listener;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.api.HereshoppyAPI;
import com.hereshoppy.hereshoppy.gui.ShopGUI;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import com.hereshoppy.hereshoppy.shop.BobShopHolder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class ShopListener implements Listener {

    private static WanderingTrader activeBob = null;
    private static Inventory bobInventory = null;
    private static BukkitTask bobFollowTask = null;

    private static final Material[] BOB_ITEMS_POOL = {
        Material.NETHERITE_SWORD, Material.DIAMOND_SWORD, Material.IRON_SWORD,
        Material.NETHERITE_PICKAXE, Material.DIAMOND_PICKAXE, Material.IRON_PICKAXE,
        Material.NETHERITE_AXE, Material.DIAMOND_AXE, Material.IRON_AXE,
        Material.NETHERITE_SHOVEL, Material.DIAMOND_SHOVEL, Material.IRON_SHOVEL,
        Material.NETHERITE_HOE, Material.DIAMOND_HOE, Material.IRON_HOE,
        Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.SHEARS, Material.FISHING_ROD
    };

    private final Map<UUID, ShopGUI.SearchFilterState> activeChatSearches = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory topInv = event.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();
        if (holder == null) return;

        if (holder instanceof BobShopHolder) {
            event.setCancelled(true);
            handleBobShopClick(player, event);
            return;
        }

        // Ensure we are dealing with our shop
        if (!(holder instanceof ShopGUI.MainMenuHolder || 
              holder instanceof ShopGUI.CategoryMenuHolder || 
              holder instanceof ShopGUI.SearchMenuHolder ||
              holder instanceof ShopGUI.AlphabetMenuHolder)) {
            return;
        }

        // Prevent "Collect to Cursor" (double-click to grab all) which can bypass inventory checks
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Check if the clicked inventory is the player's inventory
        if (event.getRawSlot() >= topInv.getSize() || event.getRawSlot() < 0) {
            event.setCancelled(true);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // All clicks in the shop GUI should be cancelled
        event.setCancelled(true);

        if (holder instanceof ShopGUI.AlphabetMenuHolder alphabetHolder) {
            event.setCancelled(true);
            ShopGUI.SearchFilterState state = alphabetHolder.getState();
            if (event.getRawSlot() < 26) {
                // Letter slot
                char letter = (char) ('A' + event.getRawSlot());
                state.setLetter(String.valueOf(letter));
                state.setQuery(null); // Clear custom search text when choosing a letter
                player.closeInventory();
                Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
            } else if (event.getRawSlot() == 26) {
                // Clear letter slot
                state.setLetter(null);
                player.closeInventory();
                Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
            }
            return;
        }

        if (holder instanceof ShopGUI.SearchMenuHolder searchHolder) {
            event.setCancelled(true);
            ShopGUI.SearchFilterState state = searchHolder.getState();
            int slot = event.getRawSlot();
            
            if (slot >= 45 && slot <= 53) {
                switch (slot) {
                    case 45 -> {
                        // Name Search (Compass)
                        player.closeInventory();
                        activeChatSearches.put(player.getUniqueId(), state);
                        player.sendMessage(Component.text("💬 [HereShoppy] Type your search query in chat now (e.g., wind). This message is completely private.", NamedTextColor.YELLOW));
                    }
                    case 46 -> {
                        // Starting Letter (Paper)
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openAlphabetMenu(player, state));
                    }
                    case 47 -> {
                        // Category Filter (Chest)
                        cycleCategory(state);
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                    }
                    case 48 -> {
                        // Level Filter (Experience Bottle)
                        cycleLevelRange(state);
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                    }
                    case 49 -> {
                        // Availability Filter (Lever)
                        cycleAvailability(state);
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                    }
                    case 50 -> {
                        // Reset Filters (Redstone Dust)
                        state.reset();
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                    }
                    case 51 -> {
                        // Previous Page (Arrow)
                        if (clicked.getType() == Material.ARROW && state.getPage() > 0) {
                            state.setPage(state.getPage() - 1);
                            player.closeInventory();
                            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                        }
                    }
                    case 52 -> {
                        // Next Page (Arrow)
                        if (clicked.getType() == Material.ARROW) {
                            state.setPage(state.getPage() + 1);
                            player.closeInventory();
                            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                        }
                    }
                    case 53 -> {
                        // Back to Shop (Barrier)
                        player.closeInventory();
                        Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openMainMenu(player));
                    }
                }
                return;
            }
            
            if (slot < 45) {
                ClickType clickType = event.getClick();
                if (clickType == ClickType.LEFT) {
                    purchaseItem(player, clicked);
                } else if (clickType == ClickType.MIDDLE) {
                    if (event.isShiftClick()) {
                        // Previous page via shift middle-click
                        if (state.getPage() > 0) {
                            state.setPage(state.getPage() - 1);
                            player.closeInventory();
                            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                        }
                    } else {
                        // Next page via middle-click
                        int currentLevel = HereshoppyAPI.getLevel(player.getUniqueId());
                        List<ItemManager.ShopItem> matches = ShopGUI.getSearchMatches(state, currentLevel);
                        if (matches.size() > (state.getPage() + 1) * 45) {
                            state.setPage(state.getPage() + 1);
                            player.closeInventory();
                            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
                        }
                    }
                }
            }
            return;
        }

        if (holder instanceof ShopGUI.MainMenuHolder) {
            if (clicked.getType() == Material.ANVIL) {
                ShopGUI.openSearchMenu(player, new ShopGUI.SearchFilterState());
                return;
            }
            
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String category = findCategoryByName(clicked);
                if (category != null) {
                    ShopGUI.openCategoryMenu(player, category, 0);
                }
            }
        } else if (holder instanceof ShopGUI.CategoryMenuHolder catHolder) {
            ClickType clickType = event.getClick();
            if (clickType == ClickType.SWAP_OFFHAND || clickType == ClickType.DROP) {
                event.setCancelled(true);
                if (ShopGUI.isRandomizableCategory(catHolder.getCategory())) {
                    player.sendMessage(Component.text("Stock randomised via keybind!", NamedTextColor.GREEN));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                    ShopGUI.openCategoryMenu(player, catHolder.getCategory(), catHolder.getPage());
                }
                return;
            }
            int slot = event.getSlot();
            if (slot < 45) {
                if (clickType == ClickType.LEFT) {
                    purchaseItem(player, clicked);
                } else if (clickType == ClickType.MIDDLE) {
                    if (event.isShiftClick()) {
                        // Previous page via shift middle-click
                        int currentPage = catHolder.getPage();
                        if (currentPage > 0) {
                            player.closeInventory();
                            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openCategoryMenu(player, catHolder.getCategory(), currentPage - 1));
                        }
                    } else {
                        // Next page via middle-click
                        int currentPage = catHolder.getPage();
                        int currentLevel = HereshoppyAPI.getLevel(player.getUniqueId());
                        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
                        List<String> allKeys = plugin.getItemManager().getCategories().get(catHolder.getCategory());
                        if (allKeys != null) {
                            long availableCount = allKeys.stream()
                                .map(key -> plugin.getItemManager().getShopItem(key))
                                .filter(item -> item != null)
                                .filter(item -> item.getRequiredLevel() <= currentLevel)
                                .count();
                            if (availableCount > (currentPage + 1) * 45) {
                                player.closeInventory();
                                Bukkit.getScheduler().runTask(plugin, () -> ShopGUI.openCategoryMenu(player, catHolder.getCategory(), currentPage + 1));
                            }
                        }
                    }
                }
            } else {
                handleCategoryClick(player, clicked, catHolder, slot);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        InventoryHolder holder = topInv.getHolder();
        if (holder instanceof ShopGUI.MainMenuHolder || 
            holder instanceof ShopGUI.CategoryMenuHolder || 
            holder instanceof ShopGUI.SearchMenuHolder ||
            holder instanceof ShopGUI.AlphabetMenuHolder ||
            holder instanceof BobShopHolder) {
            
            // Cancel any drag that affects the top inventory
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topInv.getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
            
            event.setCancelled(true);
        }
    }

    private String findCategoryByName(ItemStack item) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        for (String cat : plugin.getItemManager().getCategories().keySet()) {
            if (getCategoryMaterial(cat) == item.getType()) return cat;
        }
        return null;
    }

    private Material getCategoryMaterial(String category) {
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

    private void handleCategoryClick(Player player, ItemStack clicked, ShopGUI.CategoryMenuHolder holder, int slot) {
        if (slot >= 45) { // Navigation or Preview
            if (clicked.getType() == Material.ARROW) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.displayName() != null) {
                    String name = plainText(meta.displayName());
                    if (name.contains("Next")) {
                        ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage() + 1);
                    } else if (name.contains("Previous")) {
                        ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage() - 1);
                    }
                }
            } else if (clicked.getType() == Material.BARRIER) {
                ShopGUI.openMainMenu(player);
            } else if (clicked.getType() == Material.SUNFLOWER && ShopGUI.isRandomizableCategory(holder.getCategory())) {
                player.sendMessage(Component.text("Stock randomised!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
                ShopGUI.openCategoryMenu(player, holder.getCategory(), holder.getPage());
            }
            return;
        }

        purchaseItem(player, clicked);
    }



    private void purchaseItem(Player player, ItemStack clicked) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        
        ItemMeta clickedMeta = clicked.getItemMeta();
        if (clickedMeta == null || clickedMeta.lore() == null) return;
        
        // Read config key from PDC
        String itemKey = clickedMeta.getPersistentDataContainer().get(plugin.getShopItemKey(), PersistentDataType.STRING);
        if (itemKey == null) return;
        
        ItemManager.ShopItem shopItem = plugin.getItemManager().getShopItem(itemKey);
        if (shopItem == null) return;

        // Check if the item is actually buyable (has Price lore)
        boolean isBuyable = false;
        for (Component line : clickedMeta.lore()) {
            if (plainText(line).contains("Price:")) {
                isBuyable = true;
                break;
            }
        }
        
        if (!isBuyable) {
            // Check if it's a locked item
            for (Component line : clickedMeta.lore()) {
                if (plainText(line).contains("LOCKED")) {
                    player.sendMessage(Component.text("This item is locked! You need a higher shop level.", NamedTextColor.RED));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
                    return;
                }
            }
            return;
        }

        // Double check level requirement
        int playerLevel = HereshoppyAPI.getLevel(player.getUniqueId());
        if (playerLevel < shopItem.getRequiredLevel()) {
            player.sendMessage(Component.text("You need shop level " + shopItem.getRequiredLevel() + " to purchase this!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
            return;
        }

        double price = plugin.getItemManager().calculateBuyPrice(itemKey, clicked);
        double balance = HereshoppyAPI.getKroins(player.getUniqueId());

        if (balance < price) {
            player.sendMessage(Component.text("You don't have enough Kroins!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
            return;
        }

        HereshoppyAPI.removeKroins(player.getUniqueId(), price);
        HereshoppyAPI.addShopXp(player.getUniqueId(), (int) price);
        
        ItemStack toGive = clicked.clone();
        ItemMeta meta = toGive.getItemMeta();
        if (meta != null) {
            // Remove the shop lore
            meta.lore(null);
            
            // Tag with purchase time
            meta.getPersistentDataContainer().set(plugin.getShopBoughtTimeKey(), PersistentDataType.LONG, System.currentTimeMillis());
            
            toGive.setItemMeta(meta);
        }
        
        player.getInventory().addItem(toGive).values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));
        
        String displayName = clickedMeta.hasDisplayName() ? plainText(clickedMeta.displayName()) : formatMaterialName(clicked.getType());
        player.sendMessage(Component.text("Purchased " + clicked.getAmount() + "x " + displayName + " for " + String.format("%.2f", price) + " Kroins.", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Spawn Bob check
        checkBobSpawn(player);
    }

    private String plainText(Component component) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
    }

    private String formatMaterialName(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (activeChatSearches.containsKey(uuid)) {
            event.setCancelled(true);
            ShopGUI.SearchFilterState state = activeChatSearches.remove(uuid);
            String query = plainText(event.message());
            state.setQuery(query);
            state.setLetter(null); // Clear letter filter when searching by custom query
            Bukkit.getScheduler().runTask(HereShoppyPlugin.getInstance(), () -> ShopGUI.openSearchMenu(player, state));
        }
    }

    private void cycleCategory(ShopGUI.SearchFilterState state) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        List<String> sortedCategories = plugin.getItemManager().getCategories().keySet().stream().sorted().collect(Collectors.toList());
        if (sortedCategories.isEmpty()) return;
        
        String current = state.getCategory();
        if (current == null) {
            state.setCategory(sortedCategories.get(0));
        } else {
            int index = sortedCategories.indexOf(current);
            if (index == -1 || index == sortedCategories.size() - 1) {
                state.setCategory(null); // Cycle back to all
            } else {
                state.setCategory(sortedCategories.get(index + 1));
            }
        }
    }

    private void cycleLevelRange(ShopGUI.SearchFilterState state) {
        String current = state.getLevelRange();
        String next = switch (current) {
            case "ALL" -> "1-20";
            case "1-20" -> "21-50";
            case "21-50" -> "51-80";
            case "51-80" -> "81-100";
            default -> "ALL";
        };
        state.setLevelRange(next);
    }

    private void cycleAvailability(ShopGUI.SearchFilterState state) {
        String current = state.getAvailability();
        String next = switch (current) {
            case "ALL" -> "PURCHASABLE";
            case "PURCHASABLE" -> "LOCKED";
            default -> "ALL";
        };
        state.setAvailability(next);
    }

    // --- Bob the Shady Merchant Spawning & Interaction Logic ---

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof WanderingTrader trader) {
            HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
            if (trader.getPersistentDataContainer().has(plugin.getBobKey(), PersistentDataType.BYTE)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                if (bobInventory == null) {
                    bobInventory = createBobInventory();
                }
                player.openInventory(bobInventory);
                player.playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_TRADE, 1f, 1f);
            }
        }
    }

    private void handleBobShopClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 9) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
            return;
        }

        if (clicked.getType() == Material.RED_STAINED_GLASS_PANE) {
            player.sendMessage(Component.text("This item is already sold out!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
            return;
        }

        double price = 100000.0;
        double balance = HereshoppyAPI.getKroins(player.getUniqueId());

        if (balance < price) {
            player.sendMessage(Component.text("You don't have enough Kroins! Bob requires 100,000 Kroins.", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1f, 1f);
            return;
        }

        HereshoppyAPI.removeKroins(player.getUniqueId(), price);
        HereshoppyAPI.addShopXp(player.getUniqueId(), (int) price);

        ItemStack toGive = clicked.clone();
        ItemMeta meta = toGive.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore != null && lore.size() >= 3) {
                lore.subList(lore.size() - 3, lore.size()).clear();
                meta.lore(lore.isEmpty() ? null : lore);
            }
            HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
            meta.getPersistentDataContainer().set(plugin.getShopBoughtTimeKey(), PersistentDataType.LONG, System.currentTimeMillis());
            toGive.setItemMeta(meta);
        }

        player.getInventory().addItem(toGive).values().forEach(remaining -> player.getWorld().dropItemNaturally(player.getLocation(), remaining));

        ItemStack soldOut = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta soldMeta = soldOut.getItemMeta();
        if (soldMeta != null) {
            soldMeta.displayName(Component.text("SOLD OUT", NamedTextColor.RED).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            soldOut.setItemMeta(soldMeta);
        }
        bobInventory.setItem(slot, soldOut);

        player.sendMessage(Component.text("Successfully purchased " + formatMaterialName(clicked.getType()) + " for 100,000 Kroins!", NamedTextColor.GREEN));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
        player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation(), 20, 0.5, 1.0, 0.5, 0.1);
    }

    private void checkBobSpawn(Player player) {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        if (!Bukkit.getPluginManager().isPluginEnabled("HereMobby")) {
            return;
        }
        if (activeBob != null && activeBob.isValid() && !activeBob.isDead()) {
            return;
        }

        if (new Random().nextDouble() >= 0.01) {
            return;
        }

        Location loc = player.getLocation();
        activeBob = loc.getWorld().spawn(loc, WanderingTrader.class, trader -> {
            trader.customName(Component.text("Bob the Shady Merchant", NamedTextColor.GOLD).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            trader.setCustomNameVisible(true);
            trader.setRemoveWhenFarAway(false);
            trader.setDespawnDelay(24000);
            trader.getPersistentDataContainer().set(plugin.getBobKey(), PersistentDataType.BYTE, (byte) 1);
        });

        bobInventory = createBobInventory();

        player.sendMessage(Component.text("✨ A shady figure emerges from the shadows... Bob the Shady Merchant has arrived!", NamedTextColor.GOLD));
        player.playSound(player.getLocation(), Sound.ENTITY_WANDERING_TRADER_YES, 1f, 1f);

        UUID playerUuid = player.getUniqueId();
        if (bobFollowTask != null) {
            bobFollowTask.cancel();
        }
        bobFollowTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeBob == null || !activeBob.isValid() || activeBob.isDead()) {
                cleanupBob();
                return;
            }
            Player target = Bukkit.getPlayer(playerUuid);
            if (target == null || !target.isOnline()) {
                return;
            }
            if (!activeBob.getWorld().equals(target.getWorld())) {
                activeBob.teleport(target.getLocation());
                return;
            }
            double distSq = activeBob.getLocation().distanceSquared(target.getLocation());
            if (distSq > 400) {
                activeBob.teleport(target.getLocation());
                activeBob.getWorld().spawnParticle(org.bukkit.Particle.POOF, activeBob.getLocation(), 10);
            } else if (distSq > 9) {
                activeBob.getPathfinder().moveTo(target, 1.25);
            }
        }, 20L, 20L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeBob != null && activeBob.isValid()) {
                activeBob.getWorld().spawnParticle(org.bukkit.Particle.POOF, activeBob.getLocation(), 10);
                activeBob.getWorld().playSound(activeBob.getLocation(), Sound.ENTITY_WANDERING_TRADER_DISAPPEARED, 1f, 1f);
                activeBob.remove();
            }
            cleanupBob();
        }, 6000L);
    }

    private Inventory createBobInventory() {
        HereShoppyPlugin plugin = HereShoppyPlugin.getInstance();
        BobShopHolder holder = new BobShopHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Bob's Shady Goods", NamedTextColor.DARK_RED).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
        holder.setInventory(inv);

        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(Component.empty());
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glass);
        }

        Random random = new Random();
        List<Material> pool = new ArrayList<>(Arrays.asList(BOB_ITEMS_POOL));
        Collections.shuffle(pool, random);

        int[] slots = {2, 3, 4, 5, 6};
        for (int i = 0; i < 5; i++) {
            Material mat = pool.get(i);
            ItemStack base = new ItemStack(mat);
            ItemStack enchanted = base.enchantWithLevels(15, true, random);
            ItemMeta meta = enchanted.getItemMeta();
            if (meta != null) {
                List<Component> lore = new ArrayList<>();
                if (meta.lore() != null) {
                    lore.addAll(meta.lore());
                }
                lore.add(Component.text("--------------------", NamedTextColor.GRAY).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                lore.add(Component.text("Price: 100,000 Kroins", NamedTextColor.GOLD).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                lore.add(Component.text("Click to Purchase", NamedTextColor.YELLOW).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                meta.lore(lore);
                enchanted.setItemMeta(meta);
            }
            inv.setItem(slots[i], enchanted);
        }

        return inv;
    }

    private void cleanupBob() {
        if (bobFollowTask != null) {
            bobFollowTask.cancel();
            bobFollowTask = null;
        }
        activeBob = null;
        bobInventory = null;
    }
}
