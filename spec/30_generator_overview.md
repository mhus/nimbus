# World Generatoren

## Einführung

Welt-Generatoren sind verantwortlich für die Erstellung des 
anfänglichen Welt-Layouts, einschließlich Terrain, Strukturen,
Pflanzen und Tieren, Items und Quests. Sie verwenden
verschiedene Parameter wie Größe, Biome, Ressourcenverteilung
und andere Einstellungen, um eine vielfältige und interessante
Welt zu generieren.

Die Generierung erfolgt in mehreren Schritten und ist in der Regel
nicht sofort abgeschlossen. Stattdessen wird die Welt in
Phasen generiert, wobei jede Phase bestimmte Aspekte der Welt
behandelt. Dies ermöglicht eine bessere Kontrolle über den
Generierungsprozess und die Möglichkeit, Anpassungen vorzunehmen,
sobald bestimmte Teile der Welt generiert wurden.

Entscheidungen über die Generierung können auf verschiedenen
Ebenen getroffen werden, einschließlich der Auswahl von Biomen,
der Platzierung von Strukturen und der Verteilung von Ressourcen.
Die Generierung kann auch auf bestimmte Regionen oder Zonen
fokussiert werden, um eine größere Vielfalt und Tiefe in der
Welt zu schaffen.

Die Welt-Generatoren sind in der Regel modular aufgebaut,
sodass verschiedene Komponenten unabhängig voneinander
entwickelt und getestet werden können. Dies ermöglicht eine
flexible Anpassung der Generierung und die Möglichkeit,
neue Features oder Änderungen an bestehenden Komponenten
einzuführen, ohne die gesamte Generierung zu beeinträchtigen.
Die Generierung kann auch auf verschiedene Arten von Welten
angepasst werden, einschließlich flacher Welten, komplexer
Welten oder sogar Welten mit speziellen Regeln oder
Einstellungen.

Parameter die zur weiterentwicklung der Welt benötigt werden,
werden in der Datenbank gespeichert damit der Prozess später
fortgesetzt werden kann. Dies ermöglicht eine kontinuierliche
Entwicklung der Welt und die Möglichkeit, neue Features
oder Änderungen an bestehenden Komponenten einzuführen,
ohne die gesamte Generierung zu beeinträchtigen. Die Datenbank
kann auch verwendet werden, um den Fortschritt der Generierung
zu verfolgen und sicherzustellen, dass alle Teile der Welt
erfolgreich generiert wurden.

Persistierte Daten können auch als Basis für weitere Phasen
der Generierung dienen, um eine konsistente und zusammenhängende
Welt zu schaffen.


## Generator-Phasen

Die Generierung der Welt erfolgt in mehreren Phasen, die jeweils
spezifische Aspekte der Welt behandeln. Jede Phase kann
unabhängig voneinander entwickelt und getestet werden, was
eine flexible Anpassung der Generierung ermöglicht. Die Phasen
können auch in verschiedenen Reihenfolgen ausgeführt werden,
abhängig von den spezifischen Anforderungen der Welt und den
gewünschten Ergebnissen.    
Die Phasen der Generierung können Folgendes umfassen:
- **Initialisierung**: Legt die grundlegenden Parameter der Welt fest, einschließlich Größe, Biome, Ressourcenverteilung und andere Einstellungen. Diese Phase kann auch die Festlegung von Regeln und Einschränkungen für die Generierung umfassen.
- **Asset/Material-Generierung**: Erstellt die grundlegenden Assets der Welt, einschließlich Texturen, Modelle und andere visuelle Elemente. Diese Phase kann auch die Erstellung von Soundeffekten und Musik umfassen, die in der Welt verwendet werden.
- **Kontinent-Generierung**: Erstellt die grundlegenden Kontinente und Ozeane der Welt, einschließlich der Platzierung von Landmassen, Inseln und anderen geografischen Merkmalen. Diese Phase kann auch die Festlegung von Klimazonen und Wetterbedingungen umfassen.
- **Terrain-Generierung**: Erzeugt die grundlegende Landschaft der Welt, einschließlich Berge, Täler, Flüsse und Seen. Diese Phase kann auch die Platzierung von Biomen und anderen natürlichen Merkmalen umfassen.
- **Historische Generierung**: Erstellt die Geschichte und Hintergrundinformationen der Welt, einschließlich wichtiger Ereignisse, Charaktere und Orte. Diese Phase kann auch die Festlegung von Regeln und Einschränkungen für die Welt umfassen, die das Verhalten von Spielern und NPCs beeinflussen.
- **Struktur-Generierung**: Platziert Gebäude, Dörfer, Ruinen und andere Strukturen in der Welt. Diese Phase kann auch die Platzierung von Ressourcen wie Erzen, Pflanzen und Tieren umfassen.
- **Item-Generierung**: Erstellt Gegenstände, die in der Welt gefunden oder 
  verwendet werden können, einschließlich Waffen, Rüstungen, Werkzeuge und andere nützliche Objekte. Diese Phase kann auch die Platzierung von Quests und anderen Aktivitäten umfassen.
