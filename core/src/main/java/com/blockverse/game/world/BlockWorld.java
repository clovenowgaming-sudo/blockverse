package com.blockverse.game.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small voxel world model with deterministic text save files; deliberately independent of rendering. */
public final class BlockWorld {
    private static final String MAGIC = "BLOCKVERSE_WORLD";
    private static final int FORMAT_VERSION = 1;

    private final Map<BlockPos, BlockType> blocks = new LinkedHashMap<BlockPos, BlockType>();
    private long revision;

    public BlockType getBlock(int x, int y, int z) {
        return blocks.get(new BlockPos(x, y, z));
    }

    public boolean hasBlock(int x, int y, int z) {
        return getBlock(x, y, z) != null;
    }

    /** Returns true only when the world was actually changed. */
    public boolean setBlock(int x, int y, int z, BlockType type) {
        if (type == null) return removeBlock(x, y, z);
        BlockPos position = new BlockPos(x, y, z);
        BlockType old = blocks.put(position, type);
        if (old != type) {
            revision++;
            return true;
        }
        return false;
    }

    public boolean removeBlock(int x, int y, int z) {
        BlockType removed = blocks.remove(new BlockPos(x, y, z));
        if (removed != null) {
            revision++;
            return true;
        }
        return false;
    }

    public long getRevision() {
        return revision;
    }

    public int size() {
        return blocks.size();
    }

    public List<BlockCell> getBlocksSnapshot() {
        List<BlockCell> snapshot = new ArrayList<BlockCell>(blocks.size());
        for (Map.Entry<BlockPos, BlockType> entry : blocks.entrySet()) {
            snapshot.add(new BlockCell(entry.getKey(), entry.getValue()));
        }
        Collections.sort(snapshot, new Comparator<BlockCell>() {
            @Override public int compare(BlockCell a, BlockCell b) {
                if (a.position.y != b.position.y) return Integer.compare(a.position.y, b.position.y);
                if (a.position.z != b.position.z) return Integer.compare(a.position.z, b.position.z);
                return Integer.compare(a.position.x, b.position.x);
            }
        });
        return snapshot;
    }

    public boolean collides(float x, float feetY, float z, float radius, float height) {
        int minX = floor(x - radius);
        int maxX = floor(x + radius);
        int minY = floor(feetY + 0.02f);
        int maxY = floor(feetY + height - 0.02f);
        int minZ = floor(z - radius);
        int maxZ = floor(z + radius);
        for (int bx = minX; bx <= maxX; bx++) {
            for (int by = minY; by <= maxY; by++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (hasBlock(bx, by, bz)) return true;
                }
            }
        }
        return false;
    }

    public static int floor(float value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    public static BlockWorld createDefault() {
        BlockWorld world = new BlockWorld();
        for (int x = -12; x <= 12; x++) {
            for (int z = -12; z <= 12; z++) {
                world.setBlock(x, 0, z, BlockType.GRASS);
                world.setBlock(x, -1, z, BlockType.DIRT);
                world.setBlock(x, -2, z, BlockType.STONE);
            }
        }
        // A few handcrafted props give the player blocks to inspect and mine.
        for (int y = 1; y <= 3; y++) world.setBlock(-4, y, -3, BlockType.WOOD);
        for (int x = -5; x <= -3; x++) {
            for (int z = -4; z <= -2; z++) world.setBlock(x, 4, z, BlockType.LEAVES);
        }
        for (int x = 3; x <= 5; x++) {
            for (int z = -4; z <= -2; z++) world.setBlock(x, 1, z, BlockType.BRICK);
        }
        return world;
    }

    public String serialize() {
        StringBuilder out = new StringBuilder(Math.max(64, blocks.size() * 16));
        out.append(MAGIC).append(' ').append(FORMAT_VERSION).append('\n');
        for (BlockCell block : getBlocksSnapshot()) {
            out.append(block.position.x).append(',')
               .append(block.position.y).append(',')
               .append(block.position.z).append(',')
               .append(block.type.name()).append('\n');
        }
        return out.toString();
    }

    public static BlockWorld deserialize(String text) {
        if (text == null) throw new IllegalArgumentException("Save data is null");
        String[] lines = text.split("\\r?\\n");
        if (lines.length == 0 || !(MAGIC + " " + FORMAT_VERSION).equals(lines[0].trim())) {
            throw new IllegalArgumentException("Unsupported or invalid BlockVerse save header");
        }
        BlockWorld world = new BlockWorld();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] columns = line.split(",", -1);
            if (columns.length != 4) throw new IllegalArgumentException("Invalid block record on line " + (i + 1));
            try {
                int x = Integer.parseInt(columns[0].trim());
                int y = Integer.parseInt(columns[1].trim());
                int z = Integer.parseInt(columns[2].trim());
                BlockType type = BlockType.valueOf(columns[3].trim());
                world.setBlock(x, y, z, type);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("Invalid block record on line " + (i + 1), ex);
            }
        }
        return world;
    }
}
