package com.arixoffc.whoislying.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Collection;

public class SoundUtil {
    
    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public static void playSound(Player player, Sound sound) {
        playSound(player, sound, 1.0f, 1.0f);
    }

    public static void playGameStart(Player player) {
        playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    public static void playGameStart(Collection<? extends Player> players) {
        for (Player player : players) {
            playGameStart(player);
        }
    }

    public static void playYourTurn(Player player) {
        playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    public static void playDiscussion(Player player) {
        playSound(player, Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
    }

    public static void playVotingStart(Player player) {
        playSound(player, Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
    }

    public static void playInvestigatorWin(Player player) {
        playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }

    public static void playImpostorWin(Player player) {
        playSound(player, Sound.ENTITY_GHAST_SCREAM, 0.7f, 1.0f);
    }

    public static void playReveal(Player player) {
        playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
    }
}
