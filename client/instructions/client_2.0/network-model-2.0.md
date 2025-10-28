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
{"i": "ping123","t": "p"}
```

Der Server antwortet mit einer Pong-Nachricht:

```json
{"r": "ping123","t": "p"}
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
  "e" : [    // Entity data (optional)
    EntityData,
    ...
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

## Entity Update (Server -> Client)

Der Server sendet Entity-Änderungen an den Client.

```json
{"t": "e.u", "d": 
        [
          EntityData,
          ...
      ]
}
```

## Block Update (Client -> Server)

Der Client sendet Block-Änderungen an den Server (z.B. durch den Editor).

```json
{"i":"12345", "t": "b.cu", "d": 
        [
          BlockData,
          ...
      ]
}
```

Als Antwort sendet der Server ein Block Update an alle Spieler, die den Chunk registriert haben.
(Auch der Client bekommt die Änderung zurück)

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
    "g": "123", // groupId des Blocks, optional
    "i": "123"  // id des Blocks, optional
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

## Server Notifications (Server -> Client)

Der Server sendet Benachrichtigungen an den Client (z.B. Systemmeldungen, Chat-Nachrichten, etc.).

```json
{"t": "n", "d":
  {
    "t": 0, // type: 0=system, 1=chat, 2=warning, 3=error, 4=info
    "f": "Server", // from: optional, z.B. bei chat Nachrichten
    "m": "Welcome to the server!", // message
    "ts": 1234567890 // UTC timestamp
  }
}
```

### NotificationType Enum
```
0 = SYSTEM
1 = CHAT
2 = WARNING
3 = ERROR
4 = INFO
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

## NPC Communication (Server <-> Client)

### Open NPC Dialog (Server -> Client)

Der Server sendet eine Anweisung zum Öffnen eines NPC-Dialogs an den Client.

```json
{"t": "npc.o", "d": 
  {
    "chatId": "5555",         // eine eindeutige ID für diese Chat-Sitzung
    "dialogId": "1277788238", // die ID genau dieses Dialogs
    "npcId": "npc123",
    "picturePath": "/assets/npc123.png", // optional
    "dialogData": {
      "title": "Greetings, traveler!",
      "text": "What would you like to know?",
      "options": [
        {"id": "1", "text": "Tell me about this place.", "severity": "info"},
        {"id": "2", "text": "Goodbye.", "severity": "neutral"}
      ]
    }
  }
}
```

### NPC Dialog Auswahl (Client -> Server)

Der Client sendet die Antwort/Selektion des Spielers im NPC-Dialog an den Server.

```json
{"t": "npc.se", "d": 
  {
    "npcId": "npc123",
    "dialogId": "1277788238",
    "chatId": "5555",
    "selectedOptionId": "1"
  }
}
```

## NPC Dialog Update (Server -> Client)

Der Server sendet eine Anweisung zum Ändern des NPC-Dialogs an den Client.

```json
{"t": "npc.u", "d": 
  {
    "chatId": "5555",
    "dialogId": "1277788238",
    "npcId": "npc123",
    "dialogData": {
      "title": "About this place",
      "text": "This is a land of adventure and mystery.",
      "options": [
        {"id": "1", "text": "Tell me more.", "severity": "info"},
        {"id": "2", "text": "Goodbye.", "severity": "neutral"}
      ]
    }
  }
}
```


### NPC Dialog Close (Server -> Client)

Der Server sendet eine Anweisung zum Schließen des NPC-Dialogs an den Client.

```json
{"t": "npc.c", "d": 
  {
    "chatId": "5555",
    "npcId": "npc123"
  }
}
```

### NPC Dialog Close (Client -> Server)

Der Client bestätigt das Schließen des NPC-Dialogs gegenüber dem Server.

```json
{"t": "npc.c", "d": 
  {
    "chatId": "5555",
    "npcId": "npc123"
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


