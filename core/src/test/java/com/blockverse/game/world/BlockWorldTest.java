package com.blockverse.game.world;

import org.junit.Test;
import static org.junit.Assert.*;

public class BlockWorldTest {
    @Test
    public void defaultWorldHasGroundAndProps() {
        BlockWorld world = BlockWorld.createDefault();
        assertTrue(world.size() > 1000);
        assertEquals(BlockType.GRASS, world.getBlock(0, 0, 0));
        assertEquals(BlockType.DIRT, world.getBlock(0, -1, 0));
        assertEquals(BlockType.STONE, world.getBlock(0, -2, 0));
        assertEquals(BlockType.WOOD, world.getBlock(-4, 2, -3));
    }

    @Test
    public void setAndRemoveUpdatesWorldAndRevision() {
        BlockWorld world = new BlockWorld();
        long initialRevision = world.getRevision();
        assertTrue(world.setBlock(2, 3, 4, BlockType.BRICK));
        assertEquals(BlockType.BRICK, world.getBlock(2, 3, 4));
        assertTrue(world.getRevision() > initialRevision);
        assertFalse(world.setBlock(2, 3, 4, BlockType.BRICK));
        assertTrue(world.removeBlock(2, 3, 4));
        assertFalse(world.hasBlock(2, 3, 4));
        assertFalse(world.removeBlock(2, 3, 4));
    }

    @Test
    public void serializeDeserializeRoundTripsBlockData() {
        BlockWorld source = new BlockWorld();
        source.setBlock(-2, 0, 5, BlockType.GRASS);
        source.setBlock(-2, 1, 5, BlockType.WOOD);
        source.setBlock(8, -3, -9, BlockType.BRICK);
        BlockWorld loaded = BlockWorld.deserialize(source.serialize());
        assertEquals(source.size(), loaded.size());
        assertEquals(BlockType.GRASS, loaded.getBlock(-2, 0, 5));
        assertEquals(BlockType.WOOD, loaded.getBlock(-2, 1, 5));
        assertEquals(BlockType.BRICK, loaded.getBlock(8, -3, -9));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnsupportedSaveHeader() {
        BlockWorld.deserialize("NOT_A_BLOCKVERSE_SAVE\n0,0,0,GRASS\n");
    }

    @Test
    public void collisionUsesOccupiedBlockVolume() {
        BlockWorld world = new BlockWorld();
        world.setBlock(0, 0, 0, BlockType.STONE);
        assertTrue(world.collides(0.5f, 0f, 0.5f, 0.25f, 1f));
        assertFalse(world.collides(2.5f, 0f, 0.5f, 0.25f, 1f));
    }
}
