# Nimbus MCP Server

The Nimbus MCP (Model Context Protocol) Server provides tools for Claude to directly interact with the Nimbus voxel server.

## Features

The MCP server exposes the following tools:

### 1. **create_block**
Create a new block type in the registry.

**Parameters:**
- `name` (required): Internal name (e.g., "custom_stone")
- `displayName` (required): Display name (e.g., "Custom Stone")
- `texture` (required): Texture path (e.g., "stone.png" or "models/tree.babylon")
- `shape` (optional): Block shape (0=CUBE, 1=CROSS, 2=HASH, 3=MODEL, etc.)
- `solid` (optional): Whether block has collision (default: true)
- `transparent` (optional): Whether block is transparent (default: false)
- `hardness` (optional): Mining hardness (default: 1)

**Example:**
```json
{
  "name": "my_custom_block",
  "displayName": "My Custom Block",
  "texture": "stone.png",
  "shape": 0,
  "solid": true,
  "hardness": 2
}
```

### 2. **place_block**
Place a block at specific coordinates with optional modifiers.

**Parameters:**
- `blockName` (required): Name of the block to place
- `x` (required): X coordinate
- `y` (required): Y coordinate (height)
- `z` (required): Z coordinate
- `worldName` (optional): World name (default: "main")
- `modifier` (optional): Block modifier object with properties:
  - `rotationX` (number): Rotation around X axis in degrees (vertical tilt)
  - `rotationY` (number): Rotation around Y axis in degrees (horizontal rotation)
  - `scale` ([x, y, z]): Scale factors, default [1.0, 1.0, 1.0]
  - `color` ([r, g, b]): Color tint 0-255, default [255, 255, 255]
  - `texture` (string): Override texture path
  - `facing` (number): Facing direction (0=North, 1=East, 2=South, 3=West)

**Example (simple):**
```json
{
  "blockName": "stone",
  "x": 10,
  "y": 64,
  "z": 20
}
```

**Example (with modifiers):**
```json
{
  "blockName": "stone",
  "x": 10,
  "y": 64,
  "z": 20,
  "modifier": {
    "rotationY": 45,
    "scale": [1.5, 1.5, 1.5],
    "color": [255, 0, 0]
  }
}
```

**Example (MODEL block with rotation and scale):**
```json
{
  "blockName": "skull",
  "x": 10,
  "y": 64,
  "z": 20,
  "modifier": {
    "rotationY": 90,
    "rotationX": -15,
    "scale": [2.0, 2.0, 2.0]
  }
}
```

### 3. **get_block**
Get information about a block at specific coordinates.

**Parameters:**
- `x` (required): X coordinate
- `y` (required): Y coordinate
- `z` (required): Z coordinate
- `worldName` (optional): World name (default: "main")

### 4. **list_blocks**
List all registered block types.

**Parameters:**
- `filter` (optional): Filter string to search block names

### 5. **remove_block**
Remove a block (place air) at specific coordinates.

**Parameters:**
- `x` (required): X coordinate
- `y` (required): Y coordinate
- `z` (required): Z coordinate
- `worldName` (optional): World name (default: "main")

### 6. **get_block_definition**
Get the complete definition and properties of a block type by name.

**Parameters:**
- `blockName` (required): Name of the block to get definition for

**Example:**
```json
{
  "blockName": "stone"
}
```

**Returns:**
```json
{
  "blockId": 1,
  "blockDefinition": {
    "id": 1,
    "name": "stone",
    "displayName": "Stone",
    "shape": 0,
    "texture": "stone.png",
    "solid": true,
    "transparent": false,
    "hardness": 1.5,
    "miningtime": 1500,
    "tool": "pickaxe",
    "unbreakable": false,
    "options": {}
  }
}
```

### 7. **get_player_position**
Get the current position and rotation of the player.

**Parameters:**
- `worldName` (optional): World name (default: "main")

**Returns:**
```json
{
  "playerId": "uuid-here",
  "position": [10, 64, 20],
  "rotation": 45.0,
  "pitch": -15.0,
  "chunkId": [0, 0],
  "world": "main"
}
```

