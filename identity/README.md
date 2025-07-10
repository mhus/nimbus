# Identity Service

Der Identity Service verwaltet Benutzer und Authentifizierung im Nimbus System.

## API Endpoints

### User Management

Der UserController stellt folgende REST-Endpoints zur Verfügung:

#### 1. Neuen Benutzer erstellen

**Endpoint:** `POST /api/users`

```bash
curl -X POST http://localhost:7082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Max",
    "lastName": "Mustermann"
  }'
```

**Antwort (201 Created):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Max",
  "lastName": "Mustermann",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:30:00Z"
}
```

#### 2. Benutzer anhand der ID abrufen

**Endpoint:** `GET /api/users/{id}`

```bash
curl -X GET http://localhost:7082/api/users/1 \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Max",
  "lastName": "Mustermann",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:30:00Z"
}
```

**Antwort (404 Not Found):** wenn Benutzer nicht existiert

#### 3. Benutzer anhand des Benutzernamens abrufen

**Endpoint:** `GET /api/users/username/{username}`

```bash
curl -X GET http://localhost:7082/api/users/username/testuser \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Max",
  "lastName": "Mustermann",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:30:00Z"
}
```

**Antwort (404 Not Found):** wenn Benutzer nicht existiert

#### 4. Benutzer deaktivieren

**Endpoint:** `DELETE /api/users/{id}`

```bash
curl -X DELETE http://localhost:7082/api/users/1
```

**Antwort (204 No Content):** bei erfolgreicher Deaktivierung

**Antwort (404 Not Found):** wenn Benutzer nicht existiert

## Beispiel-Workflow

### Komplettes Beispiel für User-Erstellung und -Abfrage

```bash
# 1. Neuen Benutzer erstellen
curl -X POST http://localhost:7082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john.doe@example.com",
    "password": "mySecretPassword",
    "firstName": "John",
    "lastName": "Doe"
  }'

# 2. Benutzer anhand der ID abrufen (verwende die ID aus der Antwort)
curl -X GET http://localhost:7082/api/users/1

# 3. Benutzer anhand des Benutzernamens abrufen
curl -X GET http://localhost:7082/api/users/username/johndoe

# 4. Benutzer deaktivieren
curl -X DELETE http://localhost:7082/api/users/1
```

## Fehlerbehandlung

### Häufige Fehler

- **400 Bad Request:** bei ungültigen Eingabedaten (z.B. bereits existierender Benutzername oder E-Mail)
- **404 Not Found:** wenn der angeforderte Benutzer nicht existiert
- **500 Internal Server Error:** bei unerwarteten Serverfehlern

### Beispiel für Fehlerfall

```bash
# Versuch, einen Benutzer mit bereits existierendem Benutzernamen zu erstellen
curl -X POST http://localhost:7082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "another@example.com",
    "password": "password123",
    "firstName": "Another",
    "lastName": "User"
  }'
```

**Antwort:** `400 Bad Request` (da der Benutzername bereits existiert)

## Entwicklung

### Service starten

```bash
mvn spring-boot:run
```

Der Service läuft standardmäßig auf Port 7082.

### Tests ausführen

```bash
mvn test
```

### ACE (Access Control Entity) Management

Der AceController stellt folgende REST-Endpoints zur Verfügung:

#### 1. Neue ACE erstellen

**Endpoint:** `POST /api/ace`

```bash
curl -X POST http://localhost:7082/api/ace \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "READ:user:profile",
    "userId": 1,
    "description": "Berechtigung zum Lesen von Benutzerprofilen"
  }'
```

**Antwort (201 Created):**
```json
{
  "id": 1,
  "rule": "READ:user:profile",
  "orderValue": 1,
  "description": "Berechtigung zum Lesen von Benutzerprofilen",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:30:00Z"
}
```

#### 2. ACE mit spezifischer Reihenfolge erstellen

**Endpoint:** `POST /api/ace/with-order`

```bash
curl -X POST http://localhost:7082/api/ace/with-order \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "WRITE:user:profile",
    "userId": 1,
    "orderValue": 5,
    "description": "Berechtigung zum Bearbeiten von Benutzerprofilen"
  }'
```

**Antwort (201 Created):**
```json
{
  "id": 2,
  "rule": "WRITE:user:profile",
  "orderValue": 5,
  "description": "Berechtigung zum Bearbeiten von Benutzerprofilen",
  "active": true,
  "createdAt": "2025-07-10T10:31:00Z",
  "updatedAt": "2025-07-10T10:31:00Z"
}
```

#### 3. ACE anhand der ID abrufen

**Endpoint:** `GET /api/ace/{aceId}`

```bash
curl -X GET http://localhost:7082/api/ace/1 \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
{
  "id": 1,
  "rule": "READ:user:profile",
  "orderValue": 1,
  "description": "Berechtigung zum Lesen von Benutzerprofilen",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:30:00Z"
}
```

#### 4. Alle ACEs für einen Benutzer abrufen

**Endpoint:** `GET /api/ace/user/{userId}`

```bash
curl -X GET http://localhost:7082/api/ace/user/1 \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
[
  {
    "id": 1,
    "rule": "READ:user:profile",
    "orderValue": 1,
    "description": "Berechtigung zum Lesen von Benutzerprofilen",
    "active": true,
    "createdAt": "2025-07-10T10:30:00Z",
    "updatedAt": "2025-07-10T10:30:00Z"
  },
  {
    "id": 2,
    "rule": "WRITE:user:profile",
    "orderValue": 2,
    "description": "Berechtigung zum Bearbeiten von Benutzerprofilen",
    "active": true,
    "createdAt": "2025-07-10T10:31:00Z",
    "updatedAt": "2025-07-10T10:31:00Z"
  }
]
```

#### 5. Nur aktive ACEs für einen Benutzer abrufen

**Endpoint:** `GET /api/ace/user/{userId}/active`

```bash
curl -X GET http://localhost:7082/api/ace/user/1/active \
  -H "Accept: application/json"
