# pnpm Scripts - Nimbus Voxel Engine

Zentrale Build- und Development-Scripts für alle Packages.

## Overview

Das Projekt verwendet **pnpm workspaces** mit drei Packages:
- `@nimbus/shared` - Shared types, protocols, utils
- `@nimbus/client` - 3D Client (Viewer + Editor builds)
- `@nimbus/server` - Test server

Alle Scripts können vom Root aus aufgerufen werden.

## Build Scripts

### Build All

```bash
pnpm build
```

Baut alle Packages in der richtigen Reihenfolge:
1. `@nimbus/shared` (TypeScript → dist/)
2. `@nimbus/server` (TypeScript → dist/)
3. `@nimbus/client` (Vite + TypeScript → dist/viewer/ + dist/editor/)

### Build Individual Packages

```bash
# Build shared types
pnpm build:shared

# Build server
pnpm build:server

# Build client (both viewer and editor)
pnpm build:client

# Build only viewer
pnpm build:viewer

# Build only editor
pnpm build:editor
```

**Note**: `build:viewer` and `build:editor` automatically build `@nimbus/shared` first.

## Development Scripts

### Start Development Servers

```bash
# Start test server (port 3000)
pnpm dev:server

# Start client viewer (port 3001)
pnpm dev:client

# Alias for viewer
pnpm dev:viewer

# Start client editor (port 3001)
pnpm dev:editor
```

**Typical workflow:**

```bash
# Terminal 1: Start server
pnpm dev:server

# Terminal 2: Start client
pnpm dev:client
# OR
pnpm dev:editor
```

## Production Scripts

```bash
# Start production server (after build)
pnpm start:server
```

## Utility Scripts

```bash
# Run tests in all packages
pnpm test

# Run linting in all packages
pnpm lint

# Clean all build artifacts
pnpm clean
```

## Script Details

### build

**Command**: `pnpm build`

**What it does**:
1. Builds `@nimbus/shared` (dependencies first)
2. Builds `@nimbus/server`
3. Builds `@nimbus/client` (viewer + editor + type declarations)

**Output**:
```
packages/
├── shared/dist/         # TypeScript declarations
├── server/dist/         # Compiled server
└── client/dist/
    ├── viewer/          # Viewer build
    ├── editor/          # Editor build
    ├── NimbusClient.d.ts
    └── types/
```

**Duration**: ~5-10 seconds

---

### build:shared

**Command**: `pnpm build:shared`

**What it does**: Compiles TypeScript for `@nimbus/shared`

**Output**: `packages/shared/dist/`

**Duration**: ~2 seconds

---

### build:server

**Command**: `pnpm build:server`

**What it does**: Compiles TypeScript for `@nimbus/server`

**Output**: `packages/server/dist/`

**Duration**: ~2 seconds

**Note**: Requires `@nimbus/shared` to be built first

---

### build:client

**Command**: `pnpm build:client`

**What it does**: Builds both viewer and editor variants + TypeScript declarations

**Equivalent to**:
```bash
cd packages/client
pnpm build
```

**Output**:
- `dist/viewer/` - Viewer bundle (~12.5 KB gzipped)
- `dist/editor/` - Editor bundle (~12.5 KB gzipped)
- `dist/NimbusClient.d.ts` - Type declarations
- `dist/types/` - Client type declarations

**Duration**: ~5 seconds

---

### build:viewer

**Command**: `pnpm build:viewer`

**What it does**:
1. Builds `@nimbus/shared` (if needed)
2. Builds viewer variant only

**Output**: `packages/client/dist/viewer/`

**Duration**: ~3 seconds

**Global constants**:
- `__EDITOR__ = false`
- `__VIEWER__ = true`
- `__BUILD_MODE__ = 'viewer'`

---

### build:editor

**Command**: `pnpm build:editor`

**What it does**:
1. Builds `@nimbus/shared` (if needed)
2. Builds editor variant only

**Output**: `packages/client/dist/editor/`

**Duration**: ~3 seconds

**Global constants**:
- `__EDITOR__ = true`
- `__VIEWER__ = false`
- `__BUILD_MODE__ = 'editor'`

---

### dev:server

**Command**: `pnpm dev:server`

**What it does**: Starts test server with hot reload

**Uses**: `tsx watch` for TypeScript hot reload

**Port**: 3000 (default)

**Features**:
- WebSocket server
- REST API
- Auto-restart on file changes

**Output**:
```
Nimbus Server v2.0.0
WebSocket server listening on ws://localhost:3000
REST API listening on http://localhost:3000
```

---

### dev:client

**Command**: `pnpm dev:client`

**Alias**: `pnpm dev:viewer`

**What it does**: Starts Vite dev server in viewer mode

**Port**: 3001

**Features**:
- Hot Module Replacement (HMR)
- Fast refresh
- Source maps

**Mode**: `viewer`

**Global constants**:
- `__EDITOR__ = false`
- `__VIEWER__ = true`

---

### dev:editor

**Command**: `pnpm dev:editor`

