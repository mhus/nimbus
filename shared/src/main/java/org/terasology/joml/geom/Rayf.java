// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

/**
 * Represents a ray in 3D space with an origin and direction.
 */
public class Rayf {

    /**
     * The x coordinate of the ray origin.
     */
    public float oX;

    /**
     * The y coordinate of the ray origin.
     */
    public float oY;

    /**
     * The z coordinate of the ray origin.
     */
    public float oZ;

    /**
     * The x component of the ray direction.
     */
    public float dX;

    /**
     * The y component of the ray direction.
     */
    public float dY;

    /**
     * The z component of the ray direction.
     */
    public float dZ;

    /**
     * Create a new ray at origin pointing in positive z direction.
     */
    public Rayf() {
        this.dZ = 1.0f;
    }

    /**
     * Create a new ray with the given origin and direction.
     *
     * @param oX the x coordinate of the origin
     * @param oY the y coordinate of the origin
     * @param oZ the z coordinate of the origin
     * @param dX the x component of the direction
     * @param dY the y component of the direction
     * @param dZ the z component of the direction
     */
    public Rayf(float oX, float oY, float oZ, float dX, float dY, float dZ) {
        this.oX = oX;
        this.oY = oY;
        this.oZ = oZ;
        this.dX = dX;
        this.dY = dY;
        this.dZ = dZ;
    }
}
