/**
 * Migration Script: Playground BlockTypes → Nimbus Server JSON BlockTypes
 *
 * This script reads the BlockType definitions from client_playground and generates
 * JSON files for the Nimbus server in the format expected by the server.
 *
 * Source: client_playground/packages/core/src/registry/defaultBlocks.ts
 * Target: client/packages/server/files/blocktypes/1/{id}.json
 *
 * ID Mapping: New ID = 1000 + Legacy ID (from array index)
 */

const fs = require('fs');
const path = require('path');

// BlockShape enum from BlockType.ts
const BlockShape = {
  CUBE: 0,
  CROSS: 1,
  HASH: 2,
  MODEL: 3,
  GLASS: 4,
  FLAT: 5,
  SPHERE: 6,
  COLUMN: 7,
  ROUND_CUBE: 8,
  STEPS: 9,
  STAIR: 10,
  BILLBOARD: 11,
  SPRITE: 12,
  FLAME: 13,
};

/**
 * Convert playground block definitions to server JSON format
 */
function convertBlockTypeToJSON(blockType, legacyId) {
  const newId = 1000 + legacyId;

  // Extract texture paths and convert them
  let textures = {};
  if (typeof blockType.texture === 'string') {
    // Single texture for all faces
    textures['0'] = convertTexturePath(blockType.texture);
  } else if (Array.isArray(blockType.texture)) {
    // Array of textures
    blockType.texture.forEach((tex, index) => {
      textures[index.toString()] = convertTexturePath(tex);
    });
  }

  // Build modifier structure
  const modifier = {
    visibility: {
      shape: blockType.shape,
      textures: textures
    },
    physics: {
      solid: blockType.solid !== false && blockType.options?.solid !== false
    }
  };

  // Add optional properties
  if (blockType.transparent || blockType.options?.transparent) {
    modifier.visibility.transparent = true;
  }

  if (blockType.options?.opaque !== undefined) {
    modifier.visibility.opaque = blockType.options.opaque;
  }

  if (blockType.options?.material) {
    modifier.physics.material = blockType.options.material;
  }

  if (blockType.options?.fluid) {
    modifier.physics.fluid = true;
    if (blockType.options.fluidDensity !== undefined) {
      modifier.physics.fluidDensity = blockType.options.fluidDensity;
    }
    if (blockType.options.viscosity !== undefined) {
      modifier.physics.viscosity = blockType.options.viscosity;
    }
  }

  if (blockType.hardness !== undefined) {
    modifier.physics.hardness = blockType.hardness;
  }

  if (blockType.miningtime !== undefined) {
    modifier.physics.miningtime = blockType.miningtime;
  }

  if (blockType.tool !== undefined) {
    modifier.physics.tool = blockType.tool;
  }

  if (blockType.unbreakable) {
    modifier.physics.unbreakable = true;
  }

  // Wind properties
  if (blockType.windLeafiness !== undefined || blockType.options?.windLeafiness !== undefined) {
    modifier.visibility.windLeafiness = blockType.windLeafiness || blockType.options.windLeafiness;
  }

  if (blockType.windStability !== undefined) {
    modifier.visibility.windStability = blockType.windStability;
  }

  if (blockType.windLeverUp !== undefined) {
    modifier.visibility.windLeverUp = blockType.windLeverUp;
  }

  if (blockType.windLeverDown !== undefined) {
    modifier.visibility.windLeverDown = blockType.windLeverDown;
  }

  // Create JSON structure
  return {
    id: newId,
    name: formatBlockName(blockType.name),
    description: `${formatBlockName(blockType.name)} block (migrated from playground)`,
    modifiers: {
      '0': modifier
    }
  };
}

/**
 * Convert texture path from playground format to server format
 * From: 'block/stone' → To: 'textures/block/basic/stone.png'
 */
