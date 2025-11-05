# pnpm Scripts - Nimbus Voxel Engine

Zentrale Build- und Development-Scripts für alle Packages.

## Overview

Das Projekt verwendet **pnpm workspaces** mit vier Packages:
- `@nimbus/shared` - Shared types, protocols, utils
- `@nimbus/engine` - 3D Engine (Viewer + Editor builds)
- `@nimbus/controls` - UI controls and components
- `@nimbus/test_server` - Test server

Alle Scripts können vom Root aus aufgerufen werden.

## Build Scripts

### Build All

```bash
pnpm build
```

Baut alle Packages in der richtigen Reihenfolge:
1. `@nimbus/shared` (TypeScript → dist/)
2. `@nimbus/test_server` (TypeScript → dist/)
3. `@nimbus/engine` (Vite + TypeScript → dist/viewer/ + dist/editor/)
4. `@nimbus/controls` (Vite + TypeScript → dist/)

### Build Individual Packages

```bash
# Build shared types
pnpm build:shared

# Build test server
pnpm build:test_server

# Build engine (both viewer and editor)
pnpm build:engine

# Build only viewer
pnpm build:viewer

# Build only editor
pnpm build:editor

# Build controls
pnpm build:controls
```

**Note**: `build:viewer` and `build:editor` automatically build `@nimbus/shared` first.

## Development Scripts

### Start Development Servers

```bash
# Start test server (port 3000)
pnpm dev:test_server

# Start engine viewer (port 3001)
pnpm dev:engine

# Alias for viewer
pnpm dev:viewer

# Start engine editor (port 3001)
pnpm dev:editor

# Start controls (port 3002)
pnpm dev:controls
```

**Typical workflow:**

```bash
# Terminal 1: Start test server
pnpm dev:test_server

# Terminal 2: Start engine
pnpm dev:engine
# OR
pnpm dev:editor
```

## Production Scripts

```bash
# Start production test server (after build)
pnpm start:test_server
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
2. Builds `@nimbus/test_server`
3. Builds `@nimbus/engine` (viewer + editor + type declarations)
4. Builds `@nimbus/controls`

**Output**:
```
packages/
├── shared/dist/         # TypeScript declarations
├── test_server/dist/    # Compiled test server
├── engine/dist/
│   ├── viewer/          # Viewer build
│   ├── editor/          # Editor build
│   ├── NimbusClient.d.ts
│   └── types/
└── controls/dist/       # UI controls
```

**Duration**: ~5-10 seconds

---

### build:shared

**Command**: `pnpm build:shared`

**What it does**: Compiles TypeScript for `@nimbus/shared`

**Output**: `packages/shared/dist/`

**Duration**: ~2 seconds

---

### build:test_server

**Command**: `pnpm build:test_server`

**What it does**: Compiles TypeScript for `@nimbus/test_server`

**Output**: `packages/test_server/dist/`

**Duration**: ~2 seconds

**Note**: Requires `@nimbus/shared` to be built first

---

### build:engine

**Command**: `pnpm build:engine`

**What it does**: Builds both viewer and editor variants + TypeScript declarations

**Equivalent to**:
```bash
cd packages/engine
pnpm build
```

**Output**:
- `dist/viewer/` - Viewer bundle (~12.5 KB gzipped)
- `dist/editor/` - Editor bundle (~12.5 KB gzipped)
- `dist/NimbusClient.d.ts` - Type declarations
- `dist/types/` - Engine type declarations

**Duration**: ~5 seconds

---

### build:viewer

**Command**: `pnpm build:viewer`

**What it does**:
1. Builds `@nimbus/shared` (if needed)
2. Builds viewer variant only

**Output**: `packages/engine/dist/viewer/`

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

**Output**: `packages/engine/dist/editor/`

**Duration**: ~3 seconds

**Global constants**:
- `__EDITOR__ = true`
- `__VIEWER__ = false`
- `__BUILD_MODE__ = 'editor'`

---

### dev:test_server

**Command**: `pnpm dev:test_server`

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

### dev:engine

**Command**: `pnpm dev:engine`

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

### start:test_server

**Command**: `pnpm start:test_server`

**What it does**: Starts production test server (no hot reload)

**Requires**: `pnpm build:test_server` must be run first

**Uses**: `node dist/NimbusServer.js`

---

### test

**Command**: `pnpm test`

**What it does**: Runs Jest tests in all packages

**Recursive**: Runs in `@nimbus/shared`, `@nimbus/engine`, `@nimbus/controls`, `@nimbus/test_server`

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

# 3. Start test server (Terminal 1)
pnpm dev:test_server

# 4. Start engine (Terminal 2)
pnpm dev:engine
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
ls packages/test_server/dist/
ls packages/engine/dist/viewer/
ls packages/engine/dist/editor/
ls packages/controls/dist/

# Start production test server
pnpm start:test_server
```

### Quick Client Development

```bash
# Only rebuild engine (if shared/test_server haven't changed)
pnpm build:engine

# Or just viewer
pnpm build:viewer

# Or just editor
pnpm build:editor

# Or just controls
pnpm build:controls
```

### Testing Specific Build

```bash
# Build and test viewer
pnpm build:viewer
cd packages/engine
pnpm preview

# Build and test editor
pnpm build:editor
cd packages/engine
pnpm preview:editor
```

## Package Dependencies

```
@nimbus/engine
  ↓ depends on
@nimbus/shared

@nimbus/controls
  ↓ depends on
@nimbus/shared

@nimbus/test_server
  ↓ depends on
@nimbus/shared
```

**Important**: Always build `@nimbus/shared` before building `@nimbus/engine`, `@nimbus/controls`, or `@nimbus/test_server`.

The root scripts handle this automatically.

## Parallel Execution

pnpm supports parallel execution with `-r` flag, but we use sequential builds to ensure proper dependency order:

```bash
# Sequential (correct)
pnpm build:shared && pnpm build:test_server && pnpm build:engine

# Parallel would fail due to dependencies
pnpm -r build  # ❌ May fail if engine builds before shared
```

## Environment Variables

Scripts respect environment variables from `.env` files:

```bash
# packages/engine/.env
CLIENT_USERNAME=test
CLIENT_PASSWORD=test
SERVER_WEBSOCKET_URL=ws://localhost:3000
SERVER_API_URL=http://localhost:3000

# packages/test_server/.env
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

**Solution**: Stop running engine dev server or change port in `packages/engine/vite.config.ts`

### "Port 3000 already in use"

**Solution**: Stop running test server or change port in `packages/test_server/.env`

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
| `pnpm build:test_server` | Build test server | ~2s |
| `pnpm build:engine` | Build engine (both) | ~5s |
| `pnpm build:viewer` | Build viewer only | ~3s |
| `pnpm build:editor` | Build editor only | ~3s |
| `pnpm build:controls` | Build controls | ~3s |
| `pnpm dev:test_server` | Start test server dev | - |
| `pnpm dev:engine` | Start viewer dev | - |
| `pnpm dev:editor` | Start editor dev | - |
| `pnpm dev:controls` | Start controls dev | - |
| `pnpm test` | Run all tests | varies |
| `pnpm lint` | Lint all packages | ~5s |
| `pnpm clean` | Remove artifacts | ~1s |

## See Also

- [packages/engine/BUILD_VARIANTS.md](./packages/engine/BUILD_VARIANTS.md) - Engine build variants
- [packages/engine/README.md](./packages/engine/README.md) - Engine package
- [CLAUDE.md](./CLAUDE.md) - Architecture overview
