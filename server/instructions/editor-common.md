# Umstellen des Edit Modus auf Common Editing

## Aktueller Stand

- Im player kann die session auf 'edit' gesetzt werden. 
- Dann prueft sie beim laden von chunks aus redis overlay daten zu jedem chunk
- Sind overlay daten vorhanden, werden diese mitgeladen und ueberlagern den chunk
- Overlaydaten sind an der session im redis gespeichert
- Der EditService schreibt die overlaydaten und sendet ein command an den player mit dem neuen Block, dieser wird an den client gesendet
- Beim 'speichern' werden die overlaydaten in den entsprechenden Layer geschrieben und DirtyChunk ausgeloest

- Ein client startet as editing indem es ein Layer blockt, wird (in redis?) vermerkt an der session
- beim speichern/disconnect werden die daten in den Layer geschrieben

## Common editing

- Editierte Blocks werden erstmal in WEditCache gespeichert bevor sie in Layers eingepflegt werden
  - Felder: 
    - worldId 
    - X
    - Z
    - chunk : String
    - layerDataId
    - block - Block daten : LayerBlock
    - createdAt
    - modifiedAt
  - Index: worldId + chunk
  - X, Z, chunk werden immer in world Kordinaten gespeichert die transformation in lokalen Layer Model Koordinalten 
      wird erst beim speichern in WLayerModel gemacht.
  - Es wird jeder Block einzeln gespeichert, so kann er schnell ausgetauscht werden (effektiv zum Editieren, nicht performant)
  - Beim Setzen prüfen ob es den eintrg schon gibt und ggf update
    - Es wird kein Lock auf die Tabelle gemacht, d.h. es können ausversehen doppelte einträge vorkommen, deshalb immer List<WEditCache> find...() machen und dann den ersten nehmen, rest löschen  
- Alle sessions mit actor 'editor' machen immer ein overload aus WEditCache
  - Beim auspiefern eines chunks pruefen ob in  WEditCache fuer worldId + chunk daten sind
  - Wenn Ja: Chunk wird normalerweise komprimiert gesendet, jetzt muss
    - chunk in ChunkBlock unkomprimiert werden
    - Alle Overlay Blocks in den chunk gemerged werden (reihenfolge egal, AIR Block führen zum löschen)
    - Versenden als unkomprimierter Chunk: ChunkData.c = null und ChunkData.blocks setzen
- Der EditService sendet die overlay block vie broadcast über redis an alle player pods, die diese welt aboniert haben,
  - Die player pods senden den block an die client die im actor EDITOR sind mit sessions, die den chunk registriert haben.
- User muss immernoch vor dem editieren Layer selektieren, aber kein explizietes Lock auf dem layer sondern nur damit 
  der EditService weiss welchem Layer das zuzuordnen ist (gibt es ein Lock?)
- Wenn 'Discard Changes' weiterhin geben soll, dann muss
  - Löschen aller WEditCache in dem layer
  - Ausliefern aller affected Chunks triggern - Löschen beim Client View
- 'Apply Changes' löst das mergen in die Layers aus.
  - WEditCacheDirty wird eingführt, das ist wie WDirtyChunk eine work queue.
  - WEditCacheDirtyService hat einen scheduler der ein Lock für die worldId+layerDataId holt und dann den WLayer füllt, 
    WEditCache mit der worldId+layerDataId löscht und WDirtyChunk einträge erstellt. Lock freigeben.
  - Bei WLayerModel werden immer alle Models mit einem mal für einen layerDataId geschrieben.
  - Beim Schreiben in WLayerModel werden die Block Coordinaten transformiert (wie bisher auch)
  - Bei WLayer, Type MODEL muss dann noch der Transfer in den WLayerTerrain gestartet werden
- WEditCacheDirty Entity:
  - worldId
  - layerDataId
  - createdAt

> Noch zu klären: Was machen wir mit verweissten WEditCache Einträgen - auto commit oder löschen oder einfach da lassen?

- Wenn world-generator daten schreibt, werden die als WEditCache geschrieben und dann mit einem WEditCacheDirty
  ins system eingepflegt, so kann es keine race conditions geben.

## Vorgehen

[x] Erstelle WEditCache Entity, WEditCacheRepository und WEditCacheService, in world-shared
  - Felder:
    - worldId
    - X
    - Z
    - chunk : String
    - layerDataId
    - block - Block daten : LayerBlock
    - createdAt
    - modifiedAt
  - Index: worldId + chunk
  - X, Z, chunk werden immer in world Kordinaten gespeichert die transformation in lokalen Layer Model Koordinalten
    wird erst beim speichern in WLayerModel gemacht.
  - Es wird jeder Block einzeln gespeichert, so kann er schnell ausgetauscht werden (effektiv zum Editieren, nicht performant)
  - Beim Setzen prüfen ob es den eintrg schon gibt und ggf update
      - Es wird kein Lock auf die Tabelle gemacht, d.h. es können ausversehen doppelte einträge vorkommen, deshalb immer List<WEditCache> find...() machen und dann den ersten nehmen, rest löschen  
