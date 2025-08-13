# World Bridge WebSocket Examples

Diese Beispiele zeigen, wie Sie mit websocat mit dem World Bridge Service interagieren können.

## Installation von websocat

```bash
# macOS mit Homebrew
brew install websocat

# Linux
wget https://github.com/vi/websocat/releases/latest/download/websocat.x86_64-unknown-linux-musl
chmod +x websocat.x86_64-unknown-linux-musl
sudo mv websocat.x86_64-unknown-linux-musl /usr/local/bin/websocat
```

## Verbindung zum World Bridge Service

```bash
websocat ws://localhost:8082/ws
```

## Login

Melden Sie sich mit einem gültigen Token an:

```json
{
  "service": "bridge",
  "command": "login",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  },
  "requestId": "login-001"
}
```

Erwartete Antwort bei erfolgreichem Login:

```json
{
  "service": "bridge",
  "command": "login",
  "data": {
    "valid": true,
    "userId": "user-123",
    "roles": ["USER", "PLAYER"],
    "username": "testuser"
  },
  "requestId": "login-001",
  "status": "success",
  "message": "Login successful"
}
```

## Ping

Testen Sie die Verbindung mit einem Ping:

```json
{
  "service": "bridge",
  "command": "ping",
  "data": {
    "timestamp": 1692345678901
  },
  "requestId": "ping-001"
}
```

Erwartete Antwort:

```json
{
  "service": "bridge",
  "command": "pong",
  "data": {
    "timestamp": 1692345678901
  },
  "requestId": "ping-001",
  "status": "success",
  "message": "Pong"
}
```

## Welt auswählen

Wählen Sie eine Welt aus:

```json
{
  "service": "bridge",
  "command": "use",
  "data": {
    "worldId": "world-123"
  },
  "requestId": "use-world-001"
}
```

Erwartete Antwort:

```json
{
  "service": "bridge",
  "command": "use",
  "data": {
    "id": "world-123",
    "name": "Test World",
    "description": "A test world for development"
  },
  "requestId": "use-world-001",
  "status": "success",
  "message": "World selected successfully"
}
```

## Aktuelle Welt abfragen

Fragen Sie die aktuell ausgewählte Welt ab (leere worldId):

```json
{
  "service": "bridge",
  "command": "use",
  "data": {
    "worldId": ""
  },
  "requestId": "current-world-001"
}
```

## Cluster registrieren

Registrieren Sie sich für Cluster-Events:

```json
{
  "service": "bridge",
  "command": "registerCluster",
  "data": {
    "clusters": [
      {
        "x": 0,
        "y": 0,
        "level": 0
      },
      {
        "x": 1,
        "y": 0,
        "level": 0
      },
      {
        "x": 0,
        "y": 1,
        "level": 0
      }
    ]
  },
  "requestId": "register-cluster-001"
}
```

Erwartete Antwort:

```json
{
  "service": "bridge",
  "command": "registerCluster",
  "data": {
    "clusters": [
      {
        "x": 0,
        "y": 0,
        "level": 0
      },
      {
        "x": 1,
        "y": 0,
        "level": 0
      },
      {
        "x": 0,
        "y": 1,
        "level": 0
      }
    ]
  },
  "requestId": "register-cluster-001",
  "status": "success",
  "message": "Cluster registration successful"
}
```

## Terrain-Events registrieren

Registrieren Sie sich für Terrain-Events:

```json
{
  "service": "bridge",
  "command": "registerTerrain",
  "data": {
    "events": [
      "world",
      "group",
      "cluster",
      "tile",
      "sprite"
    ]
  },
  "requestId": "register-terrain-001"
}
```

Erwartete Antwort:

```json
{
  "service": "bridge",
  "command": "registerTerrain",
  "data": {
    "events": [
      "world",
      "group",
      "cluster",
      "tile",
      "sprite"
    ]
  },
  "requestId": "register-terrain-001",
  "status": "success",
  "message": "Terrain event registration successful"
}
```

## Vollständiges Beispiel-Session

```bash
# Terminal 1: Starten Sie websocat
websocat ws://localhost:8082/ws

# Senden Sie folgende Nachrichten in der Reihenfolge:

# 1. Login
{"service":"bridge","command":"login","data":{"token":"your-jwt-token-here"},"requestId":"001"}

# 2. Welt auswählen
{"service":"bridge","command":"use","data":{"worldId":"world-123"},"requestId":"002"}

# 3. Cluster registrieren
{"service":"bridge","command":"registerCluster","data":{"clusters":[{"x":0,"y":0,"level":0}]},"requestId":"003"}

# 4. Terrain-Events registrieren
{"service":"bridge","command":"registerTerrain","data":{"events":["world","cluster"]},"requestId":"004"}

# 5. Ping senden
{"service":"bridge","command":"ping","data":{"timestamp":1692345678901},"requestId":"005"}
```

## Fehlerbehandlung

### Ungültiger Token

```json
{
  "service": "bridge",
  "command": "login",
  "data": {
    "token": "invalid-token"
  },
  "requestId": "login-error-001"
}
```

Antwort:

```json
{
  "service": "bridge",
  "command": "login",
  "requestId": "login-error-001",
  "status": "error",
  "errorCode": "INVALID_TOKEN",
  "message": "Invalid authentication token"
}
```

### Kommando ohne Login

```json
{
  "service": "bridge",
  "command": "ping",
  "data": {},
  "requestId": "ping-no-auth-001"
}
```

Antwort:

```json
{
  "service": "bridge",
  "command": "ping",
  "requestId": "ping-no-auth-001",
  "status": "error",
  "errorCode": "NOT_AUTHENTICATED",
  "message": "User not authenticated"
}
```

### Kommando ohne Welt

```json
{
  "service": "bridge",
  "command": "registerCluster",
  "data": {"clusters": []},
  "requestId": "register-no-world-001"
}
```

Antwort:

```json
{
  "service": "bridge",
  "command": "registerCluster",
  "requestId": "register-no-world-001",
  "status": "error",
  "errorCode": "NO_WORLD_SELECTED",
  "message": "No world selected"
}
```
