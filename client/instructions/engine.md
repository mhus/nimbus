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

[x] In 'client_playground/packages/server/src/world/generators/NormalWorldGenerator.ts' hat der World generator noch eine
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


jetzt sehe ich was, aber sehr merkwuerdig, denn die positions sind falsch. in einem backdrop kommt
- eine y (y1) coordinate fuer die hoehe unten, wenn die nicht kommt sollten wir eine sinnvolle hoehe nehmen, z.b. groundLevel an dieser kante berechnen. - sollte meistens fehlen, damit der
  grouldLevel genuzt wird.
- eine yUp (y2) coordinate, die oben ist. am besten relational zu y+hoehe des backdrops (die namen sollten angepasst werden, yBase und yUp ?).
- es werden locale zx coordinaten geliefert, da diese je nachdem in welche richtung das backdrop geht z+, z-, x+ oder x- sein kann. deshalb sind das lokale coordinaten, die an der jeweiligen
  ecke des chunks anfangen. mit 0 bis 16 (da ein chunk 16x16 blocks hat). Also berechne die x bzw z coordinalte ab chunk ecke bis zur naechsten chunk ecke (+16). 
- nenne yUp in height um und lb in width, la in left 

> bevor wir weiter machen. ich habe eine flaeche xz ebene bei y = 0, P1(x:0,y:0,z:0) und P2 (x:16,y:0,z:16) bei welchen beiden punkten ist die north kante?

⏺ Bei einer XZ-Ebene mit P1(0,0,0) und P2(16,0,16):

North-Kante ist bei Z = 16 (positive Z-Richtung)

Die beiden Punkte der North-Kante sind:
- (x:0, y:0, z:16)
- (x:16, y:0, z:16)

Zur Orientierung:
- North (n): z = 16 (obere Kante, positive Z)
- South (s): z = 0 (untere Kante)
- East (e): x = 16 (rechte Kante, positive X)
- West (w): x = 0 (linke Kante)

## faceVisibility?: FaceVisibility in VisibilityModifier

[x] In Block gibt es den parameter 'faceVisibility?: FaceVisibility'. Um die eiggenschaft zusaetzlich
generisch anzubeiten, soll er zusaetzlich auch in VisibilityModifier angegeben werden.
- Erweitere den Typ
- Erweitere den Editor von VisibilityModifier - Benutze die gleichen checkboxen wie im Editor fuer Block
- Benutze den Parameter in der Engine. FaceVisibilityHelper.isVisible muss erweitert werden. Am besten es wird
  anstelle von block.block.faceVisibility (number) gleich 'block.block' (Block) uebergeben wird.
- Nutzung von FaceVisibilityHelper.isVisible muss ueberall angepasst werden.
- FaceVisibilityHelper.isVisible prueft zuerst, ob visibility.faceVisibility existiert, nimmt dann den wert von dort, alternativ wie bisher.

## Smooth Block Movement

[x] Ich moechte die Bewegung auf einem Block besser steueren koennen. Die Bewegung wird in PhysicService umgesetzt.
Da die darstellung von Blocks variieren kann, d.h. ein block kann auch abgeschraegt sein, soll sich auch die bewegung so aehnlich verhalten.
Meine Idee: Ich hinterlege fuer die vier oberen ecken einen modifier, der die hoehe des blocks an deiser stelle justiert. Dann soll der physics service
erkennen ob er auto jump (uebergang von block auf naechsten zu hoch) oder ob er ueber den block sliden kann. Dann beim ueber den block laufen slided der player ueber die schraege flaeche.
Ist das eine gute idee, bigt es alternativen? 

