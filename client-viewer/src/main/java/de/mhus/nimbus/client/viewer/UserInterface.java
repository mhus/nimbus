package de.mhus.nimbus.client.viewer;

import lombok.extern.slf4j.Slf4j;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;

/**
 * Benutzeroberfläche für den Nimbus Client Viewer
 * Implementiert UI-Elemente mit NanoVG
 */
@Slf4j
public class UserInterface {

    private final ViewerWindow viewerWindow;
    private long vg; // NanoVG context
    private int fontNormal;

    // UI State
    private UIState currentState = UIState.CONNECTION_SCREEN;
    private String serverUrl = "ws://localhost:8080/nimbus";
    private String username = "";
    private String password = "";
    private boolean showPassword = false;

    // Messages
    private List<String> messages = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private long lastMessageTime = 0;

    // Window dimensions
    private int windowWidth, windowHeight;

    // Input handling
    private InputField activeInputField = null;

    public enum UIState {
        CONNECTION_SCREEN,
        LOGIN_SCREEN,
        MAIN_SCREEN,
        VOXEL_WORLD_SCREEN
    }

    public enum InputField {
        SERVER_URL,
        USERNAME,
        PASSWORD
    }

    public UserInterface(ViewerWindow viewerWindow) {
        this.viewerWindow = viewerWindow;
    }

    /**
     * Initialisiert die Benutzeroberfläche
     */
    public void init(long window) {
        // Erstelle NanoVG Context
        vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (vg == 0) {
            throw new RuntimeException("Konnte NanoVG Context nicht erstellen");
        }

        // Lade Schriftart (verwende system default)
        fontNormal = nvgCreateFont(vg, "sans", "");
        if (fontNormal == -1) {
            log.warn("Konnte Schriftart nicht laden, verwende Default");
        }

        log.info("UserInterface initialisiert");
    }

    /**
     * Rendert die Benutzeroberfläche
     */
    public void render() {
        // Get window size
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(glfwGetCurrentContext(), pWidth, pHeight);
            windowWidth = pWidth.get(0);
            windowHeight = pHeight.get(0);
        }

        // Begin frame
        nvgBeginFrame(vg, windowWidth, windowHeight, 1.0f);

        // Render based on current state
        switch (currentState) {
            case CONNECTION_SCREEN -> renderConnectionScreen();
            case LOGIN_SCREEN -> renderLoginScreen();
            case MAIN_SCREEN -> renderMainScreen();
            case VOXEL_WORLD_SCREEN -> renderVoxelWorldScreen();
        }

        // Render messages and errors
        renderMessages();

