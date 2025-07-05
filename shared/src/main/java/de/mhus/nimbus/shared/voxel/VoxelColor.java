package de.mhus.nimbus.shared.voxel;

import java.io.Serializable;

/**
 * Represents RGBA color for voxel rendering
 */
public class VoxelColor implements Serializable {

    private static final long serialVersionUID = 1L;

    private float red;
    private float green;
    private float blue;
    private float alpha;

    public VoxelColor() {
        this(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public VoxelColor(float red, float green, float blue, float alpha) {
        this.red = Math.max(0.0f, Math.min(1.0f, red));
        this.green = Math.max(0.0f, Math.min(1.0f, green));
        this.blue = Math.max(0.0f, Math.min(1.0f, blue));
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public float getRed() {
        return red;
    }

    public void setRed(float red) {
        this.red = Math.max(0.0f, Math.min(1.0f, red));
    }

    public float getGreen() {
        return green;
    }

    public void setGreen(float green) {
        this.green = Math.max(0.0f, Math.min(1.0f, green));
    }

    public float getBlue() {
        return blue;
    }

    public void setBlue(float blue) {
        this.blue = Math.max(0.0f, Math.min(1.0f, blue));
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public int toRGBA() {
        int r = (int) (red * 255);
        int g = (int) (green * 255);
        int b = (int) (blue * 255);
        int a = (int) (alpha * 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static VoxelColor fromRGBA(int rgba) {
        float a = ((rgba >> 24) & 0xFF) / 255.0f;
        float r = ((rgba >> 16) & 0xFF) / 255.0f;
        float g = ((rgba >> 8) & 0xFF) / 255.0f;
        float b = (rgba & 0xFF) / 255.0f;
        return new VoxelColor(r, g, b, a);
    }
}
