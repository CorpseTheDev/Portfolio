package org.crrupt.bloodOfDemons;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class BloodOfDemons extends JavaPlugin implements Listener {

    private final Set<Player> empoweredPlayers = new HashSet<>();
    private final NamespacedKey bloodKey = new NamespacedKey(this, "blood_of_demons");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("bd").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && sender.isOp()) {
                Player player = (Player) sender;
                player.getInventory().addItem(createBloodBottle());
            }
            return true;
        });
        getCommand("ability1").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && empoweredPlayers.contains(sender) && sender.isOp()) {
                cursedSkin((Player) sender);
            }
            return true;
        });
        getCommand("ability2").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && empoweredPlayers.contains(sender) && sender.isOp()) {
                forcedPull((Player) sender);
            }
            return true;
        });
        getCommand("ability3").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player && empoweredPlayers.contains(sender) && sender.isOp()) {
                redWave((Player) sender);
            }
            return true;
        });

        
        empoweredPlayers.clear();
    }

    @EventHandler
    public void onDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(bloodKey, PersistentDataType.BYTE)) {
            Player player = event.getPlayer();
            empoweredPlayers.add(player);
            player.sendMessage("You have consumed the Blood of Demons and gained its power!");
        }
    }

    @EventHandler
    public void onShiftRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!empoweredPlayers.contains(player)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.GLASS_BOTTLE && player.isSneaking()) {
                empoweredPlayers.remove(player);
                player.getInventory().setItemInMainHand(createBloodBottle());
                player.sendMessage("Your powers have been sealed back into the bottle.");
            }
        }
    }

    private ItemStack createBloodBottle() {
        ItemStack bloodBottle = new ItemStack(Material.POTION);
        ItemMeta meta = bloodBottle.getItemMeta();
        meta.setDisplayName("§4Blood of Demons");
        meta.setCustomModelData(1002); 
        meta.getPersistentDataContainer().set(bloodKey, PersistentDataType.BYTE, (byte) 1);
        bloodBottle.setItemMeta(meta);
        return bloodBottle;
    }

    private void cursedSkin(Player player) {
        player.sendMessage("§4You are invulnerable for 5 seconds!");
        player.setInvulnerable(true);
        Bukkit.getScheduler().runTaskLater(this, () -> player.setInvulnerable(false), 100L);
    }

    private void forcedPull(Player player) {
        player.sendMessage("§4You have pulled nearby enemies!");
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof LivingEntity && entity != player) {
                Vector pullDirection = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(2);
                pullDirection.setY(1);
                entity.setVelocity(pullDirection);
            }
        }
    }

    private void redWave(Player player) {
        player.sendMessage("§4You have unleashed a Blood Wave!");
        Location loc = player.getLocation();
        World world = player.getWorld();
        world.spawnParticle(Particle.DUST, loc, 50, 2, 1, 2, new Particle.DustOptions(Color.RED, 2));
        for (Entity entity : player.getNearbyEntities(6, 3, 6)) {
            if (entity instanceof LivingEntity && entity != player) {
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
                entity.sendMessage("§4You have been stunned by a Blood Wave!");
            }
        }
    }
}
