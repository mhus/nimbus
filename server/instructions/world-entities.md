
# World Entities

## WWorldCollection

[x] Aktuell werden World Collections unter WAnything gespeicehrt. Das soll in eine eigene Entität WWorldCollection umgestellt werden.
Erstelle die Entität WWorldCollection in world-shared mit den folgenden Feldern:
- worldId : String - collection id diese wordl collection, beginnt immer mit '@'
- title : String - Titel der Collection
- description : String - Beschreibung der Collection
Erstelle ein Repository WWorldCollectionRepository mit den üblichen CRUD Methoden.
- WWorldCollection wird in WWorldService mit verwaltet
- Stelle WWorldService auf die neue Entität um.

[x] Erweitere world-editor um einen Tab "Collections" in der Weltverwaltung.
- unter ../client/packages/controls
- Erweitere den WWorldController REST Endpunkt um die benoetigten Collection Methoden

[x] Erstelle Einen Button 'Add Shared' im world-editor im Bereich Collections.
- Es öffnet sich ein Dialog der den Namen und Titel Abfagt und erstellt eine Collection mit dem Format '@shared:NAME'

## WWorldInstances

[?] Erstelle die Entität WWorldInstance in world-shared mit den folgenden Feldern:
- instanceId : String - eindeutige Id der Instanz
- worldId : String - die Welt auf der die Instanz basiert
- title : String - Titel der Instanz
- description : String - Beschreibung der Instanz
- creator : String - PlayerId des Erstellers der Instanz
- players : List<String> - Liste der PlayerIds die in der Instanz spielen dürfen
Erstelle auch das Repository WWorldInstanceRepository mit den üblichen CRUD Methoden.
Erstelle den Service WWorldInstanceService der die Instanzen verwaltet.

[x] Erstelle einen WWorldInstanceListener in world-shared
- worldInstanceCreated(WorldInstanceEvent event)
- worldInstanceDeleted(WorldInstanceEvent event)
WWorldInstanceService hält eine LAZY liste von WWorldInstanceListenern und ruft diese bei create und delete auf.

[x] Erweitere world-editor um einen Tab "Instances" in der Weltverwaltung.
- unter ../client/packages/controls
- Erweitere den WWorldController REST Endpunkt um die benoetigten instance Methoden
- Instancen können hier nur gelöscht werden

[x] Erweitere WWorld um die Eigenschaft instanceable : boolean - ob die Welt Instanzen erstellen muss.
- instanceable soll auch im world-editor gesetzt werden können

[ ] Wenn eine neue WSession angelegt wird Redis, das geschieht in world-control in ControlAaaController, dann prüfen ob die Welt instanceable ist.
- Nur für Actor PLAYERS, nicht für EDITORS
- Wenn ja, dann eine neue Instanz der Welt erstellen
- Die InstanceId wird in der Session gespeichert und zurück geliefert, hier als worldId uebernommen
- InstanceId wird im WWroldInstanceService mit generiert und muss eindeutig sein in der DB.

[ ] Wenn eine WSession in den status CLOSED geht und es ist eine Instance WorldId, dann wird dem WWorldInstanceService 
mitgeteilt das der Player die Instanz verlässt. Ist es der Letzte Player in der Instanz, wird die Instanz gelöscht.
- Dieser mechanismus muss im world-control implementiert werden.
- Wenn die session im world-player auf closed geht, wird ein Commando via WorldClientService an den world-control geschickt.
  - Fire and forget 
- Commando muss im world-control implementiert werden.

[ ] Instanzen die länger nicht genutzt wurden automatisch löschen (48h?)
- Im world-control einen Scheduler der regelmäßig die Instanzen prüft und löscht wenn sie länger nicht genutzt wurden.
- Es reicht modifiedAt zu prüfen.

## World Editor

[?] WWord hat viele Felder die im word-editor an der World nicht gesetzt werden können.
- Analysiere WWordl und WorldInfo
- Estelle einen Plan was alles gesetzt werden kann und in welchen sektionen
- Erweitere in world-editor den World dialog so das alle felder gesetzt werden können.


> WWorldInstanceListener wird für Entities und Items benötigt. Später klären was genau passieren muss.
> Erweitere WWorld im die Eigenschaft instanceable : boolean - ob die Welt Instanzen erstellen muss.
> Keine Instanzen für EDITORS, nur für PLAYERS - In instanzen kann nicht bearbeitet werdeen - weiten teils read only!

> Eintritt in instance, wann genau und wie wird das team ermittelt. Erstellen der Instance
> Austritt: beim verlassen des letzten Spielers löschen
> Instanzen die länger nicht genutzt wurden automatisch löschen (48h?)
