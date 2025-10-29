
# Migration client playground to v 2.0

## Basics

> rename client nach client_playground
> create new client folder for client 2.0

Zur Info: Der code in 'client_playground' ist ein erster prototyp für die 3D client engine.
Der neue Code soll in 'client' für die version 2.0 der client engine entwickelt werden.
Die Entwicklung soll schritt für schritt einen bereich nach dem anderen neu implementieren.
Die Erkenntnisse und verbesserungen aus dem prototypen sollen dabei einfliessen. Es wird
bei den Aufgaben referenzen auf den prototypen code in 'client_playground' geben.
Erstelle nicht selbstständig code im 'client' ordner ausser es ist in den aufgaben beschrieben.
Du sollst aber hinweise und verbesserungen für den prototypen code in 'client_playground' geben
die dann diskutiert und ggf. umgesetzt werden können.

Frontend 3D: TypeScript + Vite + Babylon.js 
Frontend Forms: TypeScript + Vite + Vue3 + tailwind css (Werden später entwickelt und per IFrame eingebunden) 
Testing: Jest (unit)
Package Manager: pnpm

Das Projekt soll in verschiedene packages aufgeteilt werden

shared: Code der sowohl vom client, Forms als auch vom server genutzt wird. Gemeinsame datentypen, protocoll definitionen, utils, etc.
client: 3D Engine code mit Babylon js, UI, Input handling, etc. Editor etc
server: Simple server implementation, die den client mit welt daten versorgt. Dies ist nicht der haupt fokus, sondern dient nur zum testen des clients.

Der client wird in verschiedenen builds in unterschiedlichen versionen bereitgestellt:
- viewer: Nur die 3D engine zum anschauen der welt
- editor: 3D engine + editor funktionen + console

[x] Erstelle in client eine CLAUDE.md datei mit den informationen zur nutzung von claude für die entwicklung.
[x] Erstelle in pnpm ein monorepo mit den packages: shared, client, server. Für Typescript. Die App dateien sollen NimbusClient und NimbusServer heissen.
[x] Erstelle builds im client für viewer und editor. Mit pnpm run build:viewer und pnpm run build:editor.
```text
Ein Code, aber per ENV/Define baust du ein Viewer- oder ein Editor-Artefakt. Unerreichbarer Code wird vom Bundler eliminiert.

// vite.config.ts
export default defineConfig(({ mode }) => ({
  define: {
    __EDITOR__: JSON.stringify(mode === "editor"), // bools als globale Konstante
  },
}));

// App.tsx
let Editor: React.ComponentType | null = null;
if (__EDITOR__) {
  Editor = React.lazy(() => import("./editor/Editor")); // wird aus Viewer-Build entfernt
}

{
  "scripts": {
    "build:viewer": "vite build --mode viewer",
    "build:editor": "vite build --mode editor"
  }
}
```
Passe die Docu an.
[x] Auch der server soll min pnpm erstellt und gestartet werden koennen. Implementiere:
- pnpm build:server
- pnpm build:client
- pnpm build:editor
- pnpm build:viewer
- pnpm build  (Build alle)
- pnpm dev:server
- pnpm dev:client
- pnpm dev:editor
- pnpm dev:viewer
...

[x] Fuege einen log-Framework hinzu.
[x] Erstelle einen Logger Writer, der zum testen im DEV mode die Logs auf die lokale HD speichern kann. Ist das moeglich?
[x] Erstelle ueberall, wo Fehler nicht mehr abgefangen werden keonnen, Try-Catch Bloecke und schreibe den Fehler in den Logger. Erweitere CLAUDE.md.
[x] Erstelle die ersten unit tests.
[x] Teste auch bestehende Klassen, z.B. BlockSerializer, ChunkSerializer, etc.

## Basic Shared Types

Zur Info: Jede Datenstruktur soll seine eigene Datei bekommen. Enums, status, etc. zu dem Type Können in der gelchen Datei definiert sein, solange sie nur für 
diesen Type relevant sind.

