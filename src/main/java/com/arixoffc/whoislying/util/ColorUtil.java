package com.arixoffc.whoislying.util;

import org.bukkit.ChatColor;

public class ColorUtil {
    
    public static String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String strip(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(message);
    }
}
