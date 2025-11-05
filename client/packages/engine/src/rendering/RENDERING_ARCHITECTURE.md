# Rendering Architecture Documentation

## Overview

This document describes the rendering system architecture for the Nimbus 3D Voxel Engine Client 2.0, focusing on material grouping, mesh management, and special rendering cases.

## Current Implementation (Nov 2025)

### Property-Based Material Grouping

The rendering system groups blocks by **material properties** instead of texture paths to optimize draw calls.

**Key Concept:**
- All standard materials use the **Texture Atlas**
- Blocks with identical material properties share the same material and mesh
- Different textures are handled via UV coordinates pointing to different atlas positions
- Result: ~5-10 meshes per chunk instead of 50+ (one per texture)

**Material Key Format:**
```
atlas|bfc:{bool}|tm:{mode}|op:{float}|sm:{mode}|eff:{effect}|sp:{params}
```

**Example Keys:**
```
atlas|bfc:true|tm:0|op:1|sm:1|eff:0              // Standard blocks
atlas|bfc:false|tm:0|op:1|sm:1|eff:0             // Leaves, glass (double-sided)
atlas|bfc:true|tm:2|op:0.5|sm:1|eff:0            // Transparent blocks
atlas|bfc:true|tm:0|op:1|sm:1|eff:2              // Wind effect (shader)
```

**Performance:**
- 50 different textures per chunk
- Only ~5-10 different material property combinations
- Result: 5-10 meshes per chunk
- 45-90 draw calls for 9 visible chunks (instead of 450)

### Material Service

**Responsibilities:**
- Generate material keys based on properties
- Create and cache materials
- Load textures (atlas or original)
- Apply material properties (backFaceCulling, transparency, etc.)

**Key Methods:**
- `getMaterialKey(modifier, textureIndex)` - Generate property-based key
- `getMaterial(modifier, textureIndex, customKey?)` - Get or create material
- `parseMaterialKey(key)` - Parse key into properties
- `createStandardMaterialWithAtlas(name, props)` - Create atlas-based material

### Render Service

**Responsibilities:**
- Group blocks by material key
- Generate geometry (vertices, indices, UVs, normals)
- Create meshes per material group
- Manage mesh cache (Map<chunkKey, Map<materialKey, Mesh>>)

**Render Flow:**
```
1. Group blocks by material key
2. For each material group:
   a. Generate geometry for all blocks in group
   b. Create mesh
   c. Get material from MaterialService
   d. Assign material to mesh
3. Store meshes in cache
```

---

## Special Rendering Cases

Some blocks require **separate meshes** instead of being part of the chunk mesh. These cases are identified by the **BlockRenderer** that handles them.

### BlockRenderer Interface

```typescript
interface BlockRenderer {
  /**
   * Check if this renderer can handle the given shape/effect
   */
  canRender(shape: Shape, effect?: BlockEffect): boolean;

  /**
   * Render a block (adds geometry to chunk mesh OR creates separate mesh)
   */
  render(context: RenderContext, clientBlock: ClientBlock): Promise<void>;

  /**
   * Does this renderer need a separate mesh per block/group?
   * Default: false (contributes to chunk mesh)
   */
  needsSeparateMesh(): boolean;
}
```

### Separate Mesh Strategy

**When a renderer returns `needsSeparateMesh() = true`:**
1. Block is NOT added to chunk mesh material groups
2. Block is rendered separately with its own mesh
3. May use original texture instead of atlas
4. May use custom shader material

**Renderers that need separate meshes:**
- **FlipboxRenderer** - Sprite-sheet animation shader
- **BillboardRenderer** - Camera-facing billboards
- **SpriteRenderer** - Babylon.js SpriteManager
- **ModelRenderer** - Custom 3D models loaded from files
- **FlameRenderer** - Animated flame sprites

**Renderers that use chunk mesh:**
- **CubeRenderer** - Standard cubes (already implemented)
- **CrossRenderer** - Cross shapes (planned)
- **HashRenderer** - Hash/grid patterns (planned)
- Future: Stairs, slabs, custom geometry shapes

---

