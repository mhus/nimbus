# Network Model 2.0

## Koordinaten-Namenskonvention

**Wichtig:** Koordinaten müssen konsistent benannt werden:
- **x, y, z** = Welt-Koordinaten (world coordinates)
- **cx, cy, cz** = Chunk-Koordinaten (chunk coordinates)
- **localX, localY, localZ** = Lokale Koordinaten innerhalb eines Chunks (vermeiden wenn möglich)

## Login

Als erstes schickt der Client eine Login Nachricht mit den Zugangsdaten (username, password, token, etc.) worldId und ggf.
einer bestehenden sessionId.

```json
{"i": "1","t": "login", "d": 
  {
    "username": "user",
    "password": "pass", 
    "worldId": "world123",
    "clientType": "web", // xbox, mobile
    "sessionId": "existingSessionId123"
  }
}
```
oder
```json
{"i": "1","t": "login", "d": 
  {
    "token": "abcdefg12345",
    "worldId": "world123",
    "clientType": "web", // xbox, mobile
    "sessionId": "existingSessionId123"
  }
}
```

Der Server antwortet mit einer Login-Antwort-Nachricht:

```json
{"r": "1","t": "loginResponse", "d": 
  {
    "success": true, 
    "userId": "user123",
    "displayName": "User",
    "worldInfo": {
      "worldId": "world123",
      "name": "My World",
      "description": "A cool virtual world",
      "start": {"x": -1000, "y": -256, "z": -1000},
      "stop": {"x": 1000, "y": 256, "z": 1000},
      "chunkSize": 16,
      "assetPath": "/world123/assets",
      "assetPort": 3001,            // optional
      "worldGroupId": "group123",
      "status": 0,                  // current world status
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
      },
      "license": {
        "type": "creative",
        "expiresAt": "2025-01-01T12:00:00Z"
      },
      "startArea": {
        "x": 0,
        "y": 0,
        "z": 0,
        "radius": 50,
        "rotation": 0
      }
    },
    "sessionId": "newSessionId67890"}
}
```

oder bei Fehler:

```json
{"r": "1","t": "loginResponse", "d": 
  {
    "success": false, 
    "errorCode": 401,
    "errorMessage": "Invalid credentials"
  }
}
```

## Ping

Der Client sendet regelmäßig Ping-Nachrichten, um die Verbindung aufrechtzuerhalten und die Latenz zu messen.

```json
{"i": "ping123","t": "p", "d": {
  "cTs": 1697045600000 // client timestamp
}}
```

Der Server antwortet mit einer Pong-Nachricht:

```json
{"r": "ping123","t": "p", "d": {
  "cTs": 1697045600000, // client timestamp - wird zurueck geschickt
  "sTs": 1697045600500 // server timestamp - wird angehaengt
}}
```

Wird langer als `pingInterval` (+ 10 Sekunden Puffer) Sekunden kein Ping empfangen, wird die Verbindung getrennt.

> deadline = lastPingAt + pingInterval*1000 + 10000.

## Update world status (Server -> Client)

Der Server sendet einen neuen status der welt an den Client, damit werden alle chunks neu gerendert - wenn sich der status geändert hat (!)

```json
{"t": "w.su", "d": 
          {
            "s": 1 // new status value
          }
}
```

## Chunk Registration (Client -> Server)

Chunks sind immer Colums mit X und Z Koordinate (cx, cz). Die Y-Richtung wird immer komplett geliefert und gerendert.

Der Client registriert die Chunks, die er vom Server empfangen möchte, basierend auf seiner Position und Sichtweite.

Die Registrierung wird als Liste von Chunk-Koordinaten (cx, cz) gesendet, alle nicht aufgeführten Chunks werden vom Server
nicht mehr gesendet.

```json
{"t": "c.r", "d": {"c": [{"x":0,"z":0},{"x":1,"z":0},{"x":0,"z":2}]}}
```

**Hinweis:** Im JSON bleiben die kurzen Namen `x`, `z` für Netzwerk-Optimierung. Im Code verwenden wir `cx`, `cz`.

Für alle Chunks, die noch nicht registriert waren, sendet der Server automatisch die Chunk Daten.

## Chunk Anfrage (Client -> Server)

Der Client kann gezielt Chunks anfragen, z.B. wenn der Spieler sich bewegt oder die Sichtweite ändert.

```json
{"t": "c.q", "d": {"c": [{"x":0,"z":0},{"x":1,"z":0},{"x":0,"z":2}]}}
```

