package com.arixoffc.whoislying.util;

import com.arixoffc.whoislying.enums.GameState;
import com.arixoffc.whoislying.manager.GameManager;
import com.arixoffc.whoislying.model.GamePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

public final class ScoreboardUtil {

    private static final String TITLE = "§b§lWHO'S LYING?";

    private ScoreboardUtil() {
    }

    public static void updateScoreboard(Player player, GameManager gameManager) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("whoislying", "dummy", ColorUtil.colorize(TITLE));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 15;
        objective.getScore(ColorUtil.colorize("§7─────────────")).setScore(line--);
        objective.getScore(ColorUtil.colorize("§fStatus: " + getStateName(gameManager.getState()))).setScore(line--);
        objective.getScore(ColorUtil.colorize("§fFase: §e" + getPhaseName(gameManager.getCurrentPhase()))).setScore(line--);
        objective.getScore(ColorUtil.colorize("§fRonde: §a" + gameManager.getRoundNumber() + "/5")).setScore(line--);
        objective.getScore(ColorUtil.colorize("§7───────────── ")).setScore(line--);
        objective.getScore(ColorUtil.colorize("§fPemain: §a" + gameManager.getRegisteredPlayers().size())).setScore(line--);

        GamePlayer current = gameManager.getCurrentPlayer();
        if (current != null) {
            objective.getScore(ColorUtil.colorize("§fGiliran: §e" + current.getName())).setScore(line--);
        }

        int timeLeft = gameManager.getTimeLeft();
        if (timeLeft > 0) {
            objective.getScore(ColorUtil.colorize("§fTimer: §c" + timeLeft + "s")).setScore(line--);
        }

        objective.getScore(ColorUtil.colorize("§7─────────────  ")).setScore(line);
        player.setScoreboard(scoreboard);
    }

    public static void clearScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getNewScoreboard());
        }
    }

    private static String getStateName(GameState state) {
        return switch (state) {
            case IDLE -> "§eMenunggu";
            case PLAYING -> "§aBerlangsung";
            case ROUND_END -> "§6Ronde selesai";
            case ENDED -> "§7Selesai";
        };
    }

    private static String getPhaseName(GameManager.Phase phase) {
        return switch (phase) {
            case NONE -> "-";
            case GACHA -> "Gacha";
            case DESCRIPTION -> "Deskripsi";
            case DISCUSSION -> "Diskusi";
            case VOTE_CHECK -> "Vote Check";
            case VOTING -> "Voting";
            case REVEAL -> "Reveal";
            case IMPOSTOR_GUESS -> "Tebakan Impostor";
        };
    }
}
