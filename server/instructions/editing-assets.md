
# Asset Editor

Der Asset Editor zeigt aktuel eine Übersicht-Liste von assets die gesucht werden koennen. Das ist wichtig, aber es wird eine
weitere Ansicht die besser zum bearbeiten in Ordner-Strukturen geeignet ist.

Dazu soll eine Oberflaeche, die aehnlich wie Midnight Commander aussieht.
Rechts und Links eine Liste der Ordner. Die Liste der Dateien separat um die Ordnerliste nicht zu voll zu machen.

+------- +---------+---------+--------+
| Actions| Actions | Actions | Actions|
+------- +---------+---------+--------+
| Ordner | Dateien | Dateien | Ordner |
|        |         |         |        |
...
+------- +---------+---------+--------+
| Info                                |
+-------------------------------------+

- Ordner werden in einer Tree Struktur dargestellt. - entsprechende Funktionen im REST Controller, der die Ordner aus 
  den Assets zu einzelnen strukturen zusammenstellt. Indem es nur den den Path einzeln aus den Assets aus mongodb abfragt.
  z.b. @Query(fields = "{ 'path' : 1 }") ggf mit dem Filter { 'path' : { $regex : 'startsWith.*' } }
- Ordner existieren eigentlich nicht, sondern sind in jedem Asset als Pfad hinterlegt, deshalb:
  - Ordner können nicht umbenannt oder gelöscht werden. - Sie sind nur eine MetaStruktur.
  - Aber sie können ggf. mit mongodb queries angepasst werden in path.
  - An einer Datei kann der Ordner geändert werden
- Ordner können beim anlegen nur in der UI angelegt (pseudo Ordner) werden. Es gibt ja keine Leeren Ordner.
  - Sobald ein Asset in den pseudo ordner gelegt wird, wird dieser Ordner 'existent' da am Asset der Pfad gespeichert ist.
- Dateien sollen per Drag & Drop zwischen links und rechts verschoben werden können.
- Links und rehcts kann eine eigene weltId zugewiesen werden.
- Dateien können heruntergeladen werden.
- Wenn möglich werden Dateien wie in der Übersicht-Liste mit preview angezeigt.
- Dateien sollen in den Browser hinein Drag& Drop verschoben werden und hochgeladen werden, dabei wird der Dateiname übernommen.
- Dateien können über ein formular wie in der Übersicht-Liste hocghgeladen werden.

- es kann ein neuer editor asset-comander-editor dafür angelegt werden

