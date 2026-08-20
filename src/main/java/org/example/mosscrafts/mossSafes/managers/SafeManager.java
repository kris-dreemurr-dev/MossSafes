package org.example.mosscrafts.mossSafes.managers;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SafeManager {

    private final MossSafes plugin;
    private final File configFile;
    private FileConfiguration config;

    private final Map<UUID, Location> pendingCreation = new HashMap<>();
    private final Map<UUID, Location> pendingAuth = new HashMap<>();

    public SafeManager(MossSafes plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "safes.yml");
        loadData();
    }

    public void loadData() {
        if (!configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveData() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public Map<UUID, Location> getPendingCreation() {
        return pendingCreation;
    }

    public Map<UUID, Location> getPendingAuth() {
        return pendingAuth;
    }

    // Проверка: является ли блок по этой локации сейфом
    public boolean isSafe(Location loc) {
        if (loc == null) return false;
        String locKey = locationToString(loc);
        return config.contains("safes." + locKey);
    }

    public boolean isSafeNameExists(String name) {
        if (!config.contains("safes")) return false;
        for (String key : config.getConfigurationSection("safes").getKeys(false)) {
            String safeName = config.getString("safes." + key + ".name");
            if (safeName != null && safeName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public String findSafeByName(String name) {
        if (!config.contains("safes")) return null;
        for (String key : config.getConfigurationSection("safes").getKeys(false)) {
            String safeName = config.getString("safes." + key + ".name");
            if (safeName != null && safeName.equalsIgnoreCase(name)) {
                return key;
            }
        }
        return null;
    }

    public List<String> getAuthorizedSafes(UUID playerUUID) {
        List<String> list = new ArrayList<>();
        if (!config.contains("safes")) return list;

        for (String key : config.getConfigurationSection("safes").getKeys(false)) {
            List<String> auth = config.getStringList("safes." + key + ".authorized");
            if (auth.contains(playerUUID.toString())) {
                String safeName = config.getString("safes." + key + ".name");
                if (safeName != null) list.add(safeName);
            }
        }
        return list;
    }

    public String locationToString(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
    }
}