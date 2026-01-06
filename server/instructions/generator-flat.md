
# Generator Flat

## Overview

Flat sieht die Welt als eine Ebene an, auf der jeder Pixel eine bestimmte Höhe hat und ein Block definition.
Die hoehe ist zwischen 0 - 255 und die Block Definition, es gibt 1-255 verschiedene Blocktypen, definiert
welcher Block und welche Blöcke, wenn noetig, darunter liegen.

## Service

[x] Die Entity WFlat wurde bereits in world-shared erstellt, bzw. vorbereitet. Erweitere WFlat damit sie mit mongoDB
als Entity funktioniert. Erstelle eine WFlatRepository und WFlatService in world-shared.

## Import

[?] Erstelle in world-generator einen FlatCreateService der eine Funktion hat mit der ein neues WFlat erstellt wird.
- oceanLevel wird aus wordInfo geladen.
- fuehre initWithSize() aus.

[?] Erstelle in FlatCreateService eine funktion die ein WFlat anlegt und aus einen WLayer type GROUND importiert.
- Dabei werden fuer alle X,Z Koordinaten im WFlat aus dem WLayerTerrain chunks die Blöcke geladen und
  der höchste Block, der ein Ground Block ist als level in WFlat gespeichert.
  Siehe auch DirtyChunkService, hie wird auch die Ground Height ermittelt.
- Der mountX und mountZ point ist der Punkt an dem der WFlat im WLayerTerrain ansetzt.
- Falls keine Blöcke vorhanden sind, wird der oceanLevel-10 als level gesetzt.

# Ein Parameter steuert (boolean) ob auch columns erstellt werden sollen.:
# - Ausserdem wird ein BlcokDefinition mit diesem BlockTypeId erstellt (wenn noch nicht existiert) und der Block
#  in columns gespeichert. Sind mehr als 255 Blocktypen vorhanden, wird ein log ausgegeben und der Type 1 benutzt.

[?] Erstelle einen JobExecutor FlatImportJobExecutor in world-generator der den FlatCreateService benutzt um ein 
WFlat aus einem WLayer type GROUND zu importieren.

```text
 1. JobExecutor Implementation:
    - Implementiert das JobExecutor Interface
    - Executor-Name: "flat-import"
    - @Component für Spring Bean Registration
    - Wird automatisch vom JobExecutorRegistry entdeckt
    - worldId wird aus job.getWorldId() genommen (nicht als Parameter)
  2. Pflicht-Parameter:
    - layerName: Name des GROUND Layers
    - sizeX: Breite (1-800)
    - sizeZ: Höhe (1-800)
    - mountX: Mount X Position (Start-Position im Layer)
    - mountZ: Mount Z Position (Start-Position im Layer)
  3. Optionale Parameter:
    - flatId: Identifier für das neue WFlat (wenn nicht angegeben, wird UUID generiert)
  4. Validierung:
    - Prüft alle Pflicht-Parameter auf Vorhandensein
    - Validiert Integer-Konvertierung
    - Prüft Größenlimits (1-800)
    - Wirft JobExecutionException bei Fehlern
  5. Ausführung:
    - Ruft FlatCreateService.importFromLayer() auf
    - Loggt Start, Fortschritt und Ergebnis
    - Gibt detailliertes JobResult mit allen Import-Informationen zurück
  6. Helper-Methoden:
    - getRequiredParameter(): Lädt Pflicht-String-Parameter
    - getRequiredIntParameter(): Lädt Pflicht-Integer-Parameter
    - getOptionalIntParameter(): Lädt optionale Integer-Parameter mit Default
  7. Error Handling:
    - Catch-Block für IllegalArgumentException (Parameter-Fehler)
    - Catch-Block für allgemeine Exceptions
    - Detailliertes Error-Logging
    - Aussagekräftige Error-Messages in JobExecutionException
```

## Export

[?] Erstelle in world-generator einen FlatExportService der eine Funktion hat mit der ein WFlat in einen WLayer type GROUND exportiert wird.
- Dabei werden fuer alle X,Z Koordinaten im WFlat in das WLayerTerrain chunks importiert
- Es wird nur importiert, wo columns nicht 0 (NOT_SET) ist.
- Wenn eine column importiert wird werden erst alle Blocks auf der column im WLayerTerrain geloescht
- Es dürfen keine Löcher entsehen, d.h. es muss erst das tiefste level der umliegenen Blöcke ermittelt werden und
  es muessen so lange Blöcke vom level nach unten eingetragen werden bis dieses lowestSibblingLevel erreicht ist.

