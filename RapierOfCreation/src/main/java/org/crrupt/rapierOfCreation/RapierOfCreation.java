package org.crrupt.rapierOfCreation;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class RapierOfCreation extends JavaPlugin implements Listener, CommandExecutor {

    private final Map<UUID, Long> abilityCooldown = new HashMap<>();
    private final Map<UUID, Long> dashCooldown = new HashMap<>();
    private final Map<UUID, Long> bladeDanceActive = new HashMap<>();
    private final Map<UUID, Integer> hitCounter = new HashMap<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("roc").setExecutor(this);
    }

    @Override
    public void onDisable() {
        abilityCooldown.clear();
        dashCooldown.clear();
        bladeDanceActive.clear();
        hitCounter.clear();
    }

    @EventHandler
    public void onPlayerUseBladeDance(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        UUID uuid = player.getUniqueId();

        if (!isRapierOfCreation(item)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            long currentTime = System.currentTimeMillis();

            if (bladeDanceActive.containsKey(uuid) && bladeDanceActive.get(uuid) > currentTime) {
                if (dashCooldown.getOrDefault(uuid, 0L) > currentTime) {
                    player.sendActionBar(ChatColor.RED + "Dash on cooldown!");
                    return;
                }
                player.setVelocity(player.getLocation().getDirection().multiply(1.5));
                player.sendActionBar(ChatColor.AQUA + "Wind Dash!");
                spawnWindParticles(player);
                dashCooldown.put(uuid, currentTime + 1000);
                return;
            }

            if (abilityCooldown.getOrDefault(uuid, 0L) > currentTime) {
                long remainingTime = (abilityCooldown.get(uuid) - currentTime) / 1000;
                player.sendActionBar(ChatColor.RED + "Ability on cooldown: " + remainingTime + "s");
                return;
            }

            activateBladeDance(player);
            abilityCooldown.put(uuid, currentTime + 20000);
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        UUID uuid = player.getUniqueId();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isRapierOfCreation(item)) return;

        int hitCount = hitCounter.getOrDefault(uuid, 0) + 1;
        spawnWindParticles(player);

        if (hitCount >= 4) {
            event.setDamage(event.getDamage() * 2);
            player.sendActionBar(ChatColor.YELLOW + "Lightning Strike!");

            if (event.getEntity() instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) event.getEntity();
                entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 255, false, false, false));
            }

            hitCounter.put(uuid, 0);
        } else {
            hitCounter.put(uuid, hitCount);
        }
    }

    private void activateBladeDance(Player player) {
        UUID uuid = player.getUniqueId();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        player.sendActionBar(ChatColor.GREEN + "Blade Dance Activated!");

        bladeDanceActive.put(uuid, System.currentTimeMillis() + 10000);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!bladeDanceActive.containsKey(uuid) || System.currentTimeMillis() > bladeDanceActive.get(uuid)) {
                    this.cancel();
                    return;
                }
                spawnWindParticles(player);
            }
        }.runTaskTimer(this, 0L, 5L);
    }

    private void spawnWindParticles(Player player) {
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 10, 0.5, 0.5, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.add(0, 1, 0), 5);
    }

    private boolean isRapierOfCreation(ItemStack item) {
        return item != null && item.getType() == Material.DIAMOND_SWORD && item.hasItemMeta()
                && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 12345;
    }

    private ItemStack createRapierOfCreation() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Rapier of Creation");
            meta.setCustomModelData(12345);
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "A legendary blade of wind and lightning.",
                    ChatColor.AQUA + "Right-Click: Blade Dance (10s, 20s CD)",
                    ChatColor.YELLOW + "Passive: Lightning Strikes (4th hit stuns)"
            ));
            sword.setItemMeta(meta);
        }
        return sword;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("roc")) {
            player.getInventory().addItem(createRapierOfCreation());
            player.sendMessage(ChatColor.GREEN + "You have received the Rapier of Creation!");
            return true;
        }

        return false;
    }
}
