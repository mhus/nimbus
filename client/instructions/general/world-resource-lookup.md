
# Lookup von Resourcen auf Welten

## Welt Id

- worldId: regionId:worldName\[:zone\][!instance\] (aus universe.md)

Beispiele:
- earth616:main
- earth616:main:1234-553728-123

oder collection welt:

- @collectionType:collectionId

Beispiele:

- @shared:global-assets
- @region:world616

## Collection Welten

- @region:<regionId> - gemeinsame resourcen für alle welten in der region
  - Zugriff mit Assets: 'r/....' oder Blocks: 'r/blockTypeId'
- @shared:<collectionId> - gemeinsame resourcen für alle welten in dem server

- Keine chunks, kein login möglich

## Main World: region:world

- Chunks: direkt
- Assets: direkt
- BlockTypes: direkt
- Layers: direkt
- Entities: direkt
- EntityModels: direkt
- Backdrops: direkt
- Items: direkt / von Region
- ItemType: direkt / von Region
- Item Positions: direkt
- WorldInfo: direkt
- PlayerInfo: direkt

## World Zone: region:world:zone

- Chunks: direkt
- Assets: region:world
- BlockTypes: region:world
- Layers: direkt
- Entities: direkt
- EntityModels: region:world
- Backdrops: region:world
- Items: direkt / von Region
- ItemType: direkt / von Region
- Item Positions: direkt
- WorldInfo: direkt
- PlayerInfo: region:world

## Instance: region:world#instance / region:world:zone!instance

- Chunks: region:world:zone
- Assets: region:world
- BlockTypes: region:world
- Layers: region:world:zone
- Entities: region:world:zone
- EntityModels: region:world:zone
- Backdrops: region:world:zone
- Items: region:world:zone
- ItemType: region:world:zone
- Item Positions: direkt + region:world:zone (fallback)
- WorldInfo: direkt
- PlayerInfo: region:world

Instance kann nur lesen, nur Items koennen geschrieben werden.

## Aufloesen von Resource Gruppen

Resource ids/pfade beginnen mit der resource group 'group/...'

- group = 'w' : aktuelle welt, wird nicht umgeleitet
- group = 'r' : region collection world, wird umgeleitet in die welt: @region:<regionId>
- group = '*/' : shared collection world, wird umgeleitet in die welt: @shared:<collectionId>

- wenn kein '/' dann ist 'w/' der default

