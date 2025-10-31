# Claude AI Development Documentation - Nimbus Client 2.0

This document contains important information for Claude AI when working on the Nimbus Client 2.0 codebase.

## Project Context

This is the **Version 2.0** of the Nimbus voxel engine client. It is a complete rewrite based on learnings from the prototype in `client_playground`.

### Key Principles

1. **Do NOT create code autonomously** in the `client` folder unless explicitly requested in tasks
2. **Do provide feedback and suggestions** for improvements that can be discussed
3. **Reference the prototype** in `client_playground` for context and learnings
4. **Implement step-by-step** - one area at a time, incorporating improvements from the prototype

## Technology Stack

### Frontend 3D Client
- **TypeScript** - Type-safe development
- **Vite** - Build tool and dev server
- **Babylon.js** - 3D rendering engine
- **Jest** - Unit testing
- **pnpm** - Package manager

### Frontend Forms (Future)
- **TypeScript** + **Vite** + **Vue 3** + **Tailwind CSS**
- Will be integrated via iframe later

### Package Structure

```
client/
├── packages/
│   ├── shared/      # Shared types, protocols, utils (client + server)
│   ├── client/      # 3D Engine, UI, Input handling, Editor
│   └── server/      # Test server for client development
```

### Build Variants

The client uses **conditional compilation** with Vite to produce two separate builds from a single codebase. Unreachable code is eliminated by the bundler (tree-shaking).

- **viewer**: Read-only 3D engine for viewing worlds
  - Build command: `pnpm build:viewer`
  - Output: `dist/viewer/`
  - Global constant: `__EDITOR__ = false`, `__VIEWER__ = true`

- **editor**: Full 3D engine + editor functions + console
  - Build command: `pnpm build:editor`
  - Output: `dist/editor/`
  - Global constant: `__EDITOR__ = true`, `__VIEWER__ = false`

**Implementation:**
```typescript
// vite.config.ts - Injects global constants
export default defineConfig(({ mode }) => ({
  define: {
    __EDITOR__: JSON.stringify(mode === 'editor'),
    __VIEWER__: JSON.stringify(mode === 'viewer' || mode === 'development'),
    __BUILD_MODE__: JSON.stringify(mode),
  },
}));

// Code example - Editor code is tree-shaken in viewer build
if (__EDITOR__) {
  // This code is completely removed from viewer build
  console.log('Editor mode active');
  // Initialize EditorService, CommandConsole, etc.
}
```

**Development:**
- `pnpm dev` - Run viewer mode (default)
- `pnpm dev:editor` - Run editor mode

**Build:**
- `pnpm build` - Build both variants + TypeScript declarations
- `pnpm build:viewer` - Build only viewer
- `pnpm build:editor` - Build only editor

## Terminology / Begriffsdefinitionen

To avoid confusion, we use the following terms consistently:

### Block-Related Terms

- **BlockType** or **Type**: Block-Definition (Definition/Template)
  - The definition/template of a block type
  - Contains: id, name, shape, texture, material, properties
  - Stored in the Registry
  - Examples: "grass", "stone", "water"
  - **German UI**: "Blocktyp"

- **BlockInstance** or **Instance** or **Block**: Block-Instanz (Concrete instance in the world)
  - A concrete block at a specific position (x, y, z) in the world
  - Contains only a BlockType ID (references the BlockType)
  - Stored in chunk data arrays
  - Example: The grass block at position (10, 64, 5)
  - **German UI**: "Blockinstanz" or "Block"

### Naming Conventions

- Use **English terms** in code and code comments
- Use **German terms** in user-facing UI where appropriate
- Be consistent with terminology across the codebase

#### Coordinate Naming

Always use consistent coordinate naming to avoid confusion:

- **World Coordinates**: `x`, `y`, `z` - Absolute position in the world
- **Chunk Coordinates**: `cx`, `cy`, `cz` - Chunk position in chunk space
- **Local Coordinates**: `lx`, `ly`, `lz` - Position within a chunk (avoid if possible, prefer clear context)

