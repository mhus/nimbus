# World Generator Simple Service

## 00 Umbenennen

```text
Benennen Sie den Generator Simple Service `generator-simple` in 
"World Generator Simple Service", `world-generator-simple` um.
```

```text
Perfekt! Die Umbenennung des Generator Simple Service von generator-simple in "World Generator Simple Service" mit dem Namen world-generator-simple wurde erfolgreich abgeschlossen.
Zusammenfassung der durchgeführten Änderungen:
✅ Verzeichnisumbenennung: generator-simple/ → world-generator-simple/
✅ Maven-Konfiguration aktualisiert:
Haupt-pom.xml: Modulreferenz von generator-simple zu world-generator-simple
Service-pom.xml: artifactId und Name aktualisiert zu "World Generator Simple Service"
✅ Java-Package und Klassen umbenannt:
Package: de.mhus.nimbus.generator.simple → de.mhus.nimbus.worldgenerator.simple
Hauptklasse: GeneratorSimpleApplication → WorldGeneratorSimpleApplication
✅ Dokumentation aktualisiert:
agent_instructions/000_initial_setup.md: Referenzen aktualisiert
spec/00_overview.md: Service-Beschreibung aktualisiert
✅ Maven-Kompilierung erfolgreich: Das Projekt kompiliert ohne Fehler nach der Umbenennung.
Die Umbenennung wurde vollständig durchgeführt und alle Referenzen im Projekt wurden entsprechend angepasst. Der Service heißt nun offiziell "World Generator Simple Service" und verwendet das Modul world-generator-simple mit der Hauptklasse WorldGeneratorSimpleApplication.
```

## 01 Implementiere den World Generator Simple Service

```text
Erstelle im Modul `registry` einen Registry Service wie in der 
Datei `spec/11_registry.md` beschrieben.

* Erstelle eine SpringBoot RegistryService Klasse, die alle Funktionen des Registry Service implementiert.
* Erstelle Rest-API-Endpunkte und nutze den RegistryService 
  für die Implementierung.
* Erstelle für jeden Endpunkt in `examples/registry.md` ein Beispiel mit curl,
  das die Funktionsweise des Endpunkts demonstriert.
* Erstelle im modul `server-shared` eine Bean Klasse `RegistryServiceClient`, 
  die die Kommunikation mit dem Identity Service ermöglicht.
* Implementiere die Authentifizierung für den Registry Service über die 
  den JWTAuthenticationFilter in `server-shared` analog zum service `identity`.
* Erstelle Unit-Tests für den Registry Service, um die Funktionalität zu überprüfen.

Beachte die Anweisungen in der Datei `spec/02_development.md` und `spec/00_overview.md`.  
```
