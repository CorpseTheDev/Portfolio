package org.crrupt.lantisTiers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;

import java.util.*;

public class LantisTiers extends JavaPlugin {

    private final Map<UUID, String> playerTiers = new HashMap<>();
    private final List<String> validTiers = Arrays.asList(
            "C", "IR", "G1", "G2", "G3", "D1", "D2", "D3", "N1", "N2", "N3"
    );

    @Override
    public void onEnable() {
        this.getCommand("lantistiers").setExecutor(new TierCommandExecutor());
    }

    private class TierCommandExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length < 1) return false;

            String sub = args[0].toLowerCase();

            if (sub.equals("check")) {
                if (args.length == 1 && sender instanceof Player) {
                    Player player = (Player) sender;
                    String tier = playerTiers.getOrDefault(player.getUniqueId(), "Unassigned");
                    player.sendMessage(ChatColor.GREEN + "Your tier: " + ChatColor.YELLOW + tier);
                    return true;
                } else if (args.length == 2) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target != null) {
                        String tier = playerTiers.getOrDefault(target.getUniqueId(), "Unassigned");
                        sender.sendMessage(ChatColor.GREEN + target.getName() + "'s tier: " + ChatColor.YELLOW + tier);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Player not found.");
                    }
                    return true;
                }
            } else if (sub.equals("assign") && args.length == 3) {
                if (!sender.hasPermission("lantistiers.assign")) {
                    sender.sendMessage(ChatColor.RED + "You don’t have permission.");
                    return true;
                }

                String tier = args[1].toUpperCase();
                if (!validTiers.contains(tier)) {
                    sender.sendMessage(ChatColor.RED + "Invalid tier. Valid: " + String.join(", ", validTiers));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found.");
                    return true;
                }

                playerTiers.put(target.getUniqueId(), tier);
                sender.sendMessage(ChatColor.GREEN + "Assigned " + tier + " to " + target.getName());
                updateNameTag(target, tier);
                return true;
            }

            return false;
        }

        private void updateNameTag(Player player, String tier) {
            Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
            Team team = board.getTeam(player.getName());
            if (team == null) {
                team = board.registerNewTeam(player.getName());
                team.addEntry(player.getName());
            }
            team.setPrefix(ChatColor.GRAY + "[" + tier + "] " + ChatColor.RESET);
            player.setScoreboard(board);
        }
    }
}
