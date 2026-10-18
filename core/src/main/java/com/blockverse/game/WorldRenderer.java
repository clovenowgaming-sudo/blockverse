package com.blockverse.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.ShaderProgram;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.blockverse.game.player.PlayerController;
import com.blockverse.game.world.BlockCell;
import com.blockverse.game.world.BlockType;
import com.blockverse.game.world.BlockWorld;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Chunked exposed-face voxel meshes keep the block world to a small number of draw calls. */
final class WorldRenderer implements Disposable {
    private static final int CHUNK_SIZE = 8;
    private static final long ATTRIBUTES = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
    private static final String VERTEX_SHADER =
        "attribute vec3 a_position;\n" +
        "attribute vec3 a_normal;\n" +
        "attribute vec4 a_color;\n" +
        "uniform mat4 u_projViewTrans;\n" +
        "varying vec3 v_normal;\n" +
        "varying vec4 v_color;\n" +
        "void main() { v_normal = a_normal; v_color = a_color; gl_Position = u_projViewTrans * vec4(a_position, 1.0); }\n";
    private static final String FRAGMENT_SHADER =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec3 v_normal;\n" +
        "varying vec4 v_color;\n" +
        "uniform vec3 u_lightDir;\n" +
        "uniform float u_ambient;\n" +
        "void main() { float diffuse = max(dot(normalize(v_normal), normalize(-u_lightDir)), 0.0); " +
        "float light = u_ambient + diffuse * (1.0 - u_ambient); gl_FragColor = vec4(v_color.rgb * light, v_color.a); }\n";

    private final ModelBatch modelBatch = new ModelBatch();
    private final Environment environment = new Environment();
    private final ShaderProgram blockShader;
    private final Map<Long, ChunkMesh> chunks = new HashMap<Long, ChunkMesh>();
    private final List<Model> avatarModels = new ArrayList<Model>();
    private final List<ModelInstance> avatarParts = new ArrayList<ModelInstance>();
    private final Vector3 scratchPosition = new Vector3();
    private long renderedRevision = Long.MIN_VALUE;