[?] Erstelle einen JobExecutor FlatExportJobExecutor in world-generator der den FlatExportService benutzt um ein 
WFlat in einen WLayer type GROUND zu exportieren.
- Ein Parameter steuert (boolean) ob der WFlat danach geloescht werden soll.

```text

  1. JobExecutor Implementation:
    - Implementiert das JobExecutor Interface
    - Executor-Name: "flat-export"
    - @Component für Spring Bean Registration
    - Wird automatisch vom JobExecutorRegistry entdeckt
    - worldId wird aus job.getWorldId() genommen (nicht als Parameter)
  2. Pflicht-Parameter:
    - flatId: Database ID des zu exportierenden WFlat
  3. Optionale Parameter:
    - layerName: Name des Ziel-GROUND Layers (default: Layer aus dem WFlat importiert wurde)
        - Wenn nicht angegeben: Verwendet WFlat.layerDataId um den ursprünglichen Layer zu finden
      - Wenn angegeben: Erlaubt Export in ein anderes Layer
    - deleteAfterExport: Boolean (default: false)
        - Wenn true: Löscht WFlat nach erfolgreichem Export
      - Wenn false: Behält WFlat in der Datenbank
  4. Boolean-Parameter Parsing:
    - Akzeptiert flexible Werte:
        - "true", "1", "yes" → true
      - "false", "0", "no", leer/null → false
    - Case-insensitive Parsing
  5. Ausführung:
    - Ruft FlatExportService.exportToLayer() auf
    - Prüft deleteAfterExport Parameter
    - Löscht WFlat via WFlatService.deleteById() wenn gewünscht
    - Loggt alle wichtigen Schritte und Ergebnis
  6. Error Handling:
    - Catch-Block für IllegalArgumentException (Parameter-Fehler)
    - Catch-Block für allgemeine Exceptions
    - Detailliertes Error-Logging
    - Aussagekräftige Error-Messages in JobExecutionException
  7. Result Data:
    - Enthält flatId, worldId, layerName
    - Zeigt Anzahl exportierter Spalten
    - Zeigt ob WFlat gelöscht wurde
    - Format: "Successfully exported flat: flatId=X, worldId=Y, layerName=Z, exportedColumns=N, deleted=true/false"

  Der JobExecutor ist vollständig und bereit für die Verwendung über das Job-System!
```

[x] Ein weiteres flag beim Export soll die Ecken der Blocks glätten.
- Einfügen in Methode ✓
- Einfügen in JobExecutor ✓
Wenn das Flag gesetz ist wird beim obersten Block der type GROUND ist und der modifier 0 ein shape 'cube' hat., der Block so angepasst das die Ecken geglättet werden.
Dazu werden die offsets angepasst. Siehe client/BLOCK_OFFSETS.md abteilung CUBE je nachdem wie die nachbarn sind.
- ist ein nachbar tiefer, wird das offest auf -0.5 gesetzt
- ist ein nachbar gleich hoch, wird das offset auf 0.5 gesetzt
- sind zwei nachbarn an der ecke einer hoeher einer tiefer, bleibt es bei 0.
- Es wird nur das Y offset angepasst, X und Z bleiben 0.
- Cache die BlockTypen für einen Exportvorgang, damit nicht immer wieder die gleichen BlockTypen geladen werden muessen.

```text
  Corner-Smoothing Implementation:
  - Parameter smoothCorners: boolean (default: true) in FlatExportJobExecutor
  - BlockType Cache: Map<String, WBlockType> für Performance
  - applyCornerSmoothing() prüft: GROUND type, modifier==0, shape==1 (CUBE)
  - 4 Top-Corners: SW(13), SE(16), NW(19), NE(22) - nur Y-Offsets
  - Jede Ecke hat 3 Nachbarn: 2 orthogonale + 1 diagonale
    - SW: West(-1,0), South(0,-1), SW-Diagonal(-1,-1)
    - SE: East(+1,0), South(0,-1), SE-Diagonal(+1,-1)
    - NW: West(-1,0), North(0,+1), NW-Diagonal(-1,+1)
    - NE: East(+1,0), North(0,+1), NE-Diagonal(+1,+1)
  - Nachbar-Höhe aus flat.getLevel() mit Fallback am Rand
  - Offset-Regeln (Prioritätsreihenfolge):
    1. Alle 3 Nachbarn >=2 höher → +1.0
    2. Alle 3 Nachbarn >=2 tiefer → -1.0
    3. Gemischt (mind. einer höher UND mind. einer tiefer) → 0.0
    4. Mindestens einer tiefer (keiner höher) → -0.5
    5. Mindestens einer gleich (keiner tiefer) → 0.5
    6. Alle höher (aber nicht alle >=2) → 0.5
```

