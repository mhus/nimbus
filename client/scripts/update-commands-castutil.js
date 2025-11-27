#!/usr/bin/env node

/**
 * Script to update Commands to use CastUtil
 *
 * This script automatically updates command files to use CastUtil functions
 * instead of manual parameter parsing.
 */

const fs = require('fs');
const path = require('path');
const { glob } = require('glob');

// Find all Command files
const commandsDir = path.join(__dirname, '../packages/engine/src/commands');
const commandFiles = glob.sync('**/*Command.ts', { cwd: commandsDir });

let updatedCount = 0;
let skippedCount = 0;

console.log(`Found ${commandFiles.length} command files to process...\n`);

commandFiles.forEach(file => {
  const filePath = path.join(commandsDir, file);
  let content = fs.readFileSync(filePath, 'utf8');
  let originalContent = content;
  let needsImport = false;
  let hasToBoolean = false;
  let hasToNumber = false;
  let hasToString = false;

  // Pattern 1: parameters[n].toLowerCase() === 'true'/'false' -> toBoolean
  if (content.includes(".toLowerCase() === 'true'") ||
      content.includes(".toLowerCase() === 'false'") ||
      content.includes('.toLowerCase() === "true"') ||
      content.includes('.toLowerCase() === "false"')) {

    // Replace patterns like: parameters[0].toLowerCase() === 'true'
    content = content.replace(
      /parameters\[(\d+)\]\.toLowerCase\(\)\s*===\s*['"]true['"]/g,
      'toBoolean(parameters[$1])'
    );

    // Replace patterns like: value.toLowerCase() === 'true' (where value = parameters[n])
    content = content.replace(
      /const\s+(\w+)\s*=\s*parameters\[(\d+)\]\.toLowerCase\(\);[\s\S]*?\1\s*===\s*['"]true['"]/g,
      (match, varName, index) => {
        return `const ${varName} = toBoolean(parameters[${index}]);`;
      }
    );

    needsImport = true;
    hasToBoolean = true;
  }

  // Pattern 2: parseFloat(parameters[n]) -> toNumber
  if (content.includes('parseFloat(parameters[')) {
    content = content.replace(
      /parseFloat\(parameters\[(\d+)\]\)/g,
      'toNumber(parameters[$1])'
    );
    needsImport = true;
    hasToNumber = true;
  }

  // Pattern 3: parseInt(parameters[n]) -> toNumber
  if (content.includes('parseInt(parameters[')) {
    content = content.replace(
      /parseInt\(parameters\[(\d+)\](?:,\s*\d+)?\)/g,
      'toNumber(parameters[$1])'
    );
    needsImport = true;
    hasToNumber = true;
  }

  // Pattern 4: Number(parameters[n]) -> toNumber
  if (content.includes('Number(parameters[')) {
    content = content.replace(
      /Number\(parameters\[(\d+)\]\)/g,
      'toNumber(parameters[$1])'
    );
    needsImport = true;
    hasToNumber = true;
  }

  // If content changed, add import if needed
  if (content !== originalContent && needsImport) {
    // Build import list
    const imports = [];
    if (hasToBoolean) imports.push('toBoolean');
    if (hasToNumber) imports.push('toNumber');
    if (hasToString) imports.push('toString');

    // Check if @nimbus/shared import exists
    const sharedImportRegex = /import\s+{([^}]+)}\s+from\s+['"]@nimbus\/shared['"]/;
    const match = content.match(sharedImportRegex);

    if (match) {
      // Add to existing import
      const existingImports = match[1].split(',').map(s => s.trim());
      const newImports = [...new Set([...existingImports, ...imports])];
      content = content.replace(
        sharedImportRegex,
        `import { ${newImports.join(', ')} } from '@nimbus/shared'`
      );
    } else {
      // Add new import after first import
      const firstImportIndex = content.indexOf('import');
      if (firstImportIndex !== -1) {
        const firstImportEnd = content.indexOf(';', firstImportIndex);
        content =
          content.slice(0, firstImportEnd + 1) +
          `\nimport { ${imports.join(', ')} } from '@nimbus/shared';` +
          content.slice(firstImportEnd + 1);
      }
    }

    // Also update parameter type from string[] to any[]
    content = content.replace(
      /async execute\(parameters:\s*string\[\]\)/g,
      'async execute(parameters: any[])'
    );

    // Write updated content
    fs.writeFileSync(filePath, content, 'utf8');
    updatedCount++;
    console.log(`✓ Updated: ${file}`);
  } else {
    skippedCount++;
  }
});

console.log(`\n✅ Done! Updated ${updatedCount} files, skipped ${skippedCount} files.`);
