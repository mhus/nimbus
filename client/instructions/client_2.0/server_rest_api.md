
# Rest API

## Authentication

Der Client schickt einen Authentication Header im Reqest mit, entweder Basic Auth oder Bearer Token.

## World daten

GET /api/worlds

Ruft eine Liste der verfügbaren Welten auf diesem Server ab.

Response:

```json
[
  {
    "worldId": "world123",
    "name": "My World",
    "description": "A cool virtual world",
    "owner": {
      "user": "ownerUserId",
      "displayName": "Owner Display Name",
      "email": ""
    },
    "createdAt": "2024-01-01T12:00:00Z",
    "updatedAt": "2024-06-01T12:00:00Z"
  },
  ...
]
```

GET /api/worlds/{worldId}

Ruft die Metadaten für eine bestimmte Welt ab.

Response:

```json
{
  "worldId": "world123",
  "name": "My World",
  "description": "A cool virtual world",
  "start": {"x": -1000, "y": -256, "z": -1000},
  "stop": {"x": 1000, "y": 256, "z": 1000},
  "chunkSize": 16,
  "assetPath": "/world123/assets",
  "assetPort": 3001,                  // optional
  "worldGroupId": "group123",
  "createdAt": "2024-01-01T12:00:00Z",
  "updatedAt": "2024-06-01T12:00:00Z",
  "owner": {
    "user": "ownerUserId",
    "displayName": "Owner Display Name",
    "email": ""
  },
  "settings": {
    "maxPlayers": 100,
    "allowGuests": true,
    "pvpEnabled": false,
    "pingInterval": 30
  }
}
```

## Assets

### Asset-Verwaltung

#### GET /api/worlds/{worldId}/assets

Ruft alle Assets ab oder sucht nach Assets.

Query Parameter:
- `query` (optional): Suchbegriff für die Suche in Asset-Pfaden und Kategorien

Response:

```json
{
  "assets": [
    {
      "path": "textures/block/basic/stone.png",
      "size": 2048,
      "mimeType": "image/png",
      "lastModified": "2024-01-01T12:00:00Z",
      "extension": ".png",
      "category": "textures"
    },
    ...
  ]
}
```

Beispiele:
- `GET /api/worlds/world123/assets` - Alle Assets
- `GET /api/worlds/world123/assets?query=stone` - Suche nach "stone"

#### POST /api/worlds/{worldId}/assets/{assetPath}

Erstellt ein neues Asset am angegebenen Pfad.

Request:
- URL: `/api/worlds/world123/assets/textures/block/custom/my_block.png`
- Body: Binärdaten (Raw Binary)
- Content-Type: Beliebig (z.B. `image/png`, `application/octet-stream`)

Response:

```json
{
  "path": "textures/block/custom/my_block.png",
  "size": 2048,
  "mimeType": "image/png",
  "lastModified": "2024-01-01T12:00:00Z",
  "extension": ".png",
  "category": "textures"
}
```

HTTP Status Codes:
- `201 Created` - Asset erfolgreich erstellt
- `400 Bad Request` - Ungültige Daten oder Asset existiert bereits
- `404 Not Found` - Welt nicht gefunden

#### PUT /api/worlds/{worldId}/assets/{assetPath}

Aktualisiert ein existierendes Asset.

Request:
- URL: `/api/worlds/world123/assets/textures/block/basic/stone.png`
- Body: Binärdaten (Raw Binary)
- Content-Type: Beliebig

Response:

```json
{
  "path": "textures/block/basic/stone.png",
  "size": 2048,
  "mimeType": "image/png",
  "lastModified": "2024-01-01T12:00:00Z",
  "extension": ".png",
  "category": "textures"
}
```

HTTP Status Codes:
- `200 OK` - Asset erfolgreich aktualisiert
- `404 Not Found` - Welt oder Asset nicht gefunden

#### DELETE /api/worlds/{worldId}/assets/{assetPath}

Löscht ein Asset.

Request:
- URL: `/api/worlds/world123/assets/textures/block/custom/my_block.png`

Response: Keine Daten (HTTP 204)

HTTP Status Codes:
- `204 No Content` - Asset erfolgreich gelöscht
- `404 Not Found` - Welt oder Asset nicht gefunden

