
# Layers

Es gibt schon Layers, aber die sollen nochmal neu arrangiert werden.

Beim createn von chunks sollen nur noch TerrainLayer (WLayerTerrain) beruecksichtigt werden. Hier geht es um
resourcen und performance. Da TerrainLayer chunk orientiert sind, ist es so einfacher.

TerrainLayers, die das Flag ground haben, keonnen direkt bearbeitet werden. Andere TerrainLayer
sind Model based Layers, d.h. sie werden durch ModelLayers gefüllt. d.h. ModelLayers werden einem
TerrainLayer zugeordnet. Die TerrainLayer sollten nicht direkt bearbeitet werden, da sie
jederzeit neu berechnet werden keonnen.

Lösung:

Aktuell gibt es in WLayer einen Typ und eine layerDataId die im WLayerTerrain und WLayerModel referenziert wird.
Zukünftig gibt es einen TYPE "GROUND" und einen "MODEL". Für beide gibt es genau einen WLayerTerrain Layer.
Für MODEL gibt es noch viele weitere WLayerModel Layer. Alle auf der gleichen layerDataId referenziert.

## Model Anpassen

WLayer und WLayerModel habe sich geändert.
Es werden nun alle WLayer als WLayerTerrain behandelt.

[?] Passe WLayerOverlayService und WLayerService an.

[?] Das erstellen von overlayModelLayersToTerrain() soll nicht automatisch passieren wenn
DirtyChunks behandelt werden. Erstelle eine separate Methode die ein einzelnes WLayerModel in den
WLayerTerrain überträgt. Achte darauf, das im WLayer die affectedChunks erweitert werden, wenn noetig.
- mit einem parameter soll festgelegt werden ob automatisch am ende DirtyChunks erzeugt werden sollen.

[?] Ausserdem wird eine recreateModleBasedLayer benoetigt. Diese methode muss erst alle chunks des
WLayerTerrain loeschen und dann alle WLayerModel Layer die auf diesen TerrainLayer referenzieren
neu erstellen.
- Achte darauf, das im WLayer die affectedChunks neu geschrieben werden.
- mit einem parameter soll festgelegt werden ob automatisch am ende DirtyChunks erzeugt werden sollen.

[?] Erstelle für beide funktionen einen JobExecutor in world-control.

```text
 Vollständige Zusammenfassung aller Änderungen

  1. Neue DTOs in world-shared (mit TypeScript-Generierung)

  Location: world-shared/src/main/java/de/mhus/nimbus/world/shared/dto/

  - LayerDto.java - @GenerateTypeScript("dto")
    - Felder: id, worldId, name, layerType, layerDataId, allChunks, affectedChunks, order, enabled, createdAt, updatedAt
    - Entfernt: mountX/Y/Z, ground, groups (jetzt in WLayerModel)
  - CreateLayerRequest.java - @GenerateTypeScript("dto")
    - Aktive Felder: name, layerType, allChunks, affectedChunks, order, enabled
    - Deprecated (für Backwards-Kompatibilität): mountX/Y/Z, ground, groups
  - UpdateLayerRequest.java - @GenerateTypeScript("dto")
    - Aktive Felder: name, allChunks, affectedChunks, order, enabled
    - Deprecated (für Backwards-Kompatibilität): mountX/Y/Z, ground, groups

  2. JobExecutors in world-control

  Location: world-control/src/main/java/de/mhus/nimbus/world/control/job/

  - TransferModelToTerrainJobExecutor.java
    - Executor Name: transfer-model-to-terrain
    - Parameter: modelId, markChunksDirty
    - Funktion: Einzelnes WLayerModel → WLayerTerrain übertragen
  - RecreateModelBasedLayerJobExecutor.java
    - Executor Name: recreate-model-based-layer
    - Parameter: layerDataId, markChunksDirty
    - Funktion: Kompletten MODEL-Layer neu generieren

  3. WLayerService Erweiterungen (world-shared)

  Neue Methoden:
  - transferModelToTerrain(String modelId, boolean markChunksDirty) - Zeile 639-708
  - recreateModelBasedLayer(String layerDataId, boolean markChunksDirty) - Zeile 440-545
  - createModel(...) - Zeile 681-707
  - updateModel(String modelId, Consumer<WLayerModel>) - Zeile 709-725
  - getModelIds(String layerDataId) - Zeile 761-765
  - countModels(String layerDataId) - Zeile 767-771
  - loadModelById(String modelId) - Zeile 781-786
  - deleteModelById(String modelId) - Zeile 788-800

  Helper-Methoden:
  - calculateAffectedChunks(WLayerModel) - Zeile 710-741
  - updateLayerAffectedChunks(WLayer, Set<String>) - Zeile 743-763
  - transferModelToTerrainChunk(...) - Zeile 765-847
  - recreateTerrainChunk(...) - Zeile 547-636
  - blockKey(Vector3) - Zeile 849-852
  - cloneBlockWithPosition(Block, Vector3) - Zeile 854-868

  4. WLayerModelRepository Erweiterungen (world-shared)

  Neue Methoden:
  - findIdsByLayerDataId(String) - Returns List<String> mit ORDER BY
  - countByLayerDataId(String) - Zählt Models
  - findFirstByLayerDataId(String) - @Deprecated, für Backwards-Kompatibilität

  5. WLayerOverlayService Anpassungen (world-shared)

  - generateChunk() - Alle Layer werden als Terrain behandelt
  - overlayTerrainLayer() - Lädt Terrain oder ruft overlayModelLayersToTerrain auf
  - overlayModelLayersToTerrain() - Merged Models step-by-step (memory-optimiert mit ORDER BY)

  6. API Controller Anpassungen

  world-control:
  - EditorController.java - DTOs importiert, alte Definitionen entfernt
  - ELayerController.java - DTOs importiert, alte Felder entfernt

  world-generator:
  - McpController.java - CreateLayerRequest importiert, updateLayerGroups entfernt, toLayerDto angepasst

  Performance-Optimierungen:

  ✅ Memory-optimiert: Models werden schrittweise geladen, nicht alle auf einmal
  ✅ Query-optimiert: MongoDB sortiert direkt mit sort = "{ 'order': 1 }"
  ✅ 50% weniger DB-Queries: Keine doppelten Ladevorgänge mehr
  ✅ TypeScript-Generierung: Alle DTOs werden automatisch als TypeScript generiert

  Alle Module kompilieren erfolgreich! 🎯
```

