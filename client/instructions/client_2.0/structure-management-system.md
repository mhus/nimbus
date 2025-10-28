# Structure Management System

## Übersicht

Dieses Dokument beschreibt ein System zur hierarchischen Organisation von Blöcken in logischen Strukturen (z.B. Gebäude, Räume, Möbel). Das System trennt visuelles Grouping (`groupId` am Block) von logischer Strukturierung (`structureId` am ServerBlock).

## Motivation

### Probleme der aktuellen Architektur

1. **Keine persistente Block-Identität**: Blöcke werden nur über Position (x,y,z) identifiziert
2. **Fehlende Struktur-Hierarchie**: Keine Möglichkeit, Gebäude in Stockwerke/Räume zu unterteilen
3. **Visuelle vs. Logische Gruppierung**: `groupId` ist für visuelle Effekte gedacht (z.B. alle Blöcke einer Gruppe leuchten auf), nicht für logische Strukturierung
4. **Position-basierte Keys sind fragil**: Wenn Blöcke verschoben werden, gehen Referenzen verloren

### Ziele

1. **Server-only UUID System**: Jeder Block erhält eine eindeutige, unveränderliche UUID (nur Server-side)
2. **Hierarchische Strukturen**: Gebäude → Stockwerke → Räume → Möbel
3. **Trennung von Concerns**:
   - `groupId` (Client-side) = Visuelles Grouping
   - `structureId` (Server-side) = Logische Zugehörigkeit
4. **Effizienter Client-Sync**: Client bekommt nur `blockId` + `groupId`, keine UUIDs
5. **Flexibilität**: Strukturen können umorganisiert werden ohne Blöcke zu ändern

## Architektur

### 1. Server-Only Block UUID System

```typescript
// Server-side only (nie zum Client senden!)
interface ServerBlock {
  uuid: string;              // Eindeutige UUID, unveränderlich
  position: Vector3;         // Aktuelle Position (kann sich ändern)
  blockId: number;          // Block-Typ
  groupId?: string;         // Visual grouping (Client-side)
  structureId?: string;     // Logische Struktur-Zugehörigkeit
  metadata?: any;           // Custom data
}
```

**Wichtig**: Die UUID wird **niemals** zum Client gesendet. Der Client arbeitet nur mit Position + `blockId` + `groupId` wie bisher.

### 2. Block Registry

```typescript
class BlockRegistry {
  private blocksByUUID = new Map<string, ServerBlock>();
  private blocksByPosition = new Map<string, string>(); // "x,y,z" -> uuid

  registerBlock(block: ServerBlock): void {
    this.blocksByUUID.set(block.uuid, block);
    const key = this.positionKey(block.position);
    this.blocksByPosition.set(key, block.uuid);
  }

  getBlockByUUID(uuid: string): ServerBlock | undefined {
    return this.blocksByUUID.get(uuid);
  }

  getBlockByPosition(x: number, y: number, z: number): ServerBlock | undefined {
    const key = `${x},${y},${z}`;
    const uuid = this.blocksByPosition.get(key);
    return uuid ? this.blocksByUUID.get(uuid) : undefined;
  }

  // Block bewegen (Position ändert sich, UUID bleibt)
  moveBlock(uuid: string, newPos: Vector3): void {
    const block = this.blocksByUUID.get(uuid);
    if (!block) return;

    // Remove old position mapping
    const oldKey = this.positionKey(block.position);
    this.blocksByPosition.delete(oldKey);

    // Update position
    block.position = newPos;

    // Add new position mapping
    const newKey = this.positionKey(newPos);
    this.blocksByPosition.set(newKey, uuid);
  }

  removeBlock(uuid: string): void {
    const block = this.blocksByUUID.get(uuid);
    if (block) {
      const key = this.positionKey(block.position);
      this.blocksByPosition.delete(key);
      this.blocksByUUID.delete(uuid);
    }
  }

  private positionKey(pos: Vector3): string {
    return `${pos.x},${pos.y},${pos.z}`;
  }
}
```

