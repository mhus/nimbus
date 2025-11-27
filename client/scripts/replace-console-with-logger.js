#!/usr/bin/env node

/**
 * Script to replace console.* calls with logger.* calls
 *
 * Replaces:
 * - console.log() -> logger.debug()
 * - console.error() -> logger.error()
 * - console.warn() -> logger.warn()
 * - console.info() -> logger.info()
 *
 * Skips files in test directories and CommandService exposeToBrowserConsole
 */

const fs = require('fs');
const path = require('path');
const { glob } = require('glob');

// Find all TypeScript files in engine package
const engineDir = path.join(__dirname, '../packages/engine/src');
const files = glob.sync('**/*.ts', {
  cwd: engineDir,
  ignore: ['**/*.test.ts', '**/*.spec.ts'],
});

let updatedCount = 0;
let skippedCount = 0;
const changes = [];

console.log(`Found ${files.length} files to process...\n`);

files.forEach(file => {
  const filePath = path.join(engineDir, file);
  let content = fs.readFileSync(filePath, 'utf8');
  const originalContent = content;

  // Skip CommandService.exposeToBrowserConsole() - it needs console.log for user output
  if (file.includes('CommandService.ts') && content.includes('exposeToBrowserConsole')) {
    // Only replace console calls outside of exposeToBrowserConsole method
    // This is complex, so we skip this file entirely
    skippedCount++;
    return;
  }

  // Count replacements for this file
  let fileChanges = 0;

  // Replace console.log -> logger.debug
  const logMatches = content.match(/console\.log\(/g);
  if (logMatches) {
    content = content.replace(/console\.log\(/g, 'logger.debug(');
    fileChanges += logMatches.length;
  }

  // Replace console.error -> logger.error
  const errorMatches = content.match(/console\.error\(/g);
  if (errorMatches) {
    content = content.replace(/console\.error\(/g, 'logger.error(');
    fileChanges += errorMatches.length;
  }

  // Replace console.warn -> logger.warn
  const warnMatches = content.match(/console\.warn\(/g);
  if (warnMatches) {
    content = content.replace(/console\.warn\(/g, 'logger.warn(');
    fileChanges += warnMatches.length;
  }

  // Replace console.info -> logger.info
  const infoMatches = content.match(/console\.info\(/g);
  if (infoMatches) {
    content = content.replace(/console\.info\(/g, 'logger.info(');
    fileChanges += infoMatches.length;
  }

  // If content changed, write it back
  if (content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf8');
    updatedCount++;
    changes.push({ file, count: fileChanges });
    console.log(`✓ Updated: ${file} (${fileChanges} replacements)`);
  } else {
    skippedCount++;
  }
});

console.log(`\n✅ Done!`);
console.log(`   Updated: ${updatedCount} files`);
console.log(`   Skipped: ${skippedCount} files`);
console.log(`   Total replacements: ${changes.reduce((sum, c) => sum + c.count, 0)}`);

if (changes.length > 0) {
  console.log('\nFiles with most changes:');
  changes
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
    .forEach(c => console.log(`   ${c.file}: ${c.count} changes`));
}