[x] Lege die Typen aus client/instructions/client_2.0/object-model-2.0.md in shared/types an.
[x] Lege die Network Messages aus client/instructions/client_2.0/network-model-2.0.md in shared/network an.
[x] Hast du Anmerkungen zu den erzeugten Typen?
[x] Kurze Anmerkung: Bitte Coordinaten immer richtig benennen: x, y, z für Welt-Coordinaten, cx, cy, cz für Chunk-Coordinaten, lx, ly, lz für Local-Coordinaten (vermeiden).
    Bitte füge das noch in die CLAUDE.md datei hinzu und aktualisiere ChunkData entsprechend.
[x] Passe noch client/instructions/client_2.0/object-model-2.0.md und client/instructions/client_2.0/network-model-2.0.md mit den Aenderungen an.
[x] Verschiebe alle Client related typen (ClientChunk ClientBlock etc.) in client/types.
[-] Verschiebe alle Server related typen (ServerChunk ServerBlock etc.) in server/types. (Gibt es welche?)

> Netzwerk Protokoll: 'ClientCommand' und 'ClientCommandResponse'
> Shape AIR wird BlockType AIR (0), Shape 0 wird INVISIBLE, Constante für BlockType AIR.
> Displayname und Name aus dem BlockMetadata entfernen. - Kann später über REST geladen werden.
> Rest Route für Block Metadaten.

[x] Neuer Netzwerk Befehlt "cmd", "cmd.msg" und "cmd.rs" in client/shared/network anlegen. 
[x] DTOs für REST Kommunikation aus client/instructions/client_2.0/server_rest_api.md in client/shared/rest anlegen. 

[x] Logger erweitern:
- default ConsoleTransport anstelle default logging in Logger zur Console.
- NullTransport: Logger wird nicht geschrieben.
[x] Logger Transport muss in laufzeit auswechselbar sein, d.h. hier muss noch eine Capselung implementiert werden. z.b.
anstelle von direktem halten des transports immer eine static methode mit 'logge das jetzt' anlegen und hier
den transport zentral halten. Der Transport kann dann jederzeit getauscht werden.

[x] ChunkData (client/packages/shared/src/types/ChunkData.ts)
- ChunkData wurde in client/instructions/client_2.0/network-model-2.0.md unter 'Chunk Update' genau beschrieben, ist aber anders umgesetzt.
- Server: Ueber das Netzwerk soll Block serilisiert in ChunkData, in einem Array uebertragen werden. Das kann ja im chunk direkt 
  vom Storage weg geschickt werden ohne die Daten in Objekte umzuformen, nur wenn daten manipuliert werden, muss das 
  objekt model erstellt werden und dann wieder persistiert werden. Ist der Chunk lokal im cache, werden bei einer auslieferung
  diese daten serilisiert und verschickt.
- Client: Im client werden die daten in Objekte deserilisiert und in einen ClientChunk mit ClientBlock umgewandelt, da drin
  wird das vorher deserialisierte Block Objekt rein gelegt.
[x] Im Server muss ein ServerChunk implementiert werden. Dieser Chunk sortiert die Blocks in ein Map und bietet CRUD werkzeuge
    an um den Chunk anzupassen. Ein ServerChunkHelper kann die Helperfunktionen bereitstellen. Und auch wieder die Umformung von  
    ServerChunk in ChunkData. Der ServerChunk wird dann benuztz um addBlocks oder deleteBlocks zu implementieren.
[?] Der Server soll chunks speichern und laden. Siehe dazu auch client_playground/packages/server/src/world/World.ts
- Gespeichert werden ChunkData chunks und zwar so, das sie via Netzwerk direkt aus Daten ausgeleifert werden koennen.
- Beim Anfrage durch den Client auf ChunkData wird folgender Algorithmus verwendet: 
  1. geprüft ob die Welt diese chunk gerade bearbeitet (ServerChunk) wenn ja, dann dann wird der ServerChunk in ChunkData gewandelt und zurueckgegeben
  2. pruefen ob ein gespeicherter ChunkData auf dem Speicher existiert, wenn ja zurückgeben
  3. aus Generator einen ServerChunk erstellen lassen, in der Welt ablegen, in ChunkData wandeln und zurueckgeben
