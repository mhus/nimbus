# Generator Simple Module

Ein einfacher Weltgenerator für die Nimbus-Plattform, der REST-APIs zur Generierung kompletter Welten bereitstellt.

## Features

- **Verschiedene Welttypen**: FLAT, NORMAL, AMPLIFIED, DESERT, FOREST, MOUNTAINS, OCEAN
- **Konfigurierbare Weltgrößen**: Anpassbare Chunk-Größen und Weltdimensionen
- **Synchrone und asynchrone Generierung**: Sowohl blockierende als auch nicht-blockierende API-Aufrufe
- **Detaillierte Statistiken**: Vollständige Berichte über generierte Voxel und Strukturen
- **OpenAPI-Dokumentation**: Swagger UI für einfache API-Exploration

## API-Endpoints

### Hauptendpunkte

- `POST /api/v1/generator/generate` - Vollständige Weltgenerierung mit konfigurierbaren Parametern
- `POST /api/v1/generator/generate/async` - Asynchrone Weltgenerierung
- `POST /api/v1/generator/generate/simple/{worldName}` - Einfache Flat-World-Generierung
- `POST /api/v1/generator/generate/quick` - Schnelle Generierung über URL-Parameter

### Hilfsmethoden

- `GET /api/v1/generator/types` - Verfügbare Welttypen abrufen
- `GET /api/v1/generator/health` - Service-Status prüfen

## Beispiel-Aufrufe

### Einfache Weltgenerierung
```bash
curl -X POST "http://localhost:8083/api/v1/generator/generate/simple/MeineWelt"
```

### Konfigurierte Weltgenerierung
```json
POST /api/v1/generator/generate
{
  "worldName": "TestWelt",
  "worldType": "FOREST",
  "seed": 12345,
  "worldSize": {
    "width": 32,
    "height": 32
  }
}
```

### Schnelle Generierung
```bash
curl -X POST "http://localhost:8083/api/v1/generator/generate/quick?worldName=SchnelleWelt&worldType=MOUNTAINS&width=16&height=16"
```

## Konfiguration

Die Konfiguration erfolgt über `application.properties`:

```properties
# Weltgenerator-Konfiguration
world.generator.default-chunk-size=16
world.generator.default-world-height=64
world.generator.max-world-size=100
world.generator.enable-async=true
```

## Server-Port

Der Service läuft standardmäßig auf Port **8083**.

## OpenAPI-Dokumentation

Nach dem Start des Services ist die API-Dokumentation verfügbar unter:
- Swagger UI: http://localhost:8083/swagger-ui.html
- API Docs: http://localhost:8083/api-docs

## Welttypen

- **FLAT**: Flache Welt mit gleichmäßiger Oberfläche
- **NORMAL**: Normale Welt mit variierender Topographie
- **AMPLIFIED**: Verstärkte Landschaft mit extremen Höhenunterschieden
- **DESERT**: Wüstenlandschaft mit Sand und Sandstein
- **FOREST**: Waldlandschaft mit Bäumen und Vegetation
- **MOUNTAINS**: Bergige Landschaft mit hohen Erhebungen
- **OCEAN**: Ozeanwelt mit Wasser und Meeresböden

## Verwendung

1. Service starten: `mvn spring-boot:run`
2. API-Aufruf mit gewünschtem Weltnamen
3. Response enthält detaillierte Informationen über die generierte Welt
