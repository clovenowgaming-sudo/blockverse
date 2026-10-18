package com.blockverse.game.world;

public final class BlockCell {
    public final BlockPos position;
    public final BlockType type;

    public BlockCell(BlockPos position, BlockType type) {
        this.position = position;
        this.type = type;
    }
}
