
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

Sprites können verschiedene Eigenschaften haben, wie z.B. ob sie durchlässig sind oder nicht.

* **position**: Jedes Sprite hat eine Position, die durch seine X- und Y-Koordinaten im Gitter definiert ist.
* **level**: Jedes Sprite gehört zu einer bestimmten Ebene, die durch eine Ganzzahl definiert ist. 
* **size**: Jedes Sprite hat eine Größe, die seine Ausdehnung in X- und Y-Richtung definiert.
* **group**: Jedes Sprite kann zu einer Gruppe gehören, die durch eine Nummer definiert ist. 
  Gruppen haben Eigenschaften, die für alle Sprites und Felder in der Gruppe gelten.

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
- **registry**: Ein zentraler Service, der die Welten und deren Metadaten verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Welten und wie diese programatisch erreichbar sind (host/port).
- **world-terran**: Ein World Service, der die Felder und Sprites verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Welt und deren Felder.
- **world-life**: Ein World Service, der die Lebesformen in den Welt verwaltet.
  Er ermöglicht das Erstellen, Löschen und Abfragen von Lebewesen und deren Aktionen.
- **world-physics**: Ein World Service, der die Physik in den Welt verwaltet.
  Er ermöglicht das Berechnen von Kollisionen, Bewegungen und anderen physikalischen Interaktionen.
- ***world-bridge**: Ein zentraler Service, der die Kommunikation zwischen der Welt und anderen Komponenten 
  ermöglicht. Er fungiert als Gateway für alle Anfragen von aussen an die Welten-Services.
- **client-bridge**: Ein zentraler Service, der die Kommunikation zwischen den Clients und den Welten-Services ermöglicht.
  Er fungiert als Gateway für alle Anfragen von Clients an die Welten-Services.
  Er verwaltet die WebSocket-Verbindungen und leitet Anfragen an die entsprechenden Welten-Services weiter.
- **client**: Ein Client, der die Welten rendert. 
- **server-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Services bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Services (Client-Klassen), die Verwaltung von Daten und andere Hilfsfunktionen.
- **client-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Clients bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Clients und der Client Bridge, die Verwaltung von Daten und andere Hilfsfunktionen.
- **world-shared**: Eine Java-Bibliothek, die gemeinsame Funktionen und Datenstrukturen für alle Welten-Services bereitstellt.
  Sie enthält Klassen für die Kommunikation zwischen den Welten-Services, die Verwaltung von Daten und andere Hilfsfunktionen.
- **generator**: Ein Service, der Welten generiert und deren Metadaten erstellt.
  Er ermöglicht das Erstellen von neuen Welten basierend auf bestimmten Parametern und Regeln.

## Technologie-Stack

- **Spring Boot**: Ein Java-Framework, das die Entwicklung von Microservices erleichtert. 
  Es bietet eine einfache Möglichkeit, REST-APIs zu erstellen und zu verwalten. Es ist die
  Grundlage für alle Server-Services.

- **WebSockets**: Ein Protokoll, das eine bidirektionale Kommunikation zwischen Client und Server ermöglicht.
  Es wird verwendet, um Echtzeit-Updates an den Client zu senden, wenn sich die Welt ändert.



