
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
[ ] Erstelle builds im client für viewer und editor. Mit pnpm run build:viewer und pnpm run build:editor.
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

===
## Basic Client Types

[ ] AppContext: Enthält referenzen auf alle services, konfigurationen, etc. die der client braucht. Lege im main den appContext an.
[ ] Services: Lege die services aus client/instructions/client_2.0/migration-concepts.md in client/services an.
[ ] InputController und InputService anlegen, InputController wid im InputService erstellt und gehalten. InputService im AppContent

## Basic Network Protocoll

[ ] Implementiere die network messages aus client/instructions/client_2.0/network-model-2.0.md im client/network. Jeweils in einem eigenen Handler als dummy.
[ ] Implementiere den WebSocket Client im NetworkService.
[ ] Implementiere die Login Nachricht und die Ping/Pong Nachrichten im NetworkService.
[ ] 

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
