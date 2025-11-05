# Generate BlockType Descriptions

Automated script to generate AI-powered descriptions for BlockTypes using the Gemini API.

## Overview

This script:
1. Reads all BlockType JSON files from `client/packages/server/files/blocktypes/`
2. Extracts asset references (textures, models) from each BlockType
3. Reads `.info` files for each asset (containing descriptions)
4. Sends BlockType data + asset descriptions to Gemini API
5. Generates concise, descriptive text for each BlockType
6. Updates BlockType files with new descriptions

## Features

- **Safety First**: Backs up old description to `description_old` field before updating
- **Skip Existing**: By default, skips BlockTypes that already have `description_old` (prevents accidental overwrites)
- **Rate Limiting**: Configurable delay between API calls to avoid hitting quota limits
- **Retry Logic**: Automatically retries on rate limit errors (429)
- **Asset Context**: Includes asset descriptions in prompts for better AI-generated descriptions

## Installation

1. Install dependencies:
   ```bash
   npm install --save-dev @google/generative-ai
   ```

2. Get a Gemini API key from [Google AI Studio](https://ai.google.dev/)

3. Set the API key:
   ```bash
   export GOOGLE_API_KEY="your-api-key-here"
   ```

## Usage

### Basic Usage

Process all BlockTypes (skips if `description_old` exists):
```bash
node scripts/generate-blocktype-descriptions.js
```

### Options

- `--api-key KEY` - Provide API key directly (instead of env variable)
- `--overwrite` - Overwrite existing descriptions (ignores `description_old` check)
- `--delay SECONDS` - Delay between API calls (default: 6.5s for ~9 req/min)
- `--help, -h` - Show help message

### Examples

**First run** (generates descriptions for all BlockTypes):
```bash
node scripts/generate-blocktype-descriptions.js
```

**Regenerate all descriptions** (overwrites existing):
```bash
node scripts/generate-blocktype-descriptions.js --overwrite
```

**Slower rate** (4 requests per minute):
```bash
node scripts/generate-blocktype-descriptions.js --delay 15
```

**Provide API key directly**:
```bash
node scripts/generate-blocktype-descriptions.js --api-key "your-key-here"
```

## How It Works

### BlockType Analysis

For each BlockType, the script:

1. **Extracts Properties**:
   - ID, name, current description
   - Shape, physics (solid, unbreakable)
   - Wind effects (leafiness, stability)

2. **Collects Assets**:
   - Finds all texture paths in `modifiers[*].visibility.textures`
   - Finds model paths in `modifiers[*].visibility.model`
   - Reads corresponding `.info` files for asset descriptions

3. **Generates Prompt**:
   ```
   You are analyzing a BlockType definition from a voxel game engine...

   BlockType Data:
   - ID: 601
   - Name: Wheat Stage3
   - Current Description: Wheat Stage3 block

   Block Properties:
   Status 0:
     - Shape: 1
     - Solid: yes

   Assets Used:
   - textures/block/basic/wheat_stage3.png
     Description: A wheat plant in its third growth stage...

   Generate a brief, descriptive text...
   ```

4. **Updates File**:
   ```json
   {
     "id": 601,
     "name": "Wheat Stage3",
     "description": "A mid-growth wheat plant block...",
     "description_old": "Wheat Stage3 block",
     "modifiers": { ... }
   }
   ```

### Rate Limiting

- **Default**: 6.5 second delay = ~9 requests/minute
- **Gemini Free Tier**: 15 requests/minute (we use conservative rate)
- **Retry Logic**: On 429 errors, waits 30 seconds before retry (up to 3 attempts)

### Safety Mechanism

The `description_old` field serves as a safety flag:
- **First run**: No `description_old` exists → generates description
- **Second run**: `description_old` exists → skips BlockType
- **With --overwrite**: Ignores `description_old` → regenerates description

This prevents accidental overwrites while allowing manual regeneration when needed.

## Output

The script provides detailed progress information:

```
Generate BlockType Descriptions
================================

BlockTypes directory: /path/to/blocktypes
Assets directory: /path/to/assets
Mode: Skip if description_old exists
Delay between API calls: 6.5s (~9 req/min)

Finding BlockTypes...
Found 156 BlockType files

[1/156] Processing: 0.json
  ID: 0, Name: Air
  Found 0 assets
  Generating description...
  Generated: An invisible air block that represents empty space...
  Updated: /path/to/blocktypes/0/0.json

[2/156] Processing: 601.json
  ID: 601, Name: Wheat Stage3
  Found 1 assets
  Generating description...
  Generated: A mid-growth wheat plant block showing the third...
  Updated: /path/to/blocktypes/6/601.json

...

Completed!
  Processed: 145
  Skipped: 10
  Errors: 1
  Total: 156
```

## Workflow Integration

### Typical Workflow

1. **Generate Asset Descriptions** (if not done):
   ```bash
   cd /path/to/nimbus
   python scripts/generate_asset_info.py --update-empty
   ```

2. **Generate BlockType Descriptions** (first time):
   ```bash
   node scripts/generate-blocktype-descriptions.js
   ```

3. **Review Generated Descriptions**:
   - Check a few BlockType files manually
   - Verify quality of descriptions

4. **Regenerate Specific BlockTypes** (if needed):
   - Manually edit the BlockType file
   - Remove `description_old` field
   - Run script again (will regenerate only that BlockType)

5. **Regenerate All** (if needed):
   ```bash
   node scripts/generate-blocktype-descriptions.js --overwrite
   ```

## Troubleshooting

### "No API key provided"
- Set `GOOGLE_API_KEY` environment variable
- Or use `--api-key` option

### "Rate limit hit"
- Script automatically waits and retries
- If persistent, increase `--delay` value

### "description_old already exists"
- This is normal on subsequent runs (safety feature)
- Use `--overwrite` to regenerate
- Or manually remove `description_old` from specific files

### "No BlockTypes found"
- Check that `client/packages/server/files/blocktypes/` exists
- Verify BlockType JSON files are in subdirectories (e.g., `0/0.json`)

## Related Scripts

- `generate_asset_info.py` - Generate `.info` files for assets
- `generate-manifest.js` - Generate asset manifest
- `migrate-playground-blocks.js` - Migrate blocks from playground

## API Costs

Gemini API pricing (as of 2024):
- **Free tier**: 15 requests/minute
- **Flash model**: $0.00001 per 1K characters input

For ~150 BlockTypes with average prompt size of 500 characters:
- Total cost: ~$0.0075 (less than 1 cent)

## License

Part of the Nimbus voxel engine project.
