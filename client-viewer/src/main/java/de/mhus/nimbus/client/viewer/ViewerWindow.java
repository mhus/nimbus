package de.mhus.nimbus.client.viewer;

import de.mhus.nimbus.client.common.service.NimbusClientService;
import de.mhus.nimbus.client.viewer.render.VoxelWorldCanvas;
import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.concurrent.CompletableFuture;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Hauptfenster des Nimbus Client Viewers
 * Implementiert das grafische Interface mit LWJGL
 */
@Slf4j
public class ViewerWindow {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final String WINDOW_TITLE = "Nimbus Client Viewer";

    private final NimbusClientService clientService;
    private final UserInterface userInterface;
    private final WorldVoxelClient worldVoxelClient;
    private VoxelWorldCanvas voxelWorldCanvas;

    // GLFW Window Handle
    private long window;

    // Verbindungsstatus
    private boolean isConnected = false;
    private boolean isAuthenticated = false;

    public ViewerWindow(NimbusClientService clientService) {
        this.clientService = clientService;
        this.worldVoxelClient = new WorldVoxelClient(null); // TODO: KafkaTemplate injection
        this.userInterface = new UserInterface(this);
    }

    /**
     * Startet die Hauptschleife der Anwendung
     */
    public void run() {
        log.info("Initialisiere LWJGL...");

        init();
        loop();

        // Cleanup
        cleanup();
    }

    /**
     * Initialisiert GLFW und erstellt das Fenster
     */
    private void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(WINDOW_WIDTH, WINDOW_HEIGHT, WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            userInterface.handleKeyInput(key, scancode, action, mods);
        });

        // Setup mouse callbacks
        glfwSetMouseButtonCallback(window, userInterface::handleMouseButton);
        glfwSetCursorPosCallback(window, userInterface::handleMouseMove);
        glfwSetScrollCallback(window, userInterface::handleMouseScroll);

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        GL.createCapabilities();

        // Initialize UI
        userInterface.init(window);

        log.info("LWJGL initialisiert, Fenster erstellt");
    }

    /**
     * Hauptschleife der Anwendung
     */
    private void loop() {
        // Set the clear color
        glClearColor(0.1f, 0.1f, 0.1f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update connection status
            updateConnectionStatus();

            // Render UI
            userInterface.render();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events
            glfwPollEvents();
        }
    }

    /**
     * Aktualisiert den Verbindungsstatus
     */
    private void updateConnectionStatus() {
        boolean newConnectedState = clientService.isConnected();
        boolean newAuthenticatedState = clientService.isAuthenticated();

        if (newConnectedState != isConnected) {
            isConnected = newConnectedState;
            userInterface.onConnectionStatusChanged(isConnected);
        }

        if (newAuthenticatedState != isAuthenticated) {
            isAuthenticated = newAuthenticatedState;
            userInterface.onAuthenticationStatusChanged(isAuthenticated);
        }
    }

    /**
     * Verbindet zum Server
     */
    public CompletableFuture<Void> connectToServer(String serverUrl) {
        log.info("Verbinde zu Server: {}", serverUrl);
        return clientService.connect(serverUrl)
            .thenRun(() -> {
                log.info("Verbindung zum Server hergestellt");
                userInterface.showMessage("Verbunden mit " + serverUrl);
            })
            .exceptionally(ex -> {
                log.error("Fehler beim Verbinden zum Server", ex);
                userInterface.showError("Verbindungsfehler: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Authentifiziert beim Server
     */
    public CompletableFuture<WebSocketMessage> authenticate(String username, String password) {
        log.info("Authentifiziere Benutzer: {}", username);
        return clientService.authenticate(username, password, "Nimbus Client Viewer v1.0")
            .thenApply(response -> {
                log.info("Authentifizierung erfolgreich");
                userInterface.showMessage("Anmeldung erfolgreich");
                return response;
            })
            .exceptionally(ex -> {
                log.error("Fehler bei der Authentifizierung", ex);
                userInterface.showError("Anmeldung fehlgeschlagen: " + ex.getMessage());
                return null;
            });
    }

    /**
     * Trennt die Verbindung zum Server
     */
    public void disconnect() {
        log.info("Trenne Verbindung zum Server");
        clientService.disconnect();
        userInterface.showMessage("Verbindung getrennt");
    }

    /**
     * Sendet eine Nachricht an den Server
     */
    public void sendMessage(String type, Object data) {
        clientService.sendMessage(type, data);
    }

    /**
     * Ruft eine Funktion auf dem Server auf
     */
    public CompletableFuture<WebSocketMessage> callFunction(String functionName, Object parameters) {
        return clientService.callFunction(functionName, parameters);
    }

    /**
     * Initialisiert den 3D-Voxel-Canvas nach erfolgreicher Authentifizierung
     */
    public void initializeVoxelWorld() {
        if (voxelWorldCanvas == null) {
            voxelWorldCanvas = new VoxelWorldCanvas(this, worldVoxelClient, userInterface.getVgContext());
            voxelWorldCanvas.init(window);
            voxelWorldCanvas.setWorldId("default");
            log.info("3D-Voxel-Canvas initialisiert");
        }
    }

    /**
     * Gibt den 3D-Voxel-Canvas zurück
     */
    public VoxelWorldCanvas getVoxelWorldCanvas() {
        return voxelWorldCanvas;
    }

    /**
     * Cleanup-Methode
     */
    private void cleanup() {
        // Disconnect from server
        if (clientService.isConnected()) {
            clientService.disconnect();
        }

        // Cleanup 3D Canvas
        if (voxelWorldCanvas != null) {
            voxelWorldCanvas.cleanup();
        }

        // Cleanup UI
        userInterface.cleanup();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        log.info("LWJGL cleanup abgeschlossen");
    }

    // Getter methods
    public boolean isConnected() {
        return isConnected;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public NimbusClientService getClientService() {
        return clientService;
    }

    public WorldVoxelClient getWorldVoxelClient() {
        return worldVoxelClient;
    }
}
