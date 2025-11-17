#!/usr/bin/env node

/**
 * TypeScript to Java Generator
 * 
 * This script parses TypeScript interface and enum definitions from
 * client/packages/shared/src/types and generates corresponding Java classes
 * in server/generated/src/main/java/de/mhus/nimbus/generated
 * 
 * Usage: node ts-to-java-generator.js
 */

const fs = require('fs');
const path = require('path');

// Configuration
const TS_SOURCE_DIR = path.join(__dirname, '../client/packages/shared/src/types');
const JAVA_OUTPUT_DIR = path.join(__dirname, '../server/generated/src/main/java/de/mhus/nimbus/generated');
const JAVA_PACKAGE = 'de.mhus.nimbus.generated';

// Type mapping from TypeScript to Java
const TYPE_MAP = {
  'number': 'double',
  'string': 'String',
  'boolean': 'boolean',
  'any': 'Object',
  'void': 'void',
  'Date': 'java.time.Instant',
  'Vector3': 'Vector3',
  'Rotation': 'Rotation',
  'BlockModifier': 'BlockModifier',
  'Shape': 'Shape',
  'Color': 'Color',
};

/**
 * Parse TypeScript file and extract interfaces and enums
 */
function parseTypeScriptFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf-8');
  const fileName = path.basename(filePath, '.ts');
  
  const result = {
    interfaces: [],
    enums: [],
    imports: new Set()
  };
  
  // Parse enums
  const enumRegex = /export\s+enum\s+(\w+)\s*\{([^}]+)\}/g;
  let enumMatch;
  while ((enumMatch = enumRegex.exec(content)) !== null) {
    const enumName = enumMatch[1];
    const enumBody = enumMatch[2];
    
    const values = [];
    const valueRegex = /(\w+)\s*=\s*([^,\n]+)/g;
    let valueMatch;
    while ((valueMatch = valueRegex.exec(enumBody)) !== null) {
      values.push({
        name: valueMatch[1],
        value: valueMatch[2].trim()
      });
    }
    
    result.enums.push({
      name: enumName,
      values: values
    });
  }
  
  // Parse interfaces
  const interfaceRegex = /export\s+interface\s+(\w+)\s*\{([^}]+)\}/g;
  let interfaceMatch;
  while ((interfaceMatch = interfaceRegex.exec(content)) !== null) {
    const interfaceName = interfaceMatch[1];
    const interfaceBody = interfaceMatch[2];
    
    const fields = [];
    const lines = interfaceBody.split('\n');
    
    for (const line of lines) {
      // Match field: name: type or name?: type
      const fieldMatch = line.match(/^\s*(\w+)(\?)?:\s*([^;]+);?\s*$/);
      if (fieldMatch) {
        const fieldName = fieldMatch[1];
        const isOptional = !!fieldMatch[2];
        let fieldType = fieldMatch[3].trim();
        
        // Handle array types
        const isArray = fieldType.endsWith('[]');
        if (isArray) {
          fieldType = fieldType.slice(0, -2);
        }
        
        // Handle Record types
        if (fieldType.startsWith('Record<')) {
          const recordMatch = fieldType.match(/Record<([^,]+),\s*([^>]+)>/);
          if (recordMatch) {
            const keyType = mapTypeScriptTypeToJava(recordMatch[1].trim());
            const valueType = mapTypeScriptTypeToJava(recordMatch[2].trim());
            fieldType = `java.util.Map<${keyType}, ${valueType}>`;
          }
        } else {
          fieldType = mapTypeScriptTypeToJava(fieldType);
          if (isArray) {
            fieldType = `java.util.List<${boxPrimitiveType(fieldType)}>`;
          }
        }
        
        fields.push({
          name: fieldName,
          type: fieldType,
          optional: isOptional
        });
      }
    }
    
    result.interfaces.push({
      name: interfaceName,
      fields: fields
    });
  }
  
  // Extract imports
  const importRegex = /import\s+(?:type\s+)?\{([^}]+)\}\s+from\s+['"]\.\/(\w+)['"]/g;
  let importMatch;
  while ((importMatch = importRegex.exec(content)) !== null) {
    const importedTypes = importMatch[1].split(',').map(t => t.trim());
    importedTypes.forEach(type => result.imports.add(type));
  }
  
  return result;
}

/**
 * Map TypeScript type to Java type
 */
function mapTypeScriptTypeToJava(tsType) {
  // Remove any whitespace
  tsType = tsType.trim();
  
  // Check if it's in our type map
  if (TYPE_MAP[tsType]) {
    return TYPE_MAP[tsType];
  }
  
  // If it's not in the map, assume it's a custom type
  return tsType;
}

/**
 * Box primitive types for use in generics
 */
function boxPrimitiveType(javaType) {
  const boxingMap = {
    'int': 'Integer',
    'long': 'Long',
    'double': 'Double',
    'float': 'Float',
    'boolean': 'Boolean',
    'char': 'Character',
    'byte': 'Byte',
    'short': 'Short'
  };
  return boxingMap[javaType] || javaType;
}

