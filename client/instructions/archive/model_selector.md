
# Model Selector

## Visibility

[x] In SelectService soll es eine neue Art der Sichtbarmachung geben: Model Selector.
- ModelSelector zeigt gleichzeitig mehere Blocks als selektiert an. Er hat dazu eine liste von coordinaten, die
  selektiert sind. Er erzeugt eine liste von meshes, wie bei dem normalen Selektor, aber eben eine liste
- Die darstellung sit wie beim selektor eine Box, aber diese ist nbicht weiss, sondern die farbe wird beim 'enablen' definiert.
- ModelSelectot hat zwei akgivierungsebenen: enabled und visible.
  - enabled ist der grundlegende Modus um den Selektor zu aktivieren. Wird der selektor disabled, werden alle meshes und koordinaten gelöscht.
  - visible ist der Modus um den selektor sichtbar zu machen. Wenn visible false ist, sind die meshes nicht sichtbar, aber die koordinaten bleiben erhalten.
  - ein selektor der nicht enabled ist, kann nicht sichtbar gemacht werden.
- Model selector kann neue blocks vom Network automatisch zum model hinzufuegen, 
  - dazu wird im NetworkService Handler, wenn der Client im Modus EDITOR ist, ein event für neue blocks gesendet
  - Im SelectService wird das event gefangen und wenn der model selector enabled ist und watchBlocks true ist, werden die neuen blocks automatisch zum model selector hinzugefügt.
- Wird der model sselector enabled, obwohl er schon enabled ist, wird er automatisch disabled und mit den neuen parametern wieder enabled.

Neue Methoden in SelekService:
- enableModelSelector(color: string, watchBlocks boolean, show: boolean, selected: list of Vector3)
- disableModelSelector()
- showModelSelector(visible : boolean)
- addToModelSelector(selected: list of Vector3)
- removeFromModelSelector(selected: list of Vector3)
- getModelSelectorCoordinates() : list of Vector3

```text
 Der ModelSelector wurde erfolgreich in den SelectService integriert:

  Neue Features:

  1. SelectService Erweiterungen (packages/engine/src/services/SelectService.ts):
    - Private properties für ModelSelector State (enabled, visible, watchBlocks, color, coordinates, meshes, material)
    - enableModelSelector(color, watchBlocks, show, selected) - Aktiviert den Selector mit konfigurierbarer Farbe
    - disableModelSelector() - Deaktiviert und löscht alle Meshes
    - showModelSelector(visible) - Steuert Sichtbarkeit ohne Koordinaten zu löschen
    - addToModelSelector(selected) - Fügt Koordinaten hinzu (mit Duplikatsprüfung)
    - removeFromModelSelector(selected) - Entfernt Koordinaten
    - getModelSelectorCoordinates() - Gibt alle Koordinaten zurück
    - onNewBlocks(blocks) - Handler für neue Blocks vom Network (watchBlocks-Modus)
    - updateModelSelectorMeshes() - Erstellt/aktualisiert die visuellen Meshes
  2. NetworkService Integration (packages/engine/src/network/handlers/BlockUpdateHandler.ts):
    - BlockUpdateHandler sendet jetzt ein newBlocks Event für neue Blocks (nicht gelöschte)
    - Event wird nur im EDITOR-Modus verwendet
  3. Event-Handling (packages/engine/src/services/EngineService.ts):
    - Im EDITOR-Modus wird automatisch auf newBlocks Events gehört
    - Koordinaten werden von Block-Positionen zu Vector3 konvertiert
    - SelectService.onNewBlocks() wird aufgerufen wenn watchBlocks aktiv ist
  4. Aktivierungslogik:
    - enabled: Grundmodus - disabled löscht alles
    - visible: Sichtbarkeitsmodus - behält Koordinaten bei
    - Wenn enableModelSelector() aufgerufen wird während bereits enabled, wird automatisch disabled und neu enabled

  Technische Details:

  - Rendering: Verwendet RENDERING_GROUPS.SELECTION_OVERLAY (wie edit highlight)
  - Farbe: Vollständig konfigurierbar (hex string oder Color3)
  - Meshes: Box mit 1.02 scale, edges rendering mit konfigurierbarer Farbe
  - Material: Semi-transparent (alpha 0.3), emissive color für Sichtbarkeit
  - Cleanup: Alle Ressourcen werden in dispose() korrekt aufgeräumt
```

