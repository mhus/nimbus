# HeightData Usage

Named tuple for chunk height information with clear semantics.

## Type Definition

```typescript
type HeightData = readonly [
  maxHeight: number,
  minHeight: number,
  groundLevel: number,
  waterHeight: number
];
```

## Benefits of Named Tuple

✅ **Self-documenting** - Parameter names visible in IDE
✅ **Type-safe** - TypeScript enforces tuple length
✅ **Readonly** - Prevents accidental mutations
✅ **Autocomplete** - IDE shows parameter names

## Usage Examples

### 1. Creating HeightData

```typescript
// Create height data for a chunk
const heightData: HeightData = [
  128,  // maxHeight
  0,    // minHeight
  64,   // groundLevel
  62    // waterHeight
];

// With named parameters (visible in IDE)
const heightData: HeightData = [
  /*maxHeight*/ 128,
  /*minHeight*/ 0,
  /*groundLevel*/ 64,
  /*waterHeight*/ 62
];
```

### 2. Destructuring

```typescript
// Destructure with clear names
const [maxHeight, minHeight, groundLevel, waterHeight] = heightData;

console.log(`Max: ${maxHeight}, Min: ${minHeight}`);
console.log(`Ground: ${groundLevel}, Water: ${waterHeight}`);

// Partial destructuring
const [max, min] = heightData;
console.log(`Height range: ${min} to ${max}`);
```

### 3. Accessing by Index (still possible)

```typescript
const maxHeight = heightData[0];
const minHeight = heightData[1];
const groundLevel = heightData[2];
const waterHeight = heightData[3];
```

### 4. In ChunkDataTransferObject

```typescript
interface ChunkDataTransferObject {
  c: number;
  z: number;
  b: Block[];
  h: HeightData[];  // Array of height data
  a?: AreaData[];
  e?: EntityData[];
}

// Create chunk with height data
const chunkData: ChunkDataTransferObject = {
  c: 0,
  z: 0,
  b: [],
  h: [
    [128, 0, 64, 62],  // Position (0, 0)
    [130, 2, 65, 62],  // Position (1, 0)
    [125, 0, 63, 62],  // Position (2, 0)
    // ... more height data for each XZ position in chunk
  ]
};
```

### 5. Processing Height Data

```typescript
function analyzeChunk(chunk: ChunkDataTransferObject) {
  chunk.h.forEach((heightData, index) => {
    const [maxHeight, minHeight, groundLevel, waterHeight] = heightData;

    // Calculate terrain metrics
    const terrainHeight = maxHeight - minHeight;
    const isUnderwater = groundLevel < waterHeight;
    const waterDepth = Math.max(0, waterHeight - groundLevel);

    console.log(`Position ${index}:`);
    console.log(`  Terrain height: ${terrainHeight}`);
    console.log(`  Ground level: ${groundLevel}`);
    console.log(`  Underwater: ${isUnderwater}`);
    if (isUnderwater) {
      console.log(`  Water depth: ${waterDepth}`);
    }
  });
}
```

### 6. Generating Height Data

```typescript
function generateHeightData(x: number, z: number): HeightData {
  // Use noise function or other terrain generation
  const groundLevel = getTerrainHeight(x, z);
  const waterHeight = 62; // Sea level

  // Calculate bounds
  const minHeight = Math.max(0, groundLevel - 5);
  const maxHeight = Math.min(255, groundLevel + 10);

  return [maxHeight, minHeight, groundLevel, waterHeight];
}

// Generate for entire chunk
function generateChunkHeightData(chunkX: number, chunkZ: number): HeightData[] {
  const heightData: HeightData[] = [];

  for (let z = 0; z < 16; z++) {
    for (let x = 0; x < 16; x++) {
      const worldX = chunkX * 16 + x;
      const worldZ = chunkZ * 16 + z;
      heightData.push(generateHeightData(worldX, worldZ));
    }
  }

  return heightData;
}
```

### 7. Validation