**Hinweis:** Im JSON: `x`, `z` (kurz). Im Code: `cx`, `cz` (eindeutig).

## Chunk Update (Server -> Client)

Der Server sendet angefragte Chunks an den Client.

```json
{ "t": "c.u", "d": 
  [
      chunkDataTransferObject,
      ...
  ]
}
```

## chunkDataTransferObject

```json
{
  "cx": 0,   // Chunk X coordinate (benannt cx im Code für Eindeutigkeit)
  "cz": 0,   // Chunk Z coordinate (benannt cz im Code für Eindeutigkeit)
  "b":[      // Block array (nur non-air blocks)
    BlockData,
    ...
  ],
  "h": [     // Height data array
    HeightDataIntegers,
    ...
  ],
  "a": [     // Area data (optional)
    AreaData
  ],
  "i": [
    Item
  ]
}
```

**Hinweis:** Im JSON werden die Chunk-Koordinaten als `cx` und `cz` übertragen (geändert von `c` und `z`).

## HeightDataIntegers

Named tuple mit 4 Werten:

```json
[maxHeight, minHeight, groundLevel, waterHeight]
```

Im TypeScript Code als `readonly` tuple definiert:
```typescript
type HeightData = readonly [
  maxHeight: number,
  minHeight: number,
  groundLevel: number,
  waterHeight: number
];
```

## Block Update (Server -> Client)

Der Server sendet Block-Änderungen an den Client.

```json
{"t": "b.u", "d": 
        [
          BlockData,
          ...
      ]
}
```

## Item Block Update (Server -> Client)

Der Server sendet Item-Block-Änderungen an den Client. Items sind spezielle Billboard-Blöcke, die separat verwaltet werden.

**Regeln:**
- Items mit `blockTypeId: 0` (AIR) werden entfernt, aber nur wenn ein Item an der Position existiert
- Items mit `blockTypeId: 1` werden hinzugefügt/aktualisiert, aber nur wenn die Position AIR ist oder bereits ein Item enthält
- Items können keine regulären Blöcke überschreiben
- Jedes Item hat `metadata.id` (unique identifier) und optional `metadata.displayName`

```json
{"t": "b.iu", "d":
        [
          Item,  // BlockData mit metadata.id und metadata.displayName
          ...
      ]
}
```

**Beispiel:**
```json
{
  "t": "b.iu",
  "d": [
    {
      "position": {"x": 10, "y": 64, "z": 5},
      ...
    }
  ]
}
```

## Block Status Update (Server -> Client)

Der Server sendet Block-Status-Änderungen an den Client (z.B. für Animationen, Effekte, etc.).

```json
{"t": "b.s.u", "d": 
        [
          {
            "x": 10,
            "y": 64,
            "z": 10,
            "s": 1 // new status value
            "aa": [  // Optionale Animationen vor dem Statuswechsel
              AnimationData,
              ...
            ],
            "ab": [  // Optionale Animationen nach dem Statuswechsel
              AnimationData,
              ...
            ]
          }
      ]
}
```

## Block Interaction (Client -> Server)

Der Client sendet eine Interaktions information mit einem Block an den Server (z.B. wenn der Spieler mit einem Block interagiert).

**Aktionen:**
- `'click'` - Spieler klickt auf Block (INTERACTIVE Modus, physics.interactive === true)
- `'collision'` - Spieler kollidiert mit Block (physics.collisionEvent === true)
- `'climb'` - Spieler klettert über Block (physics.collisionEvent === true)
- `'fireShortcut'` - Spieler drückt Zahlentaste 1-9,0 mit Block selektiert

```json
{"i":"12345", "t": "b.int", "d":
  {
    "x": 10,
    "y": 64,
    "z": 10,
    "id": "123", // aus block.metadata.id, optional
    "gId": "123", // groupId des Blocks, optional
    "ac": "click", // action: 'click', 'collision', 'climb', 'fireShortcut'
    "pa": { // params
      "clickType": "left" // nur bei 'click'
    }
  }
}
```

**Beispiel Click:**
```json
{
  "i": "98765",
  "t": "b.int",
  "d": {
    "x": 10,
    "y": 64,
    "z": 10,
    "id": "item_sword_123",
    "gId": "weapons",
    "ac": "click",
    "pa": {
      "clickType": "left"
    }
  }
}
```

