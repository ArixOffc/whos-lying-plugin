package com.arixoffc.whoislying.model;

import com.arixoffc.whoislying.enums.PlayerRole;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.UUID;

public class GamePlayer {
    private final String name;
    private final UUID uuid;
    private PlayerRole role;
    private int order;
    private boolean hasSpoken;
    private boolean hasVoted;
    private UUID votedFor;
    private int score;
    
    // Visual entities
    private ArmorStand mannequin;
    private TextDisplay textDisplay;
    private Entity seatEntity;
    
    // Spawn & camera
    private Location chairLocation;
    private Location spawnpoint;
    private Location viewerLocation;
    private float viewerYaw;
    private float viewerPitch;
    
    private GameMode savedGameMode;

    public GamePlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
        this.role = PlayerRole.INVESTIGATOR;
        this.hasSpoken = false;
        this.hasVoted = false;
        this.votedFor = null;
        this.score = 0;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public boolean isOnline() {
        Player p = getPlayer();
        return p != null && p.isOnline();
    }

    public void reset() {
        this.hasSpoken = false;
        this.hasVoted = false;
        this.votedFor = null;
        this.role = PlayerRole.INVESTIGATOR;
    }

    public void resetForNewRound() {
        this.hasSpoken = false;
        this.hasVoted = false;
        this.votedFor = null;
    }

    // Getters & Setters
    public String getName() { return name; }
    public UUID getUuid() { return uuid; }
    public PlayerRole getRole() { return role; }
    public void setRole(PlayerRole role) { this.role = role; }
    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }
    public boolean hasSpoken() { return hasSpoken; }
    public void setHasSpoken(boolean hasSpoken) { this.hasSpoken = hasSpoken; }
    public boolean hasVoted() { return hasVoted; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }
    public UUID getVotedFor() { return votedFor; }
    public void setVotedFor(UUID votedFor) { 
        this.votedFor = votedFor;
        this.hasVoted = true;
    }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public void addScore(int points) { this.score += points; }

    public boolean isImpostor() { return role == PlayerRole.IMPOSTOR; }
    public boolean isInvestigator() { return role == PlayerRole.INVESTIGATOR; }

    // Visual entities
    public ArmorStand getMannequin() { return mannequin; }
    public void setMannequin(ArmorStand mannequin) { this.mannequin = mannequin; }
    public TextDisplay getTextDisplay() { return textDisplay; }
    public void setTextDisplay(TextDisplay textDisplay) { this.textDisplay = textDisplay; }
    public Entity getSeatEntity() { return seatEntity; }
    public void setSeatEntity(Entity seatEntity) { this.seatEntity = seatEntity; }

    // Locations
    public Location getChairLocation() { return chairLocation; }
    public void setChairLocation(Location chairLocation) { this.chairLocation = chairLocation; }
    public Location getSpawnpoint() { return spawnpoint; }
    public void setSpawnpoint(Location spawnpoint) { this.spawnpoint = spawnpoint; }
    public Location getViewerLocation() { return viewerLocation; }
    public void setViewerLocation(Location viewerLocation) { this.viewerLocation = viewerLocation; }
    public float getViewerYaw() { return viewerYaw; }
    public void setViewerYaw(float viewerYaw) { this.viewerYaw = viewerYaw; }
    public float getViewerPitch() { return viewerPitch; }
    public void setViewerPitch(float viewerPitch) { this.viewerPitch = viewerPitch; }

    // GameMode
    public GameMode getSavedGameMode() { return savedGameMode; }
    public void setSavedGameMode(GameMode savedGameMode) { this.savedGameMode = savedGameMode; }
}
