# Duplicate BlockType Feature - Implementation

## Übersicht

Diese Funktion ermöglicht es, einen bestehenden BlockType unter einer neuen ID zu duplizieren.

## Implementierte Komponenten

### 1. Backend (Java/Spring Boot)

#### REST Endpoint
**Datei:** `world-control/src/main/java/de/mhus/nimbus/world/control/api/EBlockTypeController.java`

**Neuer Endpoint:**
```
POST /api/worlds/{worldId}/blocktypes/duplicate/{sourceBlockId}
```

**Request Body:**
```json
{
  "newBlockId": "w/456"
}
```

**Wichtig:** Der `{*sourceBlockId}` Wildcard muss am Ende des Pfads stehen (Spring-Limitierung).

**Funktionalität:**
- Nimmt sourceBlockId als URL-Parameter, newBlockId im Body
- Lädt den Quell-BlockType via WBlockTypeService

**Wichtige Änderungen:**
- GET/PUT/DELETE Endpoints geändert zu `/blocktypes/type/{blockId}` um Kollision mit `/duplicate/{blockId}` zu vermeiden
- Frontend (BlockTypeService.ts) entsprechend angepasst
- Erstellt eine Deep Copy der publicData via ObjectMapper
- Setzt die neue ID im kopierten BlockType
- Fügt " (Copy)" zur Description hinzu
- Extrahiert automatisch die blockTypeGroup aus der neuen ID
- Speichert den duplizierten BlockType
- Kopiert den enabled-Status vom Original
- Gibt bei Erfolg die neue blockTypeId zurück

**Validierungen:**
- WorldId ist gültig
- sourceBlockId existiert (404 wenn nicht)
- newBlockId existiert noch nicht (409 Conflict wenn doch)
- sourceBlockId und newBlockId sind unterschiedlich
- Beide IDs sind nicht leer

**Abhängigkeiten:**
- `WBlockTypeService` für Laden und Speichern
- `ObjectMapper` für Deep Copy (kein JSON-Hack!)
- `BlockUtil.extractGroupFromBlockId()` für Group-Extraktion

### 2. Frontend (Vue 3/TypeScript)

#### Geänderte Datei
**BlockTypeEditorPanel.vue**

**Neue UI-Komponenten:**

1. **Button "Save as Copy"**
   - Position: Neben dem "Source"-Button in der Action-Bar
   - Nur sichtbar im Edit-Mode (nicht beim Erstellen)
   - Icon: Duplikat/Copy-Symbol
   - Disabled während Speichervorgängen
   - Tooltip: "Save a copy with a new ID"

2. **Dialog "Save as Copy"**
   - Eingabefeld für neue BlockType-ID
   - Validation: ID ist erforderlich
   - Placeholder: "e.g., custom:my-block or w/123"
   - Error-Anzeige bei Fehlern (z.B. ID existiert bereits)
   - Loading-State während Duplizierung
   - Enter-Taste zum Speichern
   - Cancel-Button zum Abbrechen

**Neue State-Variablen:**
- `showDuplicateDialog`: Dialog-Sichtbarkeit
- `newBlockTypeId`: Eingabefeld für neue ID
- `duplicating`: Loading-State
- `duplicateError`: Fehlermeldung

**Neue Funktionen:**
- `openDuplicateDialog()`: Öffnet Dialog und resettet State
- `closeDuplicateDialog()`: Schließt Dialog und resettet State
- `handleDuplicate()`: Hauptlogik
  - Ruft Backend-API auf
  - Zeigt Erfolgsmeldung bei Erfolg
  - Emittiert 'saved' Event zum Refresh der Liste
  - Schließt Editor nach erfolgreicher Duplizierung
  - Behandelt Fehler (404, 409, 500)

## API-Kontrakt

### Request
```http
POST /api/worlds/{worldId}/blocktypes/duplicate/{sourceBlockId}
Content-Type: application/json

{
  "newBlockId": "w/456"
}
```

**Beispiel:**
```
POST /api/worlds/main/blocktypes/duplicate/w/123
{
  "newBlockId": "w/456"
}
```

### Response (Success)
```json
{
  "blockId": "w/456",
  "message": "BlockType duplicated successfully"
}
```

### Response (Errors)

**404 - Source not found:**
```json
{
  "error": "source blocktype not found"
}
```

**409 - Target already exists:**
```json
{
  "error": "blocktype already exists with id: w/456"
}
```

**400 - Validation error:**
```json
{
  "error": "sourceBlockId and newBlockId must be different"
}
```

## Workflow

1. **Benutzer öffnet BlockType-Editor**
   - Öffnet einen bestehenden BlockType zum Bearbeiten

2. **Benutzer klickt "Save as Copy" Button**
   - Dialog öffnet sich

3. **Benutzer gibt neue BlockType-ID ein**
   - Format: `group:name` oder `group/name`
   - Beispiele: `custom:stone-copy`, `w/456`, `test:my-variant`

4. **System dupliziert BlockType**
   - Sendet API-Request an Backend
   - Backend erstellt Deep Copy
   - BlockTypeGroup wird automatisch extrahiert
   - Description wird mit " (Copy)" erweitert

5. **Erfolg-Feedback**
   - Dialog schließt sich
   - Alert mit Erfolgsmeldung und neuer ID
   - Editor schließt sich
   - BlockType-Liste wird aktualisiert (via 'saved' Event)

## Fehlerbehandlung

### Frontend
- Validierung: Neue BlockType-ID ist erforderlich
- Anzeige von API-Fehlern im Dialog
- Loading-State verhindert Doppel-Submits
- Disabled-State für Cancel/Submit während Duplizierung

