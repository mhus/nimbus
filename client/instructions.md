# Voxelsrv

https://github.com/VoxelSrv/voxelsrv.git
https://github.com/VoxelSrv/voxelsrv-server-old.git

## Setup

In `client` soll ein TypeScript projekt erstellt werden. Das projekt
soll eine 3D-Welt darstellen.
Im Ordner `client/tmp` sind die beiden alten Projekte `voxelsrv` (3D Ansicht) und `voxelsrv-server` (Server Daten)
enthalten. voxelsrv kann mit voxelsrv-server als Backend oder included im Frontend laufen. Beide Projekte enthalten 
alte dependencies, die teilweise nicht mehr genutzt werden koennen.

Du kannst schritt fuer schritt die anwendung migrieren:
- Lege eine neue Projektstruktur in `client` an.
- Analysiere `client/tmp/voxelsrv` und `client/tmp/voxelsrv-server`
- Welche neuen Versionen der dependencies sind in den beiden Projekten enthalten?
- Welche Dependencies muessen durch neue Technologie ausgetauscht werden?
- Erstelle in `client` eine neue Version des Projekts. Der Server kann gleich als Demo enthalten bleiben,
  damit das Frontent standalone nutzbar bleibt, später soll ein anderer Server verwendet werden der dann die 
  gRPC-Schnittstelle nutzt. 
- Erstelle packages: client, core, server, protocol
- Übernehme alle model Strukturen von `client/tmp/voxelsrv-server` wie z.B. 
  `client/tmp/voxelsrv-server/src/lib/world/entity.ts` in core als model.
- Erstelle einen plan, wie schritt fuer schritt die gesammte Anwendung migriert werden soll.
- Fuehre den plan schritt fuer schritt aus

- Übernehme alle Controller/Manager
- Übernehme alle protobuf configurationen und sender und handler aus den projekten.
- Übernehme alle statischen dateien wie assets, texturen, sounds

```text
 Ich habe erfolgreich die komplette Migration von client durchgeführt! Hier ist eine Zusammenfassung:

  ---
  ✅ Was wurde umgesetzt:

  1. Projekt-Analyse & Struktur

  - Alte Projekte (voxelsrv, voxelsrv-server) vollständig analysiert
  - Monorepo mit 4 Packages erstellt (core, protocol, server, client)
  - Moderne Dependencies: TypeScript 5.7, Babylon.js 7.37, Protobuf 7.4

  2. Core Package (@voxel-02/core)

  - Types: XYZ, XZ, Vector3, Rotation
  - Helpers: Chunk-Koordinaten-Transformation, Seeds
  - Models: Entity, World, Chunk, Block, Item, Inventory

  3. Protocol Package (@voxel-02/protocol)

  - Proto-Dateien kopiert: client.proto, server.proto, world.proto
  - Handler-Interfaces für Client/Server
  - Basis für Protobuf-Integration

  4. Server Package (@voxel-02/server) ⭐

  - Registry-System: Blocks, Items, Commands mit ID-Verwaltung
  - World-Manager: Multi-World-Support, Chunk-Cache, Auto-Save
  - World-Generatoren:
    - FlatWorldGenerator (flache Welt)
    - NormalWorldGenerator (Simplex-Noise-Terrain)
  - Entity-Manager: UUID-basiert, Position/Rotation, Tick-System
  - WebSocket-Server: Client-Verbindungen, Message-Handling

  5. Client Package (@voxel-02/client) ⭐

  - Babylon.js 7.x Integration
  - Scene mit FreeCamera (WASD-Steuerung)
  - Licht-System
  - Vite Dev-Server (sehr schnell!)
  - HTML/CSS mit Loading-Screen

  6. Assets 🎨

  - 1896 Dateien kopiert:
    - Texturen (Blöcke, Items, UI)
    - Audio (Sounds, Musik)
    - Fonts
    - 3D-Models

  ---
  📁 Dateien erstellt:

  Dokumentation

  - README.md - Projekt-Übersicht
  - MIGRATION_PLAN.md - Detaillierter Phasenplan
  - STATUS.md - Aktueller Projektstatus
  - QUICKSTART.md - Schnelleinstieg-Anleitung

  Code (~25 TypeScript-Dateien, ~2000 Zeilen)

  - Core: 5 Dateien
  - Protocol: 3 Dateien (+ Proto-Files)
  - Server: 8 Dateien
  - Client: 3 Dateien
  - Config: 9 package.json + tsconfig.json

  ---
  🚀 Wie starten:

  cd client

  # Dependencies installieren
  npm install

  # Server starten (Port 3001)
  npm run dev:server

  # Client starten (Port 3000)
  npm run dev:client

  Server-Ausgabe:
  [Server] Server started on port 3001
  [Server] World: world (seed: 123456)
  [Server] Generator: normal

  Client: http://localhost:3000 (Babylon.js 3D-Scene)

  ---
  🎯 Nächste mögliche Schritte (optional):

  1. Client-Server-Verbindung: WebSocket-Integration
  2. Chunk-Rendering: Mesh-Generierung aus Chunk-Daten
  3. Player-System: Spawn, Inventar, Movement
  4. GUI: Menu, HUD, Chat, Inventar
  5. Multiplayer: Testing & Optimierung

  ---
  📊 Statistiken:

  - Packages: 4
  - TypeScript Files: ~25
  - Code Lines: ~2000
  - Assets: 1896
  - Dependencies: Alle auf neueste Versionen aktualisiert

  Von → Zu:
  - TypeScript 4.2 → 5.7
  - Webpack 4 → Vite
  - Babylon.js 5.0-alpha → 7.37 (stabil)
  - Protobuf 6.10 → 7.4
  - WebSocket 7.4 → 8.18

  ---
  Die Basis-Migration ist komplett abgeschlossen! Das Projekt ist jetzt eine moderne, erweiterbare Voxel-Engine mit sauberer Architektur. Alle weiteren Features können schrittweise hinzugefügt werden. 🎮
```
---
Der Client ist aber nicht so wie in `client/tmp/voxelsrv`. Am Anfang fehlt z.b. das menu mit dem man sich einloggen 
und den server angeben kann.
---
Der client zeigt aber keine welt an. Es gibt nur eine leere szene mit einer kamera und licht.
---
Warum uebernimmst du das system nicht von `client/tmp/voxelsrv`? Das ist was wir hier erreichen wollen. Die Migration des projektes.
---
Wenn ich in den bildschirm clicke soll der mauszeiger verschwinden und die steuerung ohne gehaltene taste funktionieren.
---
Es fehlt noch collision detection. Wenn ich auf einen Block stosse kann ich nicht weiter durch den Block 'gehen'.

Es soll einen unterscheidliche vortbewegungen geben. Bei Walking wird man durch gravitation auf den boden gezogen. Mit space kann man dann springen.
Mit der F Taste kann man zwischen flight und walk mode wechseln. Im Flight mode gibt es keine gravitation.
---
Um die Welt, Voxels und Texturen zu debuggen moechte ich, wenn ich wenn ich die Teste '\' druecke einen dump der Welt auf console ausgeben haben.
---
Nachladen von Welt-Dateien-Chunks: Es wird nicht immer alles geladen und angezeigt, da welten wesentlich größer als jetzt sein können. Deshalb macht es sinn chunks im hintergrund nachzuladen wenn man
sich bewegt.
---
Die selektierten zeilen sollen im Safari auf

private renderDistance = 3; // Chunks to load around player
private unloadDistance = 4; // Chunks further than this will be unloaded

und im google chrome auf

private renderDistance = 1; // Chunks to load around player
private unloadDistance = 2; // Chunks further than this will be unloaded

gesetzt werden.
---
Im Flugmodus fliegt man nicht mehr in Blickrichtung. Wenn der flugmodus aktiv ist, soll, wie es vorher war, in Blickrichtung geflugen werden.
---
Mit den Keys Q und W soll man sich nach Links und Rechts drehen können. Zusätzlich zur Mausbewegung.
---
Im Walk Modus, wenn man an einen Block stößt, soll man automatisch, maximal einen Block, nach oben rutschen. Springen soll zwei Bloecke hoch sein.
---
Im ursprünglichen Projekt werden auch blocks mit namen, nicht integer ids unterstuetzt. In 
`/client/tmp/voxelsrv-server/src/default/blocks.ts` werden alle moeglichen Blocktypen definiert. Uebernehem die Konfiguration
in das package `@voxel-02/core` als EntryTypen. Uebernehem fehlende Attribute aus
`/client/tmp/voxelsrv-server/src/lib/registry.ts' Block in EntryType.
Fuege die Daten sinnvoll hinzu damit server und client die Typen benutzen koennen und das das system flexibel bleibt.
Pruefe die Option, das der Server die Blocktypen/EntityTypen dynamisch ausliefern kann, anstelle von statischen Konfigurationen im Client.
Auch die Generatoren sollen die Typen geim generieren nutzen.

```text
 Für weitere Verbesserungen könnten Sie:
  - Texture-Atlas-System für Block-Rendering
  - Block-Metadata-System für Rotationen/Zustände
  - Client-seitiges Caching der Registry
  - Kompressionfür Registry-Sync (Protobuf/MessagePack)
