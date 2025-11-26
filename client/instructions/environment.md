
# Umgebung

## Übersicht

- Lichtquellen
- Sonne
- Sterne und Monde
- Wolken
- Nebel (done)
- Wind (done)
- Regen / Schnee Niederschlag
- Blitze
- Tageszeiten
- Spezielle Effekte: Dark events (Sturmwolken an einer stelle), Himmelsportale, etc.
- An speziellen stellen marker im Himmel (z.B. über einem Dungeon-Eingang)

Alles im EnvironmentService.

## Welt Lichtquellen

Es soll zwei lichtquellen geben: die Sonne und eine generelle Ambient Light Quelle. Die Sonne 
ist erstmal nicht mit der darstellung der sonne in verbindung zu bringen, sondern einfach nur eine
Lichtquelle die von einer Richtung aus scheint. Die Ambient Light Quelle sorgt dafür, dass auch die Schattenbereiche
nicht komplett schwarz sind.

Die Ambiente Lichtquelle ist bereits vorhanden, aktuell kann diese mit setLightIntensity() geregelt werden.
- Umbennenne in EnvironmentService von light in ambienteLigth
- Umbennenen von setLightIntensity in setAmbientLightIntensity
- Erstelle eine Methode setAmbienteLightSpecularColor(color: Color)
- Erstelle eine Methode setAmbientLightDiffuse(color: Color)
- Erstelle eine Methode setAmbientLightGroundColor(color: Color)

Erstelle ein sunLight in EnvironmentService für die Sonne.
- Erstelle eine Methode setSunLightIntensity(intensity: number)
- Erstelle weitere methoden fuer die Eigenschaften, wie z.b. direction, color ...

Erstelle in engine Commandos um beide lichtquellen zu steuern.
- ambientLight ....
- sunLight ....

```text
 Ambient Light Commands (packages/engine/src/commands/ambientLight/):
  - doAmbientLightIntensity [value] - Set/get intensity (0-10)
  - doAmbientLightDiffuse <r> <g> <b> - Set diffuse color (0-1)
  - doAmbientLightSpecular <r> <g> <b> - Set specular color (0-1)
  - doAmbientLightGroundColor <r> <g> <b> - Set ground color (0-1)

  Sun Light Commands (packages/engine/src/commands/sunLight/):
  - doSunLightIntensity [value] - Set/get intensity (0-10)
  - doSunLightDirection [x] [y] [z] - Set/get direction (normalized)
  - doSunLightDiffuse <r> <g> <b> - Set diffuse color (0-1)
  - doSunLightSpecular <r> <g> <b> - Set specular color (0-1)
```

## Vorbereitung:

[x] Ich möchte weitere Rendering Groups einführen, z.b. die Sonne. Deshalb möchte ich vorab eine Datei, 
z.b. renderingGroups.ts erstellen, in der ich alle Rendering Groups als constante definiere. Damit
können die Gruppen schnell justiert werden wenn eine neue Gruppe hinzukommt. Aktuell sollte es zwei Gruppen geben:
- Backdrop
- Default für alles andere / Bocke und Entities
Welche rendering groups nutze ich zur zeit in BabylonJs? 
```text
Aktuell nutzt du zwei Rendering Groups:
- Group 0: Backdrop, Fog und vermutlich alle Standard-Meshes (Blöcke/Entities)
- Group 1: Selection Highlights (werden über allem gerendert)
```

[x] Es soll environment wie Wetter und Sonne geben, diese hängen alle fest an der Camera position.
- Brauchen wir einen root mesh für die environment effekte, der an der kamera hängt damit diese kommenden anforderungen sauber umgesetzt werden können?
> Bitte einen einfachen plan machen. Pruefe CameraService, hier gibt es schon mesh an der cam. gibt es schon einen cameraRootMesh oder haengen fog und underwater separat an der cam? Es gibt noch keine wetter environments, das
  ist ja die vorbereitugn dazu. du kannst deutsch mit mir sprechen.

```text
cameraEnvironmentRoot in CameraService
```

## Sonne

[x] Es soll eine sonne geben, es geht nur um die Darstellung der Sonne im SunService.ts 
- Der Service soll im RenderService referenziert werden
- Die sonne soll ein Parikel System nutzen um die sonne darzustellen
- Die sonne sieht aus wie ein stern, hat 8 strahlen die nach außen gehen, die horizontale und vertikale strahlen sind etwas dicker/weiter als die diagonalen strahlen
- Die sonne soll immer an der position der kamera + einer gewissen entfernung in richtung
- Die sonne hat eine hoehe (radius) ueber der camera, die hoehe soll einstellbar sein
- Nutze cameraEnvironmentRoot in CameraService als root mesh fuer die sonne
- Die position der sonne wird auf einer basis kreisbahn um die kamera berechnet, die parameter der kreisbahn sind:
  - radius (entfernung von der kamera)
  - winkel (x,y, z) - oder nur (x,z) ???
