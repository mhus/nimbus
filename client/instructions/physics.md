
# Physics

## Movement

[x] Support PhysicsModifier.autoClimbable while moving
- Wenn ein User an einem autoClimbable stößt, dann erklimmt er diese automatisch, funktioniert nur bei einem Block, ist bereits implementiert (PhysicsServcie),
  wird aber aktuell immer gemacht. Soll in Zukunft nur bei Blocks mit dieser Eigenschaft funktionieren.
- Default ist kein autoClimbable (false)
- Baue das Feld auch im PhysicsEditor (nimbus_editors) ein
> Ist es nicht besser dort anzusetzen, wo autoClimb ausgefuehrt wird, anstelle es im nachgang wieder rueckgaengig zu machen?
- Es wird noch weitere physische eigenschaften von umgebenen bloecken geben, ist es schlau da eine funktion zu bauen, die das alles zusammen macht,
    - Also erstmal die noetigen Blocke laed, unter dem player, vor dem player und der block(e) in dem player steht.
    - Dann entscheiden was zu zun ist.

[x] Support PhysicsModifier.autoJump
- Wenn ein User auf oder in ein Feld mit dieser eigenschaft kommt, wird automatisch ein jump ausgeloest
- Default ist kein autoJump (false)
- Baue das Feld auch im PhysicsEditor (nimbus_editors) ein
- Baue das da ein, wo jetzt auch autoClimb umgesetzt wird. (?oder ist das anders besser?)
>   Beide sollen geprueft werden, der block der fuesse und der darunter. Das sind dann auch alle die geprueft werden sollen.

[x] Bonus: Rename PhysicsModifier.autoMoveXYZ to PhysicsModifier.autoMove
- Auch im PhysicsEditor 

[x] Support PhysicsModifier.autoMove
- Wenn der Player mit den fuessen auf oder in das Feld kommt, wird er automatisch bewegt
- Default ist kein autoMove (false)
- Baue das Feld auch im PhysicsEditor (nimbus_editors) ein
- Die Bewegung soll fluessig sein mit der geschwindigkeit aus dem parameter bis der user aus/von dem Feld ist. 
- Sind mehrere Felder, wird fuer X Y Z das maximum benutzt

[x] Support PhysicsModifier.autoOrientationY
- Wenn der Player mit den fuessen oder in das Feld kommt, wird er automatisch in diese richtung gedreht (player orientation), bis er die richtung erreicht hat.
- Default ist kein autoOrientationY (false)
- Baue das Feld auch im PhysicsEditor (nimbus_editors) ein
- Die Drehung soll fluessig sein mit einer standart geschwindigkeit.
- Sind mehrere Felder, wird fuer die orientation die letzte benutzt.

> Naming:
> rotation: The physical rotation of an entity in 3D space (i.e. its angle around the X, Y, and Z axes).
> orientation: The direction an entity is facing or moving towards. / could be a rotation if technical
> turn: The rotation delta in time of an object.

[x] Support PhysicsModifier.climbable
- Wenn der User an diese Feld lauft (feld ist vor ihm), dann klettert er automatisch nach oben anstelle weiter nach vorn
- Der Wert in dem Feld ist die klettergeschwindigkeit
- Ist das Feld nicht gesetzt oder 0 ist die funktion aus (keine kletterung)
- Baue das Feld auch im PhysicsEditor (nimbus_editors) ein

~~[ ] Wenn vor mir ein climbable block ist, ich nict auf dem boden (unter den fuessen kein block) und ich mich rueckwaerts~~
  bewege, dann soll ich mich mit climbable geschwindigkeit nach unten bewegen.
  - ähnlich wie bei moveForward jetzt noch in moveBackward 
> Man faellt autom wieder runter wenn man nicht mehr auf W drueckt, das ist gut

~~[-] Wenn ich nach oben schaue (player pitch) soll ich nach oben gehen, wenn ich nach unten schaue nach unten.~~

[?] Support PhysicsModifier.passableFrom in PhysicsService
- Aktuell gibt es das Feld PhysicsModifier.gateFromDirection, es soll umbenannt werden in passableFrom
- Es ist eine Bitmap fuer jede direction ein Bit. BlockModifier.Direction
- Wenn der Block solid ist, dann funktioniert passableFrom wie eine Aufhebung, d.h. von der seite kannst du rein und auf alle seiten raus, wo kein solid block ist - nach oben moven weil in einem solid block ist aufgehoben sobald passableFrom am Block ist.
  Damit wird der Block zu einem One-Way-Block. Auch nach oben oder unten wirkt diese Einstellung.
