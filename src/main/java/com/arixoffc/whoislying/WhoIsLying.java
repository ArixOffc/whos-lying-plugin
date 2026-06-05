package com.arixoffc.whoislying;

import com.arixoffc.whoislying.command.ModeratorCommand;
import com.arixoffc.whoislying.listener.ChatListener;
import com.arixoffc.whoislying.listener.ProtectionListener;
import com.arixoffc.whoislying.manager.BuildManager;
import com.arixoffc.whoislying.manager.GameManager;
import com.arixoffc.whoislying.manager.VoteManager;
import com.arixoffc.whoislying.manager.WordManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class WhoIsLying extends JavaPlugin {
    
    private GameManager gameManager;
    private BuildManager buildManager;
    private WordManager wordManager;
    private VoteManager voteManager;

    @Override
    public void onEnable() {
        getLogger().info("=================================");
        getLogger().info("  WHO'S LYING? - LOADING");
        getLogger().info("=================================");
        
        // Initialize managers
        wordManager = new WordManager(this);
        buildManager = new BuildManager(this);
        voteManager = new VoteManager();
        gameManager = new GameManager(this, buildManager, wordManager, voteManager);
        
        // Register commands (like SambungKata)
        ModeratorCommand modCmd = new ModeratorCommand(this, gameManager);
        String[] cmds = {"regis", "unregis", "listplayer", "listscore",
                        "start", "nextround", "endgame", "resetgame", "skip", "vote"};
        for (String cmd : cmds) {
            PluginCommand pc = getCommand(cmd);
            if (pc != null) {
                pc.setExecutor(modCmd);
                pc.setTabCompleter(modCmd);
            }
        }
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(gameManager), this);
        
        getLogger().info("=================================");
        getLogger().info("  WHO'S LYING? - ENABLED");
        getLogger().info("  Categories: " + wordManager.getTotalCategories());
        getLogger().info("  Words: " + wordManager.getTotalWords());
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("=================================");
        getLogger().info("  WHO'S LYING? - DISABLING");
        getLogger().info("=================================");
        
        // End any ongoing game & cleanup
        if (gameManager != null) {
            gameManager.cleanup();
        }
        
        getLogger().info("=================================");
        getLogger().info("  WHO'S LYING? - DISABLED");
        getLogger().info("=================================");
    }

    public GameManager getGameManager() { return gameManager; }
    public BuildManager getBuildManager() { return buildManager; }
    public WordManager getWordManager() { return wordManager; }
}