### 3. Struktur-Hierarchie

```typescript
interface Structure {
  id: string;                    // "mansion-001", "room-001", etc.
  name: string;                  // "Haupthaus", "Wohnzimmer", etc.
  type: string;                  // "building", "room", "furniture", "natural"
  parentId?: string;             // Für Hierarchie
  children: string[];            // Child structure IDs
  blocks: string[];              // Block UUIDs (nicht Positionen!)
  bounds: BoundingBox;           // Cached für Performance
  metadata: StructureMetadata;
  tags: string[];                // ["player-built", "completed", etc.]
  createdAt: number;
  modifiedAt: number;
}

interface BoundingBox {
  min: { x: number; y: number; z: number };
  max: { x: number; y: number; z: number };
}

interface StructureMetadata {
  blockCount: number;
  materials: Record<number, number>;  // blockId -> count
  author?: string;
  description?: string;
  volume?: number;
}
```

**Hierarchie-Beispiel:**

```
Mansion (mansion-001)
├── Floor-1 (floor-001)
│   ├── Living-Room (room-001)
│   ├── Kitchen (room-002)
│   └── Hallway (hallway-001)
├── Floor-2 (floor-002)
│   ├── Bedroom-1 (room-003)
│   └── Bedroom-2 (room-004)
└── Staircase (stair-001)

Underground-Base (base-001)
├── Entrance (entrance-001)
├── Main-Hall (hall-001)
└── Storage-Room (storage-001)
```

### 4. Structure Manager