        // End frame
        nvgEndFrame(vg);
    }

    /**
     * Rendert den Verbindungsbildschirm
     */
    private void renderConnectionScreen() {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;

        // Titel
        renderText("Nimbus Client Viewer", centerX, centerY - 120, 32, true);

        // Server URL Input
        renderText("Server URL:", centerX - 150, centerY - 60, 16, false);
        renderInputField(centerX - 150, centerY - 40, 300, 30, serverUrl,
                        activeInputField == InputField.SERVER_URL);

        // Connect Button
        boolean canConnect = !serverUrl.trim().isEmpty() && !viewerWindow.isConnected();
        if (renderButton("Verbinden", centerX - 75, centerY + 20, 150, 40, canConnect)) {
            connectToServer();
        }

        // Disconnect Button (if connected)
        if (viewerWindow.isConnected()) {
            if (renderButton("Trennen", centerX - 75, centerY + 80, 150, 40, true)) {
                viewerWindow.disconnect();
            }
        }

        // Status
        String status = viewerWindow.isConnected() ? "Verbunden" : "Nicht verbunden";
        NVGColor color = NVGColor.create();
        if (viewerWindow.isConnected()) {
            nvgRGBA(0, 255, 0, 255, color);
        } else {
            nvgRGBA(255, 100, 100, 255, color);
        }

        nvgFillColor(vg, color);
        renderText(status, centerX, centerY + 140, 16, true);

        // Weiter zum Login wenn verbunden
        if (viewerWindow.isConnected()) {
            if (renderButton("Zum Login", centerX - 75, centerY + 180, 150, 40, true)) {
                currentState = UIState.LOGIN_SCREEN;
            }
        }
    }

    /**
     * Rendert den Login-Bildschirm
     */
    private void renderLoginScreen() {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;

        // Titel
        renderText("Anmeldung", centerX, centerY - 120, 28, true);

        // Username Input
        renderText("Benutzername:", centerX - 150, centerY - 60, 16, false);
        renderInputField(centerX - 150, centerY - 40, 300, 30, username,
                        activeInputField == InputField.USERNAME);

        // Password Input
        renderText("Passwort:", centerX - 150, centerY, 16, false);
        String displayPassword = showPassword ? password : "*".repeat(password.length());
        renderInputField(centerX - 150, centerY + 20, 300, 30, displayPassword,
                        activeInputField == InputField.PASSWORD);

        // Show Password Checkbox
        if (renderCheckbox("Passwort anzeigen", centerX - 150, centerY + 60, showPassword)) {
            showPassword = !showPassword;
        }

        // Login Button
        boolean canLogin = !username.trim().isEmpty() && !password.trim().isEmpty()
                          && !viewerWindow.isAuthenticated();
        if (renderButton("Anmelden", centerX - 75, centerY + 100, 150, 40, canLogin)) {
            authenticate();
        }

        // Back Button
        if (renderButton("Zurück", centerX - 200, centerY + 100, 100, 40, true)) {
            currentState = UIState.CONNECTION_SCREEN;
        }

        // Status
        String status = viewerWindow.isAuthenticated() ? "Angemeldet" : "Nicht angemeldet";
        NVGColor color = NVGColor.create();
        if (viewerWindow.isAuthenticated()) {
            nvgRGBA(0, 255, 0, 255, color);
        } else {
            nvgRGBA(255, 100, 100, 255, color);
        }

        nvgFillColor(vg, color);
        renderText(status, centerX, centerY + 160, 16, true);

        // Weiter zur Hauptansicht wenn angemeldet
        if (viewerWindow.isAuthenticated()) {
            if (renderButton("Voxel Welt", centerX + 75, centerY + 100, 125, 40, true)) {
                enterVoxelWorld();
            }
        }
    }

    /**
     * Rendert die Hauptansicht
     */
    private void renderMainScreen() {
        // Header
        renderText("Nimbus Client - Hauptansicht", windowWidth / 2.0f, 40, 24, true);

        // Connection Info
        String info = String.format("Verbunden mit: %s | Benutzer: %s",
                                   viewerWindow.getClientService().getServerUrl(), username);
        renderText(info, 20, 80, 14, false);

        // Function Buttons
        float buttonY = 120;
        if (renderButton("Test Funktion", 20, buttonY, 150, 40, true)) {
            testFunction();
        }

        if (renderButton("Server Info", 180, buttonY, 150, 40, true)) {
            requestServerInfo();
        }

        if (renderButton("Abmelden", windowWidth - 170, 20, 150, 40, true)) {
            logout();
        }

        // Message Area
        renderMessageArea();
    }

    /**
     * Rendert einen Text
     */
    private void renderText(String text, float x, float y, float size, boolean centered) {
        nvgFontSize(vg, size);
        nvgFontFace(vg, "sans");

        if (centered) {
            nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        } else {
            nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_TOP);
        }

        NVGColor color = NVGColor.create();
        nvgRGBA(255, 255, 255, 255, color);
        nvgFillColor(vg, color);

        nvgText(vg, x, y, text);
    }

    /**
     * Rendert ein Eingabefeld
     */
    private void renderInputField(float x, float y, float width, float height,
                                 String text, boolean active) {
        NVGColor color = NVGColor.create();

        // Background
        if (active) {
            nvgRGBA(60, 60, 80, 255, color);
        } else {
            nvgRGBA(40, 40, 50, 255, color);
        }
        nvgFillColor(vg, color);
        nvgBeginPath(vg);
        nvgRect(vg, x, y, width, height);
        nvgFill(vg);

        // Border
        if (active) {
            nvgRGBA(100, 150, 255, 255, color);
        } else {
            nvgRGBA(100, 100, 100, 255, color);
        }
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);

        // Text
        nvgRGBA(255, 255, 255, 255, color);
        nvgFillColor(vg, color);
        nvgFontSize(vg, 14);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(vg, x + 8, y + height / 2, text);

        // Cursor
        if (active && System.currentTimeMillis() % 1000 < 500) {
            nvgBeginPath(vg);
            float textWidth = nvgTextBounds(vg, 0, 0, text, null);
            nvgRect(vg, x + 8 + textWidth, y + 4, 1, height - 8);
            nvgFill(vg);
        }
    }

    /**
     * Rendert einen Button
     */
    private boolean renderButton(String text, float x, float y, float width, float height, boolean enabled) {
        NVGColor color = NVGColor.create();

        // Maus-Position für Hover-Effekt
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        glfwGetCursorPos(glfwGetCurrentContext(), mouseX, mouseY);

        boolean hover = enabled && mouseX[0] >= x && mouseX[0] <= x + width &&
                       mouseY[0] >= y && mouseY[0] <= y + height;

        // Background
        if (!enabled) {
            nvgRGBA(60, 60, 60, 255, color);
        } else if (hover) {
            nvgRGBA(80, 120, 200, 255, color);
        } else {
            nvgRGBA(70, 110, 180, 255, color);
        }
        nvgFillColor(vg, color);
        nvgBeginPath(vg);
        nvgRoundedRect(vg, x, y, width, height, 4);
        nvgFill(vg);

        // Text
        if (enabled) {
            nvgRGBA(255, 255, 255, 255, color);
        } else {
            nvgRGBA(150, 150, 150, 255, color);
        }
        nvgFillColor(vg, color);
        nvgFontSize(vg, 14);
        nvgTextAlign(vg, NVG_ALIGN_CENTER | NVG_ALIGN_MIDDLE);
        nvgText(vg, x + width / 2, y + height / 2, text);

        return hover && glfwGetMouseButton(glfwGetCurrentContext(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    /**
     * Rendert eine Checkbox
     */
    private boolean renderCheckbox(String text, float x, float y, boolean checked) {
        float size = 16;
        NVGColor color = NVGColor.create();

        // Checkbox Background
        nvgRGBA(40, 40, 50, 255, color);
        nvgFillColor(vg, color);
        nvgBeginPath(vg);
        nvgRect(vg, x, y, size, size);
        nvgFill(vg);

        // Border
        nvgRGBA(100, 100, 100, 255, color);
        nvgStrokeColor(vg, color);
        nvgStrokeWidth(vg, 1.0f);
        nvgStroke(vg);

        // Check mark
        if (checked) {
            nvgRGBA(0, 255, 0, 255, color);
            nvgFillColor(vg, color);
            nvgBeginPath(vg);
            nvgRect(vg, x + 3, y + 3, size - 6, size - 6);
            nvgFill(vg);
        }

        // Text
        nvgRGBA(255, 255, 255, 255, color);
        nvgFillColor(vg, color);
        nvgFontSize(vg, 14);
        nvgTextAlign(vg, NVG_ALIGN_LEFT | NVG_ALIGN_MIDDLE);
        nvgText(vg, x + size + 8, y + size / 2, text);

        // Check for click
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        glfwGetCursorPos(glfwGetCurrentContext(), mouseX, mouseY);

        boolean hover = mouseX[0] >= x && mouseX[0] <= x + size &&
                       mouseY[0] >= y && mouseY[0] <= y + size;

        return hover && glfwGetMouseButton(glfwGetCurrentContext(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
    }

    /**
     * Rendert den Nachrichtenbereich
     */
    private void renderMessageArea() {
        float startY = 200;
        float lineHeight = 20;

        renderText("Nachrichten:", 20, startY, 16, false);

        int maxMessages = Math.min(messages.size(), 15);
        for (int i = 0; i < maxMessages; i++) {
            String message = messages.get(messages.size() - maxMessages + i);
            renderText("• " + message, 20, startY + 30 + i * lineHeight, 12, false);
        }
    }

    /**
     * Rendert Nachrichten und Fehlermeldungen
     */
    private void renderMessages() {
        // Fade out messages after 5 seconds
        if (System.currentTimeMillis() - lastMessageTime > 5000) {
            if (!messages.isEmpty()) {
                messages.clear();
            }
            if (!errors.isEmpty()) {
                errors.clear();
            }
        }
    }

    /**
     * Rendert die 3D-Voxel-Welt-Ansicht
     */
    private void renderVoxelWorldScreen() {
        // In diesem Modus wird hauptsächlich die 3D-Welt gerendert
        // Die UI wird vom VoxelWorldCanvas übernommen
        if (viewerWindow.getVoxelWorldCanvas() != null) {
            viewerWindow.getVoxelWorldCanvas().update();
            viewerWindow.getVoxelWorldCanvas().render();
        }
    }

    /**
     * Wechselt zur 3D-Voxel-Welt nach erfolgreicher Anmeldung
     */
    private void enterVoxelWorld() {
        log.info("Wechsle zur 3D-Voxel-Welt...");

        // Initialisiere 3D-Canvas wenn noch nicht geschehen
        viewerWindow.initializeVoxelWorld();

        // Wechsle zur Voxel-Welt-Ansicht
        currentState = UIState.VOXEL_WORLD_SCREEN;

        // Deaktiviere aktive Eingabefelder
        activeInputField = null;

        showMessage("3D-Voxel-Welt geladen - WASD zum Bewegen, ESC für Maus");
    }

    // Event Handler
    public void handleKeyInput(int key, int scancode, int action, int mods) {
        // In der Voxel-Welt: Events an den 3D-Canvas weiterleiten
        if (currentState == UIState.VOXEL_WORLD_SCREEN && viewerWindow.getVoxelWorldCanvas() != null) {
            viewerWindow.getVoxelWorldCanvas().handleKeyInput(key, scancode, action, mods);
            return;
        }

        if (action == GLFW_PRESS || action == GLFW_REPEAT) {
            if (activeInputField != null) {
                handleTextInput(key, mods);
            } else {
                // Navigation keys
                if (key == GLFW_KEY_TAB) {
                    switchToNextInputField();
                }
            }
        }
    }

    private void handleTextInput(int key, int mods) {
        String currentText = getCurrentInputText();

        if (key == GLFW_KEY_BACKSPACE && !currentText.isEmpty()) {
            setCurrentInputText(currentText.substring(0, currentText.length() - 1));
        } else if (key == GLFW_KEY_ENTER) {
            handleEnterKey();
        } else if (key >= 32 && key <= 126) { // Printable characters
            char ch = (char) key;
            if ((mods & GLFW_MOD_SHIFT) == 0 && ch >= 'A' && ch <= 'Z') {
                ch = (char) (ch + 32); // Convert to lowercase
            }
            setCurrentInputText(currentText + ch);
        }
    }

    private String getCurrentInputText() {
        return switch (activeInputField) {
            case SERVER_URL -> serverUrl;
            case USERNAME -> username;
            case PASSWORD -> password;
            default -> "";
        };
    }

    private void setCurrentInputText(String text) {
        switch (activeInputField) {
            case SERVER_URL -> serverUrl = text;
            case USERNAME -> username = text;
            case PASSWORD -> password = text;
        }
    }

    private void switchToNextInputField() {
        if (currentState == UIState.CONNECTION_SCREEN) {
            activeInputField = InputField.SERVER_URL;
        } else if (currentState == UIState.LOGIN_SCREEN) {
            if (activeInputField == null || activeInputField == InputField.PASSWORD) {
                activeInputField = InputField.USERNAME;
            } else {
                activeInputField = InputField.PASSWORD;
            }
        }
    }

    private void handleEnterKey() {
        switch (currentState) {
            case CONNECTION_SCREEN -> {
                if (activeInputField == InputField.SERVER_URL) {
                    connectToServer();
                }
            }
            case LOGIN_SCREEN -> {
                if (activeInputField == InputField.PASSWORD) {
                    authenticate();
                }
            }
        }
    }

    public void handleMouseButton(long window, int button, int action, int mods) {
        // In der Voxel-Welt: Events an den 3D-Canvas weiterleiten
        if (currentState == UIState.VOXEL_WORLD_SCREEN && viewerWindow.getVoxelWorldCanvas() != null) {
            viewerWindow.getVoxelWorldCanvas().handleMouseButton(window, button, action, mods);
            return;
        }

        if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
            // Handle input field selection
            double[] mouseX = new double[1];
            double[] mouseY = new double[1];
            glfwGetCursorPos(window, mouseX, mouseY);

            // Check which input field is clicked
            activeInputField = getClickedInputField((float) mouseX[0], (float) mouseY[0]);
        }
    }

    private InputField getClickedInputField(float mouseX, float mouseY) {
        float centerX = windowWidth / 2.0f;
        float centerY = windowHeight / 2.0f;

        if (currentState == UIState.CONNECTION_SCREEN) {
            // Server URL field
            if (mouseX >= centerX - 150 && mouseX <= centerX + 150 &&
                mouseY >= centerY - 40 && mouseY <= centerY - 10) {
                return InputField.SERVER_URL;
            }
        } else if (currentState == UIState.LOGIN_SCREEN) {
            // Username field
            if (mouseX >= centerX - 150 && mouseX <= centerX + 150 &&
                mouseY >= centerY - 40 && mouseY <= centerY - 10) {
                return InputField.USERNAME;
            }
            // Password field
            if (mouseX >= centerX - 150 && mouseX <= centerX + 150 &&
                mouseY >= centerY + 20 && mouseY <= centerY + 50) {
                return InputField.PASSWORD;
            }
        }

        return null;
    }

    public void handleMouseMove(long window, double xpos, double ypos) {
        // In der Voxel-Welt: Events an den 3D-Canvas weiterleiten
        if (currentState == UIState.VOXEL_WORLD_SCREEN && viewerWindow.getVoxelWorldCanvas() != null) {
            viewerWindow.getVoxelWorldCanvas().handleMouseMove(window, xpos, ypos);
            return;
        }

        // Currently not used for UI, but could be used for hover effects
    }

    /**
     * Verarbeitet Mausrad-Ereignisse
     */
    public void handleMouseScroll(long window, double xoffset, double yoffset) {
        // In der Voxel-Welt: Events an den 3D-Canvas weiterleiten
        if (currentState == UIState.VOXEL_WORLD_SCREEN && viewerWindow.getVoxelWorldCanvas() != null) {
            viewerWindow.getVoxelWorldCanvas().handleMouseScroll(window, xoffset, yoffset);
        }
    }

    /**
     * Gibt den NanoVG-Context zurück
     */
    public long getVgContext() {
        return vg;
    }

    // Actions
    private void connectToServer() {
        viewerWindow.connectToServer(serverUrl.trim());
    }

    private void authenticate() {
        viewerWindow.authenticate(username.trim(), password);
    }

    private void logout() {
        username = "";
        password = "";
        currentState = UIState.LOGIN_SCREEN;
    }

    private void testFunction() {
        viewerWindow.callFunction("test", "Hello from Viewer!")
            .thenAccept(response -> showMessage("Test Funktion ausgeführt: " + response))
            .exceptionally(ex -> {
                showError("Fehler bei Test Funktion: " + ex.getMessage());
                return null;
            });
    }

    private void requestServerInfo() {
        viewerWindow.callFunction("getServerInfo", null)
            .thenAccept(response -> showMessage("Server Info: " + response))
            .exceptionally(ex -> {
                showError("Fehler beim Abrufen der Server Info: " + ex.getMessage());
                return null;
            });
    }

    // Status callbacks
    public void onConnectionStatusChanged(boolean connected) {
        if (connected) {
            showMessage("Verbindung hergestellt");
        } else {
            showMessage("Verbindung getrennt");
            if (currentState != UIState.CONNECTION_SCREEN) {
                currentState = UIState.CONNECTION_SCREEN;
            }
        }
    }

    public void onAuthenticationStatusChanged(boolean authenticated) {
        if (authenticated) {
            showMessage("Anmeldung erfolgreich");
        } else {
            showMessage("Abgemeldet");
            if (currentState == UIState.MAIN_SCREEN) {
                currentState = UIState.LOGIN_SCREEN;
            }
        }
    }

    public void showMessage(String message) {
        messages.add(message);
        lastMessageTime = System.currentTimeMillis();
        log.info("UI Message: {}", message);
    }

    public void showError(String error) {
        errors.add(error);
        lastMessageTime = System.currentTimeMillis();
        log.error("UI Error: {}", error);
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        if (vg != 0) {
            NanoVGGL3.nvgDelete(vg);
        }
        log.info("UserInterface cleanup abgeschlossen");
    }
}
