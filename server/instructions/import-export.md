
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

## Export





## Import

