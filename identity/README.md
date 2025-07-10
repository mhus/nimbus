# Identity Service

Der Identity Service verwaltet Benutzer und deren Charaktere im Nimbus-System.

## IdentityCharacter API

Der IdentityCharacterController bietet folgende REST-Endpunkte:

### 1. Neuen Charakter erstellen

```bash
curl -X POST http://localhost:7082/api/characters \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "name": "Aragorn",
    "characterClass": "Warrior",
    "description": "Ein tapferer Krieger aus dem Norden"
  }'
```

### 2. Charakter anhand ID abrufen

```bash
curl -X GET http://localhost:7082/api/characters/1
```

### 3. Charakter anhand Name abrufen

```bash
curl -X GET http://localhost:7082/api/characters/name/Aragorn
```

### 4. Alle Charaktere eines Benutzers abrufen

```bash
curl -X GET http://localhost:7082/api/characters/user/1
```

### 5. Alle aktiven Charaktere eines Benutzers abrufen

```bash
curl -X GET http://localhost:7082/api/characters/user/1/active
```

### 6. Position eines Charakters aktualisieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/position \
  -H "Content-Type: application/json" \
  -d '{
    "worldId": "world_001",
    "planet": "earth",
    "x": 123.45,
    "y": 67.89,
    "z": 234.56
  }'
```

### 7. Level und Erfahrungspunkte aktualisieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/level \
  -H "Content-Type: application/json" \
  -d '{
    "level": 25,
    "experiencePoints": 150000
  }'
```

### 8. Gesundheits- und Manapunkte aktualisieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/stats \
  -H "Content-Type: application/json" \
  -d '{
    "healthPoints": 100,
    "manaPoints": 80
  }'
```

### 9. Letzten Login-Zeitstempel aktualisieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/login
```

### 10. Charakter deaktivieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/deactivate
```

### 11. Charakter reaktivieren

```bash
curl -X PUT http://localhost:7082/api/characters/1/reactivate
```

## Antwortbeispiele

### Erfolgreiche Charaktererstellung (201 Created)

```json
{
  "id": 1,
  "name": "Aragorn",
  "characterClass": "Warrior",
  "description": "Ein tapferer Krieger aus dem Norden",
  "level": 1,
  "experiencePoints": 0,
  "healthPoints": 100,
  "manaPoints": 50,
  "worldId": null,
  "planet": null,
  "x": null,
  "y": null,
  "z": null,
  "createdAt": "2025-07-10T10:30:00Z",
  "lastLogin": null,
  "active": true,
  "user": {
    "id": 1,
    "username": "player1"
  }
}
```

### Fehlerbeispiele

#### Benutzer nicht gefunden (400 Bad Request)
```bash
curl -X POST http://localhost:7082/api/characters \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 999,
    "name": "TestChar",
    "characterClass": "Mage",
    "description": "Test"
  }'
```

#### Charakter nicht gefunden (404 Not Found)
```bash
curl -X GET http://localhost:7082/api/characters/999
```

## Entwicklung

### Lokale Entwicklungsumgebung starten

```bash
cd deployment/local
./start-dev-env.sh
```

### Service starten

```bash
mvn spring-boot:run
```

Der Service läuft standardmäßig auf Port 8080.
