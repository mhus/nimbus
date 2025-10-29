
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

===

## Basic Client Services

Da sehr viele Services ertellt werden müssen, die ineinander greifen, müssen sie Schritt für Schritt implementiert werden.
Für den Anfang werden einige Services als Mock erstellt und später richtig implementiert werden.

Start mit zentralen Strukturen:

[ ] AppContext: Enthält später Referenzen auf alle services, konfigurationen, etc. die der client braucht. 
Lege in NimbusClient den appContext an. Muss noch nicht vollstaendig sein, wird mit der Zeit erweitert.
[ ] ClientService: Lege einen ClientService in den AppContext der beim Starten in NimbusClient erstellt wird.
- Er benötigt Zugriff auf den HTTP-Request, User-Agent, Language um den aktuellen Client, Sprache festzustellen.
- Implementiere Client: WEB_BROWSER, XBOX, MOBILE_IOS, MOBILE_ANDROID, MOBILE
- Implementiere UserAgent : string
- Implementiere Language
- Implementiere isEditor()
- Implementiere isDevMode() - wenn mit pnpm run dev:* gestartet.
- Implementiere isLogToConsole() - wird aus dotenv gelesen. Kann mit setLogToConsole(true) gesetzt werden.

[ ] Starte den Logger mit richtigem Modus in ClientService 'setupLogger()':
- isDevMode() == true: FileLogTransport, prüfe isLogToConsole(), dann auch consoleLogTransport
- isDevMode() == false && isLogToConsole(): ConsoleLogTransport ansonsten NullLogTransport

[ ] Wenn in ClientService setLogToConsole() genutzt wird und sich der status ändert, muss setupLogger() auch aufgerufen werden.

## Server Implementation

[ ] Implementiere den Server in NimbusServer.
- orientiere dich am prototypen code in client_playground/packages/server
- Die WebSocket ist nun ein JSON Protokoll, protocoll ist unter client/instructions/client_2.0/network-model-2.0.md beschrieben.
- Die Daten werden wieder in chunks gespeichert, die in memory gehalten werden.
- Implementiere die REST API routes unter client/instructions/client_2.0/server_rest_api.md
- Implementiere die wichtigsten Nachrichten im WebSocket Protocoll: Login (aktuell werden alle zugelassen), Ping/Pong
- Die Daten werden wieder in chunks gespeichert, die in memory gehalten werden.
- Neu sind Block-Metadaten, diese werden später implementiert, gib in der REST-Funktion vorerst keine Gruppen zurück (leer).
- Neu sind ClientCommands, es gibt aktuell keine Commands, lege einen CommandService an und CommandHandler, aber noch keine Commands.


## Basic 3D Engine

[ ] Lege einen TextureService an, der aber aktuell nichts tut. Referenz in AppContext anlegen.
[ ] Lege einen BlockTypeService an, der aber aktuell nur 0 und 100 kennt und zurück gibt. Referenz in AppContext anlegen.
- 0 = AIR Block: Lege hierfür in BlockType einen constanten Block mit shape:0 an.
- 100 = Dummy: Lege hierfür einen constanten Block mit shape:CUBE und color '#00ff00'an in BlockTypeService an. - wird später wieder gelöscht.
[ ] Lege einen ChunkService an, der immer eine Horizontale Ebene auf Y=0 ausgibt mit der BlockTypeId 100. Referenz in AppContext anlegen.
[ ] Lege den EngineService an und er wird in NimbusClient erstellt und in den AppContext gespeichert. Referenz in AppContext anlegen.

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