[x] Erweiterung: sind alle nachbarn einer ecke mindestes zwei hoeher, wird das offset auf +1.0 gesetzt.
sind alle nachbarn einer ecke mindestes zwei tiefer, wird das offset auf -1.0 gesetzt.

## Manipulation

## Darstellung

[?] Erstelle unter ../client/packages/controls einen flat-editor.html der es erlaubt WFlat aufzulisten.
- Ein entsprechender rest controller wird in world-control erstellt.
- Flats koennen hier gloescht werden
- Beim laden der daten, lade nur benoetigte Felder, nicht das gesamte objekt, da hier viele daten drin stecken und das laden ineffizient ist.
- Flats werden nicht editiert
- Wenn ein flat geoffnet wird, werden zwei Bilder angezeigt: 
  - Height Map: Fabrstufen Bild das die Hoehen anzeigt, tief: blau, mitte: gruen, hoch: rot
  - Block Map: Farbiges Bild das die Blocktypen anzeigt, jeder BlockTypeId bekommt eine Farbe zugewiesen 0=schwarz. 1-255 verschiedene Farben.
    - Farben unterschiedlich gestalten z.b. 1 rot, 2 gruen, 3 blau, 4 gelb, 5 cyan, 6 magenta, 7 orange, 8 lila, 9 braun, 10 pink, ... dann nach hinten leicht unterschiedlich
  - Beide untereinander anzeigen
- world selector oben einfügen

[x] Füge den Flat Editor in HomeApp hinzu

## Material Palette

[x] Erstelle einen FlatMaterialService in world-generator der es erlaubt an einem WFlat die MaterialDefinitions zu setzen.
- Es gibt eine Funktion setMaterialDefinition(flatId, int materialId, Map<String, String> properties)
- Es gibt eine Funktion setMaterialDefinitions(flatId, Map<String, String> properties)

[?] Es gibt nun in FlatMaterialService eine konstante fuer vordefinierte paletten. Erstelle eine funktion die eine vordefinierte Palette setzt.
- setPalette(flatId, String paletteName)

[x] Erstelle einen JobExecutor fuer den flat-material executor.
- Er soll mehrerer job typen haben:
  - set-material : setzt eine material definition
    - parameter: flatId, materialId, properties (json string)
  - set-materials : setzt mehrere material definitionen
    - parameter: flatId, properties (json string)
  - set-palette : setzt eine vordefinierte palette
    - parameter: flatId, paletteName

```text
  1. JobExecutor Implementation:
    - Implementiert das JobExecutor Interface
    - Executor-Name: "flat-material"
    - @Component für Spring Bean Registration
    - Wird automatisch vom JobExecutorRegistry entdeckt
    - Unterstützt drei verschiedene Job-Typen via jobType Parameter

  2. Job-Typ: "set-material"
    - Setzt eine einzelne Material-Definition
    - Pflicht-Parameter:
      - flatId: Flat database ID
      - materialId: Material ID (0-255)
      - properties: Format "blockDef|nextBlockDef|hasOcean"
        Beispiel: "n:stone@s:default|n:dirt@s:default|false"
    - Validiert materialId (0-255)
    - Parst properties String mit Pipe-Separator

  3. Job-Typ: "set-materials"
    - Setzt mehrere Material-Definitionen auf einmal
    - Pflicht-Parameter:
      - flatId: Flat database ID
      - properties: JSON String Format {"materialId": "blockDef|nextBlockDef|hasOcean", ...}
        Beispiel: {"1":"n:grass@s:default||false","2":"n:dirt@s:default||false"}
    - Parst JSON zu Map<String, String>
    - Nutzt FlatMaterialService.setMaterialDefinitions()

  4. Job-Typ: "set-palette"
    - Setzt eine vordefinierte Material-Palette
    - Pflicht-Parameter:
      - flatId: Flat database ID
      - paletteName: Name der Palette ("nimbus" oder "legacy")
    - Nutzt FlatMaterialService.setPalette()
    - Setzt alle 9 Standard-Materialien auf einmal

  5. Job-Typ Erkennung:
    - jobType Parameter (oder job.getJobType())
    - Switch-Case für die drei Typen
    - Error bei unbekanntem Typ

  6. Error Handling:
    - Validierung aller Parameter
    - Catch-Block für JobExecutionException
    - Catch-Block für allgemeine Exceptions
    - Detailliertes Error-Logging
    - Aussagekräftige Error-Messages

  7. Helper-Methoden:
    - parsePropertiesJson(): Einfacher JSON-Parser für Map<String,String>
    - executeSetMaterial(): Handler für set-material Typ
    - executeSetMaterials(): Handler für set-materials Typ
    - executeSetPalette(): Handler für set-palette Typ
```