- Zusaetzlich kann die Position auf dieser kreisbahn gesetzt werden durch eine einzelne Zahl (0-360) die den winkel um die Y achse angibt
- Die sonne kann deaktiviert werden (enabled = false)
- Erstelle eine methode setSunPositionOnCircle(angleY: number)
- Erstelle eine methode setSunHeightOverCamera(height: number)
- Das rendern der Sonen soll in einer separaten methode in SunService geschehen, falls die darstellung angepasst werden muss
- die Farbe der sonne soll einstellbar sein

- Benutze in renderingGroups.ts die rendering group ENVIRONMENT fuer die sonne
- Wenn ein shader genutzt wird, soll der ShaderService genutzt werden um den shader zu laden

Ist der plan sinnvoll, gibt es sinnvolle ergaenzungen?

[x] Erstelle in engine Commands um die Sonne zu steuern.
  - doSunEnable
  - ...
```text
  1. sunEnable - Sonne ein-/ausschalten
  - Usage: sunEnable [true|false]
  - Zeigt/versteckt die Sonne

  2. sunPosition - Horizontale Position
  - Usage: sunPosition [angleY]
  - 0° = Nord, 90° = Ost, 180° = Süd, 270° = West
  - Ohne Parameter zeigt die aktuelle Position

  3. sunElevation - Höhe über Kamera
  - Usage: sunElevation [degrees]
  - -90° = direkt unten, 0° = Horizont, 90° = Zenit
  - Validierung: -90 bis +90 Grad

  4. sunColor - Sonnenfarbe
  - Usage: sunColor [r] [g] [b]
  - RGB Werte zwischen 0 und 1
  - Beispiele:
    - sunColor 1 1 1 - Weiß
    - sunColor 1 0.9 0.7 - Warmer Sonnenuntergang
    - sunColor 1 0.5 0.2 - Orange
```

[x] Groesse der Sonne soll einstellbar sein (radius)
- Initial aus WorldInfo
- Initial alle Sun Parameter aus WorldInfo holen
- Erstelle ein Commando mit dem die Groesse der Sonne eingestellt werden kann

[x] Sun Lense Flare Effekt
- Es soll einen Lense Flare Effekt fuer die Sonne geben
- Der Effekt kann in den SunService integriert werden
- Ein Shader kann genutzt werden um den Effekt zu erzeugen
- Der Effekt soll nur sichtbar sein wenn die Sonne im sichtfeld der Kamera ist
- Der Effekt soll in der rendering group ENVIRONMENT gerendert werden
- Es sollen ein paar Partikel genutzt werden um den Lense Flare Effekt zu erzeugen
- Die Bewegung der Partikel soll sich an der Bewegung der Kamera orientieren
- Der effekt soll deaktivierbar sein (Enabled = false)
- Erstelle eine Methode setSunLenseFlareEnabled(enabled: boolean)
- Erstelle eine Methode setSunLenseFlareIntensity(intensity: number)
- Erstelle eine Methode setSunLenseFlareColor(color: Color)
- Erstelle in engine Commands um den Lense Flare Effekt zu steuern
```text
  sunLensFlareIntensity 1.5    # Intensität erhöhen
  sunLensFlareColor 1 0.7 0.4  # Orange Flares
  sunLensFlareEnable false     # Ausschalten zum Vergleich
```
- Wenn die sonne deaktiviert ist, soll auch der Lense Flare Effekt deaktiviert werden

## Sky Box

Es soll in SkyBoxService ein Sky Box geben, der den Himmel darstellt.
- Benutze Babylon SkyBox Mesh
- Der Service soll in RenderService referenziert werden
- Die Sky Box soll an cameraEnvironmentRoot in CameraService hängen
- Die Sky Box soll in der rendering group ENVIRONMENT gerendert werden
- Die Sky Box soll entweder eine Color oder Texture set nutzen können
- Es wird immer der Basis-Pfad für die Texturen angegeben, z.b. "textures/skybox/stars" 
- Die grosse der Sky Box Mesh soll einstellbar sein
- Ist es moeglich die sky box zu rotieren, dann soll der rotationswinkel einstellbar sein

Ist der plan sinnvoll, gibt es sinnvolle ergaenzungen?
```text
     Babylon.js erwartet automatisch diese Suffixe:
     - _px.png (Positive X)
     - _nx.png (Negative X)
     - _py.png (Positive Y)
     - _ny.png (Negative Y)
     - _pz.png (Positive Z)
     - _nz.png (Negative Z)

     Beispiel: textures/skybox/stars → textures/skybox/stars_px.png, etc.

 # Commands
  skyBoxEnable on
  skyBoxColor 0.2 0.5 1.0
  skyBoxTexture textures/skybox/stars
  skyBoxSize 2000
  skyBoxRotation 45
```

## Mond

[?] Es soll eine moeglichkeit geben bis zu drei Monde im Himmel darzustellen.
- Erstelle einen MoonService
- Der Service soll in RenderService referenziert werden
- Die Monde sollen an cameraEnvironmentRoot in CameraService hängen
- Die Monde sollen in der rendering group ENVIRONMENT gerendert werden
- Jeder Mond hat folgende Eigenschaften:
  - Enabled (boolean)
  - Size (number) - groesse
  - Texture (string) - Pfad zur Textur
  - PositionOnCircle (number) - Winkel um die Y Achse (0-360)
  - HeightOverCamera (number) - Hoehe ueber der Kamera
  - distance - entfernung von der kamera
