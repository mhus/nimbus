
# Shapes Rendering

## Cube

> Habe ich manuell mit hilfe con claude angepasst. Struktur ist nun sauber und
> kann erweiytert werden auf weitere renderer.

[x] Erstelle unit tests mit BabylonJS und NullEngine fuer CubeRenderer.
- Nutze nur den CubeRenderer, render einen Cube indem du ihn mit speziell gefertigten konfigurationen erstellst.
- Natuerlich wird auch der MaterialService und TextureService benoetigt.
- Teste ob der Cube korrekt gerendert wird.
- Teste ob faceVisibility korrekt funktionieren.
[x] Erstelle noch einen test fuer 'offsets'
[ ] Kannst du einen test fuer backFaceCulling erstellen (nicht sehen auf der back side) ?

## Cross

[?] Erstelle einen CrossRenderer der ein Cross Rendert, auf dem die Texturen sind.
- Ein Cross sind zwei Flaechen, die diagonal, stehend auf einem Block angebracht sind.
- Implementiere alle eigenschften, offset, scaling, rotation wie bei einem Cube.
- Orientiere dich an CubeRenderer.

[ ] faceVisibility soll auch die Seiten im CrossRenderer deaktivieren koennen. Die beiden Seiten sollen durch LEFT und RIGHT deaktiviert werden koennen.

## Sphere

[?] Erstelle den SphereRenderer. Er rendert einen Sphere.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Offsets:
  - 0: Radius Offset
  - 1: Displacement

## Cylinder

[?] Nenne shape type 'Column' um in 'Cylinder' in BlockModifier und in den Editor controls
[?] Erstelle den CylinderRenderer. Er rendert einen Cylinder.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Offsets:
  - 0: Radius Top
  - 1: Radius Bottom
  - 2: Displacement Top (incl Y)
  - 3: Displacement Bottom (incl Y)

[?] CylinderRenderer soll oben und unten einen Deckel auf den cylinder rendern, TOP und BOTTOM sollen 
durch faceVisibility TOP und BOTTOM deaktiviert werden koennen. Die Seite durch FRONT.

## Hash 

