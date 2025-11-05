# @nimbus/test_server

Nimbus Voxel Engine - Test Server

This is a simple server implementation to provide world data to the engine. The focus is on **engine development**, not server features.

## Features

- REST API for world list and authentication
- WebSocket server for real-time communication
- Basic world generation
- Chunk management
- Message handling

## Structure

```
src/
├── NimbusServer.ts       # Main entry point
├── api/                  # REST API endpoints
│   ├── worlds.ts
│   └── auth.ts
├── network/              # WebSocket server
│   ├── WebSocketServer.ts
│   └── handlers/         # Message handlers
├── world/                # World generation
│   ├── WorldGenerator.ts
│   └── ChunkGenerator.ts
└── storage/              # World storage (in-memory for testing)
```

## Development

```bash
# Install dependencies
pnpm install

# Start dev server (with auto-reload)
pnpm dev

# Build
pnpm build

# Start production server
pnpm start
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
PORT=3000
HOST=0.0.0.0
WORLD_SEED=12345
CHUNK_SIZE=32
RENDER_DISTANCE=8
```

## API Endpoints

### REST API

- `GET /worlds` - Get list of available worlds
- `POST /login` - Authenticate user (basic auth for testing)
- `GET /world/:id` - Get world information

### WebSocket

- Connection: `ws://localhost:3000`
- Messages are JSON-serialized
- See `@nimbus/shared/network` for message protocol

## Testing

The server provides test data for client development:

- Multiple test worlds
- Simple terrain generation
- Basic chunk streaming
- Ping/pong for connection testing

## Architecture

See `../../CLAUDE.md` for detailed architecture documentation.
