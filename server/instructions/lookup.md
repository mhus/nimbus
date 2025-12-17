
# Lookup

> Klären ob ein Layer immer komplett dupliziert werden muss on Write. ggf Kopieren wir eine komplette welt wenn ein branch angelegt wird.
Das macht einiges einfacher! Aber viel mehr speicher verbrauch. ggf Branch support verschieben?

- Entscheidung: bei branches wird diffefrneziert, keine vollwertige Welt. Es wird entweder:
  - Immer resource der Hauptwelt genutzt und dort auch geaendert, keine duplikate (z.b. Assets, BackDrops, ItemTypes)
  - COW verfolgt (copy on write). Erst wenn etwas geaendert wird, wird es in den branch kopiert (z.b. chunks, Items, ItemPositions, Entities)
  - Struktueren muessen in den branch kopiert werden um hier aenderungen zu machen, read mit fallback, write if exists (z.b. layers)
  - Strukturen werden nur aus dem Branch genuzt, nicht aus der Hauptwelt. (?)
- Aber es wird keine Komplexe COW logik geben. Keep it simple!
- Ziel von Branches ist es Welten ändern zu können ohne die Hauptwelt massiv zu beeinflussen. Änderungen können dann in die Hauptwelt 
  gemerged werden. Vorrangig die Layer und Gameplay Ebene soll angepasst werden können. Basisdefinitionen wie Assets, BackDrops, ItemTypes sollen
  in der Hauptwelt bleiben.

## Aufräumen 

[x] In WorldAssetController.list() wird die komplette liste aller Assets geladen. Das geht so nicht.
- Assets muessen direkt aus der datenbank heraus gefiltert und paginiert werden. Soweit möglich, denn es kann viele 
  Assets geben. Vor allem pro WeltId müssen die Assets gefiltert werden.
- Assets haben einen prefix, der entscheidet in welcher Welt/Collection sie sind (es werden shared world collections unterstuetzt).
  Siehe dazu SAssetService.findByPath() 
- Die Filterung muss im SAssetService implementiert werden, nicht im Controller.
- Wenn keine suche (leer) angegeben wurde, wird wird in der welt gesucht (w:) 
- Die suche soll immer mit einem prefix beginnen, z.b. w:1234 oder r:5678 wenn kein prefix angegben, dann ist w: default
- Je nach prefix wird in einer anderen weltId gesucht (oder collectionId)
- Wie schon in findByPath() werden instanzen und zonen ignoriert bei assets. Bei branches wird eine COW strategie verwendet. Das
  Muss auch bei der suche beruecksichtigt werden, erst im branch, dann in der parent world suchen.

## Ablage

Pfade haben eine Ablage (storage) und pfad im storage, getrennt mit einem ':'. z.b. 'w:1234/models/tree.glb'
Default ist 'w' fuer world storage. Andere moeglichkeiten sind 'r' fuer shared region ablage, 'p' fuer shared
public region ablage. Alle anderen sind zentrale Ablagen.

Ablagen werden wie besondere Welten verwaltet, nur ohne Welt funktionalität. Sie beginnen immer mit einem '@' zeichen.
z.b. '@region', '@public', '@shared'.

## Assets

[x] Assets werden immer nur in der HauptWelt gespeichert. Instanzen und Zonen haben keine eigenen Assets. 
Branches haben keine eigenen Assets! Assets muessen neu angelegt werden in der Hauptwelt, wenn sie in einem Branch genutzt werden sollen.

[?] ENTFERNEN: Branches koennen eigene Assets haben, die aber auf die Hauptwelt zurueckfallen, wenn sie dort nicht gefunden werden (COW - Copy On Write).

[?] Im assets-editor bei der Welt-Auswahl auf HauptWelt einschränken (im REST Controller filter option einfügen). Keine Collection Welten hier.

Assets haben storage funktionalität.

## Block Types

[?] Block Types gibt es pro HauptWelt. Zonen und Instanzen haben keine eigenen Block Types.

Branches haben einen COW ansatz (copy on write). Es können keine Block Types gelöscht werden in Branches.
Block Types haben storage funktionalität. Default ist 'w'.

Beim laden von listen wird kein fallback gemacht.

