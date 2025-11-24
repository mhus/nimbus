# Local Cleanup Tool

CLI Tool zum lokalen Löschen aller Datenbanken (universe, region, world) in MongoDB auf `localhost:27017`.

## Warnung
Es werden komplette Datenbanken gelöscht. Nicht in produktiven Umgebungen verwenden.

## Konfiguration
`src/main/resources/application.properties`:
```
cleanup.mongo.uri=mongodb://localhost:27017
cleanup.mongo.databases=universe,region,world
```

## Build
```
mvn -pl tools/local-cleanup -am clean package
```

## Ausführen
```
java -jar tools/local-cleanup/target/local-cleanup-*.jar
```

Exit-Code 0 bei Erfolg, 1 bei Fehler.

