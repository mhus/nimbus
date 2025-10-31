
# Material Editor implemetation

Der Material Editor ist eine Webapplikation mit

TypeScript + Vite + Vue3 + tailwind css

Er erlaubt es Block Typen, Texturen, Assets zu erstellen, zu bearbeiten und zu speichern.

## Server

[x] Im Server werden REST Endpunkte benötigt um Block Typen zu bearbeiten
die über die Definition in 'client/instructions/client_2.0/server_rest_api.md' hinausgehen.
- GET /api/worlds/{worldId}/blocktypes?query={query}
- POST /api/worlds/{worldId}/blocktypes
Response: {id: number}
- PUT /api/worlds/{worldId}/blocktypes/{blockTypeId}
- DELETE /api/worlds/{worldId}/blocktypes/{blockTypeId}

- Erstelle die Endpunkte
- Erweitere die Dokumentation in 'client/instructions/client_2.0/server_rest_api.md'

[x] Im Server werden REST Endpunkte benötigt um Assets zu bearbeiten
die über die Definition in 'client/instructions/client_2.0/server_rest_api.md' hinausgehen.
- GET /api/worlds/{worldId}/assets?query={query}
- POST /api/worlds/{worldId}/assets/{neuerAssetPath}
- PUT /api/worlds/{worldId}/assets/{assetPath}
- DELETE /api/worlds/{worldId}/assets/{assetPath}

## Setup

[x] Erstelle ein neues Packet material_editor das auch die sourcen in 'shared' nutzt.
- Richte Vite mit Vue3 und tailwind css ein.
- Erstelle die Grundstruktur der Applikation mit den benötigten Komponenten.
- In einer dotnet Datei wird die aktuelle API_URL fest hinterlegt.
- Rest API Dokumentation: 'client/instructions/client_2.0/server_rest_api.md'
- Im Header steht die aktuelle 'World', diese ist aktuell immer 'main' (in dotnet Datei WORLD_ID) und kann aber durch click geändert werden.
  - Bei Click wird im Server 'GET /api/worlds' aufgerufen und die worlds angezeigt. 
  - Die ausgewahlte World wird übernommen.
- Es werden zwei Teile benötigt (Tabs?): Block Type Editor und Texture + Asset Editor. 
- Block Type Editor:
  - Liste der Block Types (Block Types) mit Suchfunktion
  - Button um neuen Block Type zu erstellen und zu löschen.
  - Auswahl eines Block Types öffnet den Editor
  - DTO ist in 'client/packages/shared/src/types/BlockType.ts' und 'client/packages/shared/src/types/BlockModifier.ts'
  - Die Parameter können bearbeite und wieder auf 'null' gestellt werden.
  - Es können mehrere 'status' Einträge erstellt und bearbeitet werden. Die Editoren werden dann untereinander in einer Liste angezeigt.
- Texture + Asset Editor:
  - Liste der Textures und Assets mit Suchfunktion, wenn ein bekanntes Format (Bilddatei, png, ...) dann Preview anzeigen
  - Button um neue Textures und Assets zu erstellen und zu Löschen.
  - Es gibt keinen richtigen Editor, da es sich um Binärdaten handelt.

```text
  🚀 Anwendung starten:

  # Von Root-Verzeichnis aus:
  cd /Users/hummel/sources/mhus/nimbus/client

  # Material Editor starten (Port 3002):
  pnpm dev:material_editor

  # Oder direkt im Package:
  cd packages/material_editor
  pnpm dev

  🔧 Build für Production:

  # Von Root:
  pnpm build:material_editor

  # Oder direkt:
  cd packages/material_editor
  pnpm build

  📝 Konfiguration:

  Die Datei /packages/material_editor/.env.local enthält:
  - VITE_API_URL=http://localhost:3000 (Server URL)
  - VITE_WORLD_ID=test-world-1 (Default World)
```

## Block Type Editor

[?] Das Editieren der status Sections soll besser untertuetzt werden.
- Du kannst einen neuen Dialog aufmachen im einene Status zu bearbeiten.
- Referenz in 'client/packages/shared/src/types/BlockModifier.ts'
- Die Bereiche sollen ausklappbar sein. Sind sie eingeklappt, wird das optionale Feld als Empty gespeichert (Daten gehen verloren) - Oder noch schöner, neben dem Titel des bereichs ist eine checkbox, die steuert ob der Bereich dann empty wird.
- Erstelle einen Bereich für 'visibility' und Biete Elemente an zum Bearbeiten (chckboxen, textfelder, dropdowns, ...)
  - Die IDs für Texturen sind normiert, siehe Type Docu 'TextureKey'
  - Texturen koennen zwei moeglichkeiten haben: Entweder direkt der Pfad oder ein Objekt mit vuModifier, etc (siehe TextureDefinition)
- Erstelle einen Bereich für 'physics' und Biete Elemente an zum Bearbeiten (chckboxen, textfelder, dropdowns, ...)
- Erstelle einen Bereich für 'wind'.
- Erstelle ein Dropdown für 'effect'
- Erstelle ein textFeld für 'spriteCount'
- Erstelle ein textFeld für 'alpha'
- Erstelle einen Bereich für 'illumination'
- Erstelle einen bereich für 'sound'

[x] Bei texture noch einen Button mit einem Zauberstab: Dann geht ein Dialog auf mit einer Suche nach 
Assets (siehe Asset Editor). Der pfad wird übernommen.

[x] offsets anzeigen und bearbeiten mit verschiedenen Bezeichnern.
- invisible: keine
- cube: Alle ecken (XYZ) 'top front left', 'top front right', 'top back left', 'top back right', 'bottom front left', 'bottom front right', 'bottom back left', 'bottom back right'
- column: Erste Punkt: XZ ist Radius offest oben, Zweiter Punkt XZ ist Radius Offset Unten, dritter Punkt (XYZ) ist verschiebung Oben, vierter Punkt ist verschiebung Unten
- sphere: Erster Punkt (XYZ) ist radios offset, Zweiter Punkt (XYZ) ist verschiebung
- hash: Alle Punkte (XYZ) wie bei cube
- cross: Alle Punkte (XYZ) wie bei cube
- flat: Die Vier ersten Punkte: 'front left', 'front right', 'back left', 'back right'
- glass_flat: wie flat
Alle anderen erstmal keine offsets.

[x] Bei 'Add Status' muss man die status Id angeben koennen. / Alternativ kann man die id durch click auf das 'status 0' label aendern.

===

Es gibt den material_editor der auch BlockModifier editieren kann. Wenn ich nun einen einzelnen Block bearbeiten moechte, ist dort auch der BlockModifier drinn, ich koennte also den gleichen editor benutzen. Aber dann in einem anderen
context. Ich wuerde beim aufruf der website world=wordId und block=x,y,z mitgeben und material_editor sollte sich dann anders verhalten (Block aus rest lesen, Block Editor incl BlockModifier), ist das sinnvoll und moeglich? 

