
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
- Wenn alle offsets 0 sind, dann wird wieder ein Cube gerendert, da immer drei punkte uebereinander liegen.

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

[x] Erweitere den FlipboxRenderer so, das er auch vertical flippen kann. Dazu wird effectParameters erweitert um den ersten parameter 'h' oder 'v'.
  - direction: 'h' oder 'v' (NEU!)
  - anzahl (frameCount): anzahl der bilder
  - milliseconds: wartezeit zwischen den flips - optional, default 100
  - mode: bumerang:bilder gehen vor und zurueck (bumerang, reihenfolge: 0,1,2,3,2,1,0,1,2...), rotate: bilder rotieren durch (reihenfolge: 0,1,2,3,0,1,2,3...) - optional, default: rotate
- Beispiel: 'v,4,200,bumerang' - vertical flip, 4 bilder, 200ms wartezeit, bumerang modus

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

[x] Erstelle einen ModelRenderer der ein Model rendert.
- Orientiere dich an client_playground/packages/client/src/rendering/shapes/ModelRenderer.ts
- Implementiere alle eigenschaften, offset, scaling, rotation
- Der Renderer definiert sein eigenes Mesh
- Als modell pfad wird der erste Pfad aus TextureDefinition verwendet.

## Offsets reparieren:

[x] Die offsets werden im Blockenderer falsch angewendet.

top front left (SW) - in blockrenderer:
top front right (SE) - in blockrenderer:
top back left (NW) - in blockrenderer:
top back right (NE) - in blockrenderer:

Das gleiche natuerlich auch bei bottom.

Auch in anderen Renderern muessen die offsets repariert werden.

## Wall Renderer

[?] Erstelle einen WallRenderer der Blocks mit shape WALL (muss hinzugefuegt werden) rendert.
Walls werden an der Raendern des Blocks gerendert, 
- d.h. es werden die Seiten von aussen gerendert, die von aussen sichtbar sind, das ist exakt das geliche wie bei CubeRenderer.
- zusaetzlich werden nochmal Flaechen etwas weiter innen im Block gerendert, die die Wand darstellen.
  - Die innere Wand hat eine dicke von 0.1 (constante in WallRenderer) Einheiten.
  - Die Flaechen sind anders herim Winded als die aussenwaende, damit sie von der Innenseite sichtbar sind.
  - Es gibt spezielle Texuren IDs fuer INSIDE_ALL, INSIDE_*, wenn INSIDE_ALL nicht definiert wurde wird als fallback noch auf ALL zurueckgegriffen. Ansonsten die gleiche logik wie bei den Aussenseiten.
  - Fuer die Steuerung ob die faces der inneren Wand sichtbar sind, gilt nur die faceVisibility in der modifierDefinition, nicht die am Block (das ist ein Unterschied zu allen anderen renderern).
  - Es ist vermutlich sinnvoll die innenwaende immer bis zur ausenwand zu ziehen, damit es keine Luecken wenn die 90 Grad wand nicht gezeichnet werden soll.
  - Die luecke zwischen aussenwand und innenwand muss gefuellt werden, siehe beispiel
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschften, offset, scaling, rotation wie bei einem Cube.

z.b.

```text
+------------------+
|  |  OUTSIDE   |  |
|--+------------+--|
|  |  INSIDE    |  |
|  |            |  |
|  |            |  |
|--+------------+--|
|  |            |  |
+------------------+
```

Wenn jetzt eine wand fehlt, dann  muss eine gleine luecke zwischen der aussenwand und der innenwand gefuellt werden.

```text
+-------------------
|  |  OUTSIDE        <--- Hier fehlt die wand
|--+----------------
|  |  INSIDE    
|  |            
|  |            
|--+----------------
|  |                 <--- Hier fehlt die wand
+-------------------
```

Die fehlenden wände sollen duch Innenwand gefuellt werden (wichtig winding wieder von aussen sichtbar machen).

```text
+------------------+
|  |  OUTSIDE      | <--- Hier jetzt innenwand Einsatz
|--+---------------+
|  |  INSIDE    
|  |            
|  |            
|--+---------------+
|  |               | <--- Hier jetzt innenwand Einsatz
+------------------+
```