```text
Here is Claude's plan:
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
 Implementierungsplan: Block-Höhen-Modifier mit Sliding

 1. PhysicsModifier erweitern (BlockModifier.ts)

 - Neues Property: cornerHeights?: [number, number, number, number]
 - Reihenfolge: [NordWest, NordOst, SüdOst, SüdWest] (gegen Uhrzeigersinn)
 - Werte: Relative Höhen-Adjustments (0.0 = Standard-Höhe)
 - Backward compatible: undefined oder nicht 4 Werte → [0, 0, 0, 0]

 2. PhysicsService Hilfsfunktionen hinzufügen

 Neue Funktionen:
 - getBlockSurfaceHeight(block: ClientBlock, localX: number, localZ: number): number
   - Berechnet interpolierte Höhe an Position innerhalb Block
   - Verwendet bilineare Interpolation zwischen 4 Eckpunkten
   - Performant: nur wenn cornerHeights gesetzt
 - calculateSlope(cornerHeights: [number, number, number, number]): Vector2
   - Gibt Neigungs-Vektor (X, Z) zurück
   - Durchschnittliche Neigung in beide Achsen

 3. Ground Collision erweitern (checkGroundCollision)

 - Bei Blocks mit cornerHeights: präzise Ground-Y berechnen
 - Player.position.x/z → lokale Block-Koordinaten → getBlockSurfaceHeight()
 - Fallback auf bisheriges Verhalten wenn keine cornerHeights

 4. Neue Sliding-Mechanik implementieren

 Neue Funktion: applySlidingFromSlope()
 - Ähnlich wie applyAutoMove() (Zeile 1165-1215)
 - Berechnet Sliding-Velocity aus:
   - Neigung (aus cornerHeights)
   - resistance (linear: sliding × (1 - resistance))
   - Gravitations-Komponente (mehr Sliding bei steileren Flächen)
 - Integriert in updateWalkMode() nach applyAutoMove()

 5. Integration & Testing

 - In updateWalkMode() nach Line 573 einfügen
 - Kompatibilität sicherstellen: nur wenn cornerHeights gesetzt
 - Performance: Berechnungen nur für Blocks unter Player

 Technische Entscheidungen:
 - Eckpunkt-Reihenfolge: Counter-clockwise (mathematische Konvention)
 - Resistance-Einfluss: Linear (effektiv = neigung × (1 - resistance))
 - Performance > Präzision: Einfache bilineare Interpolation
╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌╌
```

[-] Fuege im VisibilityEditor die Option 'cornerHeights' hinzu. Als Ausklappbare Eigenschaft mit vier
Werten nebeneinander.

[x] Das System muss erweitert werden:
- Auch am Block soll es den parameter 'cornerHeights' geben
- Der Block Editor muss entsprechend erweitert werden
- Im PhysicsModifier soll es einen weiteren boolean 'autoCornerHeights' geben, der bei default false ist.
- autoCornerHeights auch im PhysicsEditor anzeigen.
Die Ermittlung der cornerHeights wird erweitert:
- Pruefen ob am Block cornerHeights existiert, wenn ja diese nutzen
- Pruefen ob am currentModifier.physics das cornerHeights existiert, wenn ja diese nutzen
- Pruefen ob am currentModifier.physics das autoCornerHeights existiert und true ist, wenn ja
  - Pruefen ob am Block offsets gesetzt sind und 24 Werte drin sind, wenn ja diese nutzen (nur die top corners)
  - Pruefen ob an currentModifier die offsets gesetzt sind, wenn ja diese nutzen (nur die top corners)
- Wenn kein cornerHeights existiert, dann wird der default (wie bisher) verwendet.

[x] Erstelle ein command in engine mit dem ich das log level des Loggers umstellen kann, z.b. auf debug.
Das command muss im commandservice registriert werden.

```text
// Log-Level für PhysicsService auf DEBUG setzen
doLoglevel('PhysicsService', 'debug')
// oder
doLoglevel('PhysicsService', 0)

// Log-Level für PhysicsService auf INFO setzen (weniger Logs)
doLoglevel('PhysicsService', 'info')
// oder  
doLoglevel('PhysicsService', 2)

// Alle Logger auf DEBUG setzen
doLoglevel('debug')

// Verfügbare Levels anzeigen
doLoglevel()

Levels:
- 0 / trace - Alles (sehr verbose)
- 1 / debug - Debug-Info (für cornerHeights Debugging)
- 2 / info - Normal (default)
- 3 / warn - Nur Warnungen
- 4 / error - Nur Fehler
- 5 / fatal - Nur kritische Fehler

doLoglevel('PhysicsService', 'debug')
```

