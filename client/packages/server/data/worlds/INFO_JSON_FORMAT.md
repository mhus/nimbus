# World info.json Format

Jeder World-Ordner in `data/worlds/` sollte eine `info.json` enthalten.

## Verzeichnis-Struktur

```
data/
└── worlds/
    ├── main/
    │   ├── info.json
    │   └── chunks/
    ├── test-world-1/
    │   ├── info.json
    │   └── chunks/
    └── your-world/
        ├── info.json
        └── chunks/
```

## info.json Format

```json
{
  "name": "World Name",
  "description": "World description",
  "chunkSize": 32,
  "dimensions": {
    "minX": -128,
    "maxX": 128,
    "minY": -64,
    "maxY": 192,
    "minZ": -128,
    "maxZ": 128
  },
  "seaLevel": 0,
  "groundLevel": 64,
  "status": 0
}
```

## Felder

### Erforderlich:
- **name**: Anzeigename der Welt
- **chunkSize**: Größe eines Chunks (16, 32, 64, etc.)

### Optional (mit Defaults):
- **description**: Beschreibung der Welt (Default: '')
- **dimensions**: Weltgrenzen
  - **minX, maxX**: X-Bereich (Default: -128 bis 128)
  - **minY, maxY**: Y-Bereich (Default: -64 bis 192)
  - **minZ, maxZ**: Z-Bereich (Default: -128 bis 128)
- **seaLevel**: Meeresspiegel-Höhe (Default: 0)
- **groundLevel**: Grundhöhe für Terrain-Generation (Default: 64)
- **status**: World-Status (Default: 0)

## Beispiele

### Kleine Test-Welt
```json
{
  "name": "Small Test World",
  "description": "For quick testing",
  "chunkSize": 16,
  "dimensions": {
    "minX": -64,
    "maxX": 64,
    "minY": -64,
    "maxY": 64,
    "minZ": -64,
    "maxZ": 64
  },
  "groundLevel": 10
}
```

### Große Haupt-Welt
```json
{
  "name": "Main World",
  "description": "Production world",
  "chunkSize": 32,
  "dimensions": {
    "minX": -512,
    "maxX": 512,
    "minY": -64,
    "maxY": 256,
    "minZ": -512,
    "maxZ": 512
  },
  "seaLevel": 64,
  "groundLevel": 64
}
```

## Hinweise

- Der **Ordnername** wird als `worldId` verwendet
- `info.json` wird beim Server-Start geladen
- Änderungen an `info.json` erfordern Server-Neustart
- Wenn `info.json` fehlt, wird die Welt **nicht** geladen
