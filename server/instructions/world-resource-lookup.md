
# Lookup von Resourcen auf Welten

## Welt Id

- worldId: regionId:worldName\[:zone\]\[@branch\][#instance\] (aus universe.md)

Beispiele:
- earth616:main
- earth616:main@dev
- earth616:main:1234-553728-123

## Collection Welten

- @region:<regionId> - gemeinsame resourcen für alle welten in der region
  - Zugriff mit Assets: 'r/....' oder Blocks: 'r/blockTypeId'
- @shared:<collectionId> - gemeinsame resourcen für alle welten in dem server

