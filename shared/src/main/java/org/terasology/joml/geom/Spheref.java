// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

/**
 * Represents a sphere in 3D space.
 */
public class Spheref {

    /**
     * The x coordinate of the sphere center.
     */
    public float x;

    /**
     * The y coordinate of the sphere center.
     */
    public float y;

    /**
     * The z coordinate of the sphere center.
     */
    public float z;

    /**
     * The radius of the sphere.
     */
    public float r;

    /**
     * Create a new sphere at origin with radius 1.
     */
    public Spheref() {
        this.r = 1.0f;
    }

    /**
     * Create a new sphere with the given center and radius.
     *
     * @param x the x coordinate of the center
     * @param y the y coordinate of the center
     * @param z the z coordinate of the center
     * @param r the radius
     */
    public Spheref(float x, float y, float z, float r) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
    }
}
