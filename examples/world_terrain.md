# World Terrain Service API Examples

## Material Endpoints

### Erstelle ein neues Material
```bash
curl -X POST http://localhost:8083/materials \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "name": "grass",
    "blocking": false,
    "friction": 0.5,
    "color": "#00FF00",
    "texture": "grass.png",
    "soundWalk": "grass.wav",
    "properties": {
      "growth": "fast",
      "season": "all"
    }
  }'
```

### Hole ein Material nach ID
```bash
curl -X GET http://localhost:8083/materials/1 \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Liste alle Materialien auf
```bash
curl -X GET "http://localhost:8083/materials?page=0&size=20" \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Aktualisiere ein Material
```bash
curl -X PUT http://localhost:8083/materials/1 \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "name": "grass_updated",
    "blocking": false,
    "friction": 0.6,
    "color": "#00AA00",
    "texture": "grass_v2.png",
    "soundWalk": "grass_new.wav",
    "properties": {
      "growth": "medium",
      "season": "spring_summer"
    }
  }'
```

### Lösche ein Material
```bash
curl -X DELETE http://localhost:8083/materials/1 \
  -H "X-World-Auth: world-terrain-secret-key"
```

## Map Endpoints

### Erstelle eine neue Map
```bash
curl -v -X POST http://localhost:8083/maps \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "clusters": [
      {
        "level": 0,
        "x": 0,
        "y": 0,
        "fields": [
          {
            "x": 0,
            "y": 0,
            "z": 0,
            "groups": [1, 2],
            "materials": [1, 2, 2, 2, 2, 3],
            "opacity": 255,
            "sizeZ": 1,
            "parameters": {
              "elevation": "0.5",
              "moisture": "0.7"
            }
          },
          {
            "x": 1,
            "y": 0,
            "z": 0,
            "groups": [1],
            "materials": [2, 3, 3, 3, 3, 4],
            "opacity": 255,
            "sizeZ": 1,
            "parameters": {
              "elevation": "0.3",
              "moisture": "0.4"
            }
          }
        ]
      }
    ]
  }'
```

### Hole Map-Daten für einen Cluster
```bash
curl -X GET "http://localhost:8083/maps/0/0?world=earth-001&level=0" \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Hole Map-Daten für mehrere Cluster (Batch)
```bash
curl -X POST http://localhost:8083/maps/batch \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "level": 0,
    "clusters": [
      {"x": 0, "y": 0},
      {"x": 1, "y": 0},
      {"x": 0, "y": 1}
    ]
  }'
```

### Aktualisiere eine Map
```bash
curl -X PUT http://localhost:8083/maps \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "clusters": [
      {
        "level": 0,
        "x": 0,
        "y": 0,
        "fields": [
          {
            "x": 0,
            "y": 0,
            "z": 0,
            "groups": [1, 2, 3],
            "materials": [2, 2, 2, 2, 2, 3],
            "opacity": 200,
            "sizeZ": 2,
            "parameters": {
              "elevation": "0.8",
              "moisture": "0.9"
            }
          }
        ]
      }
    ]
  }'
```

### Lösche ein Map-Level
```bash
curl -X DELETE "http://localhost:8083/maps/level?world=earth-001&level=0" \
  -H "X-World-Auth: world-terrain-secret-key"
```

## Sprite Endpoints

### Erstelle neue Sprites
```bash
curl -X POST http://localhost:8083/sprites \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "level": 0,
    "sprites": [
      {
        "dynamic": false,
        "x": 10,
        "y": 15,
        "z": 0,
        "sizeX": 2,
        "sizeY": 2,
        "sizeZ": 3,
        "groups": [5, 6],
        "parameters": {
          "age": "mature",
          "health": "100"
        },
        "rasterType": "png",
        "raster": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==",
        "type": "tree",
        "blocking": true,
        "opacity": 255
      }
    ]
  }'
