# Überblick

## Einführung

Nimbus ist ein MMORPG System, das es Spielern ermöglicht, in 2.5D-Welten zu interagieren. 
Es besteht aus mehreren Komponenten, die zusammenarbeiten, um eine dynamische und 
interaktive Umgebung zu schaffen.

Die 2.5D-Welten sind in mehreren Ebenen übereinander angeordnet, wobei die Ebene mit der 
Nummer 0 die Oberfläche darstellt. Ebenen unter der Oberfläche sind unterirdisch und können
von der Oberfläche aus nicht gesehen werden. Ebenen über der Oberfläche sind
überirdisch und können von der Oberfläche aus gesehen werden.

## Darstellung

Die Welten werden in 3D dargestellt, wobei die Spieler in einer 2.5D-Ansicht interagieren. Jedes Feld
wird als Würfel dargestellt, der in der 3D-Welt platziert ist. Es werden nicht immer alle Seiten des Würfels
für die Anzeige benötig, da immer von schräg vorne geschaut wird.

## World 

World Terrain Data bestehen aus Map, Sprites, Assets und Terrain-Gruppen. Die Daten werden in Clustern organisiert, 
um die Performance zu verbessern. Sprites können dynamisch oder statisch sein.

Siehe [World Terrain Data Service](12_world_Terrain.md)

World Life erweckt die Daten zum Leben und simuliert Lebewesen wie Planzen, Tiere 
und NPCs. Er kontrolliert Wachstumsphasen und Interaktion mit allen Lebewesen die
nicht echte Benutzer sind. Benutzer Lebewesen werden hier auch verwaltet aber nicht
deren Lebenszyklus.

Auch Person Character werden hier verwaltet und gespeichert. Person Character sind
die Spielfiguren der Benutzer, die in der Welt interagieren können.

Der World Items Service verwaltet alle Items in der Welt. Items sind Objekte, die in der Welt
existieren können und von Spielern gesammelt, gehandelt oder verwendet werden können. Hier wird auch das Inventar
der Spieler verwaltet, das die Items enthält, die der Spieler besitzt. Auch Coins und andere Währungen werden hier
verwaltet. Alchemie und Crafting sind hier ebenfalls möglich, um neue Items zu erstellen.

World Physics sorgt dafür das alle Lebewesen der gleichen Physik unterlegen. Es
ist das Bindeglied zwischen Data und Life. Physics sorgt auch dafür das
die Parameter für Lebensenergie und Parameteränderungen bei Kämpfen oder das
Auslösen von Effekten richtig funktioniert.

Eine Welt benutzt eine einfachere Methode zur Authentifizierung, die auf einem
einfachen Shared Secret basiert. Das Secret wird jedem Service in den application.properties
bereitgestellt.

## Items

Items sind Objekte, die in der Welt existieren können. Sie können von Spielern gesammelt,
gehandelt oder verwendet werden. Sie können auch verarbeitet werden, um neue Items zu erstellen.

## Architektur

Nimbus ist in mehrere Komponenten unterteilt, die jeweils eine bestimmte Funktion erfüllen.

* Es gibt zentrale Service komponenten, die Benutzerdaten, Welten und deren Metadaten verwalten.
* Es gibt Komponenten die die Welten mit Terrain und Leben und deren Physik verwalten.
* Es gibt einen zentralen Zugangspunkt für jede Welt (World Bridge), der die Kommunikation zwischen den Clients und den Servern ermöglicht.
* Es gibt eine zentralen Zugangspunkt (Client Bridge) über den die Clients mit den Servern kommunizieren können.
* Es gibt Clients, die die Welten darstellen und es den Spielern ermöglichen, in diesen Welten zu interagieren.
* Es gibt Generatoren, die Welten generieren und deren Metadaten erstellen.
* Alle Komponenten sind in Microservices die scalierbar sind und keine Daten lokal speichern (stateless).

## Kommunikation

* Die Kommunikation zwischen den zentralen Server-Komponenten erfolgt über REST-APIs für Function Calls.
* Die Kommunikation zwischen den Welten-Servern untereinander erfolgt über REST-APIs für Function Calls und Kafka für Events.
* Die Kommunikation zwischen den Clients und der Client Bridge erfolgt über WebSockets für Function Calls und Events.
* Die Kommunikation zwischen den Client Bridge und der World Bridge erfolgt über WebSockets für Function Calls und Events. Aussname
  sind offene Resourcen wie WebSocket-Verbindungen, die direkt von der Client Bridge zu den Welten-Servern aufgebaut werden und beim 
  Abbauen erst alle Daten verlieren.

## Komponenten

- **identity**: Ein zentraler Service, der die Identität der Spieler verwaltet.
  Er ermöglicht das Anmelden, Registrieren und Verwalten von Benutzerdaten.
  - Er speichert Benutzerdaten in einer PostgreSQL-Datenbank via JPA.
  - Er bietet REST-APIs für die Kommunikation mit anderen Komponenten.
  - Er vergibt JWT-Tokens für die Authentifizierung der Benutzer.
  - Im JWT-Token sind die Benutzer-ID und die Rollen des Benutzers enthalten.
  - In der Komponente 'server-shared' wird eine Bean für die Valiiderung von JWT-Tokens bereitgestellt.
