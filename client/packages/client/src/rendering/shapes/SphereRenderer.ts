/**
 * Sphere Renderer
 * Renders SPHERE blocks (proper sphere with UV mapping and edgeOffset support)
 * Sphere with 8 latitudes and 12 longitudes: (8+1) * (12+1) = 117 vertices
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class SphereRenderer extends ShapeRenderer {
  render(context: BlockRenderContext): number {
    const {
      x,
      y,
      z,
      blockUVs,
      edgeOffsets,
      blockColor,
      positions,
      indices,
      normals,
      uvs,
      colors,
      vertexIndex,
    } = context;

    let radiusX = 0.5; // Sphere fits within 1x1x1 block
    let radiusY = 0.5;
    let radiusZ = 0.5;
    let centerX = x + 0.5;
    let centerY = y + 0.5;
    let centerZ = z + 0.5;

    // Apply edgeOffsets for sphere manipulation if available
    if (edgeOffsets) {
      // Corner 0: Left-Back-Bottom (indices 0, 1, 2) - Position offset (shift sphere)
      const offsetX = edgeOffsets[0] / 127.0;
      const offsetY = edgeOffsets[1] / 127.0;
      const offsetZ = edgeOffsets[2] / 127.0;

      // Corner 1: Right-Back-Bottom (indices 3, 4, 5) - Radius modifier (all axes)
      const radiusOffsetX = edgeOffsets[3] / 127.0;
      const radiusOffsetY = edgeOffsets[4] / 127.0;
      const radiusOffsetZ = edgeOffsets[5] / 127.0;

      // Apply position offsets (shift sphere center)
      centerX += offsetX * 0.5; // Scale for subtle effect
      centerY += offsetY * 0.5;
      centerZ += offsetZ * 0.5;

      // Apply radius modifiers (can make ellipsoid)
      radiusX = Math.max(0.05, radiusX + radiusOffsetX * 0.3);
      radiusY = Math.max(0.05, radiusY + radiusOffsetY * 0.3);
      radiusZ = Math.max(0.05, radiusZ + radiusOffsetZ * 0.3);
    }

    // Sphere tessellation parameters
    const latitudes = 8; // Horizontal divisions
    const longitudes = 12; // Vertical divisions

    // Use the top texture for the entire sphere
    const texture = blockUVs.top;

    // Generate sphere/ellipsoid vertices
    for (let lat = 0; lat <= latitudes; lat++) {
      const theta = (lat * Math.PI) / latitudes; // 0 to PI
      const sinTheta = Math.sin(theta);
      const cosTheta = Math.cos(theta);

      for (let lon = 0; lon <= longitudes; lon++) {
        const phi = (lon * 2 * Math.PI) / longitudes; // 0 to 2*PI
        const sinPhi = Math.sin(phi);
        const cosPhi = Math.cos(phi);

        // Vertex position (normalized sphere coordinates)
        const nx = cosPhi * sinTheta;
        const ny = cosTheta;
        const nz = sinPhi * sinTheta;

        // World position (using separate radii for X, Y, Z to create ellipsoid)
        const px = centerX + radiusX * nx;
        const py = centerY + radiusY * ny;
        const pz = centerZ + radiusZ * nz;

        positions.push(px, py, pz);

        // Normal (same as normalized position for sphere)
        normals.push(nx, ny, nz);

        // UV mapping (spherical projection)
        const u = texture.u0 + (lon / longitudes) * (texture.u1 - texture.u0);
        const v = texture.v0 + (lat / latitudes) * (texture.v1 - texture.v0);
        uvs.push(u, v);

        // Vertex color
        colors.push(...blockColor);
      }
    }

    // Generate sphere indices
    for (let lat = 0; lat < latitudes; lat++) {
      for (let lon = 0; lon < longitudes; lon++) {
        const first = vertexIndex + lat * (longitudes + 1) + lon;
        const second = first + longitudes + 1;

        // Two triangles per quad
        indices.push(first, second, first + 1);
        indices.push(second, second + 1, first + 1);
      }
    }

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, 117);

    // Return 117 vertices: (8+1) * (12+1) = 117
    return 117;
  }
}
