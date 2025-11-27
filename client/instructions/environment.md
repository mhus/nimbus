
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

[x] Es soll ein CloudsService geben, der Wolken im Himmel darstellt.
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

[x] Erstelle Commands in engine um die Wolken zu steuern.

```text
 # Wolke im Norden, 200 Blöcke entfernt, Höhe 180
  cloudAdd "cloud-north" 0 -200 180 80 50 "textures/clouds/cloud1.png" 3 0 0

  # Wolke im Osten, 150 Blöcke entfernt, Höhe 160
  cloudAdd "cloud-east" 150 0 160 60 40 "textures/clouds/cloud2.png" 5 90 1

  # Wolke direkt über der Kamera
  cloudAdd "cloud-above" 0 0 200 100 60 "textures/clouds/cloud3.png" 0 0 2
```
## Horizont

[x] Ich muss noch etwas mit dem horizont machen, idee
- Am horizont eine farbverlaeufe einbauen, die sich je nach tageszeit aendern
- Um den horizont herum eine box ohne deckel und boden. Die vier seiten haben eine definierte y0 und y1 hoehe. Die Konfigurierbar sind
- die Box hat einen abstand von der kamera auf der xz ebene (distance) der konfigurierbar ist
- Die Box hat einen Farbverlauf, also color0 und color1
- Die Box hat einen transparent wert, damit sie durchsichtig werden kann (alpha)
- Die box kann deaktiviert werden (enabled = false)
- Die box nutz die rendering group ENVIRONMENT
- Die box wird an cameraEnvironmentRoot in CameraService gehaengt
- anstelle von y0 y1 kann auch y und height angegeben werden

[x] Erstelle in engine Commands um den Horizont zu steuern.
```text
  horizonGradientEnable true|false
  horizonGradientDistance 300
  horizonGradientPosition 0
  horizonGradientHeight 100
  horizonGradientColor0 0.7 0.8 0.9
  horizonGradientColor1 0.3 0.5 0.8
  horizonGradientAlpha 0.5
```

## Schnee und Regen (Niederschlag)

[x] Es soll ein PrecipitationService geben, der Schnee und Regen darstellt.
- Der Service soll in RenderService referenziert werden
- Der Niederschlag soll an cameraEnvironmentRoot in CameraService hängen
- Der Niederschlag soll in der rendering group PRECIPITIATION gerendert werden, nach der WELT
- Der Niederschlag soll als Partikel System dargestellt werden
- Es wird die Partikel groesse, Farbe und Textur geben (klein und dunkel fuer regen, gross und weiss fuer schnee)
- Der Niederschlag soll sich mit der Kamera bewegen
- Der Niederschlag soll eine einstellbare Intensität haben (z.b. 0-100)
- Der Niederschlag soll deaktivierbar sein (enabled = false) default ist false
- Erstelle Methoden um die Eigenschaften des Niederschlags zu setzen

[x] Erstelle in engine Commands um den Niederschlag zu steuern.

## Blitze

[x] Im PrecipitationService, separates system, das auch Blitze darstellt.
- Blitze mit Partikel Systemen darstellen
- schnelle flashes mit viel zufall und nach unten verzweigungen.
- durch command ausloesen mit dauer und intensity und eher oben oder bis zum boden
- Position zufaellig um die Kamera herum
- (da es ein wetter blitz ist und kein effekt auf en target gibt es keinen feste position)

## Automatisierung

Systeme:
- Tageszeiten (daytime)
- Wetter (regen, schnee, wind, wolken) (weather)
- Jahreszeiten (season) - Wird vom Server gesteuert

[x] Um aenderungen an Parametern des Wetters ueber einen Zeitraum zu animieren, brauchen wir eine einfache
Methode um Werte ueber die Zeit zu aendern. Der ModifierStack bietet sich hier an.
- Erweitere ModifierStack zu AnimationStack der wie andere Stacks in StackService verwaltet wird
- Wenn sich in AnimationStack der soll wert aendert, wird der ist Wert nicht sofort gesetzt, sondern ein weiteres lambda
  gibt den naechsten naeherungsschritt an und gibt auch zurueck ob ein weiterer schritt benoetigt wird.
