# Claude AI Development Documentation

This document contains important information for Claude AI when working on this codebase.

## Terminology / Begriffsdefinitionen

To avoid confusion, we use the following terms consistently:

### Block-Related Terms

- **BlockType** or **Type**: Block-Definition (Definition/Template)
  - The definition/template of a block type
  - Contains: id, name, shape, texture, material, properties
  - Stored in the Registry
  - Examples: "grass", "stone", "water"
  - **Code Location:**
    - Interface: `packages/core/src/block/BlockType.ts`
    - Registry: `packages/core/src/registry/Registry.ts`
    - Client Registry: `packages/client/src/registry/ClientRegistry.ts`
    - Server Registry: `packages/server/src/registry/ServerRegistry.ts`
  - **Access in Code:**
    ```typescript
    const blockType: BlockType = registry.getBlockByID(blockId);
    const allBlocks: BlockType[] = registry.getAllBlocks();
    ```

- **BlockInstance** or **Instance**: Block-Instanz (Concrete instance in the world)
  - A concrete block at a specific position (x, y, z) in the world
  - Contains only a BlockType ID (references the BlockType)
  - Stored in chunk data arrays
  - Example: The grass block at position (10, 64, 5)
  - **Code Location:**
    - Chunk Data: `packages/core/src/world/ChunkData.ts`
    - Stored as: `Uint16Array` in `ChunkData.data`
    - Index calculation: `index = x + z * chunkSize + y * chunkSize * chunkSize`
  - **Access in Code:**
    ```typescript
    // Get block ID at position
    const index = x + z * 32 + y * 32 * 32;
    const blockId = chunkData.data[index];

    // Get BlockType from ID
    const blockType = registry.getBlockByID(blockId);
    ```

- **Material**: Block-Material-Eigenschaft (Logical property)
  - Logical material property of a BlockType
  - Examples: "stone", "wood", "glass", "grass"
  - Defines block behavior and properties
  - Part of BlockType definition
  - **Code Location:**
    - Defined in: `BlockType.options.material` (string)
    - Example: `blockType.options?.material === "stone"`
  - **Usage:**
    ```typescript
    interface BlockType {
      id: number;
      name: string;
      options?: {
        material?: string;  // ← Material property
        fluid?: boolean;
        // ...
      };
    }
    ```

- **RenderingMaterial**: Shader (Babylon.js Material)
  - Babylon.js Material object used for rendering
  - Contains shader, textures, and rendering properties
  - Technical rendering implementation
  - Examples: StandardMaterial, ShaderMaterial, PBRMaterial
  - **Code Location:**
    - Texture Atlas Material: `packages/client/src/rendering/TextureAtlas.ts`
    - Fluid Shader Material: `packages/client/src/rendering/FluidWaveShader.ts`
    - Chunk Rendering: `packages/client/src/rendering/ChunkRenderer.ts`
  - **Usage:**
    ```typescript
    // Standard texture material
    const material = atlas.getMaterial();

    // Fluid wave shader material
    const waterMaterial = fluidWaveShader.createWaterMaterial();
    const lavaMaterial = fluidWaveShader.createLavaMaterial();

    // Apply to mesh
    mesh.material = material;
    ```

## Code Structure

### Core Package (`@nimbus-client/core`)
- Shared types and interfaces
- BlockType definitions
- Protocol definitions

### Client Package (`@nimbus-client/client`)
- Babylon.js rendering
- Player controls
- GUI components
- Block editor

### Server Package (`@nimbus-client/server`)
- World generation
- Chunk management
- Game logic

## Important Notes

- When referring to blocks in UI/documentation, use the correct term (BlockType vs BlockInstance)
- In code comments, prefer the English terms for consistency
- In German UI, use: "Blocktyp" for BlockType, "Blockinstanz" or "Block" for BlockInstance
