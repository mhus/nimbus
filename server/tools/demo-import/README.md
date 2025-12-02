# Demo Import Tool

Automated migration tool for importing test_server data into MongoDB.

## Quick Start

No parameters needed - just run:

```bash
cd server/tools/demo-import
mvn spring-boot:run
```

Or build and run JAR:

```bash
mvn clean package
java -jar target/demo-import-1.0.0-SNAPSHOT.jar
```

## What Gets Imported

### Phase 1: World Configuration
- **Source**: `../../client/packages/test_server/data/worlds/main/info.json`
- **Target**: MongoDB `worlds` collection
- **Count**: 1 world (main)

### Phase 2: Entity Templates
- **BlockTypes**: 614 templates from `files/blocktypes/`
- **ItemTypes**: 5 templates from `files/itemtypes/`
- **EntityModels**: 4 templates from `files/entitymodels/`
- **Backdrops**: 9 configs from `files/backdrops/`

### Phase 3: Entity Templates (Player)
- **Source**: `files/entity/player_entity.json`
- **Target**: MongoDB `w_entities` collection

### Phase 4: World Entity Instances
- **Source**: `data/worlds/main/entities/*.json`
- **Target**: MongoDB `w_entities` collection
- **Count**: ~10 entities (cows, NPCs, etc.)

### Phase 5: Assets
- **Source**: `files/assets/**/*` (recursive)
- **Metadata**: `*.info` files (description, width, height, color)
- **Target**: MongoDB `s_assets` collection
- **Count**: 641+ binary files with metadata

## Collections Created

| Collection | Count | Description |
|------------|-------|-------------|
| `worlds` | 1 | World configuration (main) |
| `w_blocktypes` | 614 | Block templates |
| `w_itemtypes` | 5 | Item templates |
| `w_entity_models` | 4 | 3D model templates |
| `w_backdrops` | 9 | Visual backdrop configs |
| `w_entities` | ~11 | Entity instances in world |
| `s_assets` | 641+ | Binary assets with metadata |
| **Total** | **~1,285** | **entities** |

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017
spring.data.mongodb.database=nimbus

# Source paths (relative to server/ directory)
import.source-path=../../client/packages/test_server/files
import.data-path=../../client/packages/test_server/data

# Defaults
import.default-world-id=main
import.batch-size=100
import.asset-batch-size=50
```

## Re-runnability

The tool can be run multiple times:

1. **First run**: Creates all entities
2. **Subsequent runs**:
   - Throws duplicate key errors for existing entities (MongoDB unique indexes)
   - This is expected behavior
   - Use MongoDB cleanup before re-import if needed

To clear database before import:

```bash
# Option 1: Drop collections manually in MongoDB
mongo nimbus --eval "db.w_blocktypes.drop(); db.w_itemtypes.drop(); ..."

# Option 2: Use local-cleanup tool
cd server/tools/local-cleanup
mvn spring-boot:run
```

## Performance

Typical import time:
- **Templates**: ~2 seconds (628 entities)
- **Entity Instances**: <1 second (11 entities)
- **Assets**: ~30-60 seconds (641+ binary files)
- **Total**: ~35-65 seconds

## Troubleshooting

### MongoDB Connection Error
```
Ensure MongoDB is running:
brew services start mongodb-community
```

### File Not Found Error
```
Check paths in application.properties are correct relative to server/ directory
```

### Duplicate Key Error
```
World/Entity already exists in database - this is normal for re-runs.
Clear database first if you want fresh import.
```

## Logs

Logs are written to:
- Console (INFO level)
- Progress updates every 50-100 entities
- Detailed DEBUG logs for troubleshooting

Set log level in `application.properties`:
```properties
logging.level.de.mhus.nimbus.tools.demoimport=DEBUG
```