**Examples:**
```typescript
// ✅ Good - Clear coordinate types
interface Block {
  x: number;  // World coordinate
  y: number;
  z: number;
}

interface Chunk {
  cx: number;  // Chunk coordinate
  cz: number;
}

// ✅ Good - Function parameters with clear naming
function getBlock(worldX: number, worldY: number, worldZ: number): Block;
function getChunk(chunkX: number, chunkZ: number): Chunk;

// ❌ Avoid - Ambiguous coordinate names
interface BadBlock {
  x: number;  // World or local?
  chunkX: number;  // Should be cx
}

// ✅ Good - Local coordinates in context
function setBlockInChunk(chunk: Chunk, localX: number, localY: number, localZ: number, blockId: number) {
  // Clear that these are local coordinates
  const index = localX + localZ * chunk.size + localY * chunk.size * chunk.size;
  chunk.blocks[index] = blockId;
}
```

**Conversion helpers should make intent clear:**
```typescript
// Convert world to chunk coordinates
function worldToChunk(worldX: number, worldZ: number, chunkSize: number): { cx: number; cz: number } {
  return {
    cx: Math.floor(worldX / chunkSize),
    cz: Math.floor(worldZ / chunkSize)
  };
}

// Convert world to local coordinates within chunk
function worldToLocal(worldX: number, worldZ: number, chunkSize: number): { localX: number; localZ: number } {
  return {
    localX: worldX % chunkSize,
    localZ: worldZ % chunkSize
  };
}
```

## Architecture Overview

### Core Concepts

#### AppContext
Central context containing references to all services, configurations, and resources the client needs.

```typescript
interface AppContext {
  services: {
    network: NetworkService;
    input: InputService;
    // ... other services
  };
  config: ClientConfig;
  serverInfo: ServerInfo;
  worldInfo: WorldInfo;
  // ... other context
}
```

#### Services Architecture
Services follow a singleton pattern and are registered in AppContext. See `migration-concepts.md` for detailed service architecture.

Key services:
- **NetworkService**: WebSocket connection management
- **InputService**: Input handling and action mapping
- **ChunkService**: Chunk loading and management
- **RenderService**: Babylon.js scene and rendering
- **BlockTypeService**: Block type registry

#### Input System
- **InputController**: Abstract controller that receives input events from the system
  - Implementations: `BrowserInputController`, `XBoxInputController`, etc.
  - Provides slots for actions (e.g., "key_q", "mouse_left", etc.)
- **InputAction**: Abstract action class
  - Implementations: `MoveForwardAction`, `JumpAction`, etc.
  - Supports both discrete (keys) and continuous (mouse, analog stick) inputs
- **InputService**: Manages controller and action bindings (singleton)

#### Networking
- **NetworkService**: Manages WebSocket connection and message routing (singleton) and Rest requests to the Server Rest API
- **MessageHandler**: Abstract message handler class
  - Implementations: `LoginMessageHandler`, `ChunkMessageHandler`, etc.
  - Handles messages of a specific type
  - located in `client/network/handlers` and registered by the `NetworkService`
- Messages will be JSON-serialized and sent over the WebSocket connection
- **RestAction**: Abstract class for REST API calls
  - Implementations: `FetchWorldsAction`, etc.
  - located in `client/network/actions` and hold by the `NetworkService`
  - DTOs are defined in `client/shared/network/dto`

### 3D Engine
- **EngineService**: Core 3D engine class that initializes and manages Babylon.js, the scene, and rendering (singleton)
  - It holds all the other services that are directly related to the 3D engine, e.g., `RenderService`
- **NetworkService**: Manages WebSocket connection and message routing (singleton, AppContext)
- **RenderService**: Manages Babylon.js scene and rendering (singleton, Engine)
  - **IMPORTANT**: RenderService MUST work with `ClientChunk` and `ClientBlock`, NOT with `ChunkDataTransferObject`
  - ClientChunk contains optimized data: cached BlockType references, merged modifiers, ClientBlockType
  - Never convert back to ChunkDataTransferObject for rendering
  - `renderChunk(clientChunk: ClientChunk)` iterates over `clientChunk.data.data` Map (ClientBlocks)
  - Uses `clientBlock.currentModifier` (already merged) instead of registry lookups
- **ChunkService**: Manages chunk loading and management (singleton, AppContext)
  - Converts ChunkDataTransferObject → ClientChunk (with optimizations)
  - Maintains ClientChunk Map as single source of truth
  - Block updates modify ClientBlock Map, then emit 'chunk:updated' event
