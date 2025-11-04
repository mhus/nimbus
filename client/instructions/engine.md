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

[ ] Wenn im EDITOR modus, zeige rechts neben dem Compass die aktuelle Position des Players an. X Y Z