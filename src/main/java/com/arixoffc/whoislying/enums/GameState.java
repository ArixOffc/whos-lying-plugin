package com.arixoffc.whoislying.enums;

public enum GameState {
    IDLE,        // Belum mulai, registrasi player
    PLAYING,     // Game sedang berlangsung
    ROUND_END,   // Ronde selesai, tunggu /nextround
    ENDED        // Game selesai total
}
