
# Player Session Persistence

Der Status einer Player Session soll gespeichert werden können. Wenn der Player
sich wieder einloggt, dann kann der Status wiederhergestellt werden.

Wichtig für:
- Weiter spielen
- Wenn in eine Zone ud Zurück wechseln wird. - Hier muss ein Merge gemacht werden.

Speichern in bestimmten Situatuionen:
- WSession close / logout
- Immer mal wieder zwischendurch (Timer)
- Wenn in den Combat Modus gewechselt wird
- Wenn in eine Zone gewechselt wird

Persistiert wird:
- PlayerId, WorldId (Key)
- Position, Rotation
- Aktuelle Effekte
- Aktuelle Health / Mana / Stamina Werte

> Das ist viel GamePlay, aber GamePlay ist noch nicht umgesetzt
> Später erweitern um Gameplay Funktionalität.

## Entity

[ ] Erstelle eine Entity WPlayerSession in world-shared mit den folgenden Feldern:
- wordlId
- playerId
- position : Vector3
- rotation : Rotation (?)
- returnWorldId : String (für Zonen wechsel)
- returnPosition : Vector3
- createdAt
- modifiedAt
- unique Index auf worldId + playerId
Und das WPlayerSessionRepository mit den üblichen CRUD Methoden. Erstelle einen WPlayerSessionService der die 
WPlayerSessions verwaltet.

## PlayerSession restore

In dev-login (../client/packages/controls) eine auswahl geben zum startPunkt
1. letzter Spielstand
2. Startpunkt in Grid (q,r) angeben
3. Startpunkt der Welt
- Die Auswahl in einem string speichern. 'entryPoint=last|grid:0,0|world'
Diese Einstellung wird beim login weiter gegben an den REST Controller und an der session gehaengt (im redis)

Wenn die config im world-player abgefufen (WorldConfigController) wird, gibt es jetzt schon eine methode die diese manipuliert (patchWorldInfo).
Hier wird nun der startpunkt ermittelt und in der WorldInfo entsprechend gesetzt.
Wenn 1.) Dann wird die WPlayerSession aus der DB geholt und die Position / Rotation gesetzt. - Wenn nicht gefunden, dann auf Startpunkt der Welt.
Wenn 2.) Dann die Position aus dem HexGrid geladen, wenn nicht gefunden, dann auf Startpunkt der Welt.
Wenn 3.) Dann auf Startpunkt der Welt. (keine manipulation)



