
# Word Terrain Service

## 01 Implementiere den World Terrain Service

```text
Erstelle im Modul `world-terrain` einen Terrain Service wie in der 
Datei `spec/12_world_terrain.md` beschrieben.

* Erstelle eine SpringBoot WorldTerrainService Klasse, die alle Funktionen des World Terrain Service implementiert.
* Erstelle DTOs im modul `shared`.
* Erstelle Rest-API-Endpunkte und nutze den WorldTerrainService 
  für die Implementierung.
* Erstelle für jeden Endpunkt in `examples/world_terrain.md` ein Beispiel mit curl,
  das die Funktionsweise des Endpunkts demonstriert.
* Erstelle im modul `world-shared` eine Bean Klasse `TerrainServiceClient`, 
  die die Kommunikation mit dem Identity Service ermöglicht.
* Erstelle die Kafka-Producer für den World Terrain Service, um Terrain-Daten zu veröffentlichen.
* Erstelle Unit-Tests für den Registry Service, um die Funktionalität zu überprüfen.
* Eine Authentifizierung soll über den WorldAuthenticationFilter in `world-shared` erfolgen.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.  
```
