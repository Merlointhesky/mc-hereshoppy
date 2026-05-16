package com.hereshoppy.hereshoppy;

import com.hereshoppy.hereshoppy.command.HereShoppyCommand;
import com.hereshoppy.hereshoppy.config.DataManager;
import com.hereshoppy.hereshoppy.listener.ShopListener;
import com.hereshoppy.hereshoppy.listener.ShippingBinListener;
import com.hereshoppy.hereshoppy.shop.ItemManager;
import org.bukkit.plugin.java.JavaPlugin;

public class HereShoppyPlugin extends JavaPlugin {

    private static HereShoppyPlugin instance;
    private DataManager dataManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        instance = this;
        this.dataManager = new DataManager(this);
        this.itemManager = new ItemManager(this);

        // Register Commands
        HereShoppyCommand cmd = new HereShoppyCommand(this);
        getCommand("hereshoppy").setExecutor(cmd);
        getCommand("hereshoppy").setTabCompleter(cmd);

        // Register Listeners
        getServer().getPluginManager().registerEvents(new ShippingBinListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(), this);

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
}