**Beispiel Shortcut:**
```json
{
  "i": "11111",
  "t": "b.int",
  "d": {
    "x": 10,
    "y": 64,
    "z": 10,
    "id": "door_123",
    "gId": "buildings",
    "ac": "fireShortcut",
    "pa": {
      "shortcutNr": 1,
      "playerPosition": {"x": 8.5, "y": 64.0, "z": 9.2},
      "playerRotation": {"yaw": 1.57, "pitch": 0.0},
      "targetPosition": {"x": 10.5, "y": 64.5, "z": 10.5},
      "distance": 2.83
    }
  }
}
```


## Entity Chunk Pathway (Server -> Client)

Der Server sendet Entity-Passway Daten aufgrund der aktuell registrierten Chunks an den Client.

```json
{"t": "e.p", "d": 
  [
    EntityPathwayData,
    ...
  ]
}
```

## Entity Position Update (Client -> Server)

Der Client sendet seine aktuelle Entity-Positions und Rotation an den Server.

```json
{
  "t": "e.p.u",
  "d": [
    {
      "pl": "player", // local entity id nicht die unique id, nur lokal fuer den client, kann auch eine weitere simulierte entity sein
      "p?": {
        "x": 100.5,
        "y": 65.0,
        "z": -200.5
      }, // position
      "r?": {
        "y": 90.0,
        "p": 0.0
      }, // rotation: yaw, pitch
      "v?": {
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      }, // velocity
      "po?": 5, // pose id
      "ts": 1697045600000, // server timestamp
      "ta?": { // Vector4
        "x": 100.5,
        "y": 65.0,
        "z": -200.5,
        "ts": 1697045800000 // target arrival timestamp
      } // interpoliertes target position, der client berechnet schon fuer den server, wohin sich der player in den naechsten 200 ms bewegt, nimmt last aus dem server, update ist alle 100 ms
    }
  ]
}
```

## Entity Status Update (Server -> Client)

Update des Entity-Status (z.B. Gesundheit etc.) vom Server an den Client.

```json
{"t": "e.s.u", "d": 
  [
    {
      "entityId": "entity123",
      "status": {
        "healthMax": 100,
        "health": 80,
        ...
      }
    },
    ...
  ]
}
```

- Es gibt das flag death: 1 im status object, wenn die entity gestorben ist.
- die Staus keys sind frei definierbar, je nach entity type und welt Mechanik.

## Entity Interaction (Client -> Server)

Der Client sendet eine Interaktions information mit einer Entity an den Server (z.B. wenn der Spieler mit einer Entity interagiert).

Wird im SelectMode.INTERACTIVE gesendet, wenn der Spieler auf eine interaktive Entity klickt (entity.interactive === true).

**Aktionen:**
- `'click'` - Spieler klickt auf Entity (left, right, middle)
- `'fireShortcut'` - Spieler drückt Zahlentaste 1-9,0 mit Entity selektiert
- `'use'` - Spieler verwendet Entity (z.B. NPC ansprechen)
- `'talk'` - Spieler spricht mit Entity
- `'attack'` - Spieler greift Entity an
- `'touch'` - Automatische Kollision/Proximity
- `'entityCollision'` - Spieler kollidiert mit Entity
- `'entityProximity'` - Spieler betritt Entity Attention-Range

```json
{"i":"12345", "t": "e.int.r", "d":
  {
    "entityId": "entity123",
    "ts": 1697045600000, // timestamp der interaktion
    "ac": "click", // action z.b. 'click', 'fireShortcut', 'use', 'talk', 'attack', 'touch'
    "pa": { // params
      "clickType": "left" // 'left', 'right', 'middle' (nur bei ac: 'click')
    }
  }
}
```

**Beispiel Click-Interaktion:**
```json
{
  "i": "45678",
  "t": "e.int.r",
  "d": {
    "entityId": "npc_farmer_001",
    "ts": 1697045600000,
    "ac": "click",
    "pa": {
      "clickType": "right"
    }
  }
}
```

**Beispiel Shortcut-Interaktion:**
```json
{
  "i": "22222",
  "t": "e.int.r",
  "d": {
    "entityId": "npc_farmer_001",
    "ts": 1697045600000,
    "ac": "fireShortcut",
    "pa": {
      "shortcutNr": 2,
      "playerPosition": {"x": 8.5, "y": 64.0, "z": 9.2},
      "playerRotation": {"yaw": 1.57, "pitch": 0.0},
      "targetPosition": {"x": 10.0, "y": 64.5, "z": 12.0},
      "distance": 4.12,
      "entityId": "npc_farmer_001"
    }
  }
}
```

## User Movement Update (Client -> Server)

Der Client sendet seine aktuelle Position und Rotation an den Server.