[x] Den effect "FLIPBOX" gibt es nicht mehr, entferne ihn aus shred type und editor. (Es gibt aber noch den shape type FLIPBOX, der bleibt bestehen)

[x] AutoCornerHeights soll wieder entfernt werden dafuer automatisch beim shapeType == CUBE aktiv sein
- Aus dem PhysicsModifier entfernen
- Aus dem PhysicsEditor entfernen
- In der Engine immer auf shape == CUBE pruefen wo jetzt autoCornerHeights genutzt wird

## Player State

[x] Alle StackModifier sollen möglichst in ModifierService verwaltet werden. Deshalb wird dort eine Map benoetigt die alle StackModifier hält.
- Erstelle in ModifierService eine Map<string, StackModifier<any>> namens 'stackModifiers'
- Beim anlegen von StackModifieren im ModifierService, werden diese in die Map eingetragen mit dem key = parameterName
- Im PlayerService ist aktuell ein viewModeStack, der kann hier als referenz bestehen bleiben, aber evtl macht es eine Verwaltung ueber den ModifierService sinnvoller und einfacher.
- Wird der stack modifier dispose() dann wird er aus der Map entfernt.

- Wenn wir eine referenz des appContext mitgeben keonne wir zentral im ModifierStack, lieber in eier separaten creator classe alle stack modifier anlegen.
- Vorteil, es muss nicht immer auf verdacht an vielen stellen versucht werden die anzulegen - wie im PlayerService fuer viewModeStack.
- Vorteil: stackModifier sind garantiert da
- Nachteil: Aufgerufene actions sind evtl noch nicht verfuegbar, weil die referenzen noch nicht gesetzt sind. Finde ich akzeptabel, wir brauchen einen try-catch um jede aktion. Am besten gleich im StackModifier update()
- Im PlayerService wird dann die referenz auf den StackModifier nur geholt aus dem ModifierService.

[x] Es wird ein sauberer Player movement State benoetigt. Deshalb soll im PlayerInfo ein status gehalten werden der folgendes beinhaltet:

WALK, SPRINT, JUMP, FALL, FLY, SWIM, CROUCH, RIDING

- Es kann immer nur einer dieser zusaende aktiv sein. Aendert sich der status, wird ein event ausgelost 'onPlayerStateChanged' mit dem alten und neuen status und palyerId.
- Erstelle im PlayerInfo einen neuen parameter 'movementState' vom typ PlayerMovementState (enum). (package shared) der default wert ist WALK
- Erstelle einen StackModifier fuer den Parameter 'playerMovementState' im ModifierService.
- Erstelle einen StackModifier Creator 'playerMovementState' der den status im PlayerService setzt. Default ist WALK
- Erweitere PhysicsService, der soll einen Modifier darauf haben den player status setzen:
  - JUMP - Prio 100
  - FALL (muss spaeter noch implemetiert werden im PhysicsService) - Prio 110
  - SWIM - Prio 120
  Es werden separate Modifier mit verschiedenen Prioritaeten benoetigt.
  werden hier gesetzt.
- Im LayerService selbst wird ein Modfier gesetzt fuer: Hier ist nur ein Modifier noetig, da der status gesetzt wird. besser der default Status im StackModifier kann gesetzt werden.
  - FLY  
  - WALK 
  - SPRINT
  - CROUCH
  Diese Werte werden von aussen durch eine funktion getoggelt.
