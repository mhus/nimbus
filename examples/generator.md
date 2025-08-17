# World Generator Service API Examples

Diese Datei enthält curl-Beispiele für alle Endpunkte des World Generator Service.

## Authentifizierung

Alle API-Aufrufe benötigen einen Bearer Token mit dem Shared Secret:

```bash
export GENERATOR_TOKEN="generator-secret-key-2024"
export GENERATOR_URL="http://localhost:8083"
```

## 1. World Generator erstellen

Erstellt einen neuen World Generator mit Namen, Beschreibung und Parametern.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Fantasy World",
    "description": "Ein magisches Fantasy-Universum mit Drachen und Zauberei",
    "parameters": {
      "worldSize": 1000,
      "primaryBiome": "forest",
      "seed": 123456,
      "worldType": "fantasy",
      "itemDensity": 3,
      "structureDensity": 3,
      "questDensity": 3,
      "historyDepth": 5
    }
  }'
```

## 2. Simple World Generator mit Simple-Prozessoren erstellen

Erstellt einen World Generator der die Simple-Implementierungen verwendet.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Simple Fantasy World",
    "description": "Einfache Fantasywelt mit Simple-Generatoren",
    "parameters": {
      "worldSize": 500,
      "primaryBiome": "mixed",
      "seed": 42,
      "worldType": "fantasy",
      "itemDensity": 2,
      "structureDensity": 2,
      "questDensity": 2,
      "historyDepth": 3,
      "useSimpleProcessors": true
    }
  }'
```

## 3. Phase zu World Generator hinzufügen

Fügt eine Generierungsphase zu einem bestehenden World Generator hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleInitializationProcessor",
    "name": "Simple Initialisierung",
    "description": "Grundlegende Weltinitialisierung mit Simple-Generator",
    "phaseOrder": 1,
    "parameters": {
      "worldSize": 500,
      "primaryBiome": "forest",
      "seed": 42
    }
  }'
```

## 4. Simple Asset-Generation Phase

Fügt eine Asset-Generierungsphase hinzu, die PNG-Texturen erstellt.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleAssetProcessor",
    "name": "Simple Asset Generierung",
    "description": "Erstellt Texturen für Materialien wie Gras, Sand, Wasser, Felsen",
    "phaseOrder": 2,
    "parameters": {
      "textureSize": 64,
      "generateNoise": true
    }
  }'
```

## 5. Simple Kontinent-Generation Phase

Fügt eine Kontinent-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleContinentProcessor",
    "name": "Simple Kontinent Generierung",
    "description": "Erstellt Kontinente mit Wald, Wüste, Ozean und Bergen",
    "phaseOrder": 3,
    "parameters": {
      "worldSize": 500,
      "seed": 42,
      "continentCount": 3
    }
  }'
```

## 6. Flaches Terrain generieren

Fügt eine Flachland-Terrain-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "FlatTerrainProcessor",
    "name": "Flachland Terrain",
    "description": "Generiert flaches Terrain mit minimaler Höhenvariation",
    "phaseOrder": 4,
    "parameters": {
      "worldSize": 500,
      "seed": 42,
      "baseHeight": 50,
      "maxVariation": 5
    }
  }'
```

## 7. Bergiges Terrain generieren

Fügt eine Berg-Terrain-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "MountainTerrainProcessor",
    "name": "Berg Terrain",
    "description": "Generiert bergiges Terrain mit Gipfeln, Tälern und alpinen Features",
    "phaseOrder": 4,
    "parameters": {
      "worldSize": 500,
      "seed": 42,
      "mountainRanges": 3,
      "maxHeight": 2000
    }
  }'
```

## 8. Historische Generierung

Fügt eine historische Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleHistoryProcessor",
    "name": "Weltgeschichte",
    "description": "Generiert Zeitalter, Ereignisse und kulturelle Entwicklungen",
    "phaseOrder": 5,
    "parameters": {
      "seed": 42,
      "historyDepth": 5,
      "civilizationCount": 4
    }
  }'
```

## 9. Struktur-Generierung

Fügt eine Struktur-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleStructureProcessor",
    "name": "Strukturen und Siedlungen",
    "description": "Generiert Dörfer, Städte, Festungen und andere Bauwerke",
    "phaseOrder": 6,
    "parameters": {
      "worldSize": 500,
      "seed": 42,
      "structureDensity": 3,
      "settlementCount": 5
    }
  }'
