/**
 * Generate BlockTypes Manifest
 *
 * This script scans all BlockType JSON files in the blocktypes directory
 * and generates a fresh manifest.json file.
 *
 * The script:
 * - Recursively scans all subdirectories in client/packages/server/files/blocktypes/
 * - Reads all .json files (except manifest.json itself)
 * - Extracts id, name, and file path from each BlockType
 * - Generates a sorted manifest.json file
 * - Reports statistics and detects gaps in ID sequences
 *
 * Usage:
 *   node scripts/generate-manifest.js
 *
 * Output:
 *   client/packages/server/files/blocktypes/manifest.json
 *
 * Note: This script is idempotent - you can run it multiple times safely.
 *       It will always regenerate the manifest from scratch.
 */

const fs = require('fs');
const path = require('path');

/**
 * Recursively find all JSON files in a directory
 * @param {string} dir Directory to search
 * @param {string[]} fileList Accumulated file list
 * @returns {string[]} List of file paths
 */
function findJsonFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);

  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);

    if (stat.isDirectory()) {
      // Recursively search subdirectories
      findJsonFiles(filePath, fileList);
    } else if (file.endsWith('.json') && file !== 'manifest.json') {
      // Add JSON files (except manifest.json itself)
      fileList.push(filePath);
    }
  });

  return fileList;
}

/**
 * Extract BlockType info from JSON file
 * @param {string} filePath Path to JSON file
 * @param {string} baseDir Base blocktypes directory
 * @returns {object|null} Manifest entry or null if invalid
 */
function extractBlockTypeInfo(filePath, baseDir) {
  try {
    const data = fs.readFileSync(filePath, 'utf-8');
    const blockType = JSON.parse(data);

    // Validate required fields (id can be 0, so check for undefined/null)
    if (blockType.id === undefined || blockType.id === null || typeof blockType.id !== 'number') {
      console.warn(`⚠ Skipping ${filePath}: Missing or invalid 'id' field`);
      return null;
    }

    // Extract name (prefer explicit name, fall back to filename without extension)
    let name = blockType.name;
    if (!name) {
      const fileName = path.basename(filePath, '.json');
      name = fileName;
    }

    // Calculate relative file path from baseDir
    const relativePath = path.relative(baseDir, filePath);
    // Normalize path separators to forward slashes (for cross-platform compatibility)
    const normalizedPath = relativePath.split(path.sep).join('/');

    return {
      id: blockType.id,
      name: name,
      file: normalizedPath
    };
  } catch (error) {
    console.error(`✗ Error reading ${filePath}:`, error.message);
    return null;
  }
}

/**
 * Generate manifest.json
 * @param {string} blocktypesDir Path to blocktypes directory
 */
function generateManifest(blocktypesDir) {
  console.log('Scanning blocktypes directory...');
  console.log(`Directory: ${blocktypesDir}`);
  console.log('');

  // Find all JSON files
  const jsonFiles = findJsonFiles(blocktypesDir);
  console.log(`Found ${jsonFiles.length} JSON files`);
  console.log('');

  // Extract BlockType info
  const manifest = [];
  let successCount = 0;
  let skipCount = 0;

  jsonFiles.forEach(filePath => {
    const entry = extractBlockTypeInfo(filePath, blocktypesDir);
    if (entry) {
      manifest.push(entry);
      console.log(`✓ ID ${entry.id}: ${entry.name} (${entry.file})`);
      successCount++;
    } else {
      skipCount++;
    }
  });

  // Sort manifest by ID
  manifest.sort((a, b) => a.id - b.id);

  // Write manifest.json
  const manifestPath = path.join(blocktypesDir, 'manifest.json');
  fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');

  console.log('');
  console.log('='.repeat(60));
  console.log('Manifest generation complete!');
  console.log(`  Total files scanned: ${jsonFiles.length}`);
  console.log(`  Successfully processed: ${successCount}`);
  console.log(`  Skipped (invalid): ${skipCount}`);
  console.log(`  Output: ${manifestPath}`);
  console.log('='.repeat(60));

  // Show ID ranges
  if (manifest.length > 0) {
    const minId = manifest[0].id;
    const maxId = manifest[manifest.length - 1].id;
    console.log('');
    console.log('ID Ranges:');
    console.log(`  Min ID: ${minId}`);
    console.log(`  Max ID: ${maxId}`);
    console.log(`  Total BlockTypes: ${manifest.length}`);

    // Find gaps in ID sequence
    const gaps = [];
    for (let i = 0; i < manifest.length - 1; i++) {
      const currentId = manifest[i].id;
      const nextId = manifest[i + 1].id;
      if (nextId - currentId > 1) {
        gaps.push({ from: currentId + 1, to: nextId - 1 });
      }
    }

    if (gaps.length > 0) {
      console.log('');
      console.log('⚠ Gaps in ID sequence detected:');
      gaps.forEach(gap => {
        if (gap.from === gap.to) {
          console.log(`  - ID ${gap.from} is missing`);
        } else {
          console.log(`  - IDs ${gap.from}-${gap.to} are missing`);
        }
      });
    }
  }
}

/**
 * Main execution
 */
function main() {
  const blocktypesDir = path.join(__dirname, '..', 'client', 'packages', 'server', 'files', 'blocktypes');

  if (!fs.existsSync(blocktypesDir)) {
    console.error(`✗ BlockTypes directory not found: ${blocktypesDir}`);
    process.exit(1);
  }

  generateManifest(blocktypesDir);
}

// Run the script
main();
