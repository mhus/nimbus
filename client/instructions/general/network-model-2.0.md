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

## Entity Interaction (Client -> Server)

Der Client sendet eine Interaktions information mit einer Entity an den Server (z.B. wenn der Spieler mit einer Entity interagiert).

```json
{"i":"12345", "t": "e.int.r", "d":
  {
    "entityId": "entity123",
    "ts": 1697045600000, // timestamp der interaktion
    "ac": "use", // action z.b. 'use', 'talk', 'attack', 'touch'
    "pa": { // params
      // optionale parameter fuer die interaktion
    }
  }
}
```

## Animation Execution (Server -> Client oder Client -> Server)

**Server → Client:** Server sendet Animation mit festen Positionen
**Client → Server:** Client sendet gefüllte Animation Template für Broadcast an alle Spieler

Der Server sendet eine Anweisung zum Starten einer Animation an den Client.

```json
{"t": "a.s", "d":
  [
    {
      "x": 10,      // World coordinate (reference position)
      "y": 64,
      "z": 10,
      "animation": AnimationData
    },
    ...
  ]
}
```

**Animation Flow (Client-definiert):**
1. Client hat Template mit Placeholders (z.B. "arrow_shot")
2. Player schießt Pfeil → Client füllt Positionen (shooter, target, impact)
3. Client spielt Animation lokal (sofortiges Feedback)
4. Client sendet gefüllte Animation an Server
5. Server validiert und broadcastet an alle anderen Spieler
6. Andere Clients empfangen und spielen Animation

**Animation Flow (Server-definiert):**
1. Server erstellt Animation mit festen Positionen (z.B. Tür öffnet)
2. Server sendet an alle betroffenen Clients
3. Clients spielen Animation

### AnimationData Structure

Siehe object-model-2.0.md für vollständige AnimationData Struktur mit:
- Timeline-basierte Effekte (parallel/sequential)
- Multi-Position Support (Placeholder oder Fixed)
- Effect Types (projectile, explosion, skyChange, etc.)
- Position References (fixed vs placeholder)

**Beispiel:** Pfeilschuss mit Explosion und Sky-Effekt
```json
{
  "name": "arrow_shot",
  "duration": 2000,
  "effects": [
    {
      "type": "projectile",
      "positions": [
        {"type": "fixed", "position": {"x": 0, "y": 65, "z": 0}},
        {"type": "fixed", "position": {"x": 10, "y": 65, "z": 10}}
      ],
      "params": {"speed": 50, "trajectory": "arc"},
      "startTime": 0,
      "duration": 1000,
      "blocking": true
    },
    {
      "type": "skyChange",
      "params": {"color": "#333", "lightIntensity": 0.3},
      "startTime": 0,
      "duration": 500
    },
    {
      "type": "explosion",
      "positions": [{"type": "fixed", "position": {"x": 10, "y": 65, "z": 10}}],
      "params": {"radius": 5},
      "startTime": 1000,
      "duration": 300
    }
  ]
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