```

## 10. Item-Generierung

Fügt eine Item-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleItemProcessor",
    "name": "Items und Ausrüstung",
    "description": "Generiert Waffen, Rüstung, Werkzeuge und Schätze",
    "phaseOrder": 7,
    "parameters": {
      "seed": 42,
      "itemDensity": 3,
      "worldType": "fantasy",
      "includeMagicalItems": true
    }
  }'
```

## 11. Quest-Generierung

Fügt eine Quest-Generierungsphase hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "SimpleQuestProcessor",
    "name": "Quests und NPCs",
    "description": "Generiert Aufgaben, NPCs und Storylines",
    "phaseOrder": 8,
    "parameters": {
      "seed": 42,
      "questDensity": 3,
      "worldTheme": "fantasy",
      "npcCount": 20,
      "storylineCount": 2
    }
  }'
```

## 12. Komplette Simple World mit allen Phasen erstellen

Bash-Script zum Erstellen einer kompletten Welt mit allen Simple-Prozessoren:

```bash
#!/bin/bash

export GENERATOR_TOKEN="generator-secret-key-2024"
export GENERATOR_URL="http://localhost:8083"

echo "=== Erstelle Simple Fantasy World ==="

# 1. World Generator erstellen
GENERATOR_RESPONSE=$(curl -s -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Complete Simple World",
    "description": "Vollständige Fantasywelt mit allen Simple-Generatoren",
    "parameters": {
      "worldSize": 500,
      "primaryBiome": "mixed",
      "seed": 12345,
      "worldType": "fantasy",
      "itemDensity": 3,
      "structureDensity": 3,
      "questDensity": 3,
      "historyDepth": 5
    }
  }')

GENERATOR_ID=$(echo $GENERATOR_RESPONSE | grep -o '"id":[0-9]*' | grep -o '[0-9]*')
echo "World Generator erstellt mit ID: $GENERATOR_ID"

# 2. Alle Phasen hinzufügen
PHASES=(
  '{"processor": "SimpleInitializationProcessor", "name": "Initialisierung", "description": "Weltinitialisierung", "phaseOrder": 1, "parameters": {"worldSize": 500, "primaryBiome": "mixed", "seed": 12345}}'
  '{"processor": "SimpleAssetProcessor", "name": "Asset Generation", "description": "Texturen und Materialien", "phaseOrder": 2, "parameters": {"textureSize": 64}}'
  '{"processor": "SimpleContinentProcessor", "name": "Kontinente", "description": "Landmassen und Ozeane", "phaseOrder": 3, "parameters": {"worldSize": 500, "seed": 12345}}'
  '{"processor": "MountainTerrainProcessor", "name": "Bergterrein", "description": "Gebirge und Täler", "phaseOrder": 4, "parameters": {"worldSize": 500, "seed": 12345}}'
  '{"processor": "SimpleHistoryProcessor", "name": "Geschichte", "description": "Weltgeschichte", "phaseOrder": 5, "parameters": {"seed": 12345, "historyDepth": 5}}'
  '{"processor": "SimpleStructureProcessor", "name": "Strukturen", "description": "Siedlungen und Gebäude", "phaseOrder": 6, "parameters": {"worldSize": 500, "seed": 12345, "structureDensity": 3}}'
  '{"processor": "SimpleItemProcessor", "name": "Items", "description": "Gegenstände und Ausrüstung", "phaseOrder": 7, "parameters": {"seed": 12345, "itemDensity": 3, "worldType": "fantasy"}}'
  '{"processor": "SimpleQuestProcessor", "name": "Quests", "description": "Aufgaben und NPCs", "phaseOrder": 8, "parameters": {"seed": 12345, "questDensity": 3, "worldTheme": "fantasy"}}'
)