- Wenn der Block nicht solid ist, dann funktioniert passableFrom wie ein Blocker aus dem block am rand, man kann weder in den block noch raus, wo kein passableFrom angegeben ist, aber der block ist begehbar.
  Damit wird der block wie eine Wand, du kannst hin, aber nicht drueber hinaus. Auch nach unten oder oben wirk diese Einstellung.
  Wird aktiv sobald passableFrom am Block haengt.
- Baue das Feld auch im PhysicsEditor (nimbus_editors) um: Benutze checkboxen fuer die flags die in einer Zeile stehen. Ein Button fuer reset um das feld wieder zu loeschen.
- Es gibt eine funktion, die mich automatisch nach oben schiebt, wenn ich IN einem solid block bin, die funktion soll mich nicht nach oben schieben, wenn passableFrom existiert.
- Wenn der Block nicht solid ist soll das durchgehen in beide richtungen, aber an der kante verhindert werden
  Beispiel: Block NORTH (NORTH ist nicht enabled in passableFrom)
  - Der player kommt von NORTH, kann nicht in den Block rein
  - Der Player kommt von SOUTH, kann in den Block rein, aber nach NORTH nicht raus.
- Das muesen wir klaeren: passableFrom = east bedeutet von east kann man rein und nach west (opposit) kann man raus.

Noch mal erklaert:

Dieser Block ist solid, man kann nicht rein:
   +---+ 
-->|   |<---
   +---+

Dieser Block ist non solid, und passableFrom != EAST
   +---+
------>|<---
   +---+

Dieser Block ist non solid, und passableFrom != WEST
   +---+
-->|<-------
   +---+

Du brauchts eine tagetBlock (aktuell currentLevel) und einen sourceBlock (der verlassen wird).
wir kommen von WEST nach EAST, dann pruefst du:
canLeaveFrom(sourceBlock, EAST)
canEnterFrom(targetBlock, WEST)

passableFrom ALL But not WEST: (westliche seite ist nicht passierbar)
   +   +
-->|<-------
   +   +
- von West nach Ost: an der ausseren grenze stoppen
- von Ost nach West: in den Block hinein, aber dann nicht mehr hinaus. richung West

Wir sprechen von Block 10,10 (Y Achse spielt keine Rolle)
canLeaveFrom(10,10, EAST) = FALSE
canEnterFrom(10,10, WEST) = FALSE
Alle anderen TRUE:
canLeaveFrom(10,10, WEST) = TRUE
canEnterFrom(10,10, EAST) = TRUE
canLeaveFrom(10,10, NORTH) = TRUE
canEnterFrom(10,10, NORTH) = TRUE
canLeaveFrom(10,10, SOUTH) = TRUE
canEnterFrom(10,10, SOUTH) = TRUE

passableFrom = ALL but WEST -> passableFrom=TOB,BOTTOM,TOP,NORTH,SOUTH,EAST

Können Sie bitte ganz klar sagen:
- Block 10,10 hat passableFrom = ALL but WEST
- Spieler ist IN Block 10,10 bei Position (10.5, 10.5)
- Spieler bewegt sich nach WEST zu Position (9.9, 10.5)
- Sollte diese Bewegung erlaubt oder blockiert sein?

Hier muss aufgerufen werden:
Hier habe ich eine Verbesserung: canLeaveFrom kann canLeaveTo sein und das kehrt die Richtung um, dann sind beide abfragen aehnich:
canLeaveTo(10,10, WEST) = FALSE (opposite von canLeaveFrom(10,10, EAST) )
canEnterFrom(9,10, WEST) = ??? kenne ja den block nicht





[ ] Support PhysicsModifier.passableFrom Top/Bottom bei solid = false
- wenn solid = false und passableFrom !+ undefined und passableFrom != top , dann kann man in den Block nicht hinein fallen, d.h. gravitation hoert hier auf, man ist auf solidem grund
- wenn solid = false und passableFrom !+ undefined und passableFrom != bottom, dann kann man aus dem Block nach unten nicht raus fallen, obwohl dronter eventuell AIR oder non solid ist.

## Editor

