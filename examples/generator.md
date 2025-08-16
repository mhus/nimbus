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
      "size": "large",
      "biome": "mixed",
      "difficulty": "medium",
      "magic": true
    }
  }'
```

## 2. Phase zu World Generator hinzufügen

Fügt eine Generierungsphase zu einem bestehenden World Generator hinzu.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "terrainProcessor",
    "name": "Terrain Generation",
    "description": "Grundlegende Landschaftsgenerierung mit Bergen und Tälern",
    "phaseOrder": 1,
    "parameters": {
      "heightVariation": "high",
      "waterLevel": 64,
      "mountainDensity": 0.3
    }
  }'
```

## 3. Struktur-Phase hinzufügen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "structureProcessor",
    "name": "Structure Generation",
    "description": "Platzierung von Dörfern, Städten und Dungeons",
    "phaseOrder": 2,
    "parameters": {
      "villageDensity": 0.1,
      "dungeonCount": 5,
      "castleCount": 2
    }
  }'
```

## 4. Item-Phase hinzufügen

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "itemProcessor",
    "name": "Item Generation",
    "description": "Verteilung von Schätzen und Ressourcen",
    "phaseOrder": 3,
    "parameters": {
      "treasureRarity": "medium",
      "resourceDensity": 0.7,
      "magicItems": true
    }
  }'
```

## 5. Generierung starten

Startet die Generierung für einen World Generator. Alle Phasen werden in der definierten Reihenfolge ausgeführt.

```bash
curl -X POST "${GENERATOR_URL}/api/generator/1/start" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 6. Alle World Generators abrufen

```bash
curl -X GET "${GENERATOR_URL}/api/generator" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 7. Spezifischen World Generator abrufen

```bash
curl -X GET "${GENERATOR_URL}/api/generator/1" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 8. World Generator nach Name abrufen

```bash
curl -X GET "${GENERATOR_URL}/api/generator/by-name/Fantasy%20World" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 9. World Generators nach Status abrufen

```bash
curl -X GET "${GENERATOR_URL}/api/generator/status/COMPLETED" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

Verfügbare Status:
- `INITIALIZED` - Neu erstellt, noch nicht gestartet
- `GENERATING` - Generierung läuft
- `COMPLETED` - Generierung abgeschlossen
- `ERROR` - Fehler während der Generierung

## 10. Phasen eines World Generators abrufen

```bash
curl -X GET "${GENERATOR_URL}/api/generator/1/phases" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 11. Aktive Phasen abrufen

Zeigt nur nicht archivierte Phasen an.

```bash
curl -X GET "${GENERATOR_URL}/api/generator/1/phases/active" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 12. Phase archivieren

Archiviert eine Phase (setzt archived=true).

```bash
curl -X POST "${GENERATOR_URL}/api/generator/phases/1/archive" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## 13. World Generator löschen

```bash
curl -X DELETE "${GENERATOR_URL}/api/generator/1" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"
```

## Vollständiges Beispiel: Komplette Welt erstellen

```bash
#!/bin/bash

export GENERATOR_TOKEN="generator-secret-key-2024"
export GENERATOR_URL="http://localhost:8083"

echo "1. World Generator erstellen..."
WORLD_ID=$(curl -s -X POST "${GENERATOR_URL}/api/generator/create" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "name": "Complete Fantasy World",
    "description": "Eine vollständige Fantasy-Welt mit allen Features",
    "parameters": {
      "size": "huge",
      "biome": "varied",
      "difficulty": "hard"
    }
  }' | jq -r '.id')

echo "World Generator ID: $WORLD_ID"

echo "2. Terrain-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "terrainProcessor",
    "name": "Terrain Generation",
    "description": "Grundlegende Landschaftsgenerierung",
    "phaseOrder": 1,
    "parameters": {"heightVariation": "extreme"}
  }' > /dev/null

echo "3. Struktur-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "structureProcessor",
    "name": "Structure Generation",
    "description": "Städte und Dungeons platzieren",
    "phaseOrder": 2,
    "parameters": {"complexity": "high"}
  }' > /dev/null

echo "4. Item-Phase hinzufügen..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${WORLD_ID}/phases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}" \
  -d '{
    "processor": "itemProcessor",
    "name": "Item Generation",
    "description": "Schätze und Ressourcen verteilen",
    "phaseOrder": 3,
    "parameters": {"rarity": "legendary"}
  }' > /dev/null

echo "5. Generierung starten..."
curl -s -X POST "${GENERATOR_URL}/api/generator/${WORLD_ID}/start" \
  -H "Authorization: Bearer ${GENERATOR_TOKEN}"

echo "Generierung gestartet! Status prüfen mit:"
echo "curl -X GET \"${GENERATOR_URL}/api/generator/${WORLD_ID}\" -H \"Authorization: Bearer ${GENERATOR_TOKEN}\""
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
