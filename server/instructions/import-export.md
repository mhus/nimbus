
# Import/Export

## Overview

Es soll moeglich sein strukturen als Dateien (JSON, YAML) so zu exportieren und improtieren, das sie in
GIT Repositories abgelegt werden können. Das gibt auch die möglichkeit collections zu importieren und
zu aktualisieren über mehrere Sectoren (Server) hinweg.

## Datenablage

In WAnything wird für jede externalResource ein Eintrag angelegt. Als data Objekt wird ein ExternalResourceDTO verwendet.
Die WAnything sind nicht welt spezifisch.

?? regionId ??

In WAnything Enthalten
- name
- title

ExternalResourceDTO:
- localPath (file system path)
- target : World target
- lastSync : Instant
- lastSyncResult : String
- types : List<String> (liste der zu synchronisierenden Typen, z.b. assets, backdrops, blocktypes, layers ...)
- autoGit: boolean // try to handle git repository, pull, commit, push

## ResourceSyncService

Erstelle in world-control einen ResourceSyncService der dafuer zustaendig ist daten zu exportieren und importieren. Er
hält eine lazy liste von ResourceSyncType.
- export(world: WorldId, definition ExternalResourceDTO, boolean force)
- import(world: WorldId, definition ExternalResourceDTO)

Erstelle das Interface ResourceSyncType:
- name: String
- export(dataPath: Path, world: WorldId, boolean force): void
- import(dataPath: Path, world: WorldId): void
- removeOvertaken(dataPath: Path, world: World): void // loescht nicht mehr vorhandene Eintraege

in die Resource YAML wird ein updatedAt Zeitstempel gespeichert. Dieser wird beim Exportieren
verwendet um zu erkennen ob ein Eintrag aktualisiert wurde.

Resourcen werden möglichs so gespeichert, das 
- sie durch ein diff/patch tool gut vergleichbar sind.
- sie ebi gleichen daten kein update im content haben

## AssetResourceSyncType

Exportiert in den subfolder "assets" alle Assets als YAML dateien und die binary daten.
Die daten werden wie in den assets in die richtigen ordner gespeichert.
Daneben immer eine *.info.yaml Datei. z.b.

```text
assets/textures/blocks/stone.png
assets/textures/blocks/stone.png.info.yaml
```

Beim import werden entsprechende *.info.yaml dateien gesucht und entsprechend importiert.

## BackdropResourceSyncType

Exportiert alle Backdrops als YAML dateien in den ordner "backdrops".

```text
backdrops/none.yaml
```

## BlockTypeResourceSyncType

Exportiert alle BlockTypes als YAML dateien in den ordner "blocktypes".

```text
blocktypes/stone.yaml
```

## ModelLayerResourceSyncType

Alle WLayer vom typ model werden gesyncht in den Ordner "models".

Pro WLayer wird ein Oordner erstellt und darin für jedes model eine datei.

Im Layer ordner in einer datei _info.yaml werden die Daten aus dem WLayer gespeichert.

```text
models/NewTown/_info.yaml
models/NewTown/TownHall.yaml
```

## GroundLayerResourceSyncType

Alle GroundLayer werden gesyncht in den Ordner "ground".
Pro GroundLayer wird eine ordner erstellt. In diesem ordner ist eine _info.yaml die Daten aus dem WLayer beinhält.
Alle chunks (WLayerTerrain) werden in separate Dateien gespeichert mit dem Namen chunk_x_y.yaml.
Da es viele Dateien sein konnen, werden diese nochmal in unterordner gespeichert. Dabei wird chunk % 100 gerechnet.
Also cx % 100 und cz % 100. Als trenner _ nutzen, damit auch negative zahlen gut sichtbar sind.

```text
ground/groud/_info.yaml
ground/ground/0_0/chunk_0_0.yaml
ground/ground/-1_1/chunk_-18_177.yaml
```

## WAnything und Job

[x] Erstelle einen JobExecutor zum exportieren.
Es wird beim Job ein WAnything name mitgegeben, der in der collection 'externalResource' gesucht wird.
Job:
- name: String
- force: boolean
- worldId ist am Job
Ablauf:
- suche das WAnything objekt, lade das ExternalResourceDTO data objekt
- starte auf dem ResourceSyncService.export(worldId, data, force)

[?] Erstelle einen JobExecutor zum importieren.
Es wird beim Job ein WAnything name mitgegeben, der in der collection 'externalResource' gesucht wird.
Job:
- name: String
- force: boolean
- remove: boolean
- worldId ist am Job
Ablauf:
- suche das WAnything objekt, lade das ExternalResourceDTO data objekt
- starte auf dem ResourceSyncService.import(worldId, data, force)
- wenn remove true, dann starte auf dem ResourceSyncService.removeOvertaken(dataPath, world)

Anpassung: eigentlich kenn ein jobExecuter fuer bedes genutzt werden mit zwei verschiednenen job types
- export
- import
Nenne den jobExecutor dann "externalResourceSync"

