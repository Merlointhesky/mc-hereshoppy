package com.hereshoppy.hereshoppy.listener;

import com.hereshoppy.hereshoppy.HereShoppyPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataListener implements Listener {

    private final HereShoppyPlugin plugin;

    public PlayerDataListener(HereShoppyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Save all data when a player leaves to ensure their progress is captured
        plugin.getDataManager().savePlayers();
    }
}
