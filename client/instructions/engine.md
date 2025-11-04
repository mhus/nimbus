# Engine Enhancements

## Compass

[x] Zeige im oberen Bereich eine Compass Bar der in einem CompassService verwaltet wird. Einhaegen in AppContext.
- Die Compass bar ist ein horizontaler Balken der innen leer ist. Es gibt einen TOP, CENTER und BOTTOM bereich. CENTER ist im leeren Bereich der Bar.
- die componente ist ansonsten durchsichtig und empfaegt keine maus events.
- Die bar soll den komplette 360 bereich anzeigen.
- Es sollen sich Marker fuer Nord, Ost, Sued, West befinden (N S E W). Wobei Nord richtung Z > 0 ist.
- Am CompassService keonnen Marker hinzugefuegt werden. Die Marker haben immer World Block Koordinaten)
- Der Compass soll drehen wenn sich die Kamera bewegt
- Die Darstellung kann asynchron/versetzt erfolgen

Marker:
- Position - World Block Koordinaten
- Farbe - Rot
- Darstellung - Kreis, Raute, Dreieck, Pfeil /\
- Position: Oben (oberhalb der bar), mitte, unten
- close() - Marker wird geschlossen
- setPosition() - Marker wird neu positioniert, wenn sich das ziel bewegt hat
- range - Wie weit der Marker von der Position aus sichtbar ist (-1 = unendlich)

[x] Blende Marker die zu nah sind um eine Position anzuzeigen aus (naeher als 5 Blöcke - constante )
Fuege in 'Marker' noch eine Variable hinzu:
- nearClipDistance - Abstand in Blöcken ab dem der Marker ausgeblendet wird (default 5)

[x] Wenn der client im EDITOR modus ist, zeige rechts neben dem Compass die aktuelle Position des Players an. X Y Z
Implementiere das im CompassService

[x] Erstelle ein Commando playerPositionInfo() im client, das die aktuelle position des players ausgibt, die highData 
informationen der aktuellen column (XZ), daten zu dem selektierten block. 
- Gib noch die Daten aus WordInfo und ClientInfo aus.

[x] Die selektierte funktion 'processHeightData' ist nicht sehr effektiv, wenn in 'processChunkData' schon die hoehen 
daten mit ermittelt werden, in einem prozess schritt, waehre das vermutlich performanter. Performanze ist hier wichtig.
Ausserdem werden die default aus den WordInfo nicht richtig uebernommen. Achte darauf, dass heighData auch vom server 
kommen keonnen. 
- maxY soll eigentlich immer, wenn nicht anders geleifert, dax maxY der welt sein. muss dann eigentlich nicht berechnet werden.
- es gibt eine ausname, wenn ein block hoeher als weltMaxY ist, dann lasse noch 10 platz und setze das als maxY. 
-> worldMaxY kann tiefer als die hoechsten Berge sein um die welt allgemein zu deckelt.

[?] In 'client_playground/packages/server/src/world/generators/NormalWorldGenerator.ts' hat der World generator noch eine
funktion um die Bleocke zu kruemmen. Diese Funktion muss noch im server in 'client/packages/server/src/world/TerrainGenerator.ts' implementiert werden.
- edgeOffset ist jetzt offset
- Wichtig: edgeOffset haben einen Range von -127 bis 127, offsset sind float und haben die einheit blocks, -127 sollte -1 und 127 sollte 1 entsprechen 
