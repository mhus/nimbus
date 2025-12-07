
# Storage

[x] Erstelle einen MongoStorageService, der Daten in MongoDB speicert.
- Die daten muessen in chunks geteilt werden, ein chunk ist immer 512KB gross. (Konfigurierbar)?
- Es wir deine StorageData Entity genoetigt. 
  - id: ObjectId (automatisch generiert) uuid
  - path: string (eindeutiger Pfad)
  - index: int (index des chunks, beginnend bei 0)
  - data: byte[] (die daten des chunks)
  - final: boolean (gibt an, ob es der letzte chunk ist)
  - size: long (die gesamte groesse der daten, nur im letzten chunk gesetzt)
  - createdAt: Date (erstellungsdatum)
- Es wird pro chunk immer ein StorageData gespeichert. der letzte bekommt das flag final=true und size=<gesamte groesse>
- uuid + index bilden den Primärschlüssel.
- Wenn info geladen wird, dann wird uuid + index + final = true geladen, um die gesamte groesse zu bekommen.
- id wird als storage.id zurueckgegeben.
- warum id: path kann mehrfach verwendet werden, es sind quasi versionen der gleichen datei und die id ist die referenz auf die version.
  Dadurch sind zwischen updates keine luecken moeglich. Alte versionen werden spaeter automatsich geloescht.
- Es wird eine StorageDelete Entity benoetigt:
  - storageId: ObjectId (referenziert die StorageData id)
  - deletedAt: Date (loeschdatum)
- Bei delete und update wird fuer die alte version ein StorageDelete erstellt mit deletAt = jetzt + 5 Minuten.
- Es wird ein scheduler benoetigt, der alle StorageDelete eintraege durchgeht und die referenzierten StorageData loescht. Und die StorageDelete eintraege danach loescht.
- FileStorageService wird deaktiviert
- Es werden Stream implementierungen benoetigt, die die chunks automatisch laden und speichern.
- Wichtig ist, dass die daten nicht in den speicher passen, deshalb muessen die streams die chunks nacheinander laden und speichern.

> [x] Storage with worldId

---

[?] Ist es moeglich in Spring Boot JPA und mongoDB ein Feld in die Entities einzufuegen, das nicht in der
Entity Klasse ist. Ich denke an ein _schema Feld das beim schreiben immer die version des aktuellen Schemas hat.

[?] Wenn eine Entity geladen wird, die nicht dem aktuellen Schema entspricht, dann wird eine Funktion aufgerufen, die
vorerst ein Log ausgibt.

```text
 Beim Speichern (onBeforeConvert):
     1. Entity wird gespeichert via repository.save(entity)
     2. Event Listener wird automatisch aufgerufen
     3. @SchemaVersion Annotation wird aus Entity-Klasse gelesen (gecached)
     4. _schema Feld wird in MongoDB Document eingefügt
     5. Dokument wird in MongoDB gespeichert mit _schema Feld

     Beim Laden (onAfterConvert):
     1. Entity wird geladen via repository.findById(id) oder ähnlich
     2. MongoDB Document wird in Entity konvertiert
     3. Event Listener wird automatisch aufgerufen
     4. _schema aus Document wird mit erwarteter Version aus Annotation verglichen
     5. Bei Unterschied: Warning wird geloggt
     6. Entity wird normal zurückgegeben (keine Exception)
```

[?] Ich brauche in shared ein SchemaMigrationService, der einzelne Entities migrieren kann.
- Es wird auch ein SchemaMigrator Interface benoetigt, das eine Entity von einer Version in die naechste migriert.
- Der SchemaMigrationService sucht die passenden SchemaMigrator Services und mirgerit die Entitaet von der aktuelle Version bis zur letzen Version.
- Wenn kein _shema Feld vorhanden ist, dann wird von Version 0 ausgegangen.
- Dabei wird immer die entity als String uebergeben und kommt als String in der neuen Version zurück.
- Es soll auch die möglichkeit geben, die Entity als String aus mongo zu laden und zu speichern, ohne sie in ein Objekt zu deserialisieren.
- Die SchemaMigratoren muessen als Spring Beans registriert werden und werden in einer Liste im SchemaMigrationService Lazy gehalten.
- Erstelle in world-control ein Command mit dem man einzelne oder alle Entities einer Collection migrieren kann.

