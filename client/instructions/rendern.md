

## Rendern

[x] Beim Rendern werden Blocks mit shape Spheren als Cube dargestellt.
[x] Es koennten noch die Werte aus edgeOffsets genutzt werden um die Sphere zu manipulieren. Offset values support floats.
- Left-Back-Button um die Sphere zu verschieben
- Right-Back-Button um den Radius (X Y Z) oben zu verkleinern
  [x] Beim Rendern werden Blocks mit shape Column als Cube dargestellt, nicht als Column.
  [x] Es koennten noch die Werte aus edgeOffsets genutzt werden um die Column zu manipulieren. Offset values support floats.
- Left-Back-Button um die Column unten zu verschieben
- Left-Back-Top um die Column oben zu verschieben
- Right-Back-Button um den Radius oben zu verkleinern
- Right-Back-Top um den Radius unten zu verkleinern
  [x] Beim Rendern werden Blocks mit shape Cross nicht richtig dargestellt, es wird nur ein teil des cross angezeigt.
  [x] Es koennten noch die Werte aus edgeOffsets genutzt werden um das Cross zu manipulieren. Offset values support floats.
  Wie bei Cube auch, koennen die Werte alle 8 Ecken des Cross verschieben.
  [x] Beim rendern wird der Parameter 'rotation' nicht beruecksichtigt. Der Parameter kann alle shapes beeinflussen.
  [x] Beim rendern wird der Parameter 'facing' nicht beruecksichtigt. Der Parameter kann alle shapes beeinflussen.
  [x] Beim rendern wird der Parameter 'color' nicht beruecksichtigt oder aus dem Editor nicht richtig übernommen. Der Parameter kann alle shapes beeinflussen.
  [x] Beim Rendern werden Blocks mit shape Hash nicht richtig dargestellt, sie werden wie Cube dargestellt.
- Es sollen unabhängige seiten gerendert werden die mit edgeOffset verschoben werden koennen, sie bleiben dabei aber als Quadrate erhalten.
- Es werden also nicht die Ecken verschoben, sondern die Seiten.
- Bitte rotationMatrix beachten.
```text
  | Fläche             | Corner Index | Array Indices | Beschreibung                              |
  |--------------------|--------------|---------------|-------------------------------------------|
  | Top (oben)         | Corner 0     | [0, 1, 2]     | Verschiebt die obere Fläche (y = y + 1)   |
  | Bottom (unten)     | Corner 1     | [3, 4, 5]     | Verschiebt die untere Fläche (y = y)      |
  | North (vorne, +Z)  | Corner 2     | [6, 7, 8]     | Verschiebt die vordere Fläche (z = z + 1) |
  | South (hinten, -Z) | Corner 3     | [9, 10, 11]   | Verschiebt die hintere Fläche (z = z)     |
  | East (rechts, +X)  | Corner 4     | [12, 13, 14]  | Verschiebt die rechte Fläche (x = x + 1)  |
  | West (links, -X)   | Corner 5     | [15, 16, 17]  | Verschiebt die linke Fläche (x = x)       |
```
[x] Wenn bei Hash eine edgeOffset X einen wert von 127 hat, soll die seite nicht angezeigt werden. So koennen wir durch einen trick die seite ausblenden.
[x] Es fehlt ein rotationsparameter wir koennen aktuell nur in eine richtung rotieren, sollten aber auch in die andere richtung rotieren koennen.
Die Idee, wir rotieren erst in die eine richtung (schon implementiert) und dann in die andere richtung (noch nicht implementiert) und koennen den block dann in alle positionen rotieren.
- Diese Parameter soll in BlockModifier gesichert werden.
- Er soll im Block-Editor editierbar sein und sofort live zu sehen sein.
  [x] Rotation in 1-360 Grad in 1er schritten für beide Richtungen.