[ ] Die Parameter im PhysicsEditor bearbeitbar machen: autoClimbable, autoJump, autoOrientationY, passableFrom
> [ ] prüfen ob alle Parameter Editierbar sind und einen Effect haben.

## Bugs

[ ] Wenn der Bildschirm/Browser Fenster inaktiv ist, faellt der user immer durch alle blocks durch bis zum unteren limit.

---

## Physics Movement Requirements

- Im Teleport Modus, pruefen ob modus ausgemacht werden kann, sonst bewegung ignorieren und weg (alter teleport loop kann weg), teleportg mode ist fuer den main player, also singleton
- Gravitation
- Auto Move
- Auto Orientation Y
- Auto Jump
- Jump
- Ränder der Welt respektieren (world border)
- Passable From Direction (One Way Block) if block is solid
- Passable Out of Block if block is non solid (Block Barrier)
- Block half solid (solid marked, but with cornerHeights or autoCornerHeights == true):
  - cornerHeights/offsets slope sliding (gravitation and resistance affected)
  - cornerHeights/offsets slope movement
- Block Solid
  - Auto Climbable Block (max height 1)
- crawling

Was ist 'nicht solid':
- Wenn am block solid == false oder solid is undefined, dann ist der block non solid

Was ist 'semi solid'/slope: - Ein block der zwar solid definiert ist, aber dennoch (teilweise) durchlaufen werden kann, weil er schräg/slope ist:
- Wenn der block solid == true und 
  - cornerHeights gesetzt ist oder 
  - autoCornerHeights + offsets definiert sind

Was ist 'semi solid moveable':
- Wenn der block semi solid ist und der hoehen unterschied von der aktuelle position auf den neuen block <= maxClimbHeight ist (default 0.1 block)

Welche Entscheidung muesen wir treffen:
- Bewegt sich der player ueber eine Blockgrenze, d.h. Math.floor(player.position.x/y/z) veraendert sich
- Was machen wir, wenn sich die bewegungsrichtung während der berechnung ändert? z.b. gravitation und autoMove
  Trenne Absicht von Physik:
  - Input: movementVector (Absicht des Spielers), velocity (momentane Geschwindigkeit des Spielers aus letzen frame)
  - bei berechnung den velocity fuer die nacheste frame bestimmen
  - am anfang das uer movement mit velocity anpassen 
  - velocity rueck rechnen (verkleinern bis 0,0,0 basierend auf widerstand, wiederstand muss > 0 sein, sonst unendliche bewegung)
  - am ende die position des aktuellen moves setzen
- Es kann planar und Y separat berechnet werden, da gravitation/jump immer nur Y betrifft ???

Welche Blocke werden zur analyse benoetigt:
1. Aus PhysicsEnttity muss geladen werden wieviele blocke der player nach oben/breite belegt. 
   - z.B. Laufen: 2 blocks hoch, 1 block breit (default)
   - z.B. Kriechen: 1 block hoch, 1 block breit
2. Aus PhysicsEnttity muss geladen werden wieviele blocke der player am Grund belegt (footprint)
   - z.B. 1x1 blocke (default)
   - z.B. 1x2 blocke (Pferd)
3. Player Position (x,y,z) und Blickrichtung (pitch, yaw)
4. Aktueller Level (Lade alle benoetigten (!) Blocke in einem Bereich um den Player herum)
   - Bestimme die Blocke die der Player belegt (currentBlocks) - Muessen alle semi solid oder non solid sein, sonst bewegung nach oben Y+1
