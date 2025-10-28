
# Migration

## Ziele

* Datenmodell Block Model 2.0 implementieren
* Protocoll optimieren
* Speicherung optimieren (sekundär)
* Trennen des Editors / Console aus dem Viewer
* Input Management implementieren mit InputSchema (Keyboard, Mouse, Gamepad) und InputHandler, InputMapper für Settings
* AppContext mit allen relevanten Services nutzen, anstelle von einzelnen referenzen
* Third-Person View Mode + Ego-Mode implementieren

## UI Components

* Editor
* Console
* Notifications (links unten, Client + Server Meldungen)
* Interaction-UI (z.B. wenn man mit einem Block interagiert, öffnet sich ein UI Panel, wird vom Server gesteuert)
* External-UI (IFrame, externe Editoren für Inventar, etc.)
* ClientSettings Dialog
* Group Viewer (Zeigt alle Spieler in der Gruppe an)
* Side-UI (IFrame an der Seite der aufklappen kann, z.B. für Video Chat, inventar, etc.)

## Services

* EnvironmentService
* ChunkService
* MaterialService
* BlockTypeService
* TextureService
* AppService (informationen über die App: sind wir im Editor, Viewer; PC  Browser, XBox, Mobile, etc.)
* WorldInfoService (Host, Size, Name, Description, etc. )
* ConnectionService (WebSocketClient, MessageHandler, HttpClient )
* ShaderService
* InputService
* ChunkRenderService
* SpriteService (hat alle Sprite manager)
* CommandControllerService (für Commands)
* SoundService (Background Music, Sound Effects, 3D Sound, etc.)
* PlayerService (aktuelle posiiton, rotation, egoMode, forceEgoMode - hier Kamerasteuerung rein?)
* BlockEditorService (Trennen vom Viewer, wenn vorhanden, dann Referenz im AppContext)
* ConsoleService (Trennen vom Viewer, wenn vorhanden, dann Referenz im AppContext)
* ModellService (Lädt 3D Modelle, z.B. für Block Model und Entities)
* AnimationService (für Block Animationen und Entity Animationen, EffectAnimationen, Entity Routes separat?)
* EntityService (für Entities, NPCs, Spieler-Modelle, etc.)
* EffectService (für alle Effekte, effect registry, start animation for effects)
* SettingService (für alle Einstellungen des Users)

z.B.
- MaterialService holt Texturen von TextureService, der läd Texturen vom ConnectionService der hotl die URL vom WorldInfoService.
- Vom Server kommt eine Anweisung für zum starten einer Effect-Animation, EffectService holt die Animation vom AnimationService und startet sie, laden der Texturen über MaterialService, ...
- Vom Server kommt eine neue Entity im Terrain, EntityService erstellt die Entity, holt das Modell vom ModellService, startet lauf animation über AnimationService, etc.
- Vom Server kommt ein neuer chunk, ChunkService erstellt den Chunk, ChunkRenderService rendert den Chunk, Sound Srvicer spielt evtl. Sound ab, etc.

## Network Protocoll

Die Kommunikation wird auf zwei Kanäle aufgeteilt:
* WebSocket Kanal für Echtzeit Kommunikation (Block Änderungen, Entity Bewegungen, Effekte, etc.)
* HTTP Kanal für nicht Echtzeit Kommunikation (Laden von Texturen, Modellen, etc.)

Die WebSocket Communikation wird vorerst als JSON5 Nachrichten implementiert, so dass es später durch einen
optimiertes Verfahren der Serialisierung ersetzt werden kann. Das Basis-Object für Nachrichten ist:

```json
{
  "i": "identifier", // optional, wenn eine Antwort erwartet wird
  "r": "responseTo", // optional, verweist auf die ID der Nachricht auf die geantwortet wird
  "t": "messageType",
  "d": "payloadData" // sting, object oder byte array oder null, aktuell nur object oder null
}
``` 

z.b.
```json
{"i": "12345","t": "p"} // ping message
```

Siehe network-model-2.0.md für die komplette Liste der Nachrichten.

## Details

* 'Solid' ist keine Materialeigenschaft!
* 'Textures' sollen incl. png endung angegeben werden, damit verschiedene Formate möglich sind (jpg, webp, etc.)
* 'Block Textures' werden mit kompletten pfad angegeben, z.B. 'block/grass.png'
