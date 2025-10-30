# Nimbus Material Editor

Material Editor for Nimbus - Web application for managing Block Types, Textures, and Assets.

## Tech Stack

- **Vue 3** - Progressive JavaScript framework
- **Vite** - Fast build tool
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first CSS framework
- **DaisyUI** - Tailwind CSS component library
- **Headless UI** - Unstyled, accessible UI components
- **Axios** - HTTP client for REST API

## Development

```bash
# Install dependencies
pnpm install

# Start dev server (http://localhost:3002)
pnpm dev

# Build for production
pnpm build

# Preview production build
pnpm preview

# Lint code
pnpm lint

# Clean build artifacts
pnpm clean
```

## Configuration

Copy `.env.example` to `.env.local` and configure:

```env
VITE_API_URL=http://localhost:3000
VITE_WORLD_ID=test-world-1
```

## Features

### Block Type Editor
- List and search all block types
- Create, edit, and delete block types
- Multi-status support (edit multiple status modifiers)
- TypeScript types from `@nimbus/shared`

### Asset Editor
- List and search all assets
- Preview image assets (PNG, JPG, etc.)
- Upload new assets
- Delete existing assets
- Drag & drop file upload

## Project Structure

```
src/
├── components/     # Reusable Vue components
├── views/          # Main views (BlockTypeEditor, AssetEditor)
├── services/       # API client services
├── composables/    # Vue composables for state management
├── types/          # Local TypeScript types
├── utils/          # Utility functions
├── App.vue         # Root component
└── main.ts         # Application entry point
```

## REST API

See `/client/instructions/client_2.0/server_rest_api.md` for complete API documentation.