[x] Erweitere den FlatCreateService Job (flat-import) so das beim anlegen eines neuen WFlat eine vordefinierte Palette gesetzt werden kann.
- parameter: paletteName

```text
  1. Neuer optionaler Parameter:
    - paletteName: Name der vordefinierten Material-Palette ("nimbus" oder "legacy")
    - Optional - wenn nicht angegeben, wird keine Palette gesetzt
    - Wird nach dem erfolgreichen Import automatisch angewendet

  2. Ausführung:
    - Import läuft wie gewohnt ab
    - Nach erfolgreichem Import wird geprüft ob paletteName angegeben ist
    - Falls ja: flatMaterialService.setPalette() wird aufgerufen
    - Setzt alle 9 Standard-Materialien auf einmal

  3. Error Handling:
    - Palette-Fehler führen NICHT zum Fehlschlag des gesamten Jobs
    - Warnung wird geloggt wenn Palette nicht angewendet werden kann
    - Job wird als erfolgreich markiert (Import war erfolgreich)
    - Grund: Palette ist nice-to-have, Import ist der Hauptzweck

  4. Logging:
    - Start-Log enthält paletteName
    - Separates Log für Palette-Anwendung
    - Success/Warning für Palette-Result
    - Finale Success-Message enthält palette="nimbus"/"legacy"/"none"

  5. Service Dependencies:
    - FlatCreateService: Für den Import
    - FlatMaterialService: Für die Palette-Anwendung (neu)
    - Beide werden via Constructor Injection bereitgestellt

  Der FlatImportJobExecutor unterstützt jetzt die automatische Palette-Anwendung!
```

## Manipulatoren

[x] Erstelle in world-generator einen FlatManipulatorService der verschiedene Manipulatoren verwaltet und ausfuehrt.
- Erstelle ein FlatManipulator Interface
  - getName()
  - manipulate(WFlat flat, int x, int z, int sizeX, int sizeZ, Map<String, String> parameters)
- FlatManipulatorService hat eine Lazy liste von FlatManipulatoren Beans.
- Es gibt eine Funktion executeManipulator(String name, WFlat flat, int x, int z, int sizeX, int sizeZ, Map<String, String> parameters)
  die den entsprechenden Manipulator sucht und ausfuehrt.

```text
  1. FlatManipulator Interface:
    - getName(): Gibt den eindeutigen Namen des Manipulators zurück
    - manipulate(): Führt die Manipulation auf einem WFlat aus
    - Parameter:
      - flat: Das zu manipulierende WFlat (wird in-place modifiziert)
      - x, z: Start-Koordinaten der Region (relativ zum Flat)
      - sizeX, sizeZ: Größe der zu manipulierenden Region
      - parameters: Manipulator-spezifische Parameter (Map<String,String>)

  2. FlatManipulatorService:
    - @Service Spring Bean
    - Lazy Injection aller FlatManipulator Beans
    - Verhindert zirkuläre Abhängigkeiten
    - Funktioniert auch wenn keine Manipulatoren vorhanden sind

  3. Manipulator Registration:
    - Constructor injiziert List<FlatManipulator> mit @Lazy
    - Registriert alle Manipulatoren in HashMap nach Namen
    - Warnt bei null/leeren Namen
    - Warnt bei Duplikaten (erster gewinnt)
    - Loggt alle registrierten Manipulatoren

  4. executeManipulator() Funktion:
    - Validiert Manipulator-Namen (required, nicht blank)
    - Sucht Manipulator in Registry
    - Wirft IllegalArgumentException wenn nicht gefunden
    - Validiert WFlat (not null)
    - Validiert Region-Koordinaten (non-negative)
    - Validiert Region-Größe (positive)
    - Validiert Region-Bounds (innerhalb des Flats)
    - Führt manipulator.manipulate() aus
    - Catch-Block für Exceptions mit detailliertem Error-Logging

  5. Helper-Methoden:
    - getAvailableManipulators(): Set aller verfügbaren Namen
    - hasManipulator(name): Prüft ob Manipulator existiert

  6. Error Messages:
    - Liste verfügbarer Manipulatoren bei "not found"
    - Detaillierte Bounds-Informationen bei "out of bounds"
    - Stack-Trace bei Manipulator-Exceptions

  Das Plugin-System für Flat-Manipulatoren ist bereit!
  Neue Manipulatoren können einfach als @Component Beans hinzugefügt werden.
```

