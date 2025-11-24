# Demo Setup Tool

Ein einmalig durchlaufendes Command-Line Tool zum schnellen Prüfen erreichbarer Server (Universe, Region, World Player, World Editor).

## Features
- Liest konfigurierbare Base-URLs aus `application.properties`
- Führt einfache Health-Checks (`/actuator/health`, `/health`, `/`) durch
- Überspringt nicht konfigurierte Services
- Exit-Code 0 bei Erfolg, 1 wenn mindestens ein konfigurierter Service fehlschlägt

## Konfiguration
Bearbeite `src/main/resources/application.properties`:
```
universe.base-url=http://localhost:9040
region.base-url=http://localhost:9050
world.player.base-url=http://localhost:9060
world.editor.base-url=http://localhost:9070
```

## Build
Im Projektwurzelverzeichnis:
```
mvn -pl tools/demo-setup -am clean install
```

## Ausführen
```
java -jar tools/demo-setup/target/demo-setup-*.jar
```
Oder direkt:
```
mvn -pl tools/demo-setup spring-boot:run
```

## Exit Codes
- 0: Alle konfigurierten Services OK
- 1: Mindestens ein Fehler

## Erweiterungen
- Token-basierte Authentifizierung für geschützte Endpunkte
- Erweiterte spezifische Methoden pro Service
- Retry/Backoff bei Netzwerkfehlern

