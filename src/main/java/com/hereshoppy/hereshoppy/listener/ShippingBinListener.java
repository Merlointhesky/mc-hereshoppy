package com.hereshoppy.hereshoppy.listener;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.api.HereshoppyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShippingBinListener implements Listener {

    private final HereShoppyPlugin plugin;
    private final Map<UUID, Inventory> activeBins = new HashMap<>();

    public ShippingBinListener(HereShoppyPlugin plugin) {
        this.plugin = plugin;
        startPeriodicProcessing();
    }

    private void startPeriodicProcessing() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<org.bukkit.Location, UUID> entry : plugin.getDataManager().getPhysicalBins().entrySet()) {
                Block block = entry.getKey().getBlock();
                if (block.getState() instanceof Container container) {
                    processInventory(container.getInventory(), entry.getValue());
                }
            }
        }, 100L, 100L); // Every 5 seconds
    }

    private void processInventory(Inventory inv, UUID owner) {
        double totalEarned = 0;
        int itemsSold = 0;
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                double price = plugin.getItemManager().calculateSellPrice(item);
                if (price > 0) {
                    totalEarned += price;
                    itemsSold += item.getAmount();
                    inv.setItem(i, null);
                }
            }
        }
        if (totalEarned > 0) {
            HereshoppyAPI.addKroins(owner, totalEarned);
            Player player = Bukkit.getPlayer(owner);
            if (player != null && player.isOnline()) {
                player.sendMessage("§a§l[Here Sell!] §7Receipt: Sold §e" + itemsSold + " §7items for §e" + String.format("%.2f", totalEarned) + " §7Kroins!");
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (event.getLine(0).equalsIgnoreCase("[Here Sell!]")) {
            Block block = event.getBlock();
            Block attachedTo = getAttachedBlock(block);
            if (attachedTo != null && attachedTo.getState() instanceof Container) {
                plugin.getDataManager().addPhysicalBin(attachedTo.getLocation(), event.getPlayer().getUniqueId());
                event.getPlayer().sendMessage("§a§l[Here Sell!] §7Physical shipping bin created!");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getState() instanceof Sign) {
            Block attachedTo = getAttachedBlock(block);
            if (attachedTo != null && plugin.getDataManager().getPhysicalBins().containsKey(attachedTo.getLocation())) {
                plugin.getDataManager().removePhysicalBin(attachedTo.getLocation());
                event.getPlayer().sendMessage("§c§l[Here Sell!] §7Physical shipping bin removed.");
            }
        } else if (block.getState() instanceof Container) {
            if (plugin.getDataManager().getPhysicalBins().containsKey(block.getLocation())) {
                plugin.getDataManager().removePhysicalBin(block.getLocation());
                event.getPlayer().sendMessage("§c§l[Here Sell!] §7Physical shipping bin removed.");
            }
        }
    }

    private Block getAttachedBlock(Block signBlock) {
        if (signBlock.getBlockData() instanceof WallSign wallSign) {
            return signBlock.getRelative(wallSign.getFacing().getOppositeFace());
        } else if (signBlock.getBlockData() instanceof org.bukkit.block.data.type.Sign sign) {
            return signBlock.getRelative(org.bukkit.block.BlockFace.DOWN);
        }
        return null;
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) return;

        if (sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(0).equalsIgnoreCase("[Here Sell!]") ||
            sign.getSide(org.bukkit.block.sign.Side.BACK).getLine(0).equalsIgnoreCase("[Here Sell!]")) {
            
            Player player = event.getPlayer();
            Inventory binInv = Bukkit.createInventory(null, 27, "§6Here Sell!");
            activeBins.put(player.getUniqueId(), binInv);
            player.openInventory(binInv);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        Player player = (Player) event.getPlayer();
        
        if (activeBins.containsKey(player.getUniqueId()) && activeBins.get(player.getUniqueId()).equals(inv)) {
            double totalEarned = 0;
            int itemsSold = 0;
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    double price = plugin.getItemManager().calculateSellPrice(item);
                    if (price > 0) {
                        totalEarned += price;
                        itemsSold += item.getAmount();
                        inv.setItem(i, null);
                    }
                }
            }
            
            if (totalEarned > 0) {
                HereshoppyAPI.addKroins(player.getUniqueId(), totalEarned);
                player.sendMessage("§a§l[Here Sell!] §7Receipt: Sold §e" + itemsSold + " §7items for §e" + String.format("%.2f", totalEarned) + " §7Kroins!");
            }

            // Return remaining items
            for (ItemStack remaining : inv.getContents()) {
                if (remaining != null && remaining.getType() != Material.AIR) {
                    Map<Integer, ItemStack> leftOver = player.getInventory().addItem(remaining);
                    for (ItemStack dropped : leftOver.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), dropped);
                    }
                }
            }
            
            activeBins.remove(player.getUniqueId());
        }
    }
}
