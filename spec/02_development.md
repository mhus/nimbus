
# Entwicklung 

## Maven

Verwende Maven als Build-Tool für das Projekt. Stelle sicher, dass die `pom.xml`-Datei
alle Abhängigkeiten enthält, die für die Entwicklung und den Betrieb der Anwendung erforderlich sind.

In der root pom.xml sollten die Module definiert werden und alle Abhängigkeiten mit Version 
als `dependencyManagement`-Eintrag verwaltet werden. In den Modulen sollten die
Abhängigkeiten ohne Versionsangabe verwendet werden, um die Versionen aus der root pom.xml zu übernehmen.

## Java Version

Verwende Java 21 als Mindestversion für die Entwicklung.

## Rest API

Erstelle für jeden Rest-API-Endpunkt ein Beispiel in der Datei `examples/rest_api_<controller>.md`.'

Erstelle in den Projekten mit `shared` im Namen Client für die Rest-API, die die 
Endpunkte aufrufen und die Antworten verarbeiten. Diese Clients sollten in der Lage sein, 
Anfragen zu senden und Antworten zu empfangen.

Die DTOs (Data Transfer Objects) für die Rest-API sollten in den Projekten mit `shared` im 
Namen erstellt werden. Diese DTOs sollten die Struktur der Anfragen und Antworten definieren.
Client und Server sollten die gleichen DTOs verwenden, um die Kommunikation zu vereinfachen.

## WebSocket API

Erstelle für jeden WebSocket-Befehl ein Beispiel in der Datei `examples/websocket_<command>.md`.

## Lombok

Verwende Lombok, um Boilerplate-Code zu vermeiden.

## Shared Libraries

Erstelle in den Komponenten mit `shared` im Namen eine gemeinsame Bibliothek, die von 
den anderen Komponenten verwendet werden kann. Diese Bibliothek sollte gemeinsame Klassen, 
Interfaces, DTOs, POJOs und Utilities enthalten, die von mehreren Komponenten benötigt werden.

## Unit-Tests

Erstelle für jeden Controller und Service Unit-Tests. Verwende JUnit 5 und Mockito.

## Integrationstests

Erstelle Integrationstests für die Rest-API und WebSocket-Kommandos. Verwende Spring Boot Test und MockMvc für die Rest-API-Tests. Für WebSocket-Tests kannst 
du den `WebSocketTestClient` verwenden, um Nachrichten zu senden und zu empfangen.

## Lokale Entwicklung

Erstelle eine lokale Entwicklungsumgebung mit Docker. Verwende Docker Compose, um die benötigten Dienste (Datenbank, Redis, etc.) zu starten. Stelle sicher, dass die Anwendung in der lokalen Umgebung läuft und alle Tests erfolgreich sind.

## Dokumentation

Erstelle eine Dokumentation im Markdown-Format im Ordner `docs/`. Diese Dokumentation sollte die Architektur, die API-Spezifikation, die WebSocket-Kommandos und die Entwicklungshinweise enthalten. Verwende die bereits vorhandenen Dateien als Vorlage.

## Journal

Erstelle ein Journal im Ordner `journal/`, in dem alle Änderungen, neuen Features und Bugfixes dokumentiert werden. 
Dieses Journal sollte bei jedem Schritt eine neue Datei im Format `YYYY-MM-DD_HH-mm.md` enthalten.

## Code Style

Schreibe für jede Funktion und Klasse einen Javadoc-Kommentar, der die Funktionalität beschreibt. 

## Security

Alle Endpunkte der Rest-API sollten durch Authentifizierung und Autorisierung geschützt sein.
Dabei wird der JWT-Token als Bearer-Token in den HTTP-Headern verwendet.
Das Benutzer-Objekt und die Rollen werden im Http-Request gespeichert, um sie in den Controllern und 
Services verwenden zu können.

Nutze die Klasse `AuthorizationUtil` aus dem `server-shared` Projekt, um die 
Autorisierung durch Rollen zu überprüfen.
