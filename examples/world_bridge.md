# World Bridge WebSocket Commands - Terrain Service Integration

Dieses Beispiel demonstriert die Verwendung der neuen WebSocket-Kommandos für die Integration mit dem World Terrain Service.

## Terrain Service Kommandos

Die folgenden Kommandos sind jetzt über WebSocket verfügbar und rufen die entsprechenden REST-Endpunkte des World Terrain Service auf:

### 1. Create World (kein Welt-Kontext erforderlich)

```json
{
  "service": "terrain",
  "command": "createWorld",
  "data": {
    "world": {
      "name": "Neue Welt",
      "description": "Eine beispielhafte Welt",
      "seed": 12345
    }
  },
  "requestId": "create-world-001"
}
```

**Antwort:**
```json
{
  "service": "terrain",
  "command": "createWorld",
  "data": {
    "id": "world-uuid-123",
    "name": "Neue Welt",
    "description": "Eine beispielhafte Welt",
    "seed": 12345,
    "createdAt": "2025-08-14T10:00:00Z"
  },
  "requestId": "create-world-001",
  "status": "success",
  "message": "World created successfully"
}
```

### 2. Get All Worlds (kein Welt-Kontext erforderlich)

```json
{
  "service": "terrain",
  "command": "getWorlds",
  "data": {},
  "requestId": "get-worlds-001"
}
```

**Antwort:**
```json
{
  "service": "terrain",
  "command": "getWorlds",
  "data": [
    {
      "id": "world-uuid-123",
      "name": "Neue Welt",
      "description": "Eine beispielhafte Welt",
      "seed": 12345,
      "createdAt": "2025-08-14T10:00:00Z"
    },
    {
      "id": "world-uuid-456",
      "name": "Andere Welt",
      "description": "Eine andere Welt",
      "seed": 67890,
      "createdAt": "2025-08-14T09:30:00Z"
    }
  ],
  "requestId": "get-worlds-001",
  "status": "success"
}
```

### 3. Get World by ID (Welt-Kontext erforderlich)

```json
{
  "service": "terrain",
  "command": "getWorld",
  "data": {
    "worldId": "world-uuid-123"
  },
  "requestId": "get-world-001"
}
```

**Antwort:**
```json
{
  "service": "terrain",
  "command": "getWorld",
  "data": {
    "id": "world-uuid-123",
    "name": "Neue Welt",
    "description": "Eine beispielhafte Welt",
    "seed": 12345,
    "createdAt": "2025-08-14T10:00:00Z"
  },
  "requestId": "get-world-001",
  "status": "success"
}
```

### 4. Update World (Welt-Kontext erforderlich)

```json
{
  "service": "terrain",
  "command": "updateWorld",
  "data": {
    "worldId": "world-uuid-123",
    "world": {
      "name": "Aktualisierte Welt",
      "description": "Eine aktualisierte Beschreibung",
      "seed": 12345
    }
  },
  "requestId": "update-world-001"
}
```

**Antwort:**
```json
{
  "service": "terrain",
  "command": "updateWorld",
  "data": {
    "id": "world-uuid-123",
    "name": "Aktualisierte Welt",
    "description": "Eine aktualisierte Beschreibung",
    "seed": 12345,
    "updatedAt": "2025-08-14T11:00:00Z"
  },
  "requestId": "update-world-001",
  "status": "success"
}
```

### 5. Delete World (Welt-Kontext erforderlich)

```json
{
  "service": "terrain",
  "command": "deleteWorld",
  "data": {
    "worldId": "world-uuid-123"
  },
  "requestId": "delete-world-001"
}
```

**Antwort:**
```json
{
  "service": "terrain",
  "command": "deleteWorld",
  "data": "World deleted successfully",
  "requestId": "delete-world-001",
  "status": "success"
}
```

## Vollständiger Workflow

1. **Anmeldung** (kein Welt-Kontext erforderlich):
```json
{
  "service": "bridge",
  "command": "login",
  "data": {
    "username": "testuser",
    "password": "testpass"
  },
  "requestId": "login-001"
}
```

2. **Welten auflisten** (kein Welt-Kontext erforderlich):
```json
{
  "service": "terrain",
  "command": "getWorlds",
  "data": {},
  "requestId": "get-worlds-001"
}
```

3. **Welt auswählen** (kein Welt-Kontext erforderlich):
```json
{
  "service": "bridge",
  "command": "use",
  "data": {
    "worldId": "world-uuid-123"
  },
  "requestId": "use-world-001"
}
```

4. **Welt-Details abrufen** (Welt-Kontext erforderlich):
```json
{
  "service": "terrain",
  "command": "getWorld",
  "data": {
    "worldId": "world-uuid-123"
  },
  "requestId": "get-world-details-001"
}
```

## Fehlerbehandlung

### Welt nicht gefunden
```json
{
  "service": "terrain",
  "command": "getWorld",
  "data": null,
  "requestId": "get-world-001",
  "status": "error",
  "errorCode": "not_found",
  "message": "World not found"
}
```

### Fehlende Parameter
```json
{
  "service": "terrain",
  "command": "createWorld",
  "data": null,
  "requestId": "create-world-001",
  "status": "error",
  "errorCode": "error",
  "message": "World data is required"
}
```

### Kein Welt-Kontext
```json
{
  "service": "terrain",
  "command": "getWorld",
  "data": null,
  "requestId": "get-world-001",
  "status": "error",
  "errorCode": "NO_WORLD_SELECTED",
  "message": "No world selected. Use the 'use' command to select a world first."
}
```
