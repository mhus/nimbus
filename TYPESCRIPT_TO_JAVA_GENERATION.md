# TypeScript to Java Generation Setup

## Overview

This project includes an automated mechanism to generate Java classes from TypeScript interface and enum definitions. The TypeScript source files are located in `client/packages/shared/src/types` and the generated Java classes are placed in the `server/generated` module under the package `de.mhus.nimbus.generated`.

## Components

### 1. Generator Script
- **Location**: `scripts/ts-to-java-generator.js`
- **Type**: Node.js script
- **Function**: Parses TypeScript files and generates corresponding Java classes

### 2. Shell Wrapper
- **Location**: `scripts/generate-java-from-typescript.sh`
- **Type**: Bash script
- **Function**: Provides a convenient wrapper to run the generator

### 3. Generated Module
- **Location**: `server/generated`
- **Type**: Maven module
- **Function**: Contains the auto-generated Java classes

### 4. Maven Profile
- **Profile ID**: `generate-from-typescript`
- **Configuration**: In `server/generated/pom.xml`
- **Function**: Integrates the generation script into the Maven build process

## How to Use

### Quick Start

To generate Java classes from TypeScript definitions:

```bash
# From project root
./scripts/generate-java-from-typescript.sh
```

**Prerequisites**: Node.js must be installed

### Using Maven

```bash
# From project root
mvn generate-sources -Pgenerate-from-typescript -pl server/generated

# Or from the generated module
cd server/generated
mvn generate-sources -Pgenerate-from-typescript
```

### After Generation

Build the project normally:

```bash
mvn clean install
```

## What Gets Generated

### Source Files
- **Input**: TypeScript files in `client/packages/shared/src/types/*.ts`
- **Output**: Java files in `server/generated/src/main/java/de/mhus/nimbus/generated/*.java`

### Examples

The following sample classes are already generated:
- `Vector3.java` - 3D position/offset data class
- `Rotation.java` - 3D rotation data class with optional field
- `BlockStatus.java` - Enum with integer values

## Type Conversions

| TypeScript | Java |
|-----------|------|
| `interface` | `@Data class` (with Lombok) |
| `enum` | `enum` (with value getter) |
| `number` | `double` |
| `string` | `String` |
| `boolean` | `boolean` |
| `Type[]` | `List<Type>` |
| `Record<K,V>` | `Map<K,V>` |
| `field?` | Wrapper type (e.g., `Double` instead of `double`) |

## Key Features

1. **Repeatable**: The generation can be run multiple times without issues
2. **Automated**: Can be integrated into the build process via Maven profile
3. **Type-safe**: Proper mapping between TypeScript and Java types
4. **Lombok Integration**: Generated classes use Lombok annotations for boilerplate code
5. **Clean Generation**: Old generated files are removed before generating new ones

## Important Notes

1. **Do NOT edit generated files manually** - They will be overwritten on next generation
2. The generated files include a header comment: "DO NOT EDIT MANUALLY - This file is auto-generated"
3. Node.js is required to run the generator
4. The generation script is idempotent and can be run multiple times

## Documentation

For detailed documentation, see:
- **Main Documentation**: `server/generated/README.md`
- **Generator Script**: `scripts/ts-to-java-generator.js` (well-commented)
- **Shell Script**: `scripts/generate-java-from-typescript.sh`

## When to Regenerate

Regenerate Java classes whenever:
1. TypeScript interfaces or enums are added, modified, or removed
2. You need to sync the backend with frontend type definitions
3. Starting work after pulling changes that modified TypeScript types

## Troubleshooting

### "Node.js not found"
Install Node.js:
- macOS: `brew install node`
- Linux: `sudo apt-get install nodejs`
- Windows: Download from https://nodejs.org/

### Script doesn't have execute permissions
```bash
chmod +x scripts/generate-java-from-typescript.sh
chmod +x scripts/ts-to-java-generator.js
```

### Maven profile not found
Ensure you're running from the correct directory and the profile is defined in `server/generated/pom.xml`

## Integration with Other Modules

To use the generated classes in other server modules, add this dependency:

```xml
<dependency>
    <groupId>de.mhus.nimbus</groupId>
    <artifactId>generated</artifactId>
    <version>${project.version}</version>
</dependency>
```

Then import and use the classes:

```java
import de.mhus.nimbus.generated.Vector3;
import de.mhus.nimbus.generated.Rotation;

Vector3 pos = Vector3.builder()
    .x(10.0).y(20.0).z(30.0)
    .build();
```

## Architecture Decision

This approach was chosen to:
1. Maintain a single source of truth (TypeScript definitions)
2. Avoid manual synchronization between frontend and backend
3. Reduce the risk of type mismatches
4. Enable rapid iteration on shared data structures
5. Leverage existing TypeScript definitions without duplication