Welche Einsaetze gemacht werden muessen hängt von der kombinantion der fehlenden aussenwände ab.
- Alle Ecken sind schon vorhanden, muessen also nicht neu definiert werden
- Die Klaechen koennen sich ggf ueberlappen, das ist ok.


```text
  Verwendung:
  {
    "visibility": {
      "shape": 26,  // WALL
      "faceVisibility": {
        "value": 63  // Steuert Innenwände
      },
      "textures": {
        "0": "textures/wall_outside.png",  // ALL (Außen)
        "11": "textures/wall_inside.png"   // INSIDE_ALL (Innen + Lücken)
      }
    }
  }
```

## Fog Renderer

[?] Erstelle einen FogRenderer der Blocks mit shape FOG rendert.
- Der FogRenderer rendert einen Nebelblock der den ganzen Block ausfuellt.
- Der Nebel soll ein spezielles Material nutzen, das den Nebel Effekt umsetzt (vermutlich)
- Orientiere dich an CubeRenderer.
- Implementiere alle eigenschften, offset, scaling, rotation wie bei einem Cube.

```text
  Verwendung:
  {
    "visibility": {
      "shape": 24,  // FOG
      "effectParameters": "0.5",  // Density: 0.5 (50% opak)
      "textures": {
        "0": "textures/fog.png"  // Optional: Textur für Nebel
      }
    }
  }
```


[x] ueber items soll immer ein keiner text stehen, der name und die anzahl (fallss angegeben) als billboard text ueber dem item. (ItemRenderer)
[x] Jetzt das gleche fuer Entities die haben in EntityData.ts schon einen namen, der angezeigt werden soll: ueber entities soll immer ein kleiner text stehen, der name der entity und deren health (falls angegeben, als balken) als
billboard text ueber der entity. Wie kann hier der abglich der health geschehen? Es soll auch der health wert angezeigt und im laufenden darstellung auch aktualisiert werden. Wie macht man das denn?

[x] An Block ist nun ein neuer parameter 'level'. Wenn der parameter gesetzt ist, soll beim erstellen des Winds
aus WindModifier.leverUd und WindModifier.leverDown mit dem Wert block.level multipliziert werden.

[x] Der parameter 'level' soll im block instance editor unter packages/controls am block editierbar werden.
Wenn er nicht gesetzt ist, oder kleiner 0, wird er als undefined gespeichert.
- level werte sind immer integer

## Bush Shape and Renderer

[?] Erstelle einen BushRenderer der Blocks mit shape BUSH rendert.
- Ein Busch soll einen Busch, oder ein kleine gewächs darstellen, dafue werden mehrere flache Flächen genutzt die ineinander scheinbar 
  unstrukturiert verschraenkt sind.
- Die grosse und anzahl der Flächen soll durch die Offset Eigenschaften am Block gesteuert werden.
  So kann der Busch 'wachsen' durch die aenderung der offsets.
- Orientiere dich grob an CubeRenderer. - nicht Offsets
- rotation und Wind sollen auch funktionieren - Wie bei CubeRenderer.
```text
  Implementierte Features

  BushRenderer (packages/engine/src/rendering/BushRenderer.ts):
  - Rendert Büsche mit mehreren verschränkten Flächen (4-8 Ebenen)
  - Offset-Steuerung:
    - offset[0]: Anzahl der Flächen (4-8, Standard: 6)
    - offset[1]: Skalierungsfaktor (0.3-1.5, Standard: 1.0)
    - offset[2]: Varianz/Unordnung (0.0-0.5, Standard: 0.3)
  - Rotation: Unterstützt rotation.x und rotation.y wie beim CubeRenderer
  - Wind: Nutzt addWindAttributesAndColors() für Wind-Animation
  - Scaling: Unterstützt scalingX, scalingY, scalingZ aus dem Modifier
  - Die Flächen werden gleichmäßig in verschiedenen Winkeln verteilt und mit deterministischer Pseudo-Zufälligkeit variiert

  Shape Enum (packages/shared/src/types/Shape.ts):
  - Shape.BUSH = 29 hinzugefügt
  - ShapeNames aktualisiert mit 'bush'

  RenderService (packages/engine/src/services/RenderService.ts):
  - BushRenderer importiert und initialisiert
  - Im getRenderer() switch-case für Shape.BUSH registriert

  Verwendungsbeispiel

  {
    "visibility": {
      "shape": 29,  // BUSH
      "textures": {
        "0": "textures/bush_leaves.png"
      },
      "scalingX": 1.0,
      "scalingY": 1.0,
      "scalingZ": 1.0
    },
    "wind": {
      "leafiness": 0.8,
      "stability": 0.3,
      "leverUp": 0.5,
      "leverDown": 0.3
    }
  }

  Offsets am Block:
  block.offsets = [
    6,    // Anzahl der Flächen (4-8)
    1.0,  // Skalierung (0.3-1.5)
    0.3   // Varianz (0.0-0.5)
  ];

  Der Busch wird "wachsen", wenn du die Offsets über Zeit änderst - perfekt für animierte Wachstumseffekte!
```

