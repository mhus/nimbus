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
