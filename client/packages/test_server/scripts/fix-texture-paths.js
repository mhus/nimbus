/**
 * Fix texture paths in BlockType files
 * Removes "assets/" prefix from texture paths
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const blocktypesDir = path.join(__dirname, '../files/blocktypes');

let totalFiles = 0;
let fixedFiles = 0;
let totalReplacements = 0;

function processDirectory(dirPath) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });

  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);

    if (entry.isDirectory()) {
      // Recursively process subdirectories
      processDirectory(fullPath);
    } else if (entry.isFile() && entry.name.endsWith('.json')) {
      processJsonFile(fullPath);
    }
  }
}

function processJsonFile(filePath) {
  totalFiles++;

  try {
    // Read file
    const content = fs.readFileSync(filePath, 'utf-8');
    const data = JSON.parse(content);

    let modified = false;
    let fileReplacements = 0;

    // Process modifiers
    if (data.modifiers) {
      for (const statusKey in data.modifiers) {
        const modifier = data.modifiers[statusKey];

        if (modifier.visibility && modifier.visibility.textures) {
          const textures = modifier.visibility.textures;

          for (const textureKey in textures) {
            const texture = textures[textureKey];

            // If texture is a string
            if (typeof texture === 'string') {
              if (texture.startsWith('assets/')) {
                textures[textureKey] = texture.substring(7); // Remove "assets/"
                modified = true;
                fileReplacements++;
                totalReplacements++;
              }
            }
            // If texture is an object with path
            else if (typeof texture === 'object' && texture.path) {
              if (texture.path.startsWith('assets/')) {
                texture.path = texture.path.substring(7); // Remove "assets/"
                modified = true;
                fileReplacements++;
                totalReplacements++;
              }
            }
          }
        }
      }
    }

    // Write back if modified
    if (modified) {
      fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf-8');
      fixedFiles++;
      console.log(`✓ Fixed ${fileReplacements} textures in: ${path.relative(blocktypesDir, filePath)}`);
    }
  } catch (error) {
    console.error(`✗ Error processing ${filePath}:`, error.message);
  }
}

console.log('Starting texture path fix...');
console.log('Directory:', blocktypesDir);
console.log('');

processDirectory(blocktypesDir);

console.log('');
console.log('=== Summary ===');
console.log(`Total files processed: ${totalFiles}`);
console.log(`Files modified: ${fixedFiles}`);
console.log(`Total replacements: ${totalReplacements}`);
console.log('');
console.log('Done!');