for phase in "${PHASES[@]}"; do
  echo "Füge Phase hinzu: $(echo $phase | grep -o '"name":"[^"]*' | cut -d'"' -f4)"
  curl -s -X POST "${GENERATOR_URL}/api/generator/${GENERATOR_ID}/phases" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
    -d "$phase" > /dev/null
done

echo "=== Starte Weltgenerierung ==="

# 3. Generierung starten
curl -X POST "${GENERATOR_URL}/api/generator/${GENERATOR_ID}/start" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"

echo "Weltgenerierung gestartet für Generator ID: $GENERATOR_ID"
echo "Status prüfen mit: curl -H 'Authorization: Bearer ${GENERATOR_TOKEN}' ${GENERATOR_URL}/api/generator/${GENERATOR_ID}"
```

# World Generator Simple Service Examples

Der World Generator Simple Service bietet spezialisierte Prozessoren für die Erstellung einfacher Welten mit grundlegenden Biomen.

### Simple World erstellen

Erstellt eine einfache Welt mit allen Simple-Phasen:

```bash
#!/bin/bash

export GENERATOR_TOKEN="generator-secret-key-2024"
export GENERATOR_URL="http://localhost:8083"

echo "1. Simple World Generator erstellen..."
SIMPLE_WORLD_ID=$(curl -s -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Simple Fantasy World",
    "description": "Eine einfache Welt mit Wald, Wüste, Ozean und Bergen",
    "parameters": {
      "type": "simple",
      "size": "medium",
      "biomes": ["forest", "desert", "ocean", "mountain", "swamp"]
    }
  }' | jq -r '.id')

echo "Simple World Generator ID: $SIMPLE_WORLD_ID"

echo "2. Simple Terrain-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleTerrainProcessor",
    "name": "Simple Terrain Generation",
    "description": "Grundlegende Kontinente mit Wald, Wüste, Ozean und Bergen",
    "phaseOrder": 1,
    "parameters": {
      "continentTypes": ["forest", "desert", "ocean", "mountain"],
      "simplicity": "high"
    }
  }' > /dev/null

echo "3. Simple Asset-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleAssetProcessor",
    "name": "Simple Asset Generation",
    "description": "Lädt alle Assets und erstellt Materialien",
    "phaseOrder": 2,
    "parameters": {
      "assetPath": "simple/assets/",
      "generateMaterials": true
    }
  }' > /dev/null

echo "4. Simple Biome-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleBiomeProcessor",
    "name": "Simple Biome Generation",
    "description": "Verteilt Biome auf den Kontinenten",
    "phaseOrder": 3,
    "parameters": {
      "biomeTransitions": true,
      "swampGeneration": true
    }
  }' > /dev/null

echo "5. Simple Structure-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleStructureProcessor",
    "name": "Simple Structure Generation",
    "description": "Erstellt Pfade, Wasserfälle und Flüsse",
    "phaseOrder": 4,
    "parameters": {
      "pathGeneration": true,
      "naturalFormations": true
    }
  }' > /dev/null

echo "6. Simple World Finalization-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleWorldProcessor",
    "name": "Simple World Finalization",
    "description": "Finale Validierung und Optimierung",
    "phaseOrder": 5,
    "parameters": {
      "validation": true,
      "optimization": true
    }
  }' > /dev/null

echo "7. Simple World Generierung starten..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}/start" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"

echo "Simple World Generierung gestartet!"
echo "Status prüfen: curl -X GET \"${GENERATOR_URL}/api/generator/${SIMPLE_WORLD_ID}\" -H \"Authorization: Bearer ${GENERATOR_TOKEN}\""
```

### Einzelne Simple Phasen testen

#### Simple Terrain Processor testen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Test Simple Terrain",
    "description": "Test für Simple Terrain Processor",
    "parameters": {"type": "simple", "testMode": true}
  }'

# Phase hinzufügen
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleTerrainProcessor",
    "name": "Test Terrain",
    "description": "Test der einfachen Terrain-Generierung",
    "phaseOrder": 1,
    "parameters": {"testMode": true}
  }'
```

#### Simple Asset Processor testen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/2/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleAssetProcessor",
    "name": "Test Assets",
    "description": "Test der Asset-Generierung",
    "phaseOrder": 1,
    "parameters": {
      "assetTypes": ["gras", "sand", "wasser", "felsen"],
      "testMode": true
    }
  }'
```

#### Simple Biome Processor testen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/3/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleBiomeProcessor",
    "name": "Test Biomes",
    "description": "Test der Biome-Generierung",
    "phaseOrder": 1,
    "parameters": {
      "biomes": ["forest", "desert"],
      "testMode": true
    }
  }'
```

#### Simple Structure Processor testen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/4/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleStructureProcessor",
    "name": "Test Structures",
    "description": "Test der Struktur-Generierung",
    "phaseOrder": 1,
    "parameters": {
      "structures": ["paths", "rivers"],
      "testMode": true
    }
  }'
```

#### Simple World Processor testen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/5/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "simpleWorldProcessor",
    "name": "Test World Finalization",
    "description": "Test der Welt-Finalisierung",
    "phaseOrder": 1,
    "parameters": {
      "validationOnly": true,
      "testMode": true
    }
  }'
```

### Simple World Status überprüfen