Plan:
```text
Material Editor: Block-Instanz-Modus hinzufügen                                                                                                                                               │
     │                                                                                                                                                                                               │
     │ Übersicht                                                                                                                                                                                     │
     │                                                                                                                                                                                               │
     │ Erweitere den Material Editor um einen zweiten Modus zum Bearbeiten einzelner Block-Instanzen. URL-Parameter steuern das Verhalten: ?world={worldId}&block={x},{y},{z} aktiviert Block-Modus. │
     │                                                                                                                                                                                               │
     │ Phase 1: URL-Parameter-Routing & Mode Detection                                                                                                                                               │
     │                                                                                                                                                                                               │
     │ 1.1 Router-Composable erstellen                                                                                                                                                               │
     │                                                                                                                                                                                               │
     │ src/composables/useRouter.ts:                                                                                                                                                                 │
     │ - Parse URL-Parameter (world, block)                                                                                                                                                          │
     │ - Detect mode: 'blocktype' oder 'block'                                                                                                                                                       │
     │ - Expose: mode, blockCoordinates, currentWorldFromUrl                                                                                                                                         │
     │                                                                                                                                                                                               │
     │ 1.2 App.vue anpassen                                                                                                                                                                          │
     │                                                                                                                                                                                               │
     │ - Import useRouter                                                                                                                                                                            │
     │ - Zeige Header nur im BlockType-Modus                                                                                                                                                         │
     │ - Im Block-Modus: Zeige Block-Koordinaten statt Tabs                                                                                                                                          │
     │                                                                                                                                                                                               │
     │ 1.3 TabNavigation anpassen                                                                                                                                                                    │
     │                                                                                                                                                                                               │
     │ - Nur rendern wenn mode === 'blocktype'                                                                                                                                                       │
     │ - Im Block-Modus: Zeige BlockInstanceEditor direkt                                                                                                                                            │
     │                                                                                                                                                                                               │
     │ Phase 2: Block REST API Services                                                                                                                                                              │
     │                                                                                                                                                                                               │
     │ 2.1 BlockService erstellen                                                                                                                                                                    │
     │                                                                                                                                                                                               │
     │ src/services/BlockService.ts:                                                                                                                                                                 │
     │ - getBlockMetadata(worldId, x, y, z) → GET existierender Endpunkt                                                                                                                             │
     │ - updateBlockMetadata(worldId, x, y, z, metadata) → PUT (noch zu implementieren im Server)                                                                                                    │
     │ - deleteBlockMetadata(worldId, x, y, z) → DELETE (noch zu implementieren)                                                                                                                     │
     │                                                                                                                                                                                               │
     │ Hinweis: PUT/DELETE werden später im Server implementiert, erstmal nur GET nutzen (Read-Only Preview)                                                                                         │
     │                                                                                                                                                                                               │
     │ 2.2 Composable erstellen                                                                                                                                                                      │
     │                                                                                                                                                                                               │
     │ src/composables/useBlockInstance.ts:                                                                                                                                                          │
     │ - loadBlock(worldId, x, y, z) - Lädt Block Metadata                                                                                                                                           │
     │ - updateBlock(metadata) - Speichert Block Metadata (später)                                                                                                                                   │
     │ - State: blockMetadata, blockType (von BlockTypeService), loading, error                                                                                                                      │
     │                                                                                                                                                                                               │
     │ Phase 3: Block Instance Editor View                                                                                                                                                           │
     │                                                                                                                                                                                               │
     │ 3.1 BlockInstanceEditor View                                                                                                                                                                  │
     │                                                                                                                                                                                               │
     │ src/views/BlockInstanceEditor.vue:                                                                                                                                                            │
     │ - Header: "Edit Block at (x, y, z)"                                                                                                                                                           │
     │ - BlockType Info Section (read-only):                                                                                                                                                         │
     │   - BlockType ID                                                                                                                                                                              │
     │   - Description                                                                                                                                                                               │
     │   - Link/Button: "Edit BlockType Template"                                                                                                                                                    │
     │ - Group Management Section:                                                                                                                                                                   │
     │   - Current groups (chips/badges)                                                                                                                                                             │
     │   - Add/Remove group (Dropdown oder Input)                                                                                                                                                    │
     │ - Metadata Modifiers Section:                                                                                                                                                                 │
     │   - Liste vorhandener Status-Overrides in Block.metadata.modifiers                                                                                                                            │
     │   - Button: "Add Status Override"                                                                                                                                                             │
     │   - Nutzt gleichen ModifierEditorDialog wie BlockTypeEditor                                                                                                                                   │
     │                                                                                                                                                                                               │
     │ 3.2 Component Reuse                                                                                                                                                                           │
     │                                                                                                                                                                                               │
     │ - ModifierEditorDialog: Bereits vorhanden, kann 1:1 verwendet werden                                                                                                                          │
     │ - CollapsibleSection: Bereits vorhanden                                                                                                                                                       │
     │ - Alle Editor-Komponenten: Visibility, Physics, Wind, etc. - alle wiederverwendbar                                                                                                            │
     │                                                                                                                                                                                               │
     │ Phase 4: UI-Anpassungen                                                                                                                                                                       │
     │                                                                                                                                                                                               │
     │ 4.1 Mode Indicator                                                                                                                                                                            │
     │                                                                                                                                                                                               │
     │ - Badge im Header: "BlockType Editor" vs "Block Instance Editor"                                                                                                                              │
     │ - Breadcrumb: World > Block (x, y, z)                                                                                                                                                         │
     │                                                                                                                                                                                               │
     │ 4.2 Save-Verhalten                                                                                                                                                                            │
     │                                                                                                                                                                                               │
     │ - Im BlockType-Modus: Wie bisher                                                                                                                                                              │
     │ - Im Block-Modus:                                                                                                                                                                             │
     │   - Wenn PUT-Endpunkt vorhanden → Speichern                                                                                                                                                   │
     │   - Sonst: Warnung "Read-Only Mode (Server endpoint not yet implemented)"                                                                                                                     │
     │                                                                                                                                                                                               │
     │ 4.3 Navigation                                                                                                                                                                                │
     │                                                                                                                                                                                               │
     │ - Im Block-Modus: "Back" Button zur Hauptanwendung                                                                                                                                            │
     │ - Link zu BlockType-Editor für den zugrunde liegenden Typ                                                                                                                                     │
     │                                                                                                                                                                                               │
     │ Phase 5: Integration & Testing                                                                                                                                                                │
     │                                                                                                                                                                                               │
     │ 5.1 URL-Beispiele                                                                                                                                                                             │
     │                                                                                                                                                                                               │
     │ - BlockType-Modus: http://localhost:3002/                                                                                                                                                     │
     │ - BlockType-Modus (explizit): http://localhost:3002/?world=main                                                                                                                               │
     │ - Block-Modus: http://localhost:3002/?world=main&block=10,64,5                                                                                                                                │
     │                                                                                                                                                                                               │
     │ 5.2 Test-Szenarien                                                                                                                                                                            │
     │                                                                                                                                                                                               │
     │ - Block-Modus öffnen mit gültigen Koordinaten                                                                                                                                                 │
     │ - Block ohne Metadata (nur BlockType)                                                                                                                                                         │
     │ - Block mit Metadata.modifiers                                                                                                                                                                │
     │ - Mode-Wechsel via URL                                                                                                                                                                        │
     │ - Speichern (später wenn PUT existiert)                                                                                                                                                       │
     │                                                                                                                                                                                               │
     │ Implementierungs-Reihenfolge                                                                                                                                                                  │
     │                                                                                                                                                                                               │
     │ 1. Router & Mode Detection (Phase 1)                                                                                                                                                          │
     │ 2. BlockService + Composable (Phase 2)                                                                                                                                                        │
     │ 3. BlockInstanceEditor View (Phase 3)                                                                                                                                                         │
     │ 4. UI-Anpassungen (Phase 4)                                                                                                                                                                   │
     │ 5. Testing (Phase 5)                                                                                                                                                                          │
     │                                                                                                                                                                                               │
     │ Hinweis zu REST-Endpunkten                                                                                                                                                                    │
     │                                                                                                                                                                                               │
     │ Die folgenden Server-Endpunkte werden separat implementiert (nicht in diesem Task):                                                                                                           │
     │ - PUT /api/worlds/{worldId}/blocks/{x}/{y}/{z}/metadata                                                                                                                                       │
     │ - DELETE /api/worlds/{worldId}/blocks/{x}/{y}/{z}/metadata                                                                                                                                    │
     │                                                                                                                                                                                               │
     │ Bis dahin: Material Editor zeigt Block-Metadata im Read-Only-Modus an.       
```

