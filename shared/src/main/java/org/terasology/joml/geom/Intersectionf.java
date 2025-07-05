// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
//
// Imported from: https://github.com/MovingBlocks/joml-ext
// Original License: Apache-2.0

package org.terasology.joml.geom;

import org.joml.Vector2f;

/**
 * Contains intersection and collision detection methods.
 * This is a simplified version containing only the methods needed for AABBf.
 */
public class Intersectionf {

    // Intersection result constants
    public static final int INSIDE = -1;
    public static final int OUTSIDE = 0;
    public static final int ONE_INTERSECTION = 1;
    public static final int TWO_INTERSECTION = 2;

    /**
     * Test whether the axis-aligned box with minimum corner <code>(minX, minY, minZ)</code> and maximum corner
     * <code>(maxX, maxY, maxZ)</code> intersects the plane given as the general plane equation
     * <code>a*x + b*y + c*z + d = 0</code>.
     */
    public static boolean testAabPlane(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                       float a, float b, float c, float d) {
        float pX, pY, pZ, nX, nY, nZ;
        if (a > 0.0f) {
            pX = maxX;
            nX = minX;
        } else {
            pX = minX;
            nX = maxX;
        }
        if (b > 0.0f) {
            pY = maxY;
            nY = minY;
        } else {
            pY = minY;
            nY = maxY;
        }
        if (c > 0.0f) {
            pZ = maxZ;
            nZ = minZ;
        } else {
            pZ = minZ;
            nZ = maxZ;
        }
        float distanceToP = a * pX + b * pY + c * pZ + d;
        float distanceToN = a * nX + b * nY + c * nZ + d;
        return distanceToP >= 0.0f && distanceToN <= 0.0f;
    }

    /**
     * Test whether the axis-aligned box intersects the plane.
     */
    public static boolean testAabPlane(AABBf aabb, Planef plane) {
        return testAabPlane(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                           plane.a, plane.b, plane.c, plane.d);
    }

    /**
     * Test whether the axis-aligned box intersects the sphere.
     */
    public static boolean testAabSphere(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                        float centerX, float centerY, float centerZ, float radiusSquared) {
        float dx = Math.max(minX - centerX, Math.max(0.0f, centerX - maxX));
        float dy = Math.max(minY - centerY, Math.max(0.0f, centerY - maxY));
        float dz = Math.max(minZ - centerZ, Math.max(0.0f, centerZ - maxZ));
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }

    /**
     * Test whether the axis-aligned box intersects the sphere.
     */
    public static boolean testAabSphere(AABBf aabb, Spheref sphere) {
        return testAabSphere(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ,
                            sphere.x, sphere.y, sphere.z, sphere.r * sphere.r);
    }

    /**
     * Test whether the ray intersects the axis-aligned box.
     */
    public static boolean testRayAab(float originX, float originY, float originZ, float dirX, float dirY, float dirZ,
                                     float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        float invDirX = 1.0f / dirX;
        float invDirY = 1.0f / dirY;
        float invDirZ = 1.0f / dirZ;
        float tNear, tFar, tymin, tymax, tzmin, tzmax;
        if (invDirX >= 0.0f) {
            tNear = (minX - originX) * invDirX;
            tFar = (maxX - originX) * invDirX;
        } else {
            tNear = (maxX - originX) * invDirX;
            tFar = (minX - originX) * invDirX;
        }
        if (invDirY >= 0.0f) {
            tymin = (minY - originY) * invDirY;
            tymax = (maxY - originY) * invDirY;
        } else {
            tymin = (maxY - originY) * invDirY;
            tymax = (minY - originY) * invDirY;
        }
        if (tNear > tymax || tymin > tFar)
            return false;
        if (tymin > tNear)
            tNear = tymin;
        if (tymax < tFar)
            tFar = tymax;
        if (invDirZ >= 0.0f) {
            tzmin = (minZ - originZ) * invDirZ;
            tzmax = (maxZ - originZ) * invDirZ;
        } else {
            tzmin = (maxZ - originZ) * invDirZ;
            tzmax = (minZ - originZ) * invDirZ;
        }
        if (tNear > tzmax || tzmin > tFar)
            return false;
        return true;
    }