```typescript
class StructureManager {
  private structures = new Map<string, Structure>();
  private blockRegistry: BlockRegistry;

  constructor(blockRegistry: BlockRegistry) {
    this.blockRegistry = blockRegistry;
  }

  // Erstelle neue Struktur
  createStructure(
    name: string,
    type: string,
    parentId?: string
  ): Structure {
    const id = `${type}-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`;
    const structure: Structure = {
      id,
      name,
      type,
      parentId,
      children: [],
      blocks: [],
      bounds: this.emptyBounds(),
      metadata: { blockCount: 0, materials: {} },
      tags: [],
      createdAt: Date.now(),
      modifiedAt: Date.now()
    };

    this.structures.set(id, structure);

    // Zu Parent hinzufügen
    if (parentId) {
      const parent = this.structures.get(parentId);
      if (parent) {
        parent.children.push(id);
      }
    }

    return structure;
  }

  // Block zu Struktur hinzufügen
  addBlockToStructure(structureId: string, blockUUID: string): void {
    const structure = this.structures.get(structureId);
    const block = this.blockRegistry.getBlockByUUID(blockUUID);

    if (structure && block) {
      if (!structure.blocks.includes(blockUUID)) {
        structure.blocks.push(blockUUID);
        block.structureId = structureId;
        structure.metadata.blockCount++;
        this.updateStructureBounds(structureId);
        structure.modifiedAt = Date.now();
      }
    }
  }

  // Block von Struktur entfernen
  removeBlockFromStructure(structureId: string, blockUUID: string): void {
    const structure = this.structures.get(structureId);
    const block = this.blockRegistry.getBlockByUUID(blockUUID);

    if (structure && block) {
      const index = structure.blocks.indexOf(blockUUID);
      if (index !== -1) {
        structure.blocks.splice(index, 1);
        block.structureId = undefined;
        structure.metadata.blockCount--;
        this.updateStructureBounds(structureId);
        structure.modifiedAt = Date.now();
      }
    }
  }

  // Bounds neu berechnen (basierend auf aktuellen Block-Positionen)
  updateStructureBounds(structureId: string): void {
    const structure = this.structures.get(structureId);
    if (!structure) return;

    const positions = structure.blocks
      .map(uuid => this.blockRegistry.getBlockByUUID(uuid))
      .filter(block => block != null)
      .map(block => block!.position);

    if (positions.length === 0) {
      structure.bounds = this.emptyBounds();
      return;
    }

    structure.bounds = {
      min: {
        x: Math.min(...positions.map(p => p.x)),
        y: Math.min(...positions.map(p => p.y)),
        z: Math.min(...positions.map(p => p.z))
      },
      max: {
        x: Math.max(...positions.map(p => p.x)),
        y: Math.max(...positions.map(p => p.y)),
        z: Math.max(...positions.map(p => p.z))
      }
    };
  }

  // Hole alle Block-Positionen einer Struktur (inkl. Children)
  getBlockPositions(structureId: string, recursive = true): Vector3[] {
    const structure = this.structures.get(structureId);
    if (!structure) return [];

    let blockUUIDs = [...structure.blocks];

    if (recursive) {
      for (const childId of structure.children) {
        const childStructure = this.structures.get(childId);
        if (childStructure) {
          blockUUIDs.push(...childStructure.blocks);
        }
      }
    }

    return blockUUIDs
      .map(uuid => this.blockRegistry.getBlockByUUID(uuid))
      .filter(block => block != null)
      .map(block => block!.position);
  }

  // Hole alle Block-UUIDs einer Struktur (rekursiv)
  getBlockUUIDs(structureId: string, recursive = true): string[] {
    const structure = this.structures.get(structureId);
    if (!structure) return [];

    let blockUUIDs = [...structure.blocks];

    if (recursive) {
      for (const childId of structure.children) {
        blockUUIDs.push(...this.getBlockUUIDs(childId, true));
      }
    }

    return blockUUIDs;
  }

  // Finde Struktur an Position (durch Block-groupId)
  findStructureAtPosition(
    x: number,
    y: number,
    z: number
  ): Structure | null {
    const block = this.blockRegistry.getBlockByPosition(x, y, z);
    if (!block || !block.structureId) return null;

    return this.structures.get(block.structureId) || null;
  }

  // Lösche Struktur (und optionally Children)
  deleteStructure(structureId: string, deleteChildren = false): void {
    const structure = this.structures.get(structureId);
    if (!structure) return;

    // Remove from parent
    if (structure.parentId) {
      const parent = this.structures.get(structure.parentId);
      if (parent) {
        const index = parent.children.indexOf(structureId);
        if (index !== -1) {
          parent.children.splice(index, 1);
        }
      }
    }

    // Handle children
    if (deleteChildren) {
      for (const childId of structure.children) {
        this.deleteStructure(childId, true);
      }
    } else {
      // Move children to parent
      for (const childId of structure.children) {
        const child = this.structures.get(childId);
        if (child) {
          child.parentId = structure.parentId;
          if (structure.parentId) {
            const parent = this.structures.get(structure.parentId);
            if (parent) {
              parent.children.push(childId);
            }
          }
        }
      }
    }

    // Remove structureId from blocks
    for (const blockUUID of structure.blocks) {
      const block = this.blockRegistry.getBlockByUUID(blockUUID);
      if (block) {
        block.structureId = undefined;
      }
    }

    this.structures.delete(structureId);
  }

  private emptyBounds(): BoundingBox {
    return {
      min: { x: 0, y: 0, z: 0 },
      max: { x: 0, y: 0, z: 0 }
    };
  }
}
```

### 5. Performance: Räumlicher Index

Für schnelle räumliche Queries (z.B. "Alle Strukturen in Radius X um Position Y"):