- **BlockTypeService**: Manages block type registry (singleton, AppContext)
- **WorldService**: Manages world selection and loading (singleton, AppContext)
- **CameraControl**: Manages camera movement and rotation (singleton, Engine)
- **InputService**: Manages input handling and action mapping (singleton, AppContext)
- **ChunkLoader**: Manages chunk loading and management (singleton, AppContext)
- **ChunkRenderer**: Renders chunks (singleton, Engine)
- **ChunkMeshGenerator**: Generates meshes for chunks (singleton, Engine)
- **EnvironmentRenderer**: Renders environment (singleton, Engine)
- **ShaderService**: Manages shader loading and management (singleton, Engine)
- **TextureService**: Manages texture loading and management (singleton, Engine)

### Data Flow

Samples:
1. **Network Messages** → NetworkService → Message Handlers
2. **User Input** → InputController → InputActions → Game Logic
3. **World Data** → ChunkService → RenderService → Babylon.js Scene
4. **Block Registry** → RegistryService → Type System

#### Chunk and Block Update Flow

**IMPORTANT: Data Layer Separation**

1. **Network Layer** (ChunkDataTransferObject, Block)
   - Server sends: ChunkDataTransferObject with Block array
   - Minimal data for network efficiency
   - Only IDs (blockTypeId), no resolved references

2. **Client Layer** (ClientChunk, ClientBlock)
   - ChunkService converts: ChunkDataTransferObject → ClientChunk
   - ClientBlock contains optimizations:
     - `blockType: BlockType` - Cached reference (not just ID)
     - `currentModifier: BlockModifier` - Merged from Block + BlockType
     - `clientBlockType: ClientBlockType` - Pre-processed for rendering
   - Single source of truth: `ClientChunk.data.data` Map

3. **Rendering Layer** (RenderService)
   - **MUST use ClientChunk/ClientBlock** (NOT ChunkDataTransferObject!)
   - Iterates over ClientBlock Map: `clientChunk.data.data.values()`
   - Uses cached references: `clientBlock.blockType`, `clientBlock.currentModifier`
   - Never do registry lookups during rendering

**Block Update Flow:**
```
REST API → BlockUpdateBuffer (1s batch) → WebSocket b.u message
  → BlockUpdateHandler → ChunkService.onBlockUpdate()
  → Update ClientBlock Map → Emit 'chunk:updated'
  → RenderService.onChunkUpdated() → renderChunk(clientChunk)
  → Render from ClientBlock Map
```

**DO NOT:**
- ❌ Convert ClientChunk back to ChunkDataTransferObject for rendering
- ❌ Use chunk.b array in RenderService
- ❌ Do BlockType registry lookups during rendering
- ❌ Re-merge modifiers in RenderService (already done in ClientBlock)

## Development Guidelines

### Logging

The project uses a built-in logging framework (`@nimbus/shared/logger`).

**DO NOT use console.log** - use the logger instead:

```typescript
import { getLogger } from '@nimbus/shared';

const logger = getLogger('MyService');

logger.info('Service started');
logger.debug('Debug info', { data });
logger.error('Error occurred', { context }, error);
```

**Log levels** (from highest to lowest priority):
- `FATAL` - System must stop
- `ERROR` - Operation failed
- `WARN` - May cause issues
- `INFO` - Informational (default)
- `DEBUG` - Debug information
- `TRACE` - Verbose logging

**Configuration**:
```typescript
import { LoggerFactory, LogLevel } from '@nimbus/shared';

// Apply environment config (.env: LOG_LEVEL=DEBUG)
LoggerFactory.configureFromEnv();

// Set level programmatically
LoggerFactory.setDefaultLevel(LogLevel.DEBUG);
LoggerFactory.setLoggerLevel('NetworkService', LogLevel.TRACE);
```

**Best practices**:
- Use named loggers per service/class: `getLogger('ServiceName')`
- Include context data: `logger.info('Message', { key: 'value' })`
- Use appropriate levels (errors are ERROR, debug info is DEBUG)
- Check level before expensive operations: `if (logger.isLevelEnabled(LogLevel.DEBUG))`

See `packages/shared/src/logger/LOGGER_USAGE.md` for detailed documentation.

### Exception Handling

**CRITICAL: All exceptions must be handled using the central ExceptionHandler.**