    /**
     * Test whether the ray intersects the axis-aligned box.
     */
    public static boolean testRayAab(Rayf ray, AABBf aabb) {
        return testRayAab(ray.oX, ray.oY, ray.oZ, ray.dX, ray.dY, ray.dZ,
                         aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    /**
     * Determine whether the ray intersects the axis-aligned box and store intersection parameters.
     */
    public static boolean intersectRayAab(float originX, float originY, float originZ, float dirX, float dirY, float dirZ,
                                          float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                          Vector2f result) {
        float invDirX = 1.0f / dirX;
        float invDirY = 1.0f / dirY;
        float invDirZ = 1.0f / dirZ;
        float tNear, tFar, tymin, tymax, tzmin, tzmax;
        if (invDirX >= 0.0f) {
            tNear = (minX - originX) * invDirX;
            tFar = (maxX - originX) * invDirX;
        } else {
            tNear = (maxX - originX) * invDirX;
            tFar = (minX - originX) * invDirX;
        }
        if (invDirY >= 0.0f) {
            tymin = (minY - originY) * invDirY;
            tymax = (maxY - originY) * invDirY;
        } else {
            tymin = (maxY - originY) * invDirY;
            tymax = (minY - originY) * invDirY;
        }
        if (tNear > tymax || tymin > tFar)
            return false;
        if (tymin > tNear)
            tNear = tymin;
        if (tymax < tFar)
            tFar = tymax;
        if (invDirZ >= 0.0f) {
            tzmin = (minZ - originZ) * invDirZ;
            tzmax = (maxZ - originZ) * invDirZ;
        } else {
            tzmin = (maxZ - originZ) * invDirZ;
            tzmax = (minZ - originZ) * invDirZ;
        }
        if (tNear > tzmax || tzmin > tFar)
            return false;
        if (tzmin > tNear)
            tNear = tzmin;
        if (tzmax < tFar)
            tFar = tzmax;
        if (result != null) {
            result.x = tNear;
            result.y = tFar;
        }
        return tFar >= 0.0f;
    }

    /**
     * Determine whether the ray intersects the axis-aligned box and store intersection parameters.
     */
    public static boolean intersectRayAab(Rayf ray, AABBf aabb, Vector2f result) {
        return intersectRayAab(ray.oX, ray.oY, ray.oZ, ray.dX, ray.dY, ray.dZ,
                              aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, result);
    }

    /**
     * Determine whether the line segment intersects the axis-aligned box.
     */
    public static int intersectLineSegmentAab(float p0X, float p0Y, float p0Z, float p1X, float p1Y, float p1Z,
                                              float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                              Vector2f result) {
        float dirX = p1X - p0X;
        float dirY = p1Y - p0Y;
        float dirZ = p1Z - p0Z;

        // Use ray-AABB intersection with the direction vector
        Vector2f rayResult = new Vector2f();
        boolean intersects = intersectRayAab(p0X, p0Y, p0Z, dirX, dirY, dirZ, minX, minY, minZ, maxX, maxY, maxZ, rayResult);

        if (!intersects) {
            return OUTSIDE;
        }

        // Check if intersection points are within the line segment (t in [0,1])
        float tNear = rayResult.x;
        float tFar = rayResult.y;

        if (tFar < 0.0f || tNear > 1.0f) {
            return OUTSIDE;
        }

        // Clamp to line segment bounds
        tNear = Math.max(0.0f, tNear);
        tFar = Math.min(1.0f, tFar);

        if (result != null) {
            result.x = tNear;
            result.y = tFar;
        }

        if (tNear == tFar) {
            return ONE_INTERSECTION;
        } else {
            return TWO_INTERSECTION;
        }
    }

    /**
     * Determine whether the line segment intersects the axis-aligned box.
     */
    public static int intersectLineSegmentAab(LineSegmentf lineSegment, AABBf aabb, Vector2f result) {
        return intersectLineSegmentAab(lineSegment.aX, lineSegment.aY, lineSegment.aZ,
                                      lineSegment.bX, lineSegment.bY, lineSegment.bZ,
                                      aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ, result);
    }
}
