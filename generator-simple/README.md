# Nimbus Simple World Generator

Der Simple World Generator ist ein Microservice der Nimbus-Plattform, der verschiedene Welttypen mit unterschiedlichen Terrain-Algorithmen generiert. Er integriert sich mit dem WorldVoxelClient aus dem common Modul, um generierte Voxel-Daten direkt über Kafka zu speichern.

## 🚀 Features

- **Verschiedene Welttypen**: FLAT, NORMAL, AMPLIFIED, DESERT, FOREST, MOUNTAINS, OCEAN
- **Seed-basierte Generierung**: Reproduzierbare Welten durch Seeds
- **Konfigurierbare Weltgröße**: Anpassbare Chunk-Dimensionen
- **Batch-Verarbeitung**: Effiziente Voxel-Speicherung in 1000er-Batches
- **REST API**: Einfache HTTP-Schnittstelle
- **Swagger UI**: Interaktive API-Dokumentation

## 🛠️ Setup

### Voraussetzungen
- Java 17+
- Maven 3.6+
- Kafka (für Voxel-Speicherung)

### Starten des Services
```bash
mvn spring-boot:run
```

Der Service läuft standardmäßig auf Port `8080`.

## 📚 API Dokumentation

### Base URL
```
http://localhost:7083/api/v1/generator
```

### Swagger UI
Interaktive API-Dokumentation verfügbar unter:
```
http://localhost:7083/swagger-ui.html
```

## 🌍 Weltgenerierung

### Endpoint: `POST /api/v1/generator/generate`

Generiert eine komplette Welt mit den angegebenen Parametern.

#### Request Body Parameter

| Parameter | Typ | Beschreibung | Standard | Erforderlich |
|-----------|-----|--------------|----------|--------------|
| `worldName` | String | Name der zu generierenden Welt | - | ✅ |
| `seed` | Long | Seed für reproduzierbare Generierung | Aktueller Timestamp | ❌ |
| `worldType` | Enum | Typ der Welt (siehe Welttypen) | `NORMAL` | ❌ |
| `worldSize` | Object | Größe der Welt in Chunks | `{width: 16, height: 16}` | ❌ |
| `generatorConfig` | Object | Zusätzliche Generator-Parameter | `{}` | ❌ |

#### Welttypen

| Typ | Beschreibung |
|-----|--------------|
| `FLAT` | Flache Welt mit gleichmäßigen Schichten |
| `NORMAL` | Standard-Terrain mit natürlichen Höhenvariationen |
| `AMPLIFIED` | Dramatische Höhenunterschiede und extreme Terrain |
| `DESERT` | Wüstenlandschaft mit Sand und Sandstein |
| `FOREST` | Waldlandschaft mit Bäumen und Vegetation |
| `MOUNTAINS` | Berglandschaft mit Schneekappen |
| `OCEAN` | Ozeanwelt mit Unterwasser-Terrain |

#### WorldSize Object
```json
{
  "width": 16,
  "height": 16
}
```

## 📝 curl Beispiele

### 1. Einfache Weltgenerierung
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "MeineWelt"
  }'
```

### 2. Flache Welt mit spezifischem Seed
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "FlacheWelt",
    "worldType": "FLAT",
    "seed": 12345
  }'
```

### 3. Große Bergwelt
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Bergwelt",
    "worldType": "MOUNTAINS",
    "worldSize": {
      "width": 32,
      "height": 32
    },
    "seed": 98765
  }'
```

### 4. Wüstenwelt mit Generator-Konfiguration
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Sahara",
    "worldType": "DESERT",
    "worldSize": {
      "width": 24,
      "height": 24
    },
    "seed": 555444,
    "generatorConfig": {
      "minHeight": 10,
      "maxHeight": 25
    }
  }'
```

### 5. Waldwelt mit Bäumen
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Schwarzwald",
    "worldType": "FOREST",
    "worldSize": {
      "width": 20,
      "height": 20
    },
    "seed": 777888
  }'
```

### 6. Ozeanwelt
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Pazifik",
    "worldType": "OCEAN",
    "worldSize": {
      "width": 40,
      "height": 40
    },
    "seed": 123456789
  }'
```

### 7. Amplified Welt für extreme Landschaften
```bash
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "ExtremeWelt",
    "worldType": "AMPLIFIED",
    "worldSize": {
      "width": 16,
      "height": 16
    },
    "seed": 2024
  }'
```

## 📊 Response Format