```typescript
function isValidHeightData(data: HeightData): boolean {
  const [maxHeight, minHeight, groundLevel, waterHeight] = data;

  // Validate bounds
  if (minHeight < 0 || maxHeight > 255) return false;
  if (minHeight > maxHeight) return false;

  // Validate ground level is within range
  if (groundLevel < minHeight || groundLevel > maxHeight) return false;

  // Validate water height
  if (waterHeight < 0 || waterHeight > 255) return false;

  return true;
}
```

### 8. Network Optimization

```typescript
function serializeHeightData(data: HeightData): number[] {
  // Convert to plain array for JSON serialization
  return [...data];
}

function deserializeHeightData(data: number[]): HeightData | null {
  if (data.length !== 4) return null;

  const heightData: HeightData = [
    data[0], // maxHeight
    data[1], // minHeight
    data[2], // groundLevel
    data[3]  // waterHeight
  ];

  return isValidHeightData(heightData) ? heightData : null;
}
```

### 9. Client-Side Processing

```typescript
// Calculate optimal LOD based on height data
function calculateLOD(heightData: HeightData, cameraY: number): number {
  const [maxHeight, minHeight, groundLevel] = heightData;

  const distanceToGround = Math.abs(cameraY - groundLevel);
  const terrainComplexity = maxHeight - minHeight;

  if (distanceToGround > 100 || terrainComplexity < 5) {
    return 3; // Low detail
  } else if (distanceToGround > 50) {
    return 2; // Medium detail
  } else {
    return 1; // High detail
  }
}

// Determine chunk rendering priority
function getChunkPriority(chunk: ChunkDataTransferObject): number {
  let priority = 0;

  chunk.h.forEach(([maxHeight, minHeight, groundLevel, waterHeight]) => {
    // Higher priority for chunks with more variation
    priority += maxHeight - minHeight;

    // Higher priority for chunks with water
    if (waterHeight > groundLevel) {
      priority += 10;
    }
  });

  return priority / chunk.h.length;
}
```

### 10. Utility Functions

```typescript
// Get average ground level for chunk
function getAverageGroundLevel(heightData: HeightData[]): number {
  const sum = heightData.reduce(
    (acc, [, , groundLevel]) => acc + groundLevel,
    0
  );
  return sum / heightData.length;
}

// Check if chunk has water
function hasWater(heightData: HeightData[]): boolean {
  return heightData.some(
    ([, , groundLevel, waterHeight]) => waterHeight > groundLevel
  );
}

// Get height range for chunk
function getHeightRange(heightData: HeightData[]): [number, number] {
  let min = Infinity;
  let max = -Infinity;

  heightData.forEach(([maxHeight, minHeight]) => {
    min = Math.min(min, minHeight);
    max = Math.max(max, maxHeight);
  });

  return [min, max];
}

// Check if chunk is flat
function isFlat(heightData: HeightData[], threshold = 5): boolean {
  const [min, max] = getHeightRange(heightData);
  return max - min <= threshold;
}
```

## IDE Support

When you type `heightData[` in your IDE, you'll see:

```
heightData[0] - maxHeight: number
heightData[1] - minHeight: number
heightData[2] - groundLevel: number
heightData[3] - waterHeight: number
```

This makes the code self-documenting and reduces errors.

## Comparison: Before vs After

### Before (unnamed tuple)
```typescript
type HeightData = [number, number, number, number];

// What does each value mean? 🤔
const height = heightData[2];
```

### After (named tuple)
```typescript
type HeightData = readonly [
  maxHeight: number,
  minHeight: number,
  groundLevel: number,
  waterHeight: number
];

// Clear meaning! ✅
const [, , groundLevel] = heightData;
// or
const groundLevel = heightData[2]; // IDE shows: groundLevel: number
```

## Best Practices

### ✅ DO
- Use destructuring with clear names
- Validate height data bounds
- Use readonly to prevent mutations
- Document units (meters, blocks, etc.)

### ❌ DON'T
- Don't mutate HeightData (it's readonly)
- Don't access by magic indices without comments
- Don't skip validation when deserializing
- Don't mix up parameter order

## Summary

- **Named parameters** improve code readability
- **Readonly** prevents accidental mutations
- **Type-safe** ensures correct tuple length
- **IDE-friendly** with autocomplete support
- **Self-documenting** code
