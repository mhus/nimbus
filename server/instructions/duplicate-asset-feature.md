# Duplicate Asset Feature - Implementation

## Übersicht

Diese Funktion ermöglicht es, ein bestehendes Asset unter einem neuen Pfad zu duplizieren.

## Implementierte Komponenten

### 1. Backend (Java/Spring Boot)

#### REST Endpoint
**Datei:** `world-control/src/main/java/de/mhus/nimbus/world/control/api/EAssetController.java`

**Neuer Endpoint:**
```
POST /editor/user/asset/{regionId}/{worldId}/duplicate
```

**Request Body:**
```json
{
  "sourcePath": "textures/stone.png",
  "newPath": "textures/stone-copy.png"
}
```

**Funktionalität:**
- Nimmt sourcePath und newPath als JSON Body
- Lädt das Quell-Asset via SAssetService
- Validiert dass Quell-Asset existiert (404 wenn nicht)
- Validiert dass neuer Pfad noch nicht existiert (409 Conflict)
- Dupliziert das Asset via `SAssetService.duplicateAsset()`
- Gibt bei Erfolg den neuen Pfad zurück

**Validierungen:**
- regionId und worldId sind nicht leer
- sourcePath ist nicht leer
- newPath ist nicht leer
- sourcePath und newPath sind unterschiedlich
- Quell-Asset existiert (404 wenn nicht)
- Neuer Pfad existiert noch nicht (409 wenn doch)

**Abhängigkeiten:**
- `SAssetService` für Laden und Duplizieren

#### Service-Methode
**Datei:** `world-shared/src/main/java/de/mhus/nimbus/world/shared/world/SAssetService.java`

**Neue Methode:**
```java
@Transactional
public SAsset duplicateAsset(SAsset source, String newPath, String createdBy)
```

**Funktionalität:**
- Lädt Content vom Quell-Asset
- Erstellt neuen SAsset-Eintrag mit neuem Pfad
- Kopiert Metadaten (publicData) vom Original
- Speichert Content in neuem Storage-Eintrag
- Gibt dupliziertes Asset zurück

**Besonderheiten:**
- Deep Copy des Contents via StorageService
- Metadata (publicData) wird kopiert
- Neuer StorageId wird generiert
- Neue createdAt/createdBy Werte

### 2. Frontend (Vue 3/TypeScript)

#### Geänderte Datei
**AssetInfoDialog.vue**

**Neue UI-Komponenten:**

1. **Button "Save as Copy"**
   - Position: Links unten im Asset Info Dialog
   - Icon: Duplikat/Copy-Symbol
   - Disabled während Loading
   - Tooltip: "Save a copy with a new path"

2. **Dialog "Save as Copy"**
   - Eingabefeld für neuen Pfad
   - Auto-Suggestion: Fügt "-copy" vor Dateiendung ein
   - Beispiel: `textures/stone.png` → `textures/stone-copy.png`
   - Validation: Pfad ist erforderlich
   - Error-Anzeige bei Fehlern (404, 409, etc.)
   - Loading-State während Duplizierung
   - Enter-Taste zum Speichern

**Neue State-Variablen:**
- `showDuplicateDialog`: Dialog-Sichtbarkeit
- `newAssetPath`: Eingabefeld für neuen Pfad
- `duplicating`: Loading-State
- `duplicateError`: Fehlermeldung

**Neue Funktionen:**
- `openDuplicateDialog()`: Öffnet Dialog mit Auto-Suggestion
- `closeDuplicateDialog()`: Schließt Dialog und resettet State
- `handleDuplicate()`: Hauptlogik
  - Ruft Backend-API auf (POST /duplicate)
  - Zeigt Erfolgsmeldung bei Erfolg
  - Emittiert 'saved' Event zum Refresh der Liste
  - Schließt Info-Dialog nach erfolgreicher Duplizierung
  - Behandelt Fehler (404, 409, 500)

## API-Kontrakt

### Request
```http
POST /editor/user/asset/{regionId}/{worldId}/duplicate
Content-Type: application/json

{
  "sourcePath": "textures/stone.png",
  "newPath": "textures/stone-copy.png"
}
```

