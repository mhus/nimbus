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