```text
 Midnight Commander-Style Asset Editor - Implementierungsplan

 Zusammenfassung

 Implementierung eines zweispaltige Midnight Commander-ähnlichen Asset-Editors. Links und rechts je ein Panel mit Ordner-Baum und Dateiliste. Drag & Drop zwischen Panels und vom Browser unterstützt.

 Kern-Architektur-Erkenntnis: Ordner existieren nicht in der Datenbank - sie werden aus Asset-Pfaden abgeleitet! Keine Ordner-Dokumente in MongoDB, sondern computed aus Pfad-Präfixen (z.B. textures/block/stone.png → Ordner textures/
 und textures/block/).

 ---
 Phasenübersicht

 User-Entscheidungen:
 - ✅ Phase 2 überspringen (Bulk Path Update später)
 - ✅ In-Memory-Verarbeitung für Ordner-Extraktion (einfach, bis ~50k Assets)
 - ✅ Neue eigenständige View (McAssetEditor.vue)
 - ✅ MVP: Alle Features (Ordner-Baum, Dateiliste, Drag & Drop, Actions)

 Phase 1: Backend - Virtual Folder Tree API ⭐ START HERE

 Erstelle REST-Endpoint für Ordnerstruktur aus Asset-Pfaden.
 Strategie: In-Memory-Verarbeitung im Service

 Phase 2: Backend - Bulk Path Update API

 ⚠️ ÜBERSPRUNGEN - Später implementieren wenn Bedarf besteht.

 Phase 3: Frontend - Komponenten-Struktur

 MC-Layout mit wiederverwendbaren Komponenten erstellen.
 Integration: Neue eigenständige View McAssetEditor.vue

 Phase 4: Frontend - Folder Tree Integration

 Ordner-API einbinden mit Lazy-Loading.

 Phase 5: Frontend - File List Integration

 Asset-Liste nach Ordner-Pfad filtern.

 Phase 6: Frontend - Drag & Drop

 Drag & Drop zwischen Panels und vom Browser.

 Phase 7: Frontend - Actions & Info Bar

 Action-Buttons und Info-Anzeige.

 Phase 8: Testing & Polish

 Tests, Performance, UX-Verbesserungen.

 ---
 Phase 1: Backend - Virtual Folder Tree API

 1.1 Ordner-Extraktion in SAssetService

 Datei: world-shared/src/main/java/de/mhus/nimbus/world/shared/world/SAssetService.java

 Neue Methode hinzufügen:
 /**
  * Extract unique folder paths from assets in a world.
  * Folders are virtual - derived from asset paths.
  *
  * @param worldId The world identifier
  * @param parentPath Optional parent path filter (e.g., "textures/")
  * @return List of folder paths with metadata
  */
 public List<FolderInfo> extractFolders(WorldId worldId, String parentPath) {
     // 1. Query all assets (mit path projection für Performance)
     List<SAsset> assets = repository.findByWorldId(worldId.getWorldId());

     // 2. Ordnerpfade extrahieren
     Map<String, FolderStats> folderMap = new HashMap<>();

     for (SAsset asset : assets) {
         Set<String> folders = extractFolderPathsFromAssetPath(asset.getPath());
         for (String folder : folders) {
             // Filter nach parentPath
             if (parentPath != null && !folder.startsWith(parentPath)) continue;

             folderMap.computeIfAbsent(folder, k -> new FolderStats())
                      .incrementAssetCount();
         }
     }

     // 3. In FolderInfo DTOs konvertieren
     return folderMap.entrySet().stream()
         .map(entry -> buildFolderInfo(entry.getKey(), entry.getValue()))
         .sorted(Comparator.comparing(FolderInfo::path))
         .toList();
 }

 private Set<String> extractFolderPathsFromAssetPath(String assetPath) {
     Set<String> folders = new HashSet<>();
     String[] parts = assetPath.split("/");
     StringBuilder current = new StringBuilder();

     // Nur bis vorletztem Teil (letzter ist Dateiname)
     for (int i = 0; i < parts.length - 1; i++) {
         if (i > 0) current.append("/");
         current.append(parts[i]);
         folders.add(current.toString());
     }

     return folders;
 }

 private FolderInfo buildFolderInfo(String path, FolderStats stats) {
     String name = path.contains("/")
         ? path.substring(path.lastIndexOf("/") + 1)
         : path;
     String parentPath = path.contains("/")
         ? path.substring(0, path.lastIndexOf("/"))
         : "";

     return new FolderInfo(
         path,
         name,
         stats.getAssetCount(),
         stats.getTotalAssetCount(),
         stats.getSubfolderCount(),
         parentPath
     );
 }

 @Data
 private static class FolderStats {
     private int assetCount = 0;
     private int totalAssetCount = 0;
     private int subfolderCount = 0;

     void incrementAssetCount() { assetCount++; totalAssetCount++; }
 }

 1.2 FolderInfo DTO erstellen

 Neue Datei: world-shared/src/main/java/de/mhus/nimbus/world/shared/world/FolderInfo.java

 package de.mhus.nimbus.world.shared.world;

 /**
  * Metadata for a virtual folder derived from asset paths.
  */
 public record FolderInfo(
     String path,           // Full path: "textures/block"
     String name,           // Just name: "block"
     int assetCount,        // Number of direct assets in this folder
     int totalAssetCount,   // Including subfolders
     int subfolderCount,    // Number of immediate subfolders
     String parentPath      // Parent folder: "textures"
 ) {}

 1.3 REST Endpoint in WorldAssetController

 Datei: world-control/src/main/java/de/mhus/nimbus/world/control/api/WorldAssetController.java

 Neue Methode nach der duplicate-Methode hinzufügen:

 /**
  * Get virtual folder tree for a world.
  * GET /control/worlds/{worldId}/assets/folders?parent=textures/
  */
 @GetMapping("/folders")
 @Operation(summary = "Get folder tree for world",
            description = "Returns folders derived from asset paths. Folders don't exist in DB.")
 @ApiResponses({
     @ApiResponse(responseCode = "200", description = "Success"),
     @ApiResponse(responseCode = "404", description = "World not found")
 })
 public ResponseEntity<?> getFolders(
     @Parameter(description = "World identifier")
     @PathVariable String worldId,

     @Parameter(description = "Parent path filter (optional)")
     @RequestParam(required = false) String parent) {

     log.debug("GET folders: worldId={}, parent={}", worldId, parent);

     var wid = WorldId.of(worldId).orElseThrow(
         () -> new IllegalStateException("Invalid worldId: " + worldId)
     );

     List<FolderInfo> folders = assetService.extractFolders(wid, parent);

     return ResponseEntity.ok(Map.of(
         "folders", folders,
         "count", folders.size(),
         "parent", parent != null ? parent : ""
     ));
 }

 1.4 Unit Tests

 Neue Datei: world-shared/src/test/java/de/mhus/nimbus/world/shared/world/SAssetServiceFolderTest.java

 Test-Cases:
 - Ordner aus flacher Asset-Liste extrahieren
 - Verschachtelte Hierarchie (3+ Ebenen)
 - Filter nach parentPath
 - Assets ohne Ordner (im Root)
 - Leere World → leere Ordner-Liste

 1.5 Integration Tests

 Neue Datei: world-control/src/integration-test/java/de/mhus/nimbus/world/control/api/WorldAssetControllerFolderIT.java

 ---
 Phase 3: Frontend - Komponenten-Struktur

 Technologie-Stack

 - Framework: Vue 3 + TypeScript
 - Styling: TailwindCSS + DaisyUI
 - HTTP: Axios via ApiService
 - Package: @nimbus/controls bei /Users/hummel/sources/mhus/nimbus-server/client/packages/controls

 3.1 Main MC Editor Component

 Neue Datei: client/packages/controls/src/material/views/McAssetEditor.vue

 Layout:
 <template>
   <div class="mc-asset-editor">
     <!-- Actions Bar -->
     <div class="actions-bar">
       <ActionBar panel="left" />
       <ActionBar panel="right" />
     </div>

     <!-- Dual Panels -->
     <div class="panels-container">
       <AssetPanel
         v-model:world-id="leftWorldId"
         v-model:current-path="leftCurrentPath"
         panel="left"
       />
       <AssetPanel
         v-model:world-id="rightWorldId"
         v-model:current-path="rightCurrentPath"
         panel="right"
       />
     </div>

     <!-- Info Bar -->
     <InfoBar :selected-asset="selectedAsset" />
   </div>
 </template>

 <style scoped>
 .panels-container {
   display: grid;
   grid-template-columns: 1fr 1fr;
   gap: 1rem;
 }
 </style>

 3.2 AssetPanel Component (wiederverwendbar)

 Neue Datei: client/packages/controls/src/material/components/AssetPanel.vue

 Split-View: 30% Ordner-Baum, 70% Dateiliste

 3.3 FolderTree Component

 Neue Datei: client/packages/controls/src/material/components/FolderTree.vue

 Features:
 - Hierarchische Baumstruktur
 - Lazy-loading bei expand
 - Pseudo-Ordner-Erstellung (UI-only)

 3.4 FileList Component

 Neue Datei: client/packages/controls/src/material/components/FileList.vue

 Features:
 - Grid/List-Ansicht
 - Multi-Select
 - Preview
 - Pagination

 3.5 ActionBar Component

 Neue Datei: client/packages/controls/src/material/components/ActionBar.vue

 Buttons: Upload, Download, Delete, Move/Copy, Create Folder, Refresh

 3.6 InfoBar Component

 Neue Datei: client/packages/controls/src/material/components/InfoBar.vue

 Display: Metadaten, Größe, MIME-Type, Modified-Date

 ---
 Phase 4: Frontend - Folder Tree Integration

 4.1 AssetService erweitern

 Datei: client/packages/controls/src/services/AssetService.ts

 Neue Methoden:
 async getFolders(worldId: string, parent?: string): Promise<FolderListResponse>
 async moveFolder(worldId: string, oldPath: string, newPath: string): Promise<void>

 4.2 useFolders Composable

 Neue Datei: client/packages/controls/src/composables/useFolders.ts

 State-Management für Ordner-Baum:
 - folders, folderTree, loading, error
 - pseudoFolders (nicht persistiert)
 - loadFolders(), createPseudoFolder(), removePseudoFolder()

 ---
 Phase 5: Frontend - File List Integration

 5.1 useAssets Composable erweitern

 Datei: client/packages/controls/src/composables/useAssets.ts

 Filter nach folderPath:
 const query = options?.folderPath
   ? `^${escapeRegex(options.folderPath)}/[^/]+$`  // Nur direkte Kinder
   : searchQuery.value;

 ---
 Phase 6: Frontend - Drag & Drop

 6.1 File Dragging (innerhalb App)

 FileList: handleDragStart mit dataTransfer

 6.2 Asset Move/Copy

 async moveAsset(sourceWorldId, sourcePath, targetWorldId, targetPath, copy) {
   if (copy) {
     // PATCH /duplicate
   } else {
     // Duplicate + Delete
   }
 }

 6.3 Browser File Drops

 const handleFileUpload = async (files: FileList) => {
   for (const file of files) {
     await assetService.uploadAsset(worldId, `${currentPath}/${file.name}`, file);
   }
 };

 ---
 Phase 7: Frontend - Actions & Info Bar

 Action-Buttons implementieren

 - Upload (File Picker)
 - Download (neue Tabs)
 - Delete (mit Confirm)
 - Copy to Other Panel
 - Create Pseudo-Folder

 Info-Bar

 - Metadaten anzeigen
 - Formatierungs-Helfer (formatSize, formatDate)

 ---
 Phase 8: Testing & Polish

 Backend Tests

 - Unit: SAssetServiceFolderTest
 - Integration: WorldAssetControllerFolderIT
 - Performance: 10k+ assets

 Frontend Tests

 - Component: Jest + Vue Test Utils
 - E2E: Optional

 Error Handling

 - Backend: 404, 500, Validierung
 - Frontend: ErrorAlert, Retry, Timeouts

 Performance

 - Backend: Caching, Indexes
 - Frontend: Virtual Scrolling, Debounce, Lazy-Load

 UX Polish

 - Keyboard Shortcuts
 - Loading Skeletons
 - Animations
 - Dark Mode

 ---
 Kritische Dateien

 Backend (zu modifizieren)

 1. world-shared/.../SAssetService.java - extractFolders()
 2. world-control/.../WorldAssetController.java - /folders endpoint
 3. world-shared/.../SAssetRepository.java - Optional: Path projection query

 Backend (neu erstellen)

 4. world-shared/.../FolderInfo.java - DTO

 Frontend (zu modifizieren)

 5. client/packages/controls/src/services/AssetService.ts - Folder-Methoden
 6. client/packages/controls/src/composables/useAssets.ts - Folder-Filter

 Frontend (neu erstellen)

 7. client/packages/controls/src/composables/useFolders.ts
 8. client/packages/controls/src/material/views/McAssetEditor.vue
 9. client/packages/controls/src/material/components/AssetPanel.vue
 10. client/packages/controls/src/material/components/FolderTree.vue
 11. client/packages/controls/src/material/components/FileList.vue
 12. client/packages/controls/src/material/components/ActionBar.vue
 13. client/packages/controls/src/material/components/InfoBar.vue

 ---
 Implementierungs-Reihenfolge

 Phase 1 (Backend Folder API) ⭐
   ↓
 Phase 3 (Frontend Struktur) - Neue View
   ↓
 Phase 4 (Folder Tree) - In-Memory
   ↓
 Phase 5 (File List)
   ↓
 Phase 6 (Drag & Drop) - MVP Feature
   ↓
 Phase 7 (Actions & Info) - MVP Feature
   ↓
 Phase 8 (Testing & Polish)

 Phase 2 (Bulk Update) → ⚠️ ÜBERSPRUNGEN

 MVP Scope: Alle Phasen außer Phase 2 werden implementiert.

 ---
 Risiken & Mitigation

 Hohe Risiken

 1. Ordner Umbenennen/Verschieben (Phase 2)
   - Mitigation: Phase 2 überspringen, später implementieren
 2. Performance bei 50k+ Assets
   - Mitigation: MongoDB Aggregation, Caching, Pagination
 3. Drag & Drop Cross-Panel
   - Mitigation: Reactive Refs, Optimistic Updates

 Mittlere Risiken

 4. Pseudo-Ordner State
   - Mitigation: Visual distinction, localStorage backup
 5. Große File Uploads
   - Mitigation: Size limits, Progress indicators

 ---
 Aufwandsschätzung

 Angepasst basierend auf User-Entscheidungen:

 | Phase   | Backend | Frontend | Testing | Total        |
 |---------|---------|----------|---------|--------------|
 | Phase 1 | 4h      | -        | 2h      | 6h           |
 | Phase 2 | -       | -        | -       | ÜBERSPRUNGEN |
 | Phase 3 | -       | 6h       | 1h      | 7h           |
 | Phase 4 | -       | 4h       | 1h      | 5h           |
 | Phase 5 | -       | 3h       | 1h      | 4h           |
 | Phase 6 | -       | 6h       | 2h      | 8h           |
 | Phase 7 | -       | 4h       | 1h      | 5h           |
 | Phase 8 | 2h      | 3h       | 5h      | 10h          |
 | Total   | 6h      | 26h      | 13h     | 45h          |

 Optimierungen durch Entscheidungen:
 - In-Memory-Verarbeitung reduziert Phase 1 Komplexität
 - Eigenständige View vermeidet Refactoring von AssetEditor.vue
 - Phase 2 später = -5h initial

 ---
 Nächste Schritte

 Implementierungs-Start (Phase 1)

 ⭐ START HERE:

 1. Backend Folder Tree API (~6h):
   - SAssetService.extractFolders() - Ordner aus Asset-Pfaden extrahieren
   - FolderInfo.java - DTO für Ordner-Metadaten
   - WorldAssetController.getFolders() - REST Endpoint /folders
   - Unit Tests + Integration Tests
 2. Frontend Komponenten-Struktur (~7h):
   - McAssetEditor.vue - Neue eigenständige View
   - AssetPanel.vue, FolderTree.vue, FileList.vue
   - ActionBar.vue, InfoBar.vue
 3. Integration & Features (~17h):
   - Folder Tree mit API verbinden (Lazy-Loading)
   - File List mit Ordner-Filter
   - Drag & Drop (zwischen Panels + vom Browser)
   - Actions (Upload, Download, Delete, Create Folder)
 4. Testing & Polish (~10h):
   - Backend/Frontend Tests
   - Error Handling
   - Performance Optimierung
   - UX Polish (Keyboard Shortcuts, Loading States)

 Wichtige Architektur-Entscheidungen

 ✅ In-Memory-Verarbeitung: Alle Asset-Pfade laden, im Service verarbeiten
 ✅ Eigenständige View: Neue McAssetEditor.vue, kein Refactoring von AssetEditor.vue
 ✅ MVP: Alle Features: Ordner-Baum, Dateiliste, Drag & Drop, Actions
 ✅ Phase 2 später: Bulk Path Update für Ordner-Umbenennung übersprungen

 Technische Hinweise

 Backend (Java/Spring Boot):
 - Clean Architecture: Controller → Service → Repository
 - MongoDB Queries mit Pagination
 - Lombok Annotations
 - Unit Tests (JUnit 5) + Integration Tests (mvn verify)

 Frontend (Vue 3 + TypeScript):
 - Composition API mit Composables
 - TailwindCSS + DaisyUI für Styling
 - Axios via ApiService für HTTP
 - Package: @nimbus/controls

 Ordner-Konzept:
 - Ordner existieren NICHT in MongoDB
 - Werden aus Asset-Pfaden abgeleitet (textures/block/stone.png → Ordner textures/, textures/block/)
 - Pseudo-Ordner in UI = nur lokaler State bis erstes Asset hinzugefügt
```