## Layer Editor

Der Layer Editor in ../client/packages/controls layer-editor.html muss angepasst werden.

[?] Bei MODEL Type Layers muessen auch Modelle angelegt und bearbeitet werden können die zu diesem Layer gehören.
- Der editor kann nun die DTOs in shared/generated/dto verwenden. Die auch in den REST Controllern verwendet werden.
- Wenn diese erweitert werden müssen, dann: DTO in world-shared anlegen, mvn install machen und dann sind die DTOs auch in TypeScript verfügbar.

[ ] Bug: beim schliessen des Model Dialogs im Layer Dialog, schliesst sich der Layer Dialog auch. Damit ist man ganz raus. ggf. wegen dem neu laden?

## Layer Grid Editor

Es soll einen Editor geben, der Block-Daten anzeigen kann. Die Quelle kann unterschiedlich sein, entweder ein LayerTerrain mit chunks
oder ein LayerModel, der alle Blocks mit einem mal gespeichert hat.
- Es soll ein isometrische Block Grid Ansicht erstellt weerden. Alle Blöcke sind in einer isometrischen Matrix zusammengefasst. X,Y,Z
- Die Blöcke sind immer durchsichtig, es wird nur das Drahtgitter angezeigt
- Die Blöcke sind entweder unsichtbar (nicht vorhanden) oder sichtbar (vorhanden)
- Jedes Drahtgitter eines Blocks kann eine eigene Farbe haben.
- Ein Block kann selektiert werden wenn er sichtbar ist und kein anderer davor ist.
- Benutze das bestehende Navigation Component (wird auch im Block Editor genutzt) um durch das Drahtgitter zu navigieren.


[?] Erstelle diese Componente in ../client/packages/controls

[?] In layer-editor.html soll für jeden Layer der GridEditor geöffnet werden können.
Es werden dann immer die Daten des WLayerTerrain angezeigt.
- Erstelle dazu einen neuen REST Endpunkt der die Chunks zurückgibt, aber nur die Coordinaten für jeden Block in einer Liste. 
- Bei Select eines Blocks werden die Daten des Blocks durch einen zweiten REST Endpunkt abgerufen und angezeigt.

