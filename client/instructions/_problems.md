
[ ] In PhysicsServer passableFrom wenn solid aus ist funktioniert nicht richtig

[x] ClientChunk aufbereitung sollte optimiert werden

[x] Wenn browser idle im background faellt man durch die Blocks

[x] Collision detection beim Start ist immernoch bugy, geloest durch hohen einsprung
Sollte aber mit teleportationPending behoben sein. Ist es aber nicht.

[x] Claude hat in die Texture neben effect einen shader parameter eingefuegt, der muss wieder raus.

[ ] MovementMode fuer player audio kommt vom PlayerService, das wird ein problem wenn NPCs simuliert werden.
[ ] sound 'audio/step/swim1.ogg' muss aus der World Config kommen

- Nenne parameters in scrawl um in variables

- Im Material Editor probleme beim setzen von Transparency
- Im Material Editor beim oeffen des editors, in der liste, item immer neu laden.

- Im Sprint modus kein Jump
- autoJump als number ? Jump height?

[ ] Alle services sollen im AppContext bzw. in den unteren Services wie RenderService referenziert sein. 
Pruefe ob das so umgesetzt ist, es gab jetzt immer wieder faelle, da wurde das nicht erledigt.
Auch wenn es nicht noetig ist, aber das ist das Konzept.