- Dann wird nach (wait) ms der naechste schritt ausgefuehrt bis der soll wert erreicht ist.
- Aendert sich der Zielwert, ist das kein problem, denn der naechste schritt wird immer auf den aktuellen Zielwert bezogen.
- 'wait' soll einstellbar sein fuer den AnimationStack, default 100ms
- Beim setzen von Werten in AnimationStack kann jetz auch die wait time mit anegeben werden, die wird ueberschrieben wenn ein neuer wert gesetzt wird.
- Erweitere das command setStackModifier um die angabe der wait time fuer AnimationStack
- Erstelle ein command getStackModifierCurrentValue um den aktuellen wert eines modifiers im stack zu holen
```text
  - SetStackModifierCommand (commands/stack/SetStackModifierCommand.ts):
    - Syntax: setStackModifier <stackName> <modifierName> <value> [prio] [waitTime]
    - Erstellt oder aktualisiert einen Modifier im Stack
    - Unterstützt waitTime Parameter für AnimationStacks
  - GetStackModifierCurrentValueCommand (commands/stack/GetStackModifierCurrentValueCommand.ts):
    - Syntax: getStackModifierCurrentValue <stackName>
    - Gibt den aktuellen effektiven Wert eines Stacks zurück

 # Default-Wert direkt setzen
  setStackModifier ambientLightIntensity '' 0.8

  # Default-Wert mit Animation
  setStackModifier sunPosition '' 90 0 500
```

[x] Erstelle ein Command in engine um alle bekannten ModifierStacks aufzulisten

[?] Erstelle in EnvironmentService ein kleines ScrawlScript controll system um einfache wetter aenderungen ueber zeit zu automatisieren
- fuer verschiedene 'namen keys' koennen hinterlegt werden: EnvironmentScript
  - script: ScrawlActionDescriptions
  - group: string (z.b. 'environment')
  - currentScript (wird intern gesetzt)
  - lastRunTime: number (timestamp der letzten ausfuehrung)
- Diese scripte werden NICHT remote (zum server) uebertragen, sondern nur lokal ausgefuehrt (ggf wird hier in ScrawlActionDefinitions noch ein flag benoetigt)
- Ein Script kann ausgeloest werden, dabei prueft EnvironmentService ob fuer die Gruppe schon ein script laeuft und bricht dieses ggf. ab
- Erstelle ein Command um ein EnvironmentScript zu starten
  - startEnvironmentScript(group: string, script: ScrawlActionDescriptions)
- Erstelle ein Command um ein EnvironmentScript zu stoppen
  - stopEnvironmentScript(group: string)
- Erstelle ein Command im Scripte anzulegen und zu entfernene
    - createEnvironmentScript(name: string, group: string, script: ScrawlActionDescriptions)
    - deleteEnvironmentScript(name: string)
- Eine Liste von definitionen soll im WorldInfo hinterlegt werden keonnen und beim starten/init automatisch gefuellt werden
- Es soll moeglich sein das letzte ausgefuegrte script name por gruppe zu holen
  - getCurrentEnvironmentScriptName(group: string) : string | null