/**
 * Generate Java class from parsed TypeScript data
 */
function generateJavaClass(parsedData, fileName) {
  const javaClasses = [];
  
  // Generate enums
  for (const enumData of parsedData.enums) {
    let javaCode = `package ${JAVA_PACKAGE};\n\n`;
    javaCode += `/**\n`;
    javaCode += ` * Generated from ${fileName}.ts\n`;
    javaCode += ` * DO NOT EDIT MANUALLY - This file is auto-generated\n`;
    javaCode += ` */\n`;
    javaCode += `public enum ${enumData.name} {\n`;
    
    const enumValues = enumData.values.map(v => `    ${v.name}(${v.value})`).join(',\n');
    javaCode += enumValues + ';\n\n';
    
    javaCode += `    private final int value;\n\n`;
    javaCode += `    ${enumData.name}(int value) {\n`;
    javaCode += `        this.value = value;\n`;
    javaCode += `    }\n\n`;
    javaCode += `    public int getValue() {\n`;
    javaCode += `        return value;\n`;
    javaCode += `    }\n`;
    javaCode += `}\n`;
    
    javaClasses.push({
      name: enumData.name,
      code: javaCode
    });
  }
  
  // Generate interfaces as Java classes with Lombok
  for (const interfaceData of parsedData.interfaces) {
    let javaCode = `package ${JAVA_PACKAGE};\n\n`;
    
    // Add imports
    const imports = new Set();
    imports.add('lombok.Data');
    imports.add('lombok.Builder');
    imports.add('lombok.NoArgsConstructor');
    imports.add('lombok.AllArgsConstructor');
    
    for (const field of interfaceData.fields) {
      if (field.type.includes('java.util.List')) {
        imports.add('java.util.List');
      }
      if (field.type.includes('java.util.Map')) {
        imports.add('java.util.Map');
      }
      if (field.type.includes('java.time.')) {
        imports.add(field.type.split('<')[0]);
      }
    }
    
    Array.from(imports).sort().forEach(imp => {
      javaCode += `import ${imp};\n`;
    });
    
    javaCode += `\n/**\n`;
    javaCode += ` * Generated from ${fileName}.ts\n`;
    javaCode += ` * DO NOT EDIT MANUALLY - This file is auto-generated\n`;
    javaCode += ` */\n`;
    javaCode += `@Data\n`;
    javaCode += `@Builder\n`;
    javaCode += `@NoArgsConstructor\n`;
    javaCode += `@AllArgsConstructor\n`;
    javaCode += `public class ${interfaceData.name} {\n`;
    
    for (const field of interfaceData.fields) {
      javaCode += `\n    /**\n`;
      javaCode += `     * ${field.name}${field.optional ? ' (optional)' : ''}\n`;
      javaCode += `     */\n`;
      javaCode += `    private ${field.type} ${field.name};\n`;
    }
    
    javaCode += `}\n`;
    
    javaClasses.push({
      name: interfaceData.name,
      code: javaCode
    });
  }
  
  return javaClasses;
}

/**
 * Main function
 */
function main() {
  console.log('TypeScript to Java Generator');
  console.log('============================\n');
  
  // Ensure output directory exists
  if (!fs.existsSync(JAVA_OUTPUT_DIR)) {
    fs.mkdirSync(JAVA_OUTPUT_DIR, { recursive: true });
  }
  
  // Get all TypeScript files
  const tsFiles = fs.readdirSync(TS_SOURCE_DIR)
    .filter(file => file.endsWith('.ts') && !file.endsWith('.d.ts'))
    .map(file => path.join(TS_SOURCE_DIR, file));
  
  console.log(`Found ${tsFiles.length} TypeScript files\n`);
  
  let totalGenerated = 0;
  
  // Process each file
  for (const tsFile of tsFiles) {
    const fileName = path.basename(tsFile, '.ts');
    console.log(`Processing ${fileName}.ts...`);
    
    try {
      const parsedData = parseTypeScriptFile(tsFile);
      const javaClasses = generateJavaClass(parsedData, fileName);
      
      // Write Java files
      for (const javaClass of javaClasses) {
        const outputPath = path.join(JAVA_OUTPUT_DIR, `${javaClass.name}.java`);
        fs.writeFileSync(outputPath, javaClass.code);
        console.log(`  ✓ Generated ${javaClass.name}.java`);
        totalGenerated++;
      }
    } catch (error) {
      console.error(`  ✗ Error processing ${fileName}: ${error.message}`);
    }
  }
  
  console.log(`\n✓ Generation complete! Generated ${totalGenerated} Java classes.`);
}

// Run the generator
if (require.main === module) {
  main();
}

module.exports = { parseTypeScriptFile, generateJavaClass };