```
---
Wenn du auch die assets, also texturen, modelle, sounds durch den server ausliefern kannst, verschiebe diese dateien vom 
client in den server und passe server und client an.

```text


  Block Metadata System Implementation Complete

  1. Core Metadata Types (packages/core/src/models/BlockMetadata.ts)

  - BlockFacing enum (NORTH, EAST, SOUTH, WEST, UP, DOWN)
  - RotationAxis enum (NONE, X, Y, Z)
  - BlockState flags (OPEN, POWERED, LIT, TRIGGERED, etc.)
  - BlockMetadata interface with 16-bit packed structure
  - Pack/unpack functions for efficient storage

  2. Chunk Data Structure (packages/core/src/models/ChunkData.ts)

  - Shared ChunkData interface with optional metadata array
  - Helper functions: getBlockIndex(), setBlock(), getBlock()
  - Support for both block IDs and metadata storage

  3. Protocol Messages (packages/protocol/src/messages/ChunkMessages.ts)

  - ChunkDataMessage - includes optional metadata array
  - ChunkUpdateMessage - supports single block updates with metadata
  - Helper functions for creating messages

  4. Rendering Support (packages/client/src/rendering/ChunkRenderer.ts)

  - Extracts metadata from chunk data
  - Applies rotation matrices based on metadata
  - Rotates vertices and normals for directional blocks
  - Support for X, Y, and Z axis rotations

  5. Helper Functions (BlockMetadata.ts:259-380)

  - getOppositeFacing() - get opposite direction
  - createMetadataFromPlayerPlacement() - block faces player
  - createMetadataFromPlayerDirection() - block faces player's look direction
  - metadataEquals(), cloneMetadata(), mergeMetadata()
  - isDefaultMetadata() - check if metadata is default
  - createMetadataWithStates() - create with multiple state flags

  Key Features:

  - Memory Efficient: 16-bit packed metadata per block
  - Flexible Rotation: Support for X, Y, Z axis rotations in 45° increments
  - State Flags: 8 bits for various block states (powered, lit, open, etc.)
  - Network Optimized: Optional metadata transmission (only when needed)
  - Backward Compatible: Metadata is optional, defaults work without it

  The system is now ready for use! Blocks can have rotations and states that are:
  - Stored efficiently in chunks
  - Transmitted through protocol messages
  - Rendered correctly with rotation transforms
  - Manipulated with helper functions

---

  ✅ Asset-Delivery-System vollständig implementiert und gebaut!

  Behobene Probleme:

  1. Export-Konflikte: Doppelte Typdefinitionen (BlockType, ItemType, ChunkData, EntityType) in World.ts und Entity.ts entfernt
  2. ChunkData-Struktur: Von {id: XZ} zu {chunkX, chunkZ} aktualisiert
  3. BlockDefinition: Shape-Property hinzugefügt, Tool-Type korrigiert
  4. Item IDs: Von string zu number konvertiert
  5. Type-Safety: Buffer-Handling für Uint16Array vs number[] Kompatibilität
  6. Null-Safety: World loading null/undefined Probleme behoben

  Erfolgreich gebaut:

  - ✅ @voxel-02/core
  - ✅ @voxel-02/protocol
  - ✅ @voxel-02/server
  - ✅ @voxel-02/client

  System-Architektur:

  Server (Port 3000)
  ├── WebSocket Server (Port 3000) - Game logic, chunks, registry
  └── HTTP Asset Server (Port 3001) - Assets (textures, models, audio)
      ├── GET /manifest - Asset manifest
      ├── GET /assets/{path} - Asset files
      └── GET /health - Health check

  Client
  ├── WebSocket Client - Receives manifest & registry
  ├── ClientAssetManager - Manages asset loading & caching
  └── TextureAtlas - Loads atlas from server URL

  Das System ist bereit zum Testen! Der Server wird Assets dynamisch ausliefern und der Client lädt sie bei Bedarf.
```
---
Die Voxels sollen nun etwas weniger quaderartig werden, deshalb koennen die Ecken jetzt verzerrt werden.
In der Entity soll nun fuer jeden Block ein `edgeOffset` array von signet bytes (-127 bis 128) gespeichert werden.
Das Array soll fuer jede Ecke drei werte enthalten die angeben wie stark die ecke in x,y,z richtung verschoben wird. Negative
Werte verschieben die Ecke nach innen, positive nach aussen. `edgeOffset` muss nicht gesetzt sein. In diesem Fall wird der
Block wie bisher nicht verzerrt.

- Anpassen der Entity-Definition
- Anpassen der Darstellung im Client
- Anpassen der Generatoren. Durch die Verzerrung koennen nun Berge glatter dargestellt werden.
---
Liquide materialien koennen immer durch gegangen werden, also das collision management im client muss angepasst werden.
---
Stelle liquide materialien auch so dar. Beachte beim rendern im client die eigenschaften aus dem material
fluid, fluidDensity, viscosity.
---
Beachte beim Rendern die Eigenschaft 'shape' des materials. Rendere den CUBE entsprechend anders.
---
Beachte beim Rendern im client auch die Material Eigenschaft 'transparent'. Dabei soll die Textur transparent sein koennen.
---
Ich benoetige eine Console eingabe. Wenn der Key '/' gedrueckt wird soll das die console triggern / verbergen. Diese Console ist ein textfenster
mit scrolling und unten einer eingabe zeile. Wird die console gezeigt, springt der cursor in die eingabeeinforderung, 
wird die console verborgen, kann mit der maus wieder die welt gesteuert werden.

Eingaben in der console werden zu einem CommandController geleitet. Hier wird die eingabe geparst und entsprechende registrierte commands ausgefuehrt.
Es wird ein CmdExecutionContext erzeugt. Hier sind alle parameter abrufbar und kann die ausgabe geschrieben werden. Im Cmd wird dann
execute(context: CmdExecutionContext) aufgerufen.

Commandos starten mit einem command name und werden durch Leerzeichen parameter getrennt. Mit quotes kann man Parameter mit leerzeichen eingeben.
Wenn enter gedrueckt wird

Startet ein command mit '/' wird der gesamte commando zu einem speziellen ServerCmdController geleitet. Alle anderen 
Commandos werden im client verarbeitet.


Cmd:
- getHelp()
- getName()
- getDescription()
- execute(context: CmdExecutionContext)

Erste commandos sind:
- help - Listet alle commands auf
- position - Gibt die aktuelle position des spielers aus
- teleport <x> <y> <z> - Teleportiert den spieler zu den angegebenen koordinaten
- start - Teleportiert den spiele zu den Startkoordinaten
- fligh - Geht in den Flug modus
- walk - Geht in den Walk modus
---
Ein neuer Modus 'select' Modus kann an und ausgeschaltet werden.
Wenn selekt aktiuv ist, soll direkt vor der Kamera, im radius von 5 Blocks ein Block vor der Camera steht, soll dieser Block als aktueller Block angezeigt werden.
Der Block soll markiert werden, z.b. durch einen rahmen um den Block.
---
Anpassung an der Console:
- Beim starten soll die Console aus sein
- Ist die Console aktiv soll sie mit ESC geschlossen werden, nicht mehr mit '/', dann soll wieder die Maus-Steuerung uebernommen werden.
- Wenn Enter gedrueckt wird, soll der Cursor im Eingabefeld bleiben.
---
Neue Commandos:
- raster <on|off> - Schaltet die Raster-Ansicht ein oder aus
- collission <on|off> - Schaltet collission detection im Flugmodus ein oder aus
---
Es soll einen Editor geben, mit dem ich die Konfiguration von einem selektierten Block sehen und anpassen kann.
Der editor kann in der Konsole mit dem commando `editor <on|off>` aktiviert werden.
---
Im NormalGenerator soll wasser nicht als Shape CUBE, sondern FLAT erstellt werden.
---
Wasser und Lava soll sich wellenartig verhalten. Das soll direkt per shader umgesetzt werden. Parameter aus dem programm
sollen den shader steuern wie intensiv die wellen sind. z.B. geschwindigkeit und höhe und richtung.
Die parameter sollen per commando in der console eingestellt werden koennen. Lava und wasser separat.
```text
 # Wasser-Wellen steuern
  waterwave speed 2.0           # Geschwindigkeit erhöhen
  waterwave amplitude 0.3       # Wellenhöhe erhöhen
  waterwave frequency 3.0       # Wellenfrequenz ändern
  waterwave show                # Aktuelle Werte anzeigen
  waterwave reset               # Zurücksetzen

  # Lava-Wellen steuern
  lavawave speed 1.0            # Geschwindigkeit erhöhen
  lavawave amplitude 0.25       # Wellenhöhe erhöhen
  lavawave show                 # Aktuelle Werte anzeigen
  lavawave reset                # Zurücksetzen
  
 How It Works:

  - Water and lava blocks are now rendered with custom GLSL shaders
  - The vertex shader applies wave displacement to the top surface based on:
    - Time (for animation)
    - Wave speed, amplitude, and frequency parameters
    - Wave direction (X, Z components)
  - The fragment shader applies fluid coloring with lighting
  - Each fluid type has separate default parameters:
    - Water: speed=1.0, amplitude=0.1, frequency=2.0, direction=(1.0, 0.5)
    - Lava: speed=0.5, amplitude=0.15, frequency=1.5, direction=(0.7, 0.7)
  - Users can control wave parameters via console commands:
    - waterwave speed 2.0
    - lavawave amplitude 0.3
    - etc.

  The wave animations should now be visible on water and lava surfaces in the game!
  