- Anpassen des Renderns
- Im Block Editor aus Dropdown ein Eingabefeld machen, die beiden Eingabefelder X Y koennen in eine Zeile gelegt werden.
- Muss beim Netzwerktransport etwas gemacht werden?
  [x] Beim Rendern werden Blocks mit shape Glass nicht richtig gerendert. Es soll ein durchsichtiger Cube sein an dem die Textur angezeigt wird.
- Erstelle eine GlassRenderer.ts Datei
- Die Textur soll Transparent sein.
  [x] backFaceCulling = false soll immer gestetz sein wenn an einem material transparent aktiv ist. ggf braucht es material in mehreren auspraegungen um die modifier am Block gerecht zu werden. die materialien sollten bei anforderung generiert und
  gechached werden. Es kann hier ggf noch weitere eigenschaften geben, die es erfordern materialien in weiteren auspraegungen doppelt vorzuhalten, z.b. leaves mit WindShader und ohne WindShader.  Gib es dafuer einen service der das uebernummt, z.b.
  einen MaterialManager oder MaterialService?
- die getMaterial() methode braucht keine texture als uebergabe. der MaterialManager hat zugiff, bzw. verwaltet alle Material Definitionen (BlockType) und Texturen und Shader. und an der Block instanz haengen alle informationen wie BlockType
  BlockModifier um das material zu bestimmen. du kannst noch ein 'private createMaterialKey(block) : string' hinzufuegen.
  [x] Wenn am Block 'transparent' aktiviert ist, sollen die Texturen durchscheinen wo sie als transparent markiert sind. Das funktioniert nicht. Bitte alle Shade Renderer prüfen.
  [x] Wenn ich die '\' taste druecke werden debug informationen in der console ausgegeben, gib hier auch informationen ueber den selektierten block aus. So dass
  ich die informationen an dich weitergeben kann und du die konfiguration der blocks und des materials analysieren kannst.
  [x] Das sollte im ChunkRenderer genauso wie schon im MaterialManager generalisiert werden. wenn du die bloecke durch gehst,
  kannst du im MaterialManager fragen wie der material-Key ist. Fuehre eine Map(key,Container) in der du diese materialien und
  eben das mega-mesh fuer das material und ggf andere werte (indices, positions, uvs, colors, ...) haelst, gibt es den key
  noch nicht, dann anlegen, wenn es den key schon gibt, kannst du den block hinzufuegen. So kannst du generalisiert damit
  umgehen und nicht hart codiert fuer jedes material. Ausserdem ist die auswahl, welche eigenschaften zu welchem material
  gehoeren zentral im MaterialManager. Wuede das so klappen oder gibt es edge cases?
  [x] Jetzt kannst du das hinzufuegen der bloecke auch an die Shape-Renderer deligieren, was machst du, wenn ein Block aus mehreren materialien besteht. -  Erweiterung des BlockRenderContext
  [x] Erstelle ein commando, das den aktuellen chunk neu rendert und ein command das alle visible chunks neu rendert.
```text
 Created two command classes following the existing pattern:
  - RechunkCommand - /rechunk command for current chunk
  - RechunkAllCommand - /rechunkall command for all visible chunks
```

[x] Neuer Shade: Steps - Macht einen zwischen step/treppe im cube, eine stufe
Es soll einen neuern shader 'steps' geben, der einen cube in eine treppenform bringt.
Es wird eine zwischen-step eingebaut, so dass ich mehrere blocke aneinander reihen kann um eine treppe zu bauen.
- (?) edgeOffset wird genutzt um die ecken zu verschieben (wie bei cube)
- rotationX, rotationY dreht das treppen teil (wie bei cube)

[x] Neuer Shade: Stair - Macht einen zwischen treppe im cube, eine stufe
Es soll einen neuern shader 'stair' geben, der einen cube in eine treppenform bringt. Im gegensatz zu steps ist die Treppe unten ausgefüllt.
Es wird eine zwischen-step eingebaut, so dass ich mehrere blocke aneinander reihen kann um eine treppe zu bauen.
- rotationX, rotationY dreht das treppen teil (wie bei cube)
- Es kann grundsaetzlich StepRenderer uebernommen werden
- Im Editor den neuen shader in das dropdown übernehmen