```

### Hole ein Sprite nach ID
```bash
curl -X GET http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Liste Sprites in einem Cluster auf
```bash
curl -X GET http://localhost:8083/sprites/earth-001/0/0/0 \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Aktualisiere ein Sprite
```bash
curl -X PUT http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "x": 10,
    "y": 15,
    "z": 0,
    "sizeX": 3,
    "sizeY": 3,
    "sizeZ": 4,
    "groups": [5, 6, 7],
    "parameters": {
      "age": "old",
      "health": "80"
    },
    "rasterType": "png",
    "raster": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==",
    "type": "tree",
    "blocking": true,
    "opacity": 200,
    "enabled": true
  }'
```

### Lösche ein Sprite
```bash
curl -X DELETE http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Aktualisiere Sprite-Koordinaten
```bash
curl -X PUT http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc/coordinates \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "x": 20,
    "y": 25,
    "z": 1
  }'
```

### Aktiviere ein Sprite
```bash
curl -X POST http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc/enable \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Deaktiviere ein Sprite
```bash
curl -X POST http://localhost:8083/sprites/S12345678-1234-5678-9abc-123456789abc/disable \
  -H "X-World-Auth: world-terrain-secret-key"
```

## Asset Endpoints

### Erstelle ein neues Asset
```bash
curl -X POST http://localhost:8083/assets \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "name": "tree_oak.png",
    "type": "image",
    "data": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==",
    "properties": {
      "width": "64",
      "height": "128",
      "format": "PNG",
      "animated": "false"
    }
  }'
```

### Hole ein Asset
```bash
curl -X GET http://localhost:8083/assets/earth-001/tree_oak.png \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Liste Assets einer Welt auf
```bash
curl -X GET "http://localhost:8083/assets/earth-001?page=0&size=20" \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Hole mehrere Assets (Batch)
```bash
curl -X POST http://localhost:8083/assets/batch \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "assets": [
      "tree_oak.png",
      "grass_texture.jpg",
      "water_sound.wav"
    ]
  }'
```

### Aktualisiere ein Asset
```bash
curl -X PUT http://localhost:8083/assets/earth-001/tree_oak.png \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "name": "tree_oak.png",
    "type": "image",
    "data": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==",
    "properties": {
      "width": "128",
      "height": "256",
      "format": "PNG",
      "animated": "false",
      "version": "2.0"
    }
  }'
```

### Lösche ein Asset
```bash
curl -X DELETE http://localhost:8083/assets/earth-001/tree_oak.png \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Komprimiere Assets
```bash
curl -X POST http://localhost:8083/assets/compress \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '"earth-001"'
```

## Terrain Group Endpoints

### Erstelle eine neue Terrain-Gruppe
```bash
curl -X POST http://localhost:8083/groups \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "world": "earth-001",
    "name": "forest_area",
    "type": "field",
    "properties": {
      "biome": "temperate_forest",
      "temperature": "moderate",
      "humidity": "high"
    }
  }'
```

### Hole eine Terrain-Gruppe
```bash
curl -X GET http://localhost:8083/groups/earth-001/1 \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Liste Terrain-Gruppen einer Welt auf
```bash
curl -X GET http://localhost:8083/groups/earth-001 \
  -H "X-World-Auth: world-terrain-secret-key"
```

### Aktualisiere eine Terrain-Gruppe
```bash
curl -X PUT http://localhost:8083/groups/earth-001/1 \
  -H "Content-Type: application/json" \
  -H "X-World-Auth: world-terrain-secret-key" \
  -d '{
    "name": "forest_area_updated",
    "type": "field",
    "properties": {
      "biome": "dense_forest",
      "temperature": "cool",
      "humidity": "very_high",
      "wildlife": "abundant"
    }
  }'
```

### Lösche eine Terrain-Gruppe
```bash
curl -X DELETE http://localhost:8083/groups/earth-001/1 \
  -H "X-World-Auth: world-terrain-secret-key"
```
