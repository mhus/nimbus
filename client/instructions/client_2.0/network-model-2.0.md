
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

Chunks sind immer Colums mit X und Z Koordinate. Die Y-Richtung wird immer komplett geliefert und gerendert.

Der Client registriert die Chunks, die er vom Server empfangen möchte, basierend auf seiner Position und Sichtweite.

Die Registrierung wird als Liste von Chunk-Koordinaten (x,z) gesendet, alle nicht aufgeführten Chunks werden vom Server
nicht mehr gesendet.

```json
{"t": "c.r", "d": {"c": [{"x":0,"z":0},{"x":1,"z":0},{"x":0,"z":2}]}}
```

Für alle Chunks, die noch nicht registriert waren, sendet der Server automatisch die Chunk Daten.

## Chunk Anfrage (Client -> Server)

Der Client kann gezielt Chunks anfragen, z.B. wenn der Spieler sich bewegt oder die Sichtweite ändert.

```json
{"t": "c.q", "d": {"c": [{"x":0,"z":0},{"x":1,"z":0},{"x":0,"z":2}]}}
```

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
  "c": 0,
  "z": 0,
  "b":[
    BlockData,
    ...
  ],
  "h": [
    HeightDataIntegers,
    ...
  ], 
  "a": [
    AreaData
  ],
  "e" : [
    EntityData,
    ...
  ]
}
```

## HeightDataIntegers

```json
[maxHeight, minHeigth, groundLevel, waterHeight]
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

## Animation Execution (Server -> Client)

Der Server sendet eine Anweisung zum Starten einer Animation an den Client.

```json
{"t": "a.s", "d": 
  [
        {
          "x": 10,
          "y": 64,
          "z": 10,
          "animation": AnimationData
        },
    ...
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
    "t": "system", // type: system, chat, warning, error
    "f": "Server", // from: optional, z.B. bei chat Nachrichten
    "m": "Welcome to the server!", // message
    "ts": 1234567890 // UTC timestamp
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


