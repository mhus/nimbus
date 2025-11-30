# BlockTypes Directory Structure

## New Group-Based Structure

BlockTypes are now organized in groups based on their ID format:

### ID Format

```
{blockTypeIdGroup}:{blockTypeName}
```

- **blockTypeIdGroup**: Group name (e.g., 'core', 'custom', 'mod_xyz')
- **blockTypeName**: Block type name (e.g., 'stone', 'wood', 'custom_block')
- If no ':' in ID, default group 'w' is used
- All IDs are normalized to lowercase
- Valid characters: a-z0-9_-

### Directory Structure

```
files/blocktypes/
├── w/                    # Default group (legacy blocks without group)
│   ├── 0.json           # w:0 (Air block)
│   ├── 1.json           # w:1
│   └── ...
├── core/                 # Core blocks group
│   ├── stone.json       # core:stone
│   ├── dirt.json        # core:dirt
│   └── ...
├── custom/               # Custom blocks group
│   └── ...
└── README.md            # This file
```

### File Naming

- Files are named `{blockTypeName}.json`
- The group is determined by the parent directory
- Example: `core/stone.json` → BlockType ID: `core:stone`

### Validation

- Group names must only contain: a-z0-9_-
- Group names are always lowercase
- Invalid group names will be rejected by the API

## API Endpoints

### Load BlockType Group

```
GET /api/worlds/{worldId}/blocktypeschunk/{groupName}
```

Returns all BlockTypes in the specified group as an array.

**Example:**
```bash
GET /api/worlds/main/blocktypeschunk/core
```

Response:
```json
[
  {
    "id": "core:stone",
    "description": "Stone block",
    "modifiers": { ... }
  },
  {
    "id": "core:dirt",
    "description": "Dirt block",
    "modifiers": { ... }
  }
]
```

## Migration Note

The old numeric-based structure (0/, 1/, 2/, etc.) is still supported for backward compatibility.
BlockTypes in the old structure should be migrated to the new group-based structure.

Migration is NOT part of this implementation and should be done separately.
