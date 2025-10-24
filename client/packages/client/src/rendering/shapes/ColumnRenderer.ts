/**
 * Column Renderer
 * Renders COLUMN blocks (proper cylinder/column geometry)
 * Cylinder with 8 segments: sides (8*4) + top cap (1+8) + bottom cap (1+8) = 50 vertices
 */

import { ShapeRenderer } from './ShapeRenderer';
import type { BlockRenderContext } from './BlockRenderContext';

export class ColumnRenderer extends ShapeRenderer {
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

    // Render as a cylinder (column/pillar)
    let radiusBottom = 0.3; // Default column radius (0.3 blocks = 60% width)
    let radiusTop = 0.3;
    const segments = 8; // Number of sides around the cylinder
    const height = 1.0; // Full block height

    let centerX = x + 0.5;
    let centerZ = z + 0.5;
    let bottomY = y;
    let topY = y + height;

    // Apply edgeOffsets for column manipulation if available
    if (edgeOffsets) {
      // Corner 0: Left-Back-Bottom (indices 0, 1, 2) - Position offset bottom
      const bottomOffsetX = edgeOffsets[0] / 127.0;
      const bottomOffsetZ = edgeOffsets[2] / 127.0;

      // Corner 4: Left-Back-Top (indices 12, 13, 14) - Position offset top
      const topOffsetX = edgeOffsets[12] / 127.0;
      const topOffsetZ = edgeOffsets[14] / 127.0;

      // Corner 1: Right-Back-Bottom (indices 3, 4, 5) - Radius bottom (use X offset)
      const radiusBottomOffset = edgeOffsets[3] / 127.0;

      // Corner 5: Right-Back-Top (indices 15, 16, 17) - Radius top (use X offset)
      const radiusTopOffset = edgeOffsets[15] / 127.0;

      // Apply position offsets (shift column center)
      centerX += bottomOffsetX * 0.5; // Scale down for subtle effect
      centerZ += bottomOffsetZ * 0.5;

      // Apply radius modifiers (positive = larger, negative = smaller)
      radiusBottom = Math.max(0.05, radiusBottom + radiusBottomOffset * 0.3);
      radiusTop = Math.max(0.05, radiusTop + radiusTopOffset * 0.3);

      // Note: We ignore the separate top position offset for now to keep column vertical
      // If needed, could interpolate between bottom and top positions for tapered columns
    }

    const bottomCenterX = centerX;
    const bottomCenterZ = centerZ;
    const topCenterX = centerX; // Could use topOffsetX here for tilted columns
    const topCenterZ = centerZ; // Could use topOffsetZ here for tilted columns

    // Get UVs for different faces
    const sideUV = blockUVs.side || blockUVs.sides || blockUVs.all;
    const topUV = blockUVs.top || blockUVs.all;
    const bottomUV = blockUVs.bottom || blockUVs.all;

    // Generate cylinder side vertices (with potentially different radii for top and bottom)
    for (let i = 0; i < segments; i++) {
      const angle1 = (i / segments) * Math.PI * 2;
      const angle2 = ((i + 1) / segments) * Math.PI * 2;

      const cos1 = Math.cos(angle1);
      const sin1 = Math.sin(angle1);
      const cos2 = Math.cos(angle2);
      const sin2 = Math.sin(angle2);

      // Four vertices for this side segment (quad)
      // Bottom vertices use radiusBottom
      const x1Bottom = bottomCenterX + cos1 * radiusBottom;
      const z1Bottom = bottomCenterZ + sin1 * radiusBottom;
      const x2Bottom = bottomCenterX + cos2 * radiusBottom;
      const z2Bottom = bottomCenterZ + sin2 * radiusBottom;

      // Top vertices use radiusTop
      const x1Top = topCenterX + cos1 * radiusTop;
      const z1Top = topCenterZ + sin1 * radiusTop;
      const x2Top = topCenterX + cos2 * radiusTop;
      const z2Top = topCenterZ + sin2 * radiusTop;

      // Bottom-left
      positions.push(x1Bottom, bottomY, z1Bottom);
      normals.push(cos1, 0, sin1);
      uvs.push(sideUV.u0 + (i / segments) * (sideUV.u1 - sideUV.u0), sideUV.v1);
      colors.push(...blockColor);

      // Bottom-right
      positions.push(x2Bottom, bottomY, z2Bottom);
      normals.push(cos2, 0, sin2);
      uvs.push(sideUV.u0 + ((i + 1) / segments) * (sideUV.u1 - sideUV.u0), sideUV.v1);
      colors.push(...blockColor);

      // Top-right
      positions.push(x2Top, topY, z2Top);
      normals.push(cos2, 0, sin2);
      uvs.push(sideUV.u0 + ((i + 1) / segments) * (sideUV.u1 - sideUV.u0), sideUV.v0);
      colors.push(...blockColor);

      // Top-left
      positions.push(x1Top, topY, z1Top);
      normals.push(cos1, 0, sin1);
      uvs.push(sideUV.u0 + (i / segments) * (sideUV.u1 - sideUV.u0), sideUV.v0);
      colors.push(...blockColor);

      // Indices for this quad (2 triangles)
      const base = vertexIndex + i * 4;
      indices.push(base, base + 1, base + 2);
      indices.push(base, base + 2, base + 3);
    }

