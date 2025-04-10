package org.crrupt.hauntedHammer;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HauntedHammer extends JavaPlugin implements Listener, CommandExecutor {

    private final Set<Player> holdingHammer = new HashSet<>();
    private final HashMap<UUID, Long> abilityCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("hh").setExecutor(this);
        startEffectTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player && sender.isOp()) {
            Player player = (Player) sender;
            player.getInventory().addItem(createHauntedHammer());
            player.sendMessage(ChatColor.DARK_PURPLE + "u got the haunted hammer!");
            return true;
        }
        return false;
    }

    private ItemStack createHauntedHammer() {
        ItemStack hammer = new ItemStack(Material.MACE);
        ItemMeta meta = hammer.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Haunted Hammer");
            hammer.setItemMeta(meta);
        }
        return hammer;
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && item.hasItemMeta() && ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Haunted Hammer")) {
            holdingHammer.add(player);
        } else {
            holdingHammer.remove(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        holdingHammer.remove(event.getPlayer());
        abilityCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHammerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() == Material.MACE && item.hasItemMeta() &&
                ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Haunted Hammer")) {

            event.setCancelled(true);

            long currentTime = System.currentTimeMillis();
            boolean isSneaking = player.isSneaking();
            long cooldown = isSneaking ? 10000 : 5000;

            if (abilityCooldowns.containsKey(player.getUniqueId()) && currentTime - abilityCooldowns.get(player.getUniqueId()) < cooldown) {
                player.sendMessage(ChatColor.RED + "its is on cooldown!");
                return;
            }

            abilityCooldowns.put(player.getUniqueId(), currentTime);

            if (isSneaking) {
                performSlam(player);
            } else {
                performJump(player);
            }
        }
    }

    private void performJump(Player player) {
        player.setVelocity(new Vector(0, 2, 0));
        playSpiralEffect(player);
        preventFallDamage(player, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.2f);
    }

    private void performSlam(Player player) {
        player.setVelocity(new Vector(0, 2, 0));
        playSpiralEffect(player);
        preventFallDamage(player, 15);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                Location loc = player.getLocation();


                for (int i = 0; i < 3; i++) {
                    Bukkit.getScheduler().runTaskLater(HauntedHammer.this, () ->
                            loc.getWorld().createExplosion(loc, 2.0f, false, false), i * 5L);
                }

                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 1.0f);
            }
        }.runTaskLater(this, 20);
    }

    private void preventFallDamage(Player player, int blocks) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, blocks * 4, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 4, false, false));
    }

    private void playSpiralEffect(Player player) {
        new BukkitRunnable() {
            double t = 0;
            final Location loc = player.getLocation();

            @Override
            public void run() {
                if (t > Math.PI * 2) {
                    cancel();
                    return;
                }

                double x = Math.cos(t) * 1.5;
                double y = t / Math.PI;
                double z = Math.sin(t) * 1.5;

                player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(x, y, z), 2, 0, 0, 0, 0.05);
                player.getWorld().spawnParticle(Particle.SOUL, loc.clone().add(-x, y, -z), 2, 0, 0, 0, 0.05);

                t += Math.PI / 8;
            }
        }.runTaskTimer(this, 0, 1);
    }

    private void startEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (holdingHammer.contains(player)) {

                        player.sendActionBar("\uE500");


                        double radius = 4.0;
                        int points = 40;
                        double y = player.getLocation().getY() + 0.2;

                        for (int i = 0; i < points; i++) {
                            double angle = 2 * Math.PI * i / points;
                            double x = player.getLocation().getX() + radius * Math.cos(angle);
                            double z = player.getLocation().getZ() + radius * Math.sin(angle);

                            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, x, y, z, 0, 0, 0, 0, 1);
                        }


                        for (Player nearby : player.getWorld().getPlayers()) {
                            if (nearby.getLocation().distance(player.getLocation()) <= radius && !nearby.equals(player)) {
                                nearby.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0, true, false));
                                if (nearby.getHealth() > 1.0) {
                                    nearby.setHealth(Math.max(nearby.getHealth() - 0.5, 0));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0, 10);
    }
}