Es wird noch mehr editoren geben die auch ineinander greifen muessen. Ist es sinnvoll alle dialoge / editoren in einem projekt (aktuell material_editor) zu erstellen und dann je nach konfiguration (z.b. url parameter oder dotenv) einen
anderen editor anzuzeigen? 

```text
  Drei mögliche Architektur-Ansätze:

  Option 1: Ein "Universal Editor" (Multi-Mode in einem Projekt)

  Wie: Material Editor wird zum "Nimbus Editor Hub" mit verschiedenen Modi via URL-Parameter

  Vorteile:
  - ✅ Maximale Code-Wiederverwendung (alle Komponenten an einem Ort)
  - ✅ Ein Build-Prozess, ein Deployment
  - ✅ Konsistente UX über alle Editor-Modi
  - ✅ Einfache Cross-Navigation zwischen Modi (URL-Change)
  - ✅ Shared State möglich (z.B. World-Auswahl bleibt erhalten)

  Nachteile:
  - ⚠️ Bundle-Size wächst mit jedem neuen Editor
  - ⚠️ Viele if/else Conditionals im Code
  - ⚠️ Kann unübersichtlich werden bei vielen Modi
  - ⚠️ Ein Bug kann alle Modi betreffen

  Best für: 3-5 eng verwandte Editor-Modi

  ---
  Option 2: Separate Projekte pro Editor

  Wie: @nimbus/material_editor, @nimbus/block_editor, @nimbus/entity_editor, etc.

  Vorteile:
  - ✅ Klare Separation of Concerns
  - ✅ Kleinere Bundles (nur was benötigt wird)
  - ✅ Unabhängige Entwicklung/Deployment
  - ✅ Weniger Risiko bei Breaking Changes

  Nachteile:
  - ❌ Massive Code-Duplikation (ModifierEditorDialog, etc.)
  - ❌ Inkonsistente UX wenn nicht gut koordiniert
  - ❌ Mehr Build/Maintenance-Aufwand
  - ❌ Schwieriger zu warten bei Änderungen

  Best für: Komplett unterschiedliche Editoren mit wenig Überschneidung

  ---
  Option 3: Shared Component Library + Focused Apps ⭐ (Empfehlung)

  Wie: Neues Package @nimbus/editor_components + mehrere kleine Editor-Apps

  Struktur:
  packages/
  ├── shared/              # Bestehend: Types, Protokolle
  ├── editor_components/   # NEU: Shared Editor UI Components
  │   ├── ModifierEditorDialog.vue
  │   ├── CollapsibleSection.vue
  │   ├── AssetPickerDialog.vue
  │   ├── VisibilityEditor.vue
  │   ├── PhysicsEditor.vue
  │   └── ...
  ├── material_editor/     # Bestehend: BlockType + Asset Management
  ├── block_editor/        # NEU: Einzelne Block-Instanzen
  ├── entity_editor/       # Zukünftig: Entities
  └── world_editor/        # Zukünftig: World Settings

  Vorteile:
  - ✅ Keine Code-Duplikation - Komponenten werden geteilt
  - ✅ Kleine, fokussierte Apps - Jeder Editor tut eine Sache gut
  - ✅ Flexible Kombination - Apps können sich gegenseitig embedden (iframe)
  - ✅ Unabhängige Bundles - Nur laden was benötigt wird
  - ✅ Leicht erweiterbar - Neuer Editor = neues kleines Package
  - ✅ Versionierung - Editor-Components können versioniert werden
  - ✅ Konsistente UX - Durch geteilte Komponenten garantiert

  Nachteile:
  - ⚠️ Mehr Setup (aber nur einmal)
  - ⚠️ Dependency Management zwischen Packages

  Best für: Skalierbare, wartbare Multi-Editor-Architektur

  ---
  🎯 Meine Empfehlung:

  Für deine Anforderungen empfehle ich Option 3: Shared Component Library

  Begründung:
  1. Du sagst "es wird noch mehr Editoren geben" → Langfristige Skalierbarkeit wichtig
  2. Editoren "greifen ineinander" → Gemeinsame Komponenten nötig
  3. Du hast bereits Erfahrung mit pnpm Workspaces (shared package)
  4. Vue-Komponenten sind perfekt für dieses Pattern

  Konkret würde das bedeuten:

  // In block_editor - verwendet editor_components
  import { ModifierEditorDialog } from '@nimbus/editor_components';

  // In entity_editor - gleiche Komponente
  import { ModifierEditorDialog } from '@nimbus/editor_components';

  Migrations-Pfad:
  1. Jetzt: Material Editor wie er ist lassen
  2. Später: @nimbus/editor_components Package erstellen
  3. Komponenten aus material_editor nach editor_components verschieben
  4. material_editor importiert aus editor_components
  5. Neue Editoren nutzen editor_components von Anfang an
```

