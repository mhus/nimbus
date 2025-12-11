
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

[ ] In edit-config soll die Block Palette managed werden koennen.
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