## Illumination

Im BlockModifier gibt es eine Sektion Illumination (IlluminationModifier). Diese kann auch noch angepasst werden.

Blöcke sollen entweder licht emmitieren oder bei dunkelheit nicht dunkel werden.
Welche möglichkeiten habe ich mit BabylonJs?

```text
⏺ ✅ Illuminations-System erfolgreich implementiert!

  Die komplette Implementierung ist abgeschlossen und der Build war erfolgreich. Hier eine Zusammenfassung der Änderungen:

  📦 Phase 1: MaterialService Updates (Foundation)

  ✅ Material Key Generation erweitert um Illumination-Properties
  ✅ Material Key Parsing erweitert
  ✅ Emissive Properties (emissiveColor, emissiveTexture) und disableLighting für alle Materialien aktiviert

  🌟 Phase 2: IlluminationService (GlowLayer Management)

  ✅ Neuer IlluminationService erstellt (/packages/engine/src/services/IlluminationService.ts)
  ✅ GlowLayer mit optimierten Settings (0.5x Resolution für 75% bessere Performance)
  ✅ Service in EngineService initialisiert und in AppContext registriert

  🎯 Phase 3: Mesh-Registrierung (Complete Integration)

  ✅ Chunk-Meshes werden beim GlowLayer registriert
  ✅ Cleanup-Logic beim Chunk-Unload implementiert
  ✅ Separate-Mesh Renderer unterstützt:
  - BillboardRenderer
  - FlipboxRenderer
  - ModelRenderer
  - FogRenderer
  - FlameRenderer
  - GlassRenderer

  📝 Modifizierte Dateien:

  1. /packages/engine/src/services/MaterialService.ts - Material-Pipeline
  2. /packages/engine/src/services/IlluminationService.ts - NEU - GlowLayer Management
  3. /packages/engine/src/services/EngineService.ts - Service-Initialisierung
  4. /packages/engine/src/services/RenderService.ts - Mesh-Registrierung & Cleanup
  5. /packages/engine/src/AppContext.ts - Type-Definitionen
  6. 6x Renderer-Dateien - Separate-Mesh Support

  🎨 Visuelles Verhalten:

  - Ohne Illumination: Blöcke bleiben bei Dunkelheit sichtbar (disableLighting)
  - Mit Illumination: Blöcke leuchten mit emissiveColor + GlowLayer-Halo

  ⚡ Performance:

  - GlowLayer: ~2-5ms pro Frame
  - Skalierung: 1000 leuchtende Blöcke = ~5-8ms (akzeptabel)
  - 75% weniger Pixel-Processing durch 0.5x Resolution

  Die Implementierung ist produktionsbereit und kann jetzt getestet werden! 🚀
```

[?] An der Camera soll es eine Beleuchtung geben 'CamLight, die man an schalten kann. Wie z.b. eine Fackel.
- Es soll ein Lichtquelle an der Kamera sein, die immer in Blickrichtung leuchtet.
- Es soll ein einstellbarer Radius sein, der den beleuchteten bereich steuert.
- Es soll ein einstellbare Intensität sein, die die helligkeit steuert.
- Die lichtquelle soll aktivierbar sein (enable).
[?] Erstelle Commands um die CamLight zu steuern.

