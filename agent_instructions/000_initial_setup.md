
# Initial Setup Instructions

## 01 Erstelle alle Module und Verzeichnisse

```text
Erstelle die pom.xml Dateien und alle module für die Komponenten, die in der Datei `spec/00_overview.md` beschrieben sind.
Beachte die Anweisungen in der Datei `spec/02_development.md`.
Lege alle nötigen Verzeichnisse und Dateien an. Erstelle noch keine Klassen oder Methoden die darüber hinaus gehen.
```
```text
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
```

## 02 Spring Boot 3.5.4

```text
Erbe in der root-Pom.xml von Spring Boot 3.5.4.
Erstelle in allen Modulen ohne `shared` im Namen eine Application-Klasse, die von `SpringBootServletInitializer` erbt.

Beachte die Anweisungen in der Datei `spec/02_development.md`.
```

```text
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
```

## 03 Spring Boot 3.5.4 als parent pom.xml

Hat im Schritt 02 als BOM eingebunden, deshalb nochmal.

```text
Stelle die root-Pom.xml so ein, dass sie von Spring Boot 3.5.4 erbt.
Stelle module pom.xml Dateien gegebenenfalls um.
```

```text
Perfect! I have successfully configured the root pom.xml to inherit from Spring Boot 3.5.4 and made the necessary adjustments. Here's what I accomplished:
Changes Made:
1. Root pom.xml Configuration
Added Spring Boot Parent: The root pom.xml now inherits from spring-boot-starter-parent version 3.5.4
Removed redundant dependency management: Removed the Spring Boot BOM from dependency management since it's now inherited from the parent
Updated properties: Changed from maven.compiler.source/target to java.version property (Spring Boot standard)
Simplified plugin management: Removed the Spring Boot Maven plugin version since it's now managed by the parent
2. Module Dependencies Fix
Fixed duplicate H2 dependency: Removed the duplicate H2 database dependency in the identity module that was causing Maven warnings
3. Validation Results
The Maven validation confirms that:
✅ All 14 modules build successfully
✅ Spring Boot 3.5.4 parent is properly downloaded and integrated
✅ No more dependency warnings
✅ All module pom.xml files correctly inherit from the nimbus parent, which now inherits from Spring Boot
Key Benefits:
Automatic dependency management: Spring Boot manages versions for all Spring and common dependencies
Consistent plugin versions: Maven plugins are managed by Spring Boot parent
Simplified configuration: Less boilerplate in the root pom.xml
Better Spring Boot integration: Full access to Spring Boot features and conventions
The configuration is now ready and all modules are properly set up to use Spring Boot 3.5.4 with Java 21.
```

## 04 local compose file

```text
Erstelle eine Datei `docker-compose.local.yml` im Verzeichnis `deployment/local` des Projekts.
Diese Datei soll die lokale Entwicklungsumgebung mit Docker Compose konfigurieren.
Die Datei soll folgende Services enthalten:
- Eine PostgreSQL-Datenbank für alle Komponenten
- Ein Redis-Cache für alle Komponenten
- Ein Kafka Services für alle Komponenten
- Ein Kafka UI Service
```

```text
Perfect! I have successfully created the Docker Compose configuration for your local development environment. Here's what I've set up:
Created Files
deployment/local/docker-compose.local.yml - The main Docker Compose configuration
deployment/local/init-db.sql - Database initialization script
Services Configuration
The Docker Compose file includes all the requested services:
PostgreSQL Database
Container: nimbus-postgres (PostgreSQL 15 Alpine)
Port: 5432
Credentials: User nimbus, Password nimbus123
Databases: Automatically creates separate databases for each component:
identity - for the identity service
registry - for the registry service
world_data - for world data management
world_life - for life form management
Persistent storage with Docker volume
Redis Cache
Container: nimbus-redis (Redis 7 Alpine)
Port: 6379
Configuration: Persistence enabled with AOF (Append Only File)
Persistent storage with Docker volume
Kafka Service
Container: nimbus-kafka (Confluent Platform 7.4.0)
Ports: 9092 (external), 9093 (internal)
Configuration: Single broker setup with auto-topic creation enabled
Dependencies: Includes Zookeeper as required dependency
Persistent storage with Docker volume
Kafka UI Service
Container: nimbus-kafka-ui (Provectus Kafka UI)
Port: 8080
Features: Web interface to manage and monitor Kafka topics, messages, and consumers
Usage
To start the local development environment:
cd deployment/local
docker-compose -f docker-compose.local.yml up -d
To stop the services:
docker-compose -f docker-compose.local.yml down
Access Points
PostgreSQL: localhost:5432
Redis: localhost:6379
Kafka: localhost:9092
Kafka UI: http://localhost:8080
All services are connected via a custom Docker network (nimbus-network) and configured for development use with appropriate restart policies.
```

