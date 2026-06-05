package com.arixoffc.whoislying.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class TitleUtil {

    public static void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(
                ColorUtil.colorize(title),
                ColorUtil.colorize(subtitle),
                fadeIn,
                stay,
                fadeOut
        );
    }

    public static void sendTitle(Player player, String title, String subtitle) {
        sendTitle(player, title, subtitle, 10, 40, 10);
    }

    public static void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(
                ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(ColorUtil.colorize(message))
        );
    }

    public static void clearTitle(Player player) {
        player.resetTitle();
    }
}