# World Test Module

System-Tests für den externen World Service test_server.

## Überblick

Dieses Modul testet:
- **WebSocket Messages** gegen test_server auf Port 3001
- **Player API Endpoints** gegen player-server auf Port 3002 (Read-Only)
- **Editor API Endpoints** gegen editor-server auf Port 3003 (Full CRUD)
- **Generated DTOs Contract** - alle Typen aus `../client/packages/shared/src`

## Generated DTOs als Contract

Das `generated` Maven-Modul enthält alle migrierten TypeScript-Typen:
- **Types**: `de.mhus.nimbus.generated.types.*` (BlockType, Entity, etc.)
- **REST DTOs**: `de.mhus.nimbus.generated.rest.*` (BlockTypeDTO, etc.)
- **Network Messages**: `de.mhus.nimbus.generated.network.messages.*`

**Wichtig**: Diese DTOs dienen als unveränderlicher Contract und validieren die API-Kompatibilität.

### Beispiel Verwendung:
```java
// REST API Contract Validierung
BlockTypeDTO blockType = objectMapper.treeToValue(jsonNode, BlockTypeDTO.class);

// WebSocket Message Contract
ChunkCoordinate coord = ChunkCoordinate.builder()
    .cx(0.0)  // Chunk X coordinate  
    .cz(0.0)  // Chunk Z coordinate
    .build();
```

## Externe Abhängigkeiten

Die Tests benötigen laufende Server:

**test_server** (WebSocket + Player Read-Only):
```bash
cd ../client/packages/test_server
npm start
```

**world-editor** (Full CRUD):
```bash
# Separater Editor Server mit CRUD Support
# (Details zur Konfiguration werden separat bereitgestellt)
```

Server URLs:
- **WebSocket**: ws://localhost:3001
- **Player API (Read-Only)**: http://localhost:3011  
- **Editor API (CRUD)**: http://localhost:3011 (aktuell gleicher Server)

**Hinweis**: WebSocket läuft auf 3001, beide REST APIs auf 3011. Nach der Migration werden sie getrennt.

## Konfiguration

Test-Parameter in `src/test/resources/application.yaml`:
```yaml
test.server.websocket.url: ws://localhost:3001
test.server.player.url: http://localhost:3011
test.server.editor.url: http://localhost:3011  # aktuell gleicher Server
test.login.username: testuser
test.login.password: testpass
test.login.worldId: test-world
test.login.clientType: web
```

## Test-Struktur

```
src/test/java/de/mhus/nimbus/world/test/
├── AbstractSystemTest.java              # Basis-Konfiguration
├── GeneratedContractExampleTest.java    # DTO Contract Examples
├── websocket/
│   ├── AbstractWebSocketTest.java       # WebSocket Test Basis
│   ├── WebSocketLoginTest.java          # Login & Ping Tests
│   └── WebSocketChunkTest.java          # Chunk Registration/Query Tests
├── player/
│   ├── AbstractPlayerTest.java          # Player Test Basis (Read-Only Server)
│   ├── PlayerWorldApiTest.java          # World API Tests (/api/worlds)
│   ├── PlayerAssetsApiTest.java         # Assets API Tests (/api/worlds/{id}/assets)
│   ├── PlayerBlockTypeTest.java         # BlockType API Tests (/api/worlds/{id}/blocktypes)
│   ├── PlayerBlockOperationsTest.java   # Block Operations (/api/worlds/{id}/blocks)
│   └── PlayerApiOverviewTest.java       # Player API Übersicht
└── editor/
    ├── AbstractEditorTest.java          # Editor Test Basis (Full CRUD Server)
    ├── EditorBlockTypeTest.java         # BlockType CRUD Tests (POST/PUT/DELETE)
    ├── EditorAssetsTest.java            # Assets CRUD Tests (Upload/Update/Delete)
    ├── EditorBlockOperationsTest.java   # Block CRUD Tests (Create/Update/Delete)
    └── EditorApiOverviewTest.java       # Editor API Übersicht
```

## Test-Kategorien

### WebSocket Tests
- **Login/Authentication** mit sessionId
- **Ping/Pong** Latenz-Messung
- **Chunk Registration** mit generated ChunkCoordinate DTOs
- **Chunk Query** mit Contract-Validierung

### Player Tests (alle GET Endpoints)
- **World API** - Liste und Details von Welten
- **Assets API** - Asset-Listen, Suche, Filterung und Download
- **BlockType API** - BlockType Listen, Einzelne, Suche und Chunks
- **Block Operations** - Einzelne Blöcke an Koordinaten abfragen
- **Bearer Token Auth** - Alle Requests nutzen SessionId als Bearer Token

### Editor Tests (vollständige CRUD Operations)
- **BlockType CRUD** - Create, Read, Update, Delete BlockTypes
- **Assets CRUD** - Upload, Update, Delete von Binary Assets
- **Block CRUD** - Create, Update, Delete von Blöcken an Koordinaten
- **Multi-Server** - Gleiche GET Tests + zusätzliche POST/PUT/DELETE

## Test-Ablauf