```
---
## Editor

Das Editor-Fenster soll ausgebaut werden. Der Editor braucht zwei Modi. Einen Anzeigemodus und einen Bearbeitungsmodus.
Beide werden durch zwei Tabs umgesetzt.
Im Block-Info-Tab werden immer alle Eigenschaften des selektierten Blocks angezeigt. Druecke ich die Taste '.' und der editor ist aktiv,
wird der Bearbeitungsmodus aktiviert, d.h. der Editor springt in den Block-Editor Tab, das Formular wird aktiv und übernimmt die Daten 
des gerde selektierten Blocks.

Der Block-Editor Tab zeigt ein Formular in dem der die Eigenschaften des Block bearbeitet werden koennen. Eine Änderung soll sofort in
der Welt angezeigt werden. Blöcke die angepasst wurden (neu, bearbeitet, gelöscht) werden in einer Liste vermerkt. Ein Apply-All Button im Editor
schickt alle geänderten Blöcke an den Server. Ein Revert-All Button macht alle Änderungen rückgängig (betroffene chunk werden neu vomserver geladen). 
Ein Revert-Block Button macht Änderungen am aktuellen Block rückgängig.

Der New-Block Button erstellt einen neuen Block. Wenn der Editor aktiv ist kann das auch mit dem Key ',' gemacht werden.
Der Neue Block wird dann sofort im Block-Editor Tab angezeigt.

Aufbau des Editor-Fensters:
- Toolbar
  - Revert-Block Button
  - (Spacer)
  - Cancel-Editor-Mode Button - Formulare werden inaktiv, sprung in den Block-Info Tab
  - Apply-All Button
  - Revert-All Button
  - Revert-Block Button
  - (Spacer)
  - New Block Button
  - (Spacer)
  - Delete Block Button
- Tabs (Block-Info, Block-Editor, Block-Liste)
- Block-Info
  - Listet wie bisher alle Block-Eigenschaften auf.
- Block-Liste (Block-ID, Block-Name, Block-Shape, Block-Material)
- Block-Editor
  - Block-Position (read only)
  - Block-ID
  - Block-Shape
  - Block-Material
  - edge offsets
  - (alle anderen Eigenschaften des Blocks)
```text
  1. Zuerst die grundlegende Tab-Struktur und Toolbar erstellen
  2. Dann den Edit-Modus und Change-Tracking hinzufügen
  3. Keyboard-Shortcuts (. und ,) implementieren
  4. Server-Kommunikation für Apply/Revert
```
---
Wenn der Editor nicht aktiv ist und es wird der Key '.' drueckt, soll der Editor und select mode aktiviert werden.
---
Kurze Frage zum wording: Ich glaube das das Wort Block mehrfache Bedeutung hat. Es gibt eine Block Entitaet und ein Block Typ. Ausserdem gibt es Materialien. Wie sollen wir diese dinge nennen damit keine verwechlung auftritt?
```text
Aktuelle Konzepte im Code:

  1. BlockType (in @voxel-02/core):
    - Ist die Definition/Template eines Blocks
    - Enthält: id, name, shape, texture, material, properties
    - Beispiel: "grass", "stone", "water"
    - Wird im Registry gespeichert
    - Das ist der Typ/die Art des Blocks
  2. Block Instance (in der Welt):
    - Eine konkrete Block-Instanz an einer Position (x,y,z)
    - Hat nur eine BlockType-ID (referenziert den BlockType)
    - Wird im Chunk-Daten-Array gespeichert
    - Das ist ein konkreter Block in der Welt
  3. Material:
    - Kann zwei Bedeutungen haben:
        - Rendering-Material (Babylon.js Material für Textur/Shader)
      - Block-Material-Eigenschaft (z.B. "stone", "wood", "glass" als logische Eigenschaft)

  Vorschlag für klare Begriffe:

  BlockType         -> "BlockTyp" oder "Blockart" (Definition)
  Block Instance    -> "Block" oder "Blockinstanz" (konkret in der Welt)
  Material (logic)  -> "Material" (logische Eigenschaft des BlockTyps)
  Material (render) -> "Rendering-Material" oder "Shader-Material"

  Im UI könnte man zeigen:

  Block-Info Tab:
  - Position: (x, y, z) → Die konkrete Block-Instanz
  - Blocktyp: "grass" → Der BlockType name
  - Blocktyp-ID: 2 → Die BlockType id
  - Material: "grass" → Die Material-Eigenschaft

  Block-Editor Tab:
  - "Blocktyp-ID ändern" statt "Block ID"
  - Damit wird klar: Man ändert welcher Typ von Block an dieser Position ist
```
Dann nennen wir das folgendermassen:
- BlockType oder Type: Block-Definition
- BlockInstance oder Instance: Block-Instanz
- Material: Block-Material-Eigenschaft (logical)
- RenderingMaterial: Shader (Babylon.js Material)
Schreibe das in die CLAUD.md Datei.
Ergänze noch informationen, z.b. wie und wo das dann im code definiert ist.
---
Nenne Block-Liste im Editor in Block-Type Liste um.
---
Es wird ein weiterer Tab im Editor benötigt, hier werden alle geänderten Block-Instanzen aufgelistet
die noch nicht zum Server gesendet wurden.
---
> Block-Editor Formular: Das Feld shape soll eine drop-down box werden. Alle Shape-Typen werden in der drop-down box angezeigt.
---
## Erweiterung Block-Instanz

Frage: Die definition wie ein Block ist soll eigentlich individuell fuer jeden Block definiert werden.
Macht es sinn Block Typ zu entfernen und immer alle Parameter an der Block Instanz zu haben?
```text
 Aktuelles System (BlockType + BlockInstance):

  Vorteile:
  - ✅ Speicher-Effizient: Millionen von Grass-Blöcken teilen sich eine BlockType-Definition
    - Pro Block: nur 2 bytes (Uint16 ID)
    - Chunk mit 32×256×32 = 262,144 Blöcken = ~512 KB
  - ✅ Netzwerk-Effizient: Registry wird einmal synchronisiert, dann nur IDs übertragen
  - ✅ Standard in Voxel-Games: Minecraft, Minetest nutzen dieses Pattern
  - ✅ Einfache Updates: BlockType-Änderung betrifft alle Instanzen
  - ✅ Performance: Schneller Lookup, weniger Daten zu verarbeiten

  Nachteile:
  - ❌ Keine individuellen Properties: Jeder Grass-Block ist identisch
  - ❌ Limitierte Flexibilität: Spezielle Blöcke brauchen Workarounds

  Alternative: Alle Parameter pro Instanz:

  Vorteile:
  - ✅ Maximale Flexibilität: Jeder Block kann unique sein
  - ✅ Keine Type-ID nötig: Direkte Property-Definition

  Nachteile:
  - ❌ Speicher-Explosion:
    - Aktuell: 2 bytes/Block
    - Neu: ~50-100+ bytes/Block (shape, texture, material, options...)
    - Chunk: von 512 KB → 13-26 MB (25-50x mehr!)
  - ❌ Netzwerk-Overhead: Jeder Block = vollständige Definition übertragen
  - ❌ Render-Performance: Mehr Daten zu parsen beim Mesh-Building
  - ❌ Komplexität: Schwerer zu optimieren und zu cachen

  Empfohlener Hybrid-Ansatz:

  // BlockType bleibt für Standard-Properties
  interface BlockType {
    id: number;
    name: string;
    shape: number;
    texture: string | string[];
    // ... standard properties
  }

  // BlockInstance mit optionalen Override-Properties
  interface BlockInstance {
    typeId: number;              // Standard: Referenz zu BlockType
    metadata?: {                 // Optional: Individuelle Properties
      customTexture?: string;
      rotation?: number;
      customColor?: [r, g, b];
      nbt?: Record<string, any>; // Arbitrary data (Minecraft-style)
    };
  }

  Vorteile des Hybrid-Ansatzes:
  - Standard-Blöcke: Nur 2 bytes (99% der Blöcke)
  - Spezielle Blöcke: Metadata für Customization (Chests, Signs, etc.)
  - Chunk-Daten: Haupt-Array (Uint16) + Sparse Metadata-Map
  - Speicher: Effizient für normale Blöcke, flexibel für spezielle

  Aktuelle Code-Stelle für Metadata:
  // ChunkData hat bereits metadata!
  interface ChunkData {
    data: Uint16Array;          // Block IDs
    metadata?: Uint16Array;     // ← Schon vorhanden, aber wenig genutzt!
  }

  Meine Empfehlung:

  Behalte BlockType + BlockInstance, aber:
  1. Nutze metadata für individuelle Properties
  2. Erweitere ChunkData.metadata für flexible Block-Daten
  3. Implementiere Metadata-Editor im Block Editor

  Das gibt dir Flexibilität wo nötig ohne Performance zu opfern.

  Soll ich das Metadata-System ausbauen statt BlockTypes zu entfernen?