[x] Erstelle einen JobExecutor FlatManipulateJobExecutor in world-generator der den FlatManipulatorService benutzt um einen
WFlat mit einem bestimmten Manipulator zu manipulieren.

```text
  1. JobExecutor Implementation:
    - Implementiert das JobExecutor Interface
    - Executor-Name: "flat-manipulate"
    - @Component für Spring Bean Registration
    - Wird automatisch vom JobExecutorRegistry entdeckt

  2. Job Type:
    - Der job type ist der Name des Manipulators
    - Beispiel: job type "raise" → führt "raise" Manipulator aus
    - Beispiel: job type "lower" → führt "lower" Manipulator aus

  3. Pflicht-Parameter:
    - flatId: Database ID des zu manipulierenden WFlat

  4. Optionale Region-Parameter:
    - x: X-Koordinate der Region (default: 0)
    - z: Z-Koordinate der Region (default: 0)
    - sizeX: Breite der Region (default: flat.getSizeX() - gesamte Breite)
    - sizeZ: Höhe der Region (default: flat.getSizeZ() - gesamte Höhe)
    - Wenn nicht angegeben: Manipulator wird auf gesamten Flat angewendet

  5. Optionale Manipulator-Parameter:
    - parameters: JSON String mit Manipulator-spezifischen Parametern
      Format: {"key1":"value1","key2":"value2"}
      Beispiel: {"height":"10","strength":"0.5"}

  5. Ausführung:
    - Lädt WFlat aus Datenbank via WFlatService
    - Wirft JobExecutionException wenn nicht gefunden
    - Parst optionale Parameter als JSON
    - Führt manipulatorService.executeManipulator() aus
    - touchUpdate() auf WFlat
    - Speichert aktualisiertes WFlat
    - Gibt detailliertes JobResult zurück

  5. Validierung:
    - Alle Pflicht-Parameter auf Vorhandensein prüfen
    - Integer-Konvertierung mit aussagekräftigen Error-Messages
    - JSON-Parsing mit Error-Handling
    - FlatManipulatorService validiert:
      - Manipulator existiert
      - Region-Bounds innerhalb des Flats
      - Region-Größe positiv

  6. Service Dependencies:
    - FlatManipulatorService: Für die Manipulator-Ausführung
    - WFlatService: Für laden/speichern des WFlat

  7. Error Handling:
    - Catch-Block für IllegalArgumentException (Parameter-Fehler)
    - Catch-Block für JobExecutionException (weitergeleitet)
    - Catch-Block für allgemeine Exceptions
    - Detailliertes Error-Logging
    - Aussagekräftige Error-Messages

  8. Helper-Methoden:
    - parseParametersJson(): Einfacher JSON-Parser für Map<String,String>
    - getRequiredParameter(): Lädt Pflicht-String-Parameter
    - getOptionalParameter(): Lädt optionale String-Parameter mit Default
    - getRequiredIntParameter(): Lädt Pflicht-Integer-Parameter

  Der JobExecutor ist bereit für Flat-Manipulationen!
  Beispiel: Manipulator "raise" hebt eine Region um X Einheiten an.
```

[x] Erstelle drei manipulatoren nach dem Vorbild von FlatWorldGenerator, HillyWorldGenerator und NormalWorldGenerator
- Benutze fuer den noise immer die echte World Coordinate damit unabhaengig generierte teile zusammen passen.
  - X = flat local X + WFlat.mountX
  - Z = flat local Z + WFlat.mountZ
- Benutze die Konstanten aus FlatMaterialService fuer die Materialien.