- PhysicsService und CameraService werden nun nicht mehr direkt mit dem status versorgt, sonder durch das event,
  das im PlayerService ausgelost wird wenn sich der status aendert. Intern werden die gleichen flags gesetzt wie bisher.
- Stelle den InputController fuer FLY um und setze im PlayerService den status auf FLY wenn der player fliegt. und WALK wenn er wieder zurueck toggelt. 
  playerService.setMovementMode( playerService.getMovementMode() === FLY ? WALK : FLY )
- Wenn sich der Movementmode aendert soll wie auch jetzt bei FLY eine Notification via NotificationService ausgelost werden.

[x] Fallen status FALL im PhysicsService implementieren.
- Wenn der player fällt PhysicsService (y velocity < 0) wird die falltiefe addiert in einem parameter en PhysicEntity.
- Ist die Falltiefe groesser als ein threshold (z.b. 2), wird der status auf FALL gesetzt im PlayerService.
- Wird der fall gestoppt (y velocity >= 0) und die falltiefe war groesser als threshold, wird der status wieder auf WALK gesetzt und es wird ein event zum Server 'onPlayerLanded' ausgelost mit der falltiefe.
  - "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md - Es wird der Block, auf den er gefallen ist als position mit gesendet.
- Nach dem landen wird die falltiefe wieder auf 0 gesetzt.

[x] Erstelle InputController fuer die fuer:
- CROUCH - standard taste: N
- SPRINT - standard taste: M
- Implementierung wie bei FLY
[x] Stelle die InputController bitte nochmal um:
- N und M kommen wieder weg
- Der InputController fuer FLY: F rotiert jetzt durch FLY, SPRINT, CROUCH, WALK
- FLY ist nur im EDITOR modus verfuegbar, wie bisher
- Schau mal ob der InputController fuer aktuell FLY immer erstellt wird, aktuell wurde der nur im EDITOR moduls erstellt, das muss jetzt immer sein.
- Wenn der status von FALL auf einen anderen status geht, dann keine Notification

[?] Erweitere den CommandService in engine und test_server so dass Commands asynchron ausgefuehrt werden koennen.
- Erstelle eine neue funktion 'executeAsyncCommand' die ein command asynchron ausfuehrt. return ist void
- Die function gibt oneway: true beim command ausfuehren mit.
- Siehe 'Client Command (Client -> Server)' in client/instructions/general/network-model-2.0.md
- Siehe 'Server Command (Server -> Client)' in client/instructions/general/network-model-2.0.md
- Implemetiere die jeweiligen NetworkHandler so, das bei einem asynchronen command oneway: true keine response gesendet wird.


[?] im PhysicsService wird die Bewegung des Players geprueft und angepasst.
Wenn die bewegung mehr als einen Block pro frame ist, koennte es sein das der player durch einen block hindurch geht.
In diese fall sollte die pruefung in mehreren schritten erfolgen. Jeweils ein schritt von max 0.8 block.

[?] Prepare für Govenor
- Bei laden von Assets wird aktuell immer die Base URL vom NetworkService geladen (getApiUrl) und dann manuell der Pfad angehaengt.
- Erstelle im NetworkService eine funktion getAssetUrl(assetPath: string): string die den kompletten URL zurueck gibt.
- Passe alle stellen an wo asset urls erstellt werden.
- getApiUrl soll private werden und entsprechende funktionen bereit gestellt werden
  - getAssetUrl(assetPath)
  - getEntityModelUrl(entityTypeId)
  - getBackdropUrl(backdropTypeId)
  - getEntityUrl(entityId)

[?] Prepare für Govenor
- Der Aufruf von REST APIs im client appContext.config.apiUrl / config.apiUrl sollte ueber den NetworkService erfolgen.
- Erstelle im NetworkService eine funktion getRestRequestUrl(endpoint: string): string die den kompletten URL zurueck gibt.
- Passe alle stellen an wo REST API urls erstellt werden in 'engine'

