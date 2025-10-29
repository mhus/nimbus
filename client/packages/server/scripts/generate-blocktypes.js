#!/usr/bin/env node
/**
 * Generate BlockType JSON files from texture assets
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const TEXTURES_DIR = path.join(__dirname, '../files/assets/textures/block/basic');
const OUTPUT_DIR = path.join(__dirname, '../files/blocktypes');
const START_ID = 100;

// Shape mapping based on texture name patterns
const getShape = (name) => {
  if (name.includes('sapling') || name.includes('flower') || name.includes('grass_plant') || name.includes('deadbush')) return 2; // CROSS
  if (name.includes('door') || name.includes('trapdoor') || name.includes('fence') || name.includes('rail')) return 3; // HASH
  return 1; // CUBE
};

// Detect multi-sided textures (top/side/bottom variants)
const getTextureVariants = (baseName, allFiles) => {
  const variants = {
    top: allFiles.find(f => f === `${baseName}_top.png`),
    side: allFiles.find(f => f === `${baseName}_side.png`),
    bottom: allFiles.find(f => f === `${baseName}_bottom.png`),
  };

  if (variants.top || variants.side || variants.bottom) {
    return variants;
  }
  return null;
};

// Convert texture name to readable name
const toReadableName = (textureName) => {
  return textureName
    .split('_')
    .map(word => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
};

// Generate description based on name
const generateDescription = (textureName) => {
  const name = toReadableName(textureName);
  if (textureName.includes('flower') || textureName.includes('plant')) return `${name} - Decorative plant`;
  if (textureName.includes('log') || textureName.includes('wood')) return `${name} - Natural wood material`;
  if (textureName.includes('leaves')) return `${name} - Tree foliage`;
  if (textureName.includes('planks')) return `${name} - Processed wood`;
  if (textureName.includes('stone') || textureName.includes('brick')) return `${name} - Building material`;
  if (textureName.includes('ore')) return `${name} - Mineable resource`;
  if (textureName.includes('glass')) return `${name} - Transparent block`;
  return `${name} block`;
};

// Check if block should be solid
const isSolidBlock = (textureName) => {
  // Solid blocks: stone, grass, dirt, planks, and most building materials
  if (textureName.includes('stone')) return true;
  if (textureName.includes('grass')) return true;
  if (textureName.includes('dirt')) return true;
  if (textureName.includes('planks')) return true;
  if (textureName.includes('log') || textureName.includes('wood')) return true;
  if (textureName.includes('brick')) return true;
  if (textureName.includes('ore')) return true;
  if (textureName.includes('sand')) return true;
  if (textureName.includes('gravel')) return true;
  if (textureName.includes('clay')) return true;
  if (textureName.includes('concrete')) return true;
  if (textureName.includes('bedrock')) return true;

  // Non-solid blocks
  if (textureName.includes('glass')) return false;
  if (textureName.includes('flower')) return false;
  if (textureName.includes('plant')) return false;
  if (textureName.includes('sapling')) return false;
  if (textureName.includes('leaves')) return false;
  if (textureName.includes('water')) return false;
  if (textureName.includes('lava')) return false;
  if (textureName.includes('air')) return false;

  // Default: solid
  return true;
};

// Create BlockType JSON
const createBlockType = (id, textureName) => {
  const shape = getShape(textureName);
  const basePath = `textures/block/basic/${textureName}.png`; // Removed 'assets/' prefix
  const name = toReadableName(textureName);
  const description = generateDescription(textureName);
  const isSolid = isSolidBlock(textureName);

  const modifier = {
    visibility: {
      shape,
      textures: {
        0: basePath // ALL texture key
      }
    }
  };

  // Add physics property if block is solid
  if (isSolid) {
    modifier.physics = {
      solid: true
    };
  }

  return {
    id,
    name,
    description,
    modifiers: {
      0: modifier // DEFAULT status
    }
  };
};

// Main execution
console.log('Generating BlockType JSON files...\n');

// Read all texture files
const files = fs.readdirSync(TEXTURES_DIR)
  .filter(f => f.endsWith('.png'))
  .map(f => f.replace('.png', ''));

// Filter out variants (_top, _side, _bottom, _1, _2, etc.) for base blocks
const baseBlocks = files.filter(f => !f.match(/_(top|side|bottom|front|back|left|right|\d+)$/));

console.log(`Found ${files.length} texture files`);
console.log(`Found ${baseBlocks.length} unique block types\n`);

// Create output directory structure
if (!fs.existsSync(OUTPUT_DIR)) {
  fs.mkdirSync(OUTPUT_DIR, { recursive: true });
}

// Generate JSON files
let currentId = START_ID;
const manifest = [];

baseBlocks.forEach((blockName) => {
  const blockType = createBlockType(currentId, blockName);

  // Create subdirectory based on (id % 100)
  const subDir = Math.floor(currentId / 100);
  const dirPath = path.join(OUTPUT_DIR, subDir.toString());

  if (!fs.existsSync(dirPath)) {
    fs.mkdirSync(dirPath, { recursive: true });
  }

  // Write JSON file
  const filePath = path.join(dirPath, `${currentId}.json`);
  fs.writeFileSync(filePath, JSON.stringify(blockType, null, 2));

  manifest.push({ id: currentId, name: blockName, file: `${subDir}/${currentId}.json` });

  console.log(`Created: ${subDir}/${currentId}.json (${blockName})`);
  currentId++;
});

// Write manifest
const manifestPath = path.join(OUTPUT_DIR, 'manifest.json');
fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));

console.log(`\n✓ Generated ${manifest.length} BlockType JSON files`);
console.log(`✓ IDs: ${START_ID}-${currentId - 1}`);
console.log(`✓ Manifest: ${manifestPath}`);