## Atlas vs Original Texture

### When to Use Atlas
- ✅ Standard shapes (CUBE, CROSS, HASH, etc.)
- ✅ Shader effects that work with atlas (WIND, WATER, LAVA)
- ✅ Most blocks (95%+ of all blocks)

### When to Use Original Texture
- ✅ **FLIPBOX Shader** - Needs complete sprite-sheet with all frames
- ✅ **SPRITE/FLAME** - SpriteManager requires separate textures
- ✅ **MODEL** - Models have their own texture files
- ✅ **BILLBOARD** (optional) - May need high-res textures

### Decision Logic

```typescript
// MaterialService.getMaterial()
if (effect === BlockEffect.FLIPBOX) {
  // Load ORIGINAL texture (sprite-sheet with all frames)
  const texture = await this.loadTexture(textureDef);
  material = this.shaderService.createMaterial('flipbox', { texture, ... });
}
else if (effect === BlockEffect.WIND || effect === BlockEffect.WATER) {
  // Use ATLAS texture
  const atlasTexture = this.textureAtlas.getTexture();
  material = this.shaderService.createMaterial('wind', { texture: atlasTexture, ... });
}
else {
  // Standard: Use ATLAS texture
  material = this.createStandardMaterialWithAtlas(cacheKey, props);
}
```

---

## FLIPBOX Shader Implementation Plan

### What is FLIPBOX?

FLIPBOX is a **shader-based sprite-sheet animation** effect for blocks. It cycles through animation frames defined in a single texture.

**Configuration (in TextureDefinition):**
```typescript
{
  path: 'textures/animated_box.png',
  uvMapping: {
    x: 0.0,    // Start X in texture (first frame)
    y: 0.0,    // Start Y in texture
    w: 0.25,   // Width of one frame (1/4 if 4 frames)
    h: 1.0     // Height of frame
  },
  shaderParameters: '4,100',  // Format: "frameCount,delayMs"
  effect: BlockEffect.FLIPBOX
}
```

**Shader Parameters Format:**
```
"frameCount,delayMs"

Examples:
"4,100"   → 4 frames, 100ms between frames
"8,200"   → 8 frames, 200ms between frames
"2,500"   → 2 frames (flip/flop), 500ms delay
```

### FLIPBOX Material Key

```
atlas|bfc:true|tm:0|op:1|sm:1|eff:3|sp:4,100
                              ↑      ↑
                          FLIPBOX   frames,delay
```

**Grouping Behavior:**
- Blocks with same effect + shaderParameters → Same material/mesh
- Different textures but same parameters → Different material keys (because different textures)
  - **NOTE:** For FLIPBOX, we need texture path in key!

**Enhanced Material Key for FLIPBOX:**
```typescript
// For FLIPBOX only: Include texture path
if (effect === BlockEffect.FLIPBOX) {
  return `flipbox|tex:${texturePath}|sp:${shaderParameters}|bfc:${backFaceCulling}|...`;
}
```

This ensures:
- Same texture + same params → Same material (✓)
- Different texture → Different material (✓)

### Shader Implementation

**Vertex Shader:**
- Standard vertex shader (position, normal, uv, color)
- Pass through to fragment shader

**Fragment Shader:**
- Parse `shaderParameters` uniform: "frameCount,delayMs"
- Calculate current frame based on time: `currentFrame = floor(time / delay) % frameCount`
- Offset UV coordinates: `uv.x += frameWidth * currentFrame`
- Sample texture with animated UV
- Apply lighting

**Uniforms:**
```glsl
uniform sampler2D textureSampler;
uniform float time;                // Updated each frame
uniform float frameCount;          // From shaderParameters
uniform float frameDelay;          // From shaderParameters (in seconds)
uniform float frameWidth;          // Calculated from uvMapping.w
uniform vec3 lightDirection;
```

**Animation Logic:**
```glsl
// Calculate current frame (cycles 0, 1, 2, 3, 0, 1, ...)
float currentFrame = mod(floor(time / frameDelay), frameCount);

// Offset UV to current frame (assumes horizontal sprite-sheet)
vec2 animatedUV = vUV;
animatedUV.x = vUV.x + (frameWidth * currentFrame);

// Sample texture
vec4 texColor = texture2D(textureSampler, animatedUV);
```

