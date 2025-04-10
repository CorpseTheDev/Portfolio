package org.crrupt.meteorPlugin;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MeteorPlugin extends JavaPlugin implements Listener, TabExecutor {

    @Override
    public void onEnable() {
        this.getCommand("meteor").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 3) {
            player.sendMessage(ChatColor.RED + "Usage: /meteor <x> <y> <z>");
            return true;
        }

        try {
            double x = Double.parseDouble(args[0]);
            double y = Double.parseDouble(args[1]);
            double z = Double.parseDouble(args[2]);
            Location target = new Location(player.getWorld(), x, y, z);
            spawnMeteor(target);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Coordinates must be numbers.");
        }

        return true;
    }

    private void spawnMeteor(Location target) {
        World world = target.getWorld();
        Location spawnLoc = target.clone().add(0, 120, 0); // Spawn from higher up

        Fireball meteor = (Fireball) world.spawnEntity(spawnLoc, EntityType.FIREBALL);
        meteor.setVelocity(new Vector(0, -3, 0)); // Faster downward velocity
        meteor.setYield(150); // Bigger explosion
        meteor.setIsIncendiary(true);
        meteor.setShooter(null);

        // Create more dramatic meteor approach effects
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid()) {
                    cancel();
                    return;
                }

                Location currentLoc = meteor.getLocation();

                // Enhanced particle effects
                for (int i = 0; i < 30; i++) {
                    world.spawnParticle(Particle.FLAME, currentLoc, 80, 1.5, 1.5, 1.5, 0.05);
                    world.spawnParticle(Particle.LAVA, currentLoc, 50, 1.8, 1.8, 1.8, 0.03);
                    world.spawnParticle(Particle.LARGE_SMOKE, currentLoc, 40, 2.0, 2.0, 2.0, 0.02);
                    world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, currentLoc, 30, 1.2, 2.4, 1.2, 0.03);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, currentLoc, 20, 1.0, 1.0, 1.0, 0.02);
                }

                world.playSound(currentLoc, Sound.ITEM_FIRECHARGE_USE, 2.0f, 0.6f);
                world.playSound(currentLoc, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.4f);
            }
        }.runTaskTimer(this, 0L, 2L); // Run more frequently for smoother effects

        // Delay impact check
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!meteor.isValid()) return;

                Location impactLoc = meteor.getLocation();

                // MASSIVE explosion - power 1000 instead of 50
                world.createExplosion(impactLoc, 1000.0f, true, true);

                // Enhanced impact effects
                world.playSound(impactLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 10.0f, 0.3f);
                world.playSound(impactLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, 8.0f, 0.5f);
                world.playSound(impactLoc, Sound.ENTITY_GENERIC_EXPLODE, 10.0f, 0.4f);

                // Massive particle effects
                world.spawnParticle(Particle.EXPLOSION, impactLoc, 50, 25, 25, 25, 0.5);
                world.spawnParticle(Particle.FLAME, impactLoc, 800, 40, 40, 40, 0.4);
                world.spawnParticle(Particle.LAVA, impactLoc, 600, 40, 40, 40, 0.3);
                world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impactLoc, 400, 40, 40, 40, 0.1);

                // Replace all blocks around the impact with Netherrack and Basalt
                List<Material> replaceBlocks = Arrays.asList(Material.NETHERRACK, Material.BASALT);
                Random random = new Random();
                int radius = 200; // Increased radius for more impact

                // Create a huge crater with Netherrack and Basalt
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -8; y <= 4; y++) { // Deeper crater
                        for (int z = -radius; z <= radius; z++) {
                            // Calculate distance from center
                            double distance = Math.sqrt(x * x + y * y + z * z);

                            if (distance <= radius) {
                                Location blockLoc = impactLoc.clone().add(x, y, z);

                                // Replace blocks with Netherrack and Basalt
                                if (blockLoc.getBlock().getType().isSolid() || random.nextInt(10) == 0) {
                                    Material chosen = replaceBlocks.get(random.nextInt(replaceBlocks.size()));
                                    blockLoc.getBlock().setType(chosen);
                                }
                            }
                        }
                    }
                }

                // Drop meteor core item
                ItemStack meteorCore = new ItemStack(Material.FIRE_CHARGE, 1);
                ItemMeta meta = meteorCore.getItemMeta();
                meta.setDisplayName(ChatColor.RED + "Meteor Core");
                meta.setLore(Arrays.asList(
                        ChatColor.GOLD + "A burning fragment from the catastrophic impact",
                        ChatColor.DARK_RED + "Emits intense heat"
                ));
                meteorCore.setItemMeta(meta);
                world.dropItemNaturally(impactLoc, meteorCore);

                // Continue effects after impact
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks > 200) { // Continue for 10 seconds
                            cancel();
                            return;
                        }

                        // Ongoing effects in the crater
                        world.spawnParticle(Particle.LAVA, impactLoc, 50, radius / 2, 5, radius / 2, 0.1);
                        world.spawnParticle(Particle.FLAME, impactLoc, 30, radius / 2, 5, radius / 2, 0.05);
                        world.spawnParticle(Particle.LARGE_SMOKE, impactLoc, 20, radius / 2, 8, radius / 2, 0.01);

                        if (ticks % 20 == 0) {
                            world.playSound(impactLoc, Sound.BLOCK_FIRE_AMBIENT, 2.0f, 0.6f);
                            world.playSound(impactLoc, Sound.BLOCK_LAVA_AMBIENT, 2.0f, 0.5f);
                        }

                        ticks++;
                    }
                }.runTaskTimer(MeteorPlugin.this, 20L, 1L);
            }
        }.runTaskLater(this, 100L); // Slightly longer delay for dramatic effect
    }
}
