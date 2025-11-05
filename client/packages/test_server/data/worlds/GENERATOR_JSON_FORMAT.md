# Generator Configuration (generator.json)

Jeder World-Ordner kann eine `generator.json` enthalten, die den Terrain-Generator konfiguriert.

## Verzeichnis-Struktur

```
data/worlds/
└── your-world/
    ├── info.json
    ├── generator.json  ← Generator-Konfiguration
    └── chunks/
```

## Generator-Typen

### 1. Flat Generator
Generiert eine flache Ebene mit konfigurierbaren Schichten.

```json
{
  "type": "flat",
  "seed": 12345,
  "parameters": {
    "groundLevel": 64,
    "layers": [
      { "blockType": "bedrock", "thickness": 1 },
      { "blockType": "stone", "thickness": 50 },
      { "blockType": "dirt", "thickness": 3 },
      { "blockType": "grass", "thickness": 1 }
    ]
  }
}
```

**Parameter:**
- `groundLevel`: Höhe der Oberfläche (Default: 64)
- `layers`: Array von Schichten (von unten nach oben)
  - `blockType`: Name des BlockTypes (z.B. "stone", "grass", "dirt")
  - `thickness`: Dicke der Schicht in Blöcken

### 2. Normal Generator
Generiert hügeliges Terrain mit Simplex Noise.

```json
{
  "type": "normal",
  "seed": 12345,
  "parameters": {
    "waterLevel": 62,
    "baseHeight": 64,
    "heightVariation": 32
  }
}
```

**Parameter:**
- `waterLevel`: Wasserspiegel-Höhe (Default: 62)
- `baseHeight`: Basis-Höhe des Terrains (Default: 64)
- `heightVariation`: Maximale Abweichung von baseHeight (Default: 32)

**Terrain-Schichten (automatisch):**
- Y=0: Bedrock
- Unten: Stone
- 3 Blöcke unter Oberfläche: Dirt
- Oberfläche über Wasser: Grass
- Oberfläche unter Wasser: Sand
- Wasseroberfläche: Water

## Felder

### Erforderlich:
- **type**: Generator-Typ (`"flat"` oder `"normal"`)

### Optional:
- **seed**: Zufallsseed für Terrain-Generation (Default: aktuelle Zeit)
- **parameters**: Generator-spezifische Parameter (siehe oben)

## Beispiele

### Einfache flache Testwelt
```json
{
  "type": "flat",
  "seed": 1000,
  "parameters": {
    "groundLevel": 10,
    "layers": [
      { "blockType": "stone", "thickness": 10 }
    ]
  }
}
```

### Bergige Landschaft
```json
{
  "type": "normal",
  "seed": 42,
  "parameters": {
    "waterLevel": 64,
    "baseHeight": 80,
    "heightVariation": 64
  }
}
```

### Superflache Welt (nur Gras)
```json
{
  "type": "flat",
  "parameters": {
    "groundLevel": 1,
    "layers": [
      { "blockType": "grass", "thickness": 1 }
    ]
  }
}
```

## Hinweise

- Wenn `generator.json` fehlt, wird **Normal Generator** mit Default-Werten verwendet
- Änderungen an `generator.json` erfordern Server-Neustart
- Bereits generierte Chunks werden **nicht** neu generiert
- Seed bestimmt das Terrain-Muster (gleicher Seed = gleiches Terrain)
- BlockType-Namen müssen im BlockType-Registry existieren

## Block-Namen (häufig verwendet)

Verfügbare Block-Namen für `layers`:
- `bedrock` - Grundgestein
- `stone` - Stein
- `dirt` - Erde
- `grass` - Gras
- `sand` - Sand
- `water` - Wasser
- `lava` - Lava

Die vollständige Liste finden Sie in `files/blocktypes/manifest.json`.
