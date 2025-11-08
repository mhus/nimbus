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
funktion um die Bleocke zu kruemmen. Diese Funktion muss noch im server in 'client/packages/test_server/src/world/TerrainGenerator.ts' implementiert werden.
- edgeOffset ist jetzt offset
- Wichtig: edgeOffset haben einen Range von -127 bis 127, offsset sind float und haben die einheit blocks, -127 sollte -1 und 127 sollte 1 entsprechen 

## Backdrop

- Brauche einen pseudo Wall am chunk der an den raundern zu macht, damit die sonne nicht in den tunnel scheint.
  oder fuer weit weit weg. Ideal mit Alpha ausblendung oben.
    - name: backdrop
    - 4 x Array of Backdrop(s)
    - backdrop?: {
       n? : Array<Backdrop>,
       e? : Array<Backdrop>,
       s? : Array<Backdrop>, 
       w? : Array<Backdrop>
      }
- Backdrop: {
  typeId?, // a backdrop type id, that will be overwritten with following parameters
  la?,   // local x/z coordinate a (0-16) - start, default 0
  ya?,   // world y coordinate a - start, default 0
  lb?,   // local x/z coordinate b (0-16) - end, default 16
  yb?,   // world y coordinate b - end, default ya + 60
  texture? : string,
  color? : string,
  alpha? : number,
  alphaMode? : int
  }

- Im Server eine rest api auf der backdropType heruntergeladen werden koennen.
  GET /api/backdrop/{id}
    - die backdropTypes werden im filesystem des servers im ordner files/backdrops/ gespeichert mit ihrer id als name, z.b. 1.json

- Die client 'engine' laed die packDrops lazy wenn sie angefordert werden und cache diese im
  BackdropService

- Default backdrop if not set in ChunkService setzen
  wenn eine seite nicht gesetzt wird, wird ein default
  backdrop gesetet, der in ChunkService als const hinterlegt wird.
  - Der default ist die texture backdrop/fog1.png

- backdrops brauchen jeweils ihr eigenes mesh, das sind aber wenige, da nur die umrandung backdrops hat, bei
  3 x 3 chunks sind das 12 mesh. 
- bei einem update
    - neuer chunk
    - drop chunk
  muss jeder chunk geprueft werden, hat er einen nachbarn
  noth, south, east west. Hat er keinen, muss dort ein backdrop gezeichnet werden
  z.b pruefe chunk 3,4:
    - gibt es einen north chunk 3,5 ? nein, zeichne backdrop n3,4 mit den daten aus chunk(3,4).backdrop.n
    - gibt es einen south chunk 3,2 ? ja, nichts machen
    - gibt es einen west chunk ...
    - gibt es einen east chunk ...
    - alle chunks pruefen, nicht mehr gebrauchte backdrops loeschen
