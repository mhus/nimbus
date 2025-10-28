
# Migration client playground to v 2.0

## Basics

> rename client nach client_playground
> create new client folder for client 2.0

Der code in 'client_playground' ist ein erster prototyp für die 3D client engine.
Der neue Code soll in 'client' für die version 2.0 der client engine entwickelt werden.
Die Entwicklung soll schritt für schritt einen bereich nach dem anderen neu implementieren.
Die Erkenntnisse und verbesserungen aus dem prototypen sollen dabei einfliessen. Es wird
bei den Aufgaben referenzen auf den prototypen code in 'client_playground' geben.
Erstelle nicht selbstständig code im 'client' ordner ausser es ist in den aufgaben beschrieben.
Du sollst aber hinweise und verbesserungen für den prototypen code in 'client_playground' geben
die dann diskutiert und ggf. umgesetzt werden können.

Frontend: TypeScript + Vite + Vue3 + Babylon.js
Testing: Jest (unit)

Das Projekt soll in verschiedene packages aufgeteilt werden

shared: Code der sowohl vom client als auch vom server genutzt wird. Gemeinsamme datentypen, protocoll definitionen, utils, etc.
client: 3D Engine code mit Babylon js, UI, Input handling, etc. Editor etc
server: Simple server implementation, die den client mit welt daten versorgt. Dies ist nicht der haupt fokus, sondern dient nur zum testen des clients.

Der client wird in verschiedenen builds in unterschiedlichen versionen bereitgestellt:
- viewer: Nur die 3D engine zum anschauen der welt
- editor: 3D engine + editor funktionen + console

[ ] Erstelle in client eine CLAUDE.md datei mit den informationen zur nutzung von claude für die entwicklung.
[ ] Erstelle in pnpm ein monorepo mit den packages: shared, client, editor, server. Für Typescript. Die App dateien sollen NimbusClient und NimbusServer heissen.

## Basic Shared Types

Jede Datenstruktur soll seine eigene Datei bekommen. Enums, status, etc. zu dem Type Können in der gelchen Datei definiert sein, solange sie nur für 
diesen Type relevant sind.

[ ] Lege die Typen aus client/instructions/client_2.0/object-model-2.0.md in shared/types an.
[ ] Lege die Network Messages aus client/instructions/client_2.0/network-model-2.0.md in shared/network an.

## Basic Client Types

[ ] AppContext: Enthält referenzen auf alle services, konfigurationen, etc. die der client braucht. Lege im main den appContext an.
[ ] Services: Lege die services aus client/instructions/client_2.0/migration-concepts.md in client/services an.

## Basic Network Protocoll

[ ] Implementiere die network messages aus client/instructions/client_2.0/network-model-2.0.md im client/network.
[ ] Implementiere den WebSocket Client im ConnectionService.
[ ] Implementiere die Login Nachricht und die Ping/Pong Nachrichten im ConnectionService.
[ ] Implementiere die Chunk Registration Nachricht im ConnectionService.
[ ] Implementiere die Chunk Anfrage Nachricht im ConnectionService.
[ ] Implementiere die Chunk Update Nachricht im ConnectionService.

## Basic Chunk Rendering

[ ] Implementiere den ChunkService mit der Fähigkeit Chunks zu verwalten.
[ ] Implementiere den ChunkRenderService mit der Fähigkeit Chunks zu rendern.
[ ] Implementiere die Grundlegende Darstellung von Chunks mit Blöcken im ChunkRenderService.



