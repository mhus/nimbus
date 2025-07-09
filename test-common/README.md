# Test Common Module

Dieses Modul stellt REST API Controller zur Verfügung, um die Funktionen der Client-Beans aus dem 'common' Modul zu testen. Es bietet HTTP-Endpunkte für alle vier Client-Beans: WorldLifeClient, IdentityClient, RegistryClient und WorldVoxelClient.

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
# Login mit Username und Password
curl -X POST "http://localhost:7090/api/test/identity/login" \
  -d "username=testuser" \
  -d "password=password123"
```

```bash
# Login mit zusätzlichen Client-Informationen
curl -X POST "http://localhost:7090/api/test/identity/login" \
  -d "username=testuser" \
  -d "password=password123" \
  -d "clientInfo=WebClient-1.0"
```

#### User Lookup by ID
```bash
# Suche nach einem User anhand der ID
curl -X GET "http://localhost:7090/api/test/identity/user/12345"
```

#### User Lookup by Username
```bash
# Suche nach einem User anhand des Usernamens
curl -X GET "http://localhost:7090/api/test/identity/user/username/testuser"
```

#### User Lookup by Email
```bash
# Suche nach einem User anhand der E-Mail-Adresse
curl -X GET "http://localhost:7090/api/test/identity/user/email/test@example.com"
```

#### Character Lookup by ID
```bash
# Suche nach einem Charakter anhand der ID
curl -X GET "http://localhost:7090/api/test/identity/character/67890"
```

#### Character Lookup by Name
```bash
# Suche nach einem Charakter anhand des Namens
curl -X GET "http://localhost:7090/api/test/identity/character/name/MageWarrior"
```

#### Characters Lookup by User ID
```bash
# Suche nach allen Charakteren eines Users
curl -X GET "http://localhost:7090/api/test/identity/characters/user/12345"
```

#### Public Key Request
```bash
# Anfrage für öffentlichen Schlüssel
curl -X GET "http://localhost:7090/api/test/identity/public-key"
```

### Registry Client - Planet & World Management

#### Planet Registration
```bash
# Registrierung eines Planeten (minimal)
curl -X POST "http://localhost:7090/api/test/registry/planet" \
  -d "planetName=Terra-Alpha"

# Registrierung eines Planeten mit vollständigen Informationen
curl -X POST "http://localhost:7090/api/test/registry/planet" \
  -d "planetName=Terra-Beta" \
  -d "description=A beautiful green planet" \
  -d "galaxy=Milky Way" \
  -d "sector=Alpha-7"
```

#### Planet Lookup
```bash
# Suche nach einem Planeten anhand des Namens
curl -X GET "http://localhost:7090/api/test/registry/planet/Terra-Alpha"
```

#### Planet Unregistration
```bash
# Abmeldung eines Planeten (minimal)
curl -X DELETE "http://localhost:7090/api/test/registry/planet/Terra-Alpha"

# Abmeldung eines Planeten mit Grund
curl -X DELETE "http://localhost:7090/api/test/registry/planet/Terra-Beta?reason=Maintenance"
```

#### World Registration
```bash
# Registrierung einer Welt (minimal)
curl -X POST "http://localhost:7090/api/test/registry/world" \
  -d "worldId=world-001" \
  -d "worldName=Greenlands" \
  -d "planetName=Terra-Alpha" \
  -d "managementUrl=http://localhost:8080/world-001"

# Registrierung einer Welt mit vollständigen Informationen
curl -X POST "http://localhost:7090/api/test/registry/world" \
  -d "worldId=world-002" \
  -d "worldName=Desert Lands" \
  -d "planetName=Terra-Alpha" \
  -d "managementUrl=http://localhost:8080/world-002" \
  -d "description=A vast desert world" \
  -d "worldType=SURVIVAL"
```

#### World Unregistration
```bash
# Abmeldung einer Welt (minimal)
curl -X DELETE "http://localhost:7090/api/test/registry/world/world-001"

# Abmeldung einer Welt mit vollständigen Informationen
curl -X DELETE "http://localhost:7090/api/test/registry/world/world-002?planetName=Terra-Alpha&reason=Shutdown"
```

### WorldVoxel Client - Voxel Management

#### Voxel Deletion
```bash
# Löschung eines Voxels an bestimmten Koordinaten
curl -X DELETE "http://localhost:7090/api/test/world-voxel/voxel/world-001?x=100&y=64&z=200"
```

#### Chunk Operations

##### Chunk Clearing
```bash
# Löschen aller Voxel in einem Chunk
curl -X DELETE "http://localhost:7090/api/test/world-voxel/chunk/world-001?chunkX=5&chunkY=2&chunkZ=10"
```

##### Chunk Loading
```bash
# Laden eines Chunks
curl -X GET "http://localhost:7090/api/test/world-voxel/chunk/world-001?chunkX=5&chunkY=2&chunkZ=10"
```

##### Full Chunk Loading
```bash
# Laden eines vollständigen Chunks (nur nicht-leere Voxel)
curl -X GET "http://localhost:7090/api/test/world-voxel/chunk/world-001/full?chunkX=5&chunkY=2&chunkZ=10&includeEmpty=false"

# Laden eines vollständigen Chunks (einschließlich leerer Voxel)
curl -X GET "http://localhost:7090/api/test/world-voxel/chunk/world-001/full?chunkX=5&chunkY=2&chunkZ=10&includeEmpty=true"
```

## Health Check

```bash
# Überprüfung des Anwendungsstatus
curl -X GET "http://localhost:7090/actuator/health"
```

## Hinweise

- Alle Anfragen sind asynchron und senden Kafka-Nachrichten
- Die Antworten bestätigen nur, dass die Nachricht erfolgreich gesendet wurde
- Für echte Tests sollte eine Kafka-Umgebung laufen
- Die Standard-Kafka-Konfiguration verwendet `localhost:9092`
- Alle Endpunkte loggen die empfangenen Anfragen für Debugging-Zwecke

## Abhängigkeiten

Das Modul nutzt das 'common' Modul und erbt dessen Kafka-Konfiguration und Client-Beans. Stellen Sie sicher, dass folgende Services verfügbar sind:

- Kafka Broker (Standard: localhost:9092)
- Zookeeper (für Kafka)

## Entwicklung

Zur lokalen Entwicklung können Sie die Docker-Compose-Umgebung aus dem `deployment/local` Verzeichnis verwenden:

```bash
cd ../deployment/local
./start-dev-env.sh
```
