
# World Entities

## WWorldCollection

[ ] Aktuellw erden World Collections unter WAnything gespeicehrt. Das soll in eine eigene Entität WWorldCollection umgestellt werden.
Erstelle die Entität WWorldCollection in world-shared mit den folgenden Feldern:
- worldId : String - collection id diese wordl collection, beginnt immer mit '@'
- title : String - Titel der Collection
- description : String - Beschreibung der Collection
Erstelle ein Repository WWorldCollectionRepository mit den üblichen CRUD Methoden.
- WWorldCollection wird in WWorldService mit verwaltet
- Stelle WWorldService auf die neue Entität um.

[ ] Erweitere world-editor um einen Tab "Collections" in der Weltverwaltung.
- unter ../client/packages/controls
- Erweitere den WWorldController REST Endpunkt um die benoetigten Collection Methoden

## WWorldInstances

[ ] Erstelle die Entität WWorldInstance in world-shared mit den folgenden Feldern:
- instanceId : String - eindeutige Id der Instanz
- worldId : String - die Welt auf der die Instanz basiert
- title : String - Titel der Instanz
- description : String - Beschreibung der Instanz
- creator : String - PlayerId des Erstellers der Instanz
- players : List<String> - Liste der PlayerIds die in der Instanz spielen dürfen
Erstelle auch das Repository WWorldInstanceRepository mit den üblichen CRUD Methoden.
Erstelle den Service WWorldInstanceService der die Instanzen verwaltet.

[ ] Erstelle einen WWorldInstanceListener in world-shared
- worldInstanceCreated(WorldInstanceEvent event)
- worldInstanceDeleted(WorldInstanceEvent event)
WWorldInstanceService hält eine LAZY liste von WWorldInstanceListenern und ruft diese bei create und delete auf.

[ ] Erweitere world-editor um einen Tab "Instances" in der Weltverwaltung.
- unter ../client/packages/controls
- Erweitere den WWorldController REST Endpunkt um die benoetigten instance Methoden
- Instancen können hier nur gelöscht werden

> WWorldInstanceListener wird für Entities und Items benötigt. Später klären was genau passieren muss.
> Erweitere WWorld im die Eigenschaft instanceable : boolean - ob die Welt Instanzen erstellen muss.

> Eintritt in instance, wann genau und wie wird das team ermittelt. Erstellen der Instane
> Austritt: beim verlassen des letzten Spielers löschen
> Instanzen die länger nicht genutzt wurden automatisch löschen (48h?)