```text
 Zusammenfassung der Änderungen:

  1. removeOvertaken wurde integriert

  - ✅ Entfernt als separate Methode im Interface
  - ✅ Integriert als boolean removeOvertaken Parameter in importData()
  - ✅ Effizienter: Nur ein Filesystem-Scan statt zwei

  2. Alle Implementierungen aktualisiert:

  - ✅ BackdropResourceSyncType
  - ✅ BlockTypeResourceSyncType
  - ✅ AssetResourceSyncType
  - ✅ ModelLayerResourceSyncType
  - ✅ GroundLayerResourceSyncType

  3. API aktualisiert:

  - ✅ ResourceSyncService: importData(worldId, definition, force, removeOvertaken)
  - ✅ ExternalResourceController: Import-Endpoint mit remove Parameter
  - ✅ ExternalResourceSyncJobExecutor: Kombinierter Job für Export/Import

  4. Job Executor Details:

  Executor Name: externalResourceSync

  Export Job (type: "export"):
  Parameters: name, force

  Import Job (type: "import"):
  Parameters: name, force, remove

  - remove = true: Löscht DB-Entities, die nicht im Filesystem sind
  - remove = false: Nur Import, keine Löschungen (DB behält zusätzliche Entities)

  Das System ist jetzt sauber und effizient strukturiert!
```

## Git Control

[?] wenn im ExternalResourceDTO autoGit true ist, dann versuche den git repository zu synchronisieren.
Versuche dazu eine library wie jgit zu nutzen. Erstelle eine strategy RepsoitoryControlWithJGit
zu erstellen und ein Interface RepositoryControl. Damit die funktionalitaet austauschbar ist.
- erweitere ggf. ExternalResourceDTO um neue Attribute fuer gitRepositoryUrl
- sei nicht vorsichtig. Es ist wichtig die daten zu sichern, nutze z.b. git reset -HARD

```text
Git-Integration mit JGit vollständig implementiert!

  Neu erstellt:

  1. RepositoryControl Interface - Abstraktion für Git-Operationen
    - initOrClone() - Initialisiert oder klont Repository
    - pull() - Pullt mit hard reset (sicher!)
    - commitAndPush() - Committed und pusht Änderungen
    - resetHard() - Reset --hard HEAD
    - isGitRepository() - Prüft ob Git-Repo
  2. RepositoryControlWithJGit - JGit-Implementierung
    - ✅ Verwendet JGit statt ProcessBuilder
    - ✅ Hard Reset vor Pull (Datensicherheit!)
    - ✅ Credential-Support (Username/Password)
    - ✅ Branch-Support
    - ✅ Robust Error Handling
  3. ExternalResourceDTO erweitert mit:
    - gitRepositoryUrl - Repository URL für Clone
    - gitBranch - Branch Name
    - gitUsername - Authentifizierung
    - gitPassword - Token/Passwort
  4. GitHelper komplett neu geschrieben
    - Nutzt RepositoryControl Interface
    - Arbeitet mit ExternalResourceDTO
    - Methoden: initOrClone(), pull(), commitAndPush(), resetHard()

  Workflow:

  Export:
  1. initOrClone() - Stellt sicher, dass Repo existiert
  2. pull() mit hard reset - Holt neueste Änderungen, verwirft lokale
  3. Export durchführen
  4. commitAndPush() - Committed und pusht Änderungen

  Import:
  1. initOrClone() - Stellt sicher, dass Repo existiert
  2. pull() mit hard reset - Holt neueste Änderungen
  3. Import durchführen

  Sicherheitsfeatures:

  - ✅ Hard Reset vor Pull - Keine Merge-Konflikte!
  - ✅ Austauschbare Strategie - Interface ermöglicht andere Implementierungen
  - ✅ Credential-Support - Sichere Authentifizierung
  - ✅ Robustes Error Handling - Fortsetzung bei Git-Fehlern

  Das System ist production-ready und sicher!
  
  
 Git-Credentials Fallback implementiert!

  GitCredentialsProperties erstellt

  Konfiguration in application.yaml:
  nimbus:
    git:
      username: your-git-username
      password: ghp_your_github_token
      branch: main

  Fallback-Strategie:

  Priorität:
  1. ExternalResourceDTO (höchste Priorität)
    - gitUsername, gitPassword, gitBranch
  2. application.yaml (Fallback)
    - nimbus.git.username, nimbus.git.password, nimbus.git.branch

  Verwendung:

  Für Entwicklung:
  # application.yaml oder application-dev.yaml
  nimbus:
    git:
      username: mhus
      password: ghp_xxxxxxxxxxxxx
      branch: main

  Für Produktion:
  - Credentials in ExternalResourceDTO setzen (pro Repository)
  - Oder zentrale Credentials in application.yaml
  

  Verwendung:

  Validate Job:
  {
    "type": "validate",
    "worldId": "earth616:westview",
    "executor": "externalResourceSync",
    "parameters": {
      "name": "export"
    }
  }

  Output zeigt:
  - ✅ Local path exists
  - ✅ Valid Git repository
  - ✅ Current branch: main
  - ✅ Remotes configured: 1
  - ✅ Remote connectivity OK
  - ✅ Username: from application.yaml (mhus)
  - ✅ Password: from application.yaml
  - ✅ Branch: from application.yaml (main)

  Für Entwicklung in application.yaml:
  nimbus:
    git:
      username: mhus
      password: ghp_your_github_token
      branch: main
  
```

