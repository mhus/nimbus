
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

GET /api/worlds/{worldId}/assets/{path}

Ruft eine Asset-Datei (z.B. Textur, Modell) für die angegebene Welt ab. Assets werden inclusive dateierweiterung 
angefragt, z.B. /textures/block/stone.png

## Block Types

GET /api/worlds/{worldId}/blocktypes/{id}

GET /api/worlds/{worldId}/blocktypes/{from}/{to}

Gibt eine oder ein Array von BlockType Definitionen zurück.

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

Oder als Array:

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

Als BlockType Id in der EInzelabfrage kann entweder die numerische ID oder der eindeutige Name genutzt werden.

BlockType ist hier nicht vollständig dargestellt, siehe Objekt Modell Dokumentation für alle Felder.

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
