# Test Common Module

Dieses Modul stellt REST API Controller zur Verfügung, um die Funktionen der Client-Beans aus dem 'common' Modul zu testen. Es bietet HTTP-Endpunkte für alle vier Client-Beans: WorldLifeClient, IdentityClient, RegistryClient und WorldVoxelClient sowie den SecurityService.

## Starten der Anwendung

```bash
cd test-common
mvn spring-boot:run
```

Die Anwendung startet standardmäßig auf Port 7090.

## REST API Endpunkte

### WorldLife Client - Character Management

#### Character Lookup by ID
```bash
# Suche nach einem Charakter anhand der ID
curl -X GET "http://localhost:7090/api/test/world-life/character/12345"
```

#### Character Lookup by Name
```bash
# Suche nach einem Charakter anhand des Namens
curl -X GET "http://localhost:7090/api/test/world-life/character/name/DragonSlayer"
```

#### Characters Lookup by User ID
```bash
# Suche nach allen aktiven Charakteren eines Users
curl -X GET "http://localhost:7090/api/test/world-life/characters/user/123?activeOnly=true"

# Suche nach allen Charakteren eines Users (aktive und inaktive)
curl -X GET "http://localhost:7090/api/test/world-life/characters/user/123?activeOnly=false"
```

#### Characters Lookup by World ID
```bash
# Suche nach allen aktiven Charakteren in einer Welt
curl -X GET "http://localhost:7090/api/test/world-life/characters/world/world-alpha?activeOnly=true"

# Suche nach allen Charakteren in einer Welt
curl -X GET "http://localhost:7090/api/test/world-life/characters/world/world-alpha?activeOnly=false"
```

### Identity Client - Authentication & User Management

#### Login Request
```bash
# Login mit Benutzername und Passwort
curl -X POST "http://localhost:7090/api/test/identity/login?username=testuser&password=mypassword"
```
```bash
# Login mit zusätzlichen Client-Informationen
curl -X POST "http://localhost:7090/api/test/identity/login?username=testuser&password=mypassword&clientInfo=TestClient"
```

#### User Lookup by ID
```bash
# Suche nach einem Benutzer anhand der ID
curl -X GET "http://localhost:7090/api/test/identity/user/123"
```

#### User Lookup by Username
```bash
# Suche nach einem Benutzer anhand des Benutzernamens
curl -X GET "http://localhost:7090/api/test/identity/user/username/testuser"
```

#### User Lookup by Email
```bash
# Suche nach einem Benutzer anhand der E-Mail-Adresse
curl -X GET "http://localhost:7090/api/test/identity/user/email/test@example.com"
```

#### Character Lookup by ID
```bash
# Suche nach einem Charakter anhand der ID
curl -X GET "http://localhost:7090/api/test/identity/character/12345"
```

#### Character Lookup by Name
```bash
# Suche nach einem Charakter anhand des Namens
curl -X GET "http://localhost:7090/api/test/identity/character/name/DragonSlayer"
```

#### Characters Lookup by User ID
```bash
# Suche nach allen Charakteren eines Users
curl -X GET "http://localhost:7090/api/test/identity/characters/user/123"
```

#### Public Key Request
```bash
# Öffentlichen Schlüssel vom Identity Service abrufen
curl -X GET "http://localhost:7090/api/test/identity/public-key"
```

### Security Service - JWT Token Management

Der SecurityService bietet erweiterte Sicherheitsfunktionen basierend auf dem IdentityClient.

#### Login (Synchron)
```bash
# Einfacher Login
curl -X POST "http://localhost:7090/api/test/security/login?username=testuser&password=password123"
```
```bash
# Login mit Client-Info
curl -X POST "http://localhost:7090/api/test/security/login?username=testuser&password=password123&clientInfo=WebApp"
```

#### Login (Asynchron)
```bash
# Asynchroner Login
curl -X POST "http://localhost:7090/api/test/security/login-async?username=testuser&password=mypassword"
```
```bash
# Asynchroner Login mit Client-Info
curl -X POST "http://localhost:7090/api/test/security/login-async?username=testuser&password=mypassword&clientInfo=WebApp"
```