```typescript
class SpatialBlockIndex {
  // Chunk-basierter Index für schnelle räumliche Queries
  private chunkSize = 32;
  private chunks = new Map<string, Set<string>>(); // chunkKey -> block UUIDs

  private getChunkKey(x: number, y: number, z: number): string {
    const cx = Math.floor(x / this.chunkSize);
    const cy = Math.floor(y / this.chunkSize);
    const cz = Math.floor(z / this.chunkSize);
    return `${cx},${cy},${cz}`;
  }

  addBlock(uuid: string, pos: Vector3): void {
    const key = this.getChunkKey(pos.x, pos.y, pos.z);
    if (!this.chunks.has(key)) {
      this.chunks.set(key, new Set());
    }
    this.chunks.get(key)!.add(uuid);
  }

  removeBlock(uuid: string, pos: Vector3): void {
    const key = this.getChunkKey(pos.x, pos.y, pos.z);
    const chunk = this.chunks.get(key);
    if (chunk) {
      chunk.delete(uuid);
      if (chunk.size === 0) {
        this.chunks.delete(key);
      }
    }
  }

  getBlocksInRadius(center: Vector3, radius: number): string[] {
    const results = new Set<string>();

    // Berechne betroffene Chunks
    const minChunkX = Math.floor((center.x - radius) / this.chunkSize);
    const maxChunkX = Math.floor((center.x + radius) / this.chunkSize);
    const minChunkY = Math.floor((center.y - radius) / this.chunkSize);
    const maxChunkY = Math.floor((center.y + radius) / this.chunkSize);
    const minChunkZ = Math.floor((center.z - radius) / this.chunkSize);
    const maxChunkZ = Math.floor((center.z + radius) / this.chunkSize);

    // Iteriere nur über relevante Chunks
    for (let cx = minChunkX; cx <= maxChunkX; cx++) {
      for (let cy = minChunkY; cy <= maxChunkY; cy++) {
        for (let cz = minChunkZ; cz <= maxChunkZ; cz++) {
          const key = `${cx},${cy},${cz}`;
          const chunk = this.chunks.get(key);
          if (chunk) {
            chunk.forEach(uuid => results.add(uuid));
          }
        }
      }
    }

    return Array.from(results);
  }
}
```

## Integration in VoxelServer

```typescript
class VoxelServer {
  private blockRegistry = new BlockRegistry();
  private structureManager = new StructureManager(this.blockRegistry);
  private spatialIndex = new SpatialBlockIndex();

  // Block setzen (mit optionaler Struktur-Zuordnung)
  setBlock(
    x: number,
    y: number,
    z: number,
    blockId: number,
    options?: {
      groupId?: string,
      structureId?: string
    }
  ): string {
    // UUID generieren
    const uuid = crypto.randomUUID();

    // Server-Block erstellen
    const serverBlock: ServerBlock = {
      uuid,
      position: { x, y, z },
      blockId,
      groupId: options?.groupId,
      structureId: options?.structureId
    };

    // In Registry eintragen
    this.blockRegistry.registerBlock(serverBlock);
    this.spatialIndex.addBlock(uuid, serverBlock.position);

    // Zu Struktur hinzufügen wenn angegeben
    if (options?.structureId) {
      this.structureManager.addBlockToStructure(options.structureId, uuid);
    }

    // Zum Client NUR blockId + groupId senden (wie bisher)
    this.broadcastBlockChange(x, y, z, blockId, options?.groupId);

    return uuid; // Nur für Server-side Operationen
  }

  // Block entfernen
  removeBlock(x: number, y: number, z: number): void {
    const block = this.blockRegistry.getBlockByPosition(x, y, z);
    if (!block) return;

    // Von Struktur entfernen
    if (block.structureId) {
      this.structureManager.removeBlockFromStructure(block.structureId, block.uuid);
    }

    // Aus Indices entfernen
    this.spatialIndex.removeBlock(block.uuid, block.position);
    this.blockRegistry.removeBlock(block.uuid);

    // Zum Client senden
    this.broadcastBlockChange(x, y, z, 0); // blockId 0 = Air
  }

  // Existierende Methode für Broadcast (keine Änderung nötig)
  private broadcastBlockChange(
    x: number,
    y: number,
    z: number,
    blockId: number,
    groupId?: string
  ): void {
    // Sendet {"t": "b.u", "d": [BlockData]} an alle Clients
    // BlockData enthält nur: x, y, z, blockId, groupId
    // KEINE UUID!
  }
}
```