5. Bestimme die Blocke die der Player beruehrt
   - Bestimme die Blocke die der Player betritt (enteringBlocks) - muessen alle semi solid oder non solid sein, sonst collison
   - Bestimme alle Blocke vor dem Player (frontBlocks) - muessen alle semi solid oder non solid sein, sonst collison
   - Bestimme denn Block vor bei Fuessen des Players (footBlocks) (die fuesse stehen vorn drin) - Entscheident fuer eine Spezialbewegung: 
   - Bestimme denn Block vor den Fuessen des Players (footFrontBlocks) - Entscheident fuer eine Spezialbewegung: autoClimbable, slope sliding, slope movement, cimping
   - Bestimme alle Blocke direkt unter dem Player (groundBlocks) - Entscheident fuer Gravitation und Widerstand und effekten wie AutoMove, AutoOrientationY, AutoJump
   - Bestimme alle Blocke ueber ueber dem Player (headBlocks) - Entscheident fuer collison wenn player nach oben springt

    Bild:
    
    ```
         1   2     3    
      +----+----+----+----+
    Z |    |    |    |    |
      +----+----+----+----+
    A |    | <  | <  |    |
      +----+----+----+----+
    B |    | <  | <  |    |
      +----+----+----+----+
    C |    |    |    |    |
      +----+----+----+----+   
      
      Y
      |
      --- X
      
      Bewegung <----
      
    ```
    
    Player bewget sich nach links (X-)
    - Player Position (2.5, C, Z)
      - Player Blickrichtung (pitch, yaw) = (0, 270) (nach links schauend)
      - Player Groesse = 2 blocks hoch (Y), zwei block lang (X), 1 block breit (Z)
      - foot ist bei player.position.x - 1
    
      - currentBlocks (2,B), (3,B), (2,A), (3,A)
      - enteringBlocks (1,A), (1,B)
      - frontBlocks (2,A), (2,B)
      - footBlocks (2,B)
      - footFrontBlocks (1,B)
      - groundBlocks (2,C), (3,C)
      - groundFootBlocks (2,B) (3,B) - eins ueber den groundBlocks, bei den Fuessen

Vorgehen mit und ohne bewegung (ohne bewegung koenne die gefundenen bloecke in PhysicsEntity cached werden):

// aufruf:
doMovement(player, movementVector, startJump):
-> player - der player entity
-> movementVector - input bewegungsvektor aus input system (keyboard/mouse/controller)
-> startJump - boolean, ob ein sprung gestartet werden soll (space gedrueckt)

// weitere input daten
-> physicsEntity.velocity - fuer eine fluessige bewegung ueber frames hinweg
-> physicsEntity.wishMove - movementVector aus dem letzten step
-> physicsEntity.position - aktuelle position des players
-> physicsEntity.rotation - aktuelle rotation des players

// Vorbereitung:
- harte Bedingnung: Ist system in teleport modus, dann pruefen ob fuer den main 'player' der Chunk bereit steht, wenn ja, teleport mode ausmachen, wenn nein bewegung verhindern.
- harte Bedingnung: ist im fligthModus, dann bewegung ausfuehren und exit.
- vorberechnung:
  - movementVector = movementVector + wishMove + velocity (exakte formel finden)
  - set wishMove = 0
  - set velocity = velocity x resistance (verkleinern, exakte formel finden)
  - wishNextPosition = position + movementVector
  - overBlockMovement = Math.floor(wishNextPosition) != Math.floor(position)
- daten einholen: Bloecke ermittlen / aus cache laden - headBlocks Blocke on demand laden
- harte Bedingnung: Konnten Blocke nicht geladen werden weil chunks nicht da sind, dann bewegung verhindern und exit.

// pruefen current environment - immer, auch wenn kein blockwechsel
- Ist einer der currentBlocks solid, dann:
  - pruefe ob er (jeder gefundene block mit solid) passableFrom
    - movementVector bei allen nicht erlaubten richtungen zurueck auf 0 setzen
    - pruefen ob sich wishNextPosition noch aendert, 
    - wenn ja bewegung verhindern, wishMove fuer naechsten step setzen und exit
  - wenn alle solid: bewegen den player 1 nach oben (Y+1) und exit (anty catching, keine weitere pruefung, mit dem kopf durch die Wand)
- Bei bewegung nach vorn (overBlockMovement): Ist einer der footFrontBlocks 'climbable', dann climb:
    - wenn bewegung nach vorne, dann wishMove nach oben setzen und exit (bewegung verhindern)
- gibt es footBlocks:
  - ist ein food autoRotationY, dann einen step ausfuehren richtung autoRotationY (kein exit)
  - ist ein foot autoMove, dann widhMove in richtung autoMove setzen (kein exit)
  - ist ein foot autoJump, dann einen jumpStart=true aktivieren (kein exit)
- sind alle footBlocks non solid und einer mit passableFrom und passableFrom.BOTTOM == false (kann nicht von/nach unten passieren) dann nicht nach unten bewegen (keine gravitation)
  - sind alle groundBlocks non solid und ohne passableFrom oder wenn, mit passableFrom.TOP == true (kann von oben passieren):
    - bewege den player nach unten (gravitation) -> move = move + (0, -gravity speed, 0)

