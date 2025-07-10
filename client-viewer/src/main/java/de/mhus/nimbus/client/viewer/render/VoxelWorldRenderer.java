package de.mhus.nimbus.client.viewer.render;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 3D-Renderer für Voxel-Welten
 * Lädt Chunk-Daten über WorldVoxelClient und rendert sie als 3D-Cubes
 */
@Slf4j
public class VoxelWorldRenderer {

    private static final int CHUNK_SIZE = 16;
    private static final float VOXEL_SIZE = 1.0f;
    private static final int RENDER_DISTANCE = 8; // Chunks

    private final WorldVoxelClient worldVoxelClient;
    private final Camera camera;

    // OpenGL Resources
    private int shaderProgram;
    private int cubeVAO;
    private int cubeVBO;
    private int cubeEBO;

    // Shader Uniforms
    private int uModelLoc;
    private int uViewLoc;
    private int uProjectionLoc;
    private int uColorLoc;

    // World Data
    private final Map<String, VoxelChunk> loadedChunks = new ConcurrentHashMap<>();
    private final Map<String, ChunkMesh> chunkMeshes = new ConcurrentHashMap<>();
    private String currentWorldId = "default";

    // Matrices
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();

    public VoxelWorldRenderer(WorldVoxelClient worldVoxelClient) {
        this.worldVoxelClient = worldVoxelClient;
        this.camera = new Camera();
    }

    /**
     * Initialisiert den Renderer
     */
    public void init(int windowWidth, int windowHeight) {
        LOGGER.info("Initialisiere VoxelWorldRenderer...");

        // OpenGL Setup
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        // Erstelle Shader
        createShaderProgram();

        // Erstelle Cube Mesh
        createCubeMesh();

        // Setup Projection Matrix
        updateProjection(windowWidth, windowHeight);

        LOGGER.info("VoxelWorldRenderer initialisiert");
    }

    /**
     * Rendert die Voxel-Welt
     */
    public void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Update View Matrix
        camera.updateViewMatrix(viewMatrix);

        // Use Shader Program
        glUseProgram(shaderProgram);

        // Upload Matrices
        uploadMatrix(uViewLoc, viewMatrix);
        uploadMatrix(uProjectionLoc, projectionMatrix);

        // Bind Cube VAO
        glBindVertexArray(cubeVAO);

        // Render loaded chunks
        renderLoadedChunks();