---

## Implementation TODO List

### Phase 1: Foundation (CURRENT)
- [x] Implement property-based material grouping
- [x] Refactor MaterialService.getMaterialKey() to group by properties
- [x] Update RenderService to create multiple meshes per chunk
- [x] Update mesh cache structure: Map<chunkKey, Map<materialKey, Mesh>>
- [ ] Add `needsSeparateMesh()` to BlockRenderer interface
- [ ] Update RenderService.renderChunk() to separate chunk mesh vs separate mesh blocks

### Phase 2: FLIPBOX Shader ✅
- [x] **REMOVE** FLIPBOX from BlockEffect enum (it's a shader, not an effect!)
  - Location: `/Users/hummel/sources/mhus/nimbus/client/packages/shared/src/types/BlockModifier.ts:411-426`
  - Created: `BlockShader` enum with FLIPBOX, WIND
  - Added: `shader` and `shaderParameters` fields to TextureDefinition and VisibilityModifier

- [x] Register FLIPBOX as shader in ShaderService:
  - Added `registerFlipboxShader()` method
  - Called in `ShaderService.initialize():70-71`

- [x] Implement FLIPBOX shader code:
  - `registerFlipboxShaderCode()` - Vertex & fragment shaders implemented
  - Parses shaderParameters: "frameCount,delayMs"
  - Animates UV coordinates frame-by-frame based on time
  - Supports horizontal sprite-sheets (frames side-by-side)
  - Location: `ShaderService.ts:467-535`

- [x] Create FLIPBOX material factory:
  - `createFlipboxMaterial(texture, shaderParameters, name)` in ShaderService
  - Loads ORIGINAL texture (not atlas!)
  - Sets up uniforms: frameCount, currentFrame, lightDirection
  - Time update per frame with frame cycling logic
  - Location: `ShaderService.ts:537-636`

- [x] Update MaterialService.getMaterial():
  - Checks for BlockShader (shader field in modifier)
  - Loads original texture (not atlas) for FLIPBOX shader
  - Passes shaderParameters to shader
  - Location: `MaterialService.ts:300-333`

- [x] Enhance material key for FLIPBOX:
  - Includes texture path in key: `flipbox|tex:{path}|shader:1|sp:{params}|...`
  - Ensures same texture+params share material
  - Location: `MaterialService.ts:120-140`

### Phase 3: Separate Mesh System (PARTIAL - Infrastructure Ready)
- [x] Add `needsSeparateMesh(): boolean` to BlockRenderer interface (default false)
  - Location: `BlockRenderer.ts:28-39`

- [ ] Create FlipboxRenderer extending BlockRenderer:
  - `needsSeparateMesh()` returns true
  - `render()` creates separate mesh with FLIPBOX shader material
  - **NOTE:** Will be implemented according to `/instructions/shape_rendern.md` (renders only TOP face)

- [ ] Update RenderService.renderChunk():
  - Split blocks into: chunkMeshBlocks vs separateMeshBlocks
  - Check `renderer.needsSeparateMesh()` for each block
  - Render chunk meshes (existing logic)
  - Render separate meshes (new logic)
  - **NOTE:** Current FLIPBOX implementation works via CUBE blocks with shader material
  - Separate mesh logic will be needed when FlipboxRenderer is created

- [ ] Add separate mesh tracking:
  - `separateMeshes: Map<blockKey, Mesh>` in RenderService
  - Track and dispose separately from chunk meshes

### Phase 4: Future Special Renderers (NOT in this sprint)
- [ ] BillboardRenderer (camera-facing quads)
- [ ] SpriteRenderer (SpriteManager integration)
- [ ] ModelRenderer (load .babylon/.glb files)
- [ ] FlameRenderer (animated flame sprites)

### Phase 5: Testing & Optimization
- [ ] Test FLIPBOX with various sprite-sheet configurations
- [ ] Performance profiling (draw calls, memory)
- [ ] Material cache optimization
- [ ] Documentation updates

---

## Design Decisions

### Why Property-Based Grouping?
- **Problem:** 50 textures per chunk = 50 materials = 50 meshes = 450 draw calls (too many!)
- **Solution:** Group by properties (backFaceCulling, transparency) = 5-10 meshes = 45-90 draw calls
- **Trade-off:** More complex material key logic, but massive performance gain

### Why Separate Meshes for Some Shapes?
- **Reason 1:** Different rendering APIs (SpriteManager vs Mesh)
- **Reason 2:** Original textures needed (sprite-sheets, animations)
- **Reason 3:** Custom behavior (billboards rotate to camera)
- **Trade-off:** More meshes, but still acceptable (<5% of total blocks)

### Why BlockRenderer.needsSeparateMesh()?
- **Reason:** Each renderer knows its own requirements
- **Alternative:** Central classification logic (brittle, hard to extend)
- **Advantage:** Strategy pattern - easy to add new renderers

### Why FLIPBOX as Shader (not BlockEffect)?
- **Reason:** BlockEffect is for gameplay effects (water, lava), not rendering
- **FLIPBOX:** Pure visual effect, no gameplay impact
- **Belongs in:** ShaderService (like other visual shaders)
- **Architecture:** Cleaner separation (rendering vs gameplay)

---

## Material Key Generation Logic

### Standard Blocks (Atlas)
```typescript
key = `atlas|bfc:${backFaceCulling}|tm:${transparencyMode}|op:${opacity}|sm:${samplingMode}|eff:${effect}`;
```

**No texture path in key** → Blocks with different textures can share material if properties match

### FLIPBOX Blocks (Original Texture)
```typescript
key = `flipbox|tex:${texturePath}|sp:${shaderParameters}|bfc:${backFaceCulling}|tm:${transparencyMode}|...`;
```

**Texture path IS in key** → Different textures = different materials (needed for original texture loading)

### SPRITE/BILLBOARD Blocks (Original Texture)
```typescript
key = `sprite|tex:${texturePath}|...`;
key = `billboard|tex:${texturePath}|...`;
```

### Model Blocks (Model Files)
```typescript
key = `model|path:${modelPath}|...`;
```

---

## Rendering Decision Flow

```
For each ClientBlock:
  ↓
Get BlockRenderer for block (based on shape + effect)
  ↓
Check renderer.needsSeparateMesh()
  ↓
  ├─ YES (Separate Mesh) ──────────────────┐
  │   - FLIPBOX shader                     │
  │   - BILLBOARD                           │
  │   - SPRITE                              │
  │   - MODEL                               │
  │   - FLAME                               │
  │                                         │
  │   Flow:                                 │
  │   1. Generate custom material key       │
  │   2. Get material (may load original    │
  │      texture instead of atlas)          │
  │   3. Render to separate mesh            │
  │   4. Track in separateMeshes map        │
  │                                         │
  └─ NO (Chunk Mesh) ──────────────────────┤
      - CUBE (standard)                     │
      - CROSS                               │
      - HASH                                │
      - STAIRS                              │
      - Wind/Water shader effects           │
                                            │
      Flow:                                 │
      1. Generate material key (properties) │
      2. Add to material group              │
      3. Render geometry to group's FaceData│
      4. Create mesh per material group     │
      5. Get atlas-based material           │
      6. Assign to mesh                     │
      ↓                                     │
  Both paths lead to rendered scene ────────┘
```

---

## Data Structures

### Mesh Cache
```typescript
// RenderService
private chunkMeshes: Map<string, Map<string, Mesh>>

// Structure:
// "chunk_0_0" -> {
//   "atlas|bfc:true|..." -> Mesh (standard blocks),
//   "atlas|bfc:false|..." -> Mesh (double-sided blocks),
//   "atlas|bfc:true|...|eff:2" -> Mesh (wind effect)
// }

private separateMeshes: Map<string, Mesh>

// Structure:
// "chunk_0_0_block_10_64_5" -> Mesh (FLIPBOX block at 10,64,5)
// "chunk_0_0_block_12_64_8" -> Mesh (BILLBOARD block at 12,64,8)
```

### Material Cache
```typescript
// MaterialService
private materials: Map<string, Material>

// Structure:
// "atlas|bfc:true|tm:0|..." -> StandardMaterial (atlas texture)
// "atlas|bfc:false|tm:0|..." -> StandardMaterial (atlas texture, no culling)
// "flipbox|tex:anim.png|sp:4,100|..." -> ShaderMaterial (original texture)
// "sprite|tex:grass.png|..." -> StandardMaterial (original texture)
```

---

## FLIPBOX Shader Specification

### Purpose
Animate sprite-sheet textures by cycling through frames horizontally arranged in a single texture.

### Configuration

**In TextureDefinition:**
```typescript
{
  path: 'textures/animated_box.png',      // Sprite-sheet texture
  uvMapping: {
    x: 0.0,      // Start position (first frame)
    y: 0.0,
    w: 0.25,     // Width of ONE frame (e.g., 1/4 for 4 frames)
    h: 1.0       // Full height
  },
  shaderParameters: '4,100',  // "frameCount,delayMs"
  effect: BlockEffect.FLIPBOX // ← WILL BE REMOVED! Use shader registration instead
}
```

**After refactor, use ShaderService registration:**
```typescript
// Instead of BlockEffect.FLIPBOX, configure via:
{
  path: 'textures/animated_box.png',
  uvMapping: { ... },
  shaderParameters: '4,100',
  // Shader is detected automatically by ShaderService
  // Or via explicit shader name in effectParameters
}
```

### Shader Parameters Format

```
"frameCount,delayMs"

Components:
- frameCount: Number of horizontal frames in sprite-sheet (integer)
- delayMs: Milliseconds between frame transitions (integer)

Examples:
"4,100"   → 4 frames, 100ms per frame (400ms total loop)
"8,200"   → 8 frames, 200ms per frame (1.6s total loop)
"2,500"   → 2 frames, 500ms per frame (simple flip/flop)
```

### Shader Uniforms

```glsl
uniform sampler2D textureSampler;  // Original sprite-sheet texture
uniform float time;                // Elapsed time in seconds (updated per frame)
uniform float frameCount;          // Number of frames (e.g., 4.0)
uniform float frameDelay;          // Delay in seconds (e.g., 0.1 for 100ms)
uniform float frameWidth;          // UV width of one frame (e.g., 0.25)
uniform vec3 lightDirection;       // Directional lighting
```

### Shader Logic (Fragment)

```glsl
void main(void) {
  // Calculate current frame index (cycles 0, 1, 2, 3, 0, 1, ...)
  float currentFrame = mod(floor(time / frameDelay), frameCount);

  // Offset UV to current frame position (horizontal sprite-sheet)
  vec2 animatedUV = vUV;
  animatedUV.x = vUV.x + (frameWidth * currentFrame);

  // Sample texture at animated position
  vec4 texColor = texture2D(textureSampler, animatedUV);

  // Alpha test
  if (texColor.a < 0.5) {
    discard;
  }

  // Apply vertex color tint
  vec4 finalColor = texColor * vColor;

  // Apply directional lighting
  float lightIntensity = max(dot(vNormal, lightDirection), 0.3);
  finalColor.rgb *= lightIntensity;

  gl_FragColor = vec4(finalColor.rgb, 1.0);
}
```

### Material Creation

```typescript
// ShaderService.createFlipboxMaterial()
private createFlipboxMaterial(
  texture: Texture | undefined,
  name: string,
  params?: Record<string, any>
): ShaderMaterial {
  const material = new ShaderMaterial(name, this.scene, 'flipbox', {
    attributes: ['position', 'normal', 'uv', 'color'],
    uniforms: [
      'worldViewProjection',
      'world',
      'time',
      'frameCount',
      'frameDelay',
      'frameWidth',
      'textureSampler',
      'lightDirection'
    ],
    samplers: ['textureSampler']
  });

  // Parse shaderParameters: "4,100"
  const shaderParams = params?.shaderParameters || '1,1000';
  const [frameCountStr, delayMsStr] = shaderParams.split(',');
  const frameCount = parseInt(frameCountStr) || 1;
  const delayMs = parseInt(delayMsStr) || 1000;

  // Set uniforms
  material.setTexture('textureSampler', texture);
  material.setFloat('frameCount', frameCount);
  material.setFloat('frameDelay', delayMs / 1000.0); // Convert to seconds
  material.setFloat('frameWidth', params?.frameWidth || 0.25); // From uvMapping.w
  material.setVector3('lightDirection', new Vector3(0.5, 1.0, 0.5));

  // Update time every frame
  let totalTime = 0;
  this.scene.onBeforeRenderObservable.add(() => {
    totalTime += this.scene.getEngine().getDeltaTime() / 1000.0;
    material.setFloat('time', totalTime);
  });

  material.backFaceCulling = false; // Typically want double-sided

  return material;
}
```

---

## Migration Plan from BlockEffect to Shader

### Current Problem
FLIPBOX is currently defined as `BlockEffect.FLIPBOX = 3` in the enum. This is incorrect because:
- BlockEffect is meant for **gameplay effects** (water behavior, lava damage)
- FLIPBOX is purely **visual/rendering** (no gameplay impact)
- Should be registered as a **shader** in ShaderService (like WIND)

### Migration Steps

**1. Remove from BlockEffect enum:**
```typescript
// BlockModifier.ts
export enum BlockEffect {
  NONE = 0,
  WATER = 1,
  WIND = 2,
  // FLIPBOX = 3,  ← REMOVE THIS!
  LAVA = 4,
  FOG = 5,
}
```

**2. Register as shader in ShaderService:**
```typescript
// ShaderService.initialize()
this.registerWindShader();
this.registerFlipboxShader();  // ← Add this
```

**3. Detection in MaterialService:**
Instead of checking `effect === BlockEffect.FLIPBOX`, check if shaderService has 'flipbox':
```typescript
// OLD (wrong):
if (effect === BlockEffect.FLIPBOX) { ... }

// NEW (correct):
const shaderName = this.detectShader(modifier); // Returns 'flipbox', 'wind', etc.
if (shaderName === 'flipbox' && this.shaderService) { ... }
```

**4. Block configuration changes:**
```typescript
// OLD (using BlockEffect):
{
  visibility: {
    effect: BlockEffect.FLIPBOX,  // ← Wrong!
    effectParameters: { ... }
  }
}

// NEW (using shader detection):
{
  visibility: {
    textures: {
      [TextureKey.ALL]: {
        path: 'textures/anim.png',
        shaderParameters: '4,100',  // ← Detected automatically!
        // Shader is inferred from shaderParameters presence
      }
    }
  }
}

// OR explicit shader selection (future):
{
  visibility: {
    shader: 'flipbox',  // ← New field?
    shaderParameters: { frameCount: 4, delayMs: 100 }
  }
}
```

---

## Future Enhancements

### 1. Vertical Sprite-Sheets
Support frames stacked vertically:
```typescript
shaderParameters: '4,100,vertical'  // frames,delay,direction
```

### 2. Grid Sprite-Sheets
Support frames in 2D grid:
```typescript
shaderParameters: '4,2,100'  // columns,rows,delay
```

### 3. Shader Registry
Instead of hardcoded shader names, allow dynamic registration:
```typescript
shaderService.registerCustomShader('myEffect', {
  vertexCode: '...',
  fragmentCode: '...',
  uniforms: [...],
  parameterParser: (params) => { ... }
});
```

### 4. Material Instancing
For blocks with same material but different transforms, use Babylon.js instancing:
```typescript
// Instead of separate meshes for each billboard:
const instancedMesh = billboardMesh.createInstance(`billboard_${blockId}`);
```

---

## Performance Considerations

### Current Performance (9 visible chunks)
- **Standard blocks:** ~45-90 draw calls (5-10 materials per chunk)
- **Separate meshes:** +1-5 draw calls per chunk (rare special blocks)
- **Total:** ~50-135 draw calls (excellent for 60 FPS)

### Memory Usage
- **Material cache:** One material per unique property combination (~10-20 total)
- **Texture atlas:** One large texture (shared by all standard materials)
- **Original textures:** Only loaded for special cases (FLIPBOX, SPRITE, MODEL)
- **Mesh cache:** 5-10 meshes per chunk + sparse separate meshes

### Optimization Strategies
1. **Material reuse:** Same properties = same material (already implemented)
2. **Geometry batching:** Multiple blocks per mesh (already implemented)
3. **Frustum culling:** Only render visible chunks (TODO)
4. **LOD system:** Lower detail for distant chunks (future)
5. **Instancing:** For repeated separate meshes (future)

---

## Code Locations

### Core Services
- **MaterialService:** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/services/MaterialService.ts`
- **RenderService:** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/services/RenderService.ts`
- **ShaderService:** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/services/ShaderService.ts`

### Renderers
- **BlockRenderer (interface):** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/rendering/BlockRenderer.ts`
- **CubeRenderer:** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/rendering/CubeRenderer.ts`
- **FlipboxRenderer (planned):** `/Users/hummel/sources/mhus/nimbus/client/packages/engine/src/rendering/FlipboxRenderer.ts`

### Types
- **BlockModifier:** `/Users/hummel/sources/mhus/nimbus/client/packages/shared/src/types/BlockModifier.ts`
- **TextureDefinition:** `/Users/hummel/sources/mhus/nimbus/client/packages/shared/src/types/BlockModifier.ts` (lines 373-403)
- **Shape enum:** `/Users/hummel/sources/mhus/nimbus/client/packages/shared/src/types/BlockModifier.ts` (lines 427-448)
- **BlockEffect enum:** `/Users/hummel/sources/mhus/nimbus/client/packages/shared/src/types/BlockModifier.ts` (lines 408-426)

### Playground Reference
- **Old ChunkRenderer:** `/Users/hummel/sources/mhus/nimbus/client_playground/packages/client/src/rendering/ChunkRenderer.ts` (lines 215-219)

---

## Examples

### Example 1: Standard Grass Block
```typescript
Block {
  blockTypeId: 2,
  modifier: {
    visibility: {
      shape: Shape.CUBE,
      textures: {
        [TextureKey.TOP]: 'textures/grass_top.png',
        [TextureKey.SIDE]: 'textures/grass_side.png',
        [TextureKey.BOTTOM]: 'textures/dirt.png'
      }
    }
  }
}

Result:
- needsSeparateMesh(): false
- Material Key: "atlas|bfc:true|tm:0|op:1|sm:1|eff:0"
- Rendered in: Chunk mesh
- Material: Atlas-based StandardMaterial
- Draw calls: Shared with other standard blocks
```

### Example 2: Glass Block (Double-Sided)
```typescript
Block {
  modifier: {
    visibility: {
      shape: Shape.CUBE,
      textures: {
        [TextureKey.ALL]: {
          path: 'textures/glass.png',
          backFaceCulling: false,  // See from both sides
          transparencyMode: TransparencyMode.ALPHA_BLEND
        }
      }
    }
  }
}

Result:
- needsSeparateMesh(): false
- Material Key: "atlas|bfc:false|tm:2|op:1|sm:1|eff:0"
- Rendered in: Chunk mesh (different group than standard)
- Material: Atlas-based with backFaceCulling=false
- Draw calls: Shared with other glass-like blocks
```

### Example 3: FLIPBOX Animated Block
```typescript
Block {
  modifier: {
    visibility: {
      shape: Shape.CUBE,
      textures: {
        [TextureKey.ALL]: {
          path: 'textures/animated_box.png',
          uvMapping: {
            x: 0.0, y: 0.0,
            w: 0.25, h: 1.0  // 1/4 width (4 frames)
          },
          shaderParameters: '4,100'  // 4 frames, 100ms
        }
      }
    }
  }
}

Result:
- needsSeparateMesh(): true (FlipboxRenderer)
- Material Key: "flipbox|tex:animated_box.png|sp:4,100|bfc:false|..."
- Rendered in: Separate mesh
- Material: ShaderMaterial with original texture
- Shader: Custom FLIPBOX shader with animated UVs
- Draw calls: +1 per group of identical animated blocks
```

### Example 4: Billboard (Future)
```typescript
Block {
  modifier: {
    visibility: {
      shape: Shape.BILLBOARD,
      textures: {
        [TextureKey.ALL]: 'textures/flower.png'
      }
    }
  }
}

Result:
- needsSeparateMesh(): true (BillboardRenderer)
- Material Key: "billboard|tex:flower.png|bfc:false|..."
- Rendered in: Separate mesh with billboardMode
- Material: Atlas OR original (TBD)
- Draw calls: +1 per billboard group
```

---

## Testing Checklist

### Material Grouping (Current)
- [x] Multiple textures with same properties → Same mesh
- [x] backFaceCulling:true vs false → Different meshes
- [x] Transparency settings → Different meshes
- [x] Material cache working correctly
- [ ] materialInfo() command shows all keys

### FLIPBOX Shader (Next)
- [ ] Sprite-sheet with 4 frames animates correctly
- [ ] Different frame counts (2, 4, 8) work
- [ ] Different delays (50ms, 100ms, 500ms) work
- [ ] Multiple FLIPBOX blocks with same texture share material
- [ ] Multiple FLIPBOX blocks with different textures have separate materials
- [ ] UV animation stays within frame bounds
- [ ] Lighting works correctly

### Separate Meshes (Future)
- [ ] BILLBOARD faces camera correctly
- [ ] SPRITE uses SpriteManager
- [ ] MODEL loads .babylon files
- [ ] Separate meshes disposed correctly on chunk unload

---

## Open Questions

### 1. Shader Detection
**How should blocks indicate they want FLIPBOX shader?**

**Option A:** Via BlockEffect enum (current)
```typescript
effect: BlockEffect.FLIPBOX
```
- Pro: Explicit
- Con: Mixes rendering with gameplay effects

**Option B:** Auto-detect from shaderParameters
```typescript
// If shaderParameters present and matches pattern → use FLIPBOX
shaderParameters: '4,100'  // Automatically triggers FLIPBOX shader
```
- Pro: No enum pollution
- Con: Implicit, harder to understand

**Option C:** Explicit shader field
```typescript
shader: 'flipbox',
shaderParameters: '4,100'
```
- Pro: Very explicit, extensible
- Con: New field needed

**DECISION NEEDED!**

### 2. Material Key for Separate Meshes
**Should separate mesh materials be cached?**

**Current:** Yes, keyed by texture path + parameters
**Alternative:** No cache, create per block (simpler, but wasteful)

**Recommendation:** Keep caching (blocks with same texture+params share material)

### 3. UV Mapping for FLIPBOX
**Who provides frameWidth to shader?**

**Option A:** Calculate in MaterialService from uvMapping.w
**Option B:** Pass entire uvMapping to shader
**Option C:** Require frameWidth in shaderParameters: "4,100,0.25"

**Recommendation:** Option A (cleanest, derived from uvMapping)

---

## Glossary

- **Material Key:** Unique string identifying a material based on properties (used for caching)
- **Material Grouping:** Combining blocks with same material into one mesh
- **Chunk Mesh:** Single mesh containing all compatible blocks in a chunk
- **Separate Mesh:** Individual mesh for blocks that need special rendering
- **Atlas Texture:** Large texture containing many block textures (for efficient rendering)
- **Original Texture:** Individual texture file (needed for sprites, animations, models)
- **BlockRenderer:** Strategy pattern implementation that knows how to render specific shapes/effects
- **Shader Effect:** Visual effect implemented via custom vertex/fragment shaders
- **BlockEffect:** Gameplay effect (water physics, lava damage) - different from shader effects!

---

## Revision History

- **2025-11-06:** Initial documentation
  - Documented property-based material grouping
  - Planned FLIPBOX shader implementation
  - Designed unified system for special rendering cases
  - Defined BlockRenderer.needsSeparateMesh() pattern