- **registry**: Ein zentraler Service, der die Welten und deren Metadaten verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Welten und wie diese programatisch erreichbar sind (host/port).
- **world-terrain**: Ein World Service, der die Felder/Map und Sprites verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Welt und deren Felder und Sprites.
  - Er speichert Felder und statische Sprites in einer PostgreSQL-Datenbank via JPA.
  - Er speichert Assets in einer PostgreSQL-Datenbank via JPA.
  - Es liest dynamische Sprites aus einer Redis-Datenbank (shared mit 'world-physics').
  - Alle Felder und Sprites werden in Clustern organisiert, um die Performance zu verbessern.
  - Pro Cluster wird ein BLOB (Binary Large Object) in der Datenbank gespeichert, der die Felder und Sprites enthält als JSON 
    serialisiert.
- **world-life**: Ein World Service, der die Lebesformen in den Welt verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Lebewesen und deren Aktionen.
  - Lebewesen sind alle nicht User-Entitäten in der Welt, die interagieren können. Pflanzen, Tiere und andere NPCs.
  - Er speichert Lebewesen in einer PostgreSQL-Datenbank via JPA.
  - Er verwaltet den Lifecycle von Lebewesen, z.B. das Erstellen, Sterben und Wiederbeleben von Lebewesen.
  - Lebewesen werden über die Komponente 'world-physics' verwaltet, um die Physik und Kollisionen zu berechnen.
- **world-physics**: Ein World Service, der die Physik in den Welt verwaltet.
  Er ermöglicht das Berechnen von Kollisionen, Bewegungen und anderen physikalischen Interaktionen.
  - Speichert die Physik-Daten in einer Redis-Datenbank.
  - Er berechnet die Kollisionen zwischen Feldern, Sprites und Lebewesen.
  - Nur über die Komponente 'world-physics' können Lebewesen und Sprites bewegt werden.
  - Er ermöglicht das Berechnen von Bewegungen und Kollisionen in Echtzeit.
- **world-items**: Ein World Service, der die Items in den Welten verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Items und deren Aktionen.
  - Er speichert Items in einer PostgreSQL-Datenbank via JPA.
  - Er ermöglicht das Sammeln, Handeln und Verwenden von Items in einer Welt.
  - Er ermöglicht das Verarbeiten von Items, um neue Items zu erstellen.
  - Items können Person oder Non Person Character zugeordnet werden, um deren Inventar zu verwalten.
- ***world-bridge**: Ein zentraler Service, der die Kommunikation zwischen der Welt und anderen Komponenten 
  ermöglicht. Er fungiert als Gateway für alle Anfragen von aussen an die Welten-Services.
  - Er verwaltet die WebSocket-Verbindungen und leitet Anfragen an die entsprechenden Welten-Services weiter.
  - Events von Kafka werden hier verarbeitet und an die Clients weitergeleitet.
  - Der Client kann sich an Cluster anmelden, um Updates zu erhalten.
  - Funktionen werden als Text-Kommandos an die Welt gesendet, die dann in der Welt ausgeführt werden.
  - Der Client registriert sich bei der Welt, um Updates zu erhalten.
- **client-bridge**: Ein zentraler Service, der die Kommunikation zwischen den Clients und den Welten-Services ermöglicht.
  Er fungiert als Gateway für alle Anfragen von Clients an die Welten-Services.
  Er verwaltet die WebSocket-Verbindungen und leitet Anfragen an die entsprechenden Welten-Services weiter.
  - Er verwaltet die WebSocket-Verbindungen und leitet Anfragen an die entsprechenden Welten-Bridges weiter.
  - Er ermöglicht das Abfragen von Welten und deren Metadaten.
  - Beim öffnen der WebSocket-Verbindung wird der Client authentifiziert und erhält ein JWT-Token.
  - Der Client kann sich an Welten anmelden, um Updates zu erhalten. Dabei baut die Bridge eine WebSocket-Verbindung zur entsprechenden Welt-Bridge auf.
- **client**: Ein Client, der die Welten rendert. 
  - Er ist eine Webanwendung, die in einem Browser läuft und die Welten in 2D darstellt.
  - Er kommuniziert über WebSockets mit der Client Bridge, um Updates von den Welten zu erhalten.
  - Er ermöglicht es den Spielern, in den Welten zu interagieren, indem sie Aktionen ausführen, wie z.B. das Bewegen von Sprites oder das Interagieren mit Lebewesen.
- **server-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Services bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Services (Client-Klassen), die Verwaltung von Daten und andere Hilfsfunktionen.
- **client-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Clients bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Clients und der Client Bridge, die Verwaltung von Daten und andere Hilfsfunktionen.
- **world-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Welten-Services bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Welten-Services, die Verwaltung von Daten und andere Hilfsfunktionen.
- **shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Services bereitstellt.
  Sie enthält alle DTOs (Data Transfer Objects) und andere gemeinsame Klassen, die von allen Services verwendet werden.
  - Keine dependencies auf andere Services, um eine klare Trennung der Verantwortlichkeiten zu gewährleisten.
  - Keine dependencies auf Kafka oder Spring Boot, um die Bibliothek unabhängig von der Implementierung zu halten.