```
Frage: Wenn ich Block-Instanz erweitere um ein Modfierer-Objekt, dann kann ich einzelne Eigenschaften
fuer jeden Block individuell setzen. Wie kann ich das machen?
---
Ich moechte an der Block-Instanz die gleichen oder mehr Parameter wie am Block-Type definieren koennen.
- An der Block Instanz ein Modfierer Objekt anhaengen das auch null sein kann.
- Jeder Parameter am Modfierer Objekt kann individuell gesetzt werden.
- Die Daten des Modifier Objekts muessen serialisierbar sein und per Netzwerk übertragen werden.
- Beim Laden im Clienten soll an jeder Block Instanz das einsprechende Block-Typ Objekt angehaengt werden, dieses wird
  überall benutzt (kein separaten lookup des Block-Typ Objekts).
- Wurde ein Modifier an der Instanz angehaengt, wird nicht das originale Block-Typ Objekt benutzt, sondern ein neues Objekt
  das aus dem Block-Typ Objekt kopiert wurde und die Modifier Eigenschaften ueberschreibt.
- Der Generator muss aktuell noch keine Modified Block Instanzen generieren.
- Der Editor muss angepasst werden, dass er Modifier Objekte an Block-Instanzen bearbeiten kann.
---
Beim speichern des Chunks muss das Modfierer Objekt auch gespeichert werden.
---
Das Formular des Block-Editor muss neu erstellt werden:
- Block ID Typ Änderbar (drop down)
- edge offsets: Aufklappbar, wenn zu geklappt wird ist edgeOffset nicht gesetzt 
  - Für jede Ecke ein Offset (x, y, z) von -127 bis 128
- Modifier: Aufklappbar, wenn zu geklappt wird ist modifier nicht gesetzt
  - displayName
  - shape (drop down von BlockShape)
  - texture (drop down + freie eingabe)
  - solid (checkbox)
  - transparent (checkbox)
  - unbreakable (checkbox) 
  - rotation (drop down)
  - facing (drop down)
  - color (erstmal nur als hex eingabe)
  - scale (x, y, z) float werte
  - Block Options: Aufklappbar, wenn zu geklappt wird ist blockOptions nicht gesetzt
    - solid (checkbox)
    - opaque (checkbox)   
    - transparent (checkbox) 
    - material (drop down von BlockMaterial)
    - fluid (checkbox)
    - fluidDensity (float)
    - viscosity (float)
---
Die Felder fuer den edgeOffset können jeweils für jede Ecke X Y Z in eine Zeile gelegt werden.
---
Im Editor die Felder fuer modifyer scale X Y Z koennen auch in eine Zeile gelegt werden.
---
Die Funktion 'Neuer Block' funktioniert nicht. Die funktion muss noch ueberarbeitet werden.
Wenn der Button 'Neuer Block' gedrueckt wird, soll der 'Selekt for new' modus aktiv werden.
Dabei koennen nur freie (air) Positionen selektiert werden. Die stelle wird entsprechend
wie bei select mit einem rahmen markiert. Wenn nun 'Cancel' gedrueckt wird, wird wieder in den
normalen Select Modus gewechselt. Wird der Editor mit dem Key '.' aktiviert und ein Air Block
ist ausgewählt, wird automatisch an dieser stelle ein Block erstellt.

Die umsetzung der beiden selekt modus ueberlasse ich dir.
---
Die suche soll direkt vor der kamera im abstand 1 einen air block selektieren, wenn dort ein Block bereits ist (nicht air), verschwindet wird die selektor umrandung rot, weil dort kein neuer block erstellt werden kann.
---
Der Abstand fuer einen Air Block sollte doch lieber 3 bis 1 sein. Das heisst es wird ein Freier Block bei 3,dann 2 dann 1 gesucht.
---
Nenne die Funktion Cancel im Editor um in 'Accept'. Wenn im Editor Modul ESC Key gedrueckt wird, soll die gleiche Funtkion wie bei Accept ausgefuehrt werden.
---
Programmatischer Swtch in den Pointer Lock funktioniert nicht, deshalb ausbauen bei Accept und New Block.
Bei New Block, Apply All, Revert All, Delete bitte auch ausbauen.
---
Reject All muss vermutlich den chink neu vom Server laden und rendern.
---
'Apply All' soll per Netzwerk die geänderten Bloecke (new, modify, delete) an den Server senden. Der Server baut das dann in den chunk ein.
- Netzwerk protokoll pruefen und erweitern, liste von Block-Instanzen senden
- Im Client diese funktion nutzen und die Changed-Block liste clearen.
- Im Server die Bloecke einbauen in den Chunk (wird automatisch gespeichert) und neu an alle clients schicken.

## Bug

[x] Beim Login bricht die erste Verbindung ab, beim zweiten Mal funktioniert es.
---
[x] Wenn Zwei Blöcke uebereinander sind und man steht direkt davor (wie vor einer mauer), dann schaut man durch den 
oberen Block durch. Vermutlich muss die Cam ein kleines stueck weiter zurueck, oder man sollte etwas eher stehen 
bleiben. Wichtig ist, das man auf dem Voxel steht, der vor der Mauer ist.
```text
Das ist ein klassisches Kamera-Problem in Voxel-Engines: Die Kamera ist zu nah am Spieler, sodass die Near-Clipping-Plane den oberen Block abschneidet. Lass mich die Kamera-Einstellungen überprüfen:
```
---
[x] Wenn ich im Editor auf den Tab 'Block-Type-Liste' oder 'Änderungen' gehe, geht die Maus noch in den Pointer-Lock Modus.
---
~~[ ] Im Block-Editor funktioniert der Button 'Apply All' nicht mehr. Vermutlich ein Server Problem.~~
[x] Wenn ich im editor von Sphere auf etwas anderes wechsle, wird die Sphere nicht geloescht sondern ist zusaetzlich noch da.
[ ] Der eine teil des CROSS ist nur von einer Seite sichtbar.

[x] Die felder fuer scale (XYZ) im Block-Editor lassen sich nicht richtig bedienen, es lassen sich keine kommazahlen
eingeben, nicht richtig loeschen. Das Feld windLeverUp funktioniert da sehr gut, kann das genauso konfiguriert werden?
> change erst bei ENTER oder Focus lost

## Bedienung

[x] Mit den Tasten Z und X soll man sich hoch und runter Bewegen wenn man im Flugmodus ist.
Im Walk Modus wird der Kammera-Winkel nach oben und unten gedreht.

[x] Wenn der Block_Editor aktiv werden soll und 'Cannot activate edit mode: No block selected' erscheint, dann soll der 'New Block' Modus aktiv werden.

[x] Im Block-Editor für color den ColorPicker von BabylonJS GUI verwenden.

[ ] Dropdown soll eine option an der componente haben, die ein suchfeld anzeigt. wird etwas gesucht, dann reduzieren sich die options.
[ ] Dropdown soll eine option an der componente haben, die es ermoeglicht in eine textfeld oben den Wert frei einzugeben. Bei Enter wird der Wret uebernommen.

[ ] Im Flugmodus soll mit Grossbuchstaben (kein CAPSLOCK) die Kamera um einen Punkt bewegen werden. Der Punkt soll zwei von der Kamera entfernt sein. Dieser Abstand soll in der Console mit einem command änderbar sein.
- Q und E: Roteiren linkls und Rechts um den Punkt herum
- Z und X Rotieren hoch und runter um den Punkt herum
 ```