### Asset-Download

#### GET /api/worlds/{worldId}/assets/{path}

Ruft eine Asset-Datei (z.B. Textur, Modell) für die angegebene Welt ab. Assets werden inklusive Dateierweiterung
angefragt, z.B. `/textures/block/stone.png`

Beispiel:
- `GET /api/worlds/world123/assets/textures/block/basic/stone.png`

Response:
- Binärdaten mit passendem Content-Type
- Cache-Control Header für Browser-Caching

## Block Types

### GET /api/worlds/{worldId}/blocktypes

Ruft alle BlockTypes ab oder sucht nach BlockTypes.

Query Parameter:
- `query` (optional): Suchbegriff für die Suche in BlockType-Beschreibungen und IDs

Response:

```json
{
  "blockTypes": [
    {
      "id": 100,
      "description": "Acacia fence",
      "modifiers": {
        "0": { ... }
      }
    },
    ...
  ]
}
```

Beispiele:
- `GET /api/worlds/world123/blocktypes` - Alle BlockTypes
- `GET /api/worlds/world123/blocktypes?query=fence` - Suche nach "fence"

### GET /api/worlds/{worldId}/blocktypes/{id}

Ruft einen einzelnen BlockType ab.

Response:

```json
{
  "id": 1,
  "name": "stone",
  "displayName": "Stone",
  "shape": "CUBE",
  "texture": "stone.png",
  "options": {
    "solid": true,
    "opaque": true,
    "transparent": false,
    "material": "solid"
  },
  "hardness": 1.5,
  "miningtime": 1500,
  "tool": "pickaxe",
  "unbreakable": false,
  "solid": true,
  "transparent": false,
  "windLeafiness": 0,
  "windStability": 1
}
```

Als BlockType Id kann entweder die numerische ID oder der eindeutige Name genutzt werden.

BlockType ist hier nicht vollständig dargestellt, siehe Objekt Modell Dokumentation für alle Felder.

### GET /api/worlds/{worldId}/blocktypes/{from}/{to}

Ruft einen Bereich von BlockTypes ab.

Response (Array):

```json
[
  {
    "id": 1,
    "name": "stone",
    ...
  },
  {
    "id": 2,
    "name": "dirt",
    ...
  }
]
```

Es können BlockType IDs in der Liste fehlen, wenn diese nicht definiert sind.

### POST /api/worlds/{worldId}/blocktypes

Erstellt einen neuen BlockType.

Request Body:

```json
{
  "id": 999,
  "description": "My custom block",
  "initialStatus": 0,
  "modifiers": {
    "0": {
      "visibility": {
        "shape": "CUBE"
      },
      "physical": {
        "solid": true
      }
    }
  }
}
```

Falls `id` nicht angegeben wird, wird automatisch die nächste verfügbare ID vergeben.

Response:

```json
{
  "id": 999
}
```

HTTP Status Codes:
- `201 Created` - BlockType erfolgreich erstellt
- `400 Bad Request` - Ungültige Daten oder BlockType existiert bereits
- `404 Not Found` - Welt nicht gefunden

### PUT /api/worlds/{worldId}/blocktypes/{blockTypeId}

Aktualisiert einen existierenden BlockType.

Request Body:

```json
{
  "description": "Updated description",
  "modifiers": {
    "0": {
      "visibility": {
        "shape": "CUBE"
      }
    }
  }
}
```

Die `id` im Body wird ignoriert und durch die ID in der URL ersetzt.

Response:

```json
{
  "id": 999,
  "description": "Updated description",
  "modifiers": { ... }
}
```

HTTP Status Codes:
- `200 OK` - BlockType erfolgreich aktualisiert
- `400 Bad Request` - Ungültige Daten
- `404 Not Found` - Welt oder BlockType nicht gefunden

### DELETE /api/worlds/{worldId}/blocktypes/{blockTypeId}

Löscht einen BlockType.

Response: Keine Daten (HTTP 204)

HTTP Status Codes:
- `204 No Content` - BlockType erfolgreich gelöscht
- `404 Not Found` - Welt oder BlockType nicht gefunden

## Block Operations

### GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}

Ruft einen Block an der angegebenen Position ab.

