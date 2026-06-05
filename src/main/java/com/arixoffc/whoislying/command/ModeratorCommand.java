package com.arixoffc.whoislying.command;

import com.arixoffc.whoislying.WhoIsLying;
import com.arixoffc.whoislying.manager.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModeratorCommand implements CommandExecutor, TabCompleter {
    
    private final WhoIsLying plugin;
    private final GameManager gameManager;

    public ModeratorCommand(WhoIsLying plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Handle /vote command
        if (command.getName().equalsIgnoreCase("vote")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Hanya player yang bisa vote!");
                return true;
            }
            
            Player player = (Player) sender;
            
            if (args.length < 1) {
                player.sendMessage(ChatColor.RED + "Gunakan: /vote <nama>");
                return true;
            }
            
            gameManager.handleVote(player, args[0]);
            return true;
        }
        
        // All other commands require admin permission
        if (!sender.hasPermission("whoislying.admin")) {
            sender.sendMessage(ChatColor.RED + "Kamu tidak punya izin!");
            return true;
        }
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Command ini hanya dapat dijalankan oleh player.");
            return true;
        }

        String cmdName = command.getName().toLowerCase();
        
        switch (cmdName) {
            case "regis":
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /regis <nama>");
                    return true;
                }
                gameManager.registerPlayerByName(args[0], player);
                break;
                
            case "unregis":
                if (args.length < 1) {
                    sender.sendMessage(ChatColor.RED + "Gunakan: /unregis <nama>");
                    return true;
                }
                gameManager.unregisterPlayer(args[0], player);
                break;
                
            case "listplayer":
                gameManager.listPlayers(player);
                break;
                
            case "listscore":
                gameManager.listScore(player);
                break;
                
            case "start":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Hanya player yang bisa start game!");
                    return true;
                }
                gameManager.startGame(player);
                break;
                
            case "nextround":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Hanya player yang bisa next round!");
                    return true;
                }
                gameManager.nextRound(player);
                break;
                
            case "endgame":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Hanya player yang bisa end game!");
                    return true;
                }
                gameManager.endGame(player);
                break;
                
            case "resetgame":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Hanya player yang bisa reset game!");
                    return true;
                }
                gameManager.resetGame(player);
                break;
                
            case "skip":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.RED + "Hanya player yang bisa skip!");
                    return true;
                }
                gameManager.skipCurrentPlayer(player);
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Command tidak dikenal!");
                break;
        }
        
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("vote")) {
            if (args.length == 1) {
                return plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
            }
            return completions;
        }
        
        if (args.length == 1 && (command.getName().equalsIgnoreCase("regis") || 
                                 command.getName().equalsIgnoreCase("unregis"))) {
            return plugin.getServer().getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return completions;
    }
}