Orbit Camera Controls (Flight Mode):
  - Shift+Q: Rotate left around focus point
  - Shift+E: Rotate right around focus point
  - Shift+Space: Rotate up around focus point
  - Shift+xx: Rotate down around focus point

  Console Commands:
  - orbitdist - Show current orbit distance (default: 4 blocks)
  - orbitdist 5 - Set orbit distance to 5 blocks
  - orbitdist 10.5 - Set orbit distance to 10.5 blocks
 ```

[x] Wenn ich im editor in einem Textfeld bin und '.' oder '/' druecke, dann wird der Shortkey aktiv, an der stelle soll aber das textfeld den key konsumieren.
[x] Wenn ich im Editor anfange einen bestehenden Block zu editieren, springen immer alle parameter auf 'default' anstelle die daten des block zu kopieren. 
[x] Im Block Editor ist oben, unter den Tab-Buttons bis zum Tab-Content viel Platz leer gelassen


[x] Copy/Paste: Wenn ein Block selektiert ist, brauche ich im Editor einen Button 'Copy'. Das merkt sich alle Einstellungen des Selektierten Blocks in einem Puffer
Wenn ich im Block-Editor modus bin brauche ich einen Button 'Paste'. Wenn ich auf 'Paste' druecke, dann wird der Puffer in den Editor kopiert.
- Der 'Copy' Button ist nur im Block-Info sichtbar 
- Der Paste Button ist nur im Block-Editor sichtbar
- Die beiden Button koennen auch der gleiche sein, nur der titel wechselt, wenn der Tab wechselt, es reicht wenn dort drin steht 'C' copy, 'P' Paste und '-' wenn nicht moeglich (nichts im CopyPaste Puffer)
- Paste kann auch im Neuer-Block-Select modus durch drücken des Key ',' aktiviert werden. d.h. der block an die neue stelle kopiert, der Neuer-Block-Select modus bleibt an
- Wenn ein Block im Copy-Puffer ist, wird im Info-Editor oben eine kurze info angezeigt

## Rendern

[x] Beim Rendern werden Blocks mit shape Spheren als Cube dargestellt.
[x] Es koennten noch die Werte aus edgeOffsets genutzt werden um die Sphere zu manipulieren. Ein Offset von -127 bis 128.
- Left-Back-Button um die Sphere zu verschieben
- Right-Back-Button um den Radius (X Y Z) oben zu verkleinern
[x] Beim Rendern werden Blocks mit shape Column als Cube dargestellt, nicht als Column.
[x] Es koennten noch die Werte aus edgeOffsets genutzt werden um die Column zu manipulieren. Ein Offset von -127 bis 128. 
- Left-Back-Button um die Column unten zu verschieben
- Left-Back-Top um die Column oben zu verschieben
- Right-Back-Button um den Radius oben zu verkleinern
- Right-Back-Top um den Radius unten zu verkleinern
[x] Beim Rendern werden Blocks mit shape Cross nicht richtig dargestellt, es wird nur ein teil des cross angezeigt.
[x] Es koennten noch die Werte aus edgeOffsets genutzt werden um das Cross zu manipulieren. Ein Offset von -127 bis 127.
Wie bei Cube auch, koennen die Werte alle 8 Ecken des Cross verschieben.
[x] Beim rendern wird der Parameter 'rotation' nicht beruecksichtigt. Der Parameter kann alle shapes beeinflussen.
[x] Beim rendern wird der Parameter 'facing' nicht beruecksichtigt. Der Parameter kann alle shapes beeinflussen.
[x] Beim rendern wird der Parameter 'color' nicht beruecksichtigt oder aus dem Editor nicht richtig übernommen. Der Parameter kann alle shapes beeinflussen.
[x] Beim Rendern werden Blocks mit shape Hash nicht richtig dargestellt, sie werden wie Cube dargestellt.
- Es sollen unabhängige seiten gerendert werden die mit edgeOffset verschoben werden koennen, sie bleiben dabei aber als Quadrate erhalten.
- Es werden also nicht die Ecken verschoben, sondern die Seiten. 
- Bitte rotationMatrix beachten.
```text
  | Fläche             | Corner Index | Array Indices | Beschreibung                              |
  |--------------------|--------------|---------------|-------------------------------------------|
  | Top (oben)         | Corner 0     | [0, 1, 2]     | Verschiebt die obere Fläche (y = y + 1)   |
  | Bottom (unten)     | Corner 1     | [3, 4, 5]     | Verschiebt die untere Fläche (y = y)      |
  | North (vorne, +Z)  | Corner 2     | [6, 7, 8]     | Verschiebt die vordere Fläche (z = z + 1) |
  | South (hinten, -Z) | Corner 3     | [9, 10, 11]   | Verschiebt die hintere Fläche (z = z)     |
  | East (rechts, +X)  | Corner 4     | [12, 13, 14]  | Verschiebt die rechte Fläche (x = x + 1)  |
  | West (links, -X)   | Corner 5     | [15, 16, 17]  | Verschiebt die linke Fläche (x = x)       |
```
[x] Wenn bei Hash eine edgeOffset X einen wert von 127 hat, soll die seite nicht angezeigt werden. So koennen wir durch einen trick die seite ausblenden.
[x] Es fehlt ein rotationsparameter wir koennen aktuell nur in eine richtung rotieren, sollten aber auch in die andere richtung rotieren koennen.
Die Idee, wir rotieren erst in die eine richtung (schon implementiert) und dann in die andere richtung (noch nicht implementiert) und koennen den block dann in alle positionen rotieren.
- Diese Parameter soll in BlockModifier gesichert werden.
- Er soll im Block-Editor editierbar sein und sofort live zu sehen sein.
[x] Rotation in 1-360 Grad in 1er schritten für beide Richtungen.
- Anpassen des Renderns
- Im Block Editor aus Dropdown ein Eingabefeld machen, die beiden Eingabefelder X Y koennen in eine Zeile gelegt werden.
- Muss beim Netzwerktransport etwas gemacht werden?
[x] Beim Rendern werden Blocks mit shape Glass nicht richtig gerendert. Es soll ein durchsichtiger Cube sein an dem die Textur angezeigt wird.
- Erstelle eine GlassRenderer.ts Datei
- Die Textur soll Transparent sein.
[x] backFaceCulling = false soll immer gestetz sein wenn an einem material transparent aktiv ist. ggf braucht es material in mehreren auspraegungen um die modifier am Block gerecht zu werden. die materialien sollten bei anforderung generiert und
  gechached werden. Es kann hier ggf noch weitere eigenschaften geben, die es erfordern materialien in weiteren auspraegungen doppelt vorzuhalten, z.b. leaves mit WindShader und ohne WindShader.  Gib es dafuer einen service der das uebernummt, z.b.
  einen MaterialManager oder MaterialService?
- die getMaterial() methode braucht keine texture als uebergabe. der MaterialManager hat zugiff, bzw. verwaltet alle Material Definitionen (BlockType) und Texturen und Shader. und an der Block instanz haengen alle informationen wie BlockType
  BlockModifier um das material zu bestimmen. du kannst noch ein 'private createMaterialKey(block) : string' hinzufuegen.
[x] Wenn am Block 'transparent' aktiviert ist, sollen die Texturen durchscheinen wo sie als transparent markiert sind. Das funktioniert nicht. Bitte alle Shade Renderer prüfen.
[x] Wenn ich die '\' taste druecke werden debug informationen in der console ausgegeben, gib hier auch informationen ueber den selektierten block aus. So dass
ich die informationen an dich weitergeben kann und du die konfiguration der blocks und des materials analysieren kannst.
[x] Das sollte im ChunkRenderer genauso wie schon im MaterialManager generalisiert werden. wenn du die bloecke durch gehst, 
kannst du im MaterialManager fragen wie der material-Key ist. Fuehre eine Map(key,Container) in der du diese materialien und 
eben das mega-mesh fuer das material und ggf andere werte (indices, positions, uvs, colors, ...) haelst, gibt es den key 
noch nicht, dann anlegen, wenn es den key schon gibt, kannst du den block hinzufuegen. So kannst du generalisiert damit
umgehen und nicht hart codiert fuer jedes material. Ausserdem ist die auswahl, welche eigenschaften zu welchem material 
gehoeren zentral im MaterialManager. Wuede das so klappen oder gibt es edge cases?
[x] Jetzt kannst du das hinzufuegen der bloecke auch an die Shape-Renderer deligieren, was machst du, wenn ein Block aus mehreren materialien besteht. -  Erweiterung des BlockRenderContext
[x] Erstelle ein commando, das den aktuellen chunk neu rendert und ein command das alle visible chunks neu rendert.
```text
 Created two command classes following the existing pattern:
  - RechunkCommand - /rechunk command for current chunk
  - RechunkAllCommand - /rechunkall command for all visible chunks