- **Quest-Generierung**: Erstellt Quests und Aufgaben, die die Spieler in der Welt erfüllen können. Diese Phase kann auch die Platzierung von NPCs (Nicht-Spieler-Charakteren) umfassen, die den Spielern helfen oder sie herausfordern können.

Die Phasen sind nicht festgelegt und können je nach den spezifischen Anforderungen
der Welt angepasst werden. Die Phasen werden separat entwickelt und ausgeführt,
können aber aufeinander aufbauen.

Phasen können auch dafür benutzt werden nur kleine Strukturen zu generieren, 
die dann in der Welt platziert werden.

## General Mechaniken

Um grundlegende Mechaniken vorzubereiten wird eine Bean mit vorgefertigten
Keys und Methoden erstellt, die in der Welt-Generator Phase
verwendet werden können. Diese Mechaniken können verwendet werden,
um die Zusammenarbeit bzw. grundlegende Interaktion zwischen den
verschiedenen Phasen zu ermöglichen.

Die DTO Struntkur wird im WorldGenerator als Parameter
gespeichert.

Aspekte bei der Initiierung der Welt-Generator Phase sind:
- **Thema**: Das Thema der Welt, z.B. Fantasy, Sci-Fi, Historisch.

TBD

## Generator JPA Enitäten

```text
* WorldGenerator
  - id: Long
  - name: String
  - description: String
  - status: String (e.g., "INITIALIZED", "GENERATING", "COMPLETED")
  - createdAt: Date
  - updatedAt: Date
  - inputParameters: JSON Data (to store the input for the phase)
  - parameters: JSON Data
```

```text
* WorldGeneratorPhase
    - id: Long
    - worldGenerator: Long
    - processor: String 
    - order: Integer (to define the order of execution)
    - name: String
    - archived: Boolean (to indicate if the phase is archived)
    - description: String
    - status: String (e.g., "PENDING", "IN_PROGRESS", "COM
PLETED")
    - createdAt: Date
    - updatedAt: Date
    - parameters: JSON Data (working parameters for the phase)
    - inputParameters: JSON Data (to store the input for the phase)
    - outputParameters: JSON Data (to store the output of the phase)
    - phaseOrder: Integer (to define the order of execution)
```

## Erweiterungsfähigkeit

Die Welt-Generatoren sind so konzipiert, dass sie leicht
erweitert werden können. Neue Phasen Processoren können
hinzugefügt werden, um zusätzliche Aspekte der Welt
zu generieren.

Erweiterungen werden separat spezifiziert und dann in
Beans implementiert die das Phase-Processor Interface 
implementieren.

## API Endpunkte

### World initialisieren

**POST /worlds/generators**

Role: `CREATOR`