The project uses a central exception handler (`@nimbus/shared/ExceptionHandler`) to ensure all exceptions are properly logged and can be handled uniformly.

#### When to Use Try-Catch Blocks

Add try-catch blocks in these scenarios:

1. **Top-level initialization code** - Main entry points, constructors
2. **Async operations** - Network calls, file I/O, database operations
3. **Event handlers** - User interactions, timers, callbacks
4. **Service boundaries** - Public API methods, message handlers
5. **Resource management** - File handles, connections, cleanup operations

#### How to Use ExceptionHandler

```typescript
import { ExceptionHandler } from '@nimbus/shared';

// Pattern 1: Handle and rethrow (for initialization, critical operations)
try {
  // Critical operation that must succeed
  await initializeService();
} catch (error) {
  throw ExceptionHandler.handleAndRethrow(
    error,
    'ServiceName.methodName',
    { contextData: 'optional' }
  );
}

// Pattern 2: Handle without rethrowing (for non-critical operations)
try {
  // Operation that can fail gracefully
  await savePreferences();
} catch (error) {
  ExceptionHandler.handle(error, 'ServiceName.methodName', { context });
  // Continue execution with fallback behavior
}

// Pattern 3: Wrap async functions
const wrappedFunction = ExceptionHandler.wrapAsync(
  async () => { /* ... */ },
  'ServiceName.methodName'
);

// Pattern 4: Wrap sync functions
const wrappedSync = ExceptionHandler.wrap(
  () => { /* ... */ },
  'ServiceName.methodName'
);
```

#### Exception Handling Guidelines

**✅ DO:**
- Use try-catch at boundaries where errors cannot be propagated further
- Always call `ExceptionHandler.handle()` or `ExceptionHandler.handleAndRethrow()`
- Provide meaningful context names: `'ClassName.methodName'`
- Include context data: `{ userId, operation, state }`
- Decide whether to rethrow based on criticality

**❌ DON'T:**
- Use bare try-catch without ExceptionHandler
- Swallow exceptions silently (always log via ExceptionHandler)
- Use generic context names like 'error' or 'exception'
- Catch exceptions you can't handle properly

#### Context Naming Convention

Use this format for context parameter: `'ClassName.methodName'`

Examples:
- `'NetworkService.connect'`
- `'ChunkRenderer.generateMesh'`
- `'FileLogTransport.initialize'`
- `'NimbusClient.init'`

#### Examples from Codebase

**Example 1: Critical initialization (rethrow)**
```typescript
// packages/shared/src/logger/transports/FileLogTransport.ts
async initialize(): Promise<void> {
  try {
    // ... initialization logic
  } catch (error) {
    throw ExceptionHandler.handleAndRethrow(
      error,
      'FileLogTransport.initialize',
      { options: this.options }
    );
  }
}
```

