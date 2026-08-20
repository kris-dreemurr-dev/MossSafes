package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.List;

public class InteractListener implements Listener {

    private final MossSafes plugin;

    public InteractListener(MossSafes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST)) return;

        Location loc = block.getLocation();

        if (plugin.getSafeManager().isSafe(loc)) {
            Player player = event.getPlayer();
            String locKey = plugin.getSafeManager().locationToString(loc);
            List<String> authorized = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");

            // Если игрок не авторизован
            if (!authorized.contains(player.getUniqueId().toString())) {
                event.setCancelled(true);

                // Запоминаем, что игрок пытается открыть этот сейф
                plugin.getSafeManager().getPendingAuth().put(player.getUniqueId(), loc);

                String safeName = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".name", "Сейф");
                player.sendMessage(ChatColor.GOLD + "[MossSafes] " + ChatColor.YELLOW + "Сейф '" + safeName + "' заблокирован.");
                player.sendMessage(ChatColor.YELLOW + "Введите пароль командой: " + ChatColor.GREEN + "/mosafes auth <пароль>");

                // Звук закрытой двери/замка воспроизводится в мире для всех поблизости
                if (loc.getWorld() != null) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f);
                }
            }
        }
    }
}