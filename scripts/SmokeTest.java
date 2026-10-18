import com.blockverse.game.world.BlockType;
import com.blockverse.game.world.BlockWorld;
import com.blockverse.game.player.PlayerController;

/** Dependency-free smoke test for the deterministic world model and basic movement. */
public final class SmokeTest {
    public static void main(String[] args) {
        BlockWorld world = BlockWorld.createDefault();
        check(world.size() > 1000, "default terrain created");
        check(world.getBlock(0, 0, 0) == BlockType.GRASS, "grass layer");
        world.setBlock(20, 3, 20, BlockType.BRICK);
        BlockWorld restored = BlockWorld.deserialize(world.serialize());
        check(restored.size() == world.size(), "save/load retains block count");
        check(restored.getBlock(20, 3, 20) == BlockType.BRICK, "save/load retains custom block");

        PlayerController player = new PlayerController();
        for (int i = 0; i < 120; i++) player.update(world, 0, 0, false, 1f / 60f);
        check(player.getY() >= 0.9f && player.getY() <= 1.1f, "gravity settles player on ground");
        float groundedY = player.getY();
        player.update(world, 0, 0, true, 1f / 60f);
        check(player.getY() > groundedY, "jump impulse moves player upward");
        player.setPosition(0f, 1.02f, 3f);
        for (int i = 0; i < 120; i++) player.update(world, 0, 0, false, 1f / 60f);
        float zBefore = player.getZ();
        for (int i = 0; i < 30; i++) player.update(world, 0, 1, false, 1f / 60f);
        check(player.getZ() > zBefore, "movement input changes player position");

        BlockWorld wallWorld = BlockWorld.createDefault();
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z <= 6; z++) wallWorld.setBlock(1, y, z, BlockType.STONE);
        }
        PlayerController blockedPlayer = new PlayerController();
        blockedPlayer.setPosition(0f, 1.02f, 3f);
        for (int i = 0; i < 120; i++) blockedPlayer.update(wallWorld, 0, 0, false, 1f / 60f);
        for (int i = 0; i < 120; i++) blockedPlayer.update(wallWorld, 1, 0, false, 1f / 60f);
        check(blockedPlayer.getX() < 0.75f, "solid block wall prevents walking through");
        System.out.println("PASS: all standalone smoke tests");
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError("FAIL: " + name);
        System.out.println("PASS: " + name);
    }
}
