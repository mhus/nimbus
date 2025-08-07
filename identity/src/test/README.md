# Identity Service Unit Tests

Dieses Dokument beschreibt die umfassende Test-Suite für den Identity Service, die alle Anforderungen aus den Spezifikationsdateien `spec/02_development.md` und `spec/00_overview.md` abdeckt.

## Überblick der Tests

Die Test-Suite besteht aus mehreren Kategorien von Tests:

### 1. Controller Tests (`IdentityControllerTest`)
- **Zweck**: Testet alle REST-Endpunkte des Identity Service
- **Abdeckung**:
  - User erstellen, abrufen, aktualisieren, löschen (CRUD-Operationen)
  - Login und Token-Erneuerung
  - Passwort-Änderung
  - Autorisierung basierend auf Rollen (USER, ADMIN)
  - Health-Check Endpunkt
  - Fehlerbehandlung für ungültige Anfragen

### 2. Service Tests (`IdentityServiceTest`)
- **Zweck**: Testet die Geschäftslogik des IdentityService
- **Abdeckung**:
  - User-Management-Operationen
  - Login-Authentifizierung
  - Token-Erneuerung
  - Passwort-Änderung
  - Validierung von Eingabedaten
  - Fehlerbehandlung für nicht existierende User

### 3. JWT Token Utils Tests (`JwtTokenUtilsTest`)
- **Zweck**: Testet die JWT-Token-Erstellung und -Extraktion
- **Abdeckung**:
  - Token-Erstellung mit RSA-Schlüssel
  - Extraktion von Ablaufzeit und Erstellungszeit
  - Laden des Private Key
  - Fehlerbehandlung bei ungültigen Keys
  - Token-Gültigkeitsdauer (2 Stunden)

### 4. Identity Service Utils Tests (`IdentityServiceUtilsTest`)
- **Zweck**: Testet JWT-Validierung und Passwort-Hashing
- **Abdeckung**:
  - JWT-Token-Validierung mit RSA Public Key
  - Extraktion von User-ID und Rollen aus Token
  - SHA-256 Passwort-Hashing mit User-ID als Salt
  - Passwort-Verifikation
  - Fehlerbehandlung bei ungültigen Token

### 5. JWT Authentication Filter Tests (`JWTAuthenticationFilterTest`)
- **Zweck**: Testet den JWT-Authentifizierungs-Filter
- **Abdeckung**:
  - Token-Extraktion aus Authorization Header
  - Spring Security Context Setup
  - Behandlung ungültiger Token
  - Öffentliche Endpunkte (Login, Health)
  - Rollen-Mapping zu Spring Security Authorities

### 6. PostgreSQL Integration Tests (`PostgreSQLIntegrationTest`)
- **Zweck**: Testet die Datenbankintegration
- **Abdeckung**:
  - User Entity CRUD-Operationen
  - Rollen-Serialisierung in JSON
  - Eindeutigkeits-Constraints (ID, Email)
  - Timestamp-Felder (createdAt, updatedAt)
  - Sonderzeichen und Unicode-Unterstützung
  - Transaktionale Operationen

### 7. Authorization Integration Tests (`AuthorizationIntegrationTest`)
- **Zweck**: Testet rollenbasierte Zugriffskontrolle
- **Abdeckung**:
  - ADMIN-Rolle: Vollzugriff auf alle Operationen
  - USER-Rolle: Zugriff nur auf eigene Daten
  - MODERATOR-Rolle: USER + zusätzliche Berechtigungen
  - Schutz sensitiver Endpunkte
  - Öffentliche Endpunkte ohne Authentifizierung

## Test-Konfiguration

### Profile
- **Aktives Profil**: `test`
- **Datenbank**: H2 In-Memory (PostgreSQL-Modus für Kompatibilität)
- **Security**: Selektiv deaktiviert für bestimmte Tests

### Dependencies
Die Tests nutzen folgende Frameworks:
- **JUnit 5**: Test-Framework
- **Mockito**: Mocking-Framework
- **Spring Boot Test**: Integration Testing
- **MockMvc**: REST-Endpoint Testing
- **TestEntityManager**: JPA Testing

## Ausführung der Tests

### Einzelne Test-Klassen
```bash
mvn test -Dtest=IdentityControllerTest
mvn test -Dtest=IdentityServiceTest
mvn test -Dtest=PostgreSQLIntegrationTest
```

### Komplette Test-Suite
```bash
mvn test -Dtest=IdentityServiceTestSuite
```

### Alle Tests
```bash
mvn test
```

## Test-Szenarien

### Benutzer-Lifecycle
1. **Erstellung**: User wird mit gehashtem Passwort erstellt
2. **Anmeldung**: Login generiert JWT-Token
3. **Authentifizierung**: Token wird für nachfolgende Requests verwendet
4. **Autorisierung**: Rollenbasierte Zugriffskontrolle
5. **Aktualisierung**: User-Daten können geändert werden
6. **Passwort-Änderung**: Sichere Passwort-Aktualisierung
7. **Löschung**: User wird aus System entfernt (nur Admin)

### Sicherheits-Szenarien
- **Ungültige Token**: Requests mit ungültigen JWT-Token werden abgelehnt
- **Fehlende Berechtigung**: Users können nur auf erlaubte Ressourcen zugreifen
- **Passwort-Sicherheit**: Passwörter werden mit SHA-256 + Salt gehashed
- **Token-Erneuerung**: Sichere Token-Aktualisierung
- **Session-Management**: Stateless JWT-basierte Authentifizierung

### Fehlerbehandlung
- **404 Not Found**: Nicht existierende User
- **401 Unauthorized**: Ungültige Credentials
- **403 Forbidden**: Fehlende Berechtigung
- **400 Bad Request**: Ungültige Eingabedaten
- **409 Conflict**: Duplikate (ID, Email)

## Code Coverage

Die Tests decken folgende Aspekte ab:
- **Controller**: 100% der Endpunkte
- **Service**: Alle Geschäftslogik-Methoden
- **Security**: Alle Autorisierungs-Szenarien
- **Database**: CRUD-Operationen und Constraints
- **JWT**: Token-Erstellung, -Validierung und -Extraktion
- **Password**: Hashing und Verifikation

## Best Practices

### Test-Struktur
- **Arrange-Act-Assert**: Klare Struktur aller Tests
- **Mocking**: Dependencies werden gemockt für Unit Tests
- **Integration**: Echte Datenbankoperationen in Integration Tests
- **Cleanup**: Database wird vor jedem Test geleert

### Daten-Management
- **Test-Isolation**: Jeder Test läuft isoliert
- **Transactional**: Rollback nach jedem Test
- **Mock-Data**: Realistische Test-Daten
- **Edge-Cases**: Grenzfälle werden getestet

### Sicherheit
- **Sensitive Daten**: Keine echten Passwörter in Tests
- **Mock-Tokens**: Verwendung von Mock JWT-Token
- **Test-Keys**: Separate Schlüssel für Tests
- **Isolation**: Security-Tests sind isoliert

## Kontinuierliche Integration

Die Tests sind darauf ausgelegt, in CI/CD-Pipelines zu laufen:
- **Maven**: Standard Maven-Test-Lifecycle
- **Parallel**: Tests können parallel ausgeführt werden
- **Docker**: Kompatibel mit containerisierten Umgebungen
- **Reporting**: JUnit-XML Reports für CI-Integration
