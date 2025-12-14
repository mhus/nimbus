
# Editing

[?] Erstelle in world-control eine entity in der fuer den editor die umgebung fuer jede welt geseichert werden.

Die entity heisst WWorldEditSettings und hat die folgenden felder:
- worldId:string
- userId:string - wichtig userId, nicht PlayerId !
- palette: List of Palette block definitions to use (paste)


PaletteBlockDefinition:
- block : Block
- name : string
- icon: string (icon url bzw. auf die texture)

Erstelle in world-control einen REST controller unter /api/editor/settings um die WWorldEditSettings zu verwalten.
- GET /api/worlds{worldId}/editsettings : load settings for world and current user 
  ?sessionId= wird mit angegeben - wegen dem user
- POST /api/worlds{worldId}/editsettings/palette : set palette for world and current user (body: PaletteBlockDefinition list) 
  sessionId muss angegeben werden - wegen dem user
  Es wird immer die gesammte palette gespeichert

Benutze zum validieren WorldId.of()

[x] In edit-config soll die Block Palette managed werden koennen.
Dazu einen eigenen Aufklappbaren Bereich "Blocks" erstellen.

Folgende Endpunkte gibt es:
GET /api/editor/settings/worlds/{worldId}/editsettings?sessionId=...: Settings für Welt und User laden
POST /api/editor/settings/worlds/{worldId}/editsettings/palette?sessionId=...: Palette setzen (body: Liste von PaletteBlockDefinition)
Als Entity wird WWorldEditSettings genutzt bzw. nur die Palette. Die Patelle im speicher halten.
- Wenn ein Block SMarked wird kann er in die Palette uebernommen werden (kompletter Block ist im redis)
- Ein Block kann selektiert werden, dann wird dieser als aktueller Block fuer Paste im redis gespeichert
- Palette wird persistiert vie POST endpoint
- Aus de Block wird eine Textur geladen und als icon benutzt
- Aus dem Block wird die description geladen und als name benutzt (ggf. kuerzen)

[?] In ../client/packages/controls - block-editor.html soll es einen Button geben mit dem man eine Custom Block in einen Block Type speichern kann
- Es soll ein kleiner Dialog geoeffnet werden in dem die neue BlockTypeId:string eingegben werden kann
- Es muss ein neuer rest endpunkt erstellt werden, neben den bestehenden, der den custom Block json aufnimmt (payload) und die neue BlcokTypeId (im pfad?)
- Der endpunkt Wandelt den Custoim Block in einen BlockType um und speichert diesen via WBlockTypeService als neuen block type
- Es wird OK oder fehler zurueckgegeben und im Block Editor eine Meldung angezeigt, bei erfolg mit einem link auf den blocktype-editor.html?blockTypeId=...

[?] In blocktype-editor.html soll es einen Button geben, mit dem man den Block unter einer neuen BlockTypeId speichern kann
- Es soll ein kleiner Dialog geoeffnet werden in dem die neue BlockTypeId:string eingegben werden kann
- Es muss ein neuer rest endpunkt erstellt werden, der die alte und neue BlockTypeId im pfad bekommt und den BlockType dupliziert via WBlockTypeService
- Es wird OK oder fehler zurueckgegeben und im Block Editor eine Meldung angezeigt

[?] In asset-editor.html soll es einen Button geben, mit dem man den Asset unter einer neuen Path speichern kann
- Es soll ein kleiner Dialog geoeffnet werden in dem den neuen Path:string eingegben werden kann
- Es muss ein neuer rest endpunkt erstellt werden, der den alten und neuen Path im pfad bekommt und das Asset dupliziert via SAssetService
- Es wird OK oder fehler zurueckgegeben und im Asset Editor eine Meldung angezeigt