        // Unbind
        glBindVertexArray(0);
        glUseProgram(0);
    }

    /**
     * Rendert alle geladenen Chunks
     */
    private void renderLoadedChunks() {
        for (Map.Entry<String, VoxelChunk> entry : loadedChunks.entrySet()) {
            VoxelChunk chunk = entry.getValue();
            renderChunk(chunk);
        }
    }

    /**
     * Rendert einen einzelnen Chunk
     */
    private void renderChunk(VoxelChunk chunk) {
        if (chunk == null || chunk.getVoxels() == null) return;

        for (Voxel voxel : chunk.getVoxels()) {
            if (voxel != null && !voxel.isEmpty()) {
                renderVoxel(voxel);
            }
        }
    }

    /**
     * Rendert einen einzelnen Voxel
     */
    private void renderVoxel(Voxel voxel) {
        // Position für den Voxel berechnen
        modelMatrix.identity()
            .translate(voxel.getX() * VOXEL_SIZE,
                      voxel.getY() * VOXEL_SIZE,
                      voxel.getZ() * VOXEL_SIZE)
            .scale(VOXEL_SIZE);

        // Upload Model Matrix
        uploadMatrix(uModelLoc, modelMatrix);

        // Set Color based on voxel type
        Vector3f color = getVoxelColor(voxel);
        glUniform3f(uColorLoc, color.x, color.y, color.z);

        // Draw Cube
        glDrawElements(GL_TRIANGLES, 36, GL_UNSIGNED_INT, 0);
    }

    /**
     * Bestimmt die Farbe basierend auf dem Voxel-Typ
     */
    private Vector3f getVoxelColor(Voxel voxel) {
        // Einfache Farbzuordnung basierend auf Voxel-Properties
        String material = voxel.getMaterial();
        if (material == null) material = "default";

        return switch (material.toLowerCase()) {
            case "grass" -> new Vector3f(0.2f, 0.8f, 0.2f);
            case "dirt" -> new Vector3f(0.6f, 0.4f, 0.2f);
            case "stone" -> new Vector3f(0.5f, 0.5f, 0.5f);
            case "water" -> new Vector3f(0.2f, 0.4f, 0.8f);
            case "wood" -> new Vector3f(0.6f, 0.3f, 0.1f);
            case "sand" -> new Vector3f(0.9f, 0.8f, 0.6f);
            default -> new Vector3f(0.8f, 0.8f, 0.8f);
        };
    }

    /**
     * Lädt einen Chunk über den WorldVoxelClient
     */
    public void loadChunk(int chunkX, int chunkY, int chunkZ) {
        String chunkKey = getChunkKey(chunkX, chunkY, chunkZ);

        if (loadedChunks.containsKey(chunkKey)) {
            return; // Chunk bereits geladen
        }

        LOGGER.debug("Lade Chunk: {}", chunkKey);

        worldVoxelClient.loadChunk(currentWorldId, chunkX, chunkY, chunkZ)
            .thenAccept(chunk -> {
                if (chunk != null) {
                    loadedChunks.put(chunkKey, chunk);
                    LOGGER.debug("Chunk geladen: {}", chunkKey);
                } else {
                    LOGGER.warn("Chunk ist null: {}", chunkKey);
                }
            })
            .exceptionally(ex -> {
                LOGGER.error("Fehler beim Laden von Chunk {}: {}", chunkKey, ex.getMessage());
                return null;
            });
    }

    /**
     * Lädt Chunks in der Nähe der Kamera-Position
     */
    public void loadNearbyChunks() {
        Vector3f cameraPos = camera.getPosition();
        int centerChunkX = (int) Math.floor(cameraPos.x / (CHUNK_SIZE * VOXEL_SIZE));
        int centerChunkY = (int) Math.floor(cameraPos.y / (CHUNK_SIZE * VOXEL_SIZE));
        int centerChunkZ = (int) Math.floor(cameraPos.z / (CHUNK_SIZE * VOXEL_SIZE));

        for (int dx = -RENDER_DISTANCE; dx <= RENDER_DISTANCE; dx++) {
            for (int dy = -2; dy <= 2; dy++) { // Begrenzte Y-Reichweite
                for (int dz = -RENDER_DISTANCE; dz <= RENDER_DISTANCE; dz++) {
                    loadChunk(centerChunkX + dx, centerChunkY + dy, centerChunkZ + dz);
                }
            }
        }
    }

    /**
     * Entlädt weit entfernte Chunks um Speicher zu sparen
     */
    public void unloadDistantChunks() {
        Vector3f cameraPos = camera.getPosition();
        int centerChunkX = (int) Math.floor(cameraPos.x / (CHUNK_SIZE * VOXEL_SIZE));
        int centerChunkZ = (int) Math.floor(cameraPos.z / (CHUNK_SIZE * VOXEL_SIZE));

        List<String> chunksToUnload = new ArrayList<>();

        for (String chunkKey : loadedChunks.keySet()) {
            String[] parts = chunkKey.split(",");
            int chunkX = Integer.parseInt(parts[0]);
            int chunkZ = Integer.parseInt(parts[2]);

            int distance = Math.max(Math.abs(chunkX - centerChunkX),
                                   Math.abs(chunkZ - centerChunkZ));

            if (distance > RENDER_DISTANCE + 2) {
                chunksToUnload.add(chunkKey);
            }
        }

        for (String chunkKey : chunksToUnload) {
            loadedChunks.remove(chunkKey);
            chunkMeshes.remove(chunkKey);
            LOGGER.debug("Chunk entladen: {}", chunkKey);
        }
    }

    /**
     * Update-Methode für jeden Frame
     */
    public void update(float deltaTime) {
        camera.update(deltaTime);
        loadNearbyChunks();

        // Alle 2 Sekunden weit entfernte Chunks entladen
        if (System.currentTimeMillis() % 2000 < 16) {
            unloadDistantChunks();
        }
    }

    /**
     * Aktualisiert die Projektions-Matrix bei Fenster-Resize
     */
    public void updateProjection(int width, int height) {
        float aspect = (float) width / (float) height;
        projectionMatrix.setPerspective((float) Math.toRadians(70.0), aspect, 0.1f, 1000.0f);

        glViewport(0, 0, width, height);
    }

    /**
     * Erstellt das Shader-Programm
     */
    private void createShaderProgram() {
        String vertexShaderSource = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            
            out vec3 FragPos;
            out vec3 Normal;
            
            void main() {
                FragPos = vec3(uModel * vec4(aPos, 1.0));
                Normal = mat3(transpose(inverse(uModel))) * aNormal;
                
                gl_Position = uProjection * uView * vec4(FragPos, 1.0);
            }
            """;

        String fragmentShaderSource = """
            #version 330 core
            out vec4 FragColor;
            
            in vec3 FragPos;
            in vec3 Normal;
            
            uniform vec3 uColor;
            
            void main() {
                // Einfache Beleuchtung
                vec3 lightPos = vec3(100.0, 100.0, 100.0);
                vec3 lightColor = vec3(1.0, 1.0, 1.0);
                
                // Ambient
                float ambientStrength = 0.3;
                vec3 ambient = ambientStrength * lightColor;
                
                // Diffuse
                vec3 norm = normalize(Normal);
                vec3 lightDir = normalize(lightPos - FragPos);
                float diff = max(dot(norm, lightDir), 0.0);
                vec3 diffuse = diff * lightColor;
                
                vec3 result = (ambient + diffuse) * uColor;
                FragColor = vec4(result, 1.0);
            }
            """;

        int vertexShader = compileShader(GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == GL_FALSE) {
            LOGGER.error("Shader linking failed: {}", glGetProgramInfoLog(shaderProgram));
            throw new RuntimeException("Shader linking failed");
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        // Get uniform locations
        uModelLoc = glGetUniformLocation(shaderProgram, "uModel");
        uViewLoc = glGetUniformLocation(shaderProgram, "uView");
        uProjectionLoc = glGetUniformLocation(shaderProgram, "uProjection");
        uColorLoc = glGetUniformLocation(shaderProgram, "uColor");
    }

    /**
     * Kompiliert einen Shader
     */
    private int compileShader(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            LOGGER.error("Shader compilation failed: {}", glGetShaderInfoLog(shader));
            throw new RuntimeException("Shader compilation failed");
        }

        return shader;
    }

    /**
     * Erstellt die Cube-Geometrie
     */
    private void createCubeMesh() {
        // Cube vertices with normals
        float[] vertices = {
            // Front face
            -0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
             0.5f, -0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
             0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,
            -0.5f,  0.5f,  0.5f,  0.0f,  0.0f,  1.0f,

            // Back face
            -0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
             0.5f, -0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
             0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
            -0.5f,  0.5f, -0.5f,  0.0f,  0.0f, -1.0f,
        };

        // Cube indices
        int[] indices = {
            // Front face
            0, 1, 2, 2, 3, 0,
            // Back face
            4, 5, 6, 6, 7, 4,
            // Left face
            7, 3, 0, 0, 4, 7,
            // Right face
            1, 5, 6, 6, 2, 1,
            // Top face
            3, 2, 6, 6, 7, 3,
            // Bottom face
            0, 1, 5, 5, 4, 0
        };

        // Create VAO, VBO, EBO
        cubeVAO = glGenVertexArrays();
        cubeVBO = glGenBuffers();
        cubeEBO = glGenBuffers();

        glBindVertexArray(cubeVAO);

        // Upload vertex data
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(vertices.length);
        vertexBuffer.put(vertices).flip();

        glBindBuffer(GL_ARRAY_BUFFER, cubeVBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Upload index data
        IntBuffer indexBuffer = BufferUtils.createIntBuffer(indices.length);
        indexBuffer.put(indices).flip();

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, cubeEBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);

        // Setup vertex attributes
        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Normal attribute
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    /**
     * Lädt eine Matrix in einen Uniform
     */
    private void uploadMatrix(int location, Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.get(buffer);
        glUniformMatrix4fv(location, false, buffer);
    }

    /**
     * Generiert einen eindeutigen Schlüssel für einen Chunk
     */
    private String getChunkKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        if (cubeVAO != 0) glDeleteVertexArrays(cubeVAO);
        if (cubeVBO != 0) glDeleteBuffers(cubeVBO);
        if (cubeEBO != 0) glDeleteBuffers(cubeEBO);
        if (shaderProgram != 0) glDeleteProgram(shaderProgram);
    }

    // Getter
    public Camera getCamera() {
        return camera;
    }

    public void setWorldId(String worldId) {
        this.currentWorldId = worldId;
        loadedChunks.clear();
        chunkMeshes.clear();
    }

    /**
     * Gibt die Anzahl der geladenen Chunks zurück
     */
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }

    /**
     * Hilfsklasse für Chunk-Mesh-Daten
     */
    private static class ChunkMesh {
        public int vao;
        public int vbo;
        public int vertexCount;

        public ChunkMesh(int vao, int vbo, int vertexCount) {
            this.vao = vao;
            this.vbo = vbo;
            this.vertexCount = vertexCount;
        }
    }
}