[?] Erstelle den HashRenderer. Er rendert einen Hash (#) shape. Das bedeutet die Seiten sind von
oben betrachtet wie ein hash angeordnet und verschieben sich mit dem offset einstellungen
Das bedeutet du brauchst pro seite 4 punkte, also 24 Punkte. da sich drei seiten nicht mehr einen punkt 
teilen.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Die offset verschieben nur noch aspekte von jedem punkt. andere aspekte bleiben unveraendert.
- Wenn alle offsets 0 sind, dannw wird wieder ein Cube gerendert, da immer drei punkte uebereinander liegen.

[x] faceVisibility soll auch die Seiten im HashRenderer seiten deaktivieren koennen.

## Stair

[?] Erstelle den StairRenderer. Er Rendert eine Treppe.
- Orientiere dich an CubeRenderer.
- Es werden vorerst keine offsets unterstuetzt.
- Implementiere alle weiteren eigenschaften, scaling, rotation wie bei einem Cube.
- Du kannst das als vorlage nutzen: client_playground/packages/client/src/rendering/shapes/StairRenderer.ts

[?] In StairRenderer soll die BOTTOM Seite soll durch faceVisibility deaktiviert werden koennen. Die BACK soll durch 
faceVisibility deaktiviert werden koennen.

## Steps

[?] Erstelle den StepsRenderer. Er rendert einzelne Steine als Treppe
- Orientiere dich an CubeRenderer.
- Implementiere alle weiteren eigenschaften, scaling, rotation wie bei einem Cube.
- Du kannst das als vorlage nutzen: client_playground/packages/client/src/rendering/shapes/StepsRenderer.ts

## Glass

[?] Erstelle den GlassRenderer. Er rendert einen Cube, wie CubeRenderer, richscheinende bereiche
sind aber glassiere mit der color die an der texture hinterlegt ist.
- Der Glass effekt soll kein shader sein. Benutze ein spezielles Material, das den Glass effekt
  mit den Standard BabylonJS materialien umsetzt. Das Material soll auch im MaterialService verwaltet 
  werden, ggf. brauchst du zur erstellung eine neue Methode.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.

[?] In GlassRenderer sollen mit faceVisibility die Seiten deaktiviert werden koennen.

## FlipBox

[?] Erstelle den FlipBoxRenderer. Er rendert eine Oberflaeche (nur eine TOP Seite) die den Sahder FlipBox aus
dem ShaderService verwendet.
- Orientiere dich an CubeRenderer, es werden nur 4 Punkte benoetigt.
- Implementiere alle eigenschaften, offset, scaling, rotation wie bei einem Cube.
- Der FlipShader bekommt parameter aus VisibilityModifier.effectParameters, hier werden 'anzahl,milliseconds,mode' angegeben, die vom shader verarbeitet werden.
  - anzahl (frameCount): anzahl der bilder
  - milliseconds: wartezeit zwischen den flips - optional, default 100
  - mode: bumerang:bilder gehen vor und zurueck (bumerang, reihenfolge: 0,1,2,3,2,1,0,1,2...), rotate: bilder rotieren durch (reihenfolge: 0,1,2,3,0,1,2,3...) - optional, default: rotate
- Bildausschnitt kommt aus UVMapping: w und h wird nach rechts weiter geflippt
  - z.b. |0|1|2|3| soll UVMapping den bereich fuer 0 angeben, also u=0,v=0,w=0.25,h=1 fuer 4 bilder nebeneinander.
- Der Renderer definiert seinen eigenen Mesh, da sonst der Atlas die restlchen Bilder nicht uebernimmt.

## Billboard

[?] Erstelle einen BillboardRenderer. Er rendert ein Billboard aus der ersten Textur in TextureDefinition.
- Das Billboard steht immer auf dem Block und ist by default 1 breit und ratio zur breite hoch.
- Beachte, das das Billboard erstmal auf dem Block stehen soll, also der Mittelpunkt ueber 0 sein muss, sonst ist es im Block darunter versunken.
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschaften, offset (offset 0 verschiebt den lagepunkt), scaling, rotation wie bei einem Cube.
- Der Renderer definiert sein eigenes Mesh
- Vorlage in: client_playground/packages/client/src/rendering/BillboardRenderer.ts

## Sprite

[?] Erstelle einen SpriteRenderer. Er nutzt den SpriteService (service in dieser App) um ein Sprite zu rendern.

Erstelle den SpriteService und haenge ihn in den EngineService ein. Er soll den SpriteManager von BabylonJS nutzen
und ueber alle chunks hinweg verwalten. Der manager soll pro definierter textur einen SpriteManager erstellen.
Vorlage aber nicht vollstaendig: client_playground/packages/client/src/rendering/SpriteManagerRegistry.ts

SpriteRenderer:
- Pro block kann es mehrere Sprites geben. Es wird pro definierter Texture in TextureDefinition ein
  SpriteManager auf diesem Block erstellt. Wieviele sprites es gibt wird in shaderParameters an TextureDefinition definiert.
  default 100
- Nutze scalierung und offset wie in der vorlage um die sprites zu positionieren.
- Der Renderer definiert sein eigenes Mesh.
- Sprites bewegen sich im Wind - siehe die vorlage client_playground/packages/client/src/rendering/SpriteManagerRegistry.ts

## Flame

[?] Erstelle einen FlameRenderer. Er rendert einen Flame aus der Textur in TextureDefinition.
- Das Flame steht immer auf dem Block und ist by default 1 breit und ratio zur breite hoch.
- Beachte, das das Flame erstmal auf dem Block stehen soll, also der Mittelpunkt ueber 0 sein muss, sonst ist es im Block darunter versunken.
- Vorlage: client_playground/packages/client/src/rendering/shapes/FlameRenderer.ts
- Implementiere alle eigenschaften, offset (offset 0 verschiebt den lagepunkt), scaling, rotation wie bei einem Cube.
- Der Renderer definiert sein eigenes Mesh

## Ocean

[?] Erstelle einen OceanRenderer. Er soll die von BabylonJS bereitgestellten Wasser effekt rendern.
- Es soll eine flache ocean oberflaeche on TOP des Blocks gerendert werden.
- Der Renderer definiert sein eigenes Mesh

## Thin Instances

```text
  - shape: 25 = THIN_INSTANCES
  - shaderParameters: "150" = 150 Grashalme pro Block
  - Instanzen werden zufällig im Block verteilt

  🚀 Nächste Schritte:

  1. ✅ System funktioniert - Testen Sie es!
  2. ⏳ Y-Axis Billboard Shader - Später über NodeMaterial implementieren
  3. ⏳ GPU Wind - Im Shader integrieren
```

## Model

[ ] Erstelle einen ModelRenderer der ein Model rendert.
- Orientiere dich an client_playground/packages/client/src/rendering/shapes/ModelRenderer.ts
- Implementiere alle eigenschaften, offset, scaling, rotation
- Der Renderer definiert sein eigenes Mesh
- Als modell pfad wird der erste Pfad aus TextureDefinition verwendet.

## Offsets reparieren:

[-] Die offsets werden im Blockenderer falsch angewendet.

top front left (SW) - in blockrenderer:
top front right (SE) - in blockrenderer:
top back left (NW) - in blockrenderer:
top back right (NE) - in blockrenderer:

Das gleiche natuerlich auch bei bottom.

Auch in anderen Renderern muessen die offsets repariert werden.