Option: Ein Editor mit mehrern einstiegspunkten und einem gemeinsammen src/components Ordner.

```text
 Vite Multi-Page App: Nimbus Editors - Ein Projekt, mehrere Editoren

 Ziel

 Umstrukturiere material_editor zu nimbus_editors als Multi-Page-Anwendung. Alle Editoren (Material, Block, Entity, etc.) in einem Projekt mit separaten Entry-Points. Ein Dev-Server, separate Production-Builds.

 Phase 1: Projekt-Umstrukturierung

 1.1 Umbenennen & Verzeichnisstruktur

 - Umbenennen: packages/material_editor/ → packages/nimbus_editors/
 - Neue Ordnerstruktur erstellen:
 src/
 ├── components/     # Shared components (alle Editoren)
 ├── editors/        # Modifier-Editoren (shared)
 ├── services/       # API Services (shared)
 ├── composables/    # Composables (shared)
 ├── utils/          # Utils (shared)
 ├── material/       # Material Editor App
 ├── block/          # Block Editor App (neu, leer)
 └── style.css       # Global styles

 1.2 Komponenten verschieben

 Von: src/components/ (aktuell gemischt)
 Nach: Organisiert nach Verwendung:
 - src/components/ → Shared (SearchInput, LoadingSpinner, ErrorAlert, etc.)
 - src/material/components/ → Material-spezifisch (BlockTypeList, AssetGrid, etc.)
 - src/material/views/ → Material Views

 1.3 HTML Entry-Points erstellen

 - material-editor.html → src/material/main.ts
 - block-editor.html → src/block/main.ts (neu)
 - index.html → Optional: Landing Page mit Links

 Phase 2: Vite Multi-Page Konfiguration

 2.1 vite.config.ts anpassen

 export default defineConfig({
   plugins: [vue()],
   build: {
     rollupOptions: {
       input: {
         'material-editor': resolve(__dirname, 'material-editor.html'),
         'block-editor': resolve(__dirname, 'block-editor.html'),
       },
       output: {
         dir: 'dist',
         entryFileNames: '[name]/assets/[name]-[hash].js',
         chunkFileNames: '[name]/assets/chunks/[name]-[hash].js',
         assetFileNames: '[name]/assets/[name]-[hash].[ext]',
       },
     },
   },
   server: {
     port: 3002,
     open: '/material-editor.html', // Default beim Start
   },
 });

 2.2 Path Aliases aktualisieren

 resolve: {
   alias: {
     '@': resolve(__dirname, './src'),
     '@material': resolve(__dirname, './src/material'),
     '@block': resolve(__dirname, './src/block'),
     '@components': resolve(__dirname, './src/components'),
     '@editors': resolve(__dirname, './src/editors'),
     '@nimbus/shared': resolve(__dirname, '../shared/src'),
   },
 }

 Phase 3: Material Editor refactoren

 3.1 Datei-Moves

 Aktuell:
 src/
 ├── App.vue
 ├── main.ts
 ├── components/...
 └── views/...

 Neu:
 src/material/
 ├── MaterialApp.vue (vorher App.vue)
 ├── main.ts (angepasst)
 ├── views/
 │   ├── BlockTypeEditor.vue
 │   └── AssetEditor.vue
 └── components/
     ├── BlockTypeList.vue
     ├── BlockTypeEditorPanel.vue
     ├── AssetGrid.vue
     └── AssetUploadDialog.vue

 3.2 Imports anpassen

 Vorher:
 import ModifierEditorDialog from '../components/ModifierEditorDialog.vue';

 Nachher:
 import ModifierEditorDialog from '@components/ModifierEditorDialog.vue';
 // oder
 import ModifierEditorDialog from '@/components/ModifierEditorDialog.vue';

 3.3 material-editor.html erstellen

 <!DOCTYPE html>
 <html lang="en">
   <head>
     <meta charset="UTF-8" />
     <title>Nimbus Material Editor</title>
   </head>
   <body>
     <div id="app"></div>
     <script type="module" src="/src/material/main.ts"></script>
   </body>
 </html>

 Phase 4: Block Editor erstellen (Skeleton)

 4.1 Block Editor Struktur

 src/block/
 ├── BlockApp.vue          # Root component
 ├── main.ts               # Entry point
 ├── views/
 │   └── BlockInstanceEditor.vue
 └── components/
     └── BlockTypeInfo.vue  # Zeigt BlockType-Referenz

 4.2 BlockApp.vue (simpel)

 <template>
   <div class="min-h-screen">
     <header class="navbar bg-base-300">
       <h1 class="text-xl font-bold">Block Instance Editor</h1>
     </header>
     <main class="container mx-auto p-4">
       <BlockInstanceEditor />
     </main>
   </div>
 </template>

 4.3 block-editor.html

 <!DOCTYPE html>
 <html lang="en">
   <head>
     <meta charset="UTF-8" />
     <title>Nimbus Block Editor</title>
   </head>
   <body>
     <div id="app"></div>
     <script type="module" src="/src/block/main.ts"></script>
   </body>
 </html>

 4.4 BlockInstanceEditor.vue (MVP)

 - Liest URL-Parameter: ?world=main&block=10,64,5
 - Nutzt useBlockInstance Composable
 - Zeigt BlockType-Info (read-only)
 - Nutzt gleichen ModifierEditorDialog aus @components/

 Phase 5: Package-Konfiguration anpassen

 5.1 package.json

 {
   "name": "@nimbus/nimbus_editors",
   "description": "Multi-page editor suite for Nimbus",
   "scripts": {
     "dev": "vite",
     "build": "vite build && tsc",
     "preview": "vite preview",
   }
 }

 5.2 .env.local

 VITE_API_URL=http://localhost:3000
 VITE_WORLD_ID=main

 Phase 6: Root-Integration

 6.1 Umbenennung in Root

 tsconfig.json:
 {
   "references": [
     { "path": "./packages/nimbus_editors" }  // Umbenannt
   ]
 }

 package.json Scripts:
 {
   "dev:editors": "pnpm --filter @nimbus/nimbus_editors dev",
   "build:editors": "pnpm build:shared && pnpm --filter @nimbus/nimbus_editors build"
 }

 Phase 7: Production Deployment

 7.1 Build-Output

 pnpm build:editors

 Ergebnis:
 dist/
 ├── material-editor/
 │   ├── index.html
 │   └── assets/*.js, *.css
 └── block-editor/
     ├── index.html
     └── assets/*.js, *.css

 7.2 Webserver-Struktur

 Option A: Subdirectories
 /var/www/nimbus/
 ├── material-editor/ → dist/material-editor/
 └── block-editor/    → dist/block-editor/

 URLs:
 - http://server.com/material-editor/
 - http://server.com/block-editor/

 Option B: Nginx Routing
 location /material-editor {
   alias /var/www/nimbus/dist/material-editor;
 }
 location /block-editor {
   alias /var/www/nimbus/dist/block-editor;
 }

 Implementierungs-Schritte

 1. Umbenennen material_editor → nimbus_editors
 2. Ordnerstruktur neu organisieren (src/material/, src/components/, src/editors/)
 3. Komponenten verschieben (shared → src/components/, spezifisch → src/material/components/)
 4. Vite Multi-Page Config schreiben
 5. material-editor.html erstellen
 6. Imports aktualisieren (neue Pfade)
 7. Testen (Material Editor muss noch funktionieren)
 8. block-editor.html + Skeleton erstellen
 9. Root-Integration (tsconfig, package.json)
 10. Build testen (beide Editoren separat gebaut)

 Migration der bestehenden Komponenten

 Automatisch geteilt (in src/components/ oder src/editors/):
 - ModifierEditorDialog.vue
 - CollapsibleSection.vue
 - Alle *Editor.vue (Visibility, Physics, Wind, etc.)
 - AssetPickerDialog.vue
 - SearchInput, LoadingSpinner, ErrorAlert

 Material-spezifisch (in src/material/components/):
 - AppHeader.vue
 - WorldSelector.vue
 - TabNavigation.vue
 - BlockTypeList, BlockTypeListItem, BlockTypeEditorPanel
 - AssetGrid, AssetUploadDialog

 Block-spezifisch (neu in src/block/components/):
 - BlockInstanceInfo.vue
 - GroupManager.vue (später)

 Keine if/else nötig!

 Jeder Editor ist eine komplett separate Vue-App mit eigenem Entry-Point. Null Conditionals im Code!
```
Result:
```text
⏺ ✅ Vite Multi-Page Architektur erfolgreich implementiert!

  🎉 Was wurde erreicht:

  1. Package umbenannt:
  - @nimbus/material_editor → @nimbus/nimbus_editors
  - Ein Projekt für alle zukünftigen Editoren

  2. Multi-Page-Struktur:
  nimbus_editors/
  ├── material-editor.html  → Material Editor Entry
  ├── block-editor.html     → Block Editor Entry
  └── src/
      ├── components/      # Shared: SearchInput, LoadingSpinner, etc.
      ├── editors/         # Shared: VisibilityEditor, PhysicsEditor, etc.
      ├── services/        # Shared: API Services
      ├── composables/     # Shared: State Management
      ├── material/        # Material Editor App
      │   ├── MaterialApp.vue
      │   ├── main.ts
      │   ├── views/
      │   └── components/  # Material-spezifisch
      └── block/           # Block Editor App
          ├── BlockApp.vue
          ├── main.ts
          └── views/

  3. Build-Output:
  dist/
  ├── material-editor.html         (Entry)
  ├── block-editor.html            (Entry)
  └── assets/
      ├── material-editor-xxx.js   (196 KB, gzip: 56 KB)
      ├── block-editor-xxx.js      (2 KB, gzip: 1 KB)
      └── style-xxx.css            (80 KB, gzip: 12 KB)

  4. Development:
  # Ein Prozess startet ALLES:
  pnpm dev:editors

  # Zugriff:
  - http://localhost:3002/material-editor.html
  - http://localhost:3002/block-editor.html?world=main&block=10,64,5

  5. Production:
  pnpm build:editors

  # Deployment: Einfach dist/ auf Webserver kopieren
  # Jeder Editor unter eigenem Pfad zugänglich

  ✨ Architektur-Vorteile:

  ✅ Ein Dev-Server - Keine mehreren Prozesse mehr
  ✅ Keine if/else - Komplett separate Apps
  ✅ Code-Sharing - Komponenten in src/components/ von allen nutzbar
  ✅ Kleine Bundles - Tree-Shaking: Block Editor nur 2KB!
  ✅ Unabhängig deploybar - Jeder Editor als separates HTML
  ✅ Leicht erweiterbar - Neuer Editor = neue HTML + neuer src/{name}/ Ordner

  🚀 Nächste Schritte:

  Du kannst jetzt:
  1. Neue Editoren hinzufügen: Einfach neue HTML + src/{name}/ erstellen
  2. Komponenten teilen: Alles in src/components/ oder src/editors/
  3. Production deployen: dist/ Ordner auf Webserver

  Der Material Editor funktioniert weiterhin vollständig, und die Architektur ist bereit für Block Editor, Entity Editor, World Editor, etc.! 🎉
```