    WorldRenderer() {
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.72f, 0.72f, 0.76f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.78f, 0.72f, -0.6f, -1f, -0.35f));
        blockShader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (!blockShader.isCompiled()) {
            String log = blockShader.getLog();
            blockShader.dispose();
            throw new IllegalStateException("Block shader compilation failed: " + log);
        }

        ModelBuilder builder = new ModelBuilder();
        addAvatarPart(box(builder, 0.62f, 0.78f, 0.38f, new Color(0.20f, 0.42f, 0.86f, 1f)));
        addAvatarPart(box(builder, 0.42f, 0.42f, 0.42f, new Color(0.93f, 0.72f, 0.53f, 1f)));
        addAvatarPart(box(builder, 0.22f, 0.66f, 0.24f, new Color(0.18f, 0.33f, 0.70f, 1f)));
        addAvatarPart(box(builder, 0.22f, 0.66f, 0.24f, new Color(0.18f, 0.33f, 0.70f, 1f)));
        addAvatarPart(box(builder, 0.25f, 0.72f, 0.29f, new Color(0.13f, 0.15f, 0.19f, 1f)));
        addAvatarPart(box(builder, 0.25f, 0.72f, 0.29f, new Color(0.13f, 0.15f, 0.19f, 1f)));
    }

    private Model box(ModelBuilder builder, float x, float y, float z, Color color) {
        return builder.createBox(x, y, z, new Material(ColorAttribute.createDiffuse(color)), ATTRIBUTES);
    }

    private void addAvatarPart(Model model) {
        avatarModels.add(model);
        avatarParts.add(new ModelInstance(model));
    }

    void invalidateWorld() {
        renderedRevision = Long.MIN_VALUE;
    }

    void render(BlockWorld world, PlayerController player, com.badlogic.gdx.graphics.Camera camera, float elapsed, boolean highQuality) {
        rebuildChunksIfNeeded(world);
        float range = highQuality ? 42f : 19f;
        float range2 = range * range;
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        blockShader.bind();
        blockShader.setUniformMatrix("u_projViewTrans", camera.combined);
        blockShader.setUniformf("u_lightDir", -0.6f, -1f, -0.35f);
        blockShader.setUniformf("u_ambient", 0.52f);
        for (ChunkMesh chunk : chunks.values()) {
            float dx = chunk.centerX - camera.position.x;
            float dz = chunk.centerZ - camera.position.z;
            if (dx * dx + dz * dz <= range2) chunk.mesh.render(blockShader, GL20.GL_TRIANGLES);
        }

        updateAvatar(player, elapsed);
        modelBatch.begin(camera);
        for (ModelInstance part : avatarParts) modelBatch.render(part, environment);
        modelBatch.end();
    }

    private void rebuildChunksIfNeeded(BlockWorld world) {
        if (renderedRevision == world.getRevision()) return;
        for (ChunkMesh chunk : chunks.values()) chunk.mesh.dispose();
        chunks.clear();

        Map<Long, ChunkBuilder> builders = new HashMap<Long, ChunkBuilder>();
        for (BlockCell cell : world.getBlocksSnapshot()) {
            int chunkX = Math.floorDiv(cell.position.x, CHUNK_SIZE);
            int chunkZ = Math.floorDiv(cell.position.z, CHUNK_SIZE);
            long key = chunkKey(chunkX, chunkZ);
            ChunkBuilder builder = builders.get(key);
            if (builder == null) {
                builder = new ChunkBuilder(chunkX, chunkZ);
                builders.put(key, builder);
            }
            int x = cell.position.x;
            int y = cell.position.y;
            int z = cell.position.z;
            Color color = blockColor(cell.type);
            if (!world.hasBlock(x + 1, y, z)) addFace(builder, color, 1f, 0f, 0f,
                x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x + 1, y, z + 1);
            if (!world.hasBlock(x - 1, y, z)) addFace(builder, color, -1f, 0f, 0f,
                x, y, z + 1, x, y + 1, z + 1, x, y + 1, z, x, y, z);
            if (!world.hasBlock(x, y + 1, z)) addFace(builder, color, 0f, 1f, 0f,
                x, y + 1, z, x, y + 1, z + 1, x + 1, y + 1, z + 1, x + 1, y + 1, z);
            if (!world.hasBlock(x, y - 1, z)) addFace(builder, color, 0f, -1f, 0f,
                x, y, z + 1, x, y, z, x + 1, y, z, x + 1, y, z + 1);
            if (!world.hasBlock(x, y, z + 1)) addFace(builder, color, 0f, 0f, 1f,
                x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1);
            if (!world.hasBlock(x, y, z - 1)) addFace(builder, color, 0f, 0f, -1f,
                x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z);
        }

        for (Map.Entry<Long, ChunkBuilder> entry : builders.entrySet()) {
            ChunkBuilder builder = entry.getValue();
            if (builder.indices.size == 0) continue;
            int vertices = builder.vertices.size / 10;
            Mesh mesh = new Mesh(true, vertices, builder.indices.size,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color"));
            mesh.setVertices(builder.vertices.items, 0, builder.vertices.size);
            mesh.setIndices(builder.indices.items, 0, builder.indices.size);
            chunks.put(entry.getKey(), new ChunkMesh(mesh, builder.chunkX, builder.chunkZ));
        }
        renderedRevision = world.getRevision();
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    private void addFace(ChunkBuilder builder, Color color, float nx, float ny, float nz,
                         float x0, float y0, float z0, float x1, float y1, float z1,
                         float x2, float y2, float z2, float x3, float y3, float z3) {
        short base = (short) (builder.vertices.size / 10);
        addVertex(builder.vertices, x0, y0, z0, nx, ny, nz, color);
        addVertex(builder.vertices, x1, y1, z1, nx, ny, nz, color);
        addVertex(builder.vertices, x2, y2, z2, nx, ny, nz, color);
        addVertex(builder.vertices, x3, y3, z3, nx, ny, nz, color);
        builder.indices.add(base);
        builder.indices.add((short) (base + 1));
        builder.indices.add((short) (base + 2));
        builder.indices.add((short) (base + 2));
        builder.indices.add((short) (base + 3));
        builder.indices.add(base);
    }

    private void addVertex(FloatArray vertices, float x, float y, float z, float nx, float ny, float nz, Color color) {
        vertices.add(x); vertices.add(y); vertices.add(z);
        vertices.add(nx); vertices.add(ny); vertices.add(nz);
        vertices.add(color.r); vertices.add(color.g); vertices.add(color.b); vertices.add(color.a);
    }

    private Color blockColor(BlockType type) {
        switch (type) {
            case DIRT: return new Color(0.43f, 0.28f, 0.16f, 1f);
            case STONE: return new Color(0.48f, 0.51f, 0.55f, 1f);
            case WOOD: return new Color(0.48f, 0.30f, 0.13f, 1f);
            case LEAVES: return new Color(0.19f, 0.52f, 0.20f, 1f);
            case BRICK: return new Color(0.70f, 0.28f, 0.20f, 1f);
            case GRASS:
            default: return new Color(0.32f, 0.68f, 0.27f, 1f);
        }
    }

    private void updateAvatar(PlayerController player, float elapsed) {
        float x = player.getX();
        float feet = player.getY();
        float z = player.getZ();
        float walk = (float) Math.sin(elapsed * 10f) * 0.09f;
        avatarParts.get(0).transform.setToTranslation(x, feet + 1.12f, z);
        avatarParts.get(1).transform.setToTranslation(x, feet + 1.77f, z);
        avatarParts.get(2).transform.setToTranslation(x - 0.36f, feet + 1.15f + walk, z);
        avatarParts.get(3).transform.setToTranslation(x + 0.36f, feet + 1.15f - walk, z);
        avatarParts.get(4).transform.setToTranslation(x - 0.18f, feet + 0.38f - walk, z);
        avatarParts.get(5).transform.setToTranslation(x + 0.18f, feet + 0.38f + walk, z);
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        blockShader.dispose();
        for (ChunkMesh chunk : chunks.values()) chunk.mesh.dispose();
        for (Model model : avatarModels) model.dispose();
    }

    private static final class ChunkBuilder {
        final int chunkX;
        final int chunkZ;
        final FloatArray vertices = new FloatArray();
        final ShortArray indices = new ShortArray();
        ChunkBuilder(int x, int z) { chunkX = x; chunkZ = z; }
    }

    private static final class ChunkMesh {
        final Mesh mesh;
        final float centerX;
        final float centerZ;
        ChunkMesh(Mesh mesh, int chunkX, int chunkZ) {
            this.mesh = mesh;
            this.centerX = chunkX * CHUNK_SIZE + CHUNK_SIZE * 0.5f;
            this.centerZ = chunkZ * CHUNK_SIZE + CHUNK_SIZE * 0.5f;
        }
    }
}