- ServerChunks in der Welt wird automatisch gespeichert und deallociert.
- Wenn zum Server ein Block change geschickt wird, wird der Chunk geladen/generiert und manipuliert und automatisch gespeichert.
[-] Heigth Daten erzeugen und persistieren
- Wenn ein ChunkData angefragt wird, dann muessen auch HightData (client/packages/shared/src/network/messages/ChunkMessage.ts)
  zurück gegeben werden. Die HighData koennen aus dem Chunk errechnet werden
- Wenn keine HightData verfuegbar sind, werden im Client die Defaultwerte genommen.

## Basic Client Services

Da sehr viele Services erstellt werden müssen, die ineinander greifen, müssen sie Schritt für Schritt implementiert werden.
Für den Anfang werden einige Services als Mock erstellt und später richtig implementiert werden.

Typen (etc.) in shared wurden bereits angelegt und geprüft.

Plan:
* Zentrale Strukturen in Client
* Server Implementation 
  - wird konzeptionell von Playground übernommen, Netzwerk und Typen wurden angepasst
  - REST API teilweise neu implementiert
  - BlockType werden nun von Server ausgeleifert -> BlockTypes als JSON Dateien anlegen im Filesystem anlegen
  - Tests gegen den laufenden Server um die Implementierung zu testen
* Basic 3D Engine
* Start Screen
* InputService und Input-System anlegen
* Network Protocoll
* Editor und Console anlegen
* Client/3D Engine im Detail erweitern

Start mit zentralen Strukturen:

[x] AppContext: Enthält später Referenzen auf alle services, konfigurationen, etc. die der client braucht. 
Lege in NimbusClient den appContext an. Muss noch nicht vollstaendig sein, wird mit der Zeit erweitert.
[x] ClientService: Lege einen ClientService in den AppContext der beim Starten in NimbusClient erstellt wird.
- Er benötigt Zugriff auf den HTTP-Request, User-Agent, Language um den aktuellen Client, Sprache festzustellen.
- Implementiere getClientType() : network MessageTypes.ClientType
- Implementiere getUserAgent() : string
- Implementiere getLanguage() : string
- Implementiere isEditor() : boolean (NimbusClient __EDITOR__)
- Implementiere isDevMode() : boolean - wenn mit pnpm run dev:* gestartet - (not import.meta.env.PROD)
- Implementiere isLogToConsole() - wird aus dotenv gelesen. Kann mit setLogToConsole(true) gesetzt werden.

[x] Starte den Logger mit richtigem Modus in ClientService 'setupLogger()':
- isDevMode() == true: FileLogTransport, prüfe isLogToConsole(), dann auch consoleLogTransport
- isDevMode() == false && isLogToConsole(): ConsoleLogTransport ansonsten NullLogTransport

[x] Wenn in ClientService setLogToConsole() genutzt wird und sich der status ändert, muss setupLogger() auch aufgerufen werden.

## Server Implementation

[x] Implementiere den Server in NimbusServer.
- orientiere dich am prototypen code in client_playground/packages/server
- Die WebSocket ist nun ein JSON Protokoll, protocoll ist unter client/instructions/client_2.0/network-model-2.0.md beschrieben.
- Die Daten werden wieder in chunks gespeichert, die in memory gehalten werden.
- Implementiere die REST API routes unter client/instructions/client_2.0/server_rest_api.md
- Implementiere die wichtigsten Nachrichten im WebSocket Protocoll: Login (aktuell werden alle zugelassen), Ping/Pong
- Die Daten werden wieder in chunks gespeichert, die in memory gehalten werden.
- Neu sind Block-Metadaten, diese werden später implementiert, gib in der REST-Funktion vorerst keine Gruppen zurück (leer).
- Neu sind ClientCommands, es gibt aktuell keine Commands, lege einen CommandService an und CommandHandler, aber noch keine Commands.