```

**Antwort (200 OK):** Gleiche Struktur wie oben, aber nur aktive ACEs

#### 6. ACE aktualisieren

**Endpoint:** `PUT /api/ace/{aceId}`

```bash
curl -X PUT http://localhost:7082/api/ace/1 \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "READ:user:all",
    "orderValue": 3,
    "description": "Erweiterte Berechtigung zum Lesen aller Benutzer",
    "active": true
  }'
```

**Antwort (200 OK):**
```json
{
  "id": 1,
  "rule": "READ:user:all",
  "orderValue": 3,
  "description": "Erweiterte Berechtigung zum Lesen aller Benutzer",
  "active": true,
  "createdAt": "2025-07-10T10:30:00Z",
  "updatedAt": "2025-07-10T10:35:00Z"
}
```

#### 7. ACE Position ändern

**Endpoint:** `PATCH /api/ace/{aceId}/move`

```bash
curl -X PATCH http://localhost:7082/api/ace/1/move \
  -H "Content-Type: application/json" \
  -d '{
    "newOrderValue": 1
  }'
```

**Antwort (200 OK):** ACE mit neuer Position

#### 8. ACEs anhand einer Regel suchen

**Endpoint:** `GET /api/ace/search?rule={rulePattern}`

```bash
curl -X GET "http://localhost:7082/api/ace/search?rule=READ" \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
[
  {
    "id": 1,
    "rule": "READ:user:profile",
    "orderValue": 1,
    "description": "Berechtigung zum Lesen von Benutzerprofilen",
    "active": true,
    "createdAt": "2025-07-10T10:30:00Z",
    "updatedAt": "2025-07-10T10:30:00Z"
  }
]
```

#### 9. Anzahl der ACEs für einen Benutzer abrufen

**Endpoint:** `GET /api/ace/user/{userId}/count`

```bash
curl -X GET http://localhost:7082/api/ace/user/1/count \
  -H "Accept: application/json"
```

**Antwort (200 OK):**
```json
3
```

#### 10. ACE löschen

**Endpoint:** `DELETE /api/ace/{aceId}`

```bash
curl -X DELETE http://localhost:7082/api/ace/1
```

**Antwort (204 No Content):** bei erfolgreicher Löschung

#### 11. Alle ACEs für einen Benutzer löschen

**Endpoint:** `DELETE /api/ace/user/{userId}`

```bash
curl -X DELETE http://localhost:7082/api/ace/user/1
```

**Antwort (204 No Content):** bei erfolgreicher Löschung aller ACEs

## Beispiel-Workflow

### Komplettes Beispiel für ACE-Erstellung und -Abfrage

```bash
# 1. Neue ACE erstellen
curl -X POST http://localhost:7082/api/ace \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "READ:user:profile",
    "userId": 1,
    "description": "Berechtigung zum Lesen von Benutzerprofilen"
  }'

# 2. ACE mit spezifischer Reihenfolge erstellen
curl -X POST http://localhost:7082/api/ace/with-order \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "WRITE:user:profile",
    "userId": 1,
    "orderValue": 5,
    "description": "Berechtigung zum Bearbeiten von Benutzerprofilen"
  }'

# 3. ACE anhand der ID abrufen
curl -X GET http://localhost:7082/api/ace/1

# 4. Alle ACEs für einen Benutzer abrufen
curl -X GET http://localhost:7082/api/ace/user/1

# 5. Nur aktive ACEs für einen Benutzer abrufen
curl -X GET http://localhost:7082/api/ace/user/1/active

# 6. ACE aktualisieren
curl -X PUT http://localhost:7082/api/ace/1 \
  -H "Content-Type: application/json" \
  -d '{
    "rule": "READ:user:all",
    "orderValue": 3,
    "description": "Erweiterte Berechtigung zum Lesen aller Benutzer",
    "active": true
  }'

# 7. ACE Position ändern
curl -X PATCH http://localhost:7082/api/ace/1/move \
  -H "Content-Type: application/json" \
  -d '{
    "newOrderValue": 1
  }'

# 8. ACEs anhand einer Regel suchen
curl -X GET "http://localhost:7082/api/ace/search?rule=READ" \
  -H "Accept: application/json"

# 9. Anzahl der ACEs für einen Benutzer abrufen
curl -X GET http://localhost:7082/api/ace/user/1/count

# 10. ACE löschen
curl -X DELETE http://localhost:7082/api/ace/1

# 11. Alle ACEs für einen Benutzer löschen
curl -X DELETE http://localhost:7082/api/ace/user/1
```

