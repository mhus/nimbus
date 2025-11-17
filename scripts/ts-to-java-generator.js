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
  'PositionRef': 'Object', // Union type alias - use Object as generic representation
  'MovementStateKey': 'String', // String literal union type
  'PoseType': 'String', // String literal union type
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
  
  // Parse type aliases first (needed for type resolution)
  // Use a more flexible regex that can handle multi-line definitions
  const typeAliasRegex = /export\s+type\s+(\w+)\s*=\s*([^;]+);/gs;
  let typeAliasMatch;
  while ((typeAliasMatch = typeAliasRegex.exec(content)) !== null) {
    const typeName = typeAliasMatch[1];
    let typeValue = typeAliasMatch[2].trim();
    
    // Map TypeScript type alias to Java type
    let javaType;
    
    // Handle array types (e.g., number[], AudioDefinition[])
    if (typeValue.endsWith('[]')) {
      const elementType = typeValue.slice(0, -2).trim();
      const javaElementType = mapTypeScriptTypeToJava(elementType);
      javaType = `java.util.List<${boxPrimitiveType(javaElementType)}>`;
    }
    // Handle tuple types (e.g., [number, number, number] or multi-line named tuples)
    else if (typeValue.match(/^(readonly\s+)?\[/)) {
      // Remove 'readonly' if present
      let tupleContent = typeValue.replace(/^readonly\s+/, '').trim();
      
      // Extract content between brackets
      const bracketMatch = tupleContent.match(/^\[(.+)\]$/s);
      if (bracketMatch) {
        tupleContent = bracketMatch[1];
        
        // Parse tuple elements - handle both simple and named tuples
        // For named tuples like "x: number, z: number", extract just the types
        const elements = tupleContent.split(',').map(e => e.trim()).filter(e => e);
        
        // Extract the type from each element (handle "name: type" or just "type")
        const types = elements.map(element => {
          // Match "name?: type" or "name: type" or just "type"
          const match = element.match(/(?:\w+\??\s*:\s*)?(\w+)/);
          return match ? match[1] : 'Object';
        });
        
        // Use the first type to determine the Java type (assume homogeneous)
        const elementType = types.length > 0 ? types[0] : 'Object';
        const javaElementType = mapTypeScriptTypeToJava(elementType);
        javaType = `java.util.List<${boxPrimitiveType(javaElementType)}>`;
      } else {
        javaType = 'java.util.List<Object>';
      }
    }
    // Handle union types (e.g., 'static' | 'kinematic' | 'dynamic')
    else if (typeValue.includes('|')) {
      // String literal unions -> String
      if (typeValue.includes("'") || typeValue.includes('"')) {
        javaType = 'String';
      } else {
        // Type unions -> Object
        javaType = 'Object';
      }
    }
    // Default mapping
    else {
      javaType = mapTypeScriptTypeToJava(typeValue);
    }
    
    // Add to TYPE_MAP for later resolution
    TYPE_MAP[typeName] = javaType;
  }
  
  // Parse enums
  const enumRegex = /export\s+enum\s+(\w+)\s*\{([^}]+)\}/g;
  let enumMatch;
  while ((enumMatch = enumRegex.exec(content)) !== null) {
    const enumName = enumMatch[1];
    const enumBody = enumMatch[2];
    
    const values = [];
    let enumValueType = 'int'; // default to int
    
    // Process enum body line by line
    const lines = enumBody.split('\n');
    for (const line of lines) {
      // Remove comments first
      const cleanLine = line.split('//')[0].trim();
      if (!cleanLine) continue;
      
      // Match enum value: NAME = value
      const valueMatch = cleanLine.match(/^\s*(\w+)\s*=\s*([^,]+)/);
      if (valueMatch) {
        const name = valueMatch[1];
        let rawValue = valueMatch[2].trim();
        
        // Remove trailing comma if present
        if (rawValue.endsWith(',')) {
          rawValue = rawValue.slice(0, -1).trim();
        }
        
        // Detect if value is a string (starts with ' or ")
        if (rawValue.startsWith("'") || rawValue.startsWith('"')) {
          enumValueType = 'String';
          // Convert single quotes to double quotes for Java
          const cleanValue = rawValue.replace(/^'|'$/g, '"');
          values.push({
            name: name,
            value: cleanValue
          });
        } else {
          values.push({
            name: name,
            value: rawValue
          });
        }
      }
    }
    
    result.enums.push({
      name: enumName,
      values: values,
      valueType: enumValueType
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
        
        // Handle Array<T> generic syntax
        if (fieldType.startsWith('Array<')) {
          const arrayMatch = fieldType.match(/Array<([^>]+)>/);
          if (arrayMatch) {
            let elementType = mapTypeScriptTypeToJava(arrayMatch[1].trim());
            elementType = boxPrimitiveType(elementType);
            fieldType = `java.util.List<${elementType}>`;
          }
        }
        // Handle tuple types (e.g., [number, number, number])
        else if (fieldType.startsWith('[') && fieldType.endsWith(']')) {
          // Extract tuple element types
          const tupleContent = fieldType.slice(1, -1);
          const tupleTypes = tupleContent.split(',').map(t => t.trim());
          // Get the first type and assume all are the same (common case)
          const elementType = tupleTypes.length > 0 ? tupleTypes[0] : 'Object';
          const javaElementType = mapTypeScriptTypeToJava(elementType);
          fieldType = `java.util.List<${boxPrimitiveType(javaElementType)}>`;
        }
        // Handle array types
        else if (fieldType.endsWith('[]')) {
          fieldType = fieldType.slice(0, -2);
          fieldType = mapTypeScriptTypeToJava(fieldType);
          fieldType = `java.util.List<${boxPrimitiveType(fieldType)}>`;
        }
        // Handle TypeScript Map types
        else if (fieldType.startsWith('Map<')) {
          const mapMatch = fieldType.match(/Map<([^,]+),\s*([^>]+)>/);
          if (mapMatch) {
            let keyType = mapTypeScriptTypeToJava(mapMatch[1].trim());
            let valueType = mapTypeScriptTypeToJava(mapMatch[2].trim());
            // Box primitive types for use in generics
            keyType = boxPrimitiveType(keyType);
            valueType = boxPrimitiveType(valueType);
            fieldType = `java.util.Map<${keyType}, ${valueType}>`;
          }
        }
        // Handle Record types
        else if (fieldType.startsWith('Record<')) {
          const recordMatch = fieldType.match(/Record<([^,]+),\s*([^>]+)>/);
          if (recordMatch) {
            let keyType = mapTypeScriptTypeToJava(recordMatch[1].trim());
            let valueType = mapTypeScriptTypeToJava(recordMatch[2].trim());
            // Box primitive types for use in generics
            keyType = boxPrimitiveType(keyType);
            valueType = boxPrimitiveType(valueType);
            fieldType = `java.util.Map<${keyType}, ${valueType}>`;
          }
        } else {
          fieldType = mapTypeScriptTypeToJava(fieldType);
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
  
  // Handle import() syntax: import('./Module').Type -> Type
  const importMatch = tsType.match(/import\(['"]\.\/\w+['"]\)\.(\w+)/);
  if (importMatch) {
    tsType = importMatch[1];
  }
  
  // Handle union types (e.g., 'string' | 'number' or Type1 | Type2)
  if (tsType.includes('|')) {
    // For string literal unions like 'server' | 'client', use String
    if (tsType.includes("'") || tsType.includes('"')) {
      return 'String';
    }
    // For type unions, use Object as generic fallback
    return 'Object';
  }
  
  // Handle object literals (e.g., { prop: type })
  if (tsType.startsWith('{')) {
    // Use Map<String, Object> as generic representation
    return 'java.util.Map<String, Object>';
  }
  
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
    
    const valueType = enumData.valueType || 'int';
    javaCode += `    private final ${valueType} value;\n\n`;
    javaCode += `    ${enumData.name}(${valueType} value) {\n`;
    javaCode += `        this.value = value;\n`;
    javaCode += `    }\n\n`;
    javaCode += `    public ${valueType} getValue() {\n`;
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
