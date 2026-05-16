package com.hereshoppy.hereshoppy.listener;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import com.hereshoppy.hereshoppy.api.HereshoppyAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
    }

    @EventHandler
    public void onSignInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign sign)) return;

        if (sign.getSide(org.bukkit.block.sign.Side.FRONT).getLine(0).equalsIgnoreCase("[Shipping Bin]") ||
            sign.getSide(org.bukkit.block.sign.Side.BACK).getLine(0).equalsIgnoreCase("[Shipping Bin]")) {
            
            Player player = event.getPlayer();
            Inventory binInv = Bukkit.createInventory(null, 27, "§6Shipping Bin");
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
            for (ItemStack item : inv.getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    double price = plugin.getItemManager().calculateSellPrice(item);
                    // User said: "selling any item through the sale box will only reward 1 kroin PER STACK of item."
                    // My calculateSellPrice handles the enchant bonus on top of 1 Kroin base.
                    totalEarned += price;
                }
            }
            
            if (totalEarned > 0) {
                HereshoppyAPI.addKroins(player.getUniqueId(), totalEarned);
                player.sendMessage("§a§l[Shipping Bin] §7You earned §e" + String.format("%.2f", totalEarned) + " §7Kroins from your shipment!");
            }
            activeBins.remove(player.getUniqueId());
        }
    }
}