[x] Reconnect: Wenn die Verbindung der WebSocket getrennt wird, soll automatisch ein reconnect versucht werden.
Nach dem reconnect muess die session wiederhergestellt werden.
 - Zeige eine Notification "Verbindung verloren, versuche wieder zu verbinden..."
 - Logge dich mit der sessionId wieder ein.
 - Schicke ein event raus, das alle auffordert die session wiederherzustellen.
 - Fange im ChinkServcie das event ab und registriere dich wieder fuer die chunks die vorher geladen waren.
Falls der Reconnect nicht funktioniert, dann spring auf die URL 'exitUrl' die im appContext.config hinterlegt ist. (muss noch erstellt werden, default ist '/login')
 - Reconnect versuchen alle 5 Sekunden bis zu 5 mal.
 - Nach 5 fehlgeschlagenen versuchen, springe auf exitUrl

[?] Ich brauche einen neuen Modus der Bei disconnected und auch bei Death greift.
Der Modus heisst DEAD.
- Im DEAD modus ist Physics deaktiviert.
- Im DEAD modus ist die Cam unterwasser aktiviert.
- Im DEAD modus ist die steuerung inaktiv, jegliche steuerung wird ignoriert.
- Der DEAD modus wird im PlayerService gesetzt, kann aber wieder deaktiviert werden.
- Bei disconnect wird der DEAD modus gesetzt.
- Bei reconnect wird der DEAD modus wieder deaktiviert.
- Erstelle ein Command 'setPlayerDeadState(isDead: boolean)' im PlayerService um den modus zu setzen.
- Die Posse DEAD wird ausgefuehrt, bei reconnect wird wieder auf IDLE gestellt 

[x] Beim stoppen des test_servers (Ctrl+C) soll gewartet werden bis alle chunks und items geschrieben wurden. So sollen currupt files vermieden werden.

[?] Erweitere den CommandService im packet engine so dass multiple commandos in einem aufruf ausgefuehrt werden koennen.
- Siehe "Multiple Commands (Server -> Client)" in client/instructions/general/network-model-2.0.md
- Die bestehende methode wird erweitert, wenn die daten ain Array sind und kein object, dann werden die commandos nacheinander ausgefuehrt. (oder parallel?)

[x] Wenn disconnected im NetworkService setze zusaetzlich im NotificationService eine setCenterText("Disconnected from server") die in der Mitte des Bildschirms angezeigt wird.
- Bei reconnect wird der Text entfernt.

[ ] Erstelle in ClientChunk einen neuen parameter 'ready' der by default false ist
- Sobald der chunk komplett geladen und verarbeitet ist, wird der parameter auf true gesetzt.
- Pruefe in der physik nicht nur darauf, das der chunk vorhanden ist, sondern auch auf ready == true damit der chunk betreten werden kann.
- Das gleiche kann bei teleport gemacht werden, dort muss ready == true sein damit der teleport finished ist.

[x] Ich muss noch das item system erweitern. Items in ItemData.ts sollen einen itemType haben, das ist eine referenz zu einer ItemModifier definition.
- Erweiterung von der server rest api um GET /api/world/{worldid}/itemtype/{type} die daten werden von dep platte unter client/packages/test_server/files/itemtypes geladen (wie auch blocktypes unter
  client/packages/test_server/files/blocktypes).
- Erweiterung ItemData.ts
- Im ItemService das downloaded und cache der ItemTypes implementieren, im NetworkSerice eine getItemTypeUrl(...) anlegen und nutzen.
- Mergen beim laden von Items von ItemType und custom modifiers.
- Ich sehe noch ein problem, es muss einen separaten ItemModifier geben, der dann den BlockModifier in einem prarameter hat, denn onUseEffect soll auch im ItemType enthalten sein.
- Ich sehe in Item das problem: Es wird nur ein BlockModifier benoetigt, hier ist aktuell eine Map in modifier enthalten. ggf. ersetze das komplett durch ItemModifier mit einem TexturePath (immer string)
- Wo wird Item genutzt? in ChunkService, kann das hier adaptiert werden mit einer einfacheren Item/ItemModifier Struktur. onUseEffect kann dann auch in ItemModifier rein. 