```json
{"t": "u.m", "d": 
  {
    "p": {"x": 100.5, "y": 65.0, "z": -200.5}, // optional, position
    "r": {"y": 90.0, "p": 0.0}  // optional, rotation: yaw, pitch
  }
}
```

## Interaction Request (Client -> Server)

Der Client sendet eine Interaktionsanfrage an den Server (z.B. wenn der Spieler mit einem Block interagiert).

```json
{"i":"12346", "t": "int.r", "d":
  {
    "x": 10,
    "y": 64,
    "z": 10,
    "g": "123" // groupId des Blocks, optional
  }
}
```

Fail Response:

```json
{"r":"12346", "t": "int.rs", "d":
  {
    "success": false,
    "errorCode": 403,
    "errorMessage": "You do not have permission to interact with this block."
  }
}
```

## Player Teleport (Server -> Client)

Der Server sendet eine Teleport-Anweisung an den Client.

```json
{"t": "p.t", "d":
  {
    "p": {"x": 0.0, "y": 64.0, "z": 0.0}, // position
    "r": {"y": 0.0, "p": 0.0}  // rotation: yaw, pitch
  }
}
```

## Client Command (Client -> Server)

Der Client sendet einen Client-Befehl an den Server.
Als Antwort kann er "cmd.msg" schicken, abschließend schickt er mindestens eine
"cmd.rs" message als successful (rc=0) oder failed (rc!=0).

Negative rc sind System Fehler:

* -1 = Command not found
* -2 = Command not allowed (permission denied)
* -3 = Invalid arguments
* -4 = Internal error

Positive rc werden vom Command zurückgegeben und sind individuell.

* 0 = OK bzw. "true".
* 1 = Error bzw. "false".

```json
{"i": "123", "t": "cmd", "d": 
  {
    "cmd": "say",
    "args": [
      "Hello, world!"
    ]
  }
}
```

Response Streaming Message (Server -> Client):

```json
{"r":"123", "t": "cmd.msg", "d": {
    "message": "Processing..."
  }
}
```

Response Finished Successfully (Server -> Client):

```json
{"r":"123", "t": "cmd.rs", "d": 
  {
    "rc": 0,
    "message": "Hello, world!"
  }
}
```

Response Finished Failed (Server -> Client):

```json
{"r":"123", "t": "cmd.rs", "d": 
  {
    "rc": 1,
    "message": "Command not found."
  }
}
```

Wird der Parameter 'oneway' auf true gesetzt, erwartet der Client keine Antwort.

```json
{"t": "cmd", "d": 
  {
    "cmd": "say",
    "oneway": true,
    "args": [
      "Hello, world!"
    ]
  }
}
```

### Server Command (Server -> Client)

Der Server sendet einen Server-Befehl an den Client.
Als Antwort bekommt er eine "cmd.rs" message als successful (rc=0) oder failed (rc!=0).

Negative rc sind System Fehler:

* -1 = Command not found
* -3 = Invalid arguments
* -4 = Internal error

Positive rc werden vom Command zurückgegeben und sind individuell.

* 0 = OK bzw. "true".
* 1 = Error bzw. "false".

```json
{"i": "123", "t": "scmd", "d": 
  {
    "cmd": "say",
    "args": [
      "Hello, world!"
    ]
  }
}
```

Response Finished Successfully (Client -> Server):

```json
{"r":"123", "t": "scmd.rs", "d": 
  {
    "rc": 0,
    "message": "Hello, world!"
  }
}
```

Response Finished Failed (Client -> Server):

```json
{"r":"123", "t": "scmd.rs", "d": 
  {
    "rc": 1,
    "message": "Command not found."
  }
}
```

Wird der Parameter 'oneway' auf true gesetzt, erwartet der Client keine Antwort.

```json
{"t": "cmd", "d": 
  {
    "cmd": "say",
    "oneway": true,
    "args": [
      "Hello, world!"
    ]
  }
}
```

## Multiple Commands (Server -> Client)

Der Server sendet mehrere Server-Befehle in einer Nachricht an den Client.
Die Commands werden als `cmds` Array im `d` Objekt gesendet.
Alle Commands sind oneway (keine Antwort erwartet).

**Execution Mode:**
- `parallel: false` (default) - Commands werden seriell (nacheinander) ausgeführt
- `parallel: true` - Commands werden parallel (gleichzeitig) ausgeführt

```json
{"t": "scmd", "d": {
  "cmds": [
    {
      "cmd": "say",
      "args": ["Hello!"]
    },
    {
      "cmd": "setWeather",
      "args": ["rain"]
    }
  ],
  "parallel": false
}}
```

