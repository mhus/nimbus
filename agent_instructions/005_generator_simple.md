# World Generator Simple Service

## 00 Umbenennen

```text
Benennen Sie den World Generator Simple Service `world-generator-simple` in 
"World Generator Service", `world-generator` um.
```

```text
Perfekt! Die Umbenennung des World Generator Simple Service von world-generator-simple zu "World Generator Service" mit dem Namen world-generator wurde erfolgreich abgeschlossen.
Zusammenfassung der durchgeführten Änderungen:
✅ Verzeichnisumbenennung: world-generator-simple/ → world-generator/
✅ Maven-Konfiguration aktualisiert:
Haupt-pom.xml: Modulreferenz von world-generator-simple zu world-generator
Service-pom.xml: artifactId und Name aktualisiert zu "World Generator Service"
✅ Java-Package und Klassen umbenannt:
Package: de.mhus.nimbus.worldgenerator.simple → de.mhus.nimbus.worldgenerator
Hauptklasse: WorldGeneratorSimpleApplication → WorldGeneratorApplication
✅ Dokumentation aktualisiert:
agent_instructions/000_initial_setup.md: Referenzen aktualisiert
spec/00_overview.md: Service-Beschreibung aktualisiert
✅ Maven-Kompilierung erfolgreich: Das Projekt kompiliert ohne Fehler nach der Umbenennung.
Die Umbenennung wurde vollständig durchgeführt und alle Referenzen im Projekt wurden entsprechend angepasst. Der Service heißt nun offiziell "World Generator Service" und verwendet das Modul world-generator mit der Hauptklasse WorldGeneratorApplication.
```

## 01 Implementiere den World Generator Service

```text
Erstelle im Modul `world-generator` einen World Generator Service Skeleton wie in der 
Datei `spec/30_generator_overview.md` beschrieben.

* Erstelle eine SpringBoot GeneratorService Klasse, die alle Funktionen des World Generator Service implementiert.
* Erstelle ein Interface PhaseProcessor, das die Methoden für die Generierung von Welten definiert.
  Es hat eine Methode `processPhase(PhaseInfo phase)` und eine Methode `getPhaseType()` und `getProcessorName()`, 
  die den Typ der Phase zurückgibt.
* Implementiere die JPA Entity `WorldGenerator` und `WorldGeneratorPhase` und deren Repositorys.
* Erstelle Rest-API-Endpunkte und nutze den GeneratorService 
  für die Implementierung.
* Erstelle für jeden Endpunkt in `examples/generator.md` ein Beispiel mit curl,
  das die Funktionsweise des Endpunkts demonstriert.
* Erstelle im modul `server-shared` eine Bean Klasse `GeneratorServiceClient`, 
  die die Kommunikation mit dem Generator Service ermöglicht.
* Implementiere die Authentifizierung für den Registry Service über die 
  den JWTAuthenticationFilter in `server-shared` analog zum service `identity`.
* Erstelle Unit-Tests für den GeneratorService, um die Funktionalität zu überprüfen.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.  
```
