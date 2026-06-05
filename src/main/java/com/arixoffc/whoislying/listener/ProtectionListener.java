package com.arixoffc.whoislying.listener;

import com.arixoffc.whoislying.enums.GameState;
import com.arixoffc.whoislying.manager.GameManager;
import com.arixoffc.whoislying.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProtectionListener implements Listener {
    
    private final GameManager gameManager;

    public ProtectionListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getGamePlayer(player.getUniqueId()) != null) {
            gameManager.removePlayer(player.getUniqueId());
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        if (gameManager.getGamePlayer(player.getUniqueId()) == null) {
            return;
        }
        
        GameState state = gameManager.getState();
        if (state == GameState.PLAYING) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player damager = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        if (gameManager.getGamePlayer(damager.getUniqueId()) == null && 
            gameManager.getGamePlayer(victim.getUniqueId()) == null) {
            return;
        }
        
        GameState state = gameManager.getState();
        if (state == GameState.PLAYING) {
            event.setCancelled(true);
            damager.sendMessage(ColorUtil.colorize("&cPVP dinonaktifkan saat game berlangsung!"));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getGamePlayer(player.getUniqueId()) == null) {
            return;
        }
        
        GameState state = gameManager.getState();
        if (state == GameState.PLAYING) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize("&cKamu tidak bisa break block saat game berlangsung!"));
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (gameManager.getGamePlayer(player.getUniqueId()) == null) {
            return;
        }
        
        GameState state = gameManager.getState();
        if (state == GameState.PLAYING) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.colorize("&cKamu tidak bisa place block saat game berlangsung!"));
        }
    }
}