**Parallel Execution Example:**
```json
{"t": "scmd", "d": {
  "cmds": [
    {"cmd": "flashImage", "args": ["effects/flash.png", "1000", "0.5"]},
    {"cmd": "centerText", "args": ["Boss Spawned!"]},
    {"cmd": "fog", "args": ["0.3"]}
  ],
  "parallel": true
}}
```


## Logout (Client -> Server)

Der Client sendet eine Logout-Nachricht an den Server, wenn der Spieler die Welt verlässt.
Die Session wird nicht mehr vorgehalten.

```json
{"t": "logout", "d": {}}
```

## Abkürzungen

### Objekte

* c: chunk
* b: block
* a: Animation
* e: Effekt
* s: Status
* i: id
* rq: request
* rs: response
* t: type
* ts: Timestamp

### Verben

* c: close
* o: open
* u: update
* q: query
* r: register

## Script Effeckt Trigger (Client -> Server)

Der Client sendet eine Nachricht an den Server, wenn ein Spieler einen Effeckt-Trigger auslöst 
(z.B. durch Betreten eines Bereichs).

```json
{"i":"12347", "t": "s.t", "d":
  {
    "entityId": "@player_1234", 
    "effectId": "effect_1231j2829281928",
    "chunks": [{1,4},{2,4}], 
    "effect" : {
      ...      
    } // ScriptActionDefinition
  }
}
```

EffectTriggerData:
- entityId: string (optional) - quelle entity die den effekt ausloest
- effectId: string - unique id des effekts
- chunks: ChunkPosition[] (optional) - liste von chunks die betroffen sind (wird aus source und targets ermittelt)
- effect: ScriptActionDefinition // including source und target, targets

## Script Effeckt Trigger (Server -> Client)

Der Server sendet Nachrichten an den Client weiter, wenn ein Spieler einen Effeckt-Trigger auslöst
(z.B. durch Betreten eines Bereichs).

```json
{"i":"12347", "t": "s.t", "d":
  {
    "entityId": "@player_1234",
    "effectId": "effect_1231j2829281928",
    "chunks": [{1,4},{2,4}],
    "effect" : {
      ...
    } // ScriptActionDefinition
  }
}
```

## Script Effect Update (Client -> Server)

Der Client sendet eine Nachricht an den Server, um den Status eines laufenden Effekts zu aktualisieren
(variable hat sich geandert).

```json
{"i":"12348", "t": "s.u", "d":
  {
    "effectId": "effect_1231j2829281928",
    "chunks": [{1,4},{2,4}],
    "variables": {
      "intensity": 0.8,
      "duration": 5000,
      ...
    }
  }
}
```

## Script Effect Update (Server -> Client)

Der Server sendet eine Nachricht an alle Clients, um den Status eines laufenden Effekts zu aktualisieren
(variable hat sich geandert).

```json
{"i":"12348", "t": "s.u", "d":
  {
    "effectId": "effect_1231j2829281928",
    "chunks": [{1,4},{2,4}],
    "variables": {
      "intensity": 0.8,
      "duration": 5000,
      ...
    }
  }
}
```

## Team Data (Server -> Client)

Der Server schickt Team-Daten an den Client, es wird immer das komplette Team Set geschickt.
Es handelt sich um die Metadaten des Teams. Damit wirt das Team im Client definiert, aktionen wie
added, removed fallen dadurch weg. Wird ein Leer-Team geschickt, ist das Team aufgelöst.

Daten sind nicht optional.

```json
{"t": "t.d", "d":
  {
    "name": "red", // team name
    "id": "12345", // team id
    "members": [
      {
        "player": "user123",
        "name": "PlayerOne",
        "icon": "/assets/avatars/playerone.png"
      },
      ...
    ]
  }
}
```

## Team Status (Server -> Client)

Der Server sendet einen Status Update für die Team-Miglieder an den Client. Es werden
nur relevante Daten versendet und die Team-Metadaten damit angereichert. Diese Nachricht
wird sehr oft versendet, daher nur die minimalen Daten.

```json
{"t": "t.s", "d":
  {
    "id": "red",
    "ms": [ // member status
      {
        "id": "user123",  // player id
        "h": 80, // health prozent, optional
        "po": {"x":100,"y":64,"z":200}, // position optional
        "st": 1 // status code, optional 0 -disconnected (at start), 1 - alive, 2 - dead
      }
    ]
  }
}
```
