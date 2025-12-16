
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

[x] Es wird ein Editor für RCharacter entities benoetigt.
- Erstelle in ../client/packages/controls ein neue character-editor.html und einen entsprechenden editor
- Erstelle in character-control REST Endpunkt /api/regions/{regionId}/characters/{characterId} zum holen und speichern der Character Entity
- Aehnlich wie in material-editor.html soll erst eine liste von charactern suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der region sein (in material-editor ist es die world)

[x] Es wird ein Editor für WWorld entities benoetigt.
- Erstelle in ../client/packages/controls ein neue world-editor.html und einen entsprechenden editor
- Erstelle in world-control REST Endpunkt /api/regions/{regionId}/worlds/{worldId} zum holen und speichern der World Entity
- Aehnlich wie in material-editor.html soll erst eine liste von welten suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der region sein (in material-editor ist es die world)

[x] In item-editor soll es moeglich sein oben noch die welt auszuwahlen (wie in material-editor)

[x] In scrawl-editor soll es moeglich sein oben noch die welt auszuwahlen (wie in material-editor)

[?] Es wird noch ein Editor fuer WEntity benoetigt.
- Erstelle in ../client/packages/controls ein neue entity-editor.html und einen entsprechenden editor
- Erstelle in entity-control REST Endpunkt /api/worlds/{worldId}/entities/{entityId} zum holen und speichern der Entity
- Aehnlich wie in material-editor.html soll erst eine liste von entitys suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der welt sein

[?] Es wird noch ein Editor fuer WEntityModel benoetigt.
- Erstelle in ../client/packages/controls ein neue entitymodel-editor.html und einen entsprechenden editor
- Erstelle in entity-control REST Endpunkt /api/worlds/{worldId}/entitymodels/{id} zum holen und speichern der Entity
- Aehnlich wie in material-editor.html soll erst eine liste von entitys suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der welt sein

[?] Es wird noch ein Editor fuer WBackdrop benoetigt.
- Erstelle in ../client/packages/controls ein neue backdrop-editor.html und einen entsprechenden editor
- Erstelle in entity-control REST Endpunkt /api/worlds/{worldId}/backdrops/{id} zum holen und speichern der Entity
- Aehnlich wie in material-editor.html soll erst eine liste von entitys suchbar sein, dann bei click editierbar
- Aehnlich wie bei material editor soll oben eine auswahl der welt sein

[?] In Asset Info Editor soll neben dem festen description feld noch die felder fest geben:
- source
- author
- license
Ein parameter steuert ob diese felder bearbeitet werden keonnen (nur im editor ausgegraut, keine tiefe funktionalitaet)
- 'licenseFixed': true/false - dieses feld wird auch nicht in der key/value liste angezeigt - default ist false

[?] Erstelle im index.html links auf alle editoren - in ../client/packages/controls/index.html
- region-editor
- user-editor
- character-editor
- world-editor
- material-editor
- item-editor
- item-type-editor
- scrawl-editor
...

[?] Erstelle fuer Assets einen weiteren rest ednpunkt .../license mit dem ich die lizenz daten eines assets holen oder setzen kann.
- Beim setzen wird automatisch licenseFixed auf true gesetzt.
```text
 Neuer Endpunkt: GET License Info
  - GET /api/worlds/{worldId}/assetlicense/{assetPath}
  - Returns: { source, author, license }
  - Gibt leere Strings zurück wenn Asset nicht existiert

  Neuer Endpunkt: SET License Info
  - PUT /api/worlds/{worldId}/assetlicense/{assetPath}
  - Body: { source, author, license }
  - Setzt automatisch licenseFixed = true
  - Returns: { source, author, license, licenseFixed: true }
```

[?] bei allen editoren, soll es mit url query paramtern moeglich sein
- worldId zu setzen (wie in material-editor)
- bzw. die regionId im world-editor
- die id (string) des items das bearbeitet werden soll, das item wird dann gleich geladen
- z.b. .../world-editor.html?regionId=123&id=bla456
- Es kann bereits url parameter geben (bei aelteren editoren), die bleiben dann so!

```text
  - ✅ useWorld() - Liest ?world= oder ?worldId=
  - ✅ useRegion() - Liest ?regionId= oder ?region=

  Editoren mit ?id= Parameter:

  Region-basiert:
  - ✅ region-editor - ?id={regionId}
  - ✅ user-editor - ?id={username}
  - ✅ character-editor - ?regionId={region}&id={characterId}&userId={userId}
  - ✅ world-editor - ?regionId={region}&id={worldId}

  World-basiert:
  - ✅ item-editor - ?world={worldId}&id={itemId}
  - ✅ itemtype-editor - ?world={worldId}&id={itemTypeId}
  - ✅ scrawl-editor - ?world={worldId}&id={scriptId}
  - ✅ entity-editor - ?world={worldId}&id={entityId}
  - ✅ entitymodel-editor - ?world={worldId}&id={modelId}
  - ✅ backdrop-editor - ?world={worldId}&id={backdropId}

  Beispiel-URLs:

  world-editor.html?regionId=earth616&id=main-world-1
  character-editor.html?regionId=earth616&userId=ecb&id=blade
  entity-editor.html?world=test-world-1&id=npc-001
  item-editor.html?world=test-world-1&id=sword_iron
```

[?] Bei allen editoren soll unten ein JSON Button sein, da geht ein Dialog mit dem Json auf.
Genauso wie bei material-editor. - nur die neuen editoren haben das noch nicht.
- Manchmal hast du 'Advanced (JSON)' gemacht, das kann dann weg.
