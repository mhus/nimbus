
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

## Sonne

Vorbereitung:

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

Sonne:

[ ] Es soll eine sonne geben, es geht nur um die Darstellung der Sonne im SunService.ts 
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




