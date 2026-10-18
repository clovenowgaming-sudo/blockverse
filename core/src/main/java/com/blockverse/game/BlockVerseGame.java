package com.blockverse.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.blockverse.game.player.PlayerController;
import com.blockverse.game.world.BlockPos;
import com.blockverse.game.world.BlockType;
import com.blockverse.game.world.BlockWorld;

/** Stage-one playable local sandbox prototype. No network/account/multiplayer code is included. */
public final class BlockVerseGame extends ApplicationAdapter implements InputProcessor {
    private static final int MAIN_MENU = 0;
    private static final int PLAYING = 1;
    private static final int PAUSED = 2;
    private static final int SETTINGS = 3;
    private static final float STICK_RADIUS = 66f;

    private final PlayerController player = new PlayerController();
    private BlockWorld world;
    private WorldRenderer renderer;
    private PerspectiveCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapes;
    private BitmapFont font;
    private ScreenViewport hudViewport;
    private int screenMode = MAIN_MENU;
    private boolean highQuality = true;
    private boolean queuedJump;
    private BlockType selectedBlock = BlockType.GRASS;
    private int inventorySlot = 0;
    private float cameraYaw = 0f;
    private float cameraPitch = 0.42f;
    private float cameraDistance = 7.5f;
    private float elapsed;
    private int joystickPointer = -1;
    private int lookPointer = -1;
    private float joystickX;
    private float joystickForward;
    private float stickTouchX;
    private float stickTouchY;
    private float previousLookX;
    private float previousLookY;
    private String notice = "Welcome to BlockVerse";
    private float noticeTimer = 3f;
    private final Vector3 target = new Vector3();
    private final Vector3 rayPoint = new Vector3();

    @Override
    public void create() {
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_CULL_FACE);
        Gdx.input.setInputProcessor(this);
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(false);

        world = BlockWorld.createDefault();
        renderer = new WorldRenderer();
        camera = new PerspectiveCamera(67f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 0.1f;
        camera.far = 120f;
        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(1.15f);
        hudViewport = new ScreenViewport(new OrthographicCamera());
        updateCamera();
        loadPreferenceSettings();
    }

    private void loadPreferenceSettings() {
        highQuality = Gdx.app.getPreferences("BlockVerse").getBoolean("highQuality", true);
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        elapsed += dt;
        if (noticeTimer > 0f) noticeTimer -= dt;

        if (screenMode == PLAYING) updateGameplay(dt);
        updateCamera();

        ScreenUtils.clear(0.48f, 0.73f, 0.91f, 1f, true);
        renderer.render(world, player, camera, elapsed, highQuality);
        drawOverlay();
    }