[x] In client/packages/server/files/assets/textures/block/basic liegen texturen und es muessen dafuer BlockTypes erstellt werden.
- BlockTypes werden in client/packages/server/files/blocktypes angelegt, mit dem schema /(id % 100)/id.json, also z.b. 1/101.json.
- Die erste BlockType ID soll 100 sein
- In client_playground/packages/core/src/registry/defaultBlocks.ts ist ein beispiel wie frueher BlockTypes angelegt wurden, es haben sich natuerlich daten geaendert.
- Erstelle ein script (python oder nodejs) das fuer jedes asset eine json datei mit dem schema BlockType (client/packages/shared/src/types/BlockType.ts) erstellt und fuehre es aus.
- Das ziel ist eine statische menge von BlockType json dateien, die vom server gelesen und direkt ausgeleifert werden koennen
- Es fehlt ein Name des BlockTypes, bitte den auch anlegen
- Es fehlt eine Description
- Bei der texture die dateiendung (.png) anhängen
- Schmeisse die programmatischen BlockTypen raus, das brauchen wir nicht. Lege 0/0.json noch als default im filesystem an.
- Der server soll nicht beim starten alle files einlesen, sondern die bei request aus dem filesystem lesen und zurueckgeben. Geht das so? Damit koennen sich auch dateien waehrend der laufzeit aendern ohne durchstart.

[x] Schreibe Tests, die den laufenden Server testen und testen, das blocktypes richtig ausgeleifert werden.
- Der Port soll 3011 sein fuer den server. Kannst du bei den tests den Port 3111 nehmen damit sich das nicht mit einem leufenden server colidiert.

## Basic 3D Engine

[?] Prüfe ob der Server schon texturen ausliefert. 
- Texturen sollen Lazy ausgeleifert werden.
- Texturen liegen in files/assets/textures, im BlockType werden sie "assets/textures/block/basic/acacia_fence.png" angegeben.
- Wie in client/instructions/client_2.0/server_rest_api.md soll es einen asset endpunkt im server geben.
[?] Lege einen TextureService im client an, der texturen vom Server laden kann. Referenz in AppContext anlegen.
- Texturen werden nur einmal geladen und im TextureService gecached. Siehe auch client_playground/packages/client/src/rendering/TextureAtlas.ts

[?] Prüfe ob der Server bereits einen WebSocketServer bereit stellt und hier die 'Chunk Registration' und 'Chunk Update' Nachrichten implementiert sind.
[?] lege im Client einen NetworkService an der die WebSocket Verbindung oeffnen und login automatisch macht. Referenz in AppContext anlegen.
- Als weltId soll vorerst 'main' benutzt werden.
- Die WeltInfo muss an den AppContext geschrieben werden.
- Bei einem Reconnect der WebSocket soll automatisch der Login und die letzte 'Chunk Registration' neu verschickt werden
- Sende den Ping regelmaesig, beachte die Ping zeit in WordInfo
[?] Lege einen ChunkService im client der sich an chunks registriert und der ChunkData vom Server bekommt und als ClientChunk registriert, Referenz in AppContext anlegen.
- Siehe auch client_playground/packages/client/src/world/ChunkManager.ts

[x] Der TextureService soll die Texturen ueber den NetworkManager laden lassen.

Nun soll die 3D engine von BabylonJs verwendet werden, es muss viel auf einmal umgesetzt werden: 

