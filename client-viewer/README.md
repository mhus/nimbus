# Nimbus Client Viewer

Ein grafisches Frontend für den Nimbus Client basierend auf LWJGL (Lightweight Java Game Library).

## Funktionen

- **Grafische Benutzeroberfläche**: Moderne UI mit LWJGL und NanoVG
- **Server-Verbindung**: Verbindung zum Nimbus Entrance Server über WebSocket
- **Authentifizierung**: Benutzeranmeldung mit Username/Passwort
- **Funktionsaufrufe**: Aufruf von Server-Funktionen über die grafische Oberfläche
- **Plattformübergreifend**: Läuft auf Windows, macOS und Linux

## Technische Details

### Verwendete Bibliotheken
- **LWJGL 3.3.3**: Core-Bibliothek für OpenGL, GLFW und NanoVG
- **NanoVG**: Vektorgrafik-Bibliothek für die UI-Rendering
- **Client-Common**: Nimbus Client Service für Server-Kommunikation

### Systemanforderungen
- Java 21 oder höher
- OpenGL 3.3 kompatible Grafikkarte
- Mindestens 512 MB RAM

## Verwendung

### Starten der Anwendung
```bash
mvn spring-boot:run
```

### Bedienung
1. **Verbindungsbildschirm**: Server-URL eingeben und verbinden
2. **Anmeldebildschirm**: Benutzername und Passwort eingeben
3. **Hauptansicht**: Server-Funktionen aufrufen und Nachrichten anzeigen

### Tastatursteuerung
- `Tab`: Zwischen Eingabefeldern wechseln
- `Enter`: Aktuelle Aktion ausführen (Verbinden/Anmelden)
- `Escape`: Anwendung beenden

## Architektur

### Klassen-Übersicht
- `NimbusViewerApplication`: Hauptklasse der Anwendung
- `ViewerWindow`: LWJGL-Fenster und Event-Handling
- `UserInterface`: UI-Rendering mit NanoVG

### Integration mit client-common
Das Modul nutzt den `NimbusClientService` aus dem client-common Modul für:
- WebSocket-Verbindungen zum Server
- Authentifizierung
- Funktionsaufrufe
- Nachrichtenaustausch

## Entwicklung

### Build
```bash
mvn clean compile
```

### Tests
```bash
mvn test
```

### Packaging
```bash
mvn package
```

## Plattform-spezifische Natives

Das Projekt ist so konfiguriert, dass es automatisch die korrekten LWJGL-Natives für die jeweilige Plattform verwendet:
- **macOS**: natives-macos / natives-macos-arm64
- **Linux**: natives-linux
- **Windows**: natives-windows

## Konfiguration

### Server-URL
Standard: `ws://localhost:8080/nimbus`

Die Server-URL kann in der grafischen Oberfläche geändert werden.

### Logging
Das Modul verwendet SLF4J mit Logback für Logging. Log-Level können über die Standard Spring Boot Konfiguration angepasst werden.

## Troubleshooting

### Häufige Probleme

**Problem**: Schwarzer Bildschirm beim Start
**Lösung**: Überprüfen Sie, ob Ihre Grafikkarte OpenGL 3.3 unterstützt

**Problem**: Verbindung zum Server schlägt fehl
**Lösung**: Überprüfen Sie die Server-URL und stellen Sie sicher, dass der Nimbus Entrance Server läuft

**Problem**: Native Bibliotheken nicht gefunden
**Lösung**: Führen Sie `mvn clean install` aus, um die plattformspezifischen Natives zu installieren
