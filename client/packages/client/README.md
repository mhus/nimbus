# @nimbus/client

Nimbus Voxel Engine - 3D Client (Viewer Build)

This is the **viewer** build - a read-only 3D engine for viewing worlds.

## Features

- 3D voxel world rendering with Babylon.js
- WebSocket connection to server
- Chunk loading and rendering
- Camera controls
- Block type registry

## Structure

```
src/
├── NimbusClient.ts       # Main entry point
├── config/               # Configuration
├── services/             # Core services (singleton)
│   ├── NetworkService.ts
│   ├── InputService.ts
│   ├── ChunkService.ts
│   └── ...
├── engine/               # 3D Engine
│   ├── EngineService.ts
│   ├── RenderService.ts
│   └── ...
├── network/              # Network protocol
│   ├── handlers/         # Message handlers
│   └── actions/          # REST actions
└── ui/                   # UI components
    └── StartScreen.ts
```

## Development

```bash
# Install dependencies
pnpm install

# Start dev server
pnpm dev

# Build for production
pnpm build

# Preview production build
pnpm preview
```

## Environment Variables

Copy `.env.example` to `.env` and configure:

```bash
CLIENT_USERNAME=your_username
CLIENT_PASSWORD=your_password
SERVER_WEBSOCKET_URL=ws://localhost:3000
SERVER_API_URL=http://localhost:3000
```

## Usage

The client will:
1. Show a start screen with login
2. Fetch available worlds from the server
3. Allow world selection
4. Initialize the 3D engine
5. Connect via WebSocket
6. Load and render chunks

## Architecture

See `../../CLAUDE.md` for detailed architecture documentation.