- Nicht mehr als noetig umsetzten, das ziel ist eine erste begehbare 3D welt die noch verbessert werden kann.
- Zu details kannst du in 
[?] Lege einen BlockTypeService im client an, der BlockTypes aus dem Server laden kann. Referenz in AppContext anlegen.
- Siehe auch client_playground/packages/client/src/rendering/TextureAtlas.ts
- BlockTypes werden nur einmal geladen und im BlockTypeService gecached.
[?] Lege den EngineService im client an der die 3D engine initialisiert. Referenz in AppContext anlegen.
[?] Lege einen RenderService im client an der die Chunks rendert. Referenz in EngineService anlegen.
- Siehe auch client_playground/packages/client/src/rendering/ChunkRenderer.ts
- Lege zuerst nur den BlockRenderer an. Alle Typen ausser INVISIBLE werden erstmal an den BlockRenderer zum Rendern weiter gegeben.
[?] Lege eine CameraService an der in der EngineService referenziert ist. Der Service 
- Camera hat einen egoView eigenschaft.
[?] Lege einen PlayerService an, der Service hält die aktuelle Position des Spielers und 
    referenziert den CameraService um die Kamera zu steuern. Und Rendert später auch den Player in der Third-Person-Ansicht
[?] Lege einen EnvironmentService an der in EnineService liegt. Das Environment macht das Licht an.
[?] Lege einen InputService und einen InputController ableitung WebInputController an.
- Lege InputHandler fuer jede Aktion an. MoveLeft, MoveRight, MoveForward, MoveBackward, Jump, RotateLeft RotateRigth, PitchUp, PitchDown, etc
- Die InputHandler steuern den PlayerService und die CameraService
- InputHandler werden im InputService registriert und vorerst hart im WebInputController an Keys und Mouse fest vertratet.
mach das

[x] Kannst du noch meherere Generatoren erstellen und in generator.json im Welt Ordner ablegen. In der generator.json
ist die art des Generators und weiter Infos, z.b. ein seed.
- Flat: Einfach flache Ebene XZ
- Normal: Einen huegeligen, algorithm siehe client_playground/packages/server/src/world/generators/NormalWorldGenerator.ts

[?] Es soll einen Flug und Walk modus geben
- Flug: Der Spieler kann sich in der Welt bewegen wie im flug, keine Gravitation, Vor und Rueckwaerts in richtung des Kamera Pitches
  - Kein Sprung möglich
  - Nur im Editor modus Möglich
- Walk: Der Spieler kann sich in der Welt bewegen in der XZ Ebene, Vor und Rueckwaerts in richtung um die Y Achse
  - Es gibt Gravitation, d.h. nach unten fallen
  - Sprung mit Space, zwei Blocks hoch
  - Ein Block wird automatisch erklommen wenn der Spieler dagegen läuft
- Wenn man zufällig in einem Block steht, dann wird man automatisch einen Block nach oben gehoben.
- Diese Implementierung sollte in einem PhysicsService liegen und dort fuer alle local managed Entities gueltung haben.
- Wie Agieren der PlayerService und der PhysicsService miteinander?
- Gestartet wird im Walk Modus

[x] PhysicsService sollte auch darauf achten, dass ich die grenzen der welt (WorldInfo) nicht ueberschreite, auch nach oben und unten nicht.
- wenn ich ausserhalb der grenzen bin, werde ich automatisch an die naechste grenze geleitet.

[x] Die Mausrichtung links und rechts; hoch und runter sind verkert herum gebaut.
[?] Kann man das nach oben klettern um 1 Block etwas mehr animieren, in mehreren schritten tun.
[ ] Wenn man nicht weiter kann, weil vor einem ein block ist und der block hat physics.climbable > 0 gesetzt, dann kann man anstatt vorwaerts nach oben klettern.

> Umstellung:

[x] Die methode ClientService.getBlockAt() muss ClientBlock zurueck geben, d.h. 
- in ClientChunk muss ein neues ClientChunkData object anstelle des ChunkDataTransferObject benutzt werden.
- ClientChunkData:
  - transfer: ChunkDataTransferObject
  - data : Map<string, ClientBlock>
- Beim erstellen muss die struktur mit ClientBlock initialisiert werden.
- Alle ClientBlock muss initiiert werden mit einem gemerged BlockModifier fuer einen schnellen Zugriff.
  - Merge Regeln
    Metadaten werden von oben nach unten durch gemerged (first match winns)
       1. Block-BlockType-status-Metadaten (Insance status)
       2. Block-BlockType-ID status-Metadaten (Instance status=world status)
       3. Block-BlockType-ID status-Metadaten (Base status)
       4. Block-BlockType-ID status-Metadaten (Base status=world status)
       5. Default Werte für Metadaten, z.b. shape=0
