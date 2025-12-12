
# Players

[x] Ziel ist es PlayerProvider in world-player zu entfernen und dafuer im PlayerService Entities zu nutzen
- Es gibt in world-shread RUser/RUserService. Hier sollen die 'Settings' als userSettings Map<String,Settings> gespeichert werden.
  - key ist der Client Type (src/main/java/de/mhus/nimbus/generated/network/ClientType.java) aber als String
  - Hilfsfunktionen in RUserService um Settings zu holen/setzen
- Es gibt bereits RCharacter/RCharacterService in world-shared. Diese kann komplett umgestaltet werden
  - Hier werden alle Items verwaltet die der Character bei sich hat.
- Erstelle eine Entity RUserItems in der alle Items des Users gespeichert werden pro regionId.
  - in world-shared ... Package region 
  - Items koennen mit labels versehen werden (z.b. 'equipped', 'backpack', 'stash1', ...) und gesucht.
  - itemId, amount, texture, name, labels (Set<String>)
- Erweitere PlayerService so, das er die Daten aus RUser und RCharacter/RUserItems nutzt.
- Entferne PlayerProvider

[x] Bonus!
Erstelle in shared eine Entity SSettings mit key/value Feldern um globale Einstellungen zu speichern.
  - key: String
  - value: String
  - type: String (z.b. 'string', 'secret',' 'boolean', 'int', 'double' ...)
  - options: Map<String,String> (value,title)
  - defaultValue: String
  - description: String
  - createdAt: Date
  - updatedAt: Date
- Erstelle SSettingsService mit Hilfsfunktionen um Einstellungen zu holen/setzen
- erstelle getStringValue(key), getBooleanValue(key), getIntValue(key)

[x] Es wird ein Editor für RRegion entities benoetigt.
- Erstelle in ../client/packages/controls ein neue region-editor.html und einen entsprechenden editor
- Erstelle in region-control REST Endpunkt /api/regions bzw. /api/regions/{regionId} zum holen und speichern der Region Entity
- Aehnlich wie in material-editor.html soll erst eine liste von regionen suchbar sein, dann bei click editierbar

[x] Es wird ein Editor für RUser entities benoetigt.
- Erstelle in ../client/packages/controls ein neue user-editor.html und einen entsprechenden editor
- Erstelle in user-control REST Endpunkt /api/users bzw. /api/users/{userId} zum holen und speichern der User Entity
- Aehnlich wie in material-editor.html soll erst eine liste von usern suchbar sein, dann bei click editierbar
- die 'settings' im User editierbar sein. edit, new, delete 

[?] Es wird ein Editor für RCharacter entities benoetigt.
- Erstelle in ../client/packages/controls ein neue character-editor.html und einen entsprechenden editor
- Erstelle in character-control REST Endpunkt /api/regions/{regionId}/characters/{characterId} zum holen und speichern der Character Entity
- Aehnlich wie in material-editor.html soll erst eine liste von charactern suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der region sein (in material-editor ist es die world)

[ ] Es wird ein Editor für WWorld entities benoetigt.
- Erstelle in ../client/packages/controls ein neue world-editor.html und einen entsprechenden editor
- Erstelle in world-control REST Endpunkt /api/regions/{regionId}/worlds/{worldId} zum holen und speichern der World Entity
- Aehnlich wie in material-editor.html soll erst eine liste von welten suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der region sein (in material-editor ist es die world)

[ ] In item-editor soll es moeglich sein oben noch die welt auszuwahlen (wie in material-editor)

[ ] In scrawl-editor soll es moeglich sein oben noch die welt auszuwahlen (wie in material-editor)
