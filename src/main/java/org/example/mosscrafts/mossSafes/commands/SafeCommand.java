package org.example.mosscrafts.mossSafes.commands;

import org.example.mosscrafts.mossSafes.MossSafes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class SafeCommand implements CommandExecutor, TabCompleter {

    private final MossSafes plugin;

    public SafeCommand(MossSafes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Команда доступна только игрокам!");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // 1. Ввод пароля авторизации через команду
        if (sub.equals("auth")) {
            if (!plugin.getSafeManager().getPendingAuth().containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Вы не взаимодействовали с защищенным сейфом!");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes auth <пароль>");
                return true;
            }

            String inputPassword = args[1];
            Location loc = plugin.getSafeManager().getPendingAuth().remove(uuid);
            String locKey = plugin.getSafeManager().locationToString(loc);
            String correctPassword = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".password");
            String safeName = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".name", "Сейф");

            if (Objects.equals(inputPassword, correctPassword)) {
                List<String> authorized = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");
                if (!authorized.contains(uuid.toString())) {
                    authorized.add(uuid.toString());
                    plugin.getSafeManager().getConfig().set("safes." + locKey + ".authorized", authorized);
                    plugin.getSafeManager().saveData();
                }

                player.sendMessage(ChatColor.GREEN + "[MossSafes] Пароль от сейфа '" + safeName + "' верный!");

                // Звук успешного открытия для всех вокруг
                if (loc.getWorld() != null) {
                    loc.getWorld().playSound(loc, Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                    loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }

                Block block = loc.getBlock();
                if (block.getState() instanceof Chest) {
                    Chest chest = (Chest) block.getState();
                    player.openInventory(chest.getInventory());
                }
            } else {
                player.sendMessage(ChatColor.RED + "[MossSafes] Неверный пароль от сейфа '" + safeName + "'!");

                // Удар током и SFX ловушки слышны всем рядом
                player.damage(2.0);
                if (player.getWorld() != null) {
                    Location pLoc = player.getLocation();
                    player.getWorld().playSound(pLoc, Sound.ENTITY_ALLAY_HURT, 1.0f, 1.0f);
                    player.getWorld().playSound(pLoc, Sound.ENTITY_BAT_DEATH, 1.0f, 1.0f);
                }
            }
            return true;
        }

        // 2. Создание сейфа через команду
        if (sub.equals("create")) {
            if (!plugin.getSafeManager().getPendingCreation().containsKey(uuid)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Сначала поставьте блок сейфа!");
                return true;
            }

            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes create <название> <пароль>");
                return true;
            }

            String safeName = args[1];
            String password = args[2];

            if (plugin.getSafeManager().isSafeNameExists(safeName)) {
                player.sendMessage(ChatColor.RED + "[MossSafes] Сейф с названием '" + safeName + "' уже существует!");
                return true;
            }

            Location loc = plugin.getSafeManager().getPendingCreation().remove(uuid);
            String locKey = plugin.getSafeManager().locationToString(loc);

            plugin.getSafeManager().getConfig().set("safes." + locKey + ".name", safeName);
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".password", password);
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".owner", uuid.toString());
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".authorized", List.of(uuid.toString()));
            plugin.getSafeManager().saveData();

            player.sendMessage(ChatColor.GREEN + "[MossSafes] Сейф '" + safeName + "' успешно создан!");

            if (loc.getWorld() != null) {
                loc.getWorld().playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            return true;
        }

        // --- Существующие команды ---
        if (sub.equalsIgnoreCase("give")) {
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "[MossSafes] У вас нет прав!");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes give <ник>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(ChatColor.RED + "Игрок не найден!");
                return true;
            }
            ItemStack safeItem = new ItemStack(Material.CHEST);
            ItemMeta meta = safeItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "Защищенный Сейф");
                meta.setLore(Collections.singletonList(ChatColor.GRAY + "Поставьте блок, чтобы задать пароль"));
                meta.getPersistentDataContainer().set(plugin.getSafeKey(), PersistentDataType.STRING, "safe");
                safeItem.setItemMeta(meta);
            }
            target.getInventory().addItem(safeItem);
            player.sendMessage(ChatColor.GREEN + "Защищенный сейф выдан игроку " + target.getName());
            return true;
        }

        if (sub.equalsIgnoreCase("remove")) {
            if (args.length < 3) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes remove <название> <ник>");
                return true;
            }
            String safeName = args[1];
            String targetName = args[2];
            String locKey = plugin.getSafeManager().findSafeByName(safeName);
            if (locKey == null) {
                player.sendMessage(ChatColor.RED + "Сейф не найден!");
                return true;
            }
            List<String> authList = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");
            if (!authList.contains(player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.RED + "У вас нет доступа к этому сейфу!");
                return true;
            }
            String targetUUID = null;
            for (String uuidStr : authList) {
                String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                if (targetName.equalsIgnoreCase(name)) {
                    targetUUID = uuidStr;
                    break;
                }
            }
            if (targetUUID == null) {
                player.sendMessage(ChatColor.RED + "Игрок не найден в списке авторизованных!");
                return true;
            }
            authList.remove(targetUUID);
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".authorized", authList);
            plugin.getSafeManager().saveData();
            player.sendMessage(ChatColor.GREEN + "Игрок " + targetName + " деавторизован из сейфа '" + safeName + "'!");
            return true;
        }

        if (sub.equalsIgnoreCase("changepassword")) {
            if (args.length < 4) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes changepassword <название> <старый> <новый>");
                return true;
            }
            String safeName = args[1];
            String oldPass = args[2];
            String newPass = args[3];
            String locKey = plugin.getSafeManager().findSafeByName(safeName);
            if (locKey == null) {
                player.sendMessage(ChatColor.RED + "Сейф не найден!");
                return true;
            }
            String currentPass = plugin.getSafeManager().getConfig().getString("safes." + locKey + ".password");
            if (!Objects.equals(currentPass, oldPass)) {
                player.sendMessage(ChatColor.RED + "Старый пароль введен неверно!");
                return true;
            }
            plugin.getSafeManager().getConfig().set("safes." + locKey + ".password", newPass);
            plugin.getSafeManager().saveData();
            player.sendMessage(ChatColor.GREEN + "Пароль изменен!");
            return true;
        }

        if (sub.equalsIgnoreCase("chestlist")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Использование: /mosafes chestlist <название>");
                return true;
            }
            String safeName = args[1];
            String locKey = plugin.getSafeManager().findSafeByName(safeName);
            if (locKey == null) {
                player.sendMessage(ChatColor.RED + "Сейф не найден!");
                return true;
            }
            List<String> authList = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");
            if (!authList.contains(player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.RED + "У вас нет доступа к этому сейфу!");
                return true;
            }
            player.sendMessage(ChatColor.GOLD + "=== Авторизованные в сейфе '" + safeName + "' ===");
            for (String uuidStr : authList) {
                String pName = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                player.sendMessage(ChatColor.YELLOW + "- " + (pName != null ? pName : uuidStr));
            }
            return true;
        }

        sendHelp(player);
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== MossSafes Помощь ===");
        if (player.isOp()) {
            player.sendMessage(ChatColor.YELLOW + "/mosafes give <ник>" + ChatColor.WHITE + " - Выдать особый сейф (Только OP)");
        }
        player.sendMessage(ChatColor.YELLOW + "/mosafes create <название> <пароль>" + ChatColor.WHITE + " - Создать сейф после установки");
        player.sendMessage(ChatColor.YELLOW + "/mosafes auth <пароль>" + ChatColor.WHITE + " - Ввести пароль от сейфа");
        player.sendMessage(ChatColor.YELLOW + "/mosafes remove <название> <ник>" + ChatColor.WHITE + " - Деавторизовать игрока");
        player.sendMessage(ChatColor.YELLOW + "/mosafes changepassword <название> <старый> <новый>" + ChatColor.WHITE + " - Сменить пароль");
        player.sendMessage(ChatColor.YELLOW + "/mosafes chestlist <название>" + ChatColor.WHITE + " - Список авторизованных игроков");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        Player player = (Player) sender;
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (player.isOp()) {
                completions.add("give");
            }
            completions.addAll(Arrays.asList("create", "auth", "remove", "changepassword", "chestlist"));
            return filterCompletions(completions, args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give") && player.isOp()) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("changepassword") || args[0].equalsIgnoreCase("chestlist")) {
                completions.addAll(plugin.getSafeManager().getAuthorizedSafes(player.getUniqueId()));
            }
            return filterCompletions(completions, args[1]);
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("remove")) {
                String safeName = args[1];
                String locKey = plugin.getSafeManager().findSafeByName(safeName);
                if (locKey != null) {
                    List<String> authList = plugin.getSafeManager().getConfig().getStringList("safes." + locKey + ".authorized");
                    for (String uuidStr : authList) {
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidStr)).getName();
                        if (name != null) completions.add(name);
                    }
                }
            }
            return filterCompletions(completions, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String current) {
        List<String> filtered = new ArrayList<>();
        for (String str : completions) {
            if (str.toLowerCase().startsWith(current.toLowerCase())) {
                filtered.add(str);
            }
        }
        return filtered;
    }
}