#### Public Key abrufen (mit Cache)
```bash
# Public Key aus Cache (falls vorhanden)
curl -X GET "http://localhost:7090/api/test/security/public-key"
```
```bash
# Public Key erzwingen (Cache umgehen)
curl -X GET "http://localhost:7090/api/test/security/public-key?forceRefresh=true"
```

#### Public Key asynchron abrufen
```bash
# Asynchroner Public Key Abruf
curl -X GET "http://localhost:7090/api/test/security/public-key-async"
```
```bash
# Asynchroner Public Key Abruf (Cache umgehen)
curl -X GET "http://localhost:7090/api/test/security/public-key-async?forceRefresh=true"
```

#### JWT Token validieren
```bash
# Token validieren (ersetze YOUR_JWT_TOKEN mit einem echten Token)
curl -X POST "http://localhost:7090/api/test/security/validate-token?token=YOUR_JWT_TOKEN"
```

#### Cache-Management
```bash
# Gecachten Public Key anzeigen
curl -X GET "http://localhost:7090/api/test/security/cached-public-key"
```
```bash
# Prüfen ob gültiger Public Key gecacht ist
curl -X GET "http://localhost:7090/api/test/security/has-valid-public-key"
```
```bash
# Public Key Cache leeren
curl -X DELETE "http://localhost:7090/api/test/security/public-key-cache"
```

### Beispiel-Workflow: Complete Authentication Test

```bash
# 1. Public Key abrufen
curl -X GET "http://localhost:7090/api/test/security/public-key"
```

```bash
# 2. Login durchführen
curl -X POST "http://localhost:7090/api/test/security/login?username=testuser&password=mypassword"
```

```bash
# 3. Mit dem erhaltenen Token validieren (Token aus Schritt 2 verwenden)
curl -X POST "http://localhost:7090/api/test/security/validate-token?token=RECEIVED_JWT_TOKEN"
```

```bash
# 4. Cache-Status prüfen
curl -X GET "http://localhost:7090/api/test/security/has-valid-public-key"
```

```bash
# 5. Cache leeren
curl -X DELETE "http://localhost:7090/api/test/security/public-key-cache"
```

### Beispiel für Login und Token-Validierung

```bash
# Login durchführen und Token extrahieren
token=$(curl -X POST "http://localhost:7090/api/test/security/login?username=testuser&password=password123"|grep Token|cut -d ' ' -f 2)
```

```bash
# Token validieren
curl -X POST "http://localhost:7090/api/test/security/validate-token?token=$token"
```

### Fehlerbehandlung

#### Häufige HTTP-Statuscodes
- **200 OK:** Erfolgreiche Anfrage
- **400 Bad Request:** Ungültige Eingabedaten (z.B. fehlende Parameter, ungültiges Token)
- **408 Request Timeout:** Anfrage hat das 30-Sekunden-Timeout überschritten
- **500 Internal Server Error:** Unerwarteter Serverfehler

#### Beispiele für Fehlerfälle

```bash
# Ungültige Login-Daten
curl -X POST "http://localhost:7090/api/test/security/login?username=wronguser&password=wrongpass"
# Antwort: 400 Bad Request mit NimbusException Details
```

```bash
# Ungültiges Token
curl -X POST "http://localhost:7090/api/test/security/validate-token?token=invalid.token.here"
# Antwort: 400 Bad Request mit Token-Validierungs-Fehler
```

```bash
# Fehlende Parameter
curl -X POST "http://localhost:7090/api/test/security/login"
# Antwort: 400 Bad Request wegen fehlender username/password Parameter
```

## Entwicklung

### Voraussetzungen
- Java 17+
- Maven 3.6+
- Laufende Kafka-Infrastruktur (für Client-Kommunikation)
- Laufende Identity Service Instanz

### Service starten
```bash
mvn spring-boot:run
```

### Tests ausführen
```bash
mvn test
```

### Logs
Die Anwendung schreibt detaillierte Logs für alle Test-Anfragen, einschließlich Request-IDs und Response-Status.
