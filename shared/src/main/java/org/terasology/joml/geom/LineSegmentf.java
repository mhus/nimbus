// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

/**
 * Represents a line segment in 3D space between two points.
 */
public class LineSegmentf {

    /**
     * The x coordinate of the first point.
     */
    public float aX;

    /**
     * The y coordinate of the first point.
     */
    public float aY;

    /**
     * The z coordinate of the first point.
     */
    public float aZ;

    /**
     * The x coordinate of the second point.
     */
    public float bX;

    /**
     * The y coordinate of the second point.
     */
    public float bY;

    /**
     * The z coordinate of the second point.
     */
    public float bZ;

    /**
     * Create a new line segment from origin to (1,0,0).
     */
    public LineSegmentf() {
        this.bX = 1.0f;
    }

    /**
     * Create a new line segment with the given endpoints.
     *
     * @param aX the x coordinate of the first point
     * @param aY the y coordinate of the first point
     * @param aZ the z coordinate of the first point
     * @param bX the x coordinate of the second point
     * @param bY the y coordinate of the second point
     * @param bZ the z coordinate of the second point
     */
    public LineSegmentf(float aX, float aY, float aZ, float bX, float bY, float bZ) {
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
        this.bX = bX;
        this.bY = bY;
        this.bZ = bZ;
    }
}