```bash
# Alle Simple World Generators abrufen
curl -X GET "${GENERATOR_URL}/api/generator" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  | jq '.[] | select(.parameters.type == "simple")'

# Simple World nach Name suchen
curl -X GET "${GENERATOR_URL}/api/generator/by-name/Simple%20Fantasy%20World" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"

# Phasen einer Simple World abrufen
curl -X GET "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  | jq '.[] | select(.processor | startswith("simple"))'
```

# Generator Beispiele

## Übersicht

Dieses Dokument zeigt Beispiele für die Verwendung des World Generators mit den verschiedenen Generator-Phasen.

## Vollständige Welt-Generierung

### Beispiel 1: Standard-Fantasywelt

```json
{
  "worldId": "fantasy-world-001",
  "name": "Eldoria",
  "description": "Eine mittelalterliche Fantasywelt mit Magie und mythischen Kreaturen",
  "parameters": {
    "worldSize": "large",
    "biomeVariety": "high",
    "magicLevel": "medium",
    "technologyLevel": "medieval"
  },
  "phases": [
    {
      "phaseType": "INITIALIZATION",
      "phaseOrder": 1,
      "parameters": {
        "seed": "12345",
        "worldDimensions": "2048x2048",
        "seaLevel": "64",
        "climateZones": "temperate,arctic,tropical"
      }
    },
    {
      "phaseType": "ASSET_MATERIAL_GENERATION",
      "phaseOrder": 2,
      "parameters": {
        "textureQuality": "high",
        "materialVariety": "fantasy",
        "customAssets": "enabled"
      }
    },
    {
      "phaseType": "CONTINENT_GENERATION",
      "phaseOrder": 3,
      "parameters": {
        "continentCount": "3",
        "oceanPercentage": "60",
        "islandDensity": "medium"
      }
    },
    {
      "phaseType": "TERRAIN_GENERATION",
      "phaseOrder": 4,
      "parameters": {
        "mountainHeight": "high",
        "riverDensity": "medium",
        "forestCoverage": "40",
        "desertPercentage": "15"
      }
    },
    {
      "phaseType": "HISTORICAL_GENERATION",
      "phaseOrder": 5,
      "parameters": {
        "civilizationCount": "5",
        "historicalDepth": "1000_years",
        "conflictLevel": "medium",
        "culturalDiversity": "high"
      }
    },
    {
      "phaseType": "STRUCTURE_GENERATION",
      "phaseOrder": 6,
      "parameters": {
        "cityCount": "12",
        "villageCount": "40",
        "ruinPercentage": "10",
        "dungeonDensity": "medium"
      }
    },
    {
      "phaseType": "ITEM_GENERATION",
      "phaseOrder": 7,
      "parameters": {
        "itemRarity": "balanced",
        "magicItemPercentage": "20",
        "uniqueArtifacts": "15"
      }
    },
    {
      "phaseType": "QUEST_GENERATION",
      "phaseOrder": 8,
      "parameters": {
        "questComplexity": "varied",
        "mainQuestlines": "3",
        "sideQuests": "50",
        "dynamicQuests": "enabled"
      }
    }
  ]
}
```

### Beispiel 2: Sci-Fi Weltraum-Station

```json
{
  "worldId": "space-station-alpha",
  "name": "Station Alpha-7",
  "description": "Eine große Weltraum-Station in einem fernen Sonnensystem",
  "parameters": {
    "worldType": "space_station",
    "technologyLevel": "advanced",
    "gravity": "artificial",
    "atmosphere": "controlled"
  },
  "phases": [
    {
      "phaseType": "INITIALIZATION",
      "phaseOrder": 1,
      "parameters": {
        "stationSize": "massive",
        "moduleCount": "200",
        "powergridCapacity": "unlimited"
      }
    },
    {
      "phaseType": "ASSET_MATERIAL_GENERATION",
      "phaseOrder": 2,
      "parameters": {
        "materialStyle": "sci_fi",
        "hologramSupport": "enabled",
        "neonLighting": "extensive"
      }
    },
    {
      "phaseType": "STRUCTURE_GENERATION",
      "phaseOrder": 3,
      "parameters": {
        "livingQuarters": "1000",
        "commercialAreas": "50",
        "industrialSections": "30",
        "recreationalAreas": "20"
      }
    },
    {
      "phaseType": "ITEM_GENERATION",
      "phaseOrder": 4,
      "parameters": {
        "techLevel": "futuristic",
        "weaponTypes": "energy_projectile",
        "toolVariety": "engineering_medical"
      }
    },
    {
      "phaseType": "QUEST_GENERATION",
      "phaseOrder": 5,
      "parameters": {
        "questThemes": "exploration,diplomacy,technical",
        "emergencyScenarios": "enabled",
        "tradeRoutes": "galactic"
      }
    }
  ]
}
```