function convertTexturePath(texturePath) {
  // Remove 'block/' prefix if present
  const cleanPath = texturePath.replace(/^block\//, '');

  // Add server texture path structure
  return `textures/block/basic/${cleanPath}.png`;
}

/**
 * Format block name for display (capitalize first letter of each word)
 */
function formatBlockName(name) {
  return name
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/**
 * Block definitions from playground (simplified representation)
 */
const PLAYGROUND_BLOCKS = [
  // Basic terrain blocks
  { name: 'stone', shape: BlockShape.CUBE, texture: 'block/stone', hardness: 1.5, tool: 'pickaxe' },
  { name: 'dirt', shape: BlockShape.CUBE, texture: 'block/dirt', hardness: 0.5, tool: 'shovel' },
  { name: 'grass', shape: BlockShape.CUBE, texture: ['block/grass_top', 'block/dirt', 'block/grass_side'], hardness: 0.6, tool: 'shovel' },
  { name: 'grass_snow', shape: BlockShape.CUBE, texture: ['block/snow', 'block/dirt', 'block/grass_snow'], hardness: 0.6, tool: 'shovel' },
  { name: 'cobblestone', shape: BlockShape.CUBE, texture: 'block/cobblestone', hardness: 2.0, tool: 'pickaxe' },
  { name: 'log', shape: BlockShape.CUBE, texture: ['block/log_top', 'block/log'], hardness: 2.0, tool: 'axe' },
  { name: 'sand', shape: BlockShape.CUBE, texture: 'block/sand', hardness: 0.5, tool: 'shovel' },

  // Foliage
  { name: 'leaves', shape: BlockShape.GLASS, texture: 'block/leaves', hardness: 0.2, tool: 'any', transparent: true, options: { opaque: false, windLeafiness: 1 } },

  // Fluids
  { name: 'water', shape: BlockShape.FLAT, texture: 'block/water', options: { material: 'water', fluid: true, fluidDensity: 30.0, viscosity: 200.5 } },

  // Plants & decorations
  { name: 'red_flower', shape: BlockShape.CROSS, texture: 'block/red_flower', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },
  { name: 'grass_plant', shape: BlockShape.CROSS, texture: 'block/grass_plant', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },
  { name: 'yellow_flower', shape: BlockShape.CROSS, texture: 'block/yellow_flower', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },

  // Building blocks
  { name: 'bricks', shape: BlockShape.CUBE, texture: 'block/bricks', hardness: 2.0, tool: 'pickaxe' },
  { name: 'planks', shape: BlockShape.CUBE, texture: 'block/planks', hardness: 2.0, tool: 'axe' },
  { name: 'glass', shape: BlockShape.GLASS, texture: 'block/glass', hardness: 0.3, tool: 'any', transparent: true, options: { opaque: false } },
  { name: 'bookshelf', shape: BlockShape.CUBE, texture: ['block/planks', 'block/bookshelf'], hardness: 1.5, tool: 'axe' },

  // Special blocks
  { name: 'barrier', shape: BlockShape.CUBE, texture: [], options: { material: 'barrier', transparent: true }, hardness: 0, miningtime: 0, tool: 'none', unbreakable: true },

  // More terrain
  { name: 'snow', shape: BlockShape.CUBE, texture: 'block/snow', hardness: 0.1, tool: 'shovel' },
  { name: 'coal_ore', shape: BlockShape.CUBE, texture: 'block/coal_ore', hardness: 3.0, tool: 'pickaxe' },
  { name: 'iron_ore', shape: BlockShape.CUBE, texture: 'block/iron_ore', hardness: 3.0, tool: 'pickaxe' },

  // Cactus (special shape)
  { name: 'cactus', shape: BlockShape.HASH, texture: ['block/cactus_top', 'block/cactus_side', 'block/cactus_bottom'], transparent: true, options: { opaque: false }, hardness: 0.4, tool: 'any' },

  { name: 'deadbush', shape: BlockShape.CROSS, texture: 'block/dead_bush', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },
  { name: 'gravel', shape: BlockShape.CUBE, texture: 'block/gravel', hardness: 0.6, tool: 'shovel' },
  { name: 'crafting', shape: BlockShape.CUBE, texture: ['block/crafting_table_top', 'block/oak_planks', 'block/crafting_table_side'], hardness: 2.5, tool: 'axe' },
  { name: 'stonebrick', shape: BlockShape.CUBE, texture: 'block/stonebrick', hardness: 1.5, tool: 'pickaxe' },

  // Birch variants
  { name: 'birch_log', shape: BlockShape.CUBE, texture: ['block/birch_log_top', 'block/birch_log'], hardness: 2.0, tool: 'axe' },
  { name: 'birch_leaves', shape: BlockShape.GLASS, texture: 'block/birch_leaves', hardness: 0.2, tool: 'any', transparent: true, options: { opaque: false, windLeafiness: 1 } },
  { name: 'birch_planks', shape: BlockShape.CUBE, texture: 'block/birch_planks', hardness: 2.0, tool: 'axe' },

  // Spruce variants
  { name: 'spruce_log', shape: BlockShape.CUBE, texture: ['block/spruce_log_top', 'block/spruce_log'], hardness: 2.0, tool: 'axe' },
  { name: 'spruce_leaves', shape: BlockShape.GLASS, texture: 'block/spruce_leaves', hardness: 0.2, tool: 'any', transparent: true, options: { opaque: false, windLeafiness: 1 } },
  { name: 'spruce_planks', shape: BlockShape.CUBE, texture: 'block/spruce_planks', hardness: 2.0, tool: 'axe' },

  // Valuable blocks
  { name: 'iron_block', shape: BlockShape.CUBE, texture: 'block/iron_block', hardness: 5.0, tool: 'pickaxe' },
  { name: 'gold_block', shape: BlockShape.CUBE, texture: 'block/gold_block', hardness: 3.0, tool: 'pickaxe' },

  // Bedrock (unbreakable)
  { name: 'bedrock', shape: BlockShape.CUBE, texture: 'block/bedrock', hardness: 0, miningtime: 0, tool: 'none', unbreakable: true, solid: true },

  // More ores & blocks
  { name: 'sandstone', shape: BlockShape.CUBE, texture: 'block/sandstone', hardness: 0.8, tool: 'pickaxe' },
  { name: 'diamond_ore', shape: BlockShape.CUBE, texture: 'block/diamond_ore', hardness: 3.0, tool: 'pickaxe' },
  { name: 'diamond_block', shape: BlockShape.CUBE, texture: 'block/diamond_block', hardness: 5.0, tool: 'pickaxe' },
  { name: 'lapis_ore', shape: BlockShape.CUBE, texture: 'block/lapis_ore', hardness: 3.0, tool: 'pickaxe' },
  { name: 'lapis_block', shape: BlockShape.CUBE, texture: 'block/lapis_block', hardness: 3.0, tool: 'pickaxe' },
  { name: 'mossy_cobblestone', shape: BlockShape.CUBE, texture: 'block/mossy_cobblestone', hardness: 2.0, tool: 'pickaxe' },
  { name: 'obsidian', shape: BlockShape.CUBE, texture: 'block/obsidian', hardness: 50.0, tool: 'pickaxe' },
  { name: 'mossy_stonebricks', shape: BlockShape.CUBE, texture: 'block/mossy_stone_bricks', hardness: 1.5, tool: 'pickaxe' },

  // Special blocks
  { name: 'tnt', shape: BlockShape.CUBE, texture: ['block/tnt_top', 'block/tnt_bottom', 'block/tnt_side'], hardness: 0, tool: 'any' },
  { name: 'pumpkin', shape: BlockShape.CUBE, texture: ['block/pumpkin_top', 'block/pumpkin_side', 'block/pumpkin_side'], hardness: 1.0, tool: 'axe' },
  { name: 'oak_sapling', shape: BlockShape.CROSS, texture: 'block/oak_sapling', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },
  { name: 'ice', shape: BlockShape.CUBE, texture: 'block/ice', hardness: 0.5, tool: 'pickaxe', transparent: true },

  // Alternative grass biomes
  { name: 'grass_yellow', shape: BlockShape.CUBE, texture: ['block/grass_yellow_top', 'block/dirt', 'block/grass_yellow_side'], hardness: 0.6, tool: 'shovel' },
  { name: 'grass_plant_yellow', shape: BlockShape.CROSS, texture: 'block/grass_plant_yellow', hardness: 0, tool: 'any', options: { solid: false, opaque: false } },
  { name: 'leaves_yellow', shape: BlockShape.GLASS, texture: 'block/leaves_yellow', hardness: 0.2, tool: 'any', transparent: true, options: { opaque: false, windLeafiness: 1 } },
];

// Colored wool blocks
const COLORS = [
  'white', 'yellow', 'red', 'purple', 'pink', 'orange', 'magenta',
  'lime', 'light_blue', 'green', 'gray', 'cyan', 'brown', 'blue', 'black',
];

const COLORED_WOOL_BLOCKS = COLORS.map(color => ({
  name: `${color}_wool`,
  shape: BlockShape.CUBE,
  texture: `block/${color}_wool`,
  hardness: 0.8,
  tool: 'any'
}));

const COLORED_GLASS_BLOCKS = COLORS.map(color => ({
  name: `${color}_stained_glass`,
  shape: BlockShape.GLASS,
  texture: `block/${color}_stained_glass`,
  hardness: 0.3,
  tool: 'any',
  transparent: true,
  options: { opaque: false }
}));

const COLORED_CONCRETE_BLOCKS = COLORS.map(color => ({
  name: `${color}_concrete`,
  shape: BlockShape.CUBE,
  texture: `block/${color}_concrete`,
  hardness: 1.8,
  tool: 'pickaxe'
}));

// Combine all blocks
const ALL_BLOCKS = [
  ...PLAYGROUND_BLOCKS,
  ...COLORED_WOOL_BLOCKS,
  ...COLORED_GLASS_BLOCKS,
  ...COLORED_CONCRETE_BLOCKS,
];

/**
 * Get file path for BlockType ID according to server schema
 * Schema: (id / 100)/id.json
 */
function getBlockTypeFilePath(baseDir, id) {
  const subDir = Math.floor(id / 100);
  const dirPath = path.join(baseDir, subDir.toString());
  return {
    dirPath,
    filePath: path.join(dirPath, `${id}.json`)
  };
}

/**
 * Update manifest.json with new block type entries
 */
function updateManifest(baseDir, newEntries) {
  try {
    const manifestPath = path.join(baseDir, 'manifest.json');

    // Read existing manifest
    let manifest = [];
    if (fs.existsSync(manifestPath)) {
      manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
    }

    // Add new entries
    newEntries.forEach(entry => {
      // Check if entry already exists
      const existingIndex = manifest.findIndex(e => e.id === entry.id);
      if (existingIndex === -1) {
        manifest.push(entry);
      } else {
        // Update existing entry
        manifest[existingIndex] = entry;
      }
    });

    // Sort by ID
    manifest.sort((a, b) => a.id - b.id);

    // Write updated manifest
    fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');

    console.log(`\n✓ Updated manifest.json with ${newEntries.length} entries`);
  } catch (error) {
    console.error(`✗ Failed to update manifest:`, error.message);
  }
}

/**
 * Main execution
 */
function main() {
  const baseDir = path.join(__dirname, '..', 'client', 'packages', 'server', 'files', 'blocktypes');

  console.log(`Migrating ${ALL_BLOCKS.length} block types from playground...`);
  console.log(`Base directory: ${baseDir}`);
  console.log('');

  let successCount = 0;
  let errorCount = 0;
  const manifestEntries = [];

  ALL_BLOCKS.forEach((blockType, index) => {
    try {
      const legacyId = index;
      const newId = 1000 + legacyId;
      const json = convertBlockTypeToJSON(blockType, legacyId);

      // Get correct file path according to server schema
      const { dirPath, filePath } = getBlockTypeFilePath(baseDir, newId);
      const subDir = Math.floor(newId / 100);

      // Ensure output directory exists
      if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
      }

      fs.writeFileSync(filePath, JSON.stringify(json, null, 2));
      console.log(`✓ Created ${newId}.json in folder ${subDir} - ${json.name} (legacy ID: ${legacyId})`);

      // Add to manifest entries
      manifestEntries.push({
        id: newId,
        name: blockType.name,
        file: `${subDir}/${newId}.json`
      });

      successCount++;
    } catch (error) {
      console.error(`✗ Error converting block ${blockType.name}:`, error.message);
      errorCount++;
    }
  });

  // Update manifest
  updateManifest(baseDir, manifestEntries);

  console.log('');
  console.log('='.repeat(60));
  console.log(`Migration complete!`);
  console.log(`  Success: ${successCount} blocks`);
  console.log(`  Errors: ${errorCount} blocks`);
  console.log(`  ID Range: 1000-${999 + ALL_BLOCKS.length}`);
  console.log('='.repeat(60));
}

// Run the script
main();
