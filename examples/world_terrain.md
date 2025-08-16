# World Terrain Service API Examples

Diese Datei enthält Curl-Beispiele für alle World Terrain Service API-Endpunkte.

## World Management

### Welt erstellen

```bash
curl -X POST "http://localhost:7083/api/worlds" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "id": "earth-001",
    "name": "Earth",
    "description": "Main game world",
    "sizeX": 10000,
    "sizeY": 10000,
    "properties": {
      "gravity": "9.81",
      "atmosphere": "earth-like",
      "weather": "enabled"
    }
  }'
```

### Welt abrufen

```bash
curl -X GET "http://localhost:7083/api/worlds/earth-001" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Antwort:**
```json
{
  "id": "earth-001",
  "name": "Earth",
  "description": "Main game world",
  "sizeX": 10000,
  "sizeY": 10000,
  "properties": {
    "gravity": "9.81",
    "atmosphere": "earth-like",
    "weather": "enabled"
  },
  "createdAt": "2024-12-08T10:00:00Z",
  "updatedAt": "2024-12-08T10:00:00Z"
}
```

### Alle Welten auflisten

```bash
curl -X GET "http://localhost:7083/api/worlds" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Welt aktualisieren

```bash
curl -X PUT "http://localhost:7083/api/worlds/earth-001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "Earth Prime",
    "description": "Updated main game world",
    "sizeX": 15000,
    "sizeY": 12000,
    "properties": {
      "gravity": "9.81",
      "atmosphere": "earth-like",
      "weather": "enabled",
      "seasons": "true"
    }
  }'
```

### Welt löschen

```bash
curl -X DELETE "http://localhost:7083/api/worlds/earth-001" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Material Management

### Material erstellen

```bash
curl -X POST "http://localhost:7083/api/materials" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "grass",
    "blocking": false,
    "friction": 0.5,
    "color": "#00FF00",
    "texture": "grass.png",
    "soundWalk": "grass.wav",
    "properties": {
      "type": "natural",
      "flammable": "true",
      "hardness": "1"
    }
  }'
```

### Material abrufen

```bash
curl -X GET "http://localhost:7083/api/materials/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Materialien auflisten (mit Pagination)

```bash
curl -X GET "http://localhost:7083/api/materials?page=0&size=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Materialien mit Filter suchen

```bash
curl -X GET "http://localhost:7083/api/materials?name=grass&page=0&size=10" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Material aktualisieren

```bash
curl -X PUT "http://localhost:7083/api/materials/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "name": "grass",
    "blocking": false,
    "friction": 0.6,
    "color": "#00AA00",
    "texture": "grass_v2.png",
    "soundWalk": "grass_new.wav",
    "properties": {
      "type": "natural",
      "flammable": "true",
      "hardness": "1",
      "seasonal": "true"
    }
  }'
```

### Material löschen

```bash
curl -X DELETE "http://localhost:7083/api/materials/1" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Map Management

### Map/Terrain erstellen

```bash
curl -X POST "http://localhost:7083/api/maps" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "world": "earth-001",
    "clusters": [
      {
        "level": 0,
        "x": 0,
        "y": 0,
        "fields": [
          {
            "x": 1,
            "y": 1,
            "z": 0,
            "groups": [1, 2],
            "materials": [1, 2, 2, 2, 2, 3],
            "opacity": 255,
            "sizeZ": 1,
            "parameters": {
              "temperature": "20",
              "humidity": "60"
            }
          },
          {
            "x": 2,
            "y": 1,
            "z": 0,
            "groups": [1],
            "materials": [1, 2, 2, 2, 2, 3],
            "opacity": 255,
            "sizeZ": 1,
            "parameters": {
              "temperature": "21",
              "humidity": "65"
            }
          }
        ]
      }
    ]
  }'
```

### Map-Cluster abrufen

```bash
curl -X GET "http://localhost:7083/api/maps/0/0?world=earth-001&level=0" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### Mehrere Map-Cluster als Batch abrufen

```bash
curl -X POST "http://localhost:7083/api/maps/batch" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "world": "earth-001",
    "level": 0,
    "clusters": [
      {"x": 0, "y": 0},
      {"x": 1, "y": 0},
      {"x": 0, "y": 1},
      {"x": 1, "y": 1}
    ]
  }'
```

### Map aktualisieren

```bash
curl -X PUT "http://localhost:7083/api/maps" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "world": "earth-001",
    "clusters": [
      {
        "level": 0,
        "x": 0,
        "y": 0,
        "fields": [
          {
            "x": 1,
            "y": 1,
            "z": 0,
            "groups": [1, 2, 3],
            "materials": [4, 2, 2, 2, 2, 3],
            "opacity": 200,
            "sizeZ": 2,
            "parameters": {
              "temperature": "25",
              "humidity": "70",
              "updated": "true"
            }
          }
        ]
      }
    ]
  }'
```

### Map-Felder löschen

```bash
curl -X DELETE "http://localhost:7083/api/maps" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "world": "earth-001",
    "level": 0,
    "clusters": [
      {
        "x": 0,
        "y": 0,
        "fields": [
          {"x": 1, "y": 1},
          {"x": 2, "y": 1}
        ]
      }
    ]
  }'
```

### Komplettes Level löschen

```bash
curl -X DELETE "http://localhost:7083/api/maps/level?world=earth-001&level=0" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Antwort-Beispiele

### World-Antwort

```json
{
  "id": "earth-001",
  "createdAt": "2025-08-12T10:30:00Z",
  "updatedAt": "2025-08-12T11:00:00Z",
  "name": "Earth",
  "description": "Main game world",
  "properties": {
    "gravity": "9.81",
    "atmosphere": "earth-like",
    "weather": "enabled"
  }
}
```

### Material-Antwort

```json
{
  "id": 1,
  "name": "grass",
  "blocking": false,
  "friction": 0.5,
  "color": "#00FF00",
  "texture": "grass.png",
  "soundWalk": "grass.wav",
  "properties": {
    "type": "natural",
    "flammable": "true",
    "hardness": "1"
  }
}
```

### Map-Cluster-Antwort

```json
{
  "level": 0,
  "x": 0,
  "y": 0,
  "fields": [
    {
      "x": 1,
      "y": 1,
      "z": 0,
      "groups": [1, 2],
      "materials": [1, 2, 2, 2, 2, 3],
      "opacity": 255,
      "sizeZ": 1,
      "parameters": {
        "temperature": "20",
        "humidity": "60"
      }
    }
  ]
}
```

### Paginierte Material-Liste-Antwort

```json
{
  "content": [
    {
      "id": 1,
      "name": "grass",
      "blocking": false,
      "friction": 0.5,
      "color": "#00FF00",
      "texture": "grass.png",
      "soundWalk": "grass.wav",
      "properties": {
        "type": "natural"
      }
    }
  ],
  "pageable": {
    "sort": {
      "sorted": false,
      "unsorted": true,
      "empty": true
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": false,
    "unsorted": true,
    "empty": true
  },
  "empty": false
}
```

## Fehler-Beispiele

### 404 Not Found

```json
{
  "timestamp": "2025-08-12T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "World with id 'non-existent' not found",
  "path": "/api/worlds/non-existent"
}
```

### 400 Bad Request

```json
{
  "timestamp": "2025-08-12T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/materials"
}
```

### 401 Unauthorized

```json
{
  "timestamp": "2025-08-12T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Access token required",
  "path": "/api/worlds"
}
```

### 403 Forbidden

```json
{
  "timestamp": "2025-08-12T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient privileges. CREATOR role required",
  "path": "/api/worlds"
}
```
