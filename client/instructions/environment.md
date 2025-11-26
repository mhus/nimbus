
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

[?] Es soll eine sonne geben, es geht nur um die Darstellung der Sonne im SunService.ts 
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

[?] Erstelle in engine Commands um die Sonne zu steuern.
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

[ ] Sun Lense Flare Effekt
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

## Sky