## Persistierung

```typescript
interface WorldSave {
  blocks: Array<{
    uuid: string;
    x: number;
    y: number;
    z: number;
    blockId: number;
    groupId?: string;
    structureId?: string;
  }>;
  structures: Structure[];
}

class WorldPersistence {
  constructor(
    private blockRegistry: BlockRegistry,
    private structureManager: StructureManager
  ) {}

  save(path: string): void {
    const blocks = Array.from(
      this.blockRegistry['blocksByUUID'].values()
    ).map(b => ({
      uuid: b.uuid,
      x: b.position.x,
      y: b.position.y,
      z: b.position.z,
      blockId: b.blockId,
      groupId: b.groupId,
      structureId: b.structureId
    }));

    const structures = Array.from(
      this.structureManager['structures'].values()
    );

    const data: WorldSave = { blocks, structures };

    fs.writeFileSync(path, JSON.stringify(data, null, 2));
  }

  load(path: string): void {
    const data: WorldSave = JSON.parse(fs.readFileSync(path, 'utf-8'));

    // Restore blocks
    for (const blockData of data.blocks) {
      this.blockRegistry.registerBlock({
        uuid: blockData.uuid,
        position: { x: blockData.x, y: blockData.y, z: blockData.z },
        blockId: blockData.blockId,
        groupId: blockData.groupId,
        structureId: blockData.structureId
      });
    }

    // Restore structures
    for (const structure of data.structures) {
      this.structureManager['structures'].set(structure.id, structure);
    }
  }
}
```

## MCP Integration

### Neue MCP Tools

```typescript
// Tool 1: Erstelle neue Struktur
{
  name: "create_structure",
  description: "Create a new logical structure (building, room, etc.)",
  inputSchema: {
    type: "object",
    properties: {
      name: { type: "string", description: "Structure name" },
      type: {
        type: "string",
        enum: ["building", "room", "furniture", "natural"],
        description: "Structure type"
      },
      parentId: {
        type: "string",
        description: "Parent structure ID (optional)"
      }
    },
    required: ["name", "type"]
  }
}

// Tool 2: Füge Blöcke zu Struktur hinzu
{
  name: "add_blocks_to_structure",
  description: "Add blocks to a structure",
  inputSchema: {
    type: "object",
    properties: {
      structureId: { type: "string" },
      blocks: {
        type: "array",
        items: {
          type: "object",
          properties: {
            x: { type: "number" },
            y: { type: "number" },
            z: { type: "number" }
          }
        }
      }
    },
    required: ["structureId", "blocks"]
  }
}

// Tool 3: Hole Struktur-Informationen
{
  name: "get_structure_info",
  description: "Get information about a structure",
  inputSchema: {
    type: "object",
    properties: {
      structureId: { type: "string" },
      includeChildren: {
        type: "boolean",
        default: false,
        description: "Include child structures"
      }
    },
    required: ["structureId"]
  }
}

// Tool 4: Liste alle Strukturen
{
  name: "list_structures",
  description: "List all structures in the world",
  inputSchema: {
    type: "object",
    properties: {
      type: {
        type: "string",
        description: "Filter by structure type (optional)"
      },
      parentId: {
        type: "string",
        description: "Filter by parent structure (optional)"
      }
    }
  }
}

// Tool 5: Finde Struktur an Position
{
  name: "find_structure_at_position",
  description: "Find structure at a specific position",
  inputSchema: {
    type: "object",
    properties: {
      x: { type: "number" },
      y: { type: "number" },
      z: { type: "number" }
    },
    required: ["x", "y", "z"]
  }
}

// Tool 6: Lösche Struktur
{
  name: "delete_structure",
  description: "Delete a structure",
  inputSchema: {
    type: "object",
    properties: {
      structureId: { type: "string" },
      deleteChildren: {
        type: "boolean",
        default: false,
        description: "Also delete child structures"
      }
    },
    required: ["structureId"]
  }
}
```