```text
1. EnvironmentService Erweiterungen (packages/engine/src/services/EnvironmentService.ts)

  - Interfaces hinzugefügt:
    - EnvironmentScript: Definition für Environment Scripts mit name, group und script
    - RunningEnvironmentScript: Tracking von laufenden Scripts mit executorId und startTime
  - Methoden implementiert:
    - createEnvironmentScript(name, group, script): Script anlegen
    - deleteEnvironmentScript(name): Script entfernen
    - startEnvironmentScript(name): Script starten (stoppt automatisch andere Scripts derselben Gruppe)
    - stopEnvironmentScriptByGroup(group): Script nach Gruppe stoppen
    - getCurrentEnvironmentScriptName(group): Name des aktuell laufenden Scripts für eine Gruppe abrufen
    - loadEnvironmentScriptsFromWorldInfo(): Automatisches Laden von Scripts beim Start
  - Wichtige Features:
    - Scripts werden NICHT zum Server übertragen (sendToServer: false)
    - Pro Gruppe kann nur ein Script gleichzeitig laufen
    - Beim Starten wird ein eventuell laufendes Script der gleichen Gruppe automatisch gestoppt
    - Alle Scripts werden beim Dispose gestoppt

  2. WorldInfo Erweiterung (packages/shared/src/types/World.ts)

  - Neues optionales Feld environmentScripts im settings Objekt
  - Scripts können in WorldInfo definiert werden und werden beim Start automatisch geladen

  3. Commands erstellt:

  - CreateEnvironmentScriptCommand: Script erstellen/registrieren
  - DeleteEnvironmentScriptCommand: Script löschen
  - StartEnvironmentScriptCommand: Script starten
  - StopEnvironmentScriptCommand: Script nach Gruppe stoppen
  - GetCurrentEnvironmentScriptCommand: Aktuell laufendes Script einer Gruppe abfragen
  - listEnvironmentScripts: Alle registrierten Environment Scripts auflisten
```

[x] Das commando clearClouds soll einen boolean parameter haben
- false: loescht nur wolken mit der geschwindigkeit 0 (statische wolken) (default)
- true: loescht alle wolken

[x] Erstelle in CloudsService eine Methode die das erstellen von clouds ueber einen Zeitraum automaisiert
- startCloudsAnimation(jobName : string,emitCountPerMinute: number, emitPropability: number, area: Area, speed:number, textures: string[])
- Die methode startet eine animation die ueber den angegebenen Zeitraum immer wieder neue wolken erstellt
- Alle parameter sollen mit einem leichten random arbeiten. emit emitiert mit emitpropability (0-1) eine neue wolke
- es wird immer ein random wert in area fuer die position genutzt
- es wird immer eine random textur aus der liste genutzt
- der speed wird nur mit der y (hoehe) leicht schneller, bis zu 1%, da die wolken im gleichen 'wind' fliegen sollen sie eien aehnliche geschwindigkeit haben
- neue wolken werden mit einer uuid als name erstellt
- Erstelle auch ein commando um die animation zu starten, texturen werden als kommaseparierte liste uebergeben
- Erstelle auch ein stopCloudsAnimation(jobName) um die animation zu stoppen, wird der name nicht angegeben werden alle animationen gestoppt
- Erstelle auch hierfuer ein commando
```text
 1. CloudsService erweitert (packages/engine/src/services/CloudsService.ts)

  Neue Interfaces:
  - Area: Definition für die zufällige Cloud-Positionierung mit min/max Werten für Position, Größe und Richtung
  - CloudAnimationJob: Interne Struktur für Animation-Jobs

  Neue Methoden:
  - startCloudsAnimation(): Startet automatisierte Cloud-Erstellung
    - Parameter: jobName, emitCountPerMinute, emitProbability, area, speed, textures[]
    - Erstellt Wolken mit randomisierten Parametern innerhalb der Area
    - Speed variiert mit der Höhe um bis zu 1% (wie gewünscht)
    - Verwendet UUIDs für Cloud-IDs
  - stopCloudsAnimation(): Stoppt Animation-Jobs
    - Optional: jobName (stoppt spezifischen Job)
    - Ohne Parameter: stoppt alle Jobs
  - getActiveAnimationJobs(): Liste aktiver Jobs

  2. Commands erstellt

  CloudsAnimationStartCommand (packages/engine/src/commands/clouds/CloudsAnimationStartCommand.ts)
  Usage: cloudsAnimationStart <jobName> <emitCountPerMinute> <emitProbability> <minX> <maxX> <minZ> <maxZ> <minY> <maxY> <minWidth> <maxWidth> <minHeight> <maxHeight> <minDirection> <maxDirection> <speed>
  <texture1,texture2,...>

  Beispiel:
  cloudsAnimationStart myJob 10 0.5 -100 100 -100 100 80 120 20 40 10 20 0 360 2.0 textures/cloud1.png,textures/cloud2.png

  doCloudsAnimationStart('myJob', 10, 0.5, -100, 100, -100, 100, 80, 120, 20, 40, 10, 20, 2.0, 90, 'textures/clouds/cloud1.png,textures/clouds/cloud2.png');


  CloudsAnimationStopCommand (packages/engine/src/commands/clouds/CloudsAnimationStopCommand.ts)
  Usage: cloudsAnimationStop [jobName]

  Beispiele:
  cloudsAnimationStop myJob    (stoppt spezifischen Job)
  cloudsAnimationStop           (stoppt alle Jobs)
```