**What it does**: Starts Vite dev server in editor mode

**Port**: 3001

**Features**:
- Hot Module Replacement (HMR)
- Fast refresh
- Source maps
- Editor-specific code loaded

**Mode**: `editor`

**Global constants**:
- `__EDITOR__ = true`
- `__VIEWER__ = false`

---

### start:server

**Command**: `pnpm start:server`

**What it does**: Starts production server (no hot reload)

**Requires**: `pnpm build:server` must be run first

**Uses**: `node dist/NimbusServer.js`

---

### test

**Command**: `pnpm test`

**What it does**: Runs Jest tests in all packages

**Recursive**: Runs in `@nimbus/shared`, `@nimbus/client`, `@nimbus/server`

---

### lint

**Command**: `pnpm lint`

**What it does**: Runs ESLint in all packages

**Recursive**: Lints all TypeScript files

---

### clean

**Command**: `pnpm clean`

**What it does**: Removes all build artifacts

**Removes**:
- `packages/*/dist/`
- `packages/*/*.tsbuildinfo`
- `node_modules/` (root)

**Warning**: Run `pnpm install` after clean

## Workflow Examples

### Full Development Setup

```bash
# 1. Install dependencies
pnpm install

# 2. Build all packages (first time)
pnpm build

# 3. Start server (Terminal 1)
pnpm dev:server

# 4. Start client (Terminal 2)
pnpm dev:client
# OR for editor
pnpm dev:editor
```

### Build for Production

```bash
# Clean and build everything
pnpm clean
pnpm install
pnpm build

# Verify builds
ls packages/shared/dist/
ls packages/server/dist/
ls packages/client/dist/viewer/
ls packages/client/dist/editor/

# Start production server
pnpm start:server
```

### Quick Client Development

```bash
# Only rebuild client (if shared/server haven't changed)
pnpm build:client

# Or just viewer
pnpm build:viewer

# Or just editor
pnpm build:editor
```

### Testing Specific Build

```bash
# Build and test viewer
pnpm build:viewer
cd packages/client
pnpm preview

# Build and test editor
pnpm build:editor
cd packages/client
pnpm preview:editor
```

## Package Dependencies

```
@nimbus/client
  ↓ depends on
@nimbus/shared

@nimbus/server
  ↓ depends on
@nimbus/shared
```

**Important**: Always build `@nimbus/shared` before building `@nimbus/client` or `@nimbus/server`.

The root scripts handle this automatically.

## Parallel Execution

pnpm supports parallel execution with `-r` flag, but we use sequential builds to ensure proper dependency order:

```bash
# Sequential (correct)
pnpm build:shared && pnpm build:server && pnpm build:client

# Parallel would fail due to dependencies
pnpm -r build  # ❌ May fail if client builds before shared
```

## Environment Variables

Scripts respect environment variables from `.env` files:

```bash
# packages/client/.env
CLIENT_USERNAME=test
CLIENT_PASSWORD=test
SERVER_WEBSOCKET_URL=ws://localhost:3000
SERVER_API_URL=http://localhost:3000

# packages/server/.env
PORT=3000
HOST=localhost
```

## Troubleshooting

### "Cannot find module '@nimbus/shared'"

**Solution**: Build shared package first
```bash
pnpm build:shared
```

### "Port 3001 already in use"

**Solution**: Stop running client dev server or change port in `packages/client/vite.config.ts`

### "Port 3000 already in use"

**Solution**: Stop running server or change port in `packages/server/.env`

### Build fails after git pull

**Solution**: Clean and rebuild
```bash
pnpm clean
pnpm install
pnpm build
```

## CI/CD

For continuous integration:

```yaml
# .github/workflows/build.yml
steps:
  - uses: pnpm/action-setup@v2
    with:
      version: 8

  - name: Install dependencies
    run: pnpm install

  - name: Build all
    run: pnpm build

  - name: Run tests
    run: pnpm test

  - name: Lint
    run: pnpm lint
```

## Quick Reference

| Command | Description | Duration |
|---------|-------------|----------|
| `pnpm build` | Build all packages | ~10s |
| `pnpm build:shared` | Build shared types | ~2s |
| `pnpm build:server` | Build server | ~2s |
| `pnpm build:client` | Build client (both) | ~5s |
| `pnpm build:viewer` | Build viewer only | ~3s |
| `pnpm build:editor` | Build editor only | ~3s |
| `pnpm dev:server` | Start server dev | - |
| `pnpm dev:client` | Start viewer dev | - |
| `pnpm dev:editor` | Start editor dev | - |
| `pnpm test` | Run all tests | varies |
| `pnpm lint` | Lint all packages | ~5s |
| `pnpm clean` | Remove artifacts | ~1s |

## See Also

- [packages/client/BUILD_VARIANTS.md](./packages/client/BUILD_VARIANTS.md) - Client build variants
- [packages/client/README.md](./packages/client/README.md) - Client package
- [CLAUDE.md](./CLAUDE.md) - Architecture overview