// jump vermerken
- wenn physicsEntity.velocity Y == 0 (nicht schon in der luft)
  - wenn startJump == true
    - setze velocity Y = jump speed

// bewegung nach unten auf semi solid blocks
- sind die groundFootBlocks mindestens einer semi solid
- ermittle die maximale hoehe ueber alle semi solid blocks an der nextWishPosition
- wenn die maximale hoehe < wishNextPosition.y, dann setze wishNextPosition.y auf die maximale hoehe (slide up)
- checke hier auch auf slide down und setze evtl den velocity oder wishMove nach unten und zur seite - schwierig bei mehreren unterschiedlichen blocke - hier den mittleren wert nehmen?
  // wo bauen wir gravitation ein???? hier ??? gravitation nicht, wenn obere bedingung slideDown gemacht hat. wenn alle groundFoodBlocks non solid sind, dann gravitation immer machen

// jetzt die Bewegung pruefen, wenn ueber block grenzen gewechselt wird, auch nach oben oder unten

// nach oben
- headBlocks laden und pruefen ob die alle non solid sind. wenn nicht, dann wishNextPosition.y auf den rand des oberen endes des blocks setzen (keine bewegung nach oben)
// nach unten
- ground blocks laden und pruefen
  - alle ground blaocks sind non solid -- bewegung ist ok
  - alle ground blocks sind solid, ahebn kein passableFrom, haben kein cornerHeights bzw. autoCornerHeight -- bewegung ist nicht ok, setze wishNextPosition.y auf den unteren rand des blocks
  - wenn semi solid blocks dabei sind, dann den maximalen wert der 
// nach vorn
- pruefe bloecke vorn aber schon betreten
  - Alee ersten blocks pruefen ob non solid, aber passableFrom in diese richtung nicht erlaubt (von innen), z.b. richtung Norden und passableFrom.BACK == false
    - wenn ja, dann setze wishNextPosition.x/z auf den rand des blocks
- pruefe bloecke vorn 
  - pruefe ob die blocks vor dem player ob er durchlaufwen werden kann
    - ist er non solid, hat kein passableFrom
    - ist er non solid und hat passableFrom, aber in diese richtung erlaubt (von aussen), z.b. richtung Norden und passableFrom.FRONT == true
    - ist er solid, hat passableFrom in diese richtung erlaubt (von aussen), z.b. richtung Norden und passableFrom.FRONT == true
    - ist er semi solid, hat eine gap von weniger als 0.1 block (maxClimbHeight)
    - wenn keiner der obigen punkte zutrifft, dann  vermutung auf collison
      - wenn collsion vermutet: pruefe ob alle blocks oberhalb des ersten blockes non solid sind (headBlocks), aber in der ersten reihe nicht aber alle haben autoClimb
        - dann sgarte auto climb jump (setze wishMove.y und velocity)
// nach hinten
- pruefe blocke hinten aber schin betreten
  ...
- pruefe bloecke hinten noch nicht betreten
  ...
// nach links
- pruefe bloecke links schon betreten
  ...
- pruefe bloecke links noch nicht betreten
  ...
// nach rechts
- pruefe bloecke rechts schon betreten
  ...
- pruefe bloecke rechts noch nicht betreten
  ...

// pruefe auf render der welt
- pruefe auf heightDate dieser column aus ClientChunk
  - wenn wishNextPosition.y < heightDate.minHeight, dann setze wishNextPosition.y = minHeight
  - wenn wishNextPosition.y > heightDate.maxHeight, dann setze wishNextPosition.y = maxHeight
- pruefe auf welt grenzen (world border)
  - wenn wishNextPosition.x < world.minX, dann setze wishNextPosition.x = world.minX
  - wenn wishNextPosition.x > world.maxX, dann setze wishNextPosition.x = world.maxX
  - wenn wishNextPosition.z < world.minZ, dann setze wishNextPosition.z = world.minZ
  - wenn wishNextPosition.z > world.maxZ, dann setze wishNextPosition.z = world.maxZ
  - wenn wishNextPosition.y < world.minY, dann setze wishNextPosition.y = world.minY
  - wenn wishNextPosition.y > world.maxY, dann setze wishNextPosition.y = world.maxY

// Wende wishNextPosition an
- setze player.position = wishNextPosition