### Backend
- Validierung der WorldId
- Validierung beider BlockType-IDs (nicht leer)
- Prüfung ob Quell-BlockType existiert (404 Not Found)
- Prüfung ob neue ID bereits existiert (409 Conflict)
- Prüfung dass IDs unterschiedlich sind (400 Bad Request)
- Deep Copy via ObjectMapper (kein JSON-Hack!)
- Detaillierte Fehlermeldungen in Logs

## Testing

### Manuelle Tests
1. Bestehenden BlockType öffnen
2. "Save as Copy" klicken
3. Neue ID eingeben (z.B. "test:copy1")
4. Speichern und verifizieren:
   - Dialog schließt sich
   - Erfolgsmeldung erscheint
   - Editor schließt sich
   - Neuer BlockType erscheint in Liste
   - Neuer BlockType hat alle Properties vom Original
   - Description hat " (Copy)" Suffix

### Edge Cases
- Leere neue ID: Validierungsfehler
- Existierende neue ID: 409 Conflict Error
- Gleiche source/new ID: 400 Bad Request
- Nicht existierende source ID: 404 Not Found
- Netzwerkfehler: Error-Anzeige im Dialog

## Technische Details

### Deep Copy Implementation
```java
// Create a deep copy of the publicData
BlockType sourcePublicData = source.getPublicData();
BlockType newPublicData = objectMapper.readValue(
    objectMapper.writeValueAsString(sourcePublicData),
    BlockType.class
);
```

**Warum Deep Copy?**
- Vermeidet Referenz-Probleme
- Saubere Architektur (keine Hacks)
- Type-Safety durch ObjectMapper
- Kopiert alle verschachtelten Objekte (Modifiers, etc.)

### Description Update
```java
String originalDescription = newPublicData.getDescription() != null
        ? newPublicData.getDescription()
        : "";
newPublicData.setDescription(originalDescription + " (Copy)");
```

### BlockTypeGroup Extraktion
- Automatisch aus neuer ID extrahiert
- Beispiele:
  - `custom:stone` → Group: `custom`
  - `w/123` → Group: `w`
  - `test:variant` → Group: `test`

## Unterschiede zu "Save as BlockType" Feature

| Feature | Save Block as BlockType | Duplicate BlockType |
|---------|------------------------|---------------------|
| **Quelle** | Custom Block Instance | Bestehender BlockType |
| **Ziel** | Neuer BlockType | Kopie des BlockType |
| **Konvertierung** | Block → BlockType | BlockType → BlockType (Copy) |
| **Anwendungsfall** | Block-Instance als Template speichern | BlockType als Basis für Variante verwenden |
| **Endpoint** | `/fromBlock/{id}` | `/duplicate/{source}/{new}` |
| **Beschreibung** | "Custom block converted to BlockType" | Original + " (Copy)" |

## Dateien-Übersicht

### Backend (Java)
- `world-control/src/main/java/de/mhus/nimbus/world/control/api/EBlockTypeController.java` - Duplicate Endpoint

### Frontend (TypeScript/Vue)
- `client/packages/controls/src/material/components/BlockTypeEditorPanel.vue` - UI und Logik

### Dokumentation
- `server/instructions/duplicate-blocktype-feature.md` - Diese Datei

## Architektur-Entscheidungen

1. **Endpoint-Design: POST /duplicate/{sourceBlockId} mit Body**
   - RESTful: Ressourcen-orientiert
   - "duplicate" zuerst im Pfad (Spring-Limitierung: {*...} muss am Ende sein)
   - Source ID im Pfad mit {*...} Wildcard (am Ende)
   - New ID im Body
   - POST weil neue Ressource erstellt wird
   - Klare Intention durch "duplicate" im Pfad

2. **Deep Copy via ObjectMapper**
   - Keine JSON-String-Manipulation
   - Type-Safety
   - Saubere Architektur
   - Kopiert alle verschachtelten Strukturen

3. **Description mit " (Copy)" Suffix**
   - Benutzer sieht sofort dass es eine Kopie ist
   - Kann nach Speichern angepasst werden
   - Bessere UX

4. **Editor schließt nach Duplizierung**
   - Vermeidet Verwirrung
   - Benutzer sieht aktualisierte Liste
   - Kann dann neue Kopie öffnen und bearbeiten

5. **Button nur im Edit-Mode**
   - Macht keinen Sinn beim Erstellen
   - Reduziert UI-Komplexität
   - Conditional Rendering mit `v-if="!isCreate"`

## Compliance

- ✅ Clean Code Prinzipien befolgt
- ✅ Keine Hacks, saubere Architektur
- ✅ Deep Copy mit ObjectMapper (kein JSON-Hack!)
- ✅ DTOs und Services korrekt verwendet
- ✅ Lombok Annotations genutzt
- ✅ Source Code und Kommentare in Englisch
- ✅ Fehlerbehandlung implementiert
- ✅ Logging vorhanden
- ✅ Backend kompiliert erfolgreich

## Future Enhancements

1. **Batch-Duplizierung**
   - Mehrere BlockTypes auf einmal duplizieren
   - Mit automatischer ID-Generierung

2. **Duplizierung mit Modifikationen**
   - Dialog mit erweiterten Optionen
   - z.B. Status-Modifiers anpassen während Kopie

3. **Import/Export**
   - BlockTypes als JSON exportieren
   - Importierte JSON als neue BlockTypes speichern

4. **Version History**
   - Automatische Versionierung bei Duplizierung
   - z.B. `custom:stone-v1`, `custom:stone-v2`
