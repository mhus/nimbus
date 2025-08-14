# World Bridge Specifikation

## Einleitung

Der World Bridge Service stellt WebSocket-Server bereit und ermöglicht es WebSocket-Clients
sich mit der Welt zu verbinden. Es können Kommandos im JSON-Format eingegeben werden, um
mit der Welt zu interagieren. Daten werden in JSON-Format übertragen, es werden die bekannten
DTOs aus dem modul 'shared' verwendet.

Kafka-Topics werden konsumiert und die Events (gefiltert) werden an die WebSocket-Clients gesendet. Auch
hier werden die bekannten DTOs aus dem modul 'shared' verwendet.

## WebSocket-Server

Der WebSocket-Server ist in der Lage, mehrere Verbindungen gleichzeitig zu verwalten und
stellt sicher, dass Nachrichten in Echtzeit übertragen werden.

Eine WebSocket kann einen Status haben solange sie verbunden ist.

- **world**: Die WebSocket ist mit der Welt verbunden und kann Nachrichten senden und empfangen.
- **user**: Die WebSocket ist mit einem Benutzer angemeldet.
- **roles**: Die WebSocket hat Rollen, dievom user kommen.

Ist kein user hinterlegt, sind alle anderen Kommandos ausser `login` nicht erlaubt.
Ist keine welt hinterlegt, sind alle anderen Kommandos ausser `use` nicht erlaubt.

## Kommando Basis

Kommandos werden im JSON-Format an den WebSocket-Server gesendet. Die Struktur der Kommandos
ist immer gleich. Jedes Kommando hat ein `command`-Feld, das den Namen des Kommandos angibt,
und ein `data`-Feld, das die Daten als JSON-Objekt enthält. Ausserdem wird eine `requestId` übergeben, die
zur Identifikation der Antwort verwendet wird.

Es wird immer ein `service`-Feld angegeben, das den Namen des Services angibt, 
an den das Kommando gesendet wird.

Die Struktur eines Kommandos sieht wie folgt aus:

```json
{
  "service": "string", // Name des Services, z.B. "bridge", "terrain", "item", "life"
  "command": "string", // Name des Kommandos, z.B. "login", "use", "registerCluster"
  "data": {
    // JSON-Objekt mit den Daten
  },
  "requestId": "string"
}
```

Die Antworten des WebSocket-Servers haben ebenfalls eine ähnliche Struktur.

```json
{
  "service": "string", // Name des Services, z.B. "bridge", "terrain", "item", "life"
  "command": "string", // Name des Kommandos, z.B. "login", "use", "registerCluster"
  "data": {
    // JSON-Objekt mit den Daten
  },
  "requestId": "string",
  "status": "success" | "error", // Status der Antwort
  "errorCode": "string", // Optional: Fehlercode, falls ein Fehler aufgetreten ist
  "message": "string" // Optional: Nachricht zur Beschreibung des Status
}
```

### Kommando-Implementierung

Jedes Kommando soll als separate Bean/Service Klasse implementiert
werden. Ein Interface `WebSocketCommand` wird definiert, 
das die Methode `execute` und `info` enthält.

Die `info`-Methode gibt das Objekt `WebSocketCommandInfo` zurück, das die Informationen
über das Kommando enthält, wie den Service-Namen, Namen des Kommandos und die Beschreibung.

Die `execute`-Methode nimmt als Parameter eine Klasse `ExecuteRequest` entgegen, 
die die erforderlichen Parameter für das Kommando enthält. Diese Methode führt die Logik des Kommandos 
aus und gibt eine `ExecuteResponse` zurück.

In der `WorldBridgeService` Klasse werden die Kommandos als Liste von `WebSocketCommand`-Instanzen
Autowired und in der PostConstruct-Methode in einer Map gespeichert, um sie später
schnell zu finden.

Die `executeCommand`-Methode von `WorldBridgeService` nimmt 
ein Kommando entgegen, sucht das entsprechende Kommando in der Liste und führt es aus.

## Login

Ein WebSocket-Client kann sich mit dem Befehl `login` anmelden. Dabei wird ein Token übergeben, das
vom Authentifizierungsdienst generiert wurde. Der Token wird validiert und der Benutzer wird
angemeldet. Der WebSocket-Client erhält eine Bestätigung, dass er angemeldet ist
und die Rollen des Benutzers werden an den WebSocket-Client gesendet.