### WebSocket Tests
1. **Login** - Authentifizierung mit username/password  
2. **Chunk Registration** - Chunks mit ChunkCoordinate DTOs registrieren
3. **Chunk Query** - Chunk-Daten anfragen und validieren
4. **Ping** - Latenz nach Chunk-Operationen messen

### Player Tests (Read-Only Server - Port 3011)
1. **WebSocket Login** - SessionId über WebSocket holen
2. **World API Tests**:
   - GET /api/worlds - Liste aller Welten
   - GET /api/worlds/{id} - Details einer Welt
3. **Assets API Tests**:
   - GET /api/worlds/{id}/assets - Asset Liste
   - GET /api/worlds/{id}/assets?query=stone - Asset Suche
   - GET /api/worlds/{id}/assets?ext=png - Asset Filterung  
   - GET /api/worlds/{id}/assets/{path} - Asset Download
4. **BlockType API Tests**:
   - GET /api/worlds/{id}/blocktypes - BlockType Liste
   - GET /api/worlds/{id}/blocktypes?query=fence - BlockType Suche
   - GET /api/worlds/{id}/blocktypes/{id} - Einzelner BlockType
   - GET /api/worlds/{id}/blocktypeschunk/{group} - BlockType Range
5. **Block Operations Tests**:
   - GET /api/worlds/{id}/blocks/{x}/{y}/{z} - Block an Position
   - Mehrere Positionen testen
   - Error Handling validieren

### Editor Tests (Full CRUD Server - Port 3011)
1. **WebSocket Login** - SessionId über WebSocket holen  
2. **BlockType CRUD**:
   - GET (gleich wie Player) + POST/PUT/DELETE
   - Vollständiger CRUD Cycle Test
3. **Assets CRUD**:
   - GET (gleich wie Player) + POST/PUT/DELETE
   - Binary Upload/Download Tests
   - Multiple Asset Formate
4. **Block Operations CRUD**:
   - GET (gleich wie Player) + POST/PUT/DELETE
   - Block Create/Update/Delete an Koordinaten
   - Multiple Positions Testing

## Wichtige Eigenschaften

- **Sequenziell**: Tests laufen nacheinander (forkCount=1)
- **Stateful**: SessionId wird zwischen Tests übertragen
- **External**: Testet gegen externen Server, nicht internen Code
- **Contract Driven**: Nutzt generated DTOs zur API-Validierung

## Ausführung

### Standard Tests
```bash
# Alle Tests ausführen (Server muss laufen)
mvn clean verify
mvn test
```

### Spezifische Test-Gruppen
```bash
# Nur WebSocket Tests
mvn test -Dtest="**/websocket/*Test"

# Nur Player Tests (Read-Only Server)
mvn test -Dtest="**/player/*Test"

# Nur Editor Tests (CRUD Server)  
mvn test -Dtest="**/editor/*Test"

# Nur Contract Tests (ohne Server-Verbindung)
mvn test -Dtest="GeneratedContractExampleTest"
```

### Spezifische Tests
```bash
# Player API Tests
mvn test -Dtest="PlayerWorldApiTest"
mvn test -Dtest="PlayerAssetsApiTest"
mvn test -Dtest="PlayerBlockTypeTest"
mvn test -Dtest="PlayerBlockOperationsTest"

# Editor CRUD Tests
mvn test -Dtest="EditorBlockTypeTest"
mvn test -Dtest="EditorAssetsTest"
mvn test -Dtest="EditorBlockOperationsTest"

# WebSocket Tests
mvn test -Dtest="WebSocketLoginTest"
mvn test -Dtest="WebSocketChunkTest"
```

### Server starten
```bash
cd ../client/packages/test_server
npm start  # WebSocket Port 3001 + REST APIs Port 3011
```

## Fehlerbehebung

### "Connection refused" Fehler
Wenn Tests mit `Connection refused` fehlschlagen:

1. **Überprüfe Server Status:**
   ```bash
   # REST APIs (Player + Editor)
   curl http://localhost:3011/api/worlds
   
   # WebSocket Server
   curl http://localhost:3001/
   ```

2. **Starte Test Server:**
   ```bash
   cd ../client/packages/test_server
   npm install  # falls noch nicht geschehen
   npm start
   ```

3. **Teste nur Contract Validation (ohne Server):**
   ```bash
   mvn test -Dtest="GeneratedContractExampleTest"
   ```

### Jackson Deserialization Fehler
Wenn DTOs nicht deserialisiert werden können:

- **Problem**: Generated DTOs benötigen Lombok Konfiguration
- **Lösung**: Tests sind für Serialization optimiert, Deserialization ist optional
- **Workaround**: Nutze nur DTO Construction und Serialization Tests

### Nach der Migration (Player/Editor getrennt)
Wenn Player und Editor auf verschiedene Ports migriert werden:

1. **Aktualisiere application.yaml:**
   ```yaml
   test.server.editor.url: http://localhost:3003  # neuer Port
   ```

2. **Alle Tests sollten weiterhin funktionieren** - das ist das Ziel der Tests

## ✅ Ziel der Tests

Diese Tests sind darauf ausgelegt, **nach der Migration von Player/Editor Servern** zu validieren, dass alle Funktionen noch korrekt funktionieren. Aktuell laufen beide REST APIs auf Port 3011, nach der Migration werden sie getrennt.
```