[?] Im blocktype-editor bei der Welt-Auswahl auf HauptWelt + Branches einschränken (im REST Controller filter option einfügen). Keine Collection Welten hier.

## BackDrops

[x] BackDrops werden wie Assets behandelt. Sie werden immer in der Hauptwelt gespeichert. Instanzen und Zonen haben keine eigenen BackDrops. 
Branches haben keine eigenen BackDrops! BackDrops muessen neu angelegt werden in der Hauptwelt, wenn sie in einem Branch genutzt werden sollen.

[?] ENTFERNEN: Branches koennen eigene BackDrops haben, die aber auf die Hauptwelt zurueckfallen, wenn sie dort nicht gefunden werden (COW - Copy On Write).

BackDrops haben storage funktionalität.

[?] Im backdrop-editor bei der Welt-Auswahl auf HauptWelt einschränken (im REST Controller filter option einfügen). Keine Collection Welten hier.

## ItemTypes

[?] Item Types kommen immer aus der @region collection oder shared collections. Damit werden sie in der kompletten Region geshared.
Baranches koennen keine eigenen ItemTypes haben.

ItemTypes haben storage funktionalität, unterstuetzen aber nicht 'w'. Default ist 'r'.

[?] Im itemtype-editor bei der Welt-Auswahl auf @region + shared collections einschränken (im REST Controller filter option einfügen). ???

## Items

[?] Items kommen immer aus der @region collection. Damit werden sie in der kompletten Region geshared.

Items unterstuetzen keine storage funktionalitaet.
Damit haben branches auch keine eigenen Items.

[?] Im item-editor bei der Welt-Auswahl auf @region einschränken (im REST Controller filter option einfügen). ???

## Item Positions

[?] Item Positionen gibt es fuer jede Welt/Zone/Branch/Instanze separarat. Jede welt wird als separate Instanze behandelt. 

COW für Branches (copy on write). Es können keine Item Positionen gelöscht werden in Branches.

Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.
Es werden keine storage unterstuetzt, da sie immer welt instanze spezifisch sind.

[?] Im itemposition-editor bei der Welt-Auswahl auf alle Welten/Instanzen/Branches einschränken. Keine Collection Welten hier.

## Entity Models

[?] Entity Models kommen immer aus der @region collection oder shared collections. Damit werden sie in der kompletten Region geshared.

Entity Models haben storage funktionalität, unterstuetzen aber nicht 'w'. Default ist 'r'.
Damit haben branches auch keine eigenen Entity Models.

[?] Im entitymodel-editor bei der Welt-Auswahl auf @region + shared collections einschränken (im REST Controller filter option einfügen). ???

## Entities

[?] Entities gibt es pro Welt/Zone/Instanze separarat. Jede welt wird als separate Instanze behandelt.
Ausser Branches, diese verfolgen einen COW ansatz (copy on write). Es können keine Entities gelöscht werden in Branches.

Entities unterstuetzen keine storage funktionalitaet, da sie immer welt instanze spezifisch sind.

[?] Im entity-editor bei der Welt-Auswahl auf alle Welten/Instanzen/Branches einschränken. Keine Collection Welten hier.

## Chunk

[?] Chunks gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Ausser Branches, diese verfolgen einen COW ansatz (copy on write).
Instanzen keonnen keine eigenen Chunks haben, diese werden immer von der definierten Welt genommen.

Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.

Kein Editor!

## Layers / TerrainLayer / ModelLayer

[?] Layers gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Instanzen keonnen keine eigenen Layers haben, diese werden immer von der defineirten Welt genommen.
Für Branches können Layers kopiert werden. Der Layer muss aber erst kopiert sein, bevor er geändert werden kann.

Beim erzeugen von Chunks in Branches muss auf kopierte Layers geprüft werden.

Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.

[?] im Layer-Editor bei der Welt-Auswahl auf alle Welten/Zone/Branches einschränken. Keine Collection Welten hier.

## HexGrid

[?] HexGrid gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Instanzen keonnen kein eigenes HexGrid haben, dieses wird immer von der definierten Welt genommen.

In Branches wird ein COW ansatz verfolgt (copy on write). Es können keine HexGrids gelöscht werden in Branches.