- In Physics kann direkt auf clientBlock.currentModifier.physics.solid geprueft werden ohne alles zu mergen.


[x] Wenn der Player sich bewegt werden keine chunks mehr nachgeladen.
[x] Wenn ein chonk nicht geladen wurde muss der PhsyicsService den Player daran hindern diesen bereich zu betreten.

[ ] Erste Tests mit BabylonJS NullEngine
- Erstelle einen Test, der nur den CubeRenderer testet mit NullEngine

Extra:
[x] in BlockModifier den Parameter 'effect' Typisieren 'none' | 'water' | 'wind' | 'flipbox' | 'lava' | 'fog'
[x] Funktiomiert der Ping af WebSocket in NetworkService, ich bekomme session timeouts

===

## Basic Network Protocoll

[ ] Implementiere die network messages aus client/instructions/client_2.0/network-model-2.0.md im client/network. Jeweils in einem eigenen Handler als dummy.
[ ] Implementiere den WebSocket Client im NetworkService.
[ ] Implementiere die Login Nachricht und die Ping/Pong Nachrichten im NetworkService.

## Start Screen

[ ] Implementiere einen einfachen Start Screen im client der die Login Nachricht sendet und auf die Antwort wartet.
[ ] Login Credentials sind vorerst als env variablen im client hinterlegt.
    - CLIENT_USERNAME
    - CLIENT_PASSWORD
[ ] Server credentials sind vorerst als env variablen im client hinterlegt.
    - SERVER_WEBSOCKET_URL
    - SERVER_API_URL
[ ] Der StartScreen zeigt eine Liste von Welten an, in die der Spieler einloggen kann. Die Welten können via
    - Siehe client_playground/packages/client/src/VoxelClient.ts + client_playground/packages/client/src/gui/MainMenu.ts
    - Der StartScreen soll aber in einer eigenen Datei StartScreen.ts sein.
    - Erstelle eine Klasse RenderService in RenderService.ts die das eigentliche 3D initiiert und verwaltet, auch die scene wird hier gehalten.
    - SERVER_API_URL/worlds abgerufen werden
    - Wnn die Welt ausgew
[ ] Nach erfolgreichem Login wird der screen angezeigt und das laden der chunks beginnt.
    - Wird in RenderService.ts dargestellt 
[ ] Vor dem Starten den InputService initialisieren und den InputController anlegen.
    - InputController ist ein abstracter Controller der die input events aus dem system aufnimmt und die 
      entsprechenden InputActions auslöst. Ableitungen: BrowserInputController, XBoxInputController, etc.
    - InputController stellt slots bereit in die InputActions abgelegt werden können. 
      z.b. BrowserInputController bietet den Slot "key_q", "key_w", "key_e", "key_r", "key_shift_a", "key_arrow_up", "mouse_left", "mouse_right", etc. an.
    - Default ist BrowserInputController und dieser soll auch angelegt werden. Der InputService legt dann den InputController an und legt erstmal const actions in die Slots (später per settings)
    - InputService ist ein Singleton wird in AppContext abgelegt.
    - InputAction als abstracte Klasse anlegen. Ableitungen: MoveForwardAction, MoveBackwardAction, JumpAction, etc.
      So anlegen, das sowohl single actions (keys, buttons) also auch maus oder controller steuerung moeglich ist.
[ ] CamControl Klasse anlegen, die die Kamera steuert. Anlegen der InputActions in InputService, die die Camera 
    ueber CamControl steuern.

## Networking und Server

[ ] Im Client NetworkService anlegen, der die WebSocket zum Server verwaltet. hier connectSocket() aufrufen wenn die Welt
im StartScreen ausgewählt wurde.
[ ] Das login und Ping 