[x] Neuer Shade: Billboard (assetPath)
Es soll einen neuen Shade 'billboard' geben, der eine Textur auf dem Block anzeigt.
- Parameter scaling manipuliert die größe des Billboards, kann auch größer als 1 sein.
- edgeOffset das erste XYZ im Array manipuliert/korrigiert den Mittelpunkt des Billboards (ahnlich wie bei shade Column)
- Im Editor den neuen shader in das dropdown übernehmen
- Der Parameter Facing dreht die Billboard Texture.

> Neuer Shade: Sprite, ist aus Billboard entstanden als eine von zwei Optionen der Umsetzung.
```text
Option 1: Mesh.billboardMode (Einfachste Lösung)

  Babylon.js Meshes haben eine billboardMode Eigenschaft:

  mesh.billboardMode = Mesh.BILLBOARDMODE_ALL; // Dreht sich komplett zur Kamera

  Problem: Das funktioniert nur für einzelne, separate Meshes - nicht für Vertices in einem kombinierten Chunk-Mesh.

  Option 2: Separate Billboard-Meshes

  Wir könnten Billboard-Blöcke außerhalb des Chunk-Mesh-Systems rendern:
  - Jedes Billboard = eigenes kleines Mesh
  - Nutzt mesh.billboardMode = Mesh.BILLBOARDMODE_ALL
  - Babylon.js kümmert sich automatisch um die Rotation

  Vorteil: Nutzt Babylon.js Built-in FeaturesNachteil: Weniger effizient bei vielen Billboards (mehr Draw Calls)

  Option 3: SpriteManager (Für viele kleine Billboards)

  Für viele kleine Billboards (wie Gras, Blumen):
  const spriteManager = new SpriteManager("billboards", "texture.png", 1000, 64, scene);

  Welchen Ansatz bevorzugst du?

  Ich würde Option 2 empfehlen: Billboard-Blöcke als separate Meshes mit billboardMode. Das ist einfach zu implementieren und nutzt Babylon.js' eingebaute Funktionalität. Soll ich das so umsetzen?
```
```text
 Das SpriteManager System funktioniert anders:
  - Es verwaltet viele Sprites mit einer einzigen Textur
  - Sprites sind immer camera-facing (billboarded)
  - Sehr performant für tausende von Sprites (Partikel, Gras, etc.)

  Ich muss dies jedoch anders angehen, da SpriteManager pro Textur erstellt wird und nicht pro Block. Wir bräuchten einen zentralen SpriteManager pro BlockType-Textur im ChunkRenderer oder einem höheren Level.

  Soll ich:
  1. Einen SpriteManager pro Chunk erstellen (ein Manager pro Textur im Chunk)?
  2. Einen globalen SpriteManager pro Textur in der Scene/WorldRenderer verwalten?

  Außerdem: Sollte SPRITE weiterhin X-förmig sein (2 diagonale Quads wie jetzt), oder einfach ein einzelnes camera-facing Quad wie Babylon.js Sprites normalerweise sind?
```
[x] Sprites mit SpriteManager umsetzen
```text

  1. Sprite-Anzahl: Welche Option bevorzugst du? (A: feste Zahl, B: Dichte, C: Range)
  2. Positionierung: Random, Grid, oder edgeOffsets nutzen?
  3. Standard-Wert: Wie viele Sprites sollen Standard sein, wenn nicht angegeben? (z.B. 1, 3, 5?)
  4. Sprite-Größe: Soll das über modifier.scale gesteuert werden, oder separate Parameter?
```
- Sprite-Anzahl: Feste Anzahl 'spriteCount' pro Block
- Positionierung: default Random, wenn edgeOffset nicht gesetzt, sonst edgeOffset
- Standard-Wert: 5 (kann spaeter noch angepasst werden)
- Sprite-Größe: ja ueber modifier.scale gesteuert
  [x] Sprites sollen sich auch leicht bewegen, koennen wir da wind parameter aus der App benutzen die wir schon haben?
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway
  Eine einfache implementierung wuede ich bevorzugen.


