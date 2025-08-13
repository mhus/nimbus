
# World Bridge Service

## 01 Implementiere den World Bridge Service

```text
Erstelle im Modul `world-bridge` einen World Bridge Service wie in der 
Datei `spec/20_world_bridge.md` beschrieben.

* Erstelle eine SpringBoot WorldBridgeService Klasse, die alle Funktionen des World Bridge Service implementiert.
* Erstelle DTOs im modul `shared`.
* Erstelle einen WebSocket-Server der Verbindungen von Clients akzeptiert und Nachrichten verarbeitet.
* Erstelle die Kommandos wie in der Spezifikation beschrieben, merke dir an der Websocket-Verbindung, welcher User, Rollen, Welt und Registrierungen aktiv sind.
* Erstelle für jedes Kommando in `examples/world_bridge.md` ein Beispiel mit websocat,
  das die Funktionsweise des Endpunkts demonstriert.
* Erstelle die Kafka-Consumer für den World Terrain Service, um Terrain-Events zu verarbeiten. Filtere über Registrierungen und Sende die Events an die WebSocket-Clients.
* Erstelle Unit-Tests für den Service, um die Funktionalität zu überprüfen.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.  
```