Der login kann auch mit username und password erfolgen, wenn kein Token vorhanden ist. Dann
fragt der WebSocket-Server den Authentifizierungsdienst an, ob die Anmeldedaten korrekt sind
und benutzt den zurückgegebenen Token für die Anmeldung. Der Identity Service wird
mittels der IdentityServiceUtils.login() Methode aufgerufen.

Der Befehl `login` hat folgende Struktur:

```json
{
  "service": "bridge",
  "command": "login",
    "data": {
        "token": "string" // Token des Benutzers
        // Optional: Wenn kein Token vorhanden ist, können auch username und password übergeben werden
        "username": "string", // Benutzername des Benutzers
        "password": "string" // Passwort des Benutzers
    },
    "requestId": "string"
}
```

## Use World

Ein WebSocket-Client kann sich mit dem Befehl `use` mit einer Welt verbinden. Dabei wird die ID der Welt
übergeben, mit der sich der WebSocket-Client verbinden möchte. Der WebSocket-Client erhält eine Bestätigung, dass er mit der Welt verbunden ist
und die Welt wird an den WebSocket-Client gesendet.

Der Befehl `use` hat folgende Struktur:

```json
{
  "service": "bridge",
  "command": "use",
  "data": {
    "worldId": "string" // ID der Welt, mit der sich der WebSocket-Client verbinden möchte
  },
  "requestId": "string"
}
```

Als Rückgabe werden die detaillierten Informationen der Welt zurückgegeben, die der WebSocket-Client nun 
verwenden kann. Wird keine Welt-ID übergeben (empty), wird die aktuell verwendete Welt-ID zurückgegeben.

Bestehende Registrierungen bei Clustern werden dabei gelöscht, da der WebSocket-Client nun mit einer neuen Welt 
verbunden ist.

## Ping

Ein WebSocket-Client kann den Befehl `ping` verwenden, um zu überprüfen, ob die Verbindung zum WebSocket-Server noch 
aktiv ist. Der WebSocket-Server antwortet mit einem `pong`, um zu bestätigen, dass die Verbindung noch aktiv ist.

Der Befehl `ping` hat folgende Struktur:

```json
{
  "service": "bridge",
  "command": "ping",
  "data": {
    "timestamp": "long" // Optional: Zeitstempel der Anfrage, wird zurückgegeben und verwendet, um die Latenz zu messen
  },
  "requestId": "string"
}
```

## Register Cluster

Ein WebSocket-Client kann sich mit dem Befehl `registerCluster` bei mehreren Clustern registrieren. Dabei 
wird eine Liste von Cluster-Koordinaten und Levels übergeben, bei denen sich der WebSocket-Client registrieren 
möchte. Die bestehende Registrierung wird dabei überschrieben. Der WebSocket-Client erhält eine Bestätigung, 
dass er sich bei den Clustern registriert hat.

Der Befehl `registerCluster` hat folgende Struktur:

```json
{
  "service": "bridge",
  "command": "registerCluster",
  "data": {
    "clusters": [
      {
        "x": 0, // X-Koordinate des Clusters
        "y": 0, // Y-Koordinate des Clusters
        "level": 0 // Level des Clusters
      },
      ...
    ]
  },
  "requestId": "string"
}
```

## Register Terrain

Ein WebSocket-Client kann sich mit dem Befehl `registerTerrain` bei verschiedenen Events des World Terrain Service 
registrieren. Die nicht Cluster bezogenen Events sind:. Dabei werden vorherige registrierungen gelöscht. Der 
WebSocket-Client erhält eine Bestätigung, dass er sich bei den Events registriert hat.

Der Befehl `registerTerrain` hat folgende Struktur:

```json
{
  "service": "bridge",
  "command": "registerTerrain",
  "data": {
    "events": [
      "world", // Event, wenn eine Welt aktualisiert wurde
      "group", // Event, wenn eine Gruppe aktualisiert wurde
      ...
    ]
  },
  "requestId": "string"
}
```

## Terrain Kommandos

Ein WebSocket-Client kann verschiedene Kommandos an den World Terrain Service senden, 
um mit der Welt zu interagieren. Diese Kommandos werden via REST-API an den World Terrain Service gesendet.

Siehe [World Terrain Service](spec/12_world_terrain.md) für die verfügbaren Kommandos.