## Running the MCP Server

### Option 1: Integrated Mode (Recommended for Live Server)

The MCP server is now integrated into the main VoxelServer. Enable it by setting `enableMCP: true` in the server config:

```typescript
const server = new VoxelServer({
  port: 3003,
  worldName: 'main',
  worldSeed: 12345,
  generator: 'flat',
  enableMCP: true,  // Enable MCP server integration
});
```

When enabled, the MCP server will start automatically with the VoxelServer and have access to:
- Live player positions
- Real-time world data
- Connected entities
- All runtime server state

**Note:** The integrated MCP server runs on stdio and cannot be accessed while the main server is running in a terminal. Use standalone mode for Claude Code integration.

### Option 2: Standalone Mode (Recommended for Claude Code)

Run the MCP server as a standalone process:

```bash
cd packages/server
pnpm run mcp
```

This starts a minimal Voxel server instance just for MCP tools, without starting the WebSocket server. This mode is ideal for Claude Code integration as it doesn't require the main server to be running.

## Configuring Claude Code

To use the Nimbus MCP server with Claude Code, add it to your Claude Code MCP configuration:

### macOS/Linux
Edit `~/.config/claude-code/mcp.json`:

```json
{
  "mcpServers": {
    "nimbus": {
      "command": "node",
      "args": [
        "/path/to/nimbus/client/packages/server/dist/mcp/standalone.js"
      ]
    }
  }
}
```

Or use tsx for development:

```json
{
  "mcpServers": {
    "nimbus": {
      "command": "pnpm",
      "args": [
        "--dir",
        "/path/to/nimbus/client/packages/server",
        "run",
        "mcp"
      ]
    }
  }
}
```

### Windows
Edit `%APPDATA%\claude-code\mcp.json` with similar configuration.

## Development

### Building

```bash
pnpm run build
```

### Testing

You can test the MCP server using the MCP Inspector:

```bash
npx @modelcontextprotocol/inspector pnpm --dir packages/server run mcp
```

## Example Usage

Once configured, Claude can use these tools directly:

**User:** "Create a new custom stone block"
**Claude:** *Uses create_block tool*

**User:** "Place that block at coordinates 10, 64, 20"
**Claude:** *Uses place_block tool*

**User:** "What block is at position 10, 64, 20?"
**Claude:** *Uses get_block tool*

**User:** "List all blocks with 'stone' in the name"
**Claude:** *Uses list_blocks tool with filter*

**User:** "Show me the definition of the stone block"
**Claude:** *Uses get_block_definition tool*

**User:** "Where is the player?"
**Claude:** *Uses get_player_position tool*

## Block Shapes Reference

- `0` - CUBE: Standard cube block
- `1` - CROSS: Cross model (plants, flowers)
- `2` - HASH: Hash-like construct with offset edges
- `3` - MODEL: Custom 3D model (.babylon file)
- `4` - GLASS: Transparent cube
- `5` - FLAT: Flat block (carpet, water surface)
- `6` - SPHERE: Sphere shape
- `7` - COLUMN: Cylinder shape
- `8` - ROUND_CUBE: Cube with rounded edges
- `9` - STEPS: Stair/treppen form
- `10` - STAIR: Filled stair
- `11` - BILLBOARD: Flat textured quad (camera-facing)
- `12` - SPRITE: Cross-style 2D sprite
- `13` - FLAME: Animated flame effect

## Troubleshooting

### MCP server not connecting
- Ensure the server is built: `pnpm run build`
- Check the path in your MCP configuration
- Try using `pnpm run mcp` to test the server directly

### Tool calls failing
- Check that the Voxel server is properly initialized
- Verify block names exist in the registry using `list_blocks`
- Ensure coordinates are valid (Y should be 0-255)

## Future Enhancements

- [ ] Support for block modifiers (rotation, scale, color)
- [ ] Batch block placement (fill region)
- [ ] World management (create/delete worlds)
- [ ] Player management
- [ ] Entity spawning
- [ ] Asset management (upload textures/models)
