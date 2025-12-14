# Save Custom Block as BlockType - Feature Implementation

## Übersicht

Diese Funktion ermöglicht es, einen Custom Block in einen wiederverwendbaren BlockType zu konvertieren und zu speichern.

## Implementierte Komponenten

### 1. Backend (Java/Spring Boot)

#### REST Endpoint
**Datei:** `world-control/src/main/java/de/mhus/nimbus/world/control/api/EBlockTypeController.java`

**Neuer Endpoint:**
```
POST /api/worlds/{worldId}/blocktypes/fromBlock/{blockTypeId}
```

**Funktionalität:**
- Nimmt Custom Block JSON als Payload
- Konvertiert Block-Daten in BlockType-Format
- Speichert den neuen BlockType via WBlockTypeService
- Extrahiert automatisch die blockTypeGroup aus der blockTypeId (z.B. "w" aus "w/123")
- Validiert, dass BlockType noch nicht existiert
- Gibt bei Erfolg die blockTypeId zurück

**Konvertierungslogik:**
- Kopiert `modifiers` vom Block zum BlockType
- Setzt `initialStatus` basierend auf dem Block-Status
- Verwendet ObjectMapper für typsichere Deserialisierung
- Validiert Block-Struktur während der Konvertierung

**Abhängigkeiten:**
- `WBlockTypeService` für Speicherung
- `ObjectMapper` für JSON-Konvertierung
- `BlockUtil.extractGroupFromBlockId()` für Group-Extraktion

### 2. Frontend (Vue 3/TypeScript)

#### Neue Dateien

**1. BlockInstanceEditor_SaveAsBlockType.ts**
- Enthält die Business-Logik für das Speichern
- `saveBlockAsBlockType()`: API-Aufruf zum Backend
- `getBlockTypeEditorUrl()`: Generiert URL zum BlockType-Editor

**2. PATCH_BlockInstanceEditor.md**
- Dokumentation aller notwendigen Änderungen
- Dient als Referenz für manuelle Anpassungen

#### Geänderte Dateien

**BlockInstanceEditor.vue**

**Neue Imports:**
```typescript
import { saveBlockAsBlockType, getBlockTypeEditorUrl as getBlockTypeEditorUrlHelper }
  from './BlockInstanceEditor_SaveAsBlockType';
```

**Neue State-Variablen:**
- `showSaveAsBlockTypeDialog`: Dialog-Sichtbarkeit
- `saveAsBlockTypeDialog`: Dialog-Referenz
- `newBlockTypeId`: Eingabefeld für neue BlockType-ID
- `savingAsBlockType`: Loading-State
- `saveAsBlockTypeError`: Fehlermeldung

**Neue UI-Komponenten:**
1. **Button "Save as BlockType"**
   - Position: Neben dem "Source"-Button in der Action-Bar
   - Icon: Download-Symbol
   - Aktiviert nur wenn Block valide ist
   - Tooltip erklärt Funktionalität

2. **Dialog "Save as BlockType"**
   - Eingabefeld für BlockType-ID (z.B. "custom:my-block" oder "w/123")
   - Validation: ID ist erforderlich
   - Error-Anzeige bei Fehlern
   - Loading-State während Speicherung
   - Enter-Taste zum Speichern

**Neue Funktionen:**
- `openSaveAsBlockTypeDialog()`: Öffnet Dialog und resettet State
- `closeSaveAsBlockTypeDialog()`: Schließt Dialog und resettet State
- `handleSaveAsBlockType()`: Hauptlogik
  - Ruft Backend-API auf
  - Zeigt Erfolgsmeldung mit Link zum BlockType-Editor
  - Öffnet BlockType-Editor in neuem Tab
  - Behandelt Fehler

## API-Kontrakt

### Request
```http
POST /api/worlds/{worldId}/blocktypes/fromBlock/{blockTypeId}
Content-Type: application/json

{
  "position": { "x": 10, "y": 64, "z": 20 },
  "blockTypeId": "0",
  "status": 1,
  "modifiers": {
    "0": {
      "visibility": {
        "shape": 1,
        "textures": { ... }
      }
    }
  },
  "offsets": [0.1, 0.2, 0.3],
  "rotation": { "x": 0, "y": 90 },
  "faceVisibility": 63
}
```

### Response (Success)
```json
{
  "blockId": "w/123",
  "message": "BlockType created successfully"
}
```