[?] In layer-editor.html soll für jeden Model Layer der GridEditor geöffnet werden können.
Es werden dann immer die Daten des WLayerModel angezeigt.
- Erstelle dazu einen neuen REST Endpunkt der die coordinaten der Blocks des WLayerModel zurückgibt, in einer Liste.
- Bei Select eines Blocks werden die Daten des Blocks durch einen zweiten REST Endpunkt abgerufen und angezeigt.
  (Das kann der gleiche endpunkt sein, aber auf einem anderen Layer Model + ModelId)

-- Wichtig fuer WLayerTerrain: Die daten muessen in chunks geladen werden. Aktuell ist ein chunk 32 Blocks. Du kannst das aus der WorldInfo laden

## Editing Layers

In ../client/packages/controls edit-config.html wird das editieren von Layers controlliert
und in world-control EditService im Backend.

Die Funktionalität muss jetzt angepasst werden

[x] Wenn ein MODEL Layer ausgewaehlt wird, muss auch das Model ausgewaehlt werden, das editiert werden soll.
- Das muss mit im redis gespeichert werden.
- Beim schreiben/loschen von Blöcken muss das im EditService berücksichtigt werden.

[?] Im edit-config ist noch eine gruppe zur auswahl, die ist als integer wahlbar und nur noch in WLayerModel vorhanden.
Dort ist sie ein mapping von Text in Integer. d.h. in der auswahl bitte den text anzeigen, aber den integer speichern.
Der soll bei jedem block (LayerBlock) beim speichern automatisch gesetzt werden.

[?] Wenn im edit-config dialog das editieren gestartet wird (sollte im EditService passieren) und es ist ein Layer vom 
Typ Model, dann soll das model im client angezeigt werden.
- Dazu im WLayerModel eine Funktion getBlockPositions() die gibt nur die Positionen der Blöcke zurück, ffg als stream
  der direkt aus den Block-Daten heraus streamt.
- Wie schon im EditService soll richtung Player ein Commando zum Client gesendet werden (player local url muss genuzt 
  werden, wie in blockUpdateService.sendBlockUpdate()). 
- Command: modelselector,'enable','#00ff00',true,true,<liste von positionen>
  liste von positionen: x,y,z,....

[?] Wenn im edit-config dialog das editieren beendet wurde und es ist ein Layer vom Typ Model, dann soll das model im client 
entfernt werden.
- Command: modelselector,'disable'

[?] Ich brauche im layer-editor.html noch einen knopf am "Edit Layer Model" mit dem ich dieses eine model via WLayerOverlayService sofort in den Terrain Layer pushen kann und dann auch in den chunk damit ich die inhalte im client sehen
kann. Manchmal klappt das nicht.

[?] Ich brauche im layer-editor.html noch einen knopf am "Layer" mit dem ich dieses eine model via Job komplett neu pushen lassen kann.
- Dazu muss für MODEL Layer via JobService ein neuer job mit executor "recreate-model-based-layer", siehe RecreateModelBasedLayerJobExecutor.
- Bei GROUND Layer muss fuer jeden Chunk ein DirtyChunk via WDirtyChunkService erstellt werden.

## Retracing

[?] Erstelle in WLayerService eien funktion, in die man eine Block Position übergibt, die dann rückwaerts sucht, woher der Block an dieser stelle kommt.
Als Rückgabe also der WLayer, WLayerTerrain und optional WLayerModel.
- Lade nicht alle WLayerModel gleichzeitig. Wegen Speicherüberlauf.
- Du musst rückwerts in der sortierung nach order suchen.
- Gib auch den LayerBlock zurück wenn möglich

[?] in block instance editor in ../client/packages/controls, erstelle einen button der die layerService.findBlockOrigin() aufruft
und die daten zum origin ausgibt. Natuerlich muss nicht das komplette WLayerModel geladen werden sondern nur die metadaten.
Falls der block in einer gruppe ist, gib auch diese mit zurück.

## Chunk Data und layers

