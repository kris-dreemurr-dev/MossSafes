package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class BlockListener implements Listener {

    private final MossSafes plugin;
    private final BlockFace[] CARDINAL_FACES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public BlockListener(MossSafes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlock();
        ItemStack item = event.getItemInHand();

        if (placedBlock.getType() != Material.CHEST) return;

        // 1. Проверяем, ставится ли блок вплотную к УЖЕ ЗАРЕГИСТРИРОВАННОМУ сейфу
        if (isAdjacentToRegisteredSafe(placedBlock)) {
            event.getPlayer().sendMessage(ChatColor.GREEN + "[MossSafes] Сейф успешно расширен!");
            return; // Прерываем выполнение, чтобы НЕ запрашивать новый пароль
        }

        // 2. Если рядом нет сейфа — обрабатываем как установку НОВОГО сейфа
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String tagValue = meta.getPersistentDataContainer().get(plugin.getSafeKey(), PersistentDataType.STRING);

                if ("safe".equals(tagValue)) {
                    Player player = event.getPlayer();
                    Location loc = placedBlock.getLocation();

                    plugin.getSafeManager().getPendingCreation().put(player.getUniqueId(), loc);
                    player.sendMessage(ChatColor.GOLD + "[MossSafes] " + ChatColor.YELLOW +
                            "Вы поставили сейф! Введите в чат название и пароль через пробел (Пример: my_safe 1234):");
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Chest)) return;

        Chest chest = (Chest) block.getState();
        InventoryHolder holder = chest.getInventory().getHolder();

        String locKeyToRemove = null;

        if (holder instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) holder;
            Chest left = (Chest) doubleChest.getLeftSide();
            Chest right = (Chest) doubleChest.getRightSide();

            if (left != null && right != null) {
                String leftKey = plugin.getSafeManager().locationToString(left.getLocation());
                String rightKey = plugin.getSafeManager().locationToString(right.getLocation());

                // Если сломали левую (главную) половину — переносим данные на правую
                if (block.getLocation().equals(left.getLocation()) && plugin.getSafeManager().getConfig().contains("safes." + leftKey)) {
                    copySafeData("safes." + leftKey, "safes." + rightKey);
                    locKeyToRemove = leftKey;
                } else if (block.getLocation().equals(right.getLocation())) {
                    locKeyToRemove = rightKey;
                }
            }
        } else {
            String locKey = plugin.getSafeManager().locationToString(block.getLocation());
            if (plugin.getSafeManager().getConfig().contains("safes." + locKey)) {
                locKeyToRemove = locKey;
            }
        }

        if (locKeyToRemove != null) {
            plugin.getSafeManager().getConfig().set("safes." + locKeyToRemove, null);
            plugin.getSafeManager().saveData();
            event.getPlayer().sendMessage(ChatColor.YELLOW + "[MossSafes] Данные сейфа обновлены/удалены.");
        }
    }

    /**
     * Проверяет соседей по сторонам света (N, S, E, W) на наличие сейфа в safes.yml
     */
    private boolean isAdjacentToRegisteredSafe(Block placedBlock) {
        for (BlockFace face : CARDINAL_FACES) {
            Block relative = placedBlock.getRelative(face);
            if (relative.getType() == Material.CHEST) {
                String locKey = plugin.getSafeManager().locationToString(relative.getLocation());
                if (plugin.getSafeManager().getConfig().contains("safes." + locKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void copySafeData(String fromPath, String toPath) {
        plugin.getSafeManager().getConfig().set(toPath + ".name", plugin.getSafeManager().getConfig().get(fromPath + ".name"));
        plugin.getSafeManager().getConfig().set(toPath + ".password", plugin.getSafeManager().getConfig().get(fromPath + ".password"));
        plugin.getSafeManager().getConfig().set(toPath + ".owner", plugin.getSafeManager().getConfig().get(fromPath + ".owner"));
        plugin.getSafeManager().getConfig().set(toPath + ".authorized", plugin.getSafeManager().getConfig().get(fromPath + ".authorized"));
    }
}