```text
  Zusammenfassung

  Ich habe erfolgreich das Camera Light (Fackel/Taschenlampe) Feature implementiert:

  Implementierte Komponenten:

  1. CameraService Erweiterung (packages/engine/src/services/CameraService.ts):
  - SpotLight wird an cameraEnvironmentRoot angehängt (folgt automatisch der Kamera)
  - Richtung wird jeden Frame aktualisiert um in Blickrichtung zu zeigen
  - LocalStorage-Persistierung für alle Einstellungen
  - Public API Methoden für vollständige Kontrolle

  2. Neue Commands (packages/engine/src/commands/camera/):
  - cameraLightEnable - Licht an/aus schalten
  - cameraLightIntensity - Helligkeit anpassen (0-10, Standard: 1.0)
  - cameraLightRange - Reichweite anpassen (Blöcke, Standard: 15)
  - cameraLightInfo - Alle Einstellungen anzeigen

  3. Command-Registrierung in CommandsFactory.ts

  Standard-Werte:

  - Range: 15 Blöcke
  - Intensity: 1.0
  - Farbe: Warmes Fackel-Licht (Orange: #FFA500)
  - Winkel: 0.8 Radiant (~46 Grad Lichtkegel)

  Verwendung (Commands):

  // Licht einschalten
  cameraLightEnable(true)

  // Helligkeit anpassen
  cameraLightIntensity(0.5)  // dimmer
  cameraLightIntensity(2.0)  // heller

  // Reichweite anpassen
  cameraLightRange(10)   // kurze Reichweite
  cameraLightRange(25)   // lange Reichweite

  // Informationen anzeigen
  cameraLightInfo()

  // Licht ausschalten
  cameraLightEnable(false)

  Die Einstellungen werden automatisch im LocalStorage gespeichert und beim nächsten Start wiederhergestellt. Das Licht folgt automatisch der Kamera und leuchtet immer in Blickrichtung!
```

## Water

[?] Erstelle einen WaterRenderer der Blocks mit shape WATER rendert.
Water ist nicht OCEAN und nutzt keinen shader.
- Orientire dich an CubeRenderer aber es wird nur die obere fläche gerendert.
- scaling, rotation, offsets bleiben gleich wie bei CubeRenderer
- Die Fläche ist eine halb durchsichtige Wasserfläche
- Es kann alternativ eine Textur 'TOP' oder fallback 'ALL' genutzt werden.
- nutze einen offset parameter um die farbe des wassers zu steuern (z.b. blau, grün, braun, etc)
- nutze einen weiteren offset parameter um die transparenz des wassers zu steuern (0-1)
```text
Funktionalität

  Der WaterRenderer rendert Blocks mit Shape.WATER und hat folgende Eigenschaften:

  Rendering

  - Nur obere Fläche: Rendert ausschließlich die TOP-Fläche des Blocks
  - Textur-Support: Nutzt Textur TOP (Index 1) mit Fallback auf ALL (Index 7)
  - Transformationen: Unterstützt scaling, rotation und offsets genau wie CubeRenderer

  Transparenz und Farbe

  Die Wasserfarbe und Transparenz werden über Offset-Parameter gesteuert:
  - offsets[24]: Red-Komponente (0-1)
  - offsets[25]: Green-Komponente (0-1)
  - offsets[26]: Blue-Komponente (0-1)
  - offsets[27]: Transparenz/Alpha (0-1, wobei 0=voll transparent, 1=voll opak)
  - Standard: Weiß (1, 1, 1) mit Alpha 0.5 (halb transparent)

  Beispiel-Verwendung

  // Blaues, halb-transparentes Wasser
  offsets[24] = 0.3;  // R
  offsets[25] = 0.5;  // G
  offsets[26] = 0.9;  // B
  offsets[27] = 0.5;  // Alpha

  // Grünes, trübes Wasser
  offsets[24] = 0.2;  // R
  offsets[25] = 0.6;  // G
  offsets[26] = 0.3;  // B
  offsets[27] = 0.4;  // Alpha

  // Braunes Sumpfwasser
  offsets[24] = 0.4;  // R
  offsets[25] = 0.3;  // G
  offsets[26] = 0.2;  // B
  offsets[27] = 0.6;  // Alpha

  Registrierung

  Der WaterRenderer wurde im RenderService registriert:
  - Import hinzugefügt
  - Private Property deklariert
  - Instanz im Constructor erstellt
  - Case Shape.WATER im getRenderer() Switch hinzugefügt

  Build-Status

  ✅ Der Build ist erfolgreich durchgelaufen ohne Fehler!

  Der WaterRenderer ist jetzt einsatzbereit für alle Blocks mit shape: Shape.WATER.
```