```

[x] Neuer Shade: Steps - Macht einen zwischen step/treppe im cube, eine stufe
Es soll einen neuern shader 'steps' geben, der einen cube in eine treppenform bringt.
Es wird eine zwischen-step eingebaut, so dass ich mehrere blocke aneinander reihen kann um eine treppe zu bauen.
- (?) edgeOffset wird genutzt um die ecken zu verschieben (wie bei cube)
- rotationX, rotationY dreht das treppen teil (wie bei cube)

[x] Neuer Shade: Stair - Macht einen zwischen treppe im cube, eine stufe
Es soll einen neuern shader 'stair' geben, der einen cube in eine treppenform bringt. Im gegensatz zu steps ist die Treppe unten ausgefüllt.
Es wird eine zwischen-step eingebaut, so dass ich mehrere blocke aneinander reihen kann um eine treppe zu bauen.
- rotationX, rotationY dreht das treppen teil (wie bei cube)
- Es kann grundsaetzlich StepRenderer uebernommen werden
- Im Editor den neuen shader in das dropdown übernehmen
 
[x] Neuer Shade: Billboard (assetPath)
Es soll einen neuen Shade 'billboard' geben, der eine Textur auf dem Block anzeigt.
- Parameter scaling manipuliert die größe des Billboards, kann auch größer als 1 sein.
- edgeOffset das erste XYZ im Array manipuliert/korrigiert den Mittelpunkt des Billboards (ahnlich wie bei shade Column)
- Im Editor den neuen shader in das dropdown übernehmen
- Der Parameter Facing dreht die Billboard Texture.

> Neuer Shade: Sprite, ist aus Billboard entstanden als eine von zwei Optionen der Umsetzung.
```text
Option 1: Mesh.billboardMode (Einfachste Lösung)

  Babylon.js Meshes haben eine billboardMode Eigenschaft:

  mesh.billboardMode = Mesh.BILLBOARDMODE_ALL; // Dreht sich komplett zur Kamera

  Problem: Das funktioniert nur für einzelne, separate Meshes - nicht für Vertices in einem kombinierten Chunk-Mesh.

  Option 2: Separate Billboard-Meshes

  Wir könnten Billboard-Blöcke außerhalb des Chunk-Mesh-Systems rendern:
  - Jedes Billboard = eigenes kleines Mesh
  - Nutzt mesh.billboardMode = Mesh.BILLBOARDMODE_ALL
  - Babylon.js kümmert sich automatisch um die Rotation

  Vorteil: Nutzt Babylon.js Built-in FeaturesNachteil: Weniger effizient bei vielen Billboards (mehr Draw Calls)

  Option 3: SpriteManager (Für viele kleine Billboards)

  Für viele kleine Billboards (wie Gras, Blumen):
  const spriteManager = new SpriteManager("billboards", "texture.png", 1000, 64, scene);

  Welchen Ansatz bevorzugst du?

  Ich würde Option 2 empfehlen: Billboard-Blöcke als separate Meshes mit billboardMode. Das ist einfach zu implementieren und nutzt Babylon.js' eingebaute Funktionalität. Soll ich das so umsetzen?
```
```text
 Das SpriteManager System funktioniert anders:
  - Es verwaltet viele Sprites mit einer einzigen Textur
  - Sprites sind immer camera-facing (billboarded)
  - Sehr performant für tausende von Sprites (Partikel, Gras, etc.)

  Ich muss dies jedoch anders angehen, da SpriteManager pro Textur erstellt wird und nicht pro Block. Wir bräuchten einen zentralen SpriteManager pro BlockType-Textur im ChunkRenderer oder einem höheren Level.

  Soll ich:
  1. Einen SpriteManager pro Chunk erstellen (ein Manager pro Textur im Chunk)?
  2. Einen globalen SpriteManager pro Textur in der Scene/WorldRenderer verwalten?

  Außerdem: Sollte SPRITE weiterhin X-förmig sein (2 diagonale Quads wie jetzt), oder einfach ein einzelnes camera-facing Quad wie Babylon.js Sprites normalerweise sind?
```
[x] Sprites mit SpriteManager umsetzen
```text

  1. Sprite-Anzahl: Welche Option bevorzugst du? (A: feste Zahl, B: Dichte, C: Range)
  2. Positionierung: Random, Grid, oder edgeOffsets nutzen?
  3. Standard-Wert: Wie viele Sprites sollen Standard sein, wenn nicht angegeben? (z.B. 1, 3, 5?)
  4. Sprite-Größe: Soll das über modifier.scale gesteuert werden, oder separate Parameter?
```
- Sprite-Anzahl: Feste Anzahl 'spriteCount' pro Block
- Positionierung: default Random, wenn edgeOffset nicht gesetzt, sonst edgeOffset
- Standard-Wert: 5 (kann spaeter noch angepasst werden)
- Sprite-Größe: ja ueber modifier.scale gesteuert
[x] Sprites sollen sich auch leicht bewegen, koennen wir da wind parameter aus der App benutzen die wir schon haben?
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway
Eine einfache implementierung wuede ich bevorzugen.


> [x] Es soll immer der Parameter 'texture' im BlockModifier geprueft werden und ggf. eine andere Texturen-Datei geladen werden.
- Verschiedene Texturen werden durch Komma getrennt, dann wird ein Array daraus.
- Wichtig bei shade Type BILLBOARD und SPRITE
- Texture cache, damit eine textur nur einmal in die 3D engine geladen werden muss? Schon vorhanden? TextureManager im MaterialManager?
- Muss das dnn jedesmal ein neues material werden? Anpassung von createMaterialKey() in MaterialManager?
- Wenn ich jetzt noch Atlas Funktionalität hinzfügen möchte, also eine Atlas-Textur und daraus nur einen Ausschnitt als Textur nutzen möchte. Am besten am texturnamen dahinter mit ':' und innen einen index, like :x,y,b,h ist das schlau?
- Dann sollten wir vorher noch das Thema custom Texturen umsetzten und danach Spritemanager bauen. Ich habe zwei sachen zu Texturen: 1. Texturen im BlockModifier ueberschreiben fuer einzelne Bloecke, mit komma separiert fr ein Array, hier muss im
  materialKey dann der texturePath mit eingebaut werden. 2. An Texturen noch einen Atlas Index anhaengen mit ':', z.b. texturPath=assets/foods:5,3 oder so aehnlich. Hat den vorteil das wir fuer atlas daten keine neuen Parameter bauen muessen, das
  auch im BlockModifier.texturePath funktioniert und das dann auch im materialKey gleich drinn ist. Frage ist wie wir hier am besten den Atlas Index hinterlegen, z.b. path:x,y,width,height
- Der BlockModifier hat schon das feld, es heisst 'texture' das Feld ist immer ein String und soll dann beim verarbeiten zu einem Array werden. Wir haben hier ein syntaktisches problem. Wir sollten ';' als trenner nehmen dann crasht das nicht mit
  der atlas konfiguration, also: path:x,y,w,h;path....
```text
 Was NICHT optimiert ist (GPU-Atlas):

  ❌ Atlas-Slots: Jede Sub-Texture bekommt einen eigenen Slot
  - Verschiedene Bereiche derselben Datei werden mehrfach in den Atlas gezeichnet

  Ist das ein Problem?
  - Für die Performance: Nein, weil jede Sub-Texture unterschiedlichen Inhalt hat
  - Für den Speicher: Ja, wenn man viele Sub-Textures aus derselben Quelle nutzt
```

[x] Neuer Shade: Flame
Es soll einen neuen Shade 'flame' geben, der eine animierte flamme anzeigt.
- edgeOffset manipuliert/korrigiert den Mittelpunkt der flamme (ahnlich wie bei shade Column)
- scaling manipuliert die größe der flamme
- color verändert die Farbe der flamme
- Flame wird nicht NIE vom WindShader genutzt.
- Der Parameter windStability am Block kann deshalb für die Flamme genuzt werden und die stärke der Flamme zu bestimmen.
- Der Parameter windLeafinnes gibt an wie stark die Flamme flackert
- Es muss einen FlameShader geben, der die Flamme flackern lässt. Ihm werden auch die wind-Einstellungen aus der App mitgegeben, da sich die Flamme auch im Wind bewegen muss. 
- Flammen haben immer einen WindShader, der sie bewegt.
- windLeverUp und windLeverDown koennen wie im WindShader verwendet werden.
Nutze Babylon mittel:
```typescript
  var fireMaterial = new BABYLON.FireMaterial("fireMaterial", scene);
  fireMaterial.diffuseTexture = new BABYLON.Texture("diffuse.png", scene);
  fireMaterial.distortionTexture = new BABYLON.Texture("distortion.png", scene);
  fireMaterial.opacityTexture = new BABYLON.Texture("opacity.png", scene);

    var plane = BABYLON.Mesh.CreatePlane("fireplane", 1.0, scene);
    plane.material = fireMaterial;