    private void updateGameplay(float dt) {
        float strafe = joystickX;
        float forward = joystickForward;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) strafe -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) strafe += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) forward += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) forward -= 1f;
            if (Gdx.input.isCursorCatched()) {
                cameraYaw -= Gdx.input.getDeltaX() * 0.0048f;
                cameraPitch = MathUtils.clamp(cameraPitch - Gdx.input.getDeltaY() * 0.0048f, 0.12f, 0.92f);
            }
        }
        float moveX = (float) (-Math.sin(cameraYaw) * forward + Math.cos(cameraYaw) * strafe);
        float moveZ = (float) (-Math.cos(cameraYaw) * forward - Math.sin(cameraYaw) * strafe);
        boolean jump = queuedJump;
        queuedJump = false;
        if (Gdx.app.getType() == Application.ApplicationType.Desktop && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) jump = true;
        player.update(world, moveX, moveZ, jump, dt);
    }

    private void updateCamera() {
        target.set(player.getX(), player.getY() + 1.0f, player.getZ());
        float horizontal = (float) Math.cos(cameraPitch) * cameraDistance;
        camera.position.set(
            target.x + (float) Math.sin(cameraYaw) * horizontal,
            target.y + (float) Math.sin(cameraPitch) * cameraDistance,
            target.z + (float) Math.cos(cameraYaw) * horizontal);
        camera.lookAt(target);
        camera.up.set(Vector3.Y);
        camera.update();
    }

    private void drawOverlay() {
        hudViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        batch.setProjectionMatrix(hudViewport.getCamera().combined);
        shapes.setProjectionMatrix(hudViewport.getCamera().combined);
        if (screenMode == PLAYING) drawGameHud();
        else drawMenu();
        if (noticeTimer > 0f && screenMode == PLAYING) {
            batch.begin();
            font.setColor(Color.WHITE);
            font.draw(batch, notice, 18f, Gdx.graphics.getHeight() - 22f);
            batch.end();
        }
    }

    private void drawGameHud() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.07f, 0.10f, 0.55f);
        shapes.rect(12f, height - 62f, 285f, 47f);
        shapes.setColor(0.04f, 0.07f, 0.10f, 0.65f);
        shapes.rect(width - 101f, height - 49f, 88f, 34f);
        shapes.setColor(1f, 1f, 1f, 0.88f);
        shapes.rect(width / 2f - 9f, height / 2f - 1f, 18f, 2f);
        shapes.rect(width / 2f - 1f, height / 2f - 9f, 2f, 18f);
        // Four-slot inventory bar.
        float slot = 48f;
        float inventoryX = width / 2f - slot * 2f;
        shapes.setColor(0.04f, 0.07f, 0.10f, 0.72f);
        shapes.rect(inventoryX - 5f, 14f, slot * 4f + 10f, slot + 10f);
        for (int i = 0; i < 4; i++) {
            shapes.setColor(i == inventorySlot ? 0.34f : 0.18f, i == inventorySlot ? 0.72f : 0.22f, i == inventorySlot ? 0.36f : 0.27f, 0.95f);
            shapes.rect(inventoryX + i * slot, 19f, slot - 3f, slot - 3f);
        }
        if (Gdx.app.getType() != Application.ApplicationType.Desktop) drawMobileControls(shapes, width);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "BLOCKVERSE  |  " + (highQuality ? "HIGH" : "LOW") + " GRAPHICS", 22f, height - 25f);
        font.draw(batch, "WASD move  SPACE jump  Mouse look  Left break  Right place  ESC menu", 22f, height - 46f);
        String[] slotNames = {"Grass", "Dirt", "Stone", "Brick"};
        for (int i = 0; i < slotNames.length; i++) {
            font.draw(batch, (i + 1) + " " + slotNames[i], inventoryX + i * slot + 3f, 39f);
        }
        font.draw(batch, "Blocks: " + world.size(), width - 135f, height - 24f);
        font.draw(batch, "MENU", width - 83f, height - 24f);
        if (Gdx.app.getType() != Application.ApplicationType.Desktop) {
            font.draw(batch, "MOVE", 77f, 24f);
            font.draw(batch, "BREAK", width - 287f, 89f);
            font.draw(batch, "PLACE", width - 200f, 89f);
            font.draw(batch, "JUMP", width - 117f, 89f);
        }
        batch.end();
    }

    private void drawMobileControls(ShapeRenderer shape, float width) {
        shape.setColor(0.04f, 0.07f, 0.10f, 0.45f);
        shape.circle(105f, 105f, STICK_RADIUS + 15f);
        shape.setColor(0.76f, 0.83f, 0.88f, 0.55f);
        shape.circle(105f + joystickX * STICK_RADIUS, 105f + joystickForward * STICK_RADIUS, 27f);
        shape.setColor(0.12f, 0.18f, 0.24f, 0.64f);
        shape.circle(width - 105f, 100f, 38f);
        shape.circle(width - 190f, 100f, 28f);
        shape.circle(width - 270f, 100f, 28f);
    }

    private void drawMenu() {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.035f, 0.055f, 0.085f, 0.88f);
        shapes.rect(0f, 0f, width, height);
        if (screenMode == MAIN_MENU) {
            drawMenuButtonShape(width, height * 0.50f);
            drawMenuButtonShape(width, height * 0.50f - 60f);
            drawMenuButtonShape(width, height * 0.50f - 120f);
        } else if (screenMode == PAUSED) {
            for (int i = 0; i < 5; i++) drawMenuButtonShape(width, height * 0.58f - i * 54f);
        } else if (screenMode == SETTINGS) {
            drawMenuButtonShape(width, height * 0.50f);
            drawMenuButtonShape(width, height * 0.50f - 80f);
        }
        shapes.end();

        batch.begin();
        font.setColor(new Color(0.42f, 0.88f, 0.44f, 1f));
        drawCentered("BLOCKVERSE", width / 2f, height * 0.77f);
        font.setColor(Color.WHITE);
        if (screenMode == MAIN_MENU) {
            font.getData().setScale(1.0f);
            drawButtonLabel("PLAY", height * 0.50f + 28f);
            drawButtonLabel("GRAPHICS SETTINGS", height * 0.50f - 32f);
            drawButtonLabel("QUIT", height * 0.50f - 92f);
            drawCentered("Local 3D sandbox prototype", width / 2f, height * 0.70f);
        } else if (screenMode == PAUSED) {
            font.getData().setScale(1.05f);
            drawButtonLabel("RESUME", height * 0.58f + 28f);
            drawButtonLabel("SAVE WORLD", height * 0.58f - 26f);
            drawButtonLabel("LOAD WORLD", height * 0.58f - 80f);
            drawButtonLabel("GRAPHICS SETTINGS", height * 0.58f - 134f);
            drawButtonLabel("MAIN MENU", height * 0.58f - 188f);
        } else if (screenMode == SETTINGS) {
            font.getData().setScale(1.0f);
            drawButtonLabel("GRAPHICS: " + (highQuality ? "HIGH" : "LOW"), height * 0.50f + 28f);
            drawButtonLabel("BACK", height * 0.50f - 52f);
            font.getData().setScale(0.9f);
            drawCentered("Low reduces block draw distance on slower devices.", width / 2f, height * 0.39f);
        }
        font.getData().setScale(0.82f);
        font.setColor(new Color(0.79f, 0.84f, 0.89f, 1f));
        drawCentered("STAGE 1  |  OFFLINE ONLY  |  NO ACCOUNT OR MULTIPLAYER YET", width / 2f, 24f);
        batch.end();
    }

    private void drawMenuButtonShape(float width, float y) {
        shapes.setColor(0.12f, 0.19f, 0.25f, 0.98f);
        shapes.rect(width / 2f - 160f, y, 320f, 44f);
        shapes.setColor(0.24f, 0.55f, 0.29f, 1f);
        shapes.rect(width / 2f - 160f, y, 4f, 44f);
    }

    private void drawButtonLabel(String text, float baseline) {
        drawCentered(text, Gdx.graphics.getWidth() / 2f, baseline);
    }

    private void drawCentered(String text, float centerX, float y) {
        GlyphLayout layout = new GlyphLayout(font, text);
        font.draw(batch, text, centerX - layout.width / 2f, y);
    }

    private void saveWorld() {
        try {
            com.badlogic.gdx.files.FileHandle file = Gdx.files.local("saves/world-1.bvw");
            file.parent().mkdirs();
            file.writeString(world.serialize(), false, "UTF-8");
            showNotice("World saved to " + file.path());
        } catch (Exception ex) {
            showNotice("Save failed: " + safeMessage(ex));
            Gdx.app.error("BlockVerse", "World save failed", ex);
        }
    }

    private void loadWorld() {
        try {
            com.badlogic.gdx.files.FileHandle file = Gdx.files.local("saves/world-1.bvw");
            if (!file.exists()) { showNotice("No saved world yet. Use Save World first."); return; }
            BlockWorld loaded = BlockWorld.deserialize(file.readString("UTF-8"));
            world = loaded;
            renderer.invalidateWorld();
            player.setPosition(0f, 1.02f, 3f);
            showNotice("Loaded " + world.size() + " blocks");
        } catch (Exception ex) {
            showNotice("Load failed: " + safeMessage(ex));
            Gdx.app.error("BlockVerse", "World load failed", ex);
        }
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private void showNotice(String text) {
        notice = text;
        noticeTimer = 4f;
    }

    private void handleMenuTap(float x, float y) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        if (screenMode == MAIN_MENU) {
            if (buttonHit(x, y, width, height * 0.50f)) {
                screenMode = PLAYING;
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(true);
                showNotice("WASD move | Space jump | click blocks to edit");
            } else if (buttonHit(x, y, width, height * 0.50f - 60f)) {
                enterSettings();
            } else if (buttonHit(x, y, width, height * 0.50f - 120f)) {
                Gdx.app.exit();
            }
        } else if (screenMode == PAUSED) {
            if (buttonHit(x, y, width, height * 0.58f)) {
                screenMode = PLAYING;
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(true);
            } else if (buttonHit(x, y, width, height * 0.58f - 54f)) saveWorld();
            else if (buttonHit(x, y, width, height * 0.58f - 108f)) loadWorld();
            else if (buttonHit(x, y, width, height * 0.58f - 162f)) enterSettings();
            else if (buttonHit(x, y, width, height * 0.58f - 216f)) {
                screenMode = MAIN_MENU;
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(false);
            }
        } else if (screenMode == SETTINGS) {
            if (buttonHit(x, y, width, height * 0.50f)) {
                highQuality = !highQuality;
                Gdx.app.getPreferences("BlockVerse").putBoolean("highQuality", highQuality).flush();
            } else if (buttonHit(x, y, width, height * 0.50f - 80f)) {
                screenMode = SETTINGS_RETURN_TO_PAUSE ? PAUSED : MAIN_MENU;
                SETTINGS_RETURN_TO_PAUSE = false;
            }
        }
    }

    private boolean SETTINGS_RETURN_TO_PAUSE;

    private boolean buttonHit(float x, float y, float width, float buttonY) {
        return x >= width / 2f - 160f && x <= width / 2f + 160f && y >= buttonY && y <= buttonY + 44f;
    }

    private void enterSettings() {
        SETTINGS_RETURN_TO_PAUSE = screenMode == PAUSED;
        screenMode = SETTINGS;
    }

    private void breakTargetBlock() {
        BlockPos targetBlock = raycastBlock();
        if (targetBlock != null && world.removeBlock(targetBlock.x, targetBlock.y, targetBlock.z)) {
            showNotice("Block removed");
        }
    }

    private void placeTargetBlock() {
        BlockPos targetBlock = raycastAdjacentEmptyBlock();
        if (targetBlock == null) { showNotice("No target block in reach"); return; }
        if (overlapsPlayer(targetBlock)) {
            showNotice("Can't place a block inside your character");
            return;
        }
        if (world.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, selectedBlock)) showNotice(selectedBlock + " placed");
    }

    private boolean overlapsPlayer(BlockPos block) {
        boolean overlapX = Math.abs(player.getX() - (block.x + 0.5f)) < PlayerController.RADIUS + 0.5f;
        boolean overlapZ = Math.abs(player.getZ() - (block.z + 0.5f)) < PlayerController.RADIUS + 0.5f;
        boolean overlapY = player.getY() < block.y + 1f && player.getY() + PlayerController.HEIGHT > block.y;
        return overlapX && overlapY && overlapZ;
    }

    private BlockPos raycastBlock() {
        Vector3 direction = camera.direction.cpy().nor();
        BlockPos previous = null;
        for (float distance = 0.15f; distance <= 7f; distance += 0.08f) {
            rayPoint.set(camera.position).mulAdd(direction, distance);
            int x = BlockWorld.floor(rayPoint.x);
            int y = BlockWorld.floor(rayPoint.y);
            int z = BlockWorld.floor(rayPoint.z);
            if (world.hasBlock(x, y, z)) return new BlockPos(x, y, z);
            previous = new BlockPos(x, y, z);
        }
        return null;
    }

    private BlockPos raycastAdjacentEmptyBlock() {
        Vector3 direction = camera.direction.cpy().nor();
        BlockPos previous = null;
        for (float distance = 0.15f; distance <= 7f; distance += 0.08f) {
            rayPoint.set(camera.position).mulAdd(direction, distance);
            int x = BlockWorld.floor(rayPoint.x);
            int y = BlockWorld.floor(rayPoint.y);
            int z = BlockWorld.floor(rayPoint.z);
            if (world.hasBlock(x, y, z)) return previous;
            previous = new BlockPos(x, y, z);
        }
        return null;
    }

    private boolean selectInventorySlot(float x, float y) {
        float left = Gdx.graphics.getWidth() / 2f - 96f;
        if (y < 14f || y > 66f || x < left || x >= left + 192f) return false;
        int slot = (int) ((x - left) / 48f);
        if (slot < 0 || slot > 3) return false;
        inventorySlot = slot;
        selectedBlock = blockForSlot(slot);
        return true;
    }

    private void updateJoystick(int screenX, int screenY) {
        float bottomY = Gdx.graphics.getHeight() - screenY;
        float dx = screenX - 105f;
        float dy = bottomY - 105f;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length > STICK_RADIUS) {
            dx = dx * STICK_RADIUS / length;
            dy = dy * STICK_RADIUS / length;
        }
        joystickX = dx / STICK_RADIUS;
        joystickForward = dy / STICK_RADIUS;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            if (screenMode == PLAYING) {
                screenMode = PAUSED;
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(false);
            } else if (screenMode == PAUSED) {
                screenMode = PLAYING;
                if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(true);
            } else if (screenMode == SETTINGS) {
                screenMode = SETTINGS_RETURN_TO_PAUSE ? PAUSED : MAIN_MENU;
                SETTINGS_RETURN_TO_PAUSE = false;
            }
            return true;
        }
        if (screenMode == PLAYING) {
            if (keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_4) {
                inventorySlot = keycode - Input.Keys.NUM_1;
                selectedBlock = blockForSlot(inventorySlot);
                return true;
            }
            if (keycode == Input.Keys.F5) { saveWorld(); return true; }
            if (keycode == Input.Keys.F9) { loadWorld(); return true; }
            if (keycode == Input.Keys.SPACE) { queuedJump = true; return true; }
        }
        return false;
    }

    private BlockType blockForSlot(int slot) {
        switch (slot) {
            case 1: return BlockType.DIRT;
            case 2: return BlockType.STONE;
            case 3: return BlockType.BRICK;
            default: return BlockType.GRASS;
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float x = screenX;
        float y = Gdx.graphics.getHeight() - screenY;
        if (screenMode != PLAYING) {
            handleMenuTap(x, y);
            if (screenMode == PLAYING && Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(true);
            return true;
        }
        if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
            if (button == Input.Buttons.LEFT) breakTargetBlock();
            else if (button == Input.Buttons.RIGHT) placeTargetBlock();
            Gdx.input.setCursorCatched(true);
            return true;
        }

        float width = Gdx.graphics.getWidth();
        if (x > width - 110f && y > Gdx.graphics.getHeight() - 55f) {
            screenMode = PAUSED;
            if (Gdx.app.getType() == Application.ApplicationType.Desktop) Gdx.input.setCursorCatched(false);
            return true;
        }
        if (selectInventorySlot(x, y)) return true;
        if (x < 215f && y < 215f) {
            joystickPointer = pointer;
            updateJoystick(screenX, screenY);
            return true;
        }
        if (x > width - 150f && y < 145f) {
            queuedJump = true;
            return true;
        }
        if (x > width - 230f && x <= width - 150f && y < 140f) {
            placeTargetBlock();
            return true;
        }
        if (x > width - 315f && x <= width - 230f && y < 140f) {
            breakTargetBlock();
            return true;
        }
        lookPointer = pointer;
        previousLookX = screenX;
        previousLookY = screenY;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer == joystickPointer) {
            joystickPointer = -1;
            joystickX = 0f;
            joystickForward = 0f;
        }
        if (pointer == lookPointer) lookPointer = -1;
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer == joystickPointer) {
            updateJoystick(screenX, screenY);
        } else if (pointer == lookPointer && Gdx.app.getType() != Application.ApplicationType.Desktop) {
            float dx = screenX - previousLookX;
            float dy = screenY - previousLookY;
            cameraYaw -= dx * 0.008f;
            cameraPitch = MathUtils.clamp(cameraPitch - dy * 0.008f, 0.12f, 0.92f);
            previousLookX = screenX;
            previousLookY = screenY;
        }
        return true;
    }

    @Override
    public void resize(int width, int height) {
        if (camera != null && width > 0 && height > 0) {
            camera.viewportWidth = width;
            camera.viewportHeight = height;
            camera.update();
        }
        if (hudViewport != null) hudViewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
    }

    @Override public boolean keyUp(int keycode) { return false; }
    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return touchUp(screenX, screenY, pointer, button); }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) {
        cameraDistance = MathUtils.clamp(cameraDistance + amountY * 0.6f, 4f, 12f);
        return true;
    }
}
