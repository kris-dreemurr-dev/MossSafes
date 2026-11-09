package org.example.mosscrafts.mossSafes;

import org.example.mosscrafts.mossSafes.commands.SafeCommand;
import org.example.mosscrafts.mossSafes.listeners.BlockListener;
import org.example.mosscrafts.mossSafes.listeners.InteractListener;
import org.example.mosscrafts.mossSafes.managers.SafeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MossSafes extends JavaPlugin {

    public static String formatMessage(String message) {
        String prefix = org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&x&C&6&E&9&C&1&lM&x&B&C&D&F&B&5&lᴏ&x&B&1&D&4&A&9&lꜱ&x&A&7&C&A&9&C&lꜱ&x&9&C&C&0&9&0&l-&x&9&2&B&5&8&4&lS&x&8&7&A&B&7&8&lᴀ&x&7&D&A&1&6&B&lғ&x&7&2&9&6&5&F&lᴇ&x&6&8&8&C&5&3&ls &8» &r");
        return prefix + message;
    }

    private SafeManager safeManager;
    private NamespacedKey safeKey;

    @Override
    public void onEnable() {
        this.safeKey = new NamespacedKey(this, "chest");
        this.safeManager = new SafeManager(this);

        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);

        SafeCommand safeCommand = new SafeCommand(this);
        if (getCommand("mosafes") != null) {
            getCommand("mosafes").setExecutor(safeCommand);
            getCommand("mosafes").setTabCompleter(safeCommand);
        }
    }

    @Override
    public void onDisable() {
        if (safeManager != null) {
            safeManager.saveData();
        }
    }

    public SafeManager getSafeManager() {
        return safeManager;
    }

    public NamespacedKey getSafeKey() {
        return safeKey;
    }
}