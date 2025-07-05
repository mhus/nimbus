// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

/**
 * Interface to a read-only view of an axis-aligned box defined via the minimum and maximum corner coordinates as
 * double-precision floats.
 */
public interface AABBdc {

    /**
     * @return The x coordinate of the minimum corner.
     */
    double minX();

    /**
     * @return The y coordinate of the minimum corner.
     */
    double minY();

    /**
     * @return The z coordinate of the minimum corner.
     */
    double minZ();

    /**
     * @return The x coordinate of the maximum corner.
     */
    double maxX();

    /**
     * @return The y coordinate of the maximum corner.
     */
    double maxY();

    /**
     * @return The z coordinate of the maximum corner.
     */
    double maxZ();
}
