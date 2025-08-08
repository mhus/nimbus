
# Überblick

## Einführung

Nimbus ist ein MMORPG System, das es Spielern ermöglicht, in 2D-Welten zu interagieren. 
Es besteht aus mehreren Komponenten, die zusammenarbeiten, um eine dynamische und 
interaktive Umgebung zu schaffen.

Die 2D-Welten sind in mehreren Ebenen übereinander angeordnet, wobei die Ebene mit der 
Nummer 0 die Oberfläche darstellt. Ebenen unter der Oberfläche sind unterirdisch und können
von der Oberfläche aus nicht gesehen werden. Ebenen über der Oberfläche sind
überirdisch und können von der Oberfläche aus gesehen werden.

## Terran Felder

Die Welten bestehen aus Feldern, die in einem Gitter angeordnet sind. Jedes Feld hat
Eigenschaften wie Position, Größe und Inhalt. Felder können verschiedene Eigenschaften haben.

* **position**: Jedes Feld hat eine Position, die durch seine X- und Y-Koordinaten im Gitter definiert ist.
* **level**: Jedes Feld gehört zu einer bestimmten Ebene, die durch eine Ganzzahl definiert ist. 
* **group**: Jedes Feld kann zu einer Gruppe gehören, die durch eine Nummer definiert ist. Gruppen
  haben Eigenschaften, die für alle Felder und Sprites in der Gruppe gelten. Sie werden aktiv sobald man ein Feld 
  dieser Gruppe betritt.
* **material**: Jedes Feld hat ein Material, das seine physikalischen Eigenschaften definiert. 
  Materialien können verschiedene Eigenschaften haben, wie z.B. ob sie durchlässig sind oder nicht.
* **height**: Jedes Feld hat eine Höhe, die seine vertikale Ausdehnung definiert. Die Höhe kann ein
  Feld leicht erhöhen oder senken, um z.B. eine Treppe zu bilden.

## Cluster

Felder sind in genau definierten Clustern organisiert, die eine Gruppe von Feldern darstellen.
Der Cluster ist durch seine Position im Gitter definiert.

```
clusterX = fieldX / clusterSize
clusterY = fieldY / clusterSize
```

## Sprites

Sprites sind grafische Darstellungen von Objekten in der Welt. Sie können über mehrere Felder hinweg
angeordnet sein und haben eine Position, die durch ihre X- und Y-Koordinaten im Gitter definiert ist.

Ein Sprite kann nicht größer als ein Cluster sein, aber es kann durch sine Ausdehnung über mehrere 
Cluster hinausgehen (maximal 4).

Es wird zwischen statischen und dynamischen Sprites unterschieden. Statische Sprites
werden in der Datenbank gespeichert und werden selent verändert. Dynamische Sprites
werden in Echtzeit erstellt und verändert, z.B. durch Spieleraktionen oder Ereignisse in der Welt.

Sprites können verschiedene Eigenschaften haben, wie z.B. ob sie durchlässig sind oder nicht.

* **position**: Jedes Sprite hat eine Position, die durch seine X- und Y-Koordinaten im Gitter definiert ist.
* **level**: Jedes Sprite gehört zu einer bestimmten Ebene, die durch eine Ganzzahl definiert ist. 
* **size**: Jedes Sprite hat eine Größe, die seine Ausdehnung in X- und Y-Richtung definiert.
* **group**: Jedes Sprite kann zu einer Gruppe gehören, die durch eine Nummer definiert ist. 
  Gruppen haben Eigenschaften, die für alle Sprites und Felder in der Gruppe gelten.

## Assets

Assets sind Grafiken, Sounds und andere Ressourcen, die in der Welt verwendet werden.

## Gruppen

Die Gruppen sind eine Möglichkeit, Felder und Sprites zu organisieren und ihnen gemeinsame Eigenschaften zuzuweisen.

## Architektur

Nimbus ist in mehrere Komponenten unterteilt, die jeweils eine bestimmte Funktion erfüllen.

* Es gibt zentrale Service komponenten, die Benutzerdaten, Welten und deren Metadaten verwalten.
* Es gibt Komponenten die die Welten mit Terran und Leben und deren Physik verwalten.
* Es gibt einen zentralen Zugangspunkt für jede Welt (World Bridge), der die Kommunikation zwischen den Clients und den Servern ermöglicht.
* Es gibt eine zentralen Zugangspunkt (Client Bridge) über den die Clients mit den Servern kommunizieren können.
* Es gibt Clients, die die Welten darstellen und es den Spielern ermöglichen, in diesen Welten zu interagieren.
* Es gibt Generatoren, die Welten generieren und deren Metadaten erstellen.

## Kommunikation

* Die Kommunikation zwischen den zentralen Server-Komponenten erfolgt über REST-APIs für Function Calls.
* Die Kommunikation zwischen den Welten-Servern untereinander erfolgt über REST-APIs für Function Calls und Kafka für Events.
* Die Kommunikation zwischen den Clients und der Client Bridge erfolgt über WebSockets für Function Calls und Events.
* Die Kommunikation zwischen den Client Bridge und der World Bridge erfolgt über WebSockets für Function Calls und Events.

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
- **world-data**: Ein World Service, der die Felder und Sprites verwaltet.
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
  - Es speichert dynamische Sprites in einer Redis-Datenbank (shared mit 'world-data').
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
- **generator-simple**: Ein Service, der Welten generiert und deren Metadaten erstellt.
  Er ermöglicht das Erstellen von neuen Welten basierend auf bestimmten Parametern und Regeln.

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
  Sie wird in der Welt-Data-Komponente verwendet, um die Leistung zu verbessern und die Latenz zu reduzieren.

