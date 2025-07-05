// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

/**
 * Represents a plane in 3D space defined by the equation ax + by + cz + d = 0.
 */
public class Planef {

    /**
     * The x factor in the plane equation.
     */
    public float a;

    /**
     * The y factor in the plane equation.
     */
    public float b;

    /**
     * The z factor in the plane equation.
     */
    public float c;

    /**
     * The constant in the plane equation.
     */
    public float d;

    /**
     * Create a new plane with all components set to zero.
     */
    public Planef() {
    }

    /**
     * Create a new plane with the given equation coefficients.
     *
     * @param a the x factor
     * @param b the y factor
     * @param c the z factor
     * @param d the constant
     */
    public Planef(float a, float b, float c, float d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
}
