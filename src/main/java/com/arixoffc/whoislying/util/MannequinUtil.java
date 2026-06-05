package com.arixoffc.whoislying.util;

import com.arixoffc.whoislying.model.GamePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public class MannequinUtil {
    
    private static final double MANNEQUIN_Y_OFFSET = -1.5;
    
    public static void spawnForPlayer(Plugin plugin, GamePlayer gp, Location seatLoc, float yaw) {
        Player player = Bukkit.getPlayer(gp.getUuid());
        if (player == null) return;
        
        World world = seatLoc.getWorld();
        
        // 1. Spawn invisible seat entity
        ArmorStand seat = world.spawn(seatLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.addScoreboardTag("whoislying_seat_" + gp.getUuid().toString());
        });
        gp.setSeatEntity(seat);
        
        // 2. Spawn mannequin armor stand
        Location mannequinLoc = seatLoc.clone().add(0, MANNEQUIN_Y_OFFSET, 0);
        ArmorStand mannequin = world.spawn(mannequinLoc, ArmorStand.class, stand -> {
            stand.setVisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.setSmall(false);
            
            // Apply player skin
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
            stand.setHelmet(head);
            
            // Set body armor
            if (player.getInventory().getChestplate() != null) {
                stand.setChestplate(player.getInventory().getChestplate().clone());
            }
            if (player.getInventory().getLeggings() != null) {
                stand.setLeggings(player.getInventory().getLeggings().clone());
            }
            if (player.getInventory().getBoots() != null) {
                stand.setBoots(player.getInventory().getBoots().clone());
            }
            
            stand.setRotation(yaw, 0);
            
            NamespacedKey key = new NamespacedKey(plugin, "whoislying_mannequin");
            stand.getPersistentDataContainer().set(key, PersistentDataType.STRING, gp.getUuid().toString());
        });
        
        // Make mannequin sit on seat
        seat.addPassenger(mannequin);
        gp.setMannequin(mannequin);
        
        // 3. Spawn text display (nama player di atas kepala)
        Location textLoc = mannequinLoc.clone().add(0, 2.3, 0);
        TextDisplay textDisplay = world.spawn(textLoc, TextDisplay.class, display -> {
            display.text(Component.text(gp.getName()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        });
        gp.setTextDisplay(textDisplay);
    }
}
