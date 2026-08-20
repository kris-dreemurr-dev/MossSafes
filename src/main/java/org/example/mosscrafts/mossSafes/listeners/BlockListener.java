package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;

public class BlockListener implements Listener {

    private final MossSafes plugin;

    public BlockListener(MossSafes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();
        Block placedBlock = event.getBlockPlaced();

        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(plugin.getSafeKey(), PersistentDataType.STRING)) {

                if (hasAdjacentPendingSafe(placedBlock)) {
                    player.sendMessage(ChatColor.RED + "[MossSafes] Нельзя ставить сейф рядом с другим незарегистрированным сейфом!");
                    event.setCancelled(true);
                    return;
                }

                Location loc = placedBlock.getLocation();
                plugin.getSafeManager().getPendingCreation().put(player.getUniqueId(), loc);

                player.sendMessage(ChatColor.GOLD + "[MossSafes] " + ChatColor.YELLOW + "Вы поставили сейф!");
                player.sendMessage(ChatColor.YELLOW + "Зарегистрируйте его командой: " + ChatColor.GREEN + "/mosafes create <название> <пароль>");
                return;
            }
        }

        if (placedBlock.getType() == Material.CHEST || placedBlock.getType() == Material.TRAPPED_CHEST) {
            if (hasAdjacentPendingSafe(placedBlock)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Нельзя объединять сундук с сейфом, пока не завершена его регистрация!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) {
            return;
        }

        Location loc = block.getLocation();

        if (plugin.getSafeManager().getPendingCreation().containsValue(loc)) {
            Player player = event.getPlayer();
            plugin.getSafeManager().getPendingCreation().values().remove(loc);

            event.setDropItems(false);
            dropCustomSafeItem(loc);
            player.sendMessage(ChatColor.YELLOW + "[MossSafes] Регистрация сейфа отменена.");
            return;
        }

        if (plugin.getSafeManager().isSafe(loc)) {
            Player player = event.getPlayer();
            String locKey = plugin.getSafeManager().locationToString(loc);

            String ownerUUID = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".owner");
            if (ownerUUID != null && !player.getUniqueId().toString().equals(ownerUUID) && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Вы не можете сломать чужой сейф!");
                event.setCancelled(true);
                return;
            }

            event.setDropItems(false);
            dropCustomSafeItem(loc);

            plugin.getSafeManager().getConfig().set("safes." + locKey, null);
            plugin.getSafeManager().saveData();

            player.sendMessage(ChatColor.YELLOW + "[MossSafes] Сейф был успешно демонтирован.");
        }
    }

    private boolean hasAdjacentPendingSafe(Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (plugin.getSafeManager().getPendingCreation().containsValue(relative.getLocation())) {
                return true;
            }
        }
        return false;
    }

    private void dropCustomSafeItem(Location loc) {
        ItemStack safeItem = new ItemStack(Material.CHEST);
        ItemMeta meta = safeItem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Защищенный Сейф");
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Поставьте блок, чтобы задать пароль"));
            meta.getPersistentDataContainer().set(plugin.getSafeKey(), PersistentDataType.STRING, "safe");
            safeItem.setItemMeta(meta);
        }
        loc.getWorld().dropItemNaturally(loc.add(0.5, 0.5, 0.5), safeItem);
    }
}