package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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
import java.util.List;

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

        boolean isSafeItem = item.hasItemMeta() &&
                item.getItemMeta().getPersistentDataContainer().has(plugin.getSafeKey(), PersistentDataType.STRING);

        if (placedBlock.getType() == Material.CHEST || placedBlock.getType() == Material.TRAPPED_CHEST) {

            // Запрет на объединение, если соседний сейф еще в процессе регистрации (pending)
            if (hasAdjacentPendingSafe(placedBlock)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Нельзя ставить сундук рядом с незарегистрированным сейфом!");
                event.setCancelled(true);
                return;
            }

            // Проверяем, есть ли рядом УЖЕ зарегистрированный сейф
            Location adjacentSafeLoc = getAdjacentRegisteredSafeLocation(placedBlock);

            if (adjacentSafeLoc != null) {
                // Проверяем, авторизован ли игрок в существующем сейфе
                String locKey = plugin.getSafeManager().locationToString(adjacentSafeLoc);
                List<String> authorized = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");

                if (!authorized.contains(player.getUniqueId().toString()) && !player.isOp()) {
                    player.sendMessage(ChatColor.RED + "[MossSafes] Вы не можете расширить чужой сейф!");
                    event.setCancelled(true);
                    return;
                }

                // Успешное объединение! Новая половинка автоматически берет свойства существующего сейфа
                player.sendMessage(ChatColor.GREEN + "[MossSafes] Сейф успешно расширен до двойного!");
                return; // Не добавляем в pendingCreation!
            }

            // Если ставим одиночный предметов-сейф в пустое место -> просим зарегистрировать
            if (isSafeItem) {
                Location loc = placedBlock.getLocation();
                plugin.getSafeManager().getPendingCreation().put(player.getUniqueId(), loc);

                player.sendMessage(ChatColor.GOLD + "[MossSafes] " + ChatColor.YELLOW + "Вы поставили сейф!");
                player.sendMessage(ChatColor.YELLOW + "Зарегистрируйте его командой: " + ChatColor.GREEN + "/mosafes create <название> <пароль>");
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

        // Отмена регистрации незавершенного сейфа
        if (plugin.getSafeManager().getPendingCreation().containsValue(loc)) {
            Player player = event.getPlayer();
            plugin.getSafeManager().getPendingCreation().values().remove(loc);

            event.setDropItems(false);
            dropCustomSafeItem(loc);
            player.sendMessage(ChatColor.YELLOW + "[MossSafes] Регистрация сейфа отменена.");
            return;
        }

        // Зарегистрированный сейф
        if (plugin.getSafeManager().isSafe(loc)) {
            Player player = event.getPlayer();
            String locKey = plugin.getSafeManager().locationToString(loc);

            List<String> authorized = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");

            if (!authorized.contains(player.getUniqueId().toString()) && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Вы не можете сломать этот сейф, так как не авторизованы в нем!");

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "Сломать сейф могут только авторизованные игроки!"));

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

    private Location getAdjacentRegisteredSafeLocation(Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
        for (BlockFace face : faces) {
            Block relative = block.getRelative(face);
            if (plugin.getSafeManager().isSafe(relative.getLocation())) {
                return relative.getLocation();
            }
        }
        return null;
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