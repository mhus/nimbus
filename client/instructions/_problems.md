[x] ClientChunk aufbereitung sollte optimiert werden

[x] Wenn browser idle im background faellt man durch die Blocks

[x] Collision detection beim Start ist immernoch bugy, geloest durch hohen einsprung
Sollte aber mit teleportationPending behoben sein. Ist es aber nicht.

[x] Claude hat in die Texture neben effect einen shader parameter eingefuegt, der muss wieder raus.

[x] sound 'audio/step/swim1.ogg' muss aus der World Config kommen

[x] Nenne parameters in scrawl um in variables

[x] Im Material Editor probleme beim setzen von Transparency
[x] Im Material Editor beim oeffen des editors, in der liste, item immer neu laden.

[x] Im Sprint modus kein Jump
[x] autoJump als number ? Jump height?

[-] Alle services sollen im AppContext bzw. in den unteren Services wie RenderService referenziert sein.
Pruefe ob das so umgesetzt ist, es gab jetzt immer wieder faelle, da wurde das nicht erledigt.
Auch wenn es nicht noetig ist, aber das ist das Konzept.

[x] **CRITICAL: Shadows funktionieren NICHT**
- Shadow System wurde in EnvironmentService implementiert (CascadedShadowGenerator)
- WebGL2 ist aktiv und funktioniert
- Alle BabylonJS Settings sind korrekt (shadowsEnabled: true, receiveShadows, renderList, etc.)
- Test-Scene wurde EXAKT wie funktionierendes BabylonJS Playground-Beispiel aufgebaut
- ABER: Keine Schatten sichtbar - weder auf Test-Meshes noch auf Voxel-Chunks
- Vermutung: Fundamentales Problem in der Rendering-Engine oder fehlende Bibliothek/Parameter
- Commands zum Testen: shadowsSimpleTest, shadowsSystemDebug, shadowsInfo
- Siehe: instructions/shadows.md für Details

[-] LensFlares funktionieren nicht richtig - wird nicht angezeigt - race condititon in BabylonJS: 'undefined effect'
[x] Selection Box funktioniert nicht richtig - wird nicht angezeigt

[-] MovementMode fuer player audio kommt vom PlayerService, das wird ein problem wenn NPCs simuliert werden.
> wird ggf im server gemacht

[ ] In PhysicsService passableFrom wenn solid aus ist funktioniert nicht richtig

[x] In CylinderRenderer in packages/engine/src/renderers/CylinderRenderer.ts werden die Top und Bottom Flächen gezeichnet, diese werden aber nicht mit
roteirt, so wie der Zylinder. Das muss gefixt werden.

[-] In BlockModifier.ts gibt es effect:BlockEffect in VisibilityModifier (fuer den gesamten Block) und nochmals an
TextureModifier (fuer die Textur). Das ist nicht sinnvoll. Prüfe ob beide effect Parameter benutzt werden.
```text
Fazit:

  Das Design ist sinnvoll und korrekt implementiert. Es erlaubt:
  - Globalen Effect für den ganzen Block (VisibilityModifier)
  - Pro-Textur Overrides (TextureDefinition)
  - Z.B. könnte ein Block WIND als Default haben, aber die TOP-Textur könnte NONE verwenden

  Empfehlung: Keine Änderung nötig - beide Parameter erfüllen ihren Zweck.
```

[x] Es gibt in packages/shared/types die Dateien WLayer.ts und WLayerModel.ts diese sollten nicht genutzt werden.
Bitte pruefe das.
[x] Die Dateien sollten gleich denen in packages/shared/generated/types sein.
[x] Es sollen stadtdessen die Dateien in packages/shared/generated/types genutzt werden. Die Beiden Dateien in
packages/shared/types sollen entfernt werden.
```text
 Ich sollte:
  1. ✅ Die generierten Versionen verwenden (Import von @nimbus/shared anpassen)
  2. ✅ layer.ground → layer.baseGround ändern in LayerListItem.vue
  3. ✅ LayerType Export in index.ts hinzufügen
  4. ✅ Manuelle WLayer.ts löschen (wird durch generierte ersetzt)
```

[x] Im Block Type Editor wenn neu gedrückt wird, kommt ein Dialog "Create Block Type".
Hier ist die ID ein Integer, aber die ID wurde auf String umgestellt. Das muss gefixt werden.