    // Top cap (circle) - uses radiusTop
    const topCapCenterIndex = vertexIndex + segments * 4;
    positions.push(topCenterX, topY, topCenterZ);
    normals.push(0, 1, 0);
    uvs.push(topUV.u0 + (topUV.u1 - topUV.u0) / 2, topUV.v0 + (topUV.v1 - topUV.v0) / 2);
    colors.push(...blockColor);

    for (let i = 0; i < segments; i++) {
      const angle = (i / segments) * Math.PI * 2;
      const cos = Math.cos(angle);
      const sin = Math.sin(angle);

      positions.push(topCenterX + cos * radiusTop, topY, topCenterZ + sin * radiusTop);
      normals.push(0, 1, 0);
      uvs.push(
        topUV.u0 + (topUV.u1 - topUV.u0) / 2 + cos * 0.5 * (topUV.u1 - topUV.u0),
        topUV.v0 + (topUV.v1 - topUV.v0) / 2 + sin * 0.5 * (topUV.v1 - topUV.v0)
      );
      colors.push(...blockColor);

      const nextIndex = (i + 1) % segments;
      indices.push(topCapCenterIndex, topCapCenterIndex + 1 + nextIndex, topCapCenterIndex + 1 + i);
    }

    // Bottom cap (circle) - uses radiusBottom
    const bottomCapCenterIndex = topCapCenterIndex + segments + 1;
    positions.push(bottomCenterX, bottomY, bottomCenterZ);
    normals.push(0, -1, 0);
    uvs.push(bottomUV.u0 + (bottomUV.u1 - bottomUV.u0) / 2, bottomUV.v0 + (bottomUV.v1 - bottomUV.v0) / 2);
    colors.push(...blockColor);

    for (let i = 0; i < segments; i++) {
      const angle = (i / segments) * Math.PI * 2;
      const cos = Math.cos(angle);
      const sin = Math.sin(angle);

      positions.push(bottomCenterX + cos * radiusBottom, bottomY, bottomCenterZ + sin * radiusBottom);
      normals.push(0, -1, 0);
      uvs.push(
        bottomUV.u0 + (bottomUV.u1 - bottomUV.u0) / 2 + cos * 0.5 * (bottomUV.u1 - bottomUV.u0),
        bottomUV.v0 + (bottomUV.v1 - bottomUV.v0) / 2 + sin * 0.5 * (bottomUV.v1 - bottomUV.v0)
      );
      colors.push(...blockColor);

      const nextIndex = (i + 1) % segments;
      indices.push(bottomCapCenterIndex, bottomCapCenterIndex + 1 + i, bottomCapCenterIndex + 1 + nextIndex);
    }

    // Add wind properties if rendering transparent blocks
    this.addWindProperties(context, 50);

    // Return 50 vertices: sides (8*4) + top cap (1+8) + bottom cap (1+8) = 50
    return 50;
  }
}