## IPC Erweiterung

```typescript
// IPCServer.ts - Neue Befehle

class IPCServer {
  // ...

  private async handleRequest(method: string, params: any): Promise<any> {
    switch (method) {
      // ... existing methods ...

      case 'createStructure':
        return this.createStructure(params);

      case 'addBlocksToStructure':
        return this.addBlocksToStructure(params);

      case 'getStructureInfo':
        return this.getStructureInfo(params);

      case 'listStructures':
        return this.listStructures(params);

      case 'findStructureAtPosition':
        return this.findStructureAtPosition(params);

      case 'deleteStructure':
        return this.deleteStructure(params);

      default:
        throw new Error(`Unknown method: ${method}`);
    }
  }

  private async createStructure(params: {
    name: string;
    type: string;
    parentId?: string;
  }): Promise<Structure> {
    return this.voxelServer.structureManager.createStructure(
      params.name,
      params.type,
      params.parentId
    );
  }

  private async addBlocksToStructure(params: {
    structureId: string;
    blocks: Array<{x: number; y: number; z: number}>;
  }): Promise<{ added: number }> {
    let added = 0;

    for (const pos of params.blocks) {
      const block = this.voxelServer.blockRegistry.getBlockByPosition(
        pos.x, pos.y, pos.z
      );

      if (block) {
        this.voxelServer.structureManager.addBlockToStructure(
          params.structureId,
          block.uuid
        );
        added++;
      }
    }

    return { added };
  }

  private async getStructureInfo(params: {
    structureId: string;
    includeChildren?: boolean;
  }): Promise<any> {
    const structure = this.voxelServer.structureManager['structures'].get(
      params.structureId
    );

    if (!structure) {
      throw new Error(`Structure ${params.structureId} not found`);
    }

    const positions = this.voxelServer.structureManager.getBlockPositions(
      params.structureId,
      params.includeChildren || false
    );

    return {
      ...structure,
      blocks: positions, // Return positions instead of UUIDs
      blockCount: positions.length
    };
  }

  private async listStructures(params: {
    type?: string;
    parentId?: string;
  }): Promise<Structure[]> {
    const allStructures = Array.from(
      this.voxelServer.structureManager['structures'].values()
    );

    let filtered = allStructures;

    if (params.type) {
      filtered = filtered.filter(s => s.type === params.type);
    }

    if (params.parentId) {
      filtered = filtered.filter(s => s.parentId === params.parentId);
    }

    return filtered;
  }

  private async findStructureAtPosition(params: {
    x: number;
    y: number;
    z: number;
  }): Promise<Structure | null> {
    return this.voxelServer.structureManager.findStructureAtPosition(
      params.x,
      params.y,
      params.z
    );
  }

  private async deleteStructure(params: {
    structureId: string;
    deleteChildren?: boolean;
  }): Promise<{ success: boolean }> {
    this.voxelServer.structureManager.deleteStructure(
      params.structureId,
      params.deleteChildren || false
    );

    return { success: true };
  }
}
```

## Verwendungsbeispiele

### Beispiel 1: Mansion bauen und strukturieren

```typescript
// 1. Erstelle Haupt-Struktur
const mansion = await ipcClient.createStructure({
  name: "Grand Mansion",
  type: "building"
});

// 2. Erstelle Stockwerke
const floor1 = await ipcClient.createStructure({
  name: "Ground Floor",
  type: "room",
  parentId: mansion.id
});

const floor2 = await ipcClient.createStructure({
  name: "Second Floor",
  type: "room",
  parentId: mansion.id
});

// 3. Baue Mansion (setzt Blöcke)
const blocks = buildMansion(playerPos); // Returns block positions

// 4. Ordne Blöcke Stockwerken zu
await ipcClient.addBlocksToStructure({
  structureId: floor1.id,
  blocks: blocks.filter(b => b.y >= baseY && b.y < baseY + 6)
});

await ipcClient.addBlocksToStructure({
  structureId: floor2.id,
  blocks: blocks.filter(b => b.y >= baseY + 6 && b.y < baseY + 12)
});

// 5. Später: Finde Struktur an Position
const structure = await ipcClient.findStructureAtPosition({
  x: playerPos.x,
  y: playerPos.y,
  z: playerPos.z
});

console.log(`You are in: ${structure.name}`);
```

