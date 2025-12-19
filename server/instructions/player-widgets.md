
# Player Widgets und Panels

## Widgets

Widgets sind elemente die vor allem als IFrame als overlay in der Welt angezeigt werden.
Es sind kleine Fenster. Sie haben keinen Header oder Footer.

Es gibt 
- 1/4 Anzeige. Viertel des Hauptfensters
- 1/2 Anzeige. Halber bidlschirm horizontal oder vertikal
- voll Anzeige. Gesamter bildschirm, aber kleiner als das Hauptfenster.

## Panels

Panels werden als Overlay immer Voll anzeigt. Es gibt ein main Panel das links zu den anderen Panels
hat. Panels haben einen Header mit dem man wieder zuruekc auf das main panel kommt.

Der Unterscheid zu editoren ist: Es handelt sich immer um Player Einstellungen, darauf werden Panels optimiert.
Editoren sind für Administratoren.

## PanelApp

[ ] Erstelle in ../client/pakcages/controls eine PanelApp und panels.html. Als Vorlage hkannst du HomeApp nutzen. Es wird
Panels geben, die hier verlinkt werden. Jedes Panel wird mit einem
Streifen, untereinander, und darin der Name angezeigt. Clickt man auf den streifen, wird auf das panel verlinkt, gewchselt.
Es gibt keine Rechtepruefung, wie in der HomeApp.

## Open Panel 

[ ] Erstelle in engine einen Input der auf den Key 'p' hoert und das Panel in einem dialog oeffnen.
Aehnlich wie editor-config. Auch mit den enstrechenden Funktionen. Der Dialog soll ein volles
preset benutzen, und schliessbar sein. Erweitere auch das Commando mit dem man pannels oeffnen kann.

Geoffnet wird der link auf panels.html

## Shortcuts Panel

[ ] Erstelle ein editor-shortcut-panel.html und verlinke es in der PanelApp.
Hier keonnen die Shortcut slots daten zugewiesen werden. PlayerInfo.editorShortcuts

Abrufen mit:

PlayerService.getPlayer() -> PlayerData
PlayerData
- PlayerUser
  - userId;
  - displayName;
- PlayerCharacter
  - PlayerInfo
  - PlayerBackpack
- Settings

Es wird ein neuer PlayerController in world-control benoetigt um diese Daten abzurufen.
- Erstelle eine ../playerinfo/{playerId} route die expliziet PlayerInfo Entity zurueck gibt.
- Es gibt auch die TypeScript entity unter packages/shared/src/types/PlayerInfo.ts

Vorlagen, was in die shortcuts geschrieben werden kann werden in WAnything in der collection
'editorShortcuts' für die region der aktuellen Welt hinterlegt.
- Die Anything daten laden (Rest controller gibt es bereits)
- daneben die shortcut slots des spielers laden und anzeigen.
- Slots koennen geleert und befüllt werden.
- Die Anything daten haben genau die struktur, die für die slots benoetigt wird.



## Chests Widget

Anzeige: Voll
Zeigt den Inhalt eines chest und den eigenen Backpack an. Items keonnen transferiert werden.

## Backpack

Zeigt den Inhalt des backpack an.

## Shortcuts