Erstellt einen neuen Welt-Generator. Die ID wird automatisch generiert.
```json
{
  "name": "Fantasy World",
  "description": "A magical world filled with adventures.",
  "worldId": "123e4567-e89b-12d3-a456-426614174000",
  "size": "large",
  "inputParameters": {
  }
}
```

### World-Generator Phasen anlegen

**POST /worlds/generators/{id}/phase**

Role: `CREATOR`

Startet die Generierung einer Welt. Die ID des Welt-Generators wird verwendet, um die Generierung zu starten.
```json
{
  "name": "Fantasy World Phase 1",
  "generator": "terrain",
  "description": "Initial phase of the fantasy world generation.",
  "inputParameters": {
    "terrainType": "mountainous"
  }
}
```

### World-Generator Phasen starten

**POST /worlds/generators/{id}/phase/{phaseId}/start**

Role: `ADMIN` oder owner der Welt.

Startet eine Welt-Generator Phase. Die ID der Welt-Generator Phase muss in der URL angegeben werden.
Die Phase wird in den Status "IN_PROGRESS" versetzt und die Generierung beginnt.

### World-Generator Phasen aktualisieren

**PUT /worlds/generators/{id}/phase/{phaseId}**

Role: `ADMIN` oder owner der Welt.

Aktualisiert die Metadaten einer bestehenden Welt-Generator Phase. Die ID der Welt-Generator Phase muss in der URL angegeben werden.
```json
{
  "name": "Fantasy World Phase 1 Updated",
  "description": "Updated phase of the fantasy world generation.",
  "status": "IN_PROGRESS",
  "inputParameters": {
    "terrainType": "hilly"
  },
  "parameters": {
    "terrainType": "hilly"
  },
  "outputParameters": {
    "terrainType": "hilly"
  }
}
```

### World-Generator Phasen abfragen

**GET /worlds/generators/{id}/phase**

Role: `USER`.

Fragt eine Welt-Generator Phase anhand ihrer ID ab. Gibt die Metadaten der Welt-Generator Phase zurück.

## World-Generator Phasen auflisten

**GET /worlds/generators/{id}/phase**

Role: `USER`.

Listet alle verfügbaren Welt-Generator Phasen auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Welt-Generator Phasen pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### World-Generator Phasen löschen

**DELETE /worlds/generators/{id}/phase/{phaseId}**

Role: `ADMIN` oder owner der Welt.

Löscht eine bestehende Welt-Generator Phase. Die ID der Welt-Generator Phase muss in der URL angegeben werden.
Die Welt-Generator Phase wird aus der Datenbank entfernt und kann nicht wiederhergestellt werden.

### World-Generator Phasen archivieren

**POST /worlds/generators/{id}/phase/{phaseId}/archive**

Role: `ADMIN` oder owner der Welt.

Archviert eine bestehende Welt-Generator Phase. Die ID der Welt-Generator Phase muss in der URL angegeben werden.
Die Welt-Generator Phase wird aus der aktiven Liste entfernt, aber nicht aus der Datenbank
entfernt. 

### World-Generator auflisten

**GET /worlds/generators**

Role: `USER`.

Listet alle verfügbaren Welt-Generatoren auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Welt-Generatoren pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### World-Generator abfragen

**GET /worlds/generators/{id}**

Role: `USER`.

Fragt einen Welt-Generator anhand seiner ID ab. Gibt die Metadaten des Welt-Generators zurück.

### World-Generator aktualisieren

**PUT /worlds/generators/{id}**

Role: `ADMIN` oder owner der Welt.

Aktualisiert die Metadaten eines bestehenden Welt-Generators. Die ID des Welt-Generators muss in der URL angegeben werden.

### World-Generator löschen

**DELETE /worlds/generators/{id}**

Role: `ADMIN` oder owner der Welt.

Löscht einen bestehenden Welt-Generator. Die ID des Welt-Generators muss in der URL angegeben werden.
Es werden alle Phasen des Welt-Generators gelöscht und der Welt-Generator wird aus der Datenbank entfernt.
Die Welt-Generator kann nicht wiederhergestellt werden. 