### Beispiel 2: MCP-gesteuerte Struktur-Erweiterung

```typescript
// MCP Tool kann jetzt sagen:
// "Erweitere das Mansion um ein drittes Stockwerk"

// 1. Finde Mansion
const structures = await ipcClient.listStructures({ type: "building" });
const mansion = structures.find(s => s.name.includes("Mansion"));

// 2. Erstelle neues Stockwerk
const floor3 = await ipcClient.createStructure({
  name: "Third Floor",
  type: "room",
  parentId: mansion.id
});

// 3. Baue Blöcke für drittes Stockwerk
const existingBounds = mansion.bounds;
const newBlocks = buildFloor(existingBounds.max.y + 1);

// 4. Ordne zu
await ipcClient.addBlocksToStructure({
  structureId: floor3.id,
  blocks: newBlocks
});
```

## Vorteile

1. ✅ **UUID stabil**: Block kann bewegt werden, UUID bleibt
2. ✅ **Kein Overhead**: Client bekommt nur `blockId` + `groupId` (wie bisher)
3. ✅ **Logische Trennung**: `groupId` (visual) vs `structureId` (logical)
4. ✅ **Flexibel**: Strukturen können reorganisiert werden
5. ✅ **Performance**: Spatial index für schnelle Lookups
6. ✅ **Persistierung**: UUIDs bleiben über Server-Restarts erhalten
7. ✅ **MCP-Integration**: KI kann Strukturen verstehen und manipulieren
8. ✅ **Hierarchie**: Beliebig tiefe Verschachtelung möglich

## Implementierungs-Roadmap

### Phase 1: Basis-Infrastruktur
- [ ] `BlockRegistry.ts` implementieren
- [ ] `StructureManager.ts` implementieren
- [ ] Integration in `VoxelServer.ts`
- [ ] Tests für Block UUID System

### Phase 2: Persistierung
- [ ] `WorldPersistence.ts` implementieren
- [ ] Save/Load für Blöcke mit UUID
- [ ] Save/Load für Strukturen
- [ ] Migration von alten Worlds

### Phase 3: IPC Integration
- [ ] IPC-Befehle für Struktur-Management
- [ ] IPCClient-Methoden
- [ ] Tests für IPC-Kommunikation

### Phase 4: MCP Tools
- [ ] MCP-Tools für Struktur-Operationen
- [ ] Dokumentation für MCP-Usage
- [ ] Beispiel-Scripts

### Phase 5: Performance-Optimierung
- [ ] `SpatialBlockIndex.ts` implementieren
- [ ] Performance-Tests
- [ ] Optimierung der Bounds-Berechnung

### Phase 6: Features
- [ ] Auto-Detection von Strukturen (zusammenhängende Blöcke)
- [ ] Struktur-Templates (speichern & laden)
- [ ] Struktur-Kopieren/Verschieben
- [ ] Struktur-Export/Import

## Offene Fragen

1. **Material-Zählung**: Soll automatisch bei jedem Block-Add/Remove aktualisiert werden?
2. **Structure Tags**: Welche Standard-Tags brauchen wir?
3. **Auto-Detection**: Algorithmus für automatisches Erkennen von Strukturen?
4. **Migration**: Wie migrieren wir existierende Worlds?
5. **Undo/Redo**: Wie integriert sich das mit Structure-System?

## Referenzen

- `network-model-2.0.md`: Netzwerk-Protokoll (UUID wird nie gesendet)
- `object-model-2.0.md`: Block-Model (BlockMetadata hat `groupId`)
- `migration-playground.md`: Services und Architektur
