package com.arixoffc.whoislying.manager;

import com.arixoffc.whoislying.model.GamePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class VoteManager {
    
    private final Map<UUID, UUID> votes; // voter UUID -> voted player UUID
    private final Map<UUID, String> voteCheckResponses; // player UUID -> "Y" or "N"

    public VoteManager() {
        this.votes = new HashMap<>();
        this.voteCheckResponses = new HashMap<>();
    }

    public void vote(UUID voterUUID, UUID targetUUID) {
        votes.put(voterUUID, targetUUID);
    }

    public boolean hasVoted(UUID playerUUID) {
        return votes.containsKey(playerUUID);
    }

    public UUID getVoteTarget(UUID voterUUID) {
        return votes.get(voterUUID);
    }

    public GamePlayer getTopVoted(List<GamePlayer> players) {
        Map<UUID, Integer> voteCount = new HashMap<>();
        
        for (UUID targetUUID : votes.values()) {
            voteCount.put(targetUUID, voteCount.getOrDefault(targetUUID, 0) + 1);
        }
        
        if (voteCount.isEmpty()) {
            return null;
        }
        
        int maxVotes = Collections.max(voteCount.values());
        List<UUID> topVoted = new ArrayList<>();
        
        for (Map.Entry<UUID, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() == maxVotes) {
                topVoted.add(entry.getKey());
            }
        }
        
        // If tie, random selection
        UUID selectedUUID = topVoted.get(new Random().nextInt(topVoted.size()));
        
        return players.stream()
            .filter(p -> p.getUuid().equals(selectedUUID))
            .findFirst()
            .orElse(null);
    }

    public Map<UUID, Integer> getVoteResults() {
        Map<UUID, Integer> voteCount = new HashMap<>();
        for (UUID targetUUID : votes.values()) {
            voteCount.put(targetUUID, voteCount.getOrDefault(targetUUID, 0) + 1);
        }
        return voteCount;
    }

    public void reset() {
        votes.clear();
        voteCheckResponses.clear();
    }

    public void addVoteCheckResponse(UUID playerUUID, String response) {
        voteCheckResponses.put(playerUUID, response.toUpperCase());
    }

    public boolean hasRespondedToVoteCheck(UUID playerUUID) {
        return voteCheckResponses.containsKey(playerUUID);
    }

    public boolean isMajorityYes(int totalPlayers) {
        long yesCount = voteCheckResponses.values().stream()
            .filter(response -> response.equals("Y"))
            .count();
        return yesCount > (totalPlayers / 2.0);
    }

    public void resetVoteCheck() {
        voteCheckResponses.clear();
    }

    public int getTotalVotes() {
        return votes.size();
    }

    public int getYesCount() {
        return (int) voteCheckResponses.values().stream()
            .filter(response -> response.equals("Y"))
            .count();
    }

    public int getNoCount() {
        return (int) voteCheckResponses.values().stream()
            .filter(response -> response.equals("N"))
            .count();
    }
}
