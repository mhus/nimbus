
# Lookup

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
  Muss auch bei sder suche beruecksichtigt werden, erst im branch, dann in der parent world suchen.

## Ablage

Pfade haben eine Ablage (storage) und pfad im storage, getrennt mit einem ':'. z.b. 'w:1234/models/tree.glb'
Default ist 'w' fuer world storage. Andere moeglichkeiten sind 'r' fuer shared region ablage, 'p' fuer shared
public region ablage. Alle anderen sind zentrale Ablagen.

Ablagen werden wie besondere Welten verwaltet, nur ohne Welt funktionalität. Sie beginnen immer mit einem '@' zeichen.
z.b. '@region', '@public', '@shared'.


## Assets

[x] Assets werden immer nur in der HauptWelt gespeichert. Instanzen und Zonen haben keine eigenen Assets. Branches koennen eigene Assets haben,
die aber auf die Hauptwelt zurueckfallen, wenn sie dort nicht gefunden werden (COW - Copy On Write).


## BackDrops

[x] BackDrops werden wie Assets behandelt. Sie werden immer in der Hauptwelt gespeichert. Instanzen und Zonen haben keine eigenen BackDrops. Branches koennen eigene BackDrops haben,
die aber auf die Hauptwelt zurueckfallen, wenn sie dort nicht gefunden werden (COW - Copy On Write).

## ItemTypes

[ ] Item Types kommen immer aus der Hauptwelt.


## Layers