[x] Mit der Taste F8 soll der ModelSelector sichtbar/unsichtbar gemacht werden können, wenn er enabled ist.
- Siehe hier als beispiel wie F6 implementiert ist

[x] Erstelle commandos in engine um den ModelSelector zu steuern:
- /modelselector enable <color> <watchBlocks:true|false> <show:true|false> [<x1> <y1> <z1> ... <xn> <yn> <zn>]
- /modelselector disable
- /modelselector show <true|false>
- /modelselector add <x1> <y1> <z1> ... <xn> <yn> <zn>
- /modelselector remove <x1> <y1> <z1> ... <xn> <yn> <zn>
- /modelselector list
- Siehe dazu CommandService und wie andere commands implementiert sind.

```text
  ModelSelector - Vollständige Implementation abgeschlossen! ✓

  Implementierte Features:

  1. SelectService Integration ✓

  - Multi-Block-Selektion mit konfigurierbarer Farbe
  - Zwei Aktivierungsebenen: enabled und visible
  - Automatisches Hinzufügen neuer Blocks vom Network (watchBlocks-Modus)
  - Alle geforderten Methoden implementiert

  2. F8 Tastenbindung ✓

  - Toggle-Handler für Model Selector Sichtbarkeit
  - Nur im EDITOR-Modus aktiv
  - Vollständig integriert in InputService und WebInputController

  3. Commands ✓

  Alle geforderten Commands implementiert in ModelSelectorCommand.ts:

  modelselector - Zeigt aktuellen Status
  - Enabled/Visible Status
  - Anzahl der Blöcke

  modelselector enable <color> <watchBlocks> <show> [coords] - Aktiviert den Selector
  - Beispiel: modelselector enable #ff0000 true true 10 20 30
  - Farbe als Hex-String
  - watchBlocks: automatisch neue Blocks hinzufügen
  - show: sofort sichtbar machen
  - Optionale Koordinaten (x y z Gruppen)

  modelselector disable - Deaktiviert komplett
  - Löscht alle Meshes und Koordinaten

  modelselector show <true|false> - Sichtbarkeit togglen
  - Beispiel: modelselector show false
  - Behält Koordinaten bei

  modelselector add <x1> <y1> <z1> [...] - Koordinaten hinzufügen
  - Beispiel: modelselector add 10 20 30 11 20 30
  - Duplikatsprüfung integriert

  modelselector remove <x1> <y1> <z1> [...] - Koordinaten entfernen
  - Beispiel: modelselector remove 10 20 30

  modelselector list - Alle Koordinaten auflisten
  - Zeigt alle aktuellen Koordinaten
  - Zeigt Status (enabled/visible)

  Technische Details:

  - Type Conversion: Verwendet CastUtil (toBoolean, toNumber, toString) gemäß CLAUDE.md
  - Error Handling: Vollständiges Error Handling mit detaillierten Rückmeldungen
  - Logging: Alle Aktionen werden geloggt (log.info für Focus-Debugging)
  - TypeScript: Kompilierung ohne Fehler ✓
  - Command Registration: In CommandsFactory registriert
```

[ ] Movement
- Erweitere den ModelSelector so, dass die selektierten Blöcke verschoben werden können. Alle mit einem mal.
- erstelle eine neue methode im SelectService:
  - moveModelSelector(offset: Vector3)
- Die Bloecke wedren um den offset verschoben, coordinaten und meshes werden aktualisiert.
- Erweitere auch das /modelselector command um den move befehl:
  - /modelselector move <offsetX> <offsetY> <offsetZ>