[?] Erstelle in PrecipitationService - blitz sounds 
- Einem methode mit der im laufenden Betrieb die Intensitaet des Niederschlags angepasst werden kann ohne das der Niederschlag komplett neu gestartet werden muss (nur wenn auch lauft)
- Blitze sollen ein Event emittieren wenn ein Blitz erzeugt wurde, damit andere systeme darauf reagieren koennen (z.b. sound)
  - coordinaten des blitzes
  - intensity des blitzes
- Erstelle in PrecipitationService eine methode der man eine liste von Blitz Sounds mitgeben kann, die liste wird statisch gehalten, ist die liste nicht angegeben, wird sie leer gemacht
  - registerFlashSounds(soundPaths: string[])
  - erstelle auch ein Command in engine dafuer.
- Erstelle in PrecipitationService eine Methode die die Events fuer Blitze abonniert:
  - wenn ein event empfangen wird, wird ein zufaelliger sound aus der liste abgespielt an der position des blitzes (ist die liste leer wird nichts abgespielt)
  - es soll maximal alle 500ms ein sound abgespielt werden (damit es nicht zu viele sounds gibt)
  - Nutze den AudioService um die sounds abzuspielen, an der Position des blitzes, die emmisions weite soll 20 + intensity * 2 (intensity ca. 5 -20?, max emmision weite soll ca. 64 sein) einheiten sein
--> Da emitter und listener im gleichen Service sind, kann auf das senden von Events verzichtet werden, die Methode playThunder() kann direkt aufgerufen werden.
```text
1. Intensitätsanpassung zur Laufzeit ✓

  Die Methode setIntensity() existiert bereits und ermöglicht die Anpassung der Niederschlagsintensität im laufenden Betrieb ohne Neustart des Partikelsystems (packages/engine/src/services/PrecipitationService.ts:100-110).

  2. Thunder Sound System ✓

  Neue Properties:
  - thunderSoundPaths: string[] - Statische Liste der registrierten Blitz-Sounds
  - lastThunderSoundTime: number - Throttling-Zeitstempel

  Neue Methoden:

  registerFlashSounds(soundPaths: string[])

  - Registriert eine Liste von Thunder-Sound-Pfaden
  - Leere Liste löscht die registrierten Sounds
  - Logging der Änderungen

  playThunder(position: Vector3, intensity: number) (private)

  - Spielt zufälligen Sound aus der registrierten Liste
  - Throttling: Max. alle 500ms ein Sound
  - Emission Range: 20 + intensity * 2 (max 64)
  - Nutzt AudioService.playSoundAtPosition()
  - Wird direkt in createLightningGroup() aufgerufen (keine Events nötig)

  3. Integration in Lightning System ✓

  In createLightningGroup():
  - Berechnung der Intensity basierend auf flashCount (5-20 Range)
  - Konvertierung der relativen Position in absolute Weltkoordinaten
  - Direkter Aufruf von playThunder() bei Flash-Erstellung

  4. Engine Command ✓

  Neues Command: RegisterFlashSoundsCommand
  - Datei: packages/engine/src/commands/RegisterFlashSoundsCommand.ts
  - Usage: /registerFlashSounds [path1] [path2] ...
  - Beispiele:
  /registerFlashSounds audio/weather/thunder1.ogg audio/weather/thunder2.ogg
  /registerFlashSounds (löscht die Liste)
  - Registriert in NimbusClient.ts
```

