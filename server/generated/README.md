# Generated Module

This module contains Java classes that are **generated** from TypeScript interface and enum definitions located in `client/packages/shared/src/types`.

## Purpose

The purpose of this module is to maintain a synchronized set of data classes between the TypeScript frontend and the Java backend. Instead of manually maintaining two copies of the same data structures, we generate the Java classes from the TypeScript definitions.

## Directory Structure

```
server/generated/
├── pom.xml
├── README.md
└── src/main/java/de/mhus/nimbus/generated/
    ├── Vector3.java          (example generated class)
    ├── Rotation.java         (example generated class)
    ├── BlockStatus.java      (example generated enum)
    └── ... (other generated classes)
```

## Generation Process

### Prerequisites

- **Node.js** must be installed on your system to run the generator script
- The TypeScript source files must be present in `client/packages/shared/src/types`

### How to Generate Java Classes

There are two ways to generate the Java classes:

#### Method 1: Using the Shell Script (Recommended)

Run the generation script directly:

```bash
cd /path/to/nimbus
./scripts/generate-java-from-typescript.sh
```

This will:
1. Parse all TypeScript files in `client/packages/shared/src/types`
2. Extract interfaces and enums
3. Generate corresponding Java classes in `server/generated/src/main/java/de/mhus/nimbus/generated`
4. Clean old generated files before creating new ones

#### Method 2: Using Maven Profile

Run the generation through Maven:

```bash
cd server/generated
mvn generate-sources -Pgenerate-from-typescript
```

Or from the project root:

```bash
cd /path/to/nimbus
mvn generate-sources -Pgenerate-from-typescript -pl server/generated
```

### After Generation

After generating the Java classes, build the module normally:

```bash
cd server/generated
mvn clean install
```

Or build the entire project:

```bash
cd /path/to/nimbus
mvn clean install
```

## Generated Class Structure

### TypeScript Interfaces → Java Classes

TypeScript interfaces are converted to Java classes with Lombok annotations:

**TypeScript:**
```typescript
export interface Vector3 {
  x: number;
  y: number;
  z: number;
}
```

**Generated Java:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vector3 {
    private double x;
    private double y;
    private double z;
}
```

### TypeScript Enums → Java Enums

TypeScript enums are converted to Java enums with value getters:

**TypeScript:**
```typescript
export enum BlockStatus {
  DEFAULT = 0,
  OPEN = 1,
  CLOSED = 2
}
```

**Generated Java:**
```java
public enum BlockStatus {
    DEFAULT(0),
    OPEN(1),
    CLOSED(2);

    private final int value;

    BlockStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### Optional Fields

TypeScript optional fields (marked with `?`) are converted to wrapper types in Java:

**TypeScript:**
```typescript
export interface Rotation {
  y: number;
  p: number;
  r?: number;  // optional
}
```

**Generated Java:**
```java
public class Rotation {
    private double y;
    private double p;
    private Double r;  // Optional field uses wrapper type
}
```

## Type Mapping

The generator uses the following type mappings:

| TypeScript Type | Java Type |
|----------------|-----------|
| `number` | `double` |
| `string` | `String` |
| `boolean` | `boolean` |
| `any` | `Object` |
| `Date` | `java.time.Instant` |
| `Type[]` | `java.util.List<Type>` |
| `Record<K, V>` | `java.util.Map<K, V>` |
| Custom types | Same name (assumed to be defined) |

## When to Regenerate

You should regenerate the Java classes whenever:

1. New TypeScript interfaces or enums are added
2. Existing TypeScript interfaces or enums are modified
3. TypeScript interfaces or enums are removed

## Important Notes

1. **DO NOT EDIT** the generated Java files manually. All changes will be lost on the next generation.
2. The generated files contain a header comment indicating they are auto-generated.
3. If you need custom behavior, create separate utility classes or extend the generated classes in a different package.
4. The generator script is located in `scripts/ts-to-java-generator.js`
5. The shell wrapper script is located in `scripts/generate-java-from-typescript.sh`

## Troubleshooting

### "Node.js not found" Error

If you see this error, install Node.js:
- macOS: `brew install node`
- Linux: `sudo apt-get install nodejs` or `sudo yum install nodejs`
- Windows: Download from https://nodejs.org/

### Generation Script Fails

1. Check that the TypeScript source files exist in `client/packages/shared/src/types`
2. Verify the script has execute permissions: `chmod +x scripts/generate-java-from-typescript.sh`
3. Check the Node.js script for syntax errors: `node scripts/ts-to-java-generator.js`

### Compilation Errors After Generation

1. Ensure all referenced types are also generated or exist in other modules
2. Check that Lombok is properly configured in the parent POM
3. Verify the generated code syntax by reviewing a few generated files

## Integration with Other Modules

Other server modules can depend on the `generated` module to use the generated classes:

```xml
<dependency>
    <groupId>de.mhus.nimbus</groupId>
    <artifactId>generated</artifactId>
    <version>${project.version}</version>
</dependency>
```

## Example Usage

```java
import de.mhus.nimbus.generated.Vector3;
import de.mhus.nimbus.generated.Rotation;
import de.mhus.nimbus.generated.BlockStatus;

// Using generated classes
Vector3 position = Vector3.builder()
    .x(10.0)
    .y(20.0)
    .z(30.0)
    .build();

Rotation rotation = Rotation.builder()
    .y(90.0)
    .p(45.0)
    .build();

BlockStatus status = BlockStatus.OPEN;
int statusValue = status.getValue(); // returns 1
```
