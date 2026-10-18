package com.blockverse.game.world;

import java.util.Objects;

public final class BlockPos {
    public final int x;
    public final int y;
    public final int z;

    public BlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BlockPos)) return false;
        BlockPos pos = (BlockPos) other;
        return x == pos.x && y == pos.y && z == pos.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}
