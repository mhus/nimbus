package de.mhus.nimbus.client.viewer.render;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Kamera-Klasse für 3D-Navigation in der Voxel-Welt
 * Unterstützt WASD-Bewegung und Maus-Look
 */
@Slf4j
@Getter
public class Camera {

    // Kamera-Position und Orientierung
    private final Vector3f position = new Vector3f(0.0f, 64.0f, 0.0f);
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f);
    private final Vector3f worldUp = new Vector3f(0.0f, 1.0f, 0.0f);

    // Euler-Winkel
    private float yaw = -90.0f;   // Links/Rechts
    private float pitch = 0.0f;   // Auf/Ab

    // Kamera-Optionen
    private float movementSpeed = 10.0f;      // Blöcke pro Sekunde
    private float mouseSensitivity = 0.1f;
    private float zoom = 45.0f;

    // Bewegungs-Flags
    private boolean moveForward = false;
    private boolean moveBackward = false;
    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;
    private boolean fastMode = false;

    // Maus-Handling
    private boolean firstMouse = true;
    private float lastX = 400;
    private float lastY = 300;

    /**
     * Aktualisiert die Kamera-Vektoren basierend auf den Euler-Winkeln
     */
    public void updateCameraVectors() {
        // Berechne neuen Front-Vektor
        Vector3f newFront = new Vector3f();
        newFront.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        newFront.y = (float) Math.sin(Math.toRadians(pitch));
        newFront.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

        front.set(newFront).normalize();

        // Berechne Right- und Up-Vektoren
        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }

    /**
     * Aktualisiert die View-Matrix
     */
    public void updateViewMatrix(Matrix4f viewMatrix) {
        Vector3f center = new Vector3f(position).add(front);
        viewMatrix.setLookAt(position.x, position.y, position.z,
                            center.x, center.y, center.z,
                            up.x, up.y, up.z);
    }

    /**
     * Update-Methode für jeden Frame
     */
    public void update(float deltaTime) {
        float velocity = movementSpeed * deltaTime;

        // Schneller Modus
        if (fastMode) {
            velocity *= 5.0f;
        }

        // Bewegung verarbeiten
        if (moveForward) {
            Vector3f movement = new Vector3f(front).mul(velocity);
            position.add(movement);
        }
        if (moveBackward) {
            Vector3f movement = new Vector3f(front).mul(-velocity);
            position.add(movement);
        }
        if (moveLeft) {
            Vector3f movement = new Vector3f(right).mul(-velocity);
            position.add(movement);
        }
        if (moveRight) {
            Vector3f movement = new Vector3f(right).mul(velocity);
            position.add(movement);
        }
        if (moveUp) {
            Vector3f movement = new Vector3f(worldUp).mul(velocity);
            position.add(movement);
        }
        if (moveDown) {
            Vector3f movement = new Vector3f(worldUp).mul(-velocity);
            position.add(movement);
        }

        // Verhindere, dass die Kamera zu tief unter die Welt fällt
        if (position.y < -10.0f) {
            position.y = -10.0f;
        }
    }

    /**
     * Verarbeitet Tastatureingaben
     */
    public void processKeyboard(int key, int action) {
        boolean pressed = (action == GLFW_PRESS || action == GLFW_REPEAT);

        switch (key) {
            case GLFW_KEY_W -> moveForward = pressed;
            case GLFW_KEY_S -> moveBackward = pressed;
            case GLFW_KEY_A -> moveLeft = pressed;
            case GLFW_KEY_D -> moveRight = pressed;
            case GLFW_KEY_SPACE -> moveUp = pressed;
            case GLFW_KEY_LEFT_SHIFT -> moveDown = pressed;
            case GLFW_KEY_LEFT_CONTROL -> fastMode = pressed;
        }
    }

    /**
     * Verarbeitet Mausbewegungen
     */
    public void processMouseMovement(float xpos, float ypos, boolean constrainPitch) {
        if (firstMouse) {
            lastX = xpos;
            lastY = ypos;
            firstMouse = false;
        }

        float xoffset = xpos - lastX;
        float yoffset = lastY - ypos; // Y-Koordinaten sind umgekehrt
        lastX = xpos;
        lastY = ypos;

        xoffset *= mouseSensitivity;
        yoffset *= mouseSensitivity;

        yaw += xoffset;
        pitch += yoffset;

        // Pitch-Einschränkung
        if (constrainPitch) {
            if (pitch > 89.0f) pitch = 89.0f;
            if (pitch < -89.0f) pitch = -89.0f;
        }

        // Kamera-Vektoren aktualisieren
        updateCameraVectors();
    }

    /**
     * Verarbeitet Mausrad-Scrolling für Zoom
     */
    public void processMouseScroll(float yoffset) {
        zoom -= yoffset;
        if (zoom < 1.0f) zoom = 1.0f;
        if (zoom > 45.0f) zoom = 45.0f;
    }

    /**
     * Setzt die Kamera-Position
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }

    /**
     * Setzt die Kamera-Position auf einen sicheren Spawn-Punkt
     */
    public void setSpawnPosition() {
        position.set(0.0f, 70.0f, 0.0f);
        yaw = -90.0f;
        pitch = -20.0f;
        updateCameraVectors();
        log.info("Kamera auf Spawn-Position gesetzt: {}", position);
    }

    /**
     * Teleportiert die Kamera zu einer bestimmten Position
     */
    public void teleport(float x, float y, float z) {
        setPosition(x, y, z);
        log.info("Kamera teleportiert zu: {}", position);
    }

    /**
     * Gibt die aktuelle Chunk-Position der Kamera zurück
     */
    public Vector3f getChunkPosition() {
        return new Vector3f(
            (int) Math.floor(position.x / 16.0f),
            (int) Math.floor(position.y / 16.0f),
            (int) Math.floor(position.z / 16.0f)
        );
    }

    /**
     * Prüft, ob sich die Kamera bewegt
     */
    public boolean isMoving() {
        return moveForward || moveBackward || moveLeft || moveRight || moveUp || moveDown;
    }

    /**
     * Gibt Debug-Informationen über die Kamera aus
     */
    public String getDebugInfo() {
        return String.format("Pos: (%.1f, %.1f, %.1f) | Yaw: %.1f° | Pitch: %.1f° | Chunk: %s",
                           position.x, position.y, position.z,
                           yaw, pitch,
                           getChunkPosition().toString());
    }

    // Setter für Kamera-Optionen
    public void setMovementSpeed(float speed) {
        this.movementSpeed = Math.max(0.1f, speed);
    }

    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.01f, Math.min(1.0f, sensitivity));
    }
}
