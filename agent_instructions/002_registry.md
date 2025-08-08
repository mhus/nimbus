
# Registry Service

## 01 Implementiere den Registry Service

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

