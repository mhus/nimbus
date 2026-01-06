
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
    - layerName: Name des Ziel-GROUND Layers
  3. Optionaler Parameter:
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