### Beispiel 3: Benutzerdefinierte Phasen

```json
{
  "worldId": "custom-world",
  "name": "Angepasste Welt",
  "description": "Eine Welt mit benutzerdefinierten Generierungsphasen",
  "parameters": {
    "worldType": "custom",
    "complexity": "high"
  },
  "phases": [
    {
      "phaseType": "INITIALIZATION",
      "phaseOrder": 1,
      "parameters": {
        "customSeed": "abc123"
      }
    },
    {
      "phaseType": "CUSTOM_BIOME_GENERATION",
      "phaseOrder": 2,
      "parameters": {
        "biomeTypes": "crystal_caves,floating_islands,underwater_cities",
        "magicalElements": "enabled"
      }
    },
    {
      "phaseType": "WEATHER_SYSTEM_GENERATION",
      "phaseOrder": 3,
      "parameters": {
        "dynamicWeather": "enabled",
        "seasonalChanges": "enabled",
        "extremeEvents": "magical_storms,time_rifts"
      }
    },
    {
      "phaseType": "TERRAIN_GENERATION",
      "phaseOrder": 4,
      "parameters": {
        "terrainComplexity": "extreme",
        "verticalLayers": "5"
      }
    },
    {
      "phaseType": "CUSTOM_CIVILIZATION_GENERATION",
      "phaseOrder": 5,
      "parameters": {
        "civilizationTypes": "elemental_beings,ancient_robots,interdimensional_traders",
        "interactionLevel": "complex"
      }
    }
  ]
}
```

## Einzelne Phasen

### Phase: Terrain-Generierung

```json
{
  "worldId": "test-terrain",
  "name": "Terrain Test",
  "phases": [
    {
      "phaseType": "TERRAIN_GENERATION",
      "phaseOrder": 1,
      "parameters": {
        "heightmapResolution": "1024x1024",
        "noiseType": "perlin",
        "erosionSimulation": "enabled",
        "biomeBlending": "smooth",
        "riverGeneration": "automatic",
        "caveGeneration": "sparse"
      }
    }
  ]
}
```

### Phase: Struktur-Generierung

```json
{
  "worldId": "structure-test",
  "name": "Structure Test",
  "phases": [
    {
      "phaseType": "STRUCTURE_GENERATION",
      "phaseOrder": 1,
      "parameters": {
        "buildingStyles": "medieval,fantasy",
        "roadNetworks": "enabled",
        "bridgeGeneration": "automatic",
        "defensiveStructures": "walls,towers",
        "commercialBuildings": "markets,inns,shops",
        "residentialDensity": "medium"
      }
    }
  ]
}
```

## REST API Beispiele

### Einzelne Phase ausführen

```bash
curl -X POST http://localhost:8080/api/generator/worlds/example-world/phases \
  -H "Content-Type: application/json" \
  -H "X-Shared-Secret: your-secret" \
  -d '{
    "phaseType": "TERRAIN_GENERATION",
    "phaseOrder": 1,
    "parameters": {
      "mountainHeight": "medium",
      "riverDensity": "high"
    }
  }'
```

### Phase-Status abfragen

```bash
curl -X GET http://localhost:8080/api/generator/worlds/example-world/phases/TERRAIN_GENERATION \
  -H "X-Shared-Secret: your-secret"
```

## Verfügbare Standard-Phasentypen

Das System stellt folgende vordefinierte Phasentypen zur Verfügung:

- **INITIALIZATION** - Initialisierung
- **ASSET_MATERIAL_GENERATION** - Asset/Material-Generierung  
- **CONTINENT_GENERATION** - Kontinent-Generierung
- **TERRAIN_GENERATION** - Terrain-Generierung
- **HISTORICAL_GENERATION** - Historische Generierung
- **STRUCTURE_GENERATION** - Struktur-Generierung
- **ITEM_GENERATION** - Item-Generierung
- **QUEST_GENERATION** - Quest-Generierung

Zusätzlich können beliebige benutzerdefinierte Phasentypen verwendet werden, z.B.:
- WEATHER_SYSTEM_GENERATION
- CUSTOM_BIOME_GENERATION
- MAGIC_SYSTEM_GENERATION
- ECONOMIC_SYSTEM_GENERATION
