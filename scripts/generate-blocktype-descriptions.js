#!/usr/bin/env node
/**
 * Generate descriptions for BlockTypes using Gemini API
 *
 * This script:
 * - Reads all BlockType JSON files
 * - Collects referenced assets and their .info files
 * - Sends data to Gemini API to generate descriptions
 * - Updates BlockType files with new descriptions
 *
 * Usage:
 *   node scripts/generate-blocktype-descriptions.js [--api-key KEY] [--overwrite] [--delay SECONDS]
 *
 * Environment:
 *   GOOGLE_API_KEY: Gemini API key
 */

const fs = require('fs').promises;
const path = require('path');
const { GoogleGenerativeAI } = require('@google/generative-ai');

// Configuration
const BLOCKTYPES_DIR = path.join(__dirname, '../client/packages/server/files/blocktypes');
const ASSETS_DIR = path.join(__dirname, '../client/packages/server/files/assets');
const DEFAULT_DELAY = 6.5; // seconds between API calls (~9 req/min)
const DEFAULT_MODEL = 'gemini-2.0-flash-exp';

/**
 * Parse command line arguments
 */
function parseArgs() {
  const args = {
    apiKey: process.env.GOOGLE_API_KEY,
    overwrite: false,
    delay: DEFAULT_DELAY,
    help: false,
  };

  for (let i = 2; i < process.argv.length; i++) {
    const arg = process.argv[i];

    if (arg === '--api-key' && i + 1 < process.argv.length) {
      args.apiKey = process.argv[++i];
    } else if (arg === '--overwrite') {
      args.overwrite = true;
    } else if (arg === '--delay' && i + 1 < process.argv.length) {
      args.delay = parseFloat(process.argv[++i]);
    } else if (arg === '--help' || arg === '-h') {
      args.help = true;
    }
  }

  return args;
}

/**
 * Show help message
 */
function showHelp() {
  console.log(`
Generate descriptions for BlockTypes using Gemini API

Usage:
  node scripts/generate-blocktype-descriptions.js [OPTIONS]

Options:
  --api-key KEY     Gemini API key (or set GOOGLE_API_KEY env variable)
  --overwrite       Overwrite existing descriptions (default: skip if description_old exists)
  --delay SECONDS   Delay between API calls (default: 6.5s for ~9 req/min)
  --help, -h        Show this help message

Environment:
  GOOGLE_API_KEY    Gemini API key

Examples:
  node scripts/generate-blocktype-descriptions.js
  node scripts/generate-blocktype-descriptions.js --overwrite
  node scripts/generate-blocktype-descriptions.js --delay 10
`);
}

/**
 * Find all BlockType JSON files
 */
async function findBlockTypes() {
  const blockTypes = [];

  try {
    const dirs = await fs.readdir(BLOCKTYPES_DIR);

    for (const dir of dirs) {
      const dirPath = path.join(BLOCKTYPES_DIR, dir);
      const stat = await fs.stat(dirPath);

      if (stat.isDirectory()) {
        const files = await fs.readdir(dirPath);

        for (const file of files) {
          if (file.endsWith('.json')) {
            const filePath = path.join(dirPath, file);
            blockTypes.push(filePath);
          }
        }
      }
    }
  } catch (error) {
    console.error('Error finding BlockTypes:', error.message);
    throw error;
  }

  return blockTypes;
}

/**
 * Extract asset paths from BlockType
 */
function extractAssetPaths(blockType) {
  const assets = new Set();

  // Extract from modifiers
  if (blockType.modifiers) {
    for (const status in blockType.modifiers) {
      const modifier = blockType.modifiers[status];

      // Check visibility.textures
      if (modifier.visibility?.textures) {
        for (const face in modifier.visibility.textures) {
          const texture = modifier.visibility.textures[face];
          if (texture) {
            assets.add(texture);
          }
        }
      }

      // Check visibility.model
      if (modifier.visibility?.model) {
        assets.add(modifier.visibility.model);
      }
    }
  }

  return Array.from(assets);
}

/**
 * Read asset .info file
 */
async function readAssetInfo(assetPath) {
  const fullPath = path.join(ASSETS_DIR, assetPath);
  const infoPath = `${fullPath}.info`;

  try {
    const infoData = await fs.readFile(infoPath, 'utf-8');
    return JSON.parse(infoData);
  } catch (error) {
    // .info file doesn't exist or is invalid
    return null;
  }
}

/**
 * Collect asset information for BlockType
 */
async function collectAssetInfo(blockType) {
  const assetPaths = extractAssetPaths(blockType);
  const assetInfos = [];

  for (const assetPath of assetPaths) {
    const info = await readAssetInfo(assetPath);
    assetInfos.push({
      path: assetPath,
      info: info || { description: 'No description available' }
    });
  }

  return assetInfos;
}

/**
 * Generate description using Gemini API
 */
async function generateDescription(genAI, blockType, assetInfos, retryCount = 0) {
  try {
    // Create model
    const model = genAI.getGenerativeModel({ model: DEFAULT_MODEL });

    // Build prompt
    const prompt = buildPrompt(blockType, assetInfos);

    // Generate description
    const result = await model.generateContent(prompt);
    const response = await result.response;
    const text = response.text();

    return text.trim();
  } catch (error) {
    const errorStr = error.toString();

    // Check for rate limit (429)
    if (errorStr.includes('429') || errorStr.toLowerCase().includes('quota') || errorStr.toLowerCase().includes('rate limit')) {
      if (retryCount < 3) {
        const retryDelay = 30; // 30 seconds default
        console.log(`  Rate limit hit. Waiting ${retryDelay} seconds before retry...`);
        await sleep(retryDelay * 1000);
        return generateDescription(genAI, blockType, assetInfos, retryCount + 1);
      } else {
        throw new Error(`Rate limit error after ${retryCount} retries: ${error.message}`);
      }
    }

    throw error;
  }
}

