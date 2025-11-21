
[ ] In PhysicsServer passableFrom wenn solid aus ist funktioniert nicht richtig

[ ] ClientChunk aufbereitung sollte optimiert werden

[ ] Wenn browser idle im background faellt man durch die Blocks

[ ] Collision detection beim Start ist immernoch bugy, geloest durch hohen einsprung
Sollte aber mit teleportationPending behoben sein. Ist es aber nicht.

[ ] Claude hat in die Texture neben effect einen shader parameter eingefuegt, der muss wieder raus.

- MovementMode fuer player audio kommt vom PlayerService, das wird ein problem wenn NPCs simuliert werden.
- sound 'audio/step/swim1.ogg' muss aus der World Config kommen

- Nenne parameters in scrawl um in variables

- Wenn bei texturen nur string angegeben wurde (keine komplexe), soll:
  - backfaceCulling true sein
  - Transparency auf auf ALPHAT_TEST sein
????