## Integration

[?] In client wird ein SelectService benoetigt, der von PlayerService die position und rotation des players
nimmt und eine Block vor dem player max (radius) vor dem Player findet und abgefragt werden kann.
- Modus: NONE, INTERACTIVE, BLOCK, AIR, ALL
- getSelectedBlock(modus, position, rotation, radius) : ClientBlock | null
- INTERACTIVE: Gibt nur einen Block zurueck, der block.metatdata.interactive == true hat.
- BLOCK: Gibt jeden gefundenen Block zurueck.
- AIR: Gibt nur AIR Blocks zuruek, also Blocks die es noch nicht gibt, der Abstand zur Position ist dann 'radius' oder kleiner.
- ALL: Findet Blocks oder, wenn nicht gefunden einen AIR Block.
- Wenn ein AIR Block zurueckgegeben wird, wird ein neuer ClientBlock mit einem Block mit der BlockTypeId 0 und den Coordinaten zurueckgegeben.
- SelectService hat eine Referenz auf den ChunkServcie um die Daten abzufragen.

[x] Im client soll es einen auto select mode geben (Auto Select im SelectService) der auf den Selected Block highlight macht.
- Der autoSelectMode wird wie der selectMode gesetzt: NONE, INTERACTIVE, BLOCK, AIR, ALL
- Es kann auch ein AIR Block sein auf den highlight gemacht wird.
- Optional & Clever? Der RenderService koennte eine Referenz auf den Mesh im ClientBlock hinterlegen, hier kann dann das Highlight gemacht werden.

[x] Lege einen ModalService an, der IFrames in Modalen anzeigt. Es soll eine moeglichkeit geben, das Modal, z.b. mit 'X' oben rechts zu schliessen, Des Modal kann oben einen Titel haben. also openModal(title, url, size?) : referenz und
closeModal(referenz), closeAll()

