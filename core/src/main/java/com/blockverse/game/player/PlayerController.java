package com.blockverse.game.player;

import com.blockverse.game.world.BlockWorld;

/** Lightweight voxel collision/gravity controller; not a full rigid-body physics engine. */
public final class PlayerController {
    public static final float RADIUS = 0.46f;
    public static final float HEIGHT = 1.98f;
    private static final float WALK_SPEED = 5.0f;
    private static final float GRAVITY = 22.0f;
    private static final float JUMP_SPEED = 7.4f;

    private float x;
    private float y = 1.02f;
    private float z = 3.0f;
    private float verticalVelocity;
    private boolean grounded;

    public void update(BlockWorld world, float moveX, float moveZ, boolean jumpPressed, float deltaSeconds) {
        float dt = Math.max(0f, Math.min(deltaSeconds, 0.05f));
        if (dt == 0f) return;

        float inputLength = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (inputLength > 1f) { moveX /= inputLength; moveZ /= inputLength; }
        if (jumpPressed && grounded) {
            verticalVelocity = JUMP_SPEED;
            grounded = false;
        }

        float horizontalX = moveX * WALK_SPEED * dt;
        float horizontalZ = moveZ * WALK_SPEED * dt;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(horizontalX), Math.abs(horizontalZ)) / 0.08f));
        float stepX = horizontalX / steps;
        float stepZ = horizontalZ / steps;
        for (int i = 0; i < steps; i++) {
            if (!world.collides(x + stepX, y, z, RADIUS, HEIGHT)) x += stepX;
            if (!world.collides(x, y, z + stepZ, RADIUS, HEIGHT)) z += stepZ;
        }

        verticalVelocity -= GRAVITY * dt;
        float nextY = y + verticalVelocity * dt;
        if (!world.collides(x, nextY, z, RADIUS, HEIGHT)) {
            y = nextY;
            grounded = false;
        } else {
            float low = Math.min(y, nextY);
            float high = Math.max(y, nextY);
            for (int i = 0; i < 18; i++) {
                float mid = (low + high) * 0.5f;
                if (world.collides(x, mid, z, RADIUS, HEIGHT)) {
                    if (nextY < y) low = mid;
                    else high = mid;
                } else {
                    if (nextY < y) high = mid;
                    else low = mid;
                }
            }
            y = nextY < y ? high : low;
            if (verticalVelocity < 0f) grounded = true;
            verticalVelocity = 0f;
        }
        if (verticalVelocity <= 0f && world.collides(x, y - 0.04f, z, RADIUS, HEIGHT)) {
            grounded = true;
            verticalVelocity = 0f;
        }
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getVerticalVelocity() { return verticalVelocity; }
    public boolean isGrounded() { return grounded; }
    public void setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        this.verticalVelocity = 0f;
        this.grounded = false;
    }
}
