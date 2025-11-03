
# Physics

## Movement

[ ] Support PhysicsModifier.autoClimbable while moving
[ ] Support PhysicsModifier.autoJump
[ ] Bonus: Rename PhysicsModifier.autoMoveXYZ to PhysicsModifier.autoMove
[ ] Bonus: Rename autoMoveY to autoMoveZ
[ ] Support PhysicsModifier.autoMove
[ ] Support PhysicsModifier.autoOrientationY

> Naming:
> rotation: The physical rotation of an entity in 3D space (i.e. its angle around the X, Y, and Z axes).
> orientation: The direction an entity is facing or moving towards. / could be a rotation if technical
> turn: The rotation delta in time of an object.

[ ] Support PhysicsModifier.climbable
[ ] Bonus: Rename PhysicsModifier.gateFromDirection to PhysicsModifier.passableFrom
[ ] Support PhysicsModifier.passableFrom
- Wenn der Block solid ist, dann funktioniert passableFrom wie eine Aufhebung, d.h. von der seite kannst du rein und auf alle seiten raus, wo kein solid block ist - nach oben moven weil in einem solid block ist aufgehoben sobald passableFrom am Block ist.
  Damit wird der Block zu einem One-Way-Block. Auch nach oben oder unten wirkt diese Einstellung.
- Wenn der Block nicht solid ist, dann funktioniert passableFrom wie ein Blocker aus dem block am rand, man kann weder in den block noch raus, wo kein passableFrom angegeben ist, aber der block ist begehbar.
  Damit wird der block wie eine Wand, du kannst hin, aber nicht drueber hinaus. Auch nach unten oder oben wirk diese Einstellung.
  Wird aktiv sobald passableFrom am Block haengt.

## Editor

[ ] Die Parameter im PhusicsEditor bearbeitbar machen: autoClimbable, autoJump, autoOrientationY, passableFrom
> [ ] prüfen ob alle Parameter Editierbar sind und einen Effect haben.

## Bugs

[ ] Wenn der Bildschirm/Browser Fenster inaktiv ist, faellt der user immer durch alle blocks durch bis zum unteren limit.