> [x] Es soll immer der Parameter 'texture' im BlockModifier geprueft werden und ggf. eine andere Texturen-Datei geladen werden.
- Verschiedene Texturen werden durch Komma getrennt, dann wird ein Array daraus.
- Wichtig bei shade Type BILLBOARD und SPRITE
- Texture cache, damit eine textur nur einmal in die 3D engine geladen werden muss? Schon vorhanden? TextureManager im MaterialManager?
- Muss das dnn jedesmal ein neues material werden? Anpassung von createMaterialKey() in MaterialManager?
- Wenn ich jetzt noch Atlas Funktionalität hinzfügen möchte, also eine Atlas-Textur und daraus nur einen Ausschnitt als Textur nutzen möchte. Am besten am texturnamen dahinter mit ':' und innen einen index, like :x,y,b,h ist das schlau?
- Dann sollten wir vorher noch das Thema custom Texturen umsetzten und danach Spritemanager bauen. Ich habe zwei sachen zu Texturen: 1. Texturen im BlockModifier ueberschreiben fuer einzelne Bloecke, mit komma separiert fr ein Array, hier muss im
  materialKey dann der texturePath mit eingebaut werden. 2. An Texturen noch einen Atlas Index anhaengen mit ':', z.b. texturPath=assets/foods:5,3 oder so aehnlich. Hat den vorteil das wir fuer atlas daten keine neuen Parameter bauen muessen, das
  auch im BlockModifier.texturePath funktioniert und das dann auch im materialKey gleich drinn ist. Frage ist wie wir hier am besten den Atlas Index hinterlegen, z.b. path:x,y,width,height
- Der BlockModifier hat schon das feld, es heisst 'texture' das Feld ist immer ein String und soll dann beim verarbeiten zu einem Array werden. Wir haben hier ein syntaktisches problem. Wir sollten ';' als trenner nehmen dann crasht das nicht mit
  der atlas konfiguration, also: path:x,y,w,h;path....
```text
 Was NICHT optimiert ist (GPU-Atlas):

  ❌ Atlas-Slots: Jede Sub-Texture bekommt einen eigenen Slot
  - Verschiedene Bereiche derselben Datei werden mehrfach in den Atlas gezeichnet

  Ist das ein Problem?
  - Für die Performance: Nein, weil jede Sub-Texture unterschiedlichen Inhalt hat
  - Für den Speicher: Ja, wenn man viele Sub-Textures aus derselben Quelle nutzt
```

[x] Neuer Shade: Flame
Es soll einen neuen Shade 'flame' geben, der eine animierte flamme anzeigt.
- edgeOffset manipuliert/korrigiert den Mittelpunkt der flamme (ahnlich wie bei shade Column)
- scaling manipuliert die größe der flamme
- color verändert die Farbe der flamme
- Flame wird nicht NIE vom WindShader genutzt.
- Der Parameter windStability am Block kann deshalb für die Flamme genuzt werden und die stärke der Flamme zu bestimmen.
- Der Parameter windLeafinnes gibt an wie stark die Flamme flackert
- Es muss einen FlameShader geben, der die Flamme flackern lässt. Ihm werden auch die wind-Einstellungen aus der App mitgegeben, da sich die Flamme auch im Wind bewegen muss.
- Flammen haben immer einen WindShader, der sie bewegt.
- windLeverUp und windLeverDown koennen wie im WindShader verwendet werden.
  Nutze Babylon mittel:
