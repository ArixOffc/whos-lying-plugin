package com.arixoffc.whoislying.manager;

import com.arixoffc.whoislying.WhoIsLying;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class BuildManager {

    private static final double MANNEQUIN_Y_OFFSET = -1.5;

    private static final int[][] ALL_CHAIR_POSITIONS = {
        {0, 3}, {3, 0}, {0, -3}, {-3, 0},
        {1, -3}, {-1, -3}, {3, 1}, {3, -1}
    };

    private final WhoIsLying plugin;
    private final List<BlockSnapshot> savedBlocks = new ArrayList<>();
    private final Map<Integer, Location> chairLocations = new HashMap<>();
    private final Set<String> structureBlockKeys = new HashSet<>();

    private Location centerLocation;
    private float modYaw;
    private int builtChairCount;
    private int floorY;

    public BuildManager(WhoIsLying plugin) {
        this.plugin = plugin;
    }

    public void buildArena(Location modLocation, float yaw, int playerCount) {
        restoreBlocks();
        chairLocations.clear();
        structureBlockKeys.clear();

        this.centerLocation = modLocation.getBlock().getLocation().add(0.5, 0, 0.5);
        this.floorY = modLocation.getBlockY();
        this.modYaw = yaw;
        this.builtChairCount = playerCount;

        generateArena(centerLocation, playerCount);
    }

    public void rebuildArenaAtSavedCenter(int playerCount) {
        if (centerLocation == null) return;

        Location savedCenter = centerLocation.clone();
        float savedYaw = modYaw;
        int savedFloorY = floorY;

        restoreBlocks();

        centerLocation = savedCenter;
        modYaw = savedYaw;
        floorY = savedFloorY;
        chairLocations.clear();
        structureBlockKeys.clear();

        generateArena(savedCenter, playerCount);
    }

    public void generateArena(Location center, int playerCount) {
        World world = center.getWorld();
        if (world == null) return;

        this.centerLocation = center.clone();
        this.floorY = center.getBlockY();

        int groundY = floorY - 1;
        clearExtendedArea(world, 4, groundY);
        removeFallingBlocks(world, center, 10);

        // Build floor
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                setBlock(world, blockX(x), groundY, blockZ(z), Material.GRASS_BLOCK);
            }
        }

        // Center table
        setBlock(world, blockX(0), floorY, blockZ(0), Material.SPRUCE_PLANKS);

        // Stairs (facing outward)
        setStair(world, blockX(1), floorY, blockZ(0), BlockFace.WEST);
        setStair(world, blockX(1), floorY, blockZ(1), BlockFace.WEST);
        setStair(world, blockX(1), floorY, blockZ(-1), BlockFace.WEST);
        setStair(world, blockX(-1), floorY, blockZ(0), BlockFace.EAST);
        setStair(world, blockX(-1), floorY, blockZ(1), BlockFace.EAST);
        setStair(world, blockX(-1), floorY, blockZ(-1), BlockFace.EAST);
        setStair(world, blockX(0), floorY, blockZ(1), BlockFace.NORTH);
        setStair(world, blockX(0), floorY, blockZ(-1), BlockFace.SOUTH);

        // Clear old chairs
        clearOldChairSlabs(world);

        // Build chairs
        int[][] chairOffsets = getChairOffsets(playerCount);
        chairLocations.clear();
        for (int i = 0; i < chairOffsets.length; i++) {
            Location chairLoc = relativeBlockLocation(chairOffsets[i][0], chairOffsets[i][1]);
            buildChairSlab(world, chairLoc);
            chairLocations.put(i + 1, chairLoc.clone());
        }
        builtChairCount = playerCount;

        removeFallingBlocks(world, center, 12);
        new BukkitRunnable() {
            @Override
            public void run() {
                removeFallingBlocks(world, center, 14);
            }
        }.runTaskLater(plugin, 3L);
    }

    public boolean hasArena() {
        return centerLocation != null && builtChairCount > 0;
    }

    private void removeFallingBlocks(World world, Location center, double radius) {
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof FallingBlock) {
                entity.remove();
            }
        }
    }

    private void clearOldChairSlabs(World world) {
        for (int[] pos : ALL_CHAIR_POSITIONS) {
            int x = blockX(pos[0]);
            int z = blockZ(pos[1]);
            Block block = world.getBlockAt(x, floorY, z);
            if (block.getType() == Material.SPRUCE_SLAB) {
                setBlock(world, x, floorY, z, Material.AIR);
            }
        }
    }

    private void clearExtendedArea(World world, int plotRadius, int groundY) {
        int cx = centerLocation.getBlockX();
        int cz = centerLocation.getBlockZ();
        int clearRadius = plotRadius + 5;
        int maxY = world.getMaxHeight() - 1;

        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                int bx = cx + x;
                int bz = cz + z;
                int columnTop = Math.min(
                    Math.max(world.getHighestBlockYAt(bx, bz), groundY + 2),
                    groundY + 12
                );
                boolean insidePlot = Math.abs(x) <= plotRadius && Math.abs(z) <= plotRadius;
                for (int y = groundY; y <= Math.min(columnTop, maxY); y++) {
                    Block block = world.getBlockAt(bx, y, bz);
                    Material type = block.getType();
                    if (type.isAir()) continue;
                    if (insidePlot || shouldClearBeforeBuild(type)) {
                        setBlock(world, bx, y, bz, Material.AIR);
                    }
                }
            }
        }
    }

    private boolean shouldClearBeforeBuild(Material type) {
        if (type.hasGravity()) return true;
        return switch (type) {
            case SHORT_GRASS, TALL_GRASS, FERN, LARGE_FERN, DEAD_BUSH,
                 DANDELION, POPPY, BLUE_ORCHID, ALLIUM, AZURE_BLUET,
                 SUGAR_CANE, BAMBOO, VINE, LILY_PAD, SNOW,
                 MUSHROOM_STEM, BROWN_MUSHROOM, RED_MUSHROOM,
                 TORCH, WALL_TORCH, LADDER -> true;
            default -> false;
        };
    }

    private void buildChairSlab(World world, Location slabBlock) {
        int x = slabBlock.getBlockX();
        int y = slabBlock.getBlockY();
        int z = slabBlock.getBlockZ();

        Block block = setBlock(world, x, y, z, Material.SPRUCE_SLAB);
        if (block.getBlockData() instanceof Slab slab) {
            slab.setType(Slab.Type.BOTTOM);
            block.setBlockData(slab);
        }
    }

    private void setStair(World world, int x, int y, int z, BlockFace outward) {
        Block block = setBlock(world, x, y, z, Material.SPRUCE_STAIRS);
        if (block.getBlockData() instanceof Stairs stairs) {
            stairs.setFacing(outward);
            stairs.setHalf(org.bukkit.block.data.Bisected.Half.TOP);
            block.setBlockData(stairs);
        }
    }

    public double[] getViewerPosition(Location chairBlock, Location center) {
        Location view = center.clone();
        view.setY(center.getY() + 1.15);

        Vector chairVec = chairBlock.toVector().add(new Vector(0.5, 0, 0.5));
        Vector look = chairVec.clone().subtract(view.toVector());
        look.setY(0);
        if (look.lengthSquared() < 0.01) look = getForwardVector();
        look.normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(-look.getX(), look.getZ()));
        float pitch = 18f;
        return new double[]{view.getX(), view.getY(), view.getZ(), yaw, pitch};
    }

    public Location getMannequinSeatLocation(Location chairBlock) {
        return chairBlock.clone().add(0.5, 1.0 + MANNEQUIN_Y_OFFSET, 0.5);
    }

    private Location relativeBlockLocation(int relX, int relZ) {
        Vector offset = getRightVector().multiply(relX).add(getForwardVector().multiply(relZ));
        Location loc = centerLocation.clone().add(offset);
        loc.setY(floorY);
        loc.setX(Math.floor(loc.getX()));
        loc.setZ(Math.floor(loc.getZ()));
        return loc;
    }

    private int blockX(int relX) { return centerLocation.getBlockX() + relX; }
    private int blockZ(int relZ) { return centerLocation.getBlockZ() + relZ; }

    private Vector getForwardVector() {
        double yawRad = Math.toRadians(modYaw);
        return new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
    }

    private Vector getRightVector() {
        double yawRad = Math.toRadians(modYaw);
        return new Vector(Math.cos(yawRad), 0, Math.sin(yawRad));
    }

    private int[][] getChairOffsets(int count) {
        return switch (count) {
            case 3 -> new int[][]{{0, 3}, {3, 0}, {0, -3}};
            case 4 -> new int[][]{{0, 3}, {3, 0}, {0, -3}, {-3, 0}};
            case 5 -> new int[][]{{0, 3}, {3, 0}, {1, -3}, {-1, -3}, {-3, 0}};
            case 6 -> new int[][]{{0, 3}, {3, 1}, {3, -1}, {1, -3}, {-1, -3}, {-3, 0}};
            case 7 -> new int[][]{{0, 3}, {3, 1}, {3, -1}, {1, -3}, {-1, -3}, {-3, -1}, {-3, 1}};
            case 8 -> new int[][]{{1, 3}, {3, 1}, {3, -1}, {1, -3}, {-1, -3}, {-3, -1}, {-3, 1}, {-1, 3}};
            default -> new int[0][];
        };
    }

    private Block setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        saveBlock(block);
        block.setType(material, false);
        markStructure(x, y, z);
        return block;
    }

    private void markStructure(int x, int y, int z) {
        structureBlockKeys.add(x + "," + y + "," + z);
    }

    private void saveBlock(Block block) {
        savedBlocks.add(new BlockSnapshot(block.getLocation(), block.getBlockData().clone()));
    }

    public void restoreBlocks() {
        for (int i = savedBlocks.size() - 1; i >= 0; i--) {
            BlockSnapshot snap = savedBlocks.get(i);
            Block block = snap.location().getBlock();
            block.setBlockData(snap.data(), false);
        }
        savedBlocks.clear();
        chairLocations.clear();
        structureBlockKeys.clear();
        builtChairCount = 0;
    }

    public Map<Integer, Location> getChairLocations() { return chairLocations; }
    public Location getCenterLocation() { return centerLocation; }
    public float getModYaw() { return modYaw; }
    public int getBuiltChairCount() { return builtChairCount; }

    private record BlockSnapshot(Location location, org.bukkit.block.data.BlockData data) {}
}