[x] Viewer und Editor ud Selektion
- Im Viewer Modus soll autoSelectMode beim Starten auf INTERACTIVE gesetzt werden.
- Im Editor Modus soll autoSelectMode beim Starten auf BLOCK gesetzt werden.
- Im Editor Modus werden zwei neue InputController registrier
  - Eine neue Variable editorUrl wird benoetigt. Diese wird in WordInfo (World.ts) mitgeleifert.
  - Der NetzwerkService soll eine neue Methode 'createBlockEditorUrl(x,y,z)' bereitstellen.
  - EditSelectionRotator auf Key '.': Wenn '.' gedrueck wird, springt der Modus immer weiter und fängt nach ALL wieder bei NONE an.
  - EditorActivate auf Key '/': Wenn '/' gedrueckt wird, wird ein IFrame mit der editorUrl vom NetzwerkService geladen.

[?] Mehr IFrame Features
- Wenn IFrame auf gemacht werden, soll ein name mitgegeben werden, der name ist dann der referenz key
- Wenn ein frame mit dem referenz key schon da ist, wird der aktualisiert (neue url?), aber kein neuer aauf gemacht
- SIZE_PRESETS sollen nicht nur die size, sondern auch die position mit beinhalten: 
  - left: gamze linke seite (mit margins)
  - right: gamze rechte seite (mit margins)
  - top: gamze obere seite (mit margins)
  - bottom: gamze untere seite (mit margins)
  - center_small: gamze mitte small (wie bisher)
  - center_medium: gamze mitte medium (wie bisher)
  - center_large: gamze mitte large (wie bisher)
  - left_top: Quadrant links oben
  - left_bottom, right_top, right_bottom
