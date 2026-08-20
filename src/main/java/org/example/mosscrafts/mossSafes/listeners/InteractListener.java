package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

public class InteractListener implements Listener {

    private final MossSafes plugin;

    public InteractListener(MossSafes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        Block mainBlock = getMainChestBlock(block);
        if (mainBlock == null) return;

        String locKey = plugin.getSafeManager().locationToString(mainBlock.getLocation());
        if (!plugin.getSafeManager().getConfig().contains("safes." + locKey)) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        List<String> authList = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");

        if (!authList.contains(uuid.toString())) {
            event.setCancelled(true);
            plugin.getSafeManager().getPendingAuth().put(uuid, mainBlock.getLocation());

            String safeName = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".name", "Сейф");

            // Звук закрытия калитки (нет доступа)
            player.playSound(player.getLocation(), Sound.BLOCK_FENCE_GATE_CLOSE, 1.0f, 1.0f);

            player.sendMessage(ChatColor.GOLD + "[MossSafes] " + ChatColor.RED +
                    "Сейф '" + ChatColor.YELLOW + safeName + ChatColor.RED + "' заблокирован. Введите пароль в чат:");
            return;
        }

        if (mainBlock.getState() instanceof Chest) {
            Chest chest = (Chest) mainBlock.getState();
            String safeName = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".name", "Сейф");
            chest.setCustomName(ChatColor.GOLD + "Защищенный сундук: " + ChatColor.GREEN + safeName);
            chest.update();

            // Звук подбора сферы опыта при успешном открытии
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }

    private Block getMainChestBlock(Block block) {
        if (!(block.getState() instanceof Chest)) return null;

        Chest chest = (Chest) block.getState();
        InventoryHolder holder = chest.getInventory().getHolder();

        if (holder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) holder;
            Chest leftChest = (Chest) doubleChest.getLeftSide();
            if (leftChest != null) {
                return leftChest.getBlock();
            }
        }

        return block;
    }
}