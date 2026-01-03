
- Modulares Resourcen-System mit prefixen und shared Resources
  - assets migrieren mit group als ersten teil des pfades: texture/sun.png -> w/texture/sun.png
  - ggf. fallback auf 'w'
  - prefixes: w (world), r* (region), s* (shared)
  - In BlockTypen auch groupId verwenden (schon implementiert)
- Itemverwaltung mit Region sharen
- Editoren
  - layer-editor
  - layer-model-editor
  - blocktype editor fixen mit 3-status-checkbox
- layers
  - Model reference layer: referenziert shared modelle - mount, rotation - nicht editierbar
  - Layer Description, Owner, Editors, Version
  - groups fixen Map<int,name>
  - Bei chunk Erstellung die height daten erstellen
- Resources
  - Source, Licence, Author, Version
- Hex Map
  - Hex Map Editor
  - Radius in WorldInfo
  - Entry Points per Hex (?)
- World
- Entry Points
- Import / Exporter / Version management von Daten
  - Änderungsmanagement mongodb liquibase
[x] Mechanismus der Interaktion mit interaktiven Elementen, ggf. Space-Key wenn selected
[x] Balken+Name ueber Entities
[x] Zeichen, zb. Menge + Namen ueber Items

- lookup strategien
  - erweitere worldId um @ at start: @<collection type>:<collection id>
  - z.b. @region:earth616 -> shared collection of assets for earth616 ofer all worlds in this region, z.b. item texturen
  - z.b. @shared:nebulus -> shared collection 'nebulus' of assets for all worlds (in this server, server needs to sync them before)