- Beim erstellen soll es eine Option geben, die mit bits arbeiten:
  - ob das IFrame immer geschlossen werden soll. FLAGS.CLOSEABLE
  - ob der Rand duenn sein soll (dann kein titel, kein close, etc. ) FLAGS.NO_BORDERS
  - ob ein break out button angezeigt werden soll. FLAGS.BREAK_OUT
  - z.b. openModal("editor", "Editor", "http://localhost/editor, SIZE_PRESETS.LEFT, FLAGS.CLOSEABLE | FLAGS.BREAK_OUT)
- Beim aufmachen des IFrame wird immer an die url 'embedded=true' angehängt. Die Breakout funktion oeffnen die URL in 
  einem neuen Fenster mit 'embedded=false' und schliesst das urspruengliche iframe
- Es soll eine kommunikation zwischen dem IFrame und dem Viewer geben. Funktionen:
  - closeModal(reason) - schliesst dieses Modal
  - changePosition('name of preset') - Ändert die Position dieses Modals
  - notification(type, from, message) wird an den NotificationServcie geschickt.

Beispiel fuer die Implementierung:
```typescript
Kind:

// z.B. https://child.example.com:8443
type ParentMsg = { type: "IFRAME_READY" } | { type: "REQUEST_CLOSE"; reason?: string };

function notifyReady() {
  const msg: ParentMsg = { type: "IFRAME_READY" };
  window.parent.postMessage(msg, "https://parent.example.com:3000"); // exaktes targetOrigin inkl. Port!
}

function requestClose(reason?: string) {
  const msg: ParentMsg = { type: "REQUEST_CLOSE", reason };
  window.parent.postMessage(msg, "https://parent.example.com:3000");
}

// Beispiel: Button klickt "Close"
document.getElementById("closeBtn")?.addEventListener("click", () => requestClose("done"));

Parent:

// z.B. https://parent.example.com:3000
type ChildMsg = { type: "IFRAME_READY" } | { type: "REQUEST_CLOSE"; reason?: string };

const iframe = document.getElementById("childFrame") as HTMLIFrameElement;
const ALLOWED_CHILD_ORIGIN = "https://child.example.com:8443"; // inkl. Port prüfen!

window.addEventListener("message", (event: MessageEvent<ChildMsg>) => {
  // 1) Sicherheit: nur erlaubte Origin akzeptieren
  if (event.origin !== ALLOWED_CHILD_ORIGIN) return;

  // 2) Optional: prüfen, dass die Nachricht wirklich von *diesem* iFrame stammt
  if (event.source !== iframe.contentWindow) return;

  // 3) Routing nach Nachrichtentyp
  switch (event.data?.type) {
    case "IFRAME_READY":
      console.log("iFrame ist bereit.");
      break;
    case "REQUEST_CLOSE":
      console.log("iFrame will schließen. Grund:", event.data.reason);
      // iFrame entfernen/schließen:
      iframe.remove(); // oder verstecken: iframe.style.display = "none";
      break;
  }
});

IFrmame:

<iframe
  id="childFrame"
  src="https://child.example.com:8443/app"
  referrerpolicy="no-referrer"
  sandbox="allow-scripts allow-forms"
  style="border:0; width:100%; height:600px;"
></iframe>
```

```text
⏺ Alle IFrame-Features wurden erfolgreich implementiert! Hier ist eine Zusammenfassung der Änderungen:

  Implementierte Features

  1. Referenz-Keys für IFrames

  - Beim Öffnen eines IFrames kann ein referenceKey mitgegeben werden
  - Wenn ein Frame mit diesem Key bereits existiert, wird er wiederverwendet und die URL aktualisiert
  - Beispiel: modalService.openModal('editor', 'Editor', url, ...)

  2. Erweiterte SIZE_PRESETS

  Neue Presets mit Position und Größe:
  - Seitenpanels: left, right (50% Breite, volle Höhe)
  - Oben/Unten: top, bottom (volle Breite, 50% Höhe)
  - Zentriert: center_small, center_medium, center_large
  - Quadranten: left_top, left_bottom, right_top, right_bottom
  - Alle mit 20px Margins auf allen Seiten

  3. Bitflags für Optionen

  ModalFlags.CLOSEABLE    // Modal kann geschlossen werden
  ModalFlags.NO_BORDERS   // Keine Ränder (kein Titel, kein Close-Button)
  ModalFlags.BREAK_OUT    // Break-out-Button anzeigen
  Verwendung: ModalFlags.CLOSEABLE | ModalFlags.BREAK_OUT

  4. embedded=true Parameter

  - Wird automatisch an alle IFrame-URLs angehängt
  - Bei Break-out wird embedded=false verwendet

  5. postMessage-Kommunikation

  Der IFrameHelper bietet folgende Funktionen für IFrame-Content:
  IFrameHelper.notifyReady()
  IFrameHelper.requestClose(reason)
  IFrameHelper.requestPositionChange(preset)
  IFrameHelper.sendNotification(type, from, message)
  IFrameHelper.isEmbedded()
  IFrameHelper.isInIFrame()

  6. Break-out-Funktionalität

  - Button öffnet die URL in einem neuen Fenster mit embedded=false
  - Original-Modal wird automatisch geschlossen

  Geänderte Dateien

  Neue Dateien:
  - /client/packages/client/src/services/IFrameHelper.ts - Helper für IFrame-Kommunikation
  - /client/packages/client/src/services/index.ts - Service-Exports
  - /client/packages/client/src/services/MODAL_SERVICE_USAGE.md - Dokumentation

  Aktualisierte Dateien:
  - /client/packages/client/src/types/Modal.ts - Neue Types und Enums
  - /client/packages/client/src/services/ModalService.ts - Erweiterte Implementierung
  - /client/packages/client/src/types/index.ts - Modal-Exports hinzugefügt
  - /client/packages/client/src/input/handlers/EditorHandlers.ts - Neue API verwendet

  Verwendungsbeispiel

  import { ModalService, ModalFlags } from '@nimbus/client/services';

  // Modal öffnen mit allen Features
  const modal = modalService.openModal(
    'editor',                          // referenceKey
    'Block Editor',                    // title
    'http://localhost:8080/editor',   // url
    'left',                            // preset
    ModalFlags.CLOSEABLE | ModalFlags.BREAK_OUT  // flags
  );

  // Im IFrame:
  import { IFrameHelper } from '@nimbus/client/services';

  IFrameHelper.notifyReady();
  IFrameHelper.requestPositionChange('right');
  IFrameHelper.sendNotification('info', 'Editor', 'Saved');
  IFrameHelper.requestClose('done');

  Die TypeScript-Kompilierung wurde erfolgreich getestet. Die vollständige Dokumentation finden Sie in MODAL_SERVICE_USAGE.md (src/services/ModalService.ts:7, src/types/Modal.ts:57, src/services/IFrameHelper.ts:1).

⏺ Perfekt! Ich habe das ModalSizePreset von einem String-Union-Type in ein Enum umgewandelt.

  Änderungen:

  1. Enum statt Type

  export enum ModalSizePreset {
    LEFT = 'left',
    RIGHT = 'right',
    TOP = 'top',
    BOTTOM = 'bottom',
    CENTER_SMALL = 'center_small',
    CENTER_MEDIUM = 'center_medium',
    CENTER_LARGE = 'center_large',
    LEFT_TOP = 'left_top',
    LEFT_BOTTOM = 'left_bottom',
    RIGHT_TOP = 'right_top',
    RIGHT_BOTTOM = 'right_bottom',
  }

  2. Verwendung

  // Vorher (String):
  modalService.openModal('editor', 'Editor', url, 'left', ModalFlags.CLOSEABLE);

  // Nachher (Enum):
  modalService.openModal('editor', 'Editor', url, ModalSizePreset.LEFT, ModalFlags.CLOSEABLE);

  3. Vorteile:

  - Typsicherheit: Tippfehler wie "rigth-pane" werden zur Compile-Zeit erkannt
  - Autovervollständigung: IDE kann alle verfügbaren Presets vorschlagen
  - Refactoring: Einfacher umzubenennen
  - Dokumentation: Jeder Enum-Wert hat einen Kommentar

  4. Geänderte Dateien:

  - ✅ Modal.ts - Enum definiert
  - ✅ ModalService.ts - SIZE_PRESETS und Default-Wert aktualisiert
  - ✅ IFrameHelper.ts - Import aktualisiert
  - ✅ EditorHandlers.ts - Verwendung aktualisiert (Tippfehler korrigiert!)
  - ✅ MODAL_SERVICE_USAGE.md - Dokumentation aktualisiert

  Die TypeScript-Kompilierung ist erfolgreich! (Modal.ts:41, ModalService.ts:25, EditorHandlers.ts:133)

```

[?] Erstelle in nimbus_editors ein Modul/Component, das diese Schnittstelle aus Kind/IFrame sicht bereitstellt und dort genutzt werden kann
 - isEmbedded()
 - closeModal(reason) - schliesst dieses Modal
 - changePosition('name of preset') - Ändert die Position dieses Modals
 - notification(type, from, message) wird an den NotificationServcie geschickt.
 - Du kannst gemeinsamme Constanten 'ModalSizePreset, ModalFlags' in das package 'shared' verschieben, damit client und nimbus_editors darauf zugreifen keonnen.
```text
⏺ Perfekt! Ich habe das Modul für nimbus_editors erfolgreich erstellt. Hier ist eine Zusammenfassung:

  Was wurde implementiert:

  1. Gemeinsame Konstanten nach shared verschoben

  Neue Datei: /packages/shared/src/types/Modal.ts
  - ModalSizePreset enum (alle Position-Presets)
  - ModalFlags enum (CLOSEABLE, NO_BORDERS, BREAK_OUT)
  - IFrameMessageType enum (Message-Typen)
  - IFrameMessageFromChild type (Message-Struktur)

  Aktualisiert: /packages/shared/src/types/index.ts
  - Export der neuen Modal-Types

  2. Client-Package aktualisiert

  Alle Imports wurden angepasst, um die gemeinsamen Types aus @nimbus/shared zu verwenden:
  - ✅ packages/client/src/types/Modal.ts - Re-exports von shared
  - ✅ packages/client/src/services/ModalService.ts - Import aus shared
  - ✅ packages/client/src/services/IFrameHelper.ts - Import aus shared
  - ✅ packages/client/src/input/handlers/EditorHandlers.ts - Import aus shared

  3. nimbus_editors Composable erstellt

  Neue Datei: /packages/nimbus_editors/src/composables/useModal.ts

  Vue 3 Composable mit folgenden Funktionen:

  const {
    isEmbedded,       // Check if running in embedded mode
    isInIFrame,       // Check if running in iframe
    closeModal,       // Close this modal
    changePosition,   // Change modal position
    sendNotification, // Send notification to parent
    notifyReady       // Notify parent that iframe is ready
  } = useModal();

  4. Dokumentation erstellt

  Neue Datei: /packages/nimbus_editors/src/composables/USEMODAL_USAGE.md
  - Vollständige API-Dokumentation
  - Verwendungsbeispiele
  - Best Practices
  - TypeScript-Integration
  - Sicherheitshinweise

  Verwendung in nimbus_editors:

  <template>
    <div>
      <button @click="handleSave">Save</button>
      <button v-if="isEmbedded()" @click="handleClose">Close</button>
    </div>
  </template>

  <script setup lang="ts">
  import { onMounted } from 'vue';
  import { useModal, ModalSizePreset } from '@/composables/useModal';

  const { isEmbedded, closeModal, sendNotification, notifyReady } = useModal();

  onMounted(() => {
    if (isEmbedded()) {
      notifyReady();
    }
  });

  async function handleSave() {
    // Save logic...
    sendNotification('0', 'Editor', 'Saved successfully');
    closeModal('saved');
  }

  function handleClose() {
    closeModal('user_cancelled');
  }
  </script>

  Vorteile:

  1. Type-Safe: Alle Enums und Types werden von shared geteilt
  2. Vue 3 Integration: Native Composable-API
  3. Einfache Verwendung: Klare, intuitive API
  4. Dokumentiert: Vollständige Dokumentation mit Beispielen
  5. Getestet: TypeScript-Kompilierung erfolgreich für alle Packages

  Die Implementierung ist abgeschlossen und alle TypeScript-Prüfungen sind erfolgreich! (packages/nimbus_editors/src/composables/useModal.ts:1, packages/shared/src/types/Modal.ts:1)
```

[?] Im Server werden REST Endpunkte benötigt um Blocke zu bearbeiten
die über die Definition in 'client/instructions/client_2.0/server_rest_api.md' hinausgehen.
- POST /api/worlds/{worldId}/blocks/{x}/{y}/{z}
- PUT /api/worlds/{worldId}/blocks/{x}/{y}/{z}
- DELETE /api/worlds/{worldId}/blocks/{x}/{y}/{z}

- Erstelle die Endpunkte
- Erweitere die Dokumentation in 'client/instructions/client_2.0/server_rest_api.md'

[?] Beim ändern von Blocks im Server muss an alle Clients ein Update der Blocks/Chunk gesendet werden.
Das kann cumulativ beim Speichern der Chunks passieren. Es gibt hierfuer bereits eine Netzwerk Funktion
Siehe 'client/instructions/client_2.0/network-model-2.0.md'.
Muss das angepasst werden?
- Ein DELETE schickt als Block den BlockType '0', das ist dan ein Loeschen.
- Ist es nicht sinnvoller immer kurz zu warten, falls mehrere Blocks nacheinander geandert werden kann man dann einen 
  batch an die clients schicken. ggf. immer eine sekunde warten und changes sammeln.
```text
 Plan: Block-Update-Broadcast mit Batching implementieren

 Änderungen:

 1. Neue Klasse: BlockUpdateBuffer (src/network/BlockUpdateBuffer.ts)

 class BlockUpdateBuffer {
   private pendingUpdates: Map<string, Block[]>; // worldId -> blocks[]
   private flushTimer: NodeJS.Timeout | null = null;
   private readonly FLUSH_INTERVAL = 1000; // 1 Sekunde warten
   private readonly MAX_BATCH_SIZE = 100; // Oder bei 100 Blocks sofort flushen

   addUpdate(worldId: string, block: Block): void
   flush(): void // Sendet alle gesammelten Updates
   private scheduleFlush(): void
 }

 Logik:
 - addUpdate() sammelt Block-Änderungen
 - Startet Timer beim ersten Block (1 Sekunde)
 - Bei 100+ Blocks: sofort flushen (nicht warten)
 - Timer läuft ab: alle gesammelten Blocks broadcasten

 2. NimbusServer erweitern (src/NimbusServer.ts)

 - Singleton-Pattern: static getInstance() und private static instance
 - Neue Instanz-Variable: private blockUpdateBuffer: BlockUpdateBuffer
 - Neue Methode: broadcastBlockUpdates(worldId: string, blocks: Block[])
   - Findet alle Sessions mit registrierten Chunks
   - Berechnet betroffene Chunks aus Block-Positionen
   - Sendet {"t": "b.u", "d": [blocks]} nur an relevante Clients

 3. REST-Endpunkte erweitern (src/api/routes/worldRoutes.ts)

 POST /blocks/:x/:y/:z - Nach erfolgreichem setBlock:
 blockUpdateBuffer.addUpdate(worldId, block);

 PUT /blocks/:x/:y/:z - Nach erfolgreichem setBlock:
 blockUpdateBuffer.addUpdate(worldId, block);

 DELETE /blocks/:x/:y/:z - Nach erfolgreichem deleteBlock:
 const deletionBlock = {
   position: { x, y, z },
   blockTypeId: 0,  // 0 = Löschen
   status: 0,
   metadata: {}
 };
 blockUpdateBuffer.addUpdate(worldId, deletionBlock);

 4. Broadcast-Logik Details

 Chunk-Berechnung:
 const cx = Math.floor(block.position.x / chunkSize);
 const cz = Math.floor(block.position.z / chunkSize);
 const chunkKey = `${cx},${cz}`;

 Client-Filterung:
 - Iteriert über alle Sessions mit session.worldId === worldId
 - Prüft ob session.registeredChunks.has(chunkKey)
 - Sendet Block-Updates nur an relevante Clients

 Nachrichtenformat (gemäß network-model-2.0.md):
 {
   "t": "b.u",
   "d": [
     {
       "position": {"x": 10, "y": 64, "z": 5},
       "blockTypeId": 1,
       "status": 0,
       "metadata": {}
     }
   ]
 }

 Batching-Verhalten:

 Szenario 1: Editor platziert 50 Blocks schnell
 - Alle 50 Blocks werden gesammelt
 - Nach 1 Sekunde: 1 Nachricht mit 50 Blocks

 Szenario 2: Bulk-Operation mit 150 Blocks
 - Bei Block 100: sofort flush (MAX_BATCH_SIZE)
 - Restliche 50 Blocks: nach 1 Sekunde

 Szenario 3: Einzelner Block
 - Block wird gesammelt
 - Nach 1 Sekunde: Nachricht mit 1 Block

 Vorteile:

 ✅ Effizient bei Bulk-Operationen (1 Nachricht statt 100)
 ✅ Reduzierter Netzwerk-Traffic
 ✅ Clients rendern einmal statt nach jedem Block
 ✅ Nutzt bestehendes b.u Protokoll
 ✅ DELETE wird korrekt als blockTypeId: 0 gesendet
 ✅ Konfigurierbar (FLUSH_INTERVAL, MAX_BATCH_SIZE)
```

[?] Block Editor in modal_editors implementieren
- Der Editor existiert bereits, ist aber noch nicht implementiert.
- Implementiere das Formular fuer Block (Block.ts)
- BlockModifer (BlockModifier.ts)
- BlockMetadata (BlockMetadata.ts)
- Nutze den bestehenden BlockModifier Dialog/Form
- Nutze /packages/shared/src/types/Modal.ts um festzustellen ob im Embedded Modus gearbeitet wird.
- Erstelle Delete/Save/Apply/Cancel Buttons.
- Im Embedded Modus soll der Titel/Header nicht angezeigt werden, das wird schon am IFrame angezeigt.
- Nutze den ModalService um das Modal zu schliessen und notifications zu senden.

[ ] Client soll auf die Netzwerk-Nachricht 'b.u' reagieren
- Geanderte Blocks austauschen (type 0 = DELETED)
- ClientBlock / ClientChunk aktualisieren
- Neu rendern
