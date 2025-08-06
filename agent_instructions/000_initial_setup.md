
# Initial Setup Instructions

## 01 Erstelle alle Module und Verzeichnisse

```text
Erstelle die pom.xml Dateien und alle module für die Komponenten, die in der Datei `spec/00_overview.md` beschrieben sind.
Beachte die Anweisungen in der Datei `spec/02_development.md`.
Lege alle nötigen Verzeichnisse und Dateien an. Erstelle noch keine Klassen oder Methoden die darüber hinaus gehen.
```

Perfekt! Ich habe die Maven-Modulstruktur für das Nimbus MMORPG System erfolgreich erstellt. Hier ist eine Zusammenfassung dessen, was erstellt wurde:
Haupt-pom.xml
Erstellt mit Java 21, Spring Boot 3.3.2, und allen notwendigen Abhängigkeiten
Definiert alle 14 Module gemäß der Spezifikation
Erstellte Module:
Shared-Bibliotheken:
shared: Gemeinsame DTOs ohne externe Abhängigkeiten
server-shared: Server-gemeinsame Funktionen mit JWT-Token-Validierung
client-shared: Client-gemeinsame Funktionen
world-shared: World-Service gemeinsame Funktionen mit Kafka und Redis
Zentrale Services:
identity: Benutzeridentitäts-Service mit PostgreSQL und JWT
registry: Weltmetadaten-Service mit PostgreSQL
World Services:
world-data: Feld- und Sprite-Verwaltung mit PostgreSQL und Redis
world-life: Lebensform-Verwaltung mit PostgreSQL
world-physics: Physik-Engine mit Redis
world-bridge: World-Gateway mit WebSockets und Kafka
Bridge Services:
client-bridge: Client-Gateway mit WebSockets und JWT-Authentifizierung
Client:
client: Web-Client mit Thymeleaf und WebSockets
Generatoren:
generator-simple: Weltgenerierungs-Service
Alle Module haben:
Korrekte Maven-Abhängigkeiten basierend auf der Spezifikation
Java-Paketstrukturen (de.mhus.nimbus.*)
Spring Boot Konfiguration wo benötigt
Lombok-Integration
Die Struktur ist jetzt bereit für die Implementierung der spezifischen Klassen und Methoden gemäß den Anforderungen in der Spezifikation.

## 02 Spring Boot 3.5.4

```text
Erbe in der root-Pom.xml von Spring Boot 3.5.4.
Erstelle in allen Modulen ohne `shared` im Namen eine Application-Klasse, die von `SpringBootServletInitializer` erbt.

Beachte die Anweisungen in der Datei `spec/02_development.md`.
```

Perfekt! Ich habe alle geforderten Änderungen erfolgreich umgesetzt:
1. Root-pom.xml aktualisiert
   Die Spring Boot Version wurde von 3.3.2 auf 3.5.4 aktualisiert.
2. Application-Klassen erstellt
   Ich habe für alle Module ohne "shared" im Namen Application-Klassen erstellt, die von SpringBootServletInitializer erben:
   IdentityApplication (identity-Modul)
   RegistryApplication (registry-Modul)
   WorldDataApplication (world-data-Modul)
   WorldLifeApplication (world-life-Modul)
   WorldPhysicsApplication (world-physics-Modul)
   WorldBridgeApplication (world-bridge-Modul)
   ClientBridgeApplication (client-bridge-Modul)
   ClientApplication (client-Modul)
   GeneratorSimpleApplication (generator-simple-Modul)
   Alle Application-Klassen:
   Sind mit @SpringBootApplication annotiert
   Erben von SpringBootServletInitializer (für WAR-Deployment)
   Enthalten eine main-Methode für das Starten als JAR
   Sind im korrekten Package-Namespace platziert
   Die Module mit "shared" im Namen (shared, server-shared, client-shared, world-shared) haben keine Application-Klassen erhalten, da sie als Bibliotheken fungieren und nicht eigenständig ausführbar sind.
   Die Struktur ist nun bereit für die weitere Entwicklung der Services gemäß der Nimbus MMORPG System Spezifikation.

