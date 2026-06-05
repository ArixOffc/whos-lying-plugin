package com.arixoffc.whoislying.manager;

import com.arixoffc.whoislying.WhoIsLying;
import com.arixoffc.whoislying.enums.GameState;
import com.arixoffc.whoislying.enums.PlayerRole;
import com.arixoffc.whoislying.model.GamePlayer;
import com.arixoffc.whoislying.util.ColorUtil;
import com.arixoffc.whoislying.util.MannequinUtil;
import com.arixoffc.whoislying.util.SoundUtil;
import com.arixoffc.whoislying.util.TitleUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class GameManager {
    
    public enum Phase {
        NONE, GACHA, DESCRIPTION, DISCUSSION, VOTE_CHECK, VOTING, REVEAL, IMPOSTOR_GUESS
    }
    
    private final WhoIsLying plugin;
    private final BuildManager buildManager;
    private final WordManager wordManager;
    private final VoteManager voteManager;
    
    private GameState state = GameState.IDLE;
    private Phase currentPhase = Phase.NONE;
    
    private final List<GamePlayer> registeredPlayers = new ArrayList<>();
    private final List<GamePlayer> playOrder = new ArrayList<>();
    private final LinkedList<GamePlayer> descriptionQueue = new LinkedList<>();
    
    private GamePlayer currentPlayer;
    private GamePlayer impostor;
    private String secretWord;
    private String category;
    private int roundNumber;
    private int timeLeft;
    
    private Location currentViewerLocation;
    private float currentViewerYaw;
    private float currentViewerPitch = 25f;
    
    private BukkitTask timerTask;
    private BukkitTask cameraAnimTask;
    private BukkitTask cameraLockTask;
    private BukkitTask gachaSpinTask;
    private BukkitTask gachaFinishTask;

    public GameManager(WhoIsLying plugin, BuildManager buildManager, WordManager wordManager, VoteManager voteManager) {
        this.plugin = plugin;
        this.buildManager = buildManager;
        this.wordManager = wordManager;
        this.voteManager = voteManager;
    }

    public WhoIsLying getPlugin() { return plugin; }

    // ─── Registration ───────────────────────────────────────────────────────

    public boolean registerPlayerByName(String rawName, Player mod) {
        if (state == GameState.PLAYING) {
            mod.sendMessage(ChatColor.RED + "Tidak bisa mendaftarkan player saat permainan berlangsung.");
            return false;
        }

        String name = rawName.trim();
        if (name.isEmpty()) {
            mod.sendMessage(ChatColor.RED + "Nama player tidak boleh kosong.");
            return false;
        }

        if (registeredPlayers.size() >= 8) {
            mod.sendMessage(ChatColor.RED + "Maksimal 8 player sudah terdaftar!");
            return false;
        }

        for (GamePlayer gp : registeredPlayers) {
            if (gp.getName().equalsIgnoreCase(name)) {
                mod.sendMessage(ChatColor.YELLOW + name + " sudah terdaftar.");
                return false;
            }
        }

        Player online = Bukkit.getPlayerExact(name);
        if (online == null) {
            mod.sendMessage(ChatColor.RED + "Player " + name + " tidak ada di server.");
            return false;
        }

        registeredPlayers.add(new GamePlayer(online.getName(), online.getUniqueId()));
        mod.sendMessage(ChatColor.GREEN + online.getName() + " berhasil didaftarkan! ("
                + registeredPlayers.size() + "/8)");
        return true;
    }

    public boolean unregisterPlayer(String name, Player mod) {
        Iterator<GamePlayer> it = registeredPlayers.iterator();
        while (it.hasNext()) {
            GamePlayer gp = it.next();
            if (gp.getName().equalsIgnoreCase(name)) {
                it.remove();
                mod.sendMessage(ChatColor.GREEN + name + " berhasil dihapus dari daftar.");
                return true;
            }
        }
        mod.sendMessage(ChatColor.RED + "Player " + name + " tidak ditemukan di daftar.");
        return false;
    }

    public void listPlayers(Player mod) {
        mod.sendMessage(ChatColor.GOLD + "══ Daftar Player ══");
        if (registeredPlayers.isEmpty()) {
            mod.sendMessage(ChatColor.GRAY + "  (kosong)");
        } else {
            for (int i = 0; i < registeredPlayers.size(); i++) {
                GamePlayer gp = registeredPlayers.get(i);
                String status = gp.isOnline() ? "" : ChatColor.GRAY + " (offline)";
                mod.sendMessage(ChatColor.YELLOW + "  " + (i + 1) + ". " + gp.getName() + status);
            }
        }
        mod.sendMessage(ChatColor.GOLD + "Total: " + registeredPlayers.size() + " player");
    }

    public void removePlayer(UUID uuid) {
        GamePlayer gp = getGamePlayer(uuid);
        if (gp != null) {
            registeredPlayers.remove(gp);
            playOrder.remove(gp);
            descriptionQueue.remove(gp);
            
            if (state == GameState.PLAYING && gp.isImpostor()) {
                broadcastAll(ChatColor.RED + "Impostor disconnect! Investigator menang!");
                endGameInvestigatorWin("Impostor disconnect");
            } else if (registeredPlayers.size() < 3 && state == GameState.PLAYING) {
                broadcastAll(ChatColor.RED + "Tidak cukup pemain! Game dihentikan.");
                forceEndGame("Tidak cukup pemain");
            }
        }
    }

    // ─── Start Game ──────────────────────────────────────────────────────────

    public boolean startGame(Player mod) {
        if (state != GameState.IDLE) {
            mod.sendMessage(ChatColor.RED + (state == GameState.PLAYING
                    ? "Permainan sedang berlangsung!"
                    : "Gunakan /nextround untuk ronde berikutnya atau /endgame untuk mengakhiri."));
            return false;
        }
        if (registeredPlayers.size() < 3) {
            mod.sendMessage(ChatColor.RED + "Minimal 3 player diperlukan! Saat ini: " + registeredPlayers.size());
            return false;
        }
        if (registeredPlayers.size() > 8) {
            mod.sendMessage(ChatColor.RED + "Maksimal 8 player! Saat ini: " + registeredPlayers.size());
            return false;
        }

        beginRoundSession(mod);
        return true;
    }

    private void beginRoundSession(Player mod) {
        state = GameState.PLAYING;
        currentPhase = Phase.GACHA;
        roundNumber = 0;
        playOrder.clear();
        descriptionQueue.clear();
        currentPlayer = null;

        for (GamePlayer gp : registeredPlayers) {
            gp.resetForNewRound();
            Player p = gp.getPlayer();
            if (p != null) applyPlayerGameEffects(p, gp);
        }

        broadcastAll(ChatColor.GOLD + "══════════════════");
        broadcastAll(ChatColor.GOLD + "  Permainan Dimulai!");
        broadcastAll(ChatColor.GOLD + "══════════════════");

        buildManager.buildArena(mod.getLocation(), mod.getLocation().getYaw(), registeredPlayers.size());
        SoundUtil.playGameStart(getOnlinePlayers());
        startGacha();
    }

    private void applyPlayerGameEffects(Player p, GamePlayer gp) {
        gp.setSavedGameMode(p.getGameMode());
        p.setGameMode(GameMode.SURVIVAL);
        p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        p.getInventory().clear();
    }

    private void restorePlayerNormalState(Player p, GamePlayer gp) {
        if (p == null) return;
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.getInventory().clear();
        if (gp.getSavedGameMode() != null) {
            p.setGameMode(gp.getSavedGameMode());
        } else {
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    // ─── Gacha ────────────────────────────────────────────────────────────────

    private void startGacha() {
        cancelGachaTasks();

        List<Integer> orders = new ArrayList<>();
        for (int i = 1; i <= registeredPlayers.size(); i++) orders.add(i);
        Collections.shuffle(orders);
        for (int i = 0; i < registeredPlayers.size(); i++) {
            registeredPlayers.get(i).setOrder(orders.get(i));
        }

        int totalPlayers = registeredPlayers.size();
        final int[] frame = {0};
        final int spinFrames = 20;

        gachaSpinTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                if (frame[0] < spinFrames) {
                    for (GamePlayer gp : registeredPlayers) {
                        Player p = gp.getPlayer();
                        if (p == null) continue;
                        int rand = new Random().nextInt(totalPlayers) + 1;
                        TitleUtil.sendTitle(p, ChatColor.GOLD + "Kamu berada pada urutan ke",
                                ChatColor.WHITE + String.valueOf(rand), 0, 4, 0);
                        SoundUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                    }
                    frame[0]++;
                } else {
                    cancel();
                    for (GamePlayer gp : registeredPlayers) {
                        Player p = gp.getPlayer();
                        if (p == null) continue;
                        TitleUtil.sendTitle(p,
                                ChatColor.GOLD + "Kamu berada pada urutan ke",
                                ChatColor.YELLOW + "" + ChatColor.BOLD + gp.getOrder(),
                                5, 60, 20);
                        SoundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP);
                    }
                    gachaFinishTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (state != GameState.PLAYING) return;
                            setupPlayOrder();
                            assignRoles();
                            spawnMannequins();
                            setSpawnpoints();
                            startDescriptionPhase();
                        }
                    }.runTaskLater(plugin, 80L);
                }
            }
        }.runTaskTimer(plugin, 5L, 2L);
    }

    private void cancelGachaTasks() {
        if (gachaSpinTask != null && !gachaSpinTask.isCancelled()) {
            gachaSpinTask.cancel();
        }
        if (gachaFinishTask != null && !gachaFinishTask.isCancelled()) {
            gachaFinishTask.cancel();
        }
        gachaSpinTask = null;
        gachaFinishTask = null;
    }

    private void setupPlayOrder() {
        playOrder.clear();
        List<GamePlayer> sorted = new ArrayList<>(registeredPlayers);
        sorted.sort(Comparator.comparingInt(GamePlayer::getOrder));
        playOrder.addAll(sorted);
    }

    private void assignRoles() {
        // Pick random impostor
        impostor = playOrder.get(new Random().nextInt(playOrder.size()));
        impostor.setRole(PlayerRole.IMPOSTOR);
        
        // Pick random word
        Map<String, String> wordData = wordManager.getRandomWordWithCategory();
        if (wordData == null) {
            broadcastAll(ChatColor.RED + "Error: Tidak ada kata yang tersedia!");
            forceEndGame("No words available");
            return;
        }
        
        category = wordData.get("category");
        secretWord = wordData.get("word");
        
        // Send role info to all players
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (GamePlayer gp : registeredPlayers) {
                Player p = gp.getPlayer();
                if (p == null) continue;
                
                if (gp.isImpostor()) {
                    TitleUtil.sendTitle(p, "§c§l🤥 IMPOSTOR", "§eKategori: §b" + category, 10, 80, 20);
                    p.sendMessage(ColorUtil.colorize("&8[&c&l!&8] &cKamu adalah IMPOSTOR!"));
                    p.sendMessage(ColorUtil.colorize("&7Kategori: &b" + category));
                    p.sendMessage(ColorUtil.colorize("&7Cari tahu kata rahasianya dari clue investigator!"));
                } else {
                    TitleUtil.sendTitle(p, "§a§l🕵 INVESTIGATOR", "§eKata: §e§l" + secretWord, 10, 80, 20);
                    p.sendMessage(ColorUtil.colorize("&8[&a&l✓&8] &aKamu adalah INVESTIGATOR!"));
                    p.sendMessage(ColorUtil.colorize("&7Kategori: &b" + category));
                    p.sendMessage(ColorUtil.colorize("&7Kata Rahasia: &e&l" + secretWord));
                    p.sendMessage(ColorUtil.colorize("&7Berikan deskripsi tanpa menyebutkan kata secara langsung!"));
                }
                SoundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP);
            }
        }, 20L);
    }

    private void spawnMannequins() {
        Map<Integer, Location> chairs = buildManager.getChairLocations();
        Location center = buildManager.getCenterLocation();
        if (center == null) return;

        for (GamePlayer gp : playOrder) {
            Location chairLoc = chairs.get(gp.getOrder());
            if (chairLoc == null) continue;

            gp.setChairLocation(chairLoc.clone());

            Location seatLoc = buildManager.getMannequinSeatLocation(chairLoc);
            float yaw = yawToward(seatLoc, center);
            MannequinUtil.spawnForPlayer(plugin, gp, seatLoc, yaw);

            World world = chairLoc.getWorld();
            double[] vPos = buildManager.getViewerPosition(chairLoc, center);
            gp.setViewerLocation(new Location(world, vPos[0], vPos[1], vPos[2]));
            gp.setViewerYaw((float) vPos[3]);
            gp.setViewerPitch((float) vPos[4]);
        }
    }

    private float yawToward(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    private void setSpawnpoints() {
        for (GamePlayer gp : playOrder) {
            Player p = gp.getPlayer();
            if (p == null || gp.getChairLocation() == null) continue;
            Location chair = gp.getChairLocation().clone().add(0.5, 0.5, 0.5);
            gp.setSpawnpoint(chair);
            p.setRespawnLocation(chair, true);
        }
    }

    // ─── Description Phase ────────────────────────────────────────────────────

    private void startDescriptionPhase() {
        currentPhase = Phase.DESCRIPTION;
        roundNumber++;
        
        descriptionQueue.clear();
        for (GamePlayer gp : playOrder) {
            gp.setHasSpoken(false);
            descriptionQueue.add(gp);
        }

        broadcastAll(ChatColor.AQUA + "═══ FASE DESKRIPSI - RONDE " + roundNumber + " ═══");
        broadcastAll(ChatColor.GRAY + "Setiap pemain akan giliran memberikan deskripsi!");

        Bukkit.getScheduler().runTaskLater(plugin, this::nextTurn, 40L);
    }

    private void nextTurn() {
        stopTimer();

        GamePlayer next = null;
        while (!descriptionQueue.isEmpty()) {
            GamePlayer candidate = descriptionQueue.poll();
            if (candidate.isOnline()) {
                next = candidate;
                break;
            }
        }

        if (next == null) {
            startDiscussion();
            return;
        }

        currentPlayer = next;
        String offlineHint = currentPlayer.isOnline() ? "" : ChatColor.GRAY + " [offline - /skip]";
        broadcastAll(ChatColor.AQUA + "Giliran " + ChatColor.YELLOW + currentPlayer.getName()
                + ChatColor.AQUA + " untuk Menjawab" + offlineHint);
        SoundUtil.playSound(currentPlayer.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

        animateCameraTo(currentPlayer);
        startTimer(60);
    }

    public void handlePlayerDescription(Player player, String message) {
        if (currentPhase != Phase.DESCRIPTION) return;
        if (currentPlayer == null || !player.getUniqueId().equals(currentPlayer.getUuid())) {
            player.sendMessage(ColorUtil.colorize("&cBukan giliranmu untuk bicara!"));
            return;
        }

        // Word filter
        String cleanMessage = ColorUtil.strip(message).toLowerCase();
        String cleanWord = secretWord.toLowerCase();
        if (cleanMessage.contains(cleanWord)) {
            player.sendMessage(ColorUtil.colorize("&cKamu tidak boleh menyebutkan kata rahasia secara langsung!"));
            return;
        }

        currentPlayer.setHasSpoken(true);
        String formattedMessage = ColorUtil.colorize("&7[&f" + player.getName() + "&7] &f" + message);
        registeredPlayers.forEach(gp -> {
            if (gp.getPlayer() != null) gp.getPlayer().sendMessage(formattedMessage);
        });

        stopTimer();
        Bukkit.getScheduler().runTaskLater(plugin, this::nextTurn, 20L);
    }

    // ─── Discussion Phase ─────────────────────────────────────────────────────

    private void startDiscussion() {
        currentPhase = Phase.DISCUSSION;
        
        broadcastAll(ChatColor.AQUA + "═══ FASE DISKUSI ═══");
        broadcastAll(ChatColor.GRAY + "Diskusikan siapa yang mencurigakan!");
        broadcastAll(ChatColor.GRAY + "Waktu: 60 detik");
        
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getPlayer() != null) {
                SoundUtil.playSound(gp.getPlayer(), Sound.BLOCK_BELL_USE);
            }
        }
        
        startTimer(60);
    }

    // ─── Vote Check Phase ─────────────────────────────────────────────────────

    private void startVoteCheck() {
        if (roundNumber >= 5) {
            broadcastAll(ChatColor.RED + "⚠ Ronde maksimal tercapai! Voting dipaksa!");
            Bukkit.getScheduler().runTaskLater(plugin, this::startVoting, 40L);
            return;
        }
        
        currentPhase = Phase.VOTE_CHECK;
        voteManager.resetVoteCheck();
        
        broadcastAll(ChatColor.GOLD + "═══ VOTE CHECK ═══");
        broadcastAll(ChatColor.YELLOW + "Mulai voting sekarang?");
        broadcastAll(ChatColor.GRAY + "Ketik Y (Yes) atau N (No) di chat!");
        
        startTimer(20);
    }

    public void handleVoteCheckResponse(Player player, String response) {
        if (currentPhase != Phase.VOTE_CHECK) return;
        
        GamePlayer gamePlayer = getGamePlayer(player.getUniqueId());
        if (gamePlayer == null) return;
        
        if (voteManager.hasRespondedToVoteCheck(player.getUniqueId())) {
            player.sendMessage(ColorUtil.colorize("&cKamu sudah memberikan respon!"));
            return;
        }
        
        voteManager.addVoteCheckResponse(player.getUniqueId(), response);
        player.sendMessage(ColorUtil.colorize("&aRespon dicatat: &e" + response));
        
        broadcastAll(ChatColor.GRAY + player.getName() + " telah memberikan respon. " + ChatColor.DARK_GRAY + "(" +
            (voteManager.getYesCount() + voteManager.getNoCount()) + "/" + registeredPlayers.size() + ")");
        
        if ((voteManager.getYesCount() + voteManager.getNoCount()) >= registeredPlayers.size()) {
            stopTimer();
            processVoteCheck();
        }
    }

    private void processVoteCheck() {
        if (voteManager.isMajorityYes(registeredPlayers.size())) {
            broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "Mayoritas setuju! Voting dimulai!");
            Bukkit.getScheduler().runTaskLater(plugin, this::startVoting, 40L);
        } else {
            broadcastAll(ChatColor.RED.toString() + ChatColor.BOLD + "Mayoritas tidak setuju! Ronde baru dimulai!");
            Bukkit.getScheduler().runTaskLater(plugin, this::startDescriptionPhase, 40L);
        }
    }

    // ─── Voting Phase ─────────────────────────────────────────────────────────

    private void startVoting() {
        currentPhase = Phase.VOTING;
        voteManager.reset();
        
        broadcastAll(ChatColor.RED + "═══ FASE VOTING ═══");
        broadcastAll(ChatColor.GRAY + "Vote siapa yang menurut kamu IMPOSTOR!");
        broadcastAll(ChatColor.GRAY + "Gunakan: " + ChatColor.YELLOW + "/vote <nama>");
        broadcastAll(ChatColor.GRAY + "Waktu: 30 detik");
        
        for (GamePlayer gp : registeredPlayers) {
            gp.setHasVoted(false);
            if (gp.getPlayer() != null) {
                SoundUtil.playSound(gp.getPlayer(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
            }
        }
        
        startTimer(30);
    }

    public void handleVote(Player player, String targetName) {
        if (currentPhase != Phase.VOTING) {
            player.sendMessage(ColorUtil.colorize("&cBukan fase voting!"));
            return;
        }
        
        GamePlayer voter = getGamePlayer(player.getUniqueId());
        if (voter == null) return;
        
        if (voter.hasVoted()) {
            player.sendMessage(ColorUtil.colorize("&cKamu sudah vote!"));
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ColorUtil.colorize("&cPemain tidak ditemukan!"));
            return;
        }
        
        GamePlayer targetPlayer = getGamePlayer(target.getUniqueId());
        if (targetPlayer == null) {
            player.sendMessage(ColorUtil.colorize("&cPemain tidak ada dalam game!"));
            return;
        }
        
        if (voter.getUuid().equals(targetPlayer.getUuid())) {
            player.sendMessage(ColorUtil.colorize("&cKamu tidak bisa vote dirimu sendiri!"));
            return;
        }
        
        voteManager.vote(voter.getUuid(), targetPlayer.getUuid());
        voter.setVotedFor(targetPlayer.getUuid());
        voter.setHasVoted(true);
        
        player.sendMessage(ColorUtil.colorize("&aKamu vote: &e" + target.getName()));
        broadcastAll(ChatColor.GRAY + player.getName() + " telah vote. " + ChatColor.DARK_GRAY + "(" + 
            voteManager.getTotalVotes() + "/" + registeredPlayers.size() + ")");
        
        if (voteManager.getTotalVotes() >= registeredPlayers.size()) {
            stopTimer();
            Bukkit.getScheduler().runTaskLater(plugin, this::revealResult, 20L);
        }
    }

    // ─── Reveal Phase ─────────────────────────────────────────────────────────

    private void revealResult() {
        currentPhase = Phase.REVEAL;
        
        broadcastAll(ChatColor.YELLOW + "═══ HASIL VOTING ═══");
        
        Map<UUID, Integer> results = voteManager.getVoteResults();
        broadcastAll(ChatColor.GRAY + "Hasil:");
        for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
            GamePlayer gp = getGamePlayer(entry.getKey());
            if (gp != null) {
                broadcastAll("  " + ChatColor.YELLOW + gp.getName() + ": " + ChatColor.RED + entry.getValue() + " suara");
            }
        }
        
        GamePlayer topVoted = voteManager.getTopVoted(new ArrayList<>(registeredPlayers));
        
        if (topVoted == null) {
            broadcastAll(ChatColor.RED + "Tidak ada yang di-vote! Impostor menang!");
            endGameImpostorWin("Tidak ada vote");
            return;
        }
        
        broadcastAll("");
        broadcastAll(ChatColor.YELLOW + topVoted.getName() + ChatColor.GRAY + " telah dikick dari game!");
        broadcastAll(ChatColor.GRAY + "Apakah dia Impostor...?");
        
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getPlayer() != null) {
                SoundUtil.playSound(gp.getPlayer(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.0f);
            }
        }
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (topVoted.isImpostor()) {
                broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "✓ YA! Dia adalah IMPOSTOR!");
                broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "INVESTIGATOR MENANG!");
                endGameInvestigatorWin("Impostor terkick");
            } else {
                broadcastAll(ChatColor.RED.toString() + ChatColor.BOLD + "✗ BUKAN! Dia adalah Investigator!");
                broadcastAll(ChatColor.GOLD.toString() + ChatColor.BOLD + "Impostor mendapat kesempatan menebak kata!");
                broadcastAll(ChatColor.GRAY + "Impostor, ketik tebakanmu di chat dalam 15 detik!");
                
                startImpostorGuessPhase();
            }
        }, 60L);
    }

    private void startImpostorGuessPhase() {
        currentPhase = Phase.IMPOSTOR_GUESS;
        startTimer(15);
    }

    public void handleImpostorGuess(Player player, String guess) {
        if (currentPhase != Phase.IMPOSTOR_GUESS) return;
        
        GamePlayer gamePlayer = getGamePlayer(player.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isImpostor()) return;
        
        stopTimer();
        
        broadcastAll(ChatColor.GOLD + "Impostor menebak: " + ChatColor.YELLOW + ChatColor.BOLD + guess);
        
        if (isImpostorGuessCorrect(guess)) {
            broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "✓ BENAR!");
            broadcastAll(ChatColor.RED.toString() + ChatColor.BOLD + "IMPOSTOR MENANG!");
            broadcastAll(ChatColor.GRAY + "Kata rahasia: " + ChatColor.YELLOW + ChatColor.BOLD + secretWord);
            endGameImpostorWin("Impostor menebak dengan benar");
        } else {
            broadcastAll(ChatColor.RED.toString() + ChatColor.BOLD + "✗ SALAH!");
            broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "INVESTIGATOR MENANG!");
            broadcastAll(ChatColor.GRAY + "Kata rahasia: " + ChatColor.YELLOW + ChatColor.BOLD + secretWord);
            endGameInvestigatorWin("Impostor salah menebak");
        }
    }

    public boolean isImpostorGuessCorrect(String guess) {
        String cleanGuess = ColorUtil.strip(guess).trim().toLowerCase();
        String cleanWord = secretWord.toLowerCase();
        return cleanGuess.equals(cleanWord);
    }

    // ─── End Game ─────────────────────────────────────────────────────────────

    private void endGameInvestigatorWin(String reason) {
        for (GamePlayer gp : registeredPlayers) {
            if (gp.isInvestigator()) {
                gp.addScore(1);
                if (gp.getPlayer() != null) {
                    gp.getPlayer().giveExp(50);
                    SoundUtil.playSound(gp.getPlayer(), Sound.UI_TOAST_CHALLENGE_COMPLETE);
                }
            } else {
                if (gp.getPlayer() != null) {
                    SoundUtil.playSound(gp.getPlayer(), Sound.ENTITY_GHAST_SCREAM, 0.7f, 1.0f);
                }
            }
        }
        
        broadcastAll("");
        broadcastAll(ChatColor.GRAY + "Impostor: " + ChatColor.RED + impostor.getName());
        broadcastAll(ChatColor.GRAY + "Kategori: " + ChatColor.AQUA + category);
        broadcastAll(ChatColor.GRAY + "Kata: " + ChatColor.YELLOW + ChatColor.BOLD + secretWord);
        
        Bukkit.getScheduler().runTaskLater(plugin, this::endRound, 100L);
    }

    private void endGameImpostorWin(String reason) {
        impostor.addScore(3);
        if (impostor.getPlayer() != null) {
            impostor.getPlayer().giveExp(150);
        }
        
        for (GamePlayer gp : registeredPlayers) {
            if (gp.isImpostor()) {
                if (gp.getPlayer() != null) {
                    SoundUtil.playSound(gp.getPlayer(), Sound.ENTITY_GHAST_SCREAM, 0.7f, 1.0f);
                }
            } else {
                if (gp.getPlayer() != null) {
                    SoundUtil.playSound(gp.getPlayer(), Sound.UI_TOAST_CHALLENGE_COMPLETE);
                }
            }
        }
        
        broadcastAll("");
        broadcastAll(ChatColor.GRAY + "Impostor: " + ChatColor.RED + impostor.getName());
        broadcastAll(ChatColor.GRAY + "Kategori: " + ChatColor.AQUA + category);
        
        Bukkit.getScheduler().runTaskLater(plugin, this::endRound, 100L);
    }

    private void endRound() {
        stopAllTimers();
        clearAllTextDisplays();
        
        for (GamePlayer gp : playOrder) removeMannequin(gp);
        removeAllArenaEntities();
        
        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) {
                restorePlayerNormalState(p, gp);
            }
        }

        broadcastAll(ChatColor.GOLD + "══════════════════");
        broadcastAll(ChatColor.YELLOW + "Gunakan /nextround untuk ronde berikutnya atau /endgame untuk mengakhiri.");

        state = GameState.ROUND_END;
        currentPhase = Phase.NONE;
        currentPlayer = null;
    }

    public void endGame(Player mod) {
        stopAllTimers();
        
        broadcastAll(ChatColor.GOLD + "══════════════════");
        broadcastAll(ChatColor.GOLD + "  PERMAINAN BERAKHIR  ");
        displayScores();
        broadcastAll(ChatColor.GOLD + "══════════════════");

        buildManager.restoreBlocks();
        for (GamePlayer gp : playOrder) removeMannequin(gp);
        for (GamePlayer gp : registeredPlayers) removeMannequin(gp);
        removeAllArenaEntities();

        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) {
                restorePlayerNormalState(p, gp);
            }
            gp.setScore(0);
        }

        state = GameState.IDLE;
        currentPhase = Phase.NONE;
        playOrder.clear();
        descriptionQueue.clear();
        currentPlayer = null;
        secretWord = null;
        roundNumber = 0;

        mod.sendMessage(ChatColor.GREEN + "Game telah diakhiri. Gunakan /start untuk memulai permainan baru.");
    }

    public boolean nextRound(Player mod) {
        if (state == GameState.PLAYING) {
            mod.sendMessage(ChatColor.RED + "Ronde sedang berlangsung! Tunggu hingga selesai.");
            return false;
        }
        if (state == GameState.IDLE) {
            mod.sendMessage(ChatColor.RED + "Belum ada permainan. Gunakan /start terlebih dahulu.");
            return false;
        }
        if (registeredPlayers.size() < 3) {
            mod.sendMessage(ChatColor.RED + "Minimal 3 player!");
            return false;
        }

        if (!buildManager.hasArena()) {
            mod.sendMessage(ChatColor.RED + "Arena tidak ditemukan. Gunakan /start terlebih dahulu.");
            return false;
        }

        roundNumber = 0;
        
        for (GamePlayer gp : playOrder) {
            removeMannequin(gp);
        }
        clearAllTextDisplays();

        buildManager.rebuildArenaAtSavedCenter(registeredPlayers.size());

        for (GamePlayer gp : registeredPlayers) {
            gp.resetForNewRound();
            Player p = gp.getPlayer();
            if (p != null) {
                applyPlayerGameEffects(p, gp);
                p.getInventory().clear();
            }
        }

        broadcastAll(ChatColor.GOLD + "══ Ronde Baru Dimulai! ══");
        state = GameState.PLAYING;
        currentPhase = Phase.GACHA;
        SoundUtil.playGameStart(getOnlinePlayers());
        startGacha();
        return true;
    }

    public boolean resetGame(Player mod) {
        if (state != GameState.PLAYING) {
            mod.sendMessage(ChatColor.RED + "Tidak ada permainan yang sedang berlangsung!");
            return false;
        }

        stopAllTimers();
        cancelGachaTasks();
        clearAllTextDisplays();

        for (GamePlayer gp : registeredPlayers) {
            gp.setScore(0);
            gp.resetForNewRound();
        }

        for (GamePlayer gp : registeredPlayers) removeMannequin(gp);
        for (GamePlayer gp : playOrder) removeMannequin(gp);
        removeAllArenaEntities();

        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) restorePlayerNormalState(p, gp);
        }

        secretWord = null;
        roundNumber = 0;
        currentPlayer = null;
        descriptionQueue.clear();

        state = GameState.ROUND_END;
        currentPhase = Phase.NONE;
        broadcastAll(ChatColor.YELLOW + "Game direset! Gunakan /nextround untuk melanjutkan.");
        mod.sendMessage(ChatColor.GREEN + "Game berhasil direset.");
        return true;
    }

    public boolean skipCurrentPlayer(Player mod) {
        if (state != GameState.PLAYING || currentPlayer == null) {
            mod.sendMessage(ChatColor.RED + "Tidak ada player yang sedang menjawab!");
            return false;
        }
        broadcastAll(ChatColor.YELLOW + "[MOD] Giliran " + currentPlayer.getName() + " di-skip!");
        stopTimer();
        nextTurn();
        return true;
    }

    public void forceEndGame(String reason) {
        broadcastAll(ChatColor.RED.toString() + ChatColor.BOLD + "Game dihentikan: " + reason);
        cleanup();
    }

    // ─── Camera ───────────────────────────────────────────────────────────────

    private void snapCameraTo(GamePlayer target) {
        if (target == null || target.getViewerLocation() == null) return;
        currentViewerLocation = target.getViewerLocation().clone();
        currentViewerYaw = target.getViewerYaw();
        currentViewerPitch = target.getViewerPitch();
        teleportAllToViewer();
        startCameraLock();
    }

    private void animateCameraTo(GamePlayer target) {
        stopCameraAnim();
        stopCameraLock();

        Location fromLoc = currentViewerLocation != null
                ? currentViewerLocation.clone()
                : target.getViewerLocation();
        float fromYaw = currentViewerLocation != null ? currentViewerYaw : target.getViewerYaw();
        float fromPitch = currentViewerPitch;

        Location toLoc = target.getViewerLocation();
        float toYaw = target.getViewerYaw();
        float toPitch = target.getViewerPitch();

        if (toLoc == null) {
            currentViewerLocation = null;
            return;
        }

        final int steps = 15;
        cameraAnimTask = new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                double t = step / (double) steps;
                Location blended = fromLoc.clone().add(toLoc.clone().subtract(fromLoc).multiply(t));
                float yaw = lerpAngleCounterClockwise(fromYaw, toYaw, (float) t);
                float pitch = fromPitch + (toPitch - fromPitch) * (float) t;

                currentViewerLocation = blended;
                currentViewerYaw = yaw;
                currentViewerPitch = pitch;
                teleportAllToViewer();

                if (++step > steps) {
                    cancel();
                    startCameraLock();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void startCameraLock() {
        stopCameraLock();
        cameraLockTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    cameraLockTask = null;
                    return;
                }
                teleportAllToViewer();
                lockHeldSlotForViewers();
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private void stopCameraLock() {
        if (cameraLockTask != null && !cameraLockTask.isCancelled()) {
            cameraLockTask.cancel();
        }
        cameraLockTask = null;
    }

    private void lockHeldSlotForViewers() {
        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p == null) continue;
            if (p.getInventory().getHeldItemSlot() != 0) {
                p.getInventory().setHeldItemSlot(0);
            }
        }
    }

    private float lerpAngleCounterClockwise(float from, float to, float t) {
        float normalizedFrom = normalizeYaw(from);
        float normalizedTo = normalizeYaw(to);
        float counterClockwise = ((normalizedFrom - normalizedTo) % 360f + 360f) % 360f;
        return normalizedFrom - counterClockwise * t;
    }

    private float normalizeYaw(float yaw) {
        float n = yaw % 360f;
        if (n < 0) n += 360f;
        return n;
    }

    private void teleportAllToViewer() {
        if (currentViewerLocation == null) return;
        Location viewLoc = currentViewerLocation.clone();
        viewLoc.setYaw(currentViewerYaw);
        viewLoc.setPitch(currentViewerPitch);

        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p == null) continue;
            p.teleport(viewLoc);
        }
    }

    private void stopCameraAnim() {
        if (cameraAnimTask != null && !cameraAnimTask.isCancelled()) {
            cameraAnimTask.cancel();
            cameraAnimTask = null;
        }
    }

    public boolean shouldLockMovement(Player player) {
        if (state != GameState.PLAYING) return false;
        GamePlayer gp = getGamePlayer(player.getUniqueId());
        return gp != null;
    }

    // ─── Timer ────────────────────────────────────────────────────────────────

    private void startTimer(int seconds) {
        stopTimer();
        timeLeft = seconds;

        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.PLAYING) {
                    cancel();
                    return;
                }
                
                updateTimerActionBar();
                
                if (timeLeft <= 5 && timeLeft > 0) {
                    for (GamePlayer gp : registeredPlayers) {
                        if (gp.getPlayer() != null) {
                            SoundUtil.playSound(gp.getPlayer(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                        }
                    }
                }
                
                if (timeLeft <= 0) {
                    cancel();
                    handleTimerEnd();
                    return;
                }
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void stopTimer() {
        if (timerTask != null && !timerTask.isCancelled()) {
            timerTask.cancel();
            timerTask = null;
        }
        clearTimerActionBar();
    }

    private void handleTimerEnd() {
        switch (currentPhase) {
            case DESCRIPTION -> {
                broadcastAll(ChatColor.RED + "Waktu habis! Lanjut ke giliran berikutnya.");
                nextTurn();
            }
            case DISCUSSION -> startVoteCheck();
            case VOTE_CHECK -> processVoteCheck();
            case VOTING -> {
                broadcastAll(ChatColor.RED + "Waktu voting habis!");
                Bukkit.getScheduler().runTaskLater(plugin, this::revealResult, 20L);
            }
            case IMPOSTOR_GUESS -> {
                broadcastAll(ChatColor.RED + "Waktu habis! Impostor tidak menebak.");
                broadcastAll(ChatColor.GREEN.toString() + ChatColor.BOLD + "INVESTIGATOR MENANG!");
                broadcastAll(ChatColor.GRAY + "Kata rahasia: " + ChatColor.YELLOW + ChatColor.BOLD + secretWord);
                endGameInvestigatorWin("Impostor timeout");
            }
        }
    }

    private void updateTimerActionBar() {
        NamedTextColor color = timeLeft <= 5 ? NamedTextColor.RED : NamedTextColor.WHITE;
        Component bar = Component.text("Waktu: " + timeLeft + "s").color(color);
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getPlayer() != null) {
                gp.getPlayer().sendActionBar(bar);
            }
        }
    }

    private void clearTimerActionBar() {
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getPlayer() != null) {
                gp.getPlayer().sendActionBar(Component.empty());
            }
        }
    }

    private void stopAllTimers() {
        stopTimer();
        if (cameraAnimTask != null && !cameraAnimTask.isCancelled()) {
            cameraAnimTask.cancel();
            cameraAnimTask = null;
        }
        stopCameraLock();
        cancelGachaTasks();
        clearTimerActionBar();
    }

    // ─── Text Display ─────────────────────────────────────────────────────────

    private void clearAllTextDisplays() {
        for (GamePlayer gp : playOrder) {
            if (gp.getTextDisplay() != null && !gp.getTextDisplay().isDead()) {
                gp.getTextDisplay().remove();
            }
            gp.setTextDisplay(null);
        }
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getTextDisplay() != null && !gp.getTextDisplay().isDead()) {
                gp.getTextDisplay().remove();
            }
            gp.setTextDisplay(null);
        }
    }

    // ─── Mannequin ────────────────────────────────────────────────────────────

    private void removeMannequin(GamePlayer gp) {
        if (gp.getTextDisplay() != null && !gp.getTextDisplay().isDead()) {
            gp.getTextDisplay().remove();
        }
        if (gp.getSeatEntity() != null && !gp.getSeatEntity().isDead()) {
            gp.getSeatEntity().eject();
        }
        if (gp.getMannequin() != null && !gp.getMannequin().isDead()) {
            gp.getMannequin().remove();
        }
        if (gp.getSeatEntity() != null && !gp.getSeatEntity().isDead()) {
            gp.getSeatEntity().remove();
        }
        gp.setTextDisplay(null);
        gp.setMannequin(null);
        gp.setSeatEntity(null);
    }

    private void removeAllArenaEntities() {
        Location center = buildManager.getCenterLocation();
        if (center == null || center.getWorld() == null) return;

        NamespacedKey key = new NamespacedKey(plugin, "whoislying_mannequin");
        World world = center.getWorld();
        List<Entity> toRemove = new ArrayList<>();

        for (Entity entity : world.getNearbyEntities(center, 14, 14, 14)) {
            boolean pluginEntity = entity.getPersistentDataContainer().has(key, PersistentDataType.STRING);
            boolean seatTag = entity.getScoreboardTags().stream()
                    .anyMatch(tag -> tag.startsWith("whoislying_seat_"));
            if (pluginEntity || seatTag) {
                toRemove.add(entity);
            }
        }

        for (Entity entity : toRemove) {
            for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
                passenger.remove();
            }
            entity.remove();
        }

        for (GamePlayer gp : registeredPlayers) {
            gp.setTextDisplay(null);
            gp.setMannequin(null);
            gp.setSeatEntity(null);
        }
    }

    // ─── Scores ───────────────────────────────────────────────────────────────

    private void displayScores() {
        List<GamePlayer> sorted = new ArrayList<>(registeredPlayers);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        broadcastAll(ChatColor.GOLD + "══ LEADERBOARD ══");
        for (int i = 0; i < sorted.size(); i++) {
            GamePlayer gp = sorted.get(i);
            String medal = switch (i) {
                case 0 -> ChatColor.GOLD + "#1";
                case 1 -> ChatColor.GRAY + "#2";
                case 2 -> ChatColor.YELLOW + "#3";
                default -> ChatColor.WHITE + "#" + (i + 1);
            };
            broadcastAll("  " + medal + " " + ChatColor.WHITE + gp.getName()
                    + ChatColor.GRAY + " → " + ChatColor.AQUA + gp.getScore() + " poin");
        }
    }

    public void listScore(Player mod) {
        List<GamePlayer> sorted = new ArrayList<>(registeredPlayers);
        sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        mod.sendMessage(ChatColor.GOLD + "══ LEADERBOARD ══");
        if (sorted.isEmpty()) {
            mod.sendMessage(ChatColor.GRAY + "  (tidak ada player)");
        } else {
            for (int i = 0; i < sorted.size(); i++) {
                GamePlayer gp = sorted.get(i);
                String medal = switch (i) {
                    case 0 -> ChatColor.GOLD + "#1";
                    case 1 -> ChatColor.GRAY + "#2";
                    case 2 -> ChatColor.YELLOW + "#3";
                    default -> ChatColor.WHITE + "#" + (i + 1);
                };
                mod.sendMessage("  " + medal + " " + ChatColor.WHITE + gp.getName()
                        + ChatColor.GRAY + " → " + ChatColor.AQUA + gp.getScore() + " poin");
            }
        }
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private void broadcastAll(String message) {
        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) p.sendMessage(message);
        }
    }

    private List<Player> getOnlinePlayers() {
        List<Player> list = new ArrayList<>();
        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) list.add(p);
        }
        return list;
    }

    public GamePlayer getGamePlayer(UUID uuid) {
        for (GamePlayer gp : registeredPlayers) {
            if (gp.getUuid().equals(uuid)) return gp;
        }
        return null;
    }

    public boolean isCurrentPlayer(Player player) {
        return currentPlayer != null && player.getUniqueId().equals(currentPlayer.getUuid());
    }

    public void cleanup() {
        stopAllTimers();
        clearAllTextDisplays();
        for (GamePlayer gp : registeredPlayers) removeMannequin(gp);
        removeAllArenaEntities();
        buildManager.restoreBlocks();
        
        for (GamePlayer gp : registeredPlayers) {
            Player p = gp.getPlayer();
            if (p != null) {
                restorePlayerNormalState(p, gp);
            }
        }
    }

    public boolean isProtectedEntity(Entity entity) {
        if (entity instanceof ArmorStand as && as.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "whoislying_mannequin"), PersistentDataType.STRING)) {
            return true;
        }
        return entity.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "whoislying_mannequin"), PersistentDataType.STRING);
    }

    public void handlePlayerQuit(Player player) {
        removePlayer(player.getUniqueId());
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public GameState getState() {
        return state;
    }

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public List<GamePlayer> getRegisteredPlayers() {
        return registeredPlayers;
    }

    public GamePlayer getCurrentPlayer() {
        return currentPlayer;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public String getSecretWord() {
        return secretWord;
    }

    public String getCategory() {
        return category;
    }

    public GamePlayer getImpostor() {
        return impostor;
    }
}