**Beispiel:**
```
POST /editor/user/asset/main/main/duplicate
{
  "sourcePath": "textures/stone.png",
  "newPath": "textures/stone-variant.png"
}
```

### Response (Success)
```json
{
  "path": "textures/stone-variant.png",
  "message": "Asset duplicated successfully"
}
```

### Response (Errors)

**404 - Source not found:**
```json
{
  "error": "source asset not found"
}
```

**409 - Target already exists:**
```json
{
  "error": "asset already exists at new path: textures/stone-variant.png"
}
```

**400 - Validation error:**
```json
{
  "error": "sourcePath and newPath must be different"
}
```

## Workflow

1. **Benutzer öffnet Asset Info Dialog**
   - Klickt auf ein Asset im Asset-Editor
   - Dialog zeigt Asset-Metadaten

2. **Benutzer klickt "Save as Copy" Button**
   - Duplicate-Dialog öffnet sich
   - Neuer Pfad wird automatisch vorgeschlagen (mit "-copy")

3. **Benutzer gibt neuen Pfad ein (oder nutzt Vorschlag)**
   - Format: Vollständiger Pfad inkl. Dateiname
   - Beispiele: `textures/stone-copy.png`, `audio/sounds/step-2.wav`

4. **System dupliziert Asset**
   - Sendet API-Request an Backend
   - Backend lädt Content vom Quell-Asset
   - Backend erstellt neuen Storage-Eintrag
   - Backend kopiert Metadaten
   - Backend speichert neues Asset

5. **Erfolg-Feedback**
   - Dialog schließt sich
   - Alert mit Erfolgsmeldung und neuem Pfad
   - Asset-Liste wird aktualisiert (via 'saved' Event)
   - Info-Dialog schließt sich

## Fehlerbehandlung

### Frontend
- Validierung: Neuer Pfad ist erforderlich
- Anzeige von API-Fehlern im Dialog
- Loading-State verhindert Doppel-Submits
- Auto-Suggestion für neuen Pfad

### Backend
- Validierung aller Parameter (regionId, worldId, sourcePath, newPath)
- Prüfung ob Quell-Asset existiert (404 Not Found)
- Prüfung ob neuer Pfad bereits existiert (409 Conflict)
- Prüfung dass Pfade unterschiedlich sind (400 Bad Request)
- Prüfung dass Quell-Asset enabled ist (IllegalStateException)
- Content wird via StorageService dupliziert
- Detaillierte Fehlermeldungen in Logs

## Testing

### Manuelle Tests
1. Asset im Asset-Editor öffnen
2. "Save as Copy" klicken
3. Neuen Pfad eingeben (oder Vorschlag nutzen)
4. Speichern und verifizieren:
   - Dialog schließt sich
   - Erfolgsmeldung erscheint
   - Asset-Liste wird aktualisiert
   - Neues Asset erscheint in Liste
   - Neues Asset hat gleichen Content
   - Neues Asset hat gleiche Metadaten
   - Neuer Storage-Eintrag wurde erstellt

### Edge Cases
- Leerer neuer Pfad: Validierungsfehler
- Existierender neuer Pfad: 409 Conflict Error
- Gleicher source/new Pfad: 400 Bad Request
- Nicht existierender source Pfad: 404 Not Found
- Disabled source Asset: 500 Internal Server Error
- Netzwerkfehler: Error-Anzeige im Dialog

## Technische Details

### Content Duplizierung
```java
// Load content from source
InputStream sourceContent = loadContent(source);

// Store content in new location
StorageService.StorageInfo storageInfo = storageService.store(
    STORAGE_SCHEMA,
    STORAGE_SCHEMA_VERSION,
    worldId.getId(),
    "assets/" + newPath,
    sourceContent
);
```

**Warum echte Duplizierung?**
- Storage ist isoliert (nicht shared)
- Jedes Asset hat eigenen Storage-Eintrag
- Vermeidet Referenz-Probleme beim Löschen
- Saubere Architektur

### Metadata Kopieren
```java
SAsset duplicate = SAsset.builder()
    .worldId(source.getWorldId())
    .path(newPath)
    .name(extractName(newPath))
    .createdBy(createdBy)
    .enabled(true)
    .publicData(source.getPublicData()) // Copy metadata
    .build();
```

