
# Players

[ ] Ziel ist es PlayerProvider in world-player zu entfernen und dafuer im PlayerService Entities zu nutzen
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

[ ] Bonus!
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
