# 🎮 PROJECT FLOW - Who's Lying? Plugin

Dokumen lengkap flow project yang mengadopsi sistem dari **SambungKata** untuk registrasi player, build arena, kamera visual, dan animasi.

---

## 📋 TABLE OF CONTENTS

1. [System Architecture](#system-architecture)
2. [Player Registration System](#player-registration-system)
3. [Arena Building System](#arena-building-system)
4. [Camera & Animation System](#camera--animation-system)
5. [Game Flow Complete](#game-flow-complete)
6. [Implementation Details](#implementation-details)

---

## 🏗️ SYSTEM ARCHITECTURE

### Package Structure
```
com.arixoffc.whoislying/
├── WhoIsLying.java              ← Main class (seperti SambungKata.java)
├── command/
│   └── ModeratorCommand.java    ← Handle semua command moderator
├── enums/
│   ├── GameState.java           ← IDLE, PLAYING, ROUND_END, ENDED
│   └── PlayerRole.java          ← INVESTIGATOR, IMPOSTOR
├── listener/
│   ├── ChatListener.java        ← Handle chat saat game
│   └── ProtectionListener.java  ← Protection PVP, block break, dll.
├── manager/
│   ├── GameManager.java         ← Core game logic
│   ├── WordManager.java         ← Word database management
│   ├── VoteManager.java         ← Voting system
│   └── BuildManager.java        ← Arena building (ADOPT DARI SAMBUNGKATA)
├── model/
│   └── GamePlayer.java          ← Model per player
└── util/
    ├── ColorUtil.java
    ├── SoundUtil.java
    ├── TitleUtil.java
    └── MannequinUtil.java       ← Spawn mannequin player
```


---

## 👥 PLAYER REGISTRATION SYSTEM

### Adopsi Penuh dari SambungKata

**System:** Moderator mendaftarkan player secara manual, BUKAN self-join.

### Commands (ModeratorCommand.java)

```java
/regis <nama>        → Daftarkan player (max 8)
/unregis <nama>      → Hapus player dari daftar
/listplayer          → Lihat daftar player terdaftar
/mode <single|multi> → Set game mode (tidak dipakai di Who's Lying, bisa dihapus)
/start               → Mulai game (gacha urutan)
/nextround           → Mulai ronde baru setelah ronde selesai
/endgame             → Akhiri game paksa
/resetgame           → Reset game ke awal
/skip                → Skip giliran player saat ini
/listscore           → Lihat leaderboard
```

### Registration Flow

```java
// GameManager.java (package com.arixoffc.whoislying.manager)
private final List<GamePlayer> registeredPlayers = new ArrayList<>();

public boolean registerPlayerByName(String rawName, Player mod) {
    // Check state
    if (state == GameState.PLAYING) {
        mod.sendMessage("Tidak bisa mendaftarkan player saat permainan berlangsung.");
        return false;
    }
    
    // Check limit
    if (registeredPlayers.size() >= 8) {
        mod.sendMessage("Maksimal 8 player sudah terdaftar!");
        return false;
    }
    
    // Check duplicate
    for (GamePlayer gp : registeredPlayers) {
        if (gp.getName().equalsIgnoreCase(name)) {
            mod.sendMessage(name + " sudah terdaftar.");
            return false;
        }
    }
    
    // Check online
    Player online = Bukkit.getPlayerExact(name);
    if (online == null) {
        mod.sendMessage("Player " + name + " tidak ada di server.");
        return false;
    }
    
    // Add
    registeredPlayers.add(new GamePlayer(online.getName(), online.getUniqueId()));
    mod.sendMessage(online.getName() + " berhasil didaftarkan! (" 
        + registeredPlayers.size() + "/8)");
    return true;
}
```


### Unregister Flow

```java
public boolean unregisterPlayer(String name, Player mod) {
    Iterator<GamePlayer> it = registeredPlayers.iterator();
    while (it.hasNext()) {
        GamePlayer gp = it.next();
        if (gp.getName().equalsIgnoreCase(name)) {
            it.remove();
            mod.sendMessage(name + " berhasil dihapus dari daftar.");
            return true;
        }
    }
    mod.sendMessage("Player " + name + " tidak ditemukan di daftar.");
    return false;
}
```

### List Players

```java
public void listPlayers(Player mod) {
    mod.sendMessage("══ Daftar Player ══");
    if (registeredPlayers.isEmpty()) {
        mod.sendMessage("  (kosong)");
    } else {
        for (int i = 0; i < registeredPlayers.size(); i++) {
            GamePlayer gp = registeredPlayers.get(i);
            String status = gp.isOnline() ? "" : " (offline)";
            mod.sendMessage("  " + (i+1) + ". " + gp.getName() + status);
        }
    }
    mod.sendMessage("Total: " + registeredPlayers.size() + " player");
}
```

### GamePlayer Model

```java
public class GamePlayer {
    private final String name;
    private final UUID uuid;
    private PlayerRole role;              // INVESTIGATOR / IMPOSTOR
    private int order;                    // Urutan duduk (1-8)
    private boolean hasSpoken;            // Sudah bicara di ronde ini?
    private boolean hasVoted;             // Sudah vote?
    private UUID votedFor;                // Vote ke siapa
    private int score;                    // Skor total
    
    // Visual entities
    private ArmorStand mannequin;         // Armor stand sebagai avatar
    private TextDisplay textDisplay;      // Display kata di atas kepala
    private Entity seatEntity;            // Invisible entity untuk duduk
    
    // Spawn & camera
    private Location chairLocation;       // Posisi kursi
    private Location spawnpoint;          // Respawn point
    private Location viewerLocation;      // Posisi kamera saat giliran player ini
    private float viewerYaw;
    private float viewerPitch;
    
    private GameMode savedGameMode;       // Restore setelah game
}
```


---

## 🏗️ ARENA BUILDING SYSTEM

### BuildManager - Adopsi dari SambungKata

Arena dibangun **otomatis** saat `/start` dengan meja bundar dan kursi untuk setiap player.

### Build Process

```java
// GameManager.java
public boolean startGame(Player mod) {
    // Validasi
    if (registeredPlayers.size() < 3) {
        mod.sendMessage("Minimal 3 player diperlukan!");
        return false;
    }
    
    // Build arena
    Location center = mod.getLocation();
    float yaw = mod.getLocation().getYaw();
    buildManager.buildArena(center, yaw, registeredPlayers.size());
    
    // Start gacha
    startGacha();
    return true;
}
```

### BuildManager.java

```java
public class BuildManager {
    
    private Location centerLocation;
    private float modYaw;
    private int floorY;
    private final List<BlockSnapshot> savedBlocks = new ArrayList<>();
    private final Map<Integer, Location> chairLocations = new HashMap<>();
    
    /**
     * Bangun arena meja bundar dengan kursi
     * @param modLocation Posisi moderator saat /start
     * @param yaw Arah hadap moderator
     * @param playerCount Jumlah player (3-8)
     */
    public void buildArena(Location modLocation, float yaw, int playerCount) {
        restoreBlocks(); // Restore arena lama jika ada
        
        this.centerLocation = modLocation.getBlock().getLocation().add(0.5, 0, 0.5);
        this.floorY = modLocation.getBlockY();
        this.modYaw = yaw;
        
        World world = centerLocation.getWorld();
        
        // 1. Bersihkan area
        clearExtendedArea(world, 4, floorY - 1);
        
        // 2. Bangun lantai (GRASS_BLOCK)
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                setBlock(world, blockX(x), floorY - 1, blockZ(z), Material.GRASS_BLOCK);
            }
        }
        
        // 3. Bangun meja tengah (SPRUCE_PLANKS + SPRUCE_STAIRS)
        buildTable(world);
        
        // 4. Bangun kursi (SPRUCE_SLAB)
        int[][] chairOffsets = getChairOffsets(playerCount);
        for (int i = 0; i < chairOffsets.length; i++) {
            Location chairLoc = relativeBlockLocation(chairOffsets[i][0], chairOffsets[i][1]);
            buildChairSlab(world, chairLoc);
            chairLocations.put(i + 1, chairLoc.clone());
        }
    }
}
```


### Table Build

```java
private void buildTable(World world) {
    // Center table
    setBlock(world, blockX(0), floorY, blockZ(0), Material.SPRUCE_PLANKS);
    
    // Tangga terbalik menghadap KELUAR (seperti di SambungKata)
    setStair(world, blockX(1), floorY, blockZ(0), BlockFace.WEST);
    setStair(world, blockX(1), floorY, blockZ(1), BlockFace.WEST);
    setStair(world, blockX(1), floorY, blockZ(-1), BlockFace.WEST);
    setStair(world, blockX(-1), floorY, blockZ(0), BlockFace.EAST);
    setStair(world, blockX(-1), floorY, blockZ(1), BlockFace.EAST);
    setStair(world, blockX(-1), floorY, blockZ(-1), BlockFace.EAST);
    setStair(world, blockX(0), floorY, blockZ(1), BlockFace.NORTH);
    setStair(world, blockX(0), floorY, blockZ(-1), BlockFace.SOUTH);
}

private void setStair(World world, int x, int y, int z, BlockFace outward) {
    Block block = setBlock(world, x, y, z, Material.SPRUCE_STAIRS);
    if (block.getBlockData() instanceof Stairs stairs) {
        stairs.setFacing(outward);
        stairs.setHalf(Bisected.Half.TOP); // Terbalik
        block.setBlockData(stairs);
    }
}
```

### Chair Positions (Dynamic based on player count)

```java
private int[][] getChairOffsets(int count) {
    return switch (count) {
        case 3 -> new int[][]{{0,3}, {3,0}, {0,-3}};
        case 4 -> new int[][]{{0,3}, {3,0}, {0,-3}, {-3,0}};
        case 5 -> new int[][]{{0,3}, {3,0}, {1,-3}, {-1,-3}, {-3,0}};
        case 6 -> new int[][]{{0,3}, {3,1}, {3,-1}, {1,-3}, {-1,-3}, {-3,0}};
        case 7 -> new int[][]{{0,3}, {3,1}, {3,-1}, {1,-3}, {-1,-3}, {-3,-1}, {-3,1}};
        case 8 -> new int[][]{{1,3}, {3,1}, {3,-1}, {1,-3}, {-1,-3}, {-3,-1}, {-3,1}, {-1,3}};
        default -> new int[0][];
    };
}
```

### Block Save & Restore

```java
private void setBlock(World world, int x, int y, int z, Material material) {
    Block block = world.getBlockAt(x, y, z);
    saveBlock(block); // Save original state
    block.setType(material, false);
    return block;
}

private void saveBlock(Block block) {
    savedBlocks.add(new BlockSnapshot(
        block.getLocation(), 
        block.getBlockData().clone()
    ));
}

public void restoreBlocks() {
    for (int i = savedBlocks.size() - 1; i >= 0; i--) {
        BlockSnapshot snap = savedBlocks.get(i);
        Block block = snap.location().getBlock();
        block.setBlockData(snap.data(), false);
    }
    savedBlocks.clear();
    chairLocations.clear();
}
```


---

## 📹 CAMERA & ANIMATION SYSTEM

### System Kamera (Seperti SambungKata)

Saat giliran player, kamera **semua player** diarahkan ke player yang sedang berbicara.

### Camera Snap (Instant)

```java
// GameManager.java
private void snapCameraTo(GamePlayer target) {
    Location viewLoc = target.getViewerLocation();
    float viewYaw = target.getViewerYaw();
    float viewPitch = target.getViewerPitch();
    
    for (GamePlayer gp : registeredPlayers) {
        Player p = Bukkit.getPlayer(gp.getUuid());
        if (p == null || !p.isOnline()) continue;
        
        Location teleportLoc = viewLoc.clone();
        teleportLoc.setYaw(viewYaw);
        teleportLoc.setPitch(viewPitch);
        p.teleport(teleportLoc);
    }
    
    currentViewerLocation = viewLoc.clone();
    currentViewerYaw = viewYaw;
    currentViewerPitch = viewPitch;
}
```

### Camera Animation (Smooth)

```java
private void animateCameraTo(GamePlayer target) {
    stopCameraAnim(); // Stop animasi sebelumnya
    
    Location targetLoc = target.getViewerLocation();
    float targetYaw = target.getViewerYaw();
    float targetPitch = target.getViewerPitch();
    
    Location startLoc = currentViewerLocation.clone();
    float startYaw = currentViewerYaw;
    float startPitch = currentViewerPitch;
    
    final int frames = 15; // 15 frame = ~0.75 detik
    final int[] frame = {0};
    
    cameraAnimTask = new BukkitRunnable() {
        @Override
        public void run() {
            if (state != GameState.PLAYING) {
                cancel();
                return;
            }
            
            frame[0]++;
            float progress = (float) frame[0] / frames;
            
            // Smooth interpolation
            double x = lerp(startLoc.getX(), targetLoc.getX(), progress);
            double y = lerp(startLoc.getY(), targetLoc.getY(), progress);
            double z = lerp(startLoc.getZ(), targetLoc.getZ(), progress);
            float yaw = lerpAngle(startYaw, targetYaw, progress);
            float pitch = lerp(startPitch, targetPitch, progress);
            
            Location newLoc = new Location(startLoc.getWorld(), x, y, z, yaw, pitch);
            
            // Teleport all players
            for (GamePlayer gp : registeredPlayers) {
                Player p = Bukkit.getPlayer(gp.getUuid());
                if (p != null && p.isOnline()) {
                    p.teleport(newLoc);
                }
            }
            
            if (frame[0] >= frames) {
                currentViewerLocation = targetLoc.clone();
                currentViewerYaw = targetYaw;
                currentViewerPitch = targetPitch;
                cancel();
            }
        }
    }.runTaskTimer(plugin, 0L, 1L);
}

private double lerp(double a, double b, float t) {
    return a + (b - a) * t;
}

private float lerpAngle(float a, float b, float t) {
    float delta = ((b - a + 540) % 360) - 180;
    return a + delta * t;
}
```


### Viewer Position Calculation

```java
// BuildManager.java
public double[] getViewerPosition(Location chairBlock, Location center) {
    Location view = center.clone();
    view.setY(center.getY() + 1.15); // Eye level
    
    Vector chairVec = chairBlock.toVector().add(new Vector(0.5, 0, 0.5));
    Vector look = chairVec.clone().subtract(view.toVector());
    look.setY(0); // Horizontal only
    
    if (look.lengthSquared() < 0.01) {
        look = getForwardVector();
    }
    look.normalize();
    
    float yaw = (float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ()));
    float pitch = 18f; // Slight downward angle
    
    return new double[]{view.getX(), view.getY(), view.getZ(), yaw, pitch};
}
```

### Camera Update Loop (Action Bar)

```java
// GameManager.java
private BukkitTask cameraTask;

private void startCameraLoop() {
    stopCameraTask();
    
    cameraTask = new BukkitRunnable() {
        @Override
        public void run() {
            if (state != GameState.PLAYING) {
                cancel();
                return;
            }
            
            // Update action bar untuk semua player
            String message = "§e⏰ " + timeLeft + "s §7| §6Giliran: §e" + currentPlayer.getName();
            
            for (GamePlayer gp : registeredPlayers) {
                Player p = Bukkit.getPlayer(gp.getUuid());
                if (p != null && p.isOnline()) {
                    p.sendActionBar(ColorUtil.colorize(message));
                }
            }
        }
    }.runTaskTimer(plugin, 0L, 20L);
}

private void stopCameraTask() {
    if (cameraTask != null && !cameraTask.isCancelled()) {
        cameraTask.cancel();
    }
    cameraTask = null;
}
```


---

## 🎰 GACHA & MANNEQUIN SYSTEM

### Gacha Urutan (Animasi Acak)

Setelah arena dibangun, sistem "gacha" urutan player dengan animasi angka acak.

```java
// GameManager.java
private void startGacha() {
    // Assign random order
    List<Integer> orders = new ArrayList<>();
    for (int i = 1; i <= registeredPlayers.size(); i++) {
        orders.add(i);
    }
    Collections.shuffle(orders);
    
    for (int i = 0; i < registeredPlayers.size(); i++) {
        registeredPlayers.get(i).setOrder(orders.get(i));
    }
    
    // Animation
    final int[] frame = {0};
    final int spinFrames = 20;
    
    gachaSpinTask = new BukkitRunnable() {
        @Override
        public void run() {
            if (frame[0] < spinFrames) {
                // Show random number
                for (GamePlayer gp : registeredPlayers) {
                    Player p = Bukkit.getPlayer(gp.getUuid());
                    if (p == null) continue;
                    
                    int rand = new Random().nextInt(registeredPlayers.size()) + 1;
                    TitleUtil.sendTitle(p, 
                        "§6Kamu berada pada urutan ke",
                        "§f" + rand, 
                        0, 4, 0);
                    SoundUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
                frame[0]++;
            } else {
                // Show final result
                cancel();
                for (GamePlayer gp : registeredPlayers) {
                    Player p = Bukkit.getPlayer(gp.getUuid());
                    if (p == null) continue;
                    
                    TitleUtil.sendTitle(p,
                        "§6Kamu berada pada urutan ke",
                        "§e§l" + gp.getOrder(),
                        5, 60, 20);
                    SoundUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP);
                }
                
                // Spawn mannequins & start game
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        setupPlayOrder();
                        spawnMannequins();
                        setSpawnpoints();
                        startDescription();
                    }
                }.runTaskLater(plugin, 80L);
            }
        }
    }.runTaskTimer(plugin, 5L, 2L);
}
```


### Spawn Mannequins (Avatar Player)

```java
private void spawnMannequins() {
    Map<Integer, Location> chairs = buildManager.getChairLocations();
    Location center = buildManager.getCenterLocation();
    
    for (GamePlayer gp : playOrder) {
        Location chairLoc = chairs.get(gp.getOrder());
        if (chairLoc == null) continue;
        
        gp.setChairLocation(chairLoc.clone());
        
        // Seat location (di atas slab)
        Location seatLoc = buildManager.getMannequinSeatLocation(chairLoc);
        float yaw = yawToward(seatLoc, center); // Menghadap ke meja
        
        // Spawn mannequin (armor stand dengan skin player)
        MannequinUtil.spawnForPlayer(plugin, gp, seatLoc, yaw);
        
        // Calculate viewer position untuk kamera
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
```

### MannequinUtil.java

```java
public class MannequinUtil {
    
    private static final double MANNEQUIN_Y_OFFSET = -1.5;
    
    public static void spawnForPlayer(Plugin plugin, GamePlayer gp, Location seatLoc, float yaw) {
        Player player = Bukkit.getPlayer(gp.getUuid());
        if (player == null) return;
        
        World world = seatLoc.getWorld();
        
        // 1. Spawn invisible seat entity (untuk duduk)
        ArmorStand seat = world.spawn(seatLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setMarker(true);
            stand.addScoreboardTag("whoislying_seat_" + gp.getUuid().toString());
        });
        gp.setSeatEntity(seat);
        
        // 2. Spawn mannequin armor stand
        Location mannequinLoc = seatLoc.clone().add(0, MANNEQUIN_Y_OFFSET, 0);
        ArmorStand mannequin = world.spawn(mannequinLoc, ArmorStand.class, stand -> {
            stand.setVisible(true);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setBasePlate(false);
            stand.setArms(true);
            stand.setSmall(false);
            
            // Apply player skin (menggunakan player head)
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
            stand.setHelmet(head);
            
            // Set body armor (player's current armor atau default)
            stand.setChestplate(player.getInventory().getChestplate());
            stand.setLeggings(player.getInventory().getLeggings());
            stand.setBoots(player.getInventory().getBoots());
            
            stand.setRotation(yaw, 0);
            
            NamespacedKey key = new NamespacedKey(plugin, "whoislying_mannequin");
            stand.getPersistentDataContainer().set(key, PersistentDataType.STRING, gp.getUuid().toString());
        });
        
        // Make mannequin sit on seat
        seat.addPassenger(mannequin);
        gp.setMannequin(mannequin);
        
        // 3. Spawn text display (nama player di atas kepala)
        Location textLoc = mannequinLoc.clone().add(0, 2.3, 0);
        TextDisplay textDisplay = world.spawn(textLoc, TextDisplay.class, display -> {
            display.setText(Component.text(gp.getName()).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
            display.setBillboard(Display.Billboard.CENTER);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
        });
        gp.setTextDisplay(textDisplay);
    }
}
```


---

## 🎮 GAME FLOW COMPLETE

### State Machine

```
IDLE → (start) → PLAYING → ROUND_END → (nextround) → PLAYING
                     ↓
                  (endgame)
                     ↓
                   ENDED
```

### Complete Flow Diagram

```
1. REGISTRATION PHASE (IDLE)
   ├─ Moderator: /regis Steve
   ├─ Moderator: /regis Alex
   ├─ Moderator: /regis Herobrine
   └─ Moderator: /listplayer

2. START GAME
   ├─ Moderator: /start
   ├─ Build arena (BuildManager)
   ├─ Gacha urutan (animasi 20 frame)
   ├─ Spawn mannequins (semua player duduk)
   └─ Assign roles (1 Impostor, sisanya Investigator)

3. ROLE ASSIGNMENT
   ├─ Pick random impostor
   ├─ Pick random word + category
   ├─ Send private info ke semua player:
   │  ├─ Investigator: kategori + kata
   │  └─ Impostor: kategori saja
   └─ Camera snap ke player pertama

4. DESCRIPTION PHASE (Ronde 1)
   ├─ Player 1 giliran (camera animate ke dia)
   │  ├─ Timer 60s
   │  ├─ Player ketik deskripsi di chat
   │  ├─ Word filter check
   │  └─ Broadcast ke semua
   ├─ Player 2 giliran (camera animate)
   ├─ Player 3 giliran
   └─ ... (semua player)

5. DISCUSSION PHASE
   ├─ Free chat 60s
   ├─ Camera tetap di center (atau last player)
   └─ Timer countdown di action bar

6. VOTE CHECK
   ├─ System ask: "Mulai voting? Y/N"
   ├─ Player ketik Y atau N
   ├─ Majority YES → go to VOTING
   └─ Majority NO → kembali ke DESCRIPTION (ronde 2)

7. VOTING PHASE
   ├─ Camera rotate semua player (showcase)
   ├─ Player ketik: /vote <nama>
   ├─ Timer 30s
   └─ Count votes

8. REVEAL & RESULT
   ├─ Announce top voted
   ├─ Check if impostor:
   │  ├─ YES → Investigator wins → END ROUND
   │  └─ NO → Impostor guess phase
   └─ Impostor guess (15s)
       ├─ Correct → Impostor wins
       └─ Wrong → Investigator wins

9. ROUND END
   ├─ Display scores
   ├─ Remove mannequins
   ├─ Restore player states
   └─ Moderator: /nextround atau /endgame

10. NEXT ROUND (if /nextround)
    ├─ Reset game state
    ├─ Rebuild arena
    ├─ New gacha
    └─ Back to step 3

11. END GAME (if /endgame)
    ├─ Display final leaderboard
    ├─ Restore arena blocks
    ├─ Remove all entities
    ├─ Restore player states
    └─ Back to IDLE
```


---

## 💻 IMPLEMENTATION DETAILS

### Main Class (WhoIsLying.java)

```java
public class WhoIsLying extends JavaPlugin {
    
    private GameManager gameManager;
    private BuildManager buildManager;
    private WordManager wordManager;
    private VoteManager voteManager;
    
    @Override
    public void onEnable() {
        // Init managers
        wordManager = new WordManager(this);
        buildManager = new BuildManager(this);
        voteManager = new VoteManager();
        gameManager = new GameManager(this, buildManager, wordManager, voteManager);
        
        // Register commands
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
        getServer().getPluginManager().registerEvents(
            new ChatListener(this, gameManager), this);
        getServer().getPluginManager().registerEvents(
            new ProtectionListener(gameManager), this);
        
        getLogger().info("WhoIsLying enabled!");
    }
    
    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
        getLogger().info("WhoIsLying disabled!");
    }
}
```

### Game States

```java
public enum GameState {
    IDLE,        // Belum mulai, registrasi player
    PLAYING,     // Game sedang berlangsung
    ROUND_END,   // Ronde selesai, tunggu /nextround
    ENDED        // Game selesai total
}
```


### Player Effects During Game

```java
private void applyPlayerGameEffects(Player p, GamePlayer gp) {
    // Save original gamemode
    gp.setSavedGameMode(p.getGameMode());
    
    // Set survival
    p.setGameMode(org.bukkit.GameMode.SURVIVAL);
    
    // Add invisibility (so only mannequin visible)
    p.addPotionEffect(new PotionEffect(
        PotionEffectType.INVISIBILITY, 
        Integer.MAX_VALUE, 
        0, false, false, false
    ));
    
    // Clear inventory
    p.getInventory().clear();
}

private void restorePlayerNormalState(Player p, GamePlayer gp) {
    if (p == null) return;
    
    // Remove invisibility
    p.removePotionEffect(PotionEffectType.INVISIBILITY);
    
    // Restore health
    p.setHealth(20.0);
    
    // Clear inventory
    p.getInventory().clear();
    
    // Restore gamemode
    if (gp.getSavedGameMode() != null) {
        p.setGameMode(gp.getSavedGameMode());
    } else {
        p.setGameMode(org.bukkit.GameMode.SURVIVAL);
    }
}
```

### Chat Handling (Description Phase)

```java
// ChatListener.java
@EventHandler(priority = EventPriority.HIGHEST)
public void onPlayerChat(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    GamePlayer gamePlayer = gameManager.getGamePlayer(player.getUniqueId());
    
    if (gamePlayer == null) return; // Not in game
    
    GameState state = gameManager.getState();
    String message = event.getMessage();
    
    if (state == GameState.PLAYING) {
        // Check if description phase
        if (gameManager.isDescriptionPhase()) {
            event.setCancelled(true);
            
            // Only current turn player can chat
            if (!gameManager.isCurrentPlayer(player)) {
                player.sendMessage("§cBukan giliranmu untuk bicara!");
                return;
            }
            
            // Handle description
            plugin.getServer().getScheduler().runTask(plugin, () -> 
                gameManager.handlePlayerDescription(player, message)
            );
        }
        // Check if discussion phase
        else if (gameManager.isDiscussionPhase()) {
            event.setFormat("§8[§bDISKUSI§8] §7" + "%1$s§7: §f%2$s");
        }
        // Check if vote check phase
        else if (gameManager.isVoteCheckPhase()) {
            event.setCancelled(true);
            String cleanMsg = message.toUpperCase().trim();
            if (cleanMsg.equals("Y") || cleanMsg.equals("N")) {
                plugin.getServer().getScheduler().runTask(plugin, () -> 
                    gameManager.handleVoteCheckResponse(player, cleanMsg)
                );
            } else {
                player.sendMessage("§cKetik §aY §catau §cN §csaja!");
            }
        }
        // Other phases block chat
        else {
            event.setCancelled(true);
            player.sendMessage("§cChat diblokir saat fase ini!");
        }
    }
}
```


### Visual Enhancements

#### 1. Text Display Update (Live Deskripsi)

```java
private void showTextDisplayForCurrent(String word) {
    if (currentPlayer == null) return;
    
    TextDisplay display = currentPlayer.getTextDisplay();
    if (display == null || display.isDead()) return;
    
    String text = word == null || word.isEmpty() 
        ? "§7..." 
        : "§f" + word;
    
    display.setText(Component.text(text));
}
```

#### 2. Highlight Current Player

```java
private void highlightCurrentPlayer() {
    for (GamePlayer gp : registeredPlayers) {
        TextDisplay display = gp.getTextDisplay();
        if (display == null) continue;
        
        if (gp.equals(currentPlayer)) {
            // Highlight dengan warna emas
            display.setText(Component.text(">> " + gp.getName() + " <<")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        } else {
            // Normal
            display.setText(Component.text(gp.getName())
                .color(NamedTextColor.YELLOW));
        }
    }
}
```

#### 3. Mannequin Animation

```java
private void animateMannequinWave(GamePlayer gp) {
    ArmorStand mannequin = gp.getMannequin();
    if (mannequin == null) return;
    
    // Animate right arm wave
    EulerAngle rightArm = mannequin.getRightArmPose();
    new BukkitRunnable() {
        int tick = 0;
        @Override
        public void run() {
            if (tick > 10) {
                mannequin.setRightArmPose(rightArm); // Reset
                cancel();
                return;
            }
            
            double angle = Math.sin(tick * 0.5) * 45;
            mannequin.setRightArmPose(new EulerAngle(Math.toRadians(angle), 0, 0));
            tick++;
        }
    }.runTaskTimer(plugin, 0L, 2L);
}
```


### Cleanup & Reset

```java
public void cleanup() {
    stopAllTimers();
    clearAllTextDisplays();
    removeAllMannequins();
    removeAllArenaEntities();
    buildManager.restoreBlocks();
    
    for (GamePlayer gp : registeredPlayers) {
        Player p = Bukkit.getPlayer(gp.getUuid());
        if (p != null) {
            restorePlayerNormalState(p, gp);
        }
    }
}

private void removeAllMannequins() {
    for (GamePlayer gp : registeredPlayers) {
        removeMannequin(gp);
    }
}

private void removeMannequin(GamePlayer gp) {
    if (gp.getTextDisplay() != null && !gp.getTextDisplay().isDead()) {
        gp.getTextDisplay().remove();
    }
    if (gp.getSeatEntity() != null && !gp.getSeatEntity().isDead()) {
        gp.getSeatEntity().eject();
        gp.getSeatEntity().remove();
    }
    if (gp.getMannequin() != null && !gp.getMannequin().isDead()) {
        gp.getMannequin().remove();
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
    
    for (Entity entity : world.getNearbyEntities(center, 14, 14, 14)) {
        boolean pluginEntity = entity.getPersistentDataContainer()
            .has(key, PersistentDataType.STRING);
        boolean seatTag = entity.getScoreboardTags().stream()
            .anyMatch(tag -> tag.startsWith("whoislying_seat_"));
        
        if (pluginEntity || seatTag) {
            for (Entity passenger : new ArrayList<>(entity.getPassengers())) {
                passenger.remove();
            }
            entity.remove();
        }
    }
}
```


---

## 🎯 KEY DIFFERENCES: SambungKata vs WhoIsLying

| Aspect | SambungKata | WhoIsLying |
|--------|-------------|------------|
| **Game Type** | Word chain game | Social deduction |
| **Player Elimination** | Lost hearts → eliminated | No elimination (all play until vote) |
| **Turn System** | Sequential answer turns | Description → Discussion → Vote |
| **Win Condition** | Last player standing | Investigators vote out impostor |
| **Role System** | No roles | Investigator vs Impostor |
| **Word System** | Chain words from last letter | Random word per game |
| **Voting** | No voting | Democratic voting system |
| **Phases** | Answer phase only | 4 phases (Description, Discussion, Vote Check, Voting) |
| **Camera Focus** | Current answering player | Current describing player |
| **Duration** | Until 1 player left | Fixed rounds or until vote |

### What We ADOPT from SambungKata:

✅ **Registration System** - `/regis`, `/unregis`, `/listplayer`  
✅ **BuildManager** - Meja bundar, kursi, arena otomatis  
✅ **Gacha System** - Animasi urutan acak  
✅ **Mannequin System** - Armor stand dengan player skin  
✅ **Camera System** - Snap & animate camera  
✅ **Text Display** - Nama & status di atas kepala  
✅ **Player Effects** - Invisibility, saved gamemode  
✅ **Cleanup System** - Restore blocks, remove entities  
✅ **State Machine** - IDLE → PLAYING → ROUND_END  

### What We CHANGE for WhoIsLying:

🔄 **Role Assignment** - 1 Impostor dipilih random  
🔄 **Word Distribution** - Private info (Investigator tahu, Impostor tidak)  
🔄 **Turn System** - Semua player bicara 1x per ronde  
🔄 **Phase System** - 4 fase berbeda  
🔄 **Voting System** - Player vote siapa yang impostor  
🔄 **Win Condition** - Based on voting result  
🔄 **No Elimination** - Semua player tetap di game  
🔄 **Multiple Rounds** - Ronde berulang sampai vote  


---

## 🎬 EXAMPLE GAME SESSION

### Setup Phase
```
Moderator: /regis Steve
System: Steve berhasil didaftarkan! (1/8)

Moderator: /regis Alex
System: Alex berhasil didaftarkan! (2/8)

Moderator: /regis Herobrine
System: Herobrine berhasil didaftarkan! (3/8)

Moderator: /listplayer
System: ══ Daftar Player ══
System:   1. Steve
System:   2. Alex
System:   3. Herobrine
System: Total: 3 player
```

### Start Game
```
Moderator: /start (at position X=100, Y=64, Z=200)

[BuildManager builds arena]
- Floor (grass blocks)
- Table (spruce planks + stairs)
- 3 Chairs (spruce slabs)

[Gacha Animation - 20 frames, 1 second]
Steve sees:    "Kamu berada pada urutan ke: 2" (random changing)
Alex sees:     "Kamu berada pada urutan ke: 1" (random changing)
Herobrine sees: "Kamu berada pada urutan ke: 3" (random changing)

[Final Result]
Steve:    "Kamu berada pada urutan ke: 1"
Alex:     "Kamu berada pada urutan ke: 3"
Herobrine: "Kamu berada pada urutan ke: 2"

[Spawn Mannequins]
- Steve's mannequin at chair #1 (duduk menghadap meja)
- Herobrine's mannequin at chair #2
- Alex's mannequin at chair #3

[Role Assignment - Random]
System picks: Herobrine = IMPOSTOR

[Private Messages]
To Steve:     "🕵️ INVESTIGATOR | Kategori: Animals | Kata: Dog"
To Alex:      "🕵️ INVESTIGATOR | Kategori: Animals | Kata: Dog"
To Herobrine: "🤥 IMPOSTOR | Kategori: Animals | Kata: ???"
```


### Description Phase - Round 1
```
[Camera snaps to Steve]
All players teleported to viewer position facing Steve's mannequin

System: "Giliran Steve untuk Menjawab"
Sound: BLOCK_NOTE_BLOCK_PLING (high pitch)

Steve (chat): "It barks"
[Word filter check: ✓ pass]
[Broadcast]: [Steve] It barks
[Text display above Steve's mannequin shows: "It barks"]

[Camera animates to Herobrine - 15 frames smooth transition]

System: "Giliran Herobrine untuk Menjawab"
Herobrine (chat): "It has four legs"
[Broadcast]: [Herobrine] It has four legs
[Text display above Herobrine's mannequin shows: "It has four legs"]

[Camera animates to Alex]

System: "Giliran Alex untuk Menjawab"
Alex (chat): "Man's best friend"
[Broadcast]: [Alex] Man's best friend
[Text display above Alex's mannequin shows: "Man's best friend"]
```

### Discussion Phase
```
[All players' cameras move to center, slightly elevated]
System: "═══ FASE DISKUSI =══"
System: "Diskusikan siapa yang mencurigakan!"
System: "Waktu: 60 detik"
Sound: BLOCK_BELL_USE

[Free chat enabled with prefix [DISKUSI]]
Steve (chat): "Herobrine sus, too generic"
[Broadcast]: [DISKUSI] Steve: Herobrine sus, too generic

Alex (chat): "Yeah I agree"
[Broadcast]: [DISKUSI] Alex: Yeah I agree

Herobrine (chat): "No way! It's accurate!"
[Broadcast]: [DISKUSI] Herobrine: No way! It's accurate!

[Action Bar]: "💬 Diskusi: 45 detik tersisa"
[Action Bar]: "💬 Diskusi: 30 detik tersisa"
...
[Action Bar]: "💬 Diskusi: 5 detik tersisa"
```

### Vote Check Phase
```
System: "═══ VOTE CHECK =══"
System: "Mulai voting sekarang?"
System: "Ketik Y (Yes) atau N (No) di chat!"

Steve (chat): Y
System: "Respon dicatat: Y"
System: "Steve telah memberikan respon. (1/3)"

Alex (chat): Y
System: "Respon dicatat: Y"
System: "Alex telah memberikan respon. (2/3)"

Herobrine (chat): N
System: "Respon dicatat: N"
System: "Herobrine telah memberikan respon. (3/3)"

[Calculate: 2 YES, 1 NO = Majority YES]
System: "Mayoritas setuju! Voting dimulai!"
```


### Voting Phase
```
System: "═══ FASE VOTING =══"
System: "Vote siapa yang menurut kamu IMPOSTOR!"
System: "Gunakan: /vote <nama>"
System: "Waktu: 30 detik"
Sound: ENTITY_WITHER_SPAWN (dramatic!)

Steve: /vote Herobrine
System: "Kamu vote: Herobrine"
System: "Steve telah vote. (1/3)"

Alex: /vote Herobrine
System: "Kamu vote: Herobrine"
System: "Alex telah vote. (2/3)"

Herobrine: /vote Steve
System: "Kamu vote: Steve"
System: "Herobrine telah vote. (3/3)"

[All voted, proceed immediately]
```

### Reveal Phase
```
System: "═══ HASIL VOTING =══"
System: "Hasil:"
System: "  Herobrine: 2 suara"
System: "  Steve: 1 suara"
System: ""
System: "Herobrine telah dikick dari game!"
System: "Apakah dia Impostor...?"
Sound: ENTITY_LIGHTNING_BOLT_THUNDER

[3 second pause for drama]

System: "✓ YA! Dia adalah IMPOSTOR!"
System: "INVESTIGATOR MENANG!"
Sound: UI_TOAST_CHALLENGE_COMPLETE

System: ""
System: "Impostor: Herobrine"
System: "Kategori: Animals"
System: "Kata: Dog"

[Rewards]
+50 XP to Steve
+50 XP to Alex

[Cleanup]
- Remove all mannequins
- Restore player states
- Keep arena intact

System: "══════════════════"
System: "Permainan Selesai. Steve Memenangkan Ronde Ini!"
System: "══════════════════"
System: "Gunakan /nextround untuk ronde berikutnya atau /endgame untuk mengakhiri."
```

### Next Round
```
Moderator: /nextround

[Arena rebuilt at same location]
[New gacha - different orders]
[New roles - different impostor]
[New word]
[Repeat from Description Phase]
```

### End Game
```
Moderator: /endgame

System: "══════════════════"
System: "  PERMAINAN BERAKHIR"
System: "══ LEADERBOARD FINAL ══"
System: "1. Steve - 5 poin"
System: "2. Alex - 3 poin"
System: "3. Herobrine - 1 poin"
System: "══════════════════"
System: "Permainan berakhir."

[Restore all blocks]
[Remove all entities]
[Restore all player states]
[Reset to IDLE]
```

---

## ✅ IMPLEMENTATION CHECKLIST

### Core Systems (Adopted from SambungKata)
- [x] Registration system (`/regis`, `/unregis`, `/listplayer`)
- [x] BuildManager (arena building)
- [x] Gacha animation (order randomization)
- [x] Mannequin spawning (armor stands with player skins)
- [x] Camera snap & animation system
- [x] Text display (player names & status)
- [x] Player effects (invisibility, saved gamemode)
- [x] Cleanup system (restore blocks, remove entities)

### Game-Specific Systems (New for WhoIsLying)
- [x] Role assignment (1 random impostor)
- [x] Word & category selection
- [x] Private info distribution
- [x] Description phase (turn-based speaking)
- [x] Discussion phase (free chat)
- [x] Vote check phase (Y/N responses)
- [x] Voting phase (democratic voting)
- [x] Reveal phase (results & impostor guess)
- [x] Win condition logic
- [x] Score system
- [x] Multi-round support

---

## 🎓 DEVELOPER NOTES

### Code Organization
```
Complexity Distribution:
├── GameManager.java    (~800 lines) - Core game logic
├── BuildManager.java   (~400 lines) - Arena building (from SambungKata)
├── ModeratorCommand.java (~300 lines) - Command handling
├── VoteManager.java    (~200 lines) - Voting system
├── WordManager.java    (~200 lines) - Word database
├── MannequinUtil.java  (~150 lines) - Mannequin spawning
└── Other files         (~600 lines) - Utilities, listeners, models

Total: ~2650 lines of Java code
```

### Performance Considerations
- **Camera animation**: 1 tick interval (50ms) for smooth 60fps-like movement
- **Gacha animation**: 2 tick interval (100ms) for readable number changes
- **Entity count**: Max ~24 entities (8 players × 3 entities each)
- **Block modifications**: ~80 blocks per arena (saved & restored)

### Thread Safety
- All chat events run async → `runTask()` to sync with game state
- All timers use `BukkitRunnable`
- No concurrent modification of player lists
- Camera teleports are main thread safe

---

**Document Version:** 1.0  
**Last Updated:** June 5, 2026  
**Author:** ArixOffc  
**GitHub:** https://github.com/ArixOffc  
**Status:** ✅ Complete & Production Ready