/**
 * Build prompt for Gemini
 */
function buildPrompt(blockType, assetInfos) {
  let prompt = `You are analyzing a BlockType definition from a voxel game engine. Generate a concise, descriptive text (1-2 sentences) for this block type.

BlockType Data:
- ID: ${blockType.id}
- Name: ${blockType.name}
- Current Description: ${blockType.description || 'None'}

Block Properties:
`;

  // Add modifier information
  if (blockType.modifiers) {
    for (const status in blockType.modifiers) {
      const modifier = blockType.modifiers[status];
      prompt += `\nStatus ${status}:\n`;

      if (modifier.visibility) {
        prompt += `  - Shape: ${modifier.visibility.shape || 'default'}\n`;
      }

      if (modifier.physics) {
        prompt += `  - Solid: ${modifier.physics.solid ? 'yes' : 'no'}\n`;
        if (modifier.physics.unbreakable) {
          prompt += `  - Unbreakable: yes\n`;
        }
      }

      if (modifier.wind) {
        prompt += `  - Wind Effect: leafiness=${modifier.wind.leafiness || 0}, stability=${modifier.wind.stability || 0}\n`;
      }
    }
  }

  // Add asset information
  if (assetInfos.length > 0) {
    prompt += `\nAssets Used:\n`;
    for (const asset of assetInfos) {
      prompt += `- ${asset.path}\n`;
      if (asset.info.description) {
        prompt += `  Description: ${asset.info.description}\n`;
      }
    }
  }

  prompt += `\nGenerate a brief, descriptive text for this block type. Focus on:
1. What the block represents (based on name and assets)
2. Its physical properties (solid, transparent, etc.)
3. Its purpose or use in the game

Keep it concise (1-2 sentences) and informative.`;

  return prompt;
}

/**
 * Update BlockType file with new description
 */
async function updateBlockType(filePath, blockType, newDescription, overwrite) {
  // Check if description_old exists (safety flag)
  if (blockType.description_old && !overwrite) {
    console.log(`  Skipping: description_old already exists`);
    return false;
  }

  // Save old description
  blockType.description_old = blockType.description || '';

  // Set new description
  blockType.description = newDescription;

  // Write back to file
  try {
    await fs.writeFile(filePath, JSON.stringify(blockType, null, 2) + '\n', 'utf-8');
    console.log(`  Updated: ${filePath}`);
    return true;
  } catch (error) {
    console.error(`  Error writing file: ${error.message}`);
    return false;
  }
}

/**
 * Sleep for ms milliseconds
 */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * Main function
 */
async function main() {
  const args = parseArgs();

  if (args.help) {
    showHelp();
    return;
  }

  if (!args.apiKey) {
    console.error('Error: No API key provided.');
    console.error('Set GOOGLE_API_KEY environment variable or use --api-key option.');
    console.error('Run with --help for more information.');
    process.exit(1);
  }

  console.log('Generate BlockType Descriptions');
  console.log('================================\n');
  console.log(`BlockTypes directory: ${BLOCKTYPES_DIR}`);
  console.log(`Assets directory: ${ASSETS_DIR}`);
  console.log(`Mode: ${args.overwrite ? 'Overwrite all' : 'Skip if description_old exists'}`);
  console.log(`Delay between API calls: ${args.delay}s (~${Math.floor(60 / args.delay)} req/min)`);
  console.log();

  // Initialize Gemini
  const genAI = new GoogleGenerativeAI(args.apiKey);

  // Find all BlockTypes
  console.log('Finding BlockTypes...');
  const blockTypeFiles = await findBlockTypes();
  console.log(`Found ${blockTypeFiles.length} BlockType files\n`);

  if (blockTypeFiles.length === 0) {
    console.log('No BlockTypes found.');
    return;
  }

  // Process each BlockType
  let processed = 0;
  let skipped = 0;
  let errors = 0;

  for (let i = 0; i < blockTypeFiles.length; i++) {
    const filePath = blockTypeFiles[i];
    const fileName = path.basename(filePath);

    console.log(`[${i + 1}/${blockTypeFiles.length}] Processing: ${fileName}`);

    try {
      // Read BlockType
      const fileData = await fs.readFile(filePath, 'utf-8');
      const blockType = JSON.parse(fileData);

      console.log(`  ID: ${blockType.id}, Name: ${blockType.name}`);

      // Check if already has description_old (unless overwrite)
      if (blockType.description_old && !args.overwrite) {
        console.log(`  Skipping: description_old already exists`);
        skipped++;
        console.log();
        continue;
      }

      // Collect asset information
      const assetInfos = await collectAssetInfo(blockType);
      console.log(`  Found ${assetInfos.length} assets`);

      // Generate description
      console.log(`  Generating description...`);
      const newDescription = await generateDescription(genAI, blockType, assetInfos);
      console.log(`  Generated: ${newDescription.substring(0, 80)}${newDescription.length > 80 ? '...' : ''}`);

      // Update BlockType file
      const updated = await updateBlockType(filePath, blockType, newDescription, args.overwrite);

      if (updated) {
        processed++;
      } else {
        skipped++;
      }

      console.log();

      // Add delay between API calls (except for last one)
      if (i < blockTypeFiles.length - 1) {
        await sleep(args.delay * 1000);
      }
    } catch (error) {
      console.error(`  Error: ${error.message}`);
      errors++;
      console.log();
    }
  }

  console.log('Completed!');
  console.log(`  Processed: ${processed}`);
  console.log(`  Skipped: ${skipped}`);
  console.log(`  Errors: ${errors}`);
  console.log(`  Total: ${blockTypeFiles.length}`);
}

// Run main
main().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
