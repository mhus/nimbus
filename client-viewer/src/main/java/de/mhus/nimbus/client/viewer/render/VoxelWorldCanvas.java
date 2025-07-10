package de.mhus.nimbus.client.viewer.render;

import de.mhus.nimbus.client.viewer.ViewerWindow;
import de.mhus.nimbus.common.client.WorldVoxelClient;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * 3D-Voxel-Welt-Canvas für das client-viewer Modul
 * Kombiniert 3D-Voxel-Rendering mit 2D-UI-Overlays
 */
@Slf4j
public class VoxelWorldCanvas {

    private final ViewerWindow viewerWindow;
    private final WorldVoxelClient worldVoxelClient;
    private final VoxelWorldRenderer voxelRenderer;
    private final long vgContext;

    // UI State
    private boolean showDebugInfo = true;
    private boolean showHelp = false;
    private boolean mouseCaptured = true;
    private long lastFrameTime = System.nanoTime();
    private float frameTime = 0.0f;
    private int fps = 0;
    private int frameCount = 0;
    private long lastFpsTime = System.currentTimeMillis();

    // Window dimensions
    private int windowWidth, windowHeight;

    public VoxelWorldCanvas(ViewerWindow viewerWindow, WorldVoxelClient worldVoxelClient, long vgContext) {
        this.viewerWindow = viewerWindow;
        this.worldVoxelClient = worldVoxelClient;
        this.vgContext = vgContext;
        this.voxelRenderer = new VoxelWorldRenderer();
    }