```typescript
  var fireMaterial = new BABYLON.FireMaterial("fireMaterial", scene);
  fireMaterial.diffuseTexture = new BABYLON.Texture("diffuse.png", scene);
  fireMaterial.distortionTexture = new BABYLON.Texture("distortion.png", scene);
  fireMaterial.opacityTexture = new BABYLON.Texture("opacity.png", scene);

    var plane = BABYLON.Mesh.CreatePlane("fireplane", 1.0, scene);
    plane.material = fireMaterial;
```
Die Texturen koennen ueber die texture parameter im BlockType und BlockModifier gesetzt werden. Wenn diese Fehlen, werden default Texturen verwendet.
```text
FLAME Shape Implementation Complete

  I've successfully completed the FLAME shape implementation for your voxel engine. Here's what was done:

  Files Modified/Created:

  1. BlockType.ts (/packages/core/src/registry/BlockType.ts)
    - Added FLAME = 13 to the BlockShape enum
  2. FlameRenderer.ts (NEW FILE: /packages/client/src/rendering/shapes/FlameRenderer.ts)
    - Complete flame renderer implementation with FireMaterial support
    - Uses vertical plane meshes for flame geometry
    - Supports both textured (FireMaterial) and non-textured (StandardMaterial) rendering
  3. shapes/index.ts (/packages/client/src/rendering/shapes/index.ts)
    - Added FlameRenderer export
  4. ChunkRenderer.ts (/packages/client/src/rendering/ChunkRenderer.ts:79)
    - Registered FlameRenderer for FLAME shape blocks
  5. BlockEditor.ts (/packages/client/src/gui/BlockEditor.ts)
    - Added 'FLAME' to all three shape selection lists (lines 1379, 1807, 2009)

  Key Features Implemented:

  Material System:
  - With Texture: Uses Babylon.js FireMaterial with realistic animated flames
    - Supports diffuse, distortion, and opacity textures
    - Automatic flame animation based on windStability parameter
  - Without Texture: Falls back to StandardMaterial with emissive glow

  Block Properties Support:
  - edgeOffset: Adjusts flame center position (X, Y, Z from corner 0)
  - scale: Controls flame size (width, height, depth)
  - color: Tints flame color (default: orange [255, 200, 100])
  - windStability: Controls flame strength and animation speed
  - windLeafiness: Controls flicker intensity
  - windLeverUp/windLeverDown: Stored for future advanced animation

  Rendering:
  - Creates separate mesh (not part of chunk geometry)
  - Double-sided rendering (visible from all angles)
  - Semi-transparent (alpha 0.8 for StandardMaterial)
  - Non-pickable for performance optimization
```

[ ] Texture umbenennen in assetPath
[ ] Der shader 'model' soll ein babylon oder glb model laden und anzeigen (assetPath)
- ohne shema angabe in assetPath wird das model aus der asset ablage (wie textures) geladen.
- mit shema angabe http:// https:// wird das asset aus dem internet geladen
- Models werden wie alle anderen shades durch rotation, color beeinflusst
- edgeOffset verschiebt den model punkt auf dem cube
- Das Model wird bei default auf die groesse des cube gescaled (1/1/1).
- Mit 'scale' parametern in blockModifier kann das Model gescaled werden, auch goesser als 1, dabei bleibt es auf der Fläche XZ stehen
- Der Mittelpunkt des Models wird so gewählt, das es auf der XZ Fläche des Cubes stehen bleibt. (mit edgeOffset justierbar, scale vergroessert/kleinert das model)

[ ] (!) Bei shape Cross dreht sich ein teil des Cross um 180 Grad (oder spiegelt sich?)
- Es liegt am Facing, wenn ich mich um den cube herum bewege, springt die eine fläche um 180 Grad.
- Facing funktioniert nicht richtig auf die eine Fläche, bzw. Rotation
> Ist immernoch nicht ok, verschoben auf spaeter

[ ] Race condition mit textures und PNG transparenz

## Wind

Erster Schritt Block Parameter:

