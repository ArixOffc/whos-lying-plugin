package com.arixoffc.whoislying.listener;

import com.arixoffc.whoislying.WhoIsLying;
import com.arixoffc.whoislying.enums.GameState;
import com.arixoffc.whoislying.manager.GameManager;
import com.arixoffc.whoislying.model.GamePlayer;
import com.arixoffc.whoislying.util.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    
    private final WhoIsLying plugin;
    private final GameManager gameManager;

    public ChatListener(WhoIsLying plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        GamePlayer gamePlayer = gameManager.getGamePlayer(player.getUniqueId());
        
        if (gamePlayer == null) {
            return; // Player not in game
        }
        
        GameState state = gameManager.getState();
        String message = event.getMessage();
        
        if (state == GameState.PLAYING) {
            // Get current phase
            GameManager.Phase phase = gameManager.getCurrentPhase();
            
            if (phase == GameManager.Phase.DESCRIPTION) {
                event.setCancelled(true);
                
                // Only current turn player can chat
                if (!gameManager.isCurrentPlayer(player)) {
                    player.sendMessage(ColorUtil.colorize("&cBukan giliranmu untuk bicara!"));
                    return;
                }
                
                // Handle description
                plugin.getServer().getScheduler().runTask(plugin, () -> 
                    gameManager.handlePlayerDescription(player, message)
                );
            }
            else if (phase == GameManager.Phase.DISCUSSION) {
                // Allow free chat during discussion
                event.setFormat(ColorUtil.colorize("&8[&bDISKUSI&8] &7") + "%1$s§7: §f%2$s");
            }
            else if (phase == GameManager.Phase.VOTE_CHECK) {
                event.setCancelled(true);
                String cleanMsg = message.toUpperCase().trim();
                if (cleanMsg.equals("Y") || cleanMsg.equals("N")) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> 
                        gameManager.handleVoteCheckResponse(player, cleanMsg)
                    );
                } else {
                    player.sendMessage(ColorUtil.colorize("&cKetik &aY &catau &cN &csaja!"));
                }
            }
            else if (phase == GameManager.Phase.VOTING) {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.colorize("&cChat diblokir saat voting! Gunakan &e/vote <nama>"));
            }
            else if (phase == GameManager.Phase.IMPOSTOR_GUESS) {
                // Check if impostor is guessing
                if (gamePlayer.isImpostor()) {
                    event.setCancelled(true);
                    plugin.getServer().getScheduler().runTask(plugin, () -> 
                        gameManager.handleImpostorGuess(player, message)
                    );
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ColorUtil.colorize("&cChat diblokir saat reveal!"));
                }
            }
            else {
                event.setCancelled(true);
                player.sendMessage(ColorUtil.colorize("&cChat diblokir saat fase ini!"));
            }
        }
    }
}