### Erfolgreiche Antwort (HTTP 200)
```json
{
  "worldName": "MeineWelt",
  "status": "COMPLETED",
  "worldType": "NORMAL",
  "worldSize": {
    "width": 16,
    "height": 16
  },
  "seed": 1720267200000,
  "generationStartTime": "2024-07-06T10:00:00",
  "generationEndTime": "2024-07-06T10:00:05",
  "durationMs": 5000,
  "chunksGenerated": 256,
  "voxelsGenerated": 65536,
  "stats": {
    "terrainVoxels": 45000,
    "airVoxels": 20536,
    "waterVoxels": 0,
    "structuresGenerated": 25
  },
  "messages": [
    "World generation completed successfully",
    "Generated 256 chunks with 65536 total voxels",
    "Saved 45000 terrain voxels to world-voxel module"
  ]
}
```

### Status-Werte
- `STARTED`: Generierung wurde gestartet
- `COMPLETED`: Generierung erfolgreich abgeschlossen
- `FAILED`: Generierung fehlgeschlagen

### Fehlerantworten

#### Bad Request (HTTP 400)
```bash
# Fehlendes worldName
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{}'
```

#### Internal Server Error (HTTP 500)
Wird zurückgegeben bei unerwarteten Fehlern während der Generierung.

## 🔧 Konfiguration

### Anpassbare Parameter
- **CHUNK_SIZE**: Größe eines Chunks (Standard: 16x16 Voxel)
- **WORLD_HEIGHT**: Maximale Welthöhe (Standard: 64 Blöcke)
- **BATCH_SIZE**: Anzahl Voxel pro Speicher-Batch (Standard: 1000)

### Umgebungsvariablen
```bash
# Kafka-Konfiguration für Voxel-Speicherung
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SPRING_KAFKA_PRODUCER_BOOTSTRAP_SERVERS=localhost:9092

# Logging-Level
LOGGING_LEVEL_DE_MHUS_NIMBUS=DEBUG
```

## 🎯 Anwendungsfälle

### 1. Testumgebung erstellen
```bash
# Kleine flache Welt für Tests
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "TestWelt",
    "worldType": "FLAT",
    "worldSize": {"width": 4, "height": 4}
  }'
```

### 2. Reproduzierbare Welten
```bash
# Gleicher Seed erzeugt identische Welten
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Welt1",
    "seed": 42,
    "worldType": "NORMAL"
  }'

curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "Welt2",
    "seed": 42,
    "worldType": "NORMAL"
  }'
```

### 3. Performance-Test mit großer Welt
```bash
# Große Welt für Performance-Tests
curl -X POST http://localhost:7083/api/v1/generator/generate \
  -H "Content-Type: application/json" \
  -d '{
    "worldName": "PerformanceTest",
    "worldType": "NORMAL",
    "worldSize": {"width": 64, "height": 64}
  }'
```

## 🐛 Troubleshooting

### Häufige Probleme

#### 1. Beans aus common Modul nicht gefunden
**Problem**: `WorldVoxelClient` kann nicht injiziert werden
**Lösung**: Component Scanning in `SimpleGeneratorApplication` konfiguriert für `de.mhus.nimbus.common`

#### 2. Kafka-Verbindungsfehler
**Problem**: Voxel-Daten können nicht gespeichert werden
**Lösung**: 
- Kafka-Server starten
- Bootstrap-Server in Konfiguration prüfen
- Netzwerk-Connectivity testen

#### 3. Memory-Probleme bei großen Welten
**Problem**: OutOfMemoryError bei sehr großen Welten
**Lösung**: 
- JVM Heap-Size erhöhen: `-Xmx4g`
- Kleinere Batch-Größen verwenden
- Welt in mehreren Schritten generieren

### Debug-Modus aktivieren
```bash
export LOGGING_LEVEL_DE_MHUS_NIMBUS_GENERATOR=DEBUG
mvn spring-boot:run
```

## 🚀 Integration

### Mit anderen Nimbus-Services
Der Generator integriert sich automatisch mit:
- **world-voxel**: Für Voxel-Persistierung über Kafka
- **registry**: Für Service-Discovery
- **identity**: Für Authentifizierung (optional)

### Health Check
```bash
curl http://localhost:7083/actuator/health
```

### Metrics
```bash
curl http://localhost:7083/actuator/metrics
```

## 📈 Performance

### Benchmark-Werte (Entwicklungsmaschine)
- **Flache Welt** (16x16): ~500ms
- **Normale Welt** (16x16): ~2-3s
- **Bergwelt** (32x32): ~15-20s
- **Große Welt** (64x64): ~60-90s

### Optimierungstipps
1. **Batch-Verarbeitung**: Automatisch aktiviert für bessere Performance
2. **Asynchrone Generierung**: Für große Welten empfohlen
3. **Caching**: Seeds und Konfigurationen für Wiederverwendung
4. **Monitoring**: Actuator-Endpoints für Performance-Überwachung

---

**Hinweis**: Diese API ist Teil der Nimbus-Plattform und erfordert eine korrekte Konfiguration der zugehörigen Services (Kafka, world-voxel) für vollständige Funktionalität.