**Example 2: Non-critical operation (don't rethrow)**
```typescript
// packages/shared/src/logger/transports/FileLogTransport.ts
private downloadLog(content: string): void {
  try {
    // ... download logic
  } catch (error) {
    ExceptionHandler.handle(error, 'FileLogTransport.downloadLog');
    // Don't rethrow - download errors should not break the application
  }
}
```

**Example 3: Transport errors (never rethrow)**
```typescript
// packages/shared/src/logger/transports/FileLogTransport.ts
transport = (entry: LogEntry): void => {
  try {
    // ... transport logic
  } catch (error) {
    ExceptionHandler.handle(error, 'FileLogTransport.transport', { entry });
    // Don't rethrow - transport errors should not break logging
  }
};
```

#### Custom Error Handlers

You can register a custom handler for user-facing error notifications:

```typescript
import { ExceptionHandler } from '@nimbus/shared';

// Register custom handler (e.g., show error dialog)
ExceptionHandler.registerHandler((error, context, data) => {
  // Show user-friendly error message
  showErrorDialog({
    title: 'An error occurred',
    message: error.message,
    details: context,
  });
});

// Later: unregister
ExceptionHandler.registerHandler(null);
```

**Important:** All exceptions are ALWAYS logged via the logger, regardless of custom handlers. Custom handlers are for additional actions like showing UI dialogs.

See `packages/shared/src/logger/ExceptionHandler.ts` for implementation details.

### File Organization

Each data structure should have its own file. Enums, status types, etc. related to a specific type can be defined in the same file as long as they are only relevant to that type.

```
shared/
├── types/
│   ├── Block.ts
│   ├── Chunk.ts
│   ├── Entity.ts
│   └── ...
└── network/
    ├── messages/
    │   ├── LoginMessage.ts
    │   ├── ChunkMessage.ts
    │   └── ...
    └── handlers/
```

### Code Style

- Use **TypeScript strict mode**
- Prefer **interfaces** over types for object shapes
- Use **const** by default, **let** only when necessary
- Follow **functional programming** principles where appropriate
- Write **self-documenting code** with clear variable and function names
- Add **JSDoc comments** for public APIs

### Testing

- Write unit tests for business logic
- Use Jest for testing
- Test file naming: `*.test.ts`
- Aim for high coverage of critical paths

## Environment Variables

Client configuration via environment (dotenv):

```bash
# Authentication
CLIENT_USERNAME=username
CLIENT_PASSWORD=password

# Server Connection
SERVER_WEBSOCKET_URL=ws://localhost:3000
SERVER_API_URL=http://localhost:3000
```
Implement getter in this in ClientConfig.ts

## Key Components

### EngineService.ts
The core 3D EngineService class that initializes and manages Babylon.js, the scene, and rendering.

### StartScreen.ts
Handles the initial login screen and world selection:
- Sends login message
- Fetches available worlds from `SERVER_API_URL/worlds`
- Displays world selection UI
- Transitions to EngineService after world selection

### NetworkService.ts
Manages WebSocket connection to server and Rest API calls:
- Connection management
- Message serialization/deserialization
- Message routing to handlers
- Login and ping/pong implementation

### CamControl.ts
Camera control system:
- Integrates with InputService
- Handles camera movement and rotation
- Supports different camera modes (first-person, free cam, etc.)

## Migration from Prototype

When migrating features from `client_playground`:

1. **Review** the prototype implementation
2. **Identify** improvements and issues
3. **Design** the 2.0 approach incorporating learnings
4. **Discuss** the approach before implementation
5. **Implement** with tests and documentation
6. **Verify** against requirements

### Key Improvements in 2.0

- Better separation of concerns (services architecture)
- Improved type safety
- Cleaner input handling system
- More modular network protocol
- Better testing infrastructure
- Clearer architectural boundaries

## References

For detailed information, see:
- `instructions/client_2.0/object-model-2.0.md` - Data model
- `instructions/client_2.0/network-model-2.0.md` - Network protocol
- `instructions/client_2.0/migration-concepts.md` - Architecture concepts
- `instructions/client_2.0/migration.md` - Migration tasks

## Working with Claude

### When to Create Code
Only create code in the `client` folder when:
- Explicitly requested in a task or instruction
- The task is clearly defined with requirements
- The location and structure are specified

### When to Give Feedback
Provide suggestions and improvements when:
- You notice potential issues or improvements
- There are better approaches based on TypeScript/Babylon.js best practices
- Code could be more maintainable or testable
- Performance could be improved

### Communication
- Ask clarifying questions when requirements are unclear
- Suggest alternatives when there are better approaches
- Reference line numbers when discussing specific code: `file_path:line_number`
- Use German in UI-facing strings, English in code

## Common Patterns

### Service Registration

```typescript
// In main.ts or bootstrap
const appContext: AppContext = {
  services: {
    network: new NetworkService(),
    input: new InputService(),
    // ...
  },
  // ...
};
```

### Message Handling

```typescript
// In NetworkService
private handlers = new Map<MessageType, MessageHandler>();

registerHandler(type: MessageType, handler: MessageHandler) {
  this.handlers.set(type, handler);
}

private handleMessage(message: NetworkMessage) {
  const handler = this.handlers.get(message.type);
  handler?.handle(message);
}
```

### Input Actions

```typescript
// Define action
class MoveForwardAction extends InputAction {
  execute(value: number) {
    // Move logic
  }
}

// Register in InputService
inputService.bindAction('key_w', new MoveForwardAction());
```

## Notes

- This is a **greenfield rewrite** - we can fix architectural issues from the prototype
- Focus on **quality over speed** - build it right the first time
- **Test as you go** - don't accumulate technical debt
- **Document decisions** - future maintainers will thank you
