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

- **viewer**: Read-only 3D engine for viewing worlds
- **editor**: Full 3D engine + editor functions + console

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
- **ChunkService**: Manages chunk loading and management (singleton, AppContext)
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

## Development Guidelines

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
