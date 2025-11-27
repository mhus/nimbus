#!/usr/bin/env node

/**
 * Add missing logger imports to files that use logger but don't import it
 */

const fs = require('fs');
const path = require('path');
const { glob } = require('glob');

const engineDir = path.join(__dirname, '../packages/engine/src');
const files = glob.sync('**/*.ts', {
  cwd: engineDir,
  ignore: ['**/*.test.ts', '**/*.spec.ts'],
});

let fixedCount = 0;

files.forEach(file => {
  const filePath = path.join(engineDir, file);
  let content = fs.readFileSync(filePath, 'utf8');

  // Check if file uses logger but doesn't import it
  const usesLogger = /\blogger\.(debug|info|warn|error)\(/.test(content);
  const hasLoggerImport = /getLogger|import.*logger/i.test(content);

  if (usesLogger && !hasLoggerImport) {
    // Extract class/command name from file
    const fileName = path.basename(file, '.ts');

    // Find the first import statement
    const firstImportMatch = content.match(/^import .+ from .+;$/m);
    if (firstImportMatch) {
      const firstImport = firstImportMatch[0];
      const importIndex = content.indexOf(firstImport);
      const importEnd = importIndex + firstImport.length;

      // Check if there's already an import from @nimbus/shared
      const sharedImportMatch = content.match(/import\s*{([^}]+)}\s*from\s*['"]@nimbus\/shared['"]/);

      if (sharedImportMatch) {
        // Add getLogger to existing import
        const existingImports = sharedImportMatch[1];
        if (!existingImports.includes('getLogger')) {
          const newImports = existingImports + ', getLogger';
          content = content.replace(
            /import\s*{[^}]+}\s*from\s*['"]@nimbus\/shared['"]/,
            `import { ${newImports} } from '@nimbus/shared'`
          );

          // Add logger const after imports
          const lastImportMatch = content.match(/^import .+;$/gm);
          if (lastImportMatch) {
            const lastImport = lastImportMatch[lastImportMatch.length - 1];
            const lastImportIndex = content.lastIndexOf(lastImport);
            const lastImportEnd = lastImportIndex + lastImport.length;

            content =
              content.slice(0, lastImportEnd) +
              `\n\nconst logger = getLogger('${fileName}');` +
              content.slice(lastImportEnd);
          }
        }
      } else {
        // Add new import from @nimbus/shared
        content =
          content.slice(0, importEnd) +
          `\nimport { getLogger } from '@nimbus/shared';\n\nconst logger = getLogger('${fileName}');` +
          content.slice(importEnd);
      }

      fs.writeFileSync(filePath, content, 'utf8');
      fixedCount++;
      console.log(`✓ Fixed: ${file}`);
    }
  }
});

console.log(`\n✅ Done! Fixed ${fixedCount} files`);
