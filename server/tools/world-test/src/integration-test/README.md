# Integration Tests

Dieses Verzeichnis enthält Integrationstests für das world-test Modul.

**WICHTIG:** Integration Tests benötigen einen laufenden Server und werden nur mit explizitem Profil ausgeführt!

## Struktur

- `java/` - Java-Sourcecode für Integrationstests
- `resources/` - Ressourcen für Integrationstests (Konfigurationsdateien, Test-Daten, etc.)

## Namenskonventionen

Integrationstests sollten eine der folgenden Namenskonventionen verwenden:
- `*IT.java` (z.B. `UserRegistrationIT.java`)
- `*IntegrationTest.java` (z.B. `UserRegistrationIntegrationTest.java`)

## Ausführung

### Unit Tests (ohne Integration Tests)
```bash
mvn test
mvn clean test
mvn verify
```

### Integration Tests (benötigt laufenden Server!)
```bash
# Nur Integration Tests
mvn verify -P integration-tests

# Unit Tests + Integration Tests
mvn clean verify -P integration-tests

# Oder explizit:
mvn clean test verify -P integration-tests
```

## Voraussetzungen für Integration Tests

Bevor Sie Integration Tests ausführen, stellen Sie sicher, dass:

1. **Der Nimbus Server läuft** (Universe + World Services)
2. **Alle erforderlichen Services verfügbar sind**
3. **Test-Datenbank/Konfiguration ist bereit**

## Konfiguration

- **Maven Profil**: `integration-tests` - aktiviert Integration Test Plugins nur bei expliziter Angabe
- **Surefire Plugin**: Führt nur Unit Tests aus (`src/test/java`)
- **Failsafe Plugin**: Führt Integration Tests aus (`src/integration-test/java`) - nur im Profil verfügbar
- **Compiler Plugin**: Kompiliert Integration Tests in separates Verzeichnis (`target/integration-test-classes`)

Integration Tests werden **niemals** automatisch ausgeführt - sie müssen explizit über das Profil aktiviert werden.

## Best Practices

1. Integration Tests sollten echte externe Systeme testen (Datenbanken, APIs, etc.)
2. Unit Tests bleiben schnell und isoliert im `src/test/java` Verzeichnis
3. Integration Tests können länger dauern und Abhängigkeiten zu externen Services haben
4. Verwenden Sie `@TestMethodOrder` wenn Tests in einer bestimmten Reihenfolge ausgeführt werden müssen
