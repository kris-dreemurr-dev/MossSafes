package org.example.mosscrafts.mossSafes;

import org.example.mosscrafts.mossSafes.commands.SafeCommand;
import org.example.mosscrafts.mossSafes.listeners.BlockListener;
import org.example.mosscrafts.mossSafes.listeners.ChatListener;
import org.example.mosscrafts.mossSafes.listeners.InteractListener;
import org.example.mosscrafts.mossSafes.managers.SafeManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class MossSafes extends JavaPlugin {

    private SafeManager safeManager;
    private NamespacedKey safeKey;

    @Override
    public void onEnable() {
        this.safeKey = new NamespacedKey(this, "chest");
        this.safeManager = new SafeManager(this);

        // Регистрация событий
        getServer().getPluginManager().registerEvents(new BlockListener(this), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        //getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // Регистрация команд
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