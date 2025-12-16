
# Simple World Generator

## Overview

- Grundlegende Generator Umgebung erstellen
  - HexGridService implementieren
  - JobService implementieren, JobEntity ...
- Einfache Welt generatoren in world-control implementieren, nur ground blocks um erste welt zu generieren wenn sie erstellt wird
  - FlatWorldGenerator
  - NormalWorldGenerator
  - HillyWorldGenerator
- world-generator Modul erstellen fuer weitere Generatoren
  
## Grundlagen

[?] Erstelle ein HexGridEntity, HexGridRepository und HexGridService in world-shared Module.
- Ein HExGrid ist ein fünfeckiges Gitter das die Welt in Bereiche aufteilt.
- Jeder HexGrid Bereich hat eine Position die durch q und r Koordinaten beschrieben wird. Die Spitze zeigt nach oben.
- worldId und position bilden den eindeutigen Schlüssel.
- in WWorldEntity gibt es ein gridSize Attribut das den Durchmesser eines HexGrid Bereichs in Blöcken angibt.
- HexGridEntity
  - worldId : String
  - publicData : HexGrid
  - position : String // index - ist publicData.position.q + ":" + publicData.position.r
  - generatorParameters : Map<String, String>
  - createdAt : Date
  - modifiedAt : Date
  - Erstelle funktionen die ausrechnen welche Blocks in einem HexGrid sind basierend auf der gridSize (durchmesser) in WorldEntity und der position.
    - getFlatPositionSet(worldEntity) : Set<FlatPosition> // position as x:int,z:int

[?] Erstelle in world-control einen REST Controller HexGridController unter /control/worlds/{worldId}/hexgrid

[?] Erstelle einen JobEntity, JobRepository und JobService in world-shared Module.
- JobEntity
  - worldId : String
  - executor : String
  - type : String
  - status : String (PENDING, RUNNING, COMPLETED, FAILED)
  - parameters : Map<String, String>
  - createdAt : Date
  - startedAt : Date
  - completedAt : Date
  - modifiedAt : Date
- Erstelle ein Interface JobExecutor mit einer Methode execute(JobEntity job) und executorName() : String
- Im JobService werden mit einem Scheduler alle PENDING Jobs abgearbeitet, der entsprechende 
  JobExecutor wird ueber den executor Namen gefunden und aufgerufen. Mehrere Pods muessen sich synchronisieren
  damit der gleiche Job nicht mehrfach ausgefuehrt wird. Evtl. via redis Lock.
  - Jobs koennen nur abgearbeitet werden, wenn ein entsrechender executor vorhanden ist.
- Erstelle in JobService einen Scheduler der COMPLETED oder FAILED Jobs nach einer konfigurierbaren Zeit loescht.

[?] Erstelle in world-control einen REST Controller JobController unter /control/worlds/{worldId}/jobs

[?] Erstelle in world-control ein FlatWorldGenerator, NormalWorldGenerator und HillyWorldGenerator die das Interface WorldGenerator implementieren.
- in parameters gibt es die Option "grid" in der die hexgrid position steht.
- Lade die WWorldEntity um die gridSize zu bekommen und HexGridEntity
- in parameters muss eine Optiin "layer" sein (layer name) die angibt welches Layer generiert werden soll
- in parameters muessen die namen der BlockTypeId fuer die benoetigten Blocks stehen, z.B. "groundBlockTypeId", "hillBlockTypeId", "oceanBlockTypeId"
- Lade HexGridEntity um die generatorParameters zu bekommen. - definiere sinnvolle Parameter fuer die drei Generatoren. Schreibe static const in den generatoren für die Parameter Keys
  - z.b. hillHeight, hillFrequency, oceanLevel, groundLevel, ... nutze default values wenn nicht vorhanden
- Für NormalWorldGenerator gibt es bereits im test_server eine Implementierung in ../client/packages/test_server/src/world/generators/NormalGenerator.ts
- Die generatoren erstellen fuer jeden FlatPosition im HexGrid Bereich zwei/drei... Blocks in dem angegebenen Layer mit dem entsprechenden BlockTypeId
- Manipuliere die Block offsets um die Oberfläche organischer wirken zu lassen, siehe NormalGenerator.ts

[?] Erstelle ein neues modul world-generator fuer weitere Welt Generatoren.
- verlinke world-shared und shared module
- erstelle die basis ordner und GeneratorApplication class

[?] Erstelle in ../client/packages/controls ein neues hex-editor.html das es erlaubt Hex grids in einer World zu bearbeiten
- Als vorlage kann blocktype-editor.html dienen
- Oben rechts soll eine auswahl der WolrdId sein wie in blocktype-editor.html
- Erweitere HomeApp.vue um einen neuen Eintrag "Hex Grid Editor" der auf hex-editor.html verlinkt

[?] Erstelle in ../client/packages/controls einen neuen job-controller.html der es erlaubt Jobs in einer World zu verwalten
- Als vorlage kann blocktype-editor.html dienen
- Oben rechts soll eine auswahl der WolrdId sein wie in blocktype-editor.html
- Erweitere HomeApp.vue um einen neuen Eintrag "Job Controller" der auf job-controller.html verlinkt
