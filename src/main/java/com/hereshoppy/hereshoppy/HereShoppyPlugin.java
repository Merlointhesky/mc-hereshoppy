package com.hereshoppy.hereshoppy;

import com.hereshoppy.hereshoppy.command.HereShoppyCommand;
import com.hereshoppy.hereshoppy.config.DataManager;
import com.hereshoppy.hereshoppy.listener.ShopListener;
import com.hereshoppy.hereshoppy.listener.ShippingBinListener;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class HereShoppyPlugin extends JavaPlugin {

    private static HereShoppyPlugin instance;
    private DataManager dataManager;
    private ItemManager itemManager;
    private NamespacedKey shopBoughtTimeKey;
    private NamespacedKey shopItemKey;
    private NamespacedKey bobKey;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.shopBoughtTimeKey = new NamespacedKey(this, "shop_bought_time");
        this.shopItemKey = new NamespacedKey(this, "shop_item_key");
        this.bobKey = new NamespacedKey(this, "is_bob");
        this.dataManager = new DataManager(this);
        this.itemManager = new ItemManager(this);

        // Clean up any residual Bob entities to prevent duplicates on reload
        for (org.bukkit.World world : getServer().getWorlds()) {
            for (org.bukkit.entity.WanderingTrader trader : world.getEntitiesByClass(org.bukkit.entity.WanderingTrader.class)) {
                if (trader.getPersistentDataContainer().has(bobKey, org.bukkit.persistence.PersistentDataType.BYTE)) {
                    trader.remove();
                }
            }
        }

        // Register Commands
        HereShoppyCommand cmd = new HereShoppyCommand(this);
        getCommand("hereshoppy").setExecutor(cmd);
        getCommand("hereshoppy").setTabCompleter(cmd);
        if (getCommand("shop") != null) {
            getCommand("shop").setExecutor(cmd);
            getCommand("shop").setTabCompleter(cmd);
        }

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ShippingBinListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(), this);
        getServer().getPluginManager().registerEvents(new com.hereshoppy.hereshoppy.listener.PlayerDataListener(this), this);
        
        // Start Auto-save task (every 5 minutes)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (dataManager != null) {
                dataManager.savePlayers();
                dataManager.saveBins();
                getLogger().info("Auto-saved player data and bins.");
            }
        }, 6000L, 6000L); // 20 ticks * 60 seconds * 5 minutes = 6000 ticks

        getLogger().info("HereShoppy has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.savePlayers();
        }
        getLogger().info("HereShoppy has been disabled!");
    }

    public static HereShoppyPlugin getInstance() {
        return instance;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public NamespacedKey getShopBoughtTimeKey() {
        return shopBoughtTimeKey;
    }

    public NamespacedKey getShopItemKey() {
        return shopItemKey;
    }

    public int getResaleCooldownHours() {
        return getConfig().getInt("resale-cooldown-hours", 24);
    }

    public NamespacedKey getBobKey() {
        return bobKey;
    }
}