[ ] Erstelle in SunService einen Parameter automaticSunAdjustment (boolean),default true
- erstelle ein Command in engine um den parameter zu setzen
- Wenn automaticSunAdjustment true ist, werden sunLightDirection und sunLightIntensity automatisch angepasst wenn die sonnenPosition geaendert wird
- Wenn die sonnenPosition (SunService) gesetzt wird, soll automatisch die sunLightDirection im EnvironmentService angepasst werden, berechne dafuer den winkel der sonne ueber dem horizont
- Berechne auch die intensity der sonne anhand der elevation (je hoeher die sonne desto heller) und setze den wert sunLightIntensity und amienteLightDirection automatisch
- Erstelle variablen (multiplier) im SunService womit diese Werte noch adjustiert werden koennen
  - sunLigthIntensityMultiplier (default 1.0)
  - ambientLightIntensityMultiplier (default 0.5)
- Erstelle dafuer Methoden zum setzen/lesen und Commands in engine

[ ] Erstelle in EnvironmentService einen mechanismus der die Tageszeit steuert
- in WorldInfo wird ein parameter dayTimeSyn
- Wenn sich die Tageszeit ändert ('day' | 'evening' | 'night' | 'morning') wird das entsprechende Script hier im EnvironmentService gestartet
  'daytime_change_day', 'daytime_change_evening', 'daytime_change_night', 'daytime_change_morning'

[x] Erstelle AnimationStack im StackModifierCreator:
- ambienteLightIntensity in EnvironmentService
- sunPosition in SunService
- sunElevation in SunService
- horizonGradientAlpha in HorizonGradientService
```text

  Ambient Light Intensity:
  - Default: 1.0
  - Animation: Exponential easing (Faktor 0.1)
  - Callback: environmentService.setAmbientLightIntensity()

  Sun Position:
  - Default: 90° (Osten)
  - Animation: Exponential easing (Faktor 0.05, langsamer)
  - Callback: sunService.setSunPositionOnCircle()

  Sun Elevation:
  - Default: 45° (über Horizont)
  - Animation: Exponential easing (Faktor 0.05, langsamer)
  - Callback: sunService.setSunHeightOverCamera()

  Horizon Gradient Alpha:
  - Default: 0.5 (semi-transparent)
  - Animation: Exponential easing (Faktor 0.1)
  - Callback: horizonGradientService.setAlpha()

  3. Verwendung

  # Beispiel: Sanfter Übergang der Ambient Light Intensity
  setStackModifier ambientLightIntensity weather 0.5 10 500

  # Beispiel: Sonnenposition auf Westen animieren
  setStackModifier sunPosition sunset 270 20 200

  # Beispiel: Aktuellen Wert abrufen
  getStackModifierCurrentValue ambientLightIntensity

doSetStackModifier('sunPosition','default',270, 0.1, 50);
```

[ ] Erstelle AnimationStack im StackModifierCreator:
- sunLigthIntensityMultiplier in SunService
- ambientLightIntensityMultiplier in SunService

[ ] In EnvironmentService soll ein Handle auf einen ModifierStack Element fuer AmbienteAudio geben
- dieser kann per command gesetzt werden/geloescht (leer string) werden
- EnvironmentAmbienteAudio(audioFile)

[ ] Wenn diw WorldInfo neu geladen wird, wird geprueft ob sich die season / seasonProgress geandert hat (gibt es schon) und
ggf die chunks neu gerendert (gibt es schon).
Hier soll, wenn sich die season geandert hat automatisch ein EnvironmentScript gestartet werden das die season aendert.
  - script name: season_winter, season_spring, season_summer, season_autumn