[?] im HexGrid-Editor bei der Welt-Auswahl auf alle Welten/Zone/Branches einschränken. Keine Collection Welten hier.

## World

[x] Worlds haben keine storage funktionalität. Werden geladen / gespeichert wie sie sind.

## Jobs

[?] Jobs haben keine storage funktionalität. Werden geladen / gespeichert wie sie sind.
Instanzen haben keine eigenen Jobs.

[?] Im Job-Editor bei der Welt-Auswahl auf alle Welten/Zonen/Branches einschränken. Keine Collection Welten hier.

## PlayerProgress (future)

[ ] Player Progress nur auf 'world'

### Nochmal neu ...

Resource lookup muss im controller weitergegeben werden an den Service.
Suche mit query nach service.search(worldId, query, ....)

Die anforderungen sind jeweils etwas anders, aber benutze
var lookupWorldId = worldId.without... oder worldId.mainWorld() je nach anforderung.
var collection = WordlCollection.of(worldId, path);  um die correcte welt und path zu bestimmen
lookupWorld = collection.worldId();
path = collection.path();

Umgesetz wurde bewrits WorldAssetController -> SAssetService und EBlockTypeController -> WBlockTypeService

Setze nun auch EBackdropController um.

Setze nun auch EItemTypeController um.
Item Types kommen immer aus der @region collection oder shared collections. Damit werden sie in der kompletten Region geshared.
Baranches koennen keine eigenen ItemTypes haben.
ItemTypes haben storage funktionalität, unterstuetzen aber nicht 'w'. Default ist 'r'.

Setze nun auch EItemController um.
Items kommen immer aus der @region collection. Damit werden sie in der kompletten Region geshared.
Items unterstuetzen keine storage funktionalitaet.
Damit haben branches auch keine eigenen Items.

Setze nun auch WItemPositionService um (kein controller glaube ich) - wird in world-palyer benutzt.
Item Positionen gibt es fuer jede Welt/Zone/Branch/Instanze separarat. Jede welt wird als separate Instanze behandelt.
COW für Branches (copy on write). Es können keine Item Positionen gelöscht werden in Branches.
Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.
Es werden keine storage unterstuetzt, da sie immer welt instanze spezifisch sind.

Setze nun EEntityModelController um.
Entity Models kommen immer aus der @region collection oder shared collections. Damit werden sie in der kompletten Region geshared.
Entity Models haben storage funktionalität, unterstuetzen aber nicht 'w'. Default ist 'r'.
Damit haben branches auch keine eigenen Entity Models.

Setze nun EEntityController um.
Entities gibt es pro Welt/Zone/Instanze separarat. Jede welt wird als separate Instanze behandelt.
Ausser Branches, diese verfolgen einen COW ansatz (copy on write). Es können keine Entities gelöscht werden in Branches.
Entities unterstuetzen keine storage funktionalitaet, da sie immer welt instanze spezifisch sind.

Prüfe WChunkService - ob hier noch angepasst werden muss.
Chunks gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Ausser Branches, diese verfolgen einen COW ansatz (copy on write).
Instanzen keonnen keine eigenen Chunks haben, diese werden immer von der definierten Welt genommen.
Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.

Setze ELayerController um.
Layers gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Instanzen keonnen keine eigenen Layers haben, diese werden immer von der defineirten Welt genommen.
Für Branches können Layers kopiert werden. Der Layer muss aber erst kopiert sein, bevor er geändert werden kann.
Beim erzeugen von Chunks in Branches muss auf kopierte Layers geprüft werden.
Beim laden von listen wird kein fallback auf die Hauptwelt gemacht.

Setze HexGridController um.
HexGrid gibt es pro Welt/Zone separarat. Jede welt wird als separate Instanze behandelt.
Instanzen keonnen kein eigenes HexGrid haben, dieses wird immer von der definierten Welt genommen.
In Branches wird ein COW ansatz verfolgt (copy on write). Es können keine HexGrids gelöscht werden in Branches.

Setze auch JobController um.
Jobs haben keine storage funktionalität. Werden geladen / gespeichert wie sie sind.
Instanzen haben keine eigenen Jobs.

