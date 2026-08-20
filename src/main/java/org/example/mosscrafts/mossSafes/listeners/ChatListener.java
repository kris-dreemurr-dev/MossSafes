package org.example.mosscrafts.mossSafes.listeners;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    private final MossSafes plugin;

    public ChatListener(MossSafes plugin) {
        this.plugin = plugin;
    }

    // Приоритет HIGHEST гарантирует, что мы отменяем событие до отправки плагинами чата (ZoChat)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        boolean isPendingAuth = plugin.getSafeManager().getPendingAuth().containsKey(uuid);
        boolean isPendingCreation = plugin.getSafeManager().getPendingCreation().containsKey(uuid);

        // Если игрок в процессе ввода пароля или создания — полностью блокируем сообщение для ВСЕХ
        if (isPendingAuth || isPendingCreation) {
            event.setCancelled(true);
            event.getRecipients().clear(); // Гарантированная очистка получателей для чат-плагинов
        }

        // 1. Ввод пароля для авторизации
        if (isPendingAuth) {
            String inputPassword = event.getMessage().trim();
            Location loc = plugin.getSafeManager().getPendingAuth().remove(uuid);

            String locKey = plugin.getSafeManager().locationToString(loc);
            String correctPassword = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".password");
            String safeName = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".name", "Сейф");

            if (inputPassword.equals(correctPassword)) {
                List<String> authorized = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");
                if (!authorized.contains(uuid.toString())) {
                    authorized.add(uuid.toString());
                    plugin.getSafeManager().getConfig().set("safes." + locKey + ".authorized", authorized);
                    plugin.getSafeManager().saveData();
                }

                player.sendMessage(ChatColor.GREEN + "[MossSafes] Пароль от сейфа '" + safeName + "' верный! Доступ разрешен.");

                if (loc.getWorld() != null) {
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Block block = loc.getBlock();
                    if (block.getState() instanceof Chest) {
                        Chest chest = (Chest) block.getState();
                        player.openInventory(chest.getInventory());
                    }
                });

            } else {
                player.sendMessage(ChatColor.RED + "[MossSafes] Неверный пароль от сейфа '" + ChatColor.YELLOW + safeName + ChatColor.RED + "'!");
                player.sendMessage(ChatColor.DARK_RED + "Чтобы попробовать снова, нажмите на сейф повторно.");

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.damage(2.0);
                    if (player.getWorld() != null) {
                        Location pLoc = player.getLocation();
                        player.getWorld().playSound(pLoc, Sound.ENTITY_ALLAY_HURT, 1.0f, 1.0f);
                        player.getWorld().playSound(pLoc, Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
                    }
                });
            }
            return;
        }

        // 2. Создание нового сейфа
        if (isPendingCreation) {
            String[] args = event.getMessage().split(" ");

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Введите название и пароль через пробел! (Пример: my_safe 1234)");
                return;
            }

            String safeName = args[0];
            String password = args[1];

            if (plugin.getSafeManager().isSafeNameExists(safeName)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Сейф с названием '" + safeName + "' уже существует!");
                player.sendMessage(ChatColor.YELLOW + "Придумайте другое название и введите снова (Название Пароль):");
                return;
            }

            Location loc = plugin.getSafeManager().getPendingCreation().remove(uuid);
            String locKey = plugin.getSafeManager().locationToString(loc);

            plugin.getSafeManager().getConfig().set("safes." + locKey + ".name", safeName);
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".password", password);
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".owner", uuid.toString());
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".authorized", List.of(uuid.toString()));
            plugin.getSafeManager().saveData();

            player.sendMessage(ChatColor.GREEN + "[MossSafes] Сейф '" + safeName + "' успешно создан!");
            player.sendMessage(ChatColor.DARK_GREEN + "Команды для управления " + ChatColor.GREEN + "/mosafes");

            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
        }
    }
}