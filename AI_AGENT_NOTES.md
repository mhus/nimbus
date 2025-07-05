# AI Agent Development Notes for Nimbus Project

## Logging

Use lombok `@Slf4j` for logging in all 
classes. This provides the variable 'LOGGER' for logging purposes.

## Avro Timestamp Handling

### Important: Avro Timestamp Serialization

When working with Avro schemas that use `timestamp-millis` logical type, the generated Java classes expect `java.time.Instant` objects, NOT primitive `long` values.

**CORRECT Usage:**
```java
long currentTimestamp = Instant.now().toEpochMilli();

// For Avro setTimestamp calls, always convert back to Instant:
.setTimestamp(Instant.ofEpochMilli(currentTimestamp))
```

**INCORRECT Usage:**
```java
long currentTimestamp = Instant.now().toEpochMilli();

// This will cause compilation errors:
.setTimestamp(currentTimestamp)  // ERROR: incompatible types
```

### Affected Classes
- `LookupRequest` - timestamp field
- `LookupResponse` - timestamp field  
- `PlanetLookupRequest` - timestamp field
- `PlanetLookupResponse` - timestamp field
- `ServiceInstance` - lastHealthCheck field
- `PlanetWorld` - lastUpdate field

### Schema Definition
```json
{
  "name": "timestamp",
  "type": {
    "type": "long",
    "logicalType": "timestamp-millis"
  }
}
```

### Maven Avro Plugin Configuration
The Avro Maven plugin generates Java classes that use `Instant` for timestamp logical types when configured with:
```xml
<enableDecimalLogicalType>true</enableDecimalLogicalType>
```

## Common Patterns

### Creating Timestamps
```java
// Get current timestamp as millis
long currentTimestamp = Instant.now().toEpochMilli();

// Use in Avro builders
SomeAvroClass.newBuilder()
    .setTimestamp(Instant.ofEpochMilli(currentTimestamp))
    .build();
```

### Working with Response Times
```java
// For response processing
long responseTime = Instant.now().toEpochMilli();
return SomeResponse.newBuilder()
    .setTimestamp(Instant.ofEpochMilli(responseTime))
    .build();
```

## Debugging Tips
- If you see "incompatible types: long cannot be converted to java.time.Instant", use `Instant.ofEpochMilli()`
- Always check generated Avro classes for expected parameter types
- Use IDE autocompletion to verify method signatures

## Last Updated
July 5, 2025