URL Parameter:
- `worldId`: World ID
- `x`, `y`, `z`: Block-Koordinaten (world coordinates)

Response:

```json
{
  "position": {
    "x": 10,
    "y": 64,
    "z": 5
  },
  "blockTypeId": 1,
  "status": 0,
  "metadata": {
    "displayName": "Custom Stone",
    "groups": ["building-group"]
  }
}
```

HTTP Status Codes:
- `200 OK` - Block gefunden
- `404 Not Found` - Welt nicht gefunden oder Block existiert nicht an Position
- `400 Bad Request` - Ungültige Koordinaten

### POST /api/worlds/{worldId}/blocks/{x}/{y}/{z}

Erstellt einen neuen Block an der angegebenen Position.

URL Parameter:
- `worldId`: World ID
- `x`, `y`, `z`: Block-Koordinaten (world coordinates)

Request Body:

```json
{
  "blockTypeId": 1,
  "status": 0,
  "metadata": {
    "displayName": "My Custom Block",
    "groups": ["group-uuid"]
  }
}
```

Felder:
- `blockTypeId` (required): ID des BlockType
- `status` (optional): Block-Status (default: 0)
- `metadata` (optional): Zusätzliche Metadaten (default: {})

Response:

```json
{
  "position": {
    "x": 10,
    "y": 64,
    "z": 5
  },
  "blockTypeId": 1,
  "status": 0,
  "metadata": {
    "displayName": "My Custom Block",
    "groups": ["group-uuid"]
  }
}
```

HTTP Status Codes:
- `201 Created` - Block erfolgreich erstellt
- `400 Bad Request` - Ungültige Daten, Block existiert bereits, oder ungültige blockTypeId
- `404 Not Found` - Welt nicht gefunden

### PUT /api/worlds/{worldId}/blocks/{x}/{y}/{z}

Aktualisiert einen existierenden Block an der angegebenen Position.

URL Parameter:
- `worldId`: World ID
- `x`, `y`, `z`: Block-Koordinaten (world coordinates)

Request Body:

```json
{
  "blockTypeId": 2,
  "status": 1,
  "metadata": {
    "displayName": "Updated Block",
    "groups": ["new-group"]
  }
}
```

Felder:
- `blockTypeId` (required): ID des BlockType
- `status` (optional): Block-Status (behält alten Wert, wenn nicht angegeben)
- `metadata` (optional): Zusätzliche Metadaten (behält alte Metadaten, wenn nicht angegeben)

Response:

```json
{
  "position": {
    "x": 10,
    "y": 64,
    "z": 5
  },
  "blockTypeId": 2,
  "status": 1,
  "metadata": {
    "displayName": "Updated Block",
    "groups": ["new-group"]
  }
}
```

HTTP Status Codes:
- `200 OK` - Block erfolgreich aktualisiert
- `400 Bad Request` - Ungültige Daten oder ungültige blockTypeId
- `404 Not Found` - Welt oder Block nicht gefunden

### DELETE /api/worlds/{worldId}/blocks/{x}/{y}/{z}

Löscht einen Block an der angegebenen Position.

URL Parameter:
- `worldId`: World ID
- `x`, `y`, `z`: Block-Koordinaten (world coordinates)

Response: Keine Daten (HTTP 204)

HTTP Status Codes:
- `204 No Content` - Block erfolgreich gelöscht
- `404 Not Found` - Welt oder Block nicht gefunden
- `400 Bad Request` - Ungültige Koordinaten

## Block Metadaten

Gibt nur die Metadaten eines Blocks zurück, die nicht in der Block Definition enthalten sind.
Das Metadatum 'groupId' im Block ist eine visuelle Gruppierung um mehrere Blocks gleichzeitig zu gruppieren
und um Aktionen auszuführen.

GET /api/worlds/{worldId}/blocks/{x}/{y}/{z}/metadata

Response:

```json
{
  "x": 1,
  "y": 2,
  "z": 3,
  "id": "uuid",
  "groups": ["uuid1"],
  "groupNames": ["name1"],
  "inheritedGroups": ["uuid2"],
  "inheritedGroupNames": ["name2"],
  "displayName": "Custom Block Name"
}
```
