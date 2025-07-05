package de.mhus.nimbus.shared.voxel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents RGBA color for voxel rendering
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoxelColor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private float red = 1.0f;
    private float green = 1.0f;
    private float blue = 1.0f;
    private float alpha = 1.0f;

    public void setRed(float red) {
        this.red = Math.max(0.0f, Math.min(1.0f, red));
    }

    public void setGreen(float green) {
        this.green = Math.max(0.0f, Math.min(1.0f, green));
    }

    public void setBlue(float blue) {
        this.blue = Math.max(0.0f, Math.min(1.0f, blue));
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