[?] Erstelle WEditCacheDirty Entity, WEditCacheDirtyRepository und WEditCacheDirtyService, in world-shared
  - worldId
  - layerDataId
  - createdAt
[?] Beim BlockUpdate ein 'source' anhängen, hier den layerDataId + WLayerModel.name mitgeben
  - Siehe BlockUpdateCommand, es gibt nun ein optionales feld 'source' am block.
  - In BlockUpdateCommand soll das DTO Block benutzt werden
[?] In engine am Select-Model ein 'source' mitgeben damit neue Blocks nur mit dem gleichen source markiert werden 
  - in ../client/packages/engine
  - in SelektService.ts
  - Aktuell wird ein boolean modelSelectorWatchBlocks mitgegeben, der sull nun ein string werden, wenn null dann disabled, 
    wenn gesetzt, dann nur blocks mit dem source string markieren
  - Entsprechendes ModelSelectorCommand anpassen, string anstelle von boolean
[?] Beim benutzen des ModelSelectorCommand in world-control den model layer source mitgeben anstelle von true für modelSelectorWatchBlocks
  - siehe EditService
[?] Umstellen beim senden von Block Updates von Command auf Broadcast via redis
  - in EditService senden
  - in world-player empfangen und an Websocket Sessions weiter geben
  - Beim senden mitgeben ob ALLE oder nur Actor EDITOR die Änderung bekommen sollen
  - Altes BlockUpdateCommand beibehalten
  - BlockUpdateCommand Funktionalität des senden der Message an zentraler stelle machen
  - BlockUpdateBroadcastMessage
  - Siehe BroadcastService in world-player
[?] Speichern von editierten Blocks in WEditCache im EditService
  - In EditService wird das gemacht, hier zusaetzlich via WEditCacheService daten speichern
  - Speichern in redis overlay kann gleich mit weg
  - layer und layer model daten sind ja im redis gespeichert und koenne hier genutzt werden
[?] Umstellen der Chunk Overlay mechanik im world-player
  - In world-player beim senden von chunks, wird aktuell aus redis geholt, 
  - redis overlay kann gleich weg
  - nicht mehr auf isEditMode(), sondern isEditActor() prüfen 
  - wenn ein Overlay gemacht wird werden die daten nicht in ChunkData.c (comprimiert) sondern in ChunkData.blocks
    gesendet. (ChunkData.c muss auf null gestellt werden!), damit werden die daten unkomprimiert gesendet
    - BlockData.c decomprimieren, in 'ChunkData' umwandeln
    - hier Overlay Blocks einpflegen
    - im originalen ChunkData blocks und backdrop und heighData übernehmen
    - versenden
[?] Erstellen von WEditCacheDirty: merge WEditCache nach WLayer
  - Zum Lock mechanismus siehe auch WDirtyChunkService
  - Ein WEditCacheDirtyScheduler wird benötigt, wie auch schon für WDirtyChunkService
  - Immer ein einzelner WLayer wird gelockt und bearbeitet
  - Transform der WLayerModel nicht vergessen, hier aenderns sich Koordinaten und Rotation
  - Wird aktuell schon woanders gemacht - speichern von redis overlay in layers
[?] Umstellen der 'start editing' Mechanik
  - Siehe EditService
  - wird vom client 'controls' via REST an world-control versendet und hier verarbeitet in EditService
  - Eigentlich wie vorher. Kein Edit Mode im world-player mehr setzten, kein Lock auf den layer machen
[?] Umstellen der 'apply changes' Mechanik
  - siehe EditService
  - wird vom client 'controls' via REST an world-control versendet und hier verarbeitet in EditService
  - Funktionalität in WEditCacheService.applyChanges() implementieren
  - Nur noch ein Eintrag in WEditCacheDirty machen
[ ] Umstellen der 'discard changes' Mechanik - oder entfernen
  - siehe EditService
  - wird vom client 'controls' via REST an world-control versendet und hier verarbeitet in EditService
  - Funktionalität in WEditCacheService.discardChanges() implementieren
  - Merken aller chunks
  - Löschen aller einträge mit worldId + layerDataId
  - Für alle chunks ein Update senden (funktion gibt es schon) an die world-player dmait clients das overlay wieder entfernen

[ ] Aufräumen (obsolate code entfernen): 
  - Explizietes Edit Mode Flag an WebSocket Session kann weg
  - Overlay in redis entfernen (BlockOverlayService)
  - In PlayerSession isEditModer() weg

[ ] Einen editcache-editor.html anlegen in ../client/packages/controls
  - Auswahl der Welt via world-selector im header
  - Liste aller WLayer für die es einträge gibt + anzahl Einträge (Blocks)
  - Erstes Block Datum
  - Letztes Block Datum
  - Button: Discard Changes - WEditCacheService.discardChanges()
  - Button: Apply Changes - WEditCacheService.applyChanges()
  - Keine weiteren Editier möglichkeiten benötigt!
  - Eintrag in HomeApp anlegen