[x] Wenn Effekte ausgeloest werden, muessen die zum server gesendet werden, damit diese auch auf anderen clients ausgefuehrt
werden koennen.
Es werden also zwei neue network messages benoetigt:
- Client to Server: 'effect.trigger' mit EffectTriggerData, name 'e.t'
  - siehe "Effeckt Trigger (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Server to Client: 'effect.trigger' mit EffectTriggerData, name 'e.t'
  - Siehe "Effeckt Trigger (Server -> Client)" in client/instructions/general/network-model-2.0.md
- Der Server muss die events empfangen und an alle anderen clients, die diese chunks regsitriert haben, senden (broadcast)
- Der Client muss die events empfangen und den EffectService aufrufen um die Effekte auszufuehren.
- Der Client muss Effekte die local ausgefuehrt werden (z.b. durch Item use) auch an den server senden. Die chunk daten muessen ausgefuellt werden.
- Ggf benennen wir im ScrawlService die scriote in 'local_...' und 'remote_...'
- Achtung, wenn effecte via Server kommen, diese nicht nochmal an den server senden.
- Sinn macht ein flag in ScriptActionDefinition 'sendToServer?: boolean' default true, wenn false, dann nicht senden.
  - damit kann auch ein local effect gestartet werden, der nicht an den server gesendet wird.

[?] Wenn ein effect parameters geupdated wird, muss das update an alle clients gesendet werden die den chunk geladen haben.
- Client to Server, siehe "Effect Parameter Update (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Server to Client, siehe "Effect Parameter Update (Server -> Client)" in client/instructions/general/network-model-2.0.md
- Es macht Sinn die affected chunks mit am ScriptActionDefinition zu speichern, dann muss das nru einmal berechnet werden
- Wie bei EffectTrigger muessen die Events verteilt werden.
- Im Client event empfangen, mit 'remote_' + effectId den EffectService aufrufen um die parameter zu updaten.
- Im Client bei 'local_" effecten bei parameter update und wenn sendToServer==true , an den server senden.

[?] In engine wird die worldInfo aktuell aus dem websocket geladen. Das soll anders werden. 
- Schicke im server nicht mehr die worldInfo via websocket
- Rufe in engine vor dem verbinden die REST API GET /api/world/{worldId}/config auf um alle configs zu laden.
- Erstelle einen ConfigService der sich um das laden und cachen der configs kuemmert. Nutze den NetworkService wenn noetig. Der service wird in AppContext referenziert
- Erstelle ein command mti dem die einzelnen configs im laufenden betrieb neu geladen werden.
- Siehe client/packages/shared/src/configs/EngineConfiguration.ts
- Siehe client/instructions/general/server_rest_api.md Abschnitt "Configs"

[ ] Erstelle ein Command in engine mit dem ein spezieller oder alle chunks neu gerendert werden.
- Erstele dazu erst im ChunkService eine funktion 'redrawChunk(chunkX: number, chunkZ: number)' die den chunk neu rendert.
- Erstelle im ChunkService eine funktion 'redrawAllChunks()' die alle geladenen chunks neu rendert.
- Erstelle im CommandService ein command 'redrawChunk' mit den parametern chunkX?: number, chunkZ?: number
  - Wenn chunkX und chunkZ gesetzt sind, rufe 'redrawChunk' auf
  - Wenn keine parameter gesetzt sind, rufe 'redrawAllChunks' auf

[ ] Wenn beim WorldInfo update sich der status aendert, muessen alle currentModifier in ChunkService neu errechnet werden und die chunks neu gerendert werden.
- Es soll ausserdem eine 'seasonStatus' und 'seasonProgress : number' in der WeltInfo geben. Auch hier muss bei aenderung die currentModifier neu berechnet werden.
