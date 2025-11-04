
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