    /**
     * Initialisiert den 3D-Canvas
     */
    public void init(long window) {
        log.info("Initialisiere VoxelWorldCanvas...");

        // Get initial window size
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);
            windowWidth = pWidth.get(0);
            windowHeight = pHeight.get(0);
        }

        // Initialize 3D renderer
        voxelRenderer.init(windowWidth, windowHeight);

        // Setup mouse capture
        if (mouseCaptured) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        }

        // Set camera to spawn position
        voxelRenderer.getCamera().setSpawnPosition();

        log.info("VoxelWorldCanvas initialisiert");
    }

    /**
     * Rendert den 3D-Canvas mit UI-Overlay
     */
    public void render() {
        // Update timing
        updateTiming();

        // Get current window size
        long window = glfwGetCurrentContext();
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(window, pWidth, pHeight);

            if (pWidth.get(0) != windowWidth || pHeight.get(0) != windowHeight) {
                windowWidth = pWidth.get(0);
                windowHeight = pHeight.get(0);
                voxelRenderer.updateProjection(windowWidth, windowHeight);
            }
        }

        // Render 3D world
        voxelRenderer.render();

        // Render 2D UI overlay
        renderUIOverlay();
    }

    /**
     * Update-Methode für jeden Frame
     */
    public void update() {
        voxelRenderer.update(frameTime);
    }

    /**
     * Rendert das 2D-UI-Overlay über die 3D-Welt
     */
    private void renderUIOverlay() {
        // Disable depth testing for UI
        glDisable(GL_DEPTH_TEST);

        // Begin NanoVG frame
        nvgBeginFrame(vgContext, windowWidth, windowHeight, 1.0f);

        // Render crosshair
        renderCrosshair();

        // Render debug info
        if (showDebugInfo) {
            renderDebugInfo();
        }

        // Render help
        if (showHelp) {
            renderHelp();
        }

        // Render connection status
        renderConnectionStatus();

        // End NanoVG frame
        nvgEndFrame(vgContext);

        // Re-enable depth testing
        glEnable(GL_DEPTH_TEST);
    }

    /**
     * Rendert das Fadenkreuz in der Bildschirmmitte
     */
    private void renderCrosshair() {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;
        float size = 10.0f;

        NVGColor color = NVGColor.create();
        nvgRGBA((byte)255, (byte)255, (byte)255, (byte)200, color);
        nvgStrokeColor(vgContext, color);
        nvgStrokeWidth(vgContext, 2.0f);

        // Horizontale Linie
        nvgBeginPath(vgContext);
        nvgMoveTo(vgContext, centerX - size, centerY);
        nvgLineTo(vgContext, centerX + size, centerY);
        nvgStroke(vgContext);

        // Vertikale Linie
        nvgBeginPath(vgContext);
        nvgMoveTo(vgContext, centerX, centerY - size);
        nvgLineTo(vgContext, centerX, centerY + size);
        nvgStroke(vgContext);
    }

    /**
     * Rendert Debug-Informationen
     */
    private void renderDebugInfo() {
        Camera camera = voxelRenderer.getCamera();

        String[] debugLines = {
            String.format("FPS: %d (%.1fms)", fps, frameTime * 1000),
            camera.getDebugInfo(),
            String.format("Verbunden: %s", viewerWindow.isConnected() ? "Ja" : "Nein"),
            String.format("Chunks geladen: %d", voxelRenderer.getLoadedChunkCount()),
            String.format("Maus erfasst: %s", mouseCaptured ? "Ja" : "Nein"),
            "F1: Hilfe | F3: Debug Info | ESC: Maus freigeben"
        };

        renderTextBlock(debugLines, 10, 10, 12, false);
    }

    /**
     * Rendert die Hilfe-Anzeige
     */
    private void renderHelp() {
        String[] helpLines = {
            "=== NIMBUS VOXEL WORLD VIEWER ===",
            "",
            "BEWEGUNG:",
            "W/S/A/D - Vorwärts/Rückwärts/Links/Rechts",
            "Leertaste - Nach oben",
            "Shift - Nach unten",
            "Strg - Schneller Modus",
            "",
            "KAMERA:",
            "Maus - Umschauen",
            "Mausrad - Zoom",
            "",
            "INTERFACE:",
            "F1 - Diese Hilfe ein/aus",
            "F3 - Debug-Info ein/aus",
            "ESC - Maus freigeben/erfassen",
            "",
            "Drücke F1 zum Schließen"
        };

        // Semi-transparenter Hintergrund
        NVGColor bgColor = NVGColor.create();
        nvgRGBA((byte)0, (byte)0, (byte)0, (byte)150, bgColor);
        nvgFillColor(vgContext, bgColor);
        nvgBeginPath(vgContext);
        nvgRect(vgContext, windowWidth / 2.0f - 200, windowHeight / 2.0f - 150, 400, 300);
        nvgFill(vgContext);

        renderTextBlock(helpLines, windowWidth / 2.0f - 180, windowHeight / 2.0f - 130, 14, false);
    }

    /**
     * Rendert den Verbindungsstatus
     */
    private void renderConnectionStatus() {
        String status;
        NVGColor color = NVGColor.create();

        if (!viewerWindow.isConnected()) {
            status = "Nicht verbunden";
            nvgRGBA((byte)255, (byte)100, (byte)100, (byte)255, color);
        } else if (!viewerWindow.isAuthenticated()) {
            status = "Nicht angemeldet";
            nvgRGBA((byte)255, (byte)200, (byte)100, (byte)255, color);
        } else {
            status = "Verbunden";
            nvgRGBA((byte)100, (byte)255, (byte)100, (byte)255, color);
        }

        nvgFillColor(vgContext, color);
        nvgFontSize(vgContext, 14);
        nvgTextAlign(vgContext, NVG_ALIGN_RIGHT | NVG_ALIGN_TOP);
        nvgText(vgContext, windowWidth - 10, 10, status);
    }

    /**
     * Rendert einen Textblock
     */
    private void renderTextBlock(String[] lines, float x, float y, float fontSize, boolean centered) {
        nvgFontSize(vgContext, fontSize);
        nvgFontFace(vgContext, "sans");

        if (centered) {
            nvgTextAlign(vgContext, NVG_ALIGN_CENTER | NVG_ALIGN_TOP);
        } else {
            nvgTextAlign(vgContext, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        }

        NVGColor color = NVGColor.create();
        nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, color);
        nvgFillColor(vgContext, color);

        float lineHeight = fontSize * 1.2f;
        for (int i = 0; i < lines.length; i++) {
            nvgText(vgContext, x, y + i * lineHeight, lines[i]);
        }
    }

    /**
     * Verarbeitet Tastatureingaben
     */
    public void handleKeyInput(int key, int scancode, int action, int mods) {
        // Toggle-Funktionen
        if (action == GLFW_PRESS) {
            switch (key) {
                case GLFW_KEY_F1 -> showHelp = !showHelp;
                case GLFW_KEY_F3 -> showDebugInfo = !showDebugInfo;
                case GLFW_KEY_ESCAPE -> toggleMouseCapture();
                case GLFW_KEY_R -> {
                    // Reset camera position
                    voxelRenderer.getCamera().setSpawnPosition();
                    log.info("Kamera-Position zurückgesetzt");
                }
                case GLFW_KEY_T -> {
                    // Test teleport
                    voxelRenderer.getCamera().teleport(100, 80, 100);
                }
            }
        }

        // Weiterleitung an Kamera (nur wenn Maus erfasst ist)
        if (mouseCaptured) {
            voxelRenderer.getCamera().processKeyboard(key, action);
        }
    }

    /**
     * Verarbeitet Mausbewegungen
     */
    public void handleMouseMove(long window, double xpos, double ypos) {
        if (mouseCaptured) {
            voxelRenderer.getCamera().processMouseMovement((float) xpos, (float) ypos, true);
        }
    }

    /**
     * Verarbeitet Mausrad-Eingaben
     */
    public void handleMouseScroll(long window, double xoffset, double yoffset) {
        if (mouseCaptured) {
            voxelRenderer.getCamera().processMouseScroll((float) yoffset);
        }
    }

    /**
     * Verarbeitet Mausklicks
     */
    public void handleMouseButton(long window, int button, int action, int mods) {
        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS && !mouseCaptured) {
            // Erfasse Maus wenn angeklickt
            toggleMouseCapture();
        }
    }

    /**
     * Wechselt zwischen Maus-Erfassung und freier Maus
     */
    private void toggleMouseCapture() {
        long window = glfwGetCurrentContext();
        mouseCaptured = !mouseCaptured;

        if (mouseCaptured) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            // Camera mouse handling will be reset automatically on next movement
            // voxelRenderer.getCamera().resetMousePosition();
            log.info("Maus erfasst - WASD zum Bewegen, ESC zum Freigeben");
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
            log.info("Maus freigegeben - Klicken zum Erfassen");
        }
    }

    /**
     * Aktualisiert Timing-Informationen
     */
    private void updateTiming() {
        long currentTime = System.nanoTime();
        frameTime = (currentTime - lastFrameTime) / 1_000_000_000.0f;
        lastFrameTime = currentTime;

        frameCount++;
        if (System.currentTimeMillis() - lastFpsTime >= 1000) {
            fps = frameCount;
            frameCount = 0;
            lastFpsTime = System.currentTimeMillis();
        }
    }

    /**
     * Setzt die Welt-ID für das Laden von Chunks
     */
    public void setWorldId(String worldId) {
        voxelRenderer.setWorldId(worldId);
        log.info("Welt-ID gesetzt: {}", worldId);
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        voxelRenderer.cleanup();
        log.info("VoxelWorldCanvas cleanup abgeschlossen");
    }

    // Getter
    public Camera getCamera() {
        return voxelRenderer.getCamera();
    }

    public VoxelWorldRenderer getRenderer() {
        return voxelRenderer;
    }

    public boolean isMouseCaptured() {
        return mouseCaptured;
    }
}