```
Die Texturen koennen ueber die texture parameter im BlockType und BlockModifier gesetzt werden. Wenn diese Fehlen, werden default Texturen verwendet.
```text
FLAME Shape Implementation Complete

  I've successfully completed the FLAME shape implementation for your voxel engine. Here's what was done:

  Files Modified/Created:

  1. BlockType.ts (/packages/core/src/registry/BlockType.ts)
    - Added FLAME = 13 to the BlockShape enum
  2. FlameRenderer.ts (NEW FILE: /packages/client/src/rendering/shapes/FlameRenderer.ts)
    - Complete flame renderer implementation with FireMaterial support
    - Uses vertical plane meshes for flame geometry
    - Supports both textured (FireMaterial) and non-textured (StandardMaterial) rendering
  3. shapes/index.ts (/packages/client/src/rendering/shapes/index.ts)
    - Added FlameRenderer export
  4. ChunkRenderer.ts (/packages/client/src/rendering/ChunkRenderer.ts:79)
    - Registered FlameRenderer for FLAME shape blocks
  5. BlockEditor.ts (/packages/client/src/gui/BlockEditor.ts)
    - Added 'FLAME' to all three shape selection lists (lines 1379, 1807, 2009)

  Key Features Implemented:

  Material System:
  - With Texture: Uses Babylon.js FireMaterial with realistic animated flames
    - Supports diffuse, distortion, and opacity textures
    - Automatic flame animation based on windStability parameter
  - Without Texture: Falls back to StandardMaterial with emissive glow

  Block Properties Support:
  - edgeOffset: Adjusts flame center position (X, Y, Z from corner 0)
  - scale: Controls flame size (width, height, depth)
  - color: Tints flame color (default: orange [255, 200, 100])
  - windStability: Controls flame strength and animation speed
  - windLeafiness: Controls flicker intensity
  - windLeverUp/windLeverDown: Stored for future advanced animation

  Rendering:
  - Creates separate mesh (not part of chunk geometry)
  - Double-sided rendering (visible from all angles)
  - Semi-transparent (alpha 0.8 for StandardMaterial)
  - Non-pickable for performance optimization