- Erstelle Methoden um die Eigenschaften der Monde zu setzen

Ist der plan sinnvoll, gibt es sinnvolle ergaenzungen?

orientiere dich an der sonne, der Abstand von der Kamera muss setzbar sein, sonst sind die monde hinter der SkyBox.

Ist es moeglich einen Halbmond darzustellen ohne spezielle texture (rein geometrisch) mit einem parameter fuer die mond phase?

```text
# Mond 0: Vollmond
  moonTexture 0 textures/moon/moon1.png
  moonEnable 0 true
  moonSize 0 70
  moonPosition 0 180
  moonElevation 0 60
  moonDistance 0 450
  moonPhase 0 1.0

  # Mond 1: Halbmond
  moonEnable 1 true
  moonPhase 1 0.5

  # Mond 2: Sichel
  moonEnable 2 true
  moonPhase 2 0.2

  Besonderheiten:

  ✨ Shader-basierte Mond-Phasen - Komplett geometrisch ohne Texturen!
  - Smooth transitions zwischen Phasen
  - 3D-Effekt durch spherical shading
  - Anti-Aliasing am Rand
  - Phasen-Namen: New Moon, Waxing Crescent, First Quarter, Waxing Gibbous, Full Moon, etc.


       341 -      // If no moons in WorldInfo, create 3 default moons (ENABLED for testing)
       342 -      if (this.moons.length === 0) {
       343 -        this.moons.push({
       344 -          enabled: true, // ENABLED!
       345 -          size: 150, // Large size
       346 -          positionOnCircle: 0, // North
       347 -          heightOverCamera: 45,
       348 -          distance: 200, // Close to camera
       349 -          phase: 1.0, // Full moon
       350 -          texture: 'textures/moon/moon1.png', // Moon texture
       351 -        });
       352 -        this.moons.push({
       353 -          enabled: true, // ENABLED!
       354 -          size: 150,
       355 -          positionOnCircle: 120, // East-ish
       356 -          heightOverCamera: 30,
       357 -          distance: 400, // Medium distance
       358 -          phase: 0.5, // Half moon
       359 -          texture: 'textures/moon/moon1.png', // Moon texture
       360 -        });
       361 -        this.moons.push({
       362 -          enabled: true, // ENABLED!
       363 -          size: 150,
       364 -          positionOnCircle: 240, // West-ish
       365 -          heightOverCamera: 60,
       366 -          distance: 800, // Far away
       367 -          phase: 0.25, // Crescent
       368 -          texture: 'textures/moon/moon1.png', // Moon texture
       369 -        });
```

## Wolken

[?] Es soll ein CloudsService geben, der Wolken im Himmel darstellt.
- Der Service soll in RenderService referenziert werden
- Die Wolken sollen an cameraEnvironmentRoot in CameraService hängen
- Die Wolken sollen in der rendering group ENVIRONMENT gerendert werden
- Wolken werden mit texturen dargestellt, es gibt verschiedene Wolken Texturen
- Die Wolken sollen sich bewegen, die Geschwindigkeit und Richtung der Bewegung soll einstellbar sein
- Wolken koennen deaktiviert werden (enabled = false)
- Wolken bewegen sich auf verschiedenen hoehen (y position)
- Wolken werden ein und ausgeblendet wenn sie nah an der Kamera sind
- Erstelle Methoden um die Eigenschaften der Wolken zu setzen
- Es koennen maximal 10 Wolken gleichzeitig dargestellt werden

Wenn ich wolken moechte, dann sage ich auf welcher ebene, position (startX, startZ, y), groesse (width, height),
mit welcher texture und welcher geschwindigkeit und richtung. Die Wolke wird dann von der seite kommend eingeblendet, 
bewegt sich ueber die szene und wird auf der anderen seite wieder ausgeblendet.
Wenn die geschwindigkeit 0 ist, dann wird die wolke statisch dargestellt.
Wolken sollen wieder deaktiviert werden koennen. 

Koennen wir die anzahl der Wolken dynamisch anpassen, je nach anfrage. dann wird die cloud id als string uebergeben.

[?] Erstelle Commands in engine um die Wolken zu steuern.

```text
 # Wolke im Norden, 200 Blöcke entfernt, Höhe 180
  cloudAdd "cloud-north" 0 -200 180 80 50 "textures/clouds/cloud1.png" 3 0 0

  # Wolke im Osten, 150 Blöcke entfernt, Höhe 160
  cloudAdd "cloud-east" 150 0 160 60 40 "textures/clouds/cloud2.png" 5 90 1

  # Wolke direkt über der Kamera
  cloudAdd "cloud-above" 0 0 200 100 60 "textures/clouds/cloud3.png" 0 0 2
```
## Horizont

[ ] 