[x] das selektierte feld heightData soll meim erstellen von chunk Daten (WChunkService oder WDirtyChunkService) mit erstellt werden. Das format ist:
\[
x: number, // javaType: int
z: number, // javaType: int
maxHeight: number, // javaType: int
groundLevel: number, // javaType: int
waterLevel?: number // javaType: int
\]
fuer jede column im chunk (chunkSize * chunkSize) wird aus den blocks berechnet - muss beim mergen der Layers mit berechnet werden
- wie die maximale Hoehe ist, das kommt ertmal aus WWorld.publicData.stop.y
- groundLevel: hoechster Block in einem WLayerTerrain wo baseGround=true gesetzt ist und der Block-blockTypeId.shape ist nicht OCEAN (beispiel implementierung heirzu auch in ../client/packages/engine/src/services/ChunkService.ts
  Zeile 611:624 (Water)
- waterLevel: wenn eben ein Water ocean gefunde wurde, dann wird dieser wert gesetzt. Wenn nicht, ist das array kuerzer. 

[x] Erstelle in WChunkService eine funktion die fuer eine Block Column (x,z) den heightData heraus sucht und aus dem int[] ein HeightDataDto erstellt.

[x] Ich haette gerne einen chunk-editor.html in ../client/packages/controls mit dem ich in einer welt alle chunks sehe, und suchen kann, 
wichtig: pagination. und dann die metadaten (sind nicht viele) sehen kann und dann alle Blocks,
heighData im chunks in listen sehen kann. benutze zum transfer WChunkService.getStream um den speicher zu schonen. - im header einen world selector. 

[x] Fuege in HomeApp.vue einen Abschnit für chunk-editor hinzu

## Model Layer Transformation

Es wird mehrere Transform methoden geben, sie muessen als Buttons z.b. in einem dropdown menu hinzugefügt werden.

[?] Im layer-editor in ../client/packages/controls soll es für WLayerModel (WLayerModel Dialog) in layer vom Typ Model 
eine TransformAjustCenter methode geben. Dabei werden alle coordinaten des WLayerModel verschoben und mountX, mountY und mountZ 
werden in die entgegengesetzte richtung versetzt. So bleibt das model an der gleichen Position wie es war, aber der 0,0,0 ist jetzt 
transformiert.
- Auto Adjust
- Manual Adjust

[?] Im layer-editor soll es eine TransformMove methode geben, dabei werden alle coordinaten des WLayerModel verschoben um den angegebenen wert.
Es wird für das Model ein 'Sync to Terrain' gestartet.

[?] Copy Model Layer kopiert eine WLayerModel entity in ein anderes WLayer. Das Ziel muss nicht die gleiche WorldId haben.
Am besten: Auswahl der Welt, dann auswahl des WLayer (nur Model Layer). Eingabe des names im neuen Layer - der name kann 
hier angepasst werden. Das WLayerModel wird kopiert. WorldId und layerDataId 
werden automatisch uebernommen aus der dem Ziel WLayer.

[?] Im Copier dialog soll anstelle der id
1. die Welt selektiert werden (auch World Collecions - siehe world selector fuer die Abfrage am Backend)
2. der WLayer (nur Model Layer) selektiert werden


## referenceModelId

In WLayerModel gibt es ein Feld referenceModelId in dem soll die Referenz auf ein anderes WLayerModel angelegt werden koennen.
Die Referenz wird auf 'worldId/WLayerModel.name' gesetzt. z.b. earth616:westview/TownHall
- Das macht es einfach in der Datenbank zu suchen, worldId + name
Der Ersteller muss darauf achten, das der Name eindeutig ist. Werden mehrere gefunden, wird das erste genommen.

[?] Beim synchen des models auf das Terrain (im WLayerService?) wird der Referenz gefolgt und zuerst dieses geladen und in 
den Terrain Layer übertragen. Anstelle des eigentlichen WLayerModel inhaltes.
Dabei sollen die mount werte der beiden layer (reference und aktueller) addiert werden. Die rotation wird nur aus dem aktuellen 
Layer kopiert.
Danach werden die Blocks im aktuellen layer gesyncht.
Es ist nun moeglich mehrere referenz ketten yu haben. es wird eine maximale tiefe von 10 gesetzt.
- es ist vermutlich sinnvoll auch die rotation zu addieren.

[?] Im layer-editor an referenceModelId sol es ein such icon geben. Es geht ein dialog auf in dem
1. WorldId selektiert werden kann
2. Layername ausgewaehlt werden kann

[?] Der world-selector im header von layer-editor soll auch world collections zur auswahl stellen.

## Layer Clone Block

[?] In EditService soll es einen neuen Modus geben CLONE_BLOCK
- Erweitere EditAction.ts, erzuege modul generated mit mavn install neu
- erweitere edit-config.html
- erweitere EditService.java um die neue funktionalitaet
  - den Block suchen, in den aktuellen Layer einfuegen - im grunde eine kombination von bereits bestehenden Funktionen