### Response (Error)
```json
{
  "error": "blocktype already exists with id: w/123"
}
```

## Workflow

1. **Benutzer bearbeitet Custom Block im Block-Editor**
   - Setzt Modifiers, Offsets, Rotation, etc.

2. **Benutzer klickt "Save as BlockType" Button**
   - Dialog öffnet sich

3. **Benutzer gibt BlockType-ID ein**
   - Format: `group:name` oder `group/name`
   - Beispiele: `custom:stone`, `w/123`, `test:myblock`

4. **System speichert BlockType**
   - Sendet Block-JSON an Backend
   - Backend konvertiert und speichert
   - BlockTypeGroup wird automatisch extrahiert

5. **Erfolg-Feedback**
   - Dialog schließt sich
   - Notification mit Link zum BlockType-Editor
   - BlockType-Editor öffnet sich in neuem Tab

## Fehlerbehandlung

### Frontend
- Validierung: BlockType-ID ist erforderlich
- Anzeige von API-Fehlern im Dialog
- Loading-State verhindert Doppel-Submits

### Backend
- Validierung der WorldId
- Validierung der BlockType-ID (nicht leer)
- Prüfung ob BlockType bereits existiert (409 Conflict)
- Validierung der Block-Struktur (400 Bad Request)
- Fehlerbehandlung bei Konvertierung
- Detaillierte Fehlermeldungen in Logs

## Testing

### Manuelle Tests
1. Custom Block mit Modifiers erstellen
2. "Save as BlockType" klicken
3. Neue ID eingeben (z.B. "test:block1")
4. Speichern und verifizieren:
   - Dialog schließt sich
   - Notification erscheint
   - BlockType-Editor öffnet sich
   - BlockType ist in DB gespeichert

### Edge Cases
- Leere BlockType-ID: Validierungsfehler
- Existierende BlockType-ID: 409 Conflict Error
- Ungültige Block-Struktur: 400 Bad Request
- Netzwerkfehler: Error-Anzeige im Dialog

## Offene Punkte / Future Enhancements

1. **Erweiterung der Konvertierung**
   - Offsets in BlockType-Metadaten speichern?
   - Rotation als Standard-Rotation im BlockType?
   - FaceVisibility berücksichtigen?

2. **UI-Verbesserungen**
   - Vorschau des zu erstellenden BlockType
   - Auto-Suggest für BlockType-IDs
   - Validierung der ID-Format im Frontend

3. **Zusätzliche Features**
   - "Update existing BlockType" Option
   - Batch-Konvertierung mehrerer Blöcke
   - Import/Export von BlockTypes

## Dateien-Übersicht

### Backend (Java)
- `world-control/src/main/java/de/mhus/nimbus/world/control/api/EBlockTypeController.java` - Endpoint und Konvertierungslogik

### Frontend (TypeScript/Vue)
- `client/packages/controls/src/block/views/BlockInstanceEditor_SaveAsBlockType.ts` - Business-Logik
- `client/packages/controls/src/block/views/BlockInstanceEditor.vue` - UI-Komponente (geändert)
- `client/packages/controls/src/block/views/PATCH_BlockInstanceEditor.md` - Dokumentation

### Dokumentation
- `server/instructions/save-block-as-blocktype-feature.md` - Diese Datei

## Architektur-Entscheidungen

1. **Separate TypeScript-Datei für Business-Logik**
   - Trennung von UI und Logik
   - Bessere Testbarkeit
   - Wiederverwendbarkeit

2. **Endpoint-Design: POST /fromBlock/{id}**
   - RESTful: Ressourcen-orientiert
   - Klare Intention durch "fromBlock" im Pfad
   - ID im Pfad statt im Body (RESTful Best Practice)

3. **Konvertierung im Backend**
   - Server ist Source of Truth
   - Validierung auf Server-Seite
   - Type-Safety durch generated Java-Klassen

4. **Automatisches Öffnen des Editors**
   - Bessere UX: Direkter Workflow
   - Benutzer kann sofort weiterarbeiten
   - Optional: Kann leicht deaktiviert werden

## Compliance

- ✅ Clean Code Prinzipien befolgt
- ✅ Keine Hacks, saubere Architektur
- ✅ DTOs und Services korrekt verwendet
- ✅ Lombok Annotations genutzt
- ✅ Source Code und Kommentare in Englisch
- ✅ Fehlerbehandlung implementiert
- ✅ Logging vorhanden