### Auto-Suggestion im Frontend
```typescript
const parts = props.assetPath.split('/');
const fileName = parts[parts.length - 1];
const dir = parts.slice(0, -1).join('/');
const fileNameParts = fileName.split('.');
const ext = fileNameParts.length > 1 ? '.' + fileNameParts.pop() : '';
const baseName = fileNameParts.join('.');
newAssetPath.value = dir ? `${dir}/${baseName}-copy${ext}` : `${baseName}-copy${ext}`;
```

**Beispiele:**
- `textures/stone.png` → `textures/stone-copy.png`
- `audio/step.wav` → `audio/step-copy.wav`
- `file.json` → `file-copy.json`

## Unterschiede zu anderen Duplicate-Features

| Feature | Block as BlockType | Duplicate BlockType | Duplicate Asset |
|---------|-------------------|---------------------|-----------------|
| **Quelle** | Block Instance | BlockType | Asset |
| **Ziel** | Neuer BlockType | BlockType-Kopie | Asset-Kopie |
| **Storage** | N/A | Metadata only | Content + Metadata |
| **Endpoint** | `/fromBlock/{id}` | `/duplicate/{src}/{new}` | `/duplicate` (POST body) |
| **ID Format** | BlockTypeId (string) | BlockTypeId (string) | Path (string) |

## Dateien-Übersicht

### Backend (Java)
- `world-control/src/main/java/de/mhus/nimbus/world/control/api/EAssetController.java` - Duplicate Endpoint
- `world-shared/src/main/java/de/mhus/nimbus/world/shared/world/SAssetService.java` - Duplicate Service-Methode

### Frontend (TypeScript/Vue)
- `client/packages/controls/src/material/components/AssetInfoDialog.vue` - UI und Logik

### Dokumentation
- `server/instructions/duplicate-asset-feature.md` - Diese Datei

## Architektur-Entscheidungen

1. **Endpoint-Design: POST /duplicate mit JSON Body**
   - RESTful: Action-orientiert
   - Pfade im Body statt URL (flexibler)
   - POST weil neue Ressource erstellt wird
   - Paths können komplexe Zeichen enthalten

2. **Echte Content-Duplizierung**
   - Jedes Asset hat eigenen Storage-Eintrag
   - Keine shared References
   - Saubere Architektur
   - Vermeidet Probleme beim Löschen

3. **Metadata Kopieren**
   - publicData wird kopiert
   - Neue createdAt/createdBy
   - Enabled-Status true
   - Name wird aus neuem Pfad extrahiert

4. **Auto-Suggestion im Frontend**
   - Benutzer-freundlich
   - Fügt "-copy" vor Dateiendung ein
   - Behält Verzeichnis-Struktur bei
   - Kann vom Benutzer angepasst werden

5. **Dialog schließt nach Duplizierung**
   - Benutzer sieht aktualisierte Liste
   - Vermeidet Verwirrung
   - Klarer Workflow

## Compliance

- ✅ Clean Code Prinzipien befolgt
- ✅ Keine Hacks, saubere Architektur
- ✅ Echte Content-Duplizierung (kein Sharing)
- ✅ DTOs und Services korrekt verwendet
- ✅ Lombok Annotations genutzt
- ✅ Source Code und Kommentare in Englisch
- ✅ Fehlerbehandlung implementiert
- ✅ Logging vorhanden
- ✅ Backend kompiliert erfolgreich
- ✅ @Transactional für Service-Methode

## Future Enhancements

1. **Batch-Duplizierung**
   - Mehrere Assets auf einmal duplizieren
   - Mit Pfad-Präfix/Suffix

2. **Duplicate mit Modifikationen**
   - Während Kopie Metadaten anpassen
   - z.B. Description ändern

3. **Duplicate in anderen Ordner**
   - Quick-Actions für "Copy to..."
   - Favoriten-Ordner

4. **Version History**
   - Automatische Versionierung
   - z.B. `file-v1.png`, `file-v2.png`

5. **Preview vor Duplizierung**
   - Zeige Content-Vorschau
   - Bestätige Storage-Größe