# Direct database access

[x] Der export und import soll direkt aus der mongodb heraus passieren. Zumindest bei allen strukturen, die nicht aus dem StorageService (nur WLayer mit Type Ground) kommen.
- Das wurde bereits implementiert in: server/tools/world-export/src bzw. server/tools/world-import/src
- Beim import soll, wie in server/tools/world-import/src auch die Migration von Datensaetzien durchgefuehrt werden.
- Für Storage basierte daten (nur WLayer mit Type Ground) soll sich aktuell noch nichts ändern.

[x] Bei Storage basierten daten (nur WLayer mit Type Ground) soll die schema und schema version einmalig in der datei _schema.yaml gespeichert werden.
- Dazu wird aus der ersten chunk-datei die schema version gelesen und in die _schema.yaml datei geschrieben. Neben die _info.yaml datei.
- Dies kann direkt aus dem StorageData objekt gelesen werden.
- Es wird davon ausgegenagen, das alle dateien die gleiche version haben.

[x] Bei Storage basierten daten (nur WLayer mit Type Ground) sollen die daten nicht mehr als yaml datei, sonern als json direkt aus dem storage
gespeichert werden. also anstelle von chunk_x_y.yaml chunk_x_y.json - keine umwandlung in yaml dateien noetig.

[x] ground storage daten sind immer json dateien, so wie "{"cx":5,"cz":-3,"blocks":[{"block":{"position":{"x":160.0,"y":66....."
damit diese besser in git gemerged werden keonne sollten sie mehr new line enthalten, aber keine inline spaces. Die idee
ist, immer nach/vor einem {, }, [, ] oder nach , ein enter einzfuegen, also eine art formaterung in den json dateien, aber ohne new line.
Aber nicht, wenn es sich um einen value bereich: "" handelt.

## Imports 

[?] beim import muss die worldId bei jedem entity durch die worldId im job DTO ueberschrieben werden. Es kann sein, das die daten frueher einer anderen welt
zugeordnet wurden.
- Ausserdem muessen machche parameter auf prefix gerueft werden. Ein prefix wir durch ':' getrennt.
- Ist kein prefix, muss der ggf. vorgestellt werden. <prefix>:<path>
- Ist ein ein prefix vorhanden, muss er evtl erstelt werden.
Regeln als map in ExternalResourceDTO.prefixMapping:
- alt=new
- Wenn alt leerer string, ist ein fehlender prefix gemeint
z.B prefixMapping = {"":"r", "w":"s"}
- ersetze fehlenden prefix ("some/other/path" oder "/some/other/path") durch r:some/other/path
- ersetze "w:some/other/path" durch s:some/other/path
Zu ersetzende keys:
- blocktypes
  - publicData.modifiers.<key>.audio.path
  - publicData.modifiers.<key>.visibility.textures.<key> - wenn string ist, dann prefixMapping anwenden
  - publicData.modifiers.<key>.visibility.textures.<key>.path

[ ] Import muss noch geprüft werden. Erkennen von gleichen entities.

## Weitere Daten

[x] Exportiere auf die geliche weise wie WBackdrop auch WItem

[x] Exportiere auf die geliche weise wie WBlockType auch WItemPosition

[x] Exportiere auf die geliche weise wie WBlockType auch WEntity

[x] Exportiere auf die geliche weise wie WBlockType auch WEntityModel

[x] Exportiere auf die gleiche weise wie WBackdrop auch WItemType

[x] Exportiere auf die gleiche weise wie WBackdrop auch WAnything
- siehe ResourceSyncService und ResourceSyncType
- exportiere nur WAnything, die die richtige worldId haben
- Auch import implementieren, WAnythig ist eindeutig mit worldId+collection+name
- Implementiert in AnythingResourceSyncType
- Struktur: anything/{collection}/{name}.yaml
- Registriert in ResourceSyncService unter type "anything"

[x] Exportiere auf die gleiche weise wie WBackdrop auch WHexGrid
- bei hexgrid ist worldId+position eindeutig
- siehe ResourceSyncService und ResourceSyncType
- exportiere nur WHexGrid, die die richtige worldId haben
- Auch import implementieren, WHexGrid ist eindeutig mit worldId+position
- Implementiert in HexGridResourceSyncType
- Struktur: hexgrids/{position}.yaml (position "q:r" wird zu "q_r" im Dateinamen umgewandelt)
- Registriert in ResourceSyncService unter type "hexgrid"