- **world-generator-simple**: Ein Service, der Welten generiert und deren Metadaten erstellt.
  Er ermöglicht das Erstellen von neuen Welten basierend auf bestimmten Parametern und Regeln.

## Ports und Services

Alle Services im Nimbus-System verwenden spezifische Ports für die REST-API-Kommunikation. Die Ports sind im 
70xx-Bereich konfiguriert, um Konflikte mit anderen Anwendungen zu vermeiden.

### Service Port-Übersicht

| Service | Port | Protokoll | Beschreibung |
|---------|------|-----------|--------------|
| **Identity Service** | 7081 | HTTP/REST | Benutzerauthentifizierung, JWT-Token-Verwaltung |
| **Registry Service** | 7082 | HTTP/REST | Weltenverwaltung und Metadaten |
| **World-Terrain Service** | 7083 | HTTP/REST | Terrain-Daten, Maps und Sprites |
| **World-Bridge Service** | 7089 | WebSocket | Gateway für Weltenzugriff, Client-Kommunikation |

### Entwicklungsumgebung

In der lokalen Entwicklungsumgebung verwenden alle Services `localhost` mit den oben genannten Ports. Für die Produktion können die Hosts entsprechend angepasst werden, während die Ports beibehalten werden können.

### Zusätzliche Infrastruktur-Ports

Neben den Service-Ports werden weitere Ports für die Infrastruktur verwendet:

| Service | Port | Beschreibung |
|---------|------|--------------|
| PostgreSQL | 5432 | Hauptdatenbank für persistente Daten |
| Redis | 6379 | In-Memory-Datenbank für dynamische Sprites |
| Kafka | 9092 | Event-Streaming zwischen World-Services |

Diese Infrastruktur-Ports sind Standard-Ports der jeweiligen Technologien und können bei Bedarf in der `docker-compose.local.yml` angepasst werden.

## Technologie-Stack

- **Java**: Die Hauptprogrammiersprache für alle Server- und Client-Komponenten. 
  Sie wird verwendet, um die Logik der Welten, die Kommunikation zwischen den Komponenten und die Verwaltung von Daten zu implementieren.
  - Java 21 wird verwendet, um die neuesten Funktionen und Verbesserungen der Sprache zu nutzen.
  - GraalVM wird verwendet, um die Leistung der Java-Anwendungen zu verbessern und die Startzeit zu verkürzen.
- **Spring Boot**: Ein Java-Framework, das die Entwicklung von Microservices erleichtert. 
  Es bietet eine einfache Möglichkeit, REST-APIs zu erstellen und zu verwalten. Es ist die
  Grundlage für alle Server-Services.
  - Spring Boot dependencies werden in den jeweiligen Services verwendet, um die Funktionalität zu erweitern.
  - Spring Boot Web wird verwendet um den Client als Webanwendung bereitzustellen.
  - Spring Boot Web wird verwendet, um die REST-APIs für die Kommunikation zwischen den Komponenten zu implementieren.
  - Spring Boot Kafka wird verwendet, um die Kommunikation (Events) zwischen den Welten-Services zu ermöglichen.
  - Spring Boot Data JPA wird verwendet, um die Datenbankzugriffe zu vereinfachen und zu optimieren.
  - Spring Boot Security wird verwendet, um die Sicherheit der Services zu gewährleisten.
- **WebSockets**: Ein Protokoll, das eine bidirektionale Kommunikation zwischen Client und Server ermöglicht.
  Es wird verwendet, um Echtzeit-Updates an den Client zu senden, wenn sich die Welt ändert.
  - Es wird Spring Boot WebSockets verwendet, um die WebSocket-Verbindungen zu verwalten.
- **Lombok**: Eine Java-Bibliothek, die die Entwicklung von Java-Klassen vereinfacht, indem sie Boilerplate-Code reduziert.
  Sie wird in allen Server- und Client-Komponenten verwendet, um die Codebasis sauberer und wartbarer zu halten.
- **Kafka**: Ein verteiltes Streaming-System, das es ermöglicht, Ereignisse zwischen den Komponenten zu senden.
  Es wird verwendet, um Ereignisse zwischen den Welten-Services zu senden, z.B. wenn sich ein Feld ändert oder ein Lebewesen stirbt.
  - Messages werden als JSON serialisiert und über Kafka gesendet.
  - Es wird Spring Boot Kafka verwendet, um die Kommunikation zwischen den Welten-Services zu ermöglichen.
- **PostgreSQL**: Eine relationale Datenbank, die für die Speicherung von Benutzerdaten, Welten und deren Metadaten verwendet wird.
  Sie wird in den zentralen Services wie Identity und Registry verwendet, um Daten persistent zu speichern.
- **Redis**: Eine In-Memory-Datenbank, die für die Speicherung von dynamischen Sprites verwendet wird.
  Sie wird in der Welt-Terrain-Data-Komponente verwendet, um die Leistung zu verbessern und die Latenz zu reduzieren.