```text
 1. RestController in shared (shared/src/main/java/de/mhus/nimbus/shared/api/SchemaMigrationController.java)

  REST API für Schema-Migrationen, verfügbar über /api/schema:

  Endpoints:
  - POST /api/schema/migrate - Migriert Dokumente
    - Request Body: { collectionName, documentId, entityType, targetVersion }
    - documentId kann sein: ID, "*" (alle), oder "no-schema" (ohne Schema)
  - GET /api/schema/stats/{collectionName} - Statistiken über Schema-Versionen
  - GET /api/schema/entity-types - Liste aller Entity-Typen mit Migratoren

  Verwendung:
  curl -X POST http://localhost:8080/api/schema/migrate \
    -H "Content-Type: application/json" \
    -d '{
      "collectionName": "users",
      "documentId": "*",
      "entityType": "UUser",
      "targetVersion": "1.0.0"
    }'

  2. Command in world-control (world-control/src/main/java/de/mhus/nimbus/world/control/commands/MigrateSchemaCommand.java)

  Richtiges Command für das Command-System, implementiert Command Interface:

  Verwendung:
  # Via CommandService
  MigrateSchema users 507f1f77bcf86cd799439011 UUser 1.0.0
  MigrateSchema users * UUser 1.0.0
  MigrateSchema users no-schema UUser 1.0.0

  Features:
  - Implementiert getName(), getHelp(), execute(), requiresSession()
  - Detaillierte Hilfe mit getHelp()
  - Return Codes für verschiedene Fehlerszenarien
  - Kann remote ohne Session aufgerufen werden
```

[?] Export und Import mit Schema Migration
- Erstelle in shared einen ExportService und ImportService.
- Der ExportService exportiert alle Entities einer Collection in eine Datei.
- Der ImportService importiert alle Entities aus einer Datei in die Collection.
- Beim Import wird die Entity mit dem SchemaMigrationService migriert bevor sie gespeichert wird.
- Erstelle in tools ein Modul 'world-export' und 'world-import', hier werden alle World relevanten Collections exportiert und importiert.
  - s_assets, storage_data, worlds, w_bacldrops, w_chunk, w_layer, w_blocktypes, w_items, w_entities, w_entry_models, w_item_positions, w_itemtypes, w_layer_terrain, w_layer_models
  - in application.yaml kann angegeben werden welche Collections exportiert/importiert werden sollen.
  - in application.yaml kann der Pfad der Export/Import Datei angegeben werden.

```text
  Export:
  cd tools/world-export
  mvn spring-boot:run
  # oder mit Custom-Pfad:
  mvn spring-boot:run -Dspring-boot.run.arguments="--export.output-path=/data/exports"

  Import:
  cd tools/world-import
  mvn spring-boot:run
  # oder mit Custom-Pfad:
  mvn spring-boot:run -Dspring-boot.run.arguments="--import.input-path=/data/exports"
```

[?] optional soll auch die worldId in application.yaml angegeben werden, die exportiert werden soll. Alle welten ist '*'
[?] Beim importieren muss geprueft werden ob die entity schon existiert. ein parameter in application.yaml steuert ob die entity skippt oder overwritten wird.
[x] Erstelle SchemaMigrator für
- s_assets, storage_data, worlds, w_bacldrops, w_chunk, w_layer, w_blocktypes, w_items, w_entities, w_entry_models, w_item_positions, w_itemtypes, w_layer_terrain, w_layer_models
- von verion 0 auf version 1.0.0 - ohne änderungen vorzunehmen. - damit geht alles initial auf version 1.0.0
- Kann der Import alternativ aus einer zip datei geladen werden. Wenn der import pfad mit .zip ended.
- Kann der Export alternativ in eine zip datei gespeichert werden. Wenn der export pfad mit .zip ended.

[?] Es wird in SchemaMigrationService ein zweiter Mechanismus benoetigt, mit dem
Objekte aus StorageService migriert werden koennen.
- Es gibt am StorageData die Parameter String schema, String schemaVersion
- Beim speichern wird das schema und die schemaVersion gesetzt.
- Bei StorageInfo wird das schema und die schemaVersion zurueckgegeben.
- Als migration wird der bestehende mechanismus benutzt mit den SchemaMigratoren.
  - Aktuell: migrateToLatest(String entityJson, String entityType) 
  - Hier wird die Schmea Version niht in _schema, sondern separat mitgegeben.
  - migrateToLatest(String entityJson, String entityType, String currentVersion)
- Da StorageService erreichbar ist kann die Migration direkt implementiert werden.
  - migrateStorage(String storageId) soll auf die Latest Version migrieren.
  - Nutze StorageService.replace() um die Daten zu ersetzen, so wird die StorageId nicht veraendert.
  - Lade die Daten als String, migriere sie und speichere sie wieder.
- Erstelle einen REST Knoten im bestehenden SchemaMigrationController um auch Storage Objekte zu migrieren.
- In world-import muss der importer nach dem import von 'storage_data' die Storage Objekte die importiert wurden
  migrieren.