```text
  1. FlatTerrainManipulator (Name: "flat"):
    - Einfache flache Terrain-Generation
    - Parameter:
      - groundLevel: Höhe des Terrains (default: 64)
    - Setzt alle Positionen auf gleiche Höhe
    - Material: GRASS über Wasser, SAND unter Wasser

  2. NormalTerrainManipulator (Name: "normal"):
    - Normale Terrain-Generation mit mittlerer Variation
    - Parameter:
      - baseHeight: Basis-Höhe (default: 64)
      - heightVariation: Maximale Variation (default: 32)
      - seed: Random Seed für Noise (default: current time)
    - Multi-Octave Noise:
      - SCALE_1 = 0.01, WEIGHT_1 = 0.6
      - SCALE_2 = 0.05, WEIGHT_2 = 0.3
      - SCALE_3 = 0.1, WEIGHT_3 = 0.1
    - Material: GRASS über Wasser, SAND unter Wasser

  3. HillyTerrainManipulator (Name: "hilly"):
    - Hügelige Terrain-Generation mit hoher Variation
    - Parameter:
      - baseHeight: Basis-Höhe (default: 64)
      - hillHeight: Maximale Hügel-Höhe (default: 64)
      - seed: Random Seed für Noise (default: current time)
    - Multi-Octave Noise (größere Features):
      - SCALE_1 = 0.008, WEIGHT_1 = 0.7
      - SCALE_2 = 0.04, WEIGHT_2 = 0.2
      - SCALE_3 = 0.08, WEIGHT_3 = 0.1
    - Material: GRASS über Wasser, SAND unter Wasser

  4. World-Koordinaten für Noise:
    - worldX = flatLocalX + flat.getMountX()
    - worldZ = flatLocalZ + flat.getMountZ()
    - Sorgt für konsistente Noise über unabhängig generierte Bereiche
    - Regions passen nahtlos zusammen

  5. Material-IDs aus FlatMaterialService:
    - FlatMaterialService.GRASS (über Ocean Level)
    - FlatMaterialService.SAND (unter/gleich Ocean Level)
    - Konsistent mit Palette-Definitionen

  6. Parameter-Validierung:
    - Alle Höhen geclampt auf 0-255
    - Variations-Werte geclampt auf 0-128
    - Ungültige Parameter → Defaults mit Warning
    - FastNoiseLite mit OpenSimplex2 Noise-Type

  Alle drei Manipulatoren sind als @Component registriert und
  werden automatisch vom FlatManipulatorService gefunden!
```

[x] BorderSmoothManipulator (Name: "border-smooth"):
- Dieser Manipulator glättet die Ränder eines WFlat indem er die Höhen an den Rändern anpasst.
- Die äußeren Ränder werden nicht überschrieben, dadurch ist es möglich ein, zwei Rechtecke weiter innen sich an
  die äußere Kante anzugleichen.
- An den Ecken werden zusätzliche Punkte angepasst, damit es nicht so eckig aussieht.

```text
  1. BorderSmoothManipulator (Name: "border-smooth"):
    - Glättet Terrain-Höhen an den Rändern eines WFlat
    - Parameter:
      - depth: Wie viele Pixel nach innen geglättet werden (default: 3)
      - strength: Glättungsstärke von 0.0 bis 1.0 (default: 1.0)
      - cornerDepth: Zusätzliche Glättungstiefe für Ecken (default: 2)

  2. Rand-Glättung:
    - Äußerste Randpixel bleiben unverändert
    - Innere Pixel werden interpoliert zwischen aktuellem Wert und Randwert
    - Stärkere Interpolation näher am Rand
    - Separate Verarbeitung für oben, unten, links, rechts

  3. Ecken-Glättung:
    - Ecken erhalten zusätzliche Glättungstiefe (depth + cornerDepth)
    - Diagonale und rechteckige Bereiche um Ecken werden geglättet
    - Distanzbasierte Interpolation für weiche Übergänge
    - Verhindert eckige Kanten an den vier Ecken

  4. Interpolations-Algorithmus:
    - factor = (depth - distance + 1) / (depth + 1) * strength
    - newHeight = currentHeight * (1 - factor) + edgeHeight * factor
    - Näher am Rand = stärkerer Einfluss des Randwertes
    - strength Parameter erlaubt feine Kontrolle

  5. Original-Höhen:
    - Speichert alle Höhen vor der Manipulation
    - Verhindert dass frühere Änderungen spätere beeinflussen
    - Konsistente Interpolation über gesamten Bereich

  6. Validierung:
    - depth geclampt auf 1 bis min(sizeX, sizeZ) / 2
    - strength geclampt auf 0.0 bis 1.0
    - cornerDepth geclampt auf 0 bis depth
    - Bounds-Checking für alle Koordinaten

  Der BorderSmoothManipulator ermöglicht nahtlose Übergänge zwischen
  unabhängig generierten WFlat-Bereichen!
```