```

[ ] Texture umbenennen in assetPath
[ ] Der shader 'model' soll ein babylon oder glb model laden und anzeigen (assetPath)
- ohne shema angabe in assetPath wird das model aus der asset ablage (wie textures) geladen.
- mit shema angabe http:// https:// wird das asset aus dem internet geladen
- Models werden wie alle anderen shades durch rotation, color beeinflusst
- edgeOffset verschiebt den model punkt auf dem cube
- Das Model wird bei default auf die groesse des cube gescaled (1/1/1).
- Mit 'scale' parametern in blockModifier kann das Model gescaled werden, auch goesser als 1, dabei bleibt es auf der Fläche XZ stehen
- Der Mittelpunkt des Models wird so gewählt, das es auf der XZ Fläche des Cubes stehen bleibt. (mit edgeOffset justierbar, scale vergroessert/kleinert das model)

[ ] (!) Bei shape Cross dreht sich ein teil des Cross um 180 Grad (oder spiegelt sich?)
- Es liegt am Facing, wenn ich mich um den cube herum bewege, springt die eine fläche um 180 Grad.
- Facing funktioniert nicht richtig auf die eine Fläche, bzw. Rotation
> Ist immernoch nicht ok, verschoben auf spaeter

[ ] Race condition mit textures und PNG transparenz

## Cleanup

[ ] Gerade ziehen von Metadaten: an Block-Typen und Instanzen
> Wie genau ist die Struktur? 
> BlockMaterial genauer ansehen. Typ 'barrier' weg, soll hier 'flame' oder 'lightning' eingebaut werden oder als shape? Oder beides?
> Was macht eigentlich BlockMaterial 'gas' ???
> Es muessen nicht alle Kombinationen funktionieren, z.b. BlockMaterial gas mit Wind.
> ToolType muss weg.
 

[x] Custom Testures: wird in assetPath eine Textur angegeben, dann wird diese verwendet. Mehrere Texturen können mit komma getrennt eingegeben werden (Array)
- Custom Texturen wirken auf die meisten Shapes, ausser Model, Flame, Water, Lava.

## Block Editor Metadata

[x] Aus BlockMetadata kann 'rotationAxis', 'rotationStep',' 'facing', 'state' entfernt werden
[x] Name soll von BlockModifier in BlockMetadata verschoben werden
[x] BlockMetadata soll auch im Editor als separate Aufklappbox "Metadata" bearbeitbar sein. displayName ist dann dort im formular.
[x] Group Id (string) soll in BlockMetadata angelegt werden, im editor bearbeitbar sein.
[x] Block-Info erweitern um BlockMetadata, BlockModifier, BlockType informationen, zu welchem chunk der block gehört
[ ] Facing Dropdown zeigt im Button immer ... anstelle des wertes an, aber das Element ist breit genug um den text anzuzeigen
[ ] Die Wind Parameter Leafiness und Stability sollen in eine Zeile gelegt werden.
[ ] Die Wind Parameter Lever Up und Lever Down sollen auch in eine Zeile gelegt werden.
[ ] Das Scrolling im Editor und auch Dropdown ist zu schnell, kann man das verbessern?
---
[ ] Erstelle am Editor einen Close Button (evtl ein Cross Icon) mit dem der Editor deaktiviert und select modus ausgeschalten wird.

## Wind

Erster Schritt Block Parameter:

[x] Es soll Wind geben. Folgende Parameter sollen am Block-Type erweitert werden:
- windLeafiness (float) [0..1] → 1 bei Blatt-Typen, sonst 0.
- windStability (float) [0..1] → z. B. Blatt 0.1, Holz 0.6, Stein 0.95.

[x] An der BlockInstanz wird der Folgende parameter an BlockModifier benötigt:
- windLeverUp (float) [0..n] → wie weit oben im Stack der Block sitzt oben am Cube (Hebel).
- windLeverDown (float) [0..n] → wie weit oben im Stack der Block sitzt unten am Cube (Hebel).
- Im Block-Editor editierbar machen
- Netzwerk und Speicher implementieren.

---
Zweiter Schritt Wind Parameter in der App:

[x] Es sollen die Parameter in der App mit Kommandos änderbar sein:
- windDirection (x,z) - direction of wind
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway
Setze sinnvolle default werte.
---
Dritter Schritt Implementierung am Material:

[x] Im Renderer soll ein Shader die Windbewegung umsetzen.

Aus folgenden Parametern:
- windLeafiness (float) [0..1] → 1 bei Blatt-Typen, sonst 0.
- windStability (float) [0..1] → z. B. Blatt 0.1, Holz 0.6, Stein 0.95.
- windLeverUp (float) [0..n] → wie weit oben im Stack der Block sitzt oben am Cube (Hebel).
- windLeverDown (float) [0..n] → wie weit oben im Stack der Block sitzt unten am Cube (Hebel).
- windPhase - Ergibt sich aus den Coordinaten
und
- windDirection (x,z) - direction of wind
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway

Nur aktiv, wenn am BlockType windLeafiness > 0 oder windStability > 0 ist.

Soll in diesem Schritt eine einfache Windbewegung (windLever + windStrength) um zu testen ob der Wind funktioniert.
---
[x] Am BlockType 'leaves' soll ein windLeafiness=1 configuriert werden.
---
Jetzt soll der Wind komplett umgesetzt werden. Der Shader soll alle Parameter benutzen um eine realistische Windbewegung umzusetzen.
---
[x] windLever an der BlockInstanz soll nun in windLeverUp umbenannt werden, ausserdem soll es ein
windLeverDown (float) [0..n] am BlockInstanz geben. Bitte an der Instanz, Networking, Chunk Persistenz, Block-Editor und WindShader umsetzen.
[x] windLeverUp ist ein Wert, der oben am Cube gilt und windLeverDown unten am Cube. Der Cube muss sich nun verbiegen (im Wind) zwischen unten und oben.
- Wenn zwei Bloecke Ubernander sind und der untere hat blockLeverUp den gleichen Wert wie der obere windLeverDown, dann sollen sie sich an der 
  Schnittstelle gleich verbiegen und somit nicht auseinander gehen.
- Die verbiegung ist eine Biegung/Krümmung um den Hebel sein. regulär hat der untere Block windLeverDonw=0 und windLeverUp=1 (1 Block hoch)
  definiert, der nächste Block hat windLeverDown=1 und windLeverUp=2 (1 Block runter) definiert. Wenn das material weicher ist, kann die differenz 
  zwischen Up und Down größer sein. Alle zusammen sollen sich wie eine Einheit verhalten.

[x] Die commandos fuer Wind geben einen Fehler aus, wenn ich einen Wert uebergebe, scheinen dann aber trozdem zu funktionieren. 
- Es soll ein fehler ausggeben werden, wenn der wert 'out of bounds ist'.

> Die Wirkung von parametern nochmal pruefen und ggf. anpassen lassen.
> Echte Wetter scenarien durch automatisch wechselnde Wetterparameter pruefen. Die können auch fuer chunks unterschiedlich sein um boehen / täler / berge bessser zu simulieren.
> Man koennte am chunk eine Information angeben, ob er eher auf dem Berg oder im Tal ist.

# Next Steps

===

## Entity

Es sollen zusaetzlich zu den Bloecken Entities (Entity) hinzugefuegt werden. Entities sind mobil koennen jederzeit auftauchen, sich bewegen und verschwinden.

Darstellung und Volumen:
- Entities haben ein Model, das gerendert wird. Eine .babylon odel .glb Datei kann aus den assets geladen werden.
- Entities haben eine Position und eine Rotation.
- Entities werden von einem Block-Raster umgeben, belegen also mehrere Block-Positionen. 
  - Diese ändert sich wenn sich das Object bewegt/rotiert.
  - Dieses Object-Block-Raster sind die physicalischen grenzen in der collision detection
- Entities haben Animationen definiert, die aktiviert und abgespielt werden (z.B. laufen)

Netzwerk:
- Ueber das Netzwerk registriert sich der Client an chunks. Immer mit einem befehl an einer liste von chunks. Er ist dann nur an diesen registriert, damit entfällt eine deregistrierung.
  Es kann eine Liste von Objekten gesendet werden (batch).
- Der Server schickt dem Client wenn ein Entity auftaucht (incl. aktuelle animation, rotation und position), der client laed das modell aus den assets und zeigt das Object an.
- Der Server schickt dem Client wenn sich ein Entity ändert (aktuelle animation, rotation und position). Es kann eine Liste von Entities gesendet werden (batch).
- Der Server schickt dem Client wenn ein Entity verschwindet. Es kann eine Liste Entities Objekten gesendet werden (batch). Das Entity wird im Client nicht mehr angezeigt. Ein Cache-System hält 
  das Entity noch einige zeit bevor das model verworfen wird.
- Dr Server schickt dem Client wohin sich ein Entity bewegen soll (route). Der Client laeuft die route ab. 
!!! Geht nicht, weil viele clients: Bei collision oder ende der route wird ein event an den server geschickt.

Server Demo:
- Der Server soll eine Liste von seagulf.glb ueber die landschaft laufen lassen. alle in der naehe von punkt (0/0) mit radius 100 Blocks.
- Der Server berechnet eine route schickt diese an clients, die sich registriert haben. 



> - Sprites sind auf einer Block Position
> - Haben eine Ausdehnung in Bloecken, dass kann eine Beliebeige, nicht sichtbare Anordnung von Bloecken sein, innen drin ist das Sprite
> - Die Ausdehnung in Bloecken kann Collidieren
> - Sind animiert
> - Haben vermutlich ein Mesh Model
> - Vom server koennen Bewegungs-Routen geschickt werden, die werden dann abgelaufen,
    >   - bei Collision abbruch, event an Server schicken
>   - bei ende der bewegung event an server schicken
>   - Routen koennen in einer queue gestackt werden, jeweils am ende ein event, dann kann der server schon das die naechste route schicken

## Code Optimieren

[x] Baue ein TransferObjekt das du add*Block() uebergibst, in dem Objekt sind alle wichtigen Referenzen und das Obejkt kann leicht erweitert werden
[x] Erstelle aus ChunkRenderner.ts eine class ShapeRenderer und fuer jeden shape Typ eine Ableitung. Jedes in eine eigene ts datei. 
~~[ ] Datei ChunkRenderner.ts sinnvoll weitere teile auslagern~~
[x] Datei BlockEditor.ts sinnvoll teile auslagern und so komplexitaet reduzieren.

# Backlog

## Editor Stylen

Das Aussehen des Editor soll verbessert werden. Ich moechte, dass
die Bedienelemente im Editor eleganter aussehen. Alle in der gleiche
Farbe, Hell, Icons? runde Ecken?.

## Gruppen

> Ein Block soll in einer Gruppe sein können, UUID/String
> Alle in einer Gruppe können gemeinsam verschoben werden
> Modell in JSON erstellen
> Bei Interaktion leuchten alle in der Gruppe

> Verschiedene Arten von Modellen: 
> - Block Model: Liste von Blöcken und deren Anordnung und Konfiguration
> - Mesh Model: Liste von Meshes und deren Anordnung und Konfiguration

## Interaktion

> Selektor findet einen Block, wenn block interaktiv ist, wird er oder die ganze Gruppe hervorgehoben
> Taste ?? startet interaktion - server kommunikation

## Effects

## Environment

> - Sonne, Wetter, Tag/Nacht, Zeit, Jahreszeiten

## Start und Login

> - Automatisch beim Server anmelden, Abfrage User/PW, Anzeige der moeglichen Eintrittspunkte
> - Automatischer Sprung in eine andere Welt, Weltraum Modus?

> Frage Wording: Planet -> Welt ? oder Planet (Welt) -> Kontinent/Ebene ? World -> Ebene (Plane)
> Frage Technik: Geht alles ueber EINEN Einstiegspunkt (volle Kontrolle) oder verbindet sich der Client direkt zu den Servern (mehr flexibel), ggf. Eine Zentrale Resourcenverwaltung (User, Items, Geld, Energie, ...)? 
> Frage Konzept: Energie als steuerndes Mittel einführen? Energie als gegepol zu Geld?

## Wasser

> Beim laden eines Chinks wird fuer jede XZ Column geprueft ob dort Wasser ist. Es wird sich die Ebenen (Y) und die Farbe gemerkt fuer diese Column
> Ist die Camera unter Wasser wird ein Wasser-Sphere um die Kamera gelegt in der Farbe des Wassers, simuliert wasser
> Im Walk modus und unter Wasser gibt es keine Gravitation mehr / Gravitation ist sehr gering / Ist die navigation wie im Flug-Modus (tauchen)

## Klettern

> Lauft man im Walk Modus an einen Block der 'Kletterbar' als Eigenschaft hat, bewegt man sich beim weiter laufen automatisch nach oben.
> Die Gravitation ist deaktiviert, solange man an einem Kletter-Block ist.
> - Pruefen ob das nur fuer seitliche Blocks gild. 
> - Wie ist das, wenn das der letzte Block, oben ist?

## Server Networking verbessern

[ ] Kann das senden der Chunks nicht kompakter geschehen, 
- indem daten die default sind weggelassen werden
- indem namen kuerzer oder durch ids ersetzt werden
- indem mit einer einfachen komprimierung gearbeitet wird

[ ] Die BlockTypen muessen vom Server geladen werden
- Ueber den gleichen weg wie die Assets aber dynamisch?

## Strukturierung von Block Eigenschaften

> Keine Block Optiosn mehr
> In Block Modifiers Dinge, die das Aussehen beeinflussen
> In BlockMetadata Dinge die das Verhalten beeinflussen
> BlockModifers sind: Material Eigenschaften, Positions Eigenschaften, Animations Eigenschaften
> BlockType
> Alle BlockMetadata sind auch im BlockTyp definiert

## Anderes

[ ] Flag an Block ob man da durchlaufen kann
[x] Im login den Knopf 'Raster' entfernen, dafuer eien Reihe von Buttons: Gaston, Dilbert, Popeye, Godzilla nebeneinander, 
- eine kann ausewählt werden.  
- 'Gaston' ist default
- 'Gaston' ist terrain load 1, unload 2
- 'Dilbert' ist terrain load 2, unload 3
- 'Popeye' ist terrain load 3, unload 4
- 'Godzilla' ist terrain load 4, unload 5

[x] SkyBox
Implementiere in einer Datei SkyManager.ts eien SkyBox mit Babylon.
Beispiel:
```typescript

    //SKY BOX
    var skybox = BABYLON.MeshBuilder.CreateBox("skyBox", { size: 1000.0 }, scene);
    var skyboxMaterial = new BABYLON.StandardMaterial("skyBox", scene);
    skyboxMaterial.backFaceCulling = false;
    skyboxMaterial.reflectionTexture = new BABYLON.CubeTexture("textures/skybox", scene);
    skyboxMaterial.reflectionTexture.coordinatesMode = BABYLON.Texture.SKYBOX_MODE;
    skyboxMaterial.diffuseColor = skyboxMaterial.specularColor = new BABYLON.Color3(0, 0, 0);
    skybox.material = skyboxMaterial;
```
Und nutze.