[x] Es soll Wind geben. Folgende Parameter sollen am Block-Type erweitert werden:
- windLeafiness (float) [0..1] → 1 bei Blatt-Typen, sonst 0.
- windStability (float) [0..1] → z. B. Blatt 0.1, Holz 0.6, Stein 0.95.

[x] An der BlockInstanz wird der Folgende parameter an BlockModifier benötigt:
- windLeverUp (float) [0..n] → wie weit oben im Stack der Block sitzt oben am Cube (Hebel).
- windLeverDown (float) [0..n] → wie weit oben im Stack der Block sitzt unten am Cube (Hebel).
- Im Block-Editor editierbar machen
- Netzwerk und Speicher implementieren.

---
Zweiter Schritt Wind Parameter in der App:

[x] Es sollen die Parameter in der App mit Kommandos änderbar sein:
- windDirection (x,z) - direction of wind
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway
  Setze sinnvolle default werte.
---
Dritter Schritt Implementierung am Material:

[x] Im Renderer soll ein Shader die Windbewegung umsetzen.

Aus folgenden Parametern:
- windLeafiness (float) [0..1] → 1 bei Blatt-Typen, sonst 0.
- windStability (float) [0..1] → z. B. Blatt 0.1, Holz 0.6, Stein 0.95.
- windLeverUp (float) [0..n] → wie weit oben im Stack der Block sitzt oben am Cube (Hebel).
- windLeverDown (float) [0..n] → wie weit oben im Stack der Block sitzt unten am Cube (Hebel).
- windPhase - Ergibt sich aus den Coordinaten
  und
- windDirection (x,z) - direction of wind
- windStrength (float) - strength of wind
- windGustStrength (float) - strength of wind gusts
- windSwayFactor (float) - factor for wind sway

Nur aktiv, wenn am BlockType windLeafiness > 0 oder windStability > 0 ist.

Soll in diesem Schritt eine einfache Windbewegung (windLever + windStrength) um zu testen ob der Wind funktioniert.
---
[x] Am BlockType 'leaves' soll ein windLeafiness=1 configuriert werden.
---
Jetzt soll der Wind komplett umgesetzt werden. Der Shader soll alle Parameter benutzen um eine realistische Windbewegung umzusetzen.
---
[x] windLever an der BlockInstanz soll nun in windLeverUp umbenannt werden, ausserdem soll es ein
windLeverDown (float) [0..n] am BlockInstanz geben. Bitte an der Instanz, Networking, Chunk Persistenz, Block-Editor und WindShader umsetzen.
[x] windLeverUp ist ein Wert, der oben am Cube gilt und windLeverDown unten am Cube. Der Cube muss sich nun verbiegen (im Wind) zwischen unten und oben.
- Wenn zwei Bloecke Ubernander sind und der untere hat blockLeverUp den gleichen Wert wie der obere windLeverDown, dann sollen sie sich an der
  Schnittstelle gleich verbiegen und somit nicht auseinander gehen.
- Die verbiegung ist eine Biegung/Krümmung um den Hebel sein. regulär hat der untere Block windLeverDonw=0 und windLeverUp=1 (1 Block hoch)
  definiert, der nächste Block hat windLeverDown=1 und windLeverUp=2 (1 Block runter) definiert. Wenn das material weicher ist, kann die differenz
  zwischen Up und Down größer sein. Alle zusammen sollen sich wie eine Einheit verhalten.

[x] Die commandos fuer Wind geben einen Fehler aus, wenn ich einen Wert uebergebe, scheinen dann aber trozdem zu funktionieren.
- Es soll ein fehler ausggeben werden, wenn der wert 'out of bounds ist'.

> Die Wirkung von parametern nochmal pruefen und ggf. anpassen lassen.
> Echte Wetter scenarien durch automatisch wechselnde Wetterparameter pruefen. Die können auch fuer chunks unterschiedlich sein um boehen / täler / berge bessser zu simulieren.
> Man koennte am chunk eine Information angeben, ob er eher auf dem Berg oder im Tal ist.
