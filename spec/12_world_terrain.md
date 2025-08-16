# World Terrain Service Spezifikation

## Einleitung

Der World Terrain Service verwaltet alle relevanten Daten über die Welt,
einschließlich Map, Sprites, Assets, Materials und Terrain-Gruppen.

Auch Items werden im World Terrain Service verwaltet. Sie sind aber nicht
immer in Teil der Welt.

## Cluster

Das Terrain ist in genau definierten Clustern organisiert, die eine Gruppe von Feldern darstellen.
Der Cluster ist durch seine Position im Gitter definiert. Ein Cluster-Feld beinhaltet
alle Dateien die indesem Cluster liegen.

Die ClusterSize ist in einer Constants definiert und beträgt 32x32 Felder.

```
clusterX = fieldX / clusterSize
clusterY = fieldY / clusterSize
```

## World

Welten sind die grundlegenden Einheiten des World Terrain Services. Sie repräsentieren
eine virtuelle Umgebung, in der Felder, Sprites und andere Objekte existieren können.

### Welt-JPA-Entity

```text
"id": "string", // Eindeutige ID der Welt (UUID) (primary key
"createdAt": "timestamp", // Erstellungszeitpunkt der Welt
"updatedAt": "timestamp", // Aktualisierungszeitpunkt der Welt
"name": "string", // Name der Welt (z.B. "Earth", "Mars")
"description": "string", // Beschreibung der Welt
"sizeX": "int", // Größe der Welt in X-Richtung
"sizeY": "int", // Größe der Welt in Y-Richtung
"properties": "blob" // JSON-String der Eigenschaften der Welt
```

## Materialien

Materialien definieren die physikalischen Eigenschaften von Feldern und Sprites. Sie können verschiedene
Eigenschaften haben, wie z.B. ob sie durchlässig sind oder nicht.

### Material-Datenmodell

```json
{
  "id": "int", // Eindeutige ID des Materials
  "name": "string", // Name des Materials (z.B. "grass", "water")
  "blocking": "boolean", // Ob das Material durchlässig ist oder nicht
  "friction": "float", // Reibungskoeffizient des Materials
  "color": "string", // Farbe des Materials (z.B. "#00FF00" für grün) - Grundfarbe
  "texture": "string", // Textur des Materials (z.B. "grass.png")
  "sound_walk": "string", // Sound des Materials (z.B. "grass.wav") beim Betreten
  "properties": {
    "key": "value" // Key-Value Werte der Eigenschaften als 'map of string'
  }
}
```

## Terrain

Die Welten bestehen aus Feldern, die in einem Gitter angeordnet sind. Jedes Feld hat
Eigenschaften wie Position, Größe und Inhalt. Felder können verschiedene Eigenschaften haben.

* **position**: Jedes Feld hat eine Position, die durch seine X- und Y-Koordinaten im Gitter definiert ist.
* **level**: Jedes Feld gehört zu einer bestimmten Ebene, die durch eine Ganzzahl definiert ist.
* **groups**: Jedes Feld kann zu Terrain-Gruppen gehören, die durch IDs (Long) definiert ist. Gruppen
  haben Eigenschaften, die für alle Felder und Sprites in der Gruppe gelten.
* **material**: Jedes Feld hat ein Material, das seine physikalischen Eigenschaften definiert.
  Materialien können verschiedene Eigenschaften haben, wie z.B. ob sie durchlässig sind oder nicht.
  Jedes Feld ist ein Würfel, der für jede Seite ein anderes Material haben kann.
* **height**: Jedes Feld hat eine Höhe, die seine vertikale Ausdehnung definiert. Die Höhe kann ein
  Feld leicht erhöhen oder senken, um z.B. eine Treppe zu bilden.
* **zsize**: Jedes Feld hat eine Größe, die seine Ausdehnung in Z-Richtung nach unten definiert.
* **parameters**: Jedes Feld kann zusätzliche Parameter haben, die seine Eigenschaften erweitern.
  Diese Parameter sind in einem JSON-Format gespeichert und können beliebige Schlüssel-Wert-Paare enthalten.

### Map-Datenmodell

```json
{
  "x": "int", // X-Koordinate im Gitter
  "y": "int", // Y-Koordinate im Gitter
  "z": "int",  // Z-Koordinate im Gitter (optional, für 3D-Felder), default 0
  "groups": ["long"], // IDs der Terrain-Gruppen, zu denen das Feld gehört
  "materials": ["int"], // Material des Feldes (z.B. "grass", "water"), index: 0 oben, 1-4: seitliche Materialschichten, 5 unten
  "opacity": "int", // Opazität des Feldes (0-255), 0 = transparent, 255 = undurchsichtig
  "sizeZ": "int", // Größe des Feldes in Z-Richtung nach unten, default 1
  "parameters": {
      "key": "value" // Key-Value Werte der Parameter als 'map of string'
  }
}
```

Array aller Felder eines Cluster-Feldes. Es werden immer alle Felder eines Clusters
in einem Cluster-Feld gespeichert.

## Map-JPA-Entity

```text
"world:"string", // ID der Welt, zu der das Cluster-Feld gehört
"level": "int", // Ebene des Feldes
"clusterX": "int", // X-Position des Clusters im Gitter
"clusterY": "int", // Y-Position des Clusters im Gitter
"data": "blob", // JSON-Daten des Cluster-Feldes
"compressed" : "blob" // Komprimierte Daten des Cluster-Feldes
"createdAt": "timestamp", // Erstellungszeitpunkt des Cluster-Feldes
"updatedAt": "timestamp" // Aktualisierungszeitpunkt des Cluster-Feldes (nicht bei compressed)
"compressetAt": "timestamp" // Komprimierungszeitpunkt des Cluster-Feldes
```

Auf die Felder `world`, `level`, `clusterX` und `clusterY` muss ein unique Index gesetzt werden.

Beim Speichern der Map-Daten wird ein Cluster-Feld erstellt, das alle Felder eines Clusters enthält.
Die Felder werden in einem JSON-Format gespeichert, das die Eigenschaften der Felder enthält.
Beim Ändern der Felder wird `compressed` auf `null` gesetzt. Ein Job comprimiert die Daten
später und speichert sie in `compressed`.

## Sprites

Sprites sind grafische Darstellungen von Objekten in der Welt. Sie können über mehrere Felder hinweg
angeordnet sein und haben eine Position, die durch ihre X- und Y-Koordinaten im Gitter definiert ist.

Ein Sprite kann nicht größer als ein Cluster sein, aber es kann durch sine Ausdehnung über mehrere
Cluster hinausgehen (maximal 4). In der Datenbank ist markiert ob ein Sprite in weiteren
Cluster-Feldern liegt. 

Sprites haben oft eine Referenz zu einem Item oder einem Lebewesen, das sie repräsentieren. Um das
entsprechende Item oder Lebewesen zu finden, wird die Referenz am Sprite gespeichert.

Es wird zwischen statischen und dynamischen Sprites unterschieden. Statische Sprites
werden in der Datenbank gespeichert und werden selten verändert. Dynamische Sprites
werden in Echtzeit erstellt und verändert, z.B. durch Spieleraktionen oder Ereignisse in der Welt.

Sprites bestehen aus einem 3D Gitter.

* **position**: Jedes Sprite hat eine Position, die durch seine X- und Y-Koordinaten im Gitter definiert ist.
* **level**: Jedes Sprite gehört zu einer bestimmten Ebene, die durch eine Ganzzahl definiert ist.
* **size**: Jedes Sprite hat eine Größe, die seine Ausdehnung in X- und Y-Richtung definiert.
* **groups**: Jedes Sprite kann zu Terrain-Gruppen gehören, die durch eine ID (Long) definiert ist.
  Gruppen haben Eigenschaften, die für alle Sprites und Felder in der Gruppe gelten.
* **zsize**: Int. Jedes Sprite hat eine Größe, die seine Ausdehnung in Z-Richtung nach unten definiert.
* **rasterType**: String. Der Typ der Raster-Darstellung des Sprites (z.B. "png", "jpg").
* **raster**: Blob. Darstellung des Sprites.
* **type**: String. Jedes Sprite hat einen Typ, der seine Funktion in der Welt definiert (z.B. "tree", "rock").
* **parameters**: Jedes Sprite kann zusätzliche Parameter haben, die seine Eigenschaften erweitern.
  Diese Parameter sind in einem JSON-Format gespeichert und können beliebige Schlüssel-Wert-Paare enthalten.
* **reference**: Jedes Sprite kann eine Referenz zu einem Item oder einem Lebewesen haben, das es repräsentiert.

### Sprite ID

Die ID ist immer eine UUID mit einem Prefix.

Prefix:
* "S" für Static: Wird via JPA in der Datenbank gespeichert.
* "D" für Dynamic: Wird in Redis gespeichert.

### Sprite-Datenmodell

```json
{
  "x": "int", // X-Koordinate im Gitter
  "y": "int", // Y-Koordinate im Gitter
  "z": "int", // Z-Koordinate im Gitter (optional, für 3D-Sprites), default 0
  "sizeX": "int", // Größe des Sprites in X-Richtung
  "sizeY": "int", // Größe des Sprites in Y-Richtung
  "sizeZ": "int", // Größe des Sprites in Z-Richtung nach unten, default 1
  "groups": ["long"], // IDs der Terrain-Gruppen, zu denen das Sprite gehört
  "reference": "String", // Referenz zu einem Item oder Lebewesen, das das Sprite repräsentiert
  "parameters": {
      "key": "value" // Key-Value Werte der Parameter als 'map of string'
  },
  "rasterType": "string", // Typ der Raster-Darstellung (z.B. "png", "jpg")
  "raster": "byte[]", // Darstellung des Sprites als Byte-Array
  "type": "string", // Typ des Sprites (z.B. "tree", "rock")
  "blocking": "boolean" // Ob das Sprite durchlässig ist oder nicht
  "opacity": "int" // Opazität des Sprites (0-255), 0 = transparent, 255 = undurchsichtig
}
```

Sprites werden immer einzeln in der Datenbank oder Redis gespeichert.

### Sprite-JPA-Entity

```text
"id": "string", // Eindeutige ID des Sprites ("S" + UUID) (primary key)
"world:"string", // ID der Welt, zu der das Cluster-Feld gehört
"level": "int", // Ebene des Sprites
"enabled" : "boolean", // Ob das Sprite aktiv ist
"clusterX0": "int", // X-Position des Clusters im ersten Gitter (main x)
"clusterY0": "int", // Y-Position des Clusters im ersten Gitter (main y)
"clusterX1": "int", // X-Position des Clusters im zweiten Gitter (rechts) oder null
"clusterY1": "int", // Y-Position des Clusters im zweiten Gitter (rechts) oder null
"clusterX2": "int", // X-Position des Clusters im dritten Gitter (unten) oder null
"clusterY2": "int", // Y-Position des Clusters im dritten Gitter (unten) oder null
"clusterX3": "int", // X-Position des Clusters im vierten Gitter (unten rechts) oder null
"clusterY3": "int", // Y-Position des Clusters im vierten Gitter (unten rechts) oder null
"data": "blob", // JSON-Daten des Sprite
"compressed" : "blob" // Komprimierte Daten des Sprites
"createdAt": "timestamp", // Erstellungszeitpunkt des Sprites
"updatedAt": "timestamp" // Aktualisierungszeitpunkt des Sprites (nicht bei compressed)
"compressetAt": "timestamp" // Komprimierungszeitpunkt des Sprites
```

Auf die Felder `world`, `level`, `enabled`, `clusterX0` und `clusterY0` muss ein Index gesetzt werden.
Auf die Felder `world`, `level`, `enabled`, `clusterX1` und `clusterY1` muss ein Index gesetzt werden.
Auf die Felder `world`, `level`, `enabled`, `clusterX2` und `clusterY2` muss ein Index gesetzt werden.
Auf die Felder `world`, `level`, `enabled`, `clusterX3` und `clusterY3` muss ein Index gesetzt werden.

### Sprite-Redis-Entity

```text
"id": "string", // Eindeutige ID des Sprites ("D" + UUID
"world:"string", // ID der Welt, zu der das Cluster-Feld gehört
"level": "int", // Ebene des Sprites
"enabled" : "boolean", // Ob das Sprite aktiv ist
"clusterX": "int", // X-Position des Clusters im Gitter
"clusterY": "int", // Y-Position des Clusters im Gitter
"data": "blob", // JSON-Daten des Sprite
```

In redis werden die daten in einem Hash gespeichert. Der Hash-Key ist die ID des Sprites.

Ausserdem wird ein Index von `world`, `level`, `enabled`, `clusterX0/X1/X2/X3` und `clusterY0/Y1/Y2/Y3` benötigt, 
um die Sprites schnell abfragen zu können.

## Assets

Assets sind Grafiken, Sounds und andere Ressourcen, die in der Welt verwendet werden.

### Asset-JPA-Entity

```text
"world:"string", // ID der Welt, zu der das Cluster-Feld gehört
"name": "string", // Name des Assets (z.B. "tree.png", "water.wav", uuid) - Eindeutiger Name (primary key)
"type": "string", // Typ der Gruppe (z.B. "image", "sound")
"data": "blob", // Unkomprimierte Daten des Assets
"compressed": "blob", // Komprimierte Daten des Assets
"properties": "blob", // JSON-String der Eigenschaften des Assets
"createdAt": "timestamp", // Erstellungszeitpunkt des Assets
"updatedAt": "timestamp" // Aktualisierungszeitpunkt des Assets ausser bei compressed
"compressetAt": "timestamp" // Komprimierungszeitpunkt des Assets
```

Auf die Felder `world` und `name` muss ein unique Index gesetzt werden.

## Terrain-Gruppen

Die Gruppen sind eine Möglichkeit, Felder und Sprites zu organisieren und ihnen gemeinsame Eigenschaften zuzuweisen.

### Gruppen-Datenmodell

```json
{
  "id": "long", // Eindeutige ID der Gruppe
  "name": "string", // Name der Gruppe (z.B. "forest", "mountain")
  "type": "string", // Typ der Gruppe (z.B. "field", "sprite")
  "properties": {
    "key": "value" // Key-Value Werte der Eigenschaften als 'map of string'
  }
}
```

### Gruppen-JPA-Entity

```text
"world:"string", // ID der Welt, zu der das Cluster-Feld gehört
"id": "long", // Eindeutige ID der Gruppe (primary key)
"name": "string", // Name der Gruppe
"data": "blob", // JSON-Daten der Gruppe
"type": "string", // Typ der Gruppe (z.B. "field", "sprite")
"createdAt": "timestamp", // Erstellungszeitpunkt der Gruppe
"updatedAt": "timestamp" // Aktualisierungszeitpunkt der Gruppe
```

Auf die Felder `world` und `id` muss ein unique Index gesetzt werden.

## Referenzen

Referenzen sind Verweise auf andere Objekte in der Welt, wie z.B. Items oder Lebewesen.
Eine Referenz beginnt immer mit einem Präfix, das den Typ der Referenz angibt und
schließt mit der ID des referenzierten Objekts ab. Beide werden durch einen Doppelpunkt getrennt.

Prefix:
* "I" für Item: Referenz auf ein Item in der Welt.
* "L" für Living

## API Endpunkte

Bei jeder Abfrage muss die Welt-ID als Parameter angegeben werden.

Die API Endpunkte für Updates und Abfragen können immer als batched Aufruf erfolgen. Damit können mehrere
Operationen in einem einzigen Aufruf durchgeführt werden.

### Materialien anlegen

**POST /materials**

Role: `CREATOR`

Erstellt ein neues Material. Die ID wird automatisch generiert.

### Materialien abfragen

**GET /materials/{id}**

Role: `USER`

Fragt ein Material anhand seiner ID ab. Gibt die Metadaten des Materials zurück.

### Materialien auflisten

**GET /materials**

Role: `USER`

Listet alle verfügbaren Materialien auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Materialien pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### Materialien aktualisieren

**PUT /materials/{id}**

Role: `CREATOR`

Aktualisiert ein Material anhand seiner ID. Gibt die aktualisierten Metadaten des Materials zurück

### Materialien löschen

**DELETE /materials/{id}**

Role: `CREATOR`

Löscht ein Material anhand seiner ID. Gibt eine Bestätigung zurück, dass das Material gelöscht wurde.

### Map anlegen

**POST /maps**

Role: `CREATOR`

Erstellt eine neue Map. Die ID wird automatisch generiert. Die Map-Daten müssen im
Body der Anfrage im JSON-Format übergeben werden. Die Map-Daten müssen die Felder
`orld`,`level`,`x`, `y`, `groups`, `materials`, `opacity`, `sizeZ` und `parameters` enthalten.
Die Map wird in Clustern gespeichert, die automatisch generiert werden.
Die Cluster-Daten werden im Body der Anfrage im JSON-Format übergeben.

```json
{
    "world": "string", // ID der Welt
    "clusters": [
        {
        "level": "int", // Ebene der Map
        "x": "int", // X-Koordinate des Clusters
        "y": "int", // Y-Koordinate des Clusters
        "fields": [ // Array von Feldern im Cluster
            {
            "x": "int", // X-Koordinate des Feldes im Cluster
            "y": "int", // Y-Koordinate des Feldes im Cluster
            "z": "int", // Z-Koordinate des Feldes im Cluster (optional, default 0)
            "groups": ["long"], // IDs der Terrain-Gruppen, zu denen das Feld gehört
            "materials": ["int"], // Material des Feldes (z.B. "grass", "water"), index: 0 oben, 1-4: seitliche Materialschichten, 5 unten
            "opacity": "int", // Opazität des Feldes (0-255), 0 = transparent, 255 = undurchsichtig
            "sizeZ": "int", // Größe des Feldes in Z-Richtung nach unten, default 1
            "parameters": {
                "key": "value" // Key-Value Werte der Parameter als 'map of string'
            }
            },
            ...
        ]
        },
        ...
    ]
}
```

### Map abfragen

**GET /maps/{x}/{y}**

Role: `USER`

Gibt die Map-Daten für ein Cluster zurück. Die X- und Y-Koordinaten des Clusters müssen in der URL angegeben werden.

### Map als Batch abfragen

**POST /maps/batch**

Role: `USER`

Gibt die Map-Daten für mehrere Cluster zurück. Die X- und Y-Koordinaten der Cluster müssen im Body der Anfrage im JSON-Format übergeben werden.
Die Anfrage kann mehrere Cluster enthalten, die abgefragt werden sollen.
Die Antwort enthält die Map-Daten für alle angefragten Cluster.

```json
{
  "world": "string", // ID der Welt
  "level": "int", // Ebene der Map
  "clusters": [
    {
      "x": "int", // X-Koordinate des Clusters
      "y": "int" // Y-Koordinate des Clusters
    },
    ...
  ]
}
```

### Map aktualisieren

**PUT /maps

Role: `CREATOR`

Aktualisiert eine Map. Die ID der Map muss in der URL angegeben werden.
Die Map-Daten müssen im Body der Anfrage im JSON-Format übergeben werden. Die Map
wird in Clustern gespeichert, die automatisch generiert werden.
Die Cluster-Daten werden im Body der Anfrage im JSON-Format übergeben.

```json
{
    "world": "string", // ID der Welt
    "clusters": [
        {
            "level": "int", // Ebene der Map
            "x": "int", // X-Koordinate des Clusters
            "y": "int", // Y-Koordinate des Clusters
            "fields": [ // Array von Feldern im Cluster
                {
                    "x": "int", // X-Koordinate des Feldes im Cluster
                    "y": "int", // Y-Koordinate des Feldes im Cluster
                    "z": "int", // Z-Koordinate des Feldes im Cluster (optional, default 0)
                    "groups": ["long"], // IDs der Terrain-Gruppen, zu denen das Feld gehört
                    "materials": ["int"], // Material des Feldes (z.B. "grass", "water"), index: 0 oben, 1-4: seitliche Materialschichten, 5 unten
                    "opacity": "int", // Opazität des Feldes (0-255), 0 = transparent, 255 = undurchsichtig
                    "sizeZ": "int", // Größe des Feldes in Z-Richtung nach unten, default 1
                    "parameters": {
                        "key": "value" // Key-Value Werte der Parameter als 'map of string'
                    }
                },
                ...
            ]
        },
        ...
    ]
}
```

### Map löschen

**DELETE /maps**

Role: `CREATOR`

Löscht eine Map. Die ID der Map muss in der URL angegeben werden.
Die Map wird aus der Datenbank entfernt und kann nicht wiederhergestellt werden.

```json
{
    "world": "string", // ID der Welt
    "level": "int" // Ebene der Map, die gelöscht werden soll
    "clusters": [
        {
            "x": "int", // X-Koordinate des Clusters
            "y": "int" // Y-Koordinate des Clusters
          fields: [ // Array von Feldern im Cluster, die gel��scht werden sollen
            {
              "x": "int",
              // X-Koordinate des Feldes im Cluster
              "y": "int"
              // Y-Koordinate des Feldes im Cluster
            },
          ...
          ]
        },
        ...
    ]
}
```

### Lösche Level

**DELETE /maps/level**

Role: `CREATOR`

Löscht ein Level einer Map. Die ID der Map muss in der URL angegeben werden.
Die Map wird aus der Datenbank entfernt und kann nicht wiederhergestellt werden.

```json
{
    "world": "string", // ID der Welt
    "level": "int" // Ebene der Map, die gelöscht werden soll
}
```

### Sprites anlegen

**POST /sprites**

Role: `CREATOR`

Erstellt ein neues Sprite. Die ID wird automatisch generiert. Die Sprite-Daten müssen im
Body der Anfrage im JSON-Format übergeben werden.

```json
{
    "world": "string", // ID der Welt
    "level": "int", // Ebene des Sprites
  "sprites": [
    {
        "dynamic": "boolean", // Ob das Sprite dynamisch ist oder nicht
        "x": "int", // X-Koordinate im Gitter
        "y": "int", // Y-Koordinate im Gitter
        "z": "int", // Z-Koordinate im Gitter (optional, default 0)
        "sizeX": "int", // Größe des Sprites in X-Richtung
        "sizeY": "int", // Größe des Sprites in Y-Richtung
        "sizeZ": "int", // Größe des Sprites in Z-Richtung nach unten, default 1
        "groups": ["long"], // IDs der Terrain-Gruppen, zu denen das Sprite gehört
        "parameters": {
            "key": "value" // Key-Value Werte der Parameter als 'map of string'
        },
        "rasterType": "string", // Typ der Raster-Darstellung (z.B. "png", "jpg")
        "raster": "blob", // Sprite Darstellung
        "type": "string", // Typ des Sprites (z.B. "tree", "rock")
        "blocking": "boolean", // Ob das Sprite durchlässig ist oder nicht
        "opacity": "int" // Opazität des Sprites (0-255), 0 = transparent, 255 = undurchsichtig
    },
    ...
    ]
}
```

### Sprite abfragen

**GET /sprites/{id}**

Role: `USER`

Fragt ein Sprite anhand seiner ID ab. Gibt die Metadaten des Sprites zurück.

### Sprites auflisten

**GET /sprites/{world}/{level}/{x}/{y}**

Role: `USER`

Listet alle verfügbaren Sprites in dem Cluster x,y auf.

### Sprites aktualisieren

**PUT /sprites/{id}**

Role: `CREATOR`

Aktualisiert ein Sprite anhand seiner ID. Gibt die aktualisierten Metadaten des Sprites

### Sprites löschen

**DELETE /sprites/{id}**

Role: `CREATOR`

Löscht ein Sprite anhand seiner ID. Gibt eine Bestätigung zurück, dass das Sprite gelöscht wurde

### Sprites Coordinates aktualisieren

**PUT /sprites/{id}/coordinates**

Role: `CREATOR`

Aktualisiert die Koordinaten eines Sprites anhand seiner ID. Gibt die aktualisierten Metadaten des Sprites zurück.

```json
{
    "x": "int", // Neue X-Koordinate im Gitter
    "y": "int", // Neue Y-Koordinate im Gitter
    "z": "int" // Neue Z-Koordinate im Gitter (optional, default vorherige Z-Koordinate)
}
```

### Sprite enablen/disable

**POST /sprites/{id}/enable**

**POST /sprites/{id}/disable**

Role: `CREATOR`

Aktiviert oder deaktiviert ein Sprite anhand seiner ID. Gibt die aktualisierten Metadaten des Sprites zurück.

### Assets anlegen

**POST /assets**

Role: `CREATOR`

Erstellt ein neues Asset. Die ID wird automatisch generiert. Die Asset-Daten müssen im
Body der Anfrage im JSON-Format übergeben werden.

```json
{
    "world": "string", // ID der Welt
    "name": "string", // Name des Assets (z.B. "tree.png", "water.wav")
    "type": "string", // Typ des Assets (z.B. "image", "sound")
    "data": "blob", // Unkomprimierte Daten des Assets
    "properties": {
        "key": "value" // Key-Value Werte der Eigenschaften als 'map of string'
    }
}
```

### Asset abfragen

**GET /assets/{world}/{name}**

Role: `USER`

Fragt ein Asset anhand seiner ID ab. Gibt die Metadaten des Assets zurück.

### Assets auflisten

**GET /assets/{world}**

Role: `USER`

Listet alle verfügbaren Assets in der Welt auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Assets pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### Assets aktualisieren

**PUT /assets/{world}/{name}**

Role: `CREATOR`

Aktualisiert ein Asset anhand seiner ID. Gibt die aktualisierten Metadaten des Assets zurück

### Assets löschen

**DELETE /assets/{world}/{name}**

Role: `CREATOR`

Löscht ein Asset anhand seiner ID. Gibt eine Bestätigung zurück, dass das Asset gelöscht wurde

### Assets komprimieren

**POST /assets/compress**

Role: `CREATOR`

Komprimiert alle Assets in der Welt. Die Assets werden in der Datenbank komprimiert
und die komprimierten Daten werden in der Datenbank gespeichert.

```json
{
    "world": "string" // ID der Welt
}
```

### Assets als Batch abfragen

**POST /assets/batch**

Role: `USER`

Gibt die Asset-Daten für mehrere Assets zurück. Die Namen der Assets müssen im Body der Anfrage im JSON-Format übergeben werden.
Die Anfrage kann mehrere Assets enthalten, die abgefragt werden sollen.
Die Antwort enthält die Asset-Daten für alle angefragten Assets.

```json
{
  "world": "string", // ID der Welt
  "assets": [
      "string" // Name des Assets (z.B. "tree.png", "water.wav")
  ]
}
```

### Gruppen anlegen

**POST /groups**

Role: `CREATOR`

Erstellt eine neue Gruppe. Die ID wird automatisch generiert. Die Gruppen-Daten müssen im
Body der Anfrage im JSON-Format übergeben werden.   

```json
{
    "world": "string", // ID der Welt
    "name": "string", // Name der Gruppe (z.B. "forest", "mountain")
    "type": "string", // Typ der Gruppe (z.B. "field", "sprite")
    "properties": {
        "key": "value" // Key-Value Werte der Eigenschaften als 'map of string'
    }
}
```

### Gruppe abfragen

**GET /groups/{world}/{id}**

Role: `USER`

Fragt eine Gruppe anhand ihrer ID ab. Gibt die Metadaten der Gruppe zurück.

### Gruppen auflisten

**GET /groups/{world}**

Role: `USER`

Listet alle verfügbaren Gruppen in der Welt auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Gruppen pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### Gruppen aktualisieren

**PUT /groups/{world}/{id}**

Role: `CREATOR`

Aktualisiert eine Gruppe anhand ihrer ID. Gibt die aktualisierten Metadaten der Gruppe zurück

### Gruppen löschen

**DELETE /groups/{world}/{id}**

Role: `CREATOR`

Löscht eine Gruppe anhand ihrer ID. Gibt eine Bestätigung zurück, dass die Gruppe gelöscht wurde

### Gruppen als Batch abfragen

**POST /groups/batch**

Role: `USER`

Gibt die Gruppen-Daten für mehrere Gruppen zurück. Die IDs der Gruppen müssen im Body der
Anfrage im JSON-Format übergeben werden.
Die Anfrage kann mehrere Gruppen enthalten, die abgefragt werden sollen.
Die Antwort enthält die Gruppen-Daten für alle angefragten Gruppen.

```json
{
    "world": "string", // ID der Welt
    "groups": [
        {
            "id": "long" // ID der Gruppe
        },
        ...
    ]
}
```

### Welten anlegen

**POST /worlds**

Role: `CREATOR`

Erstellt eine neue Welt. Die ID wird automatisch generiert. Die Welt-Daten müssen im
Body der Anfrage im JSON-Format übergeben werden.

```json
{
    "name": "string", // Name der Welt (z.B. "Earth", "Mars")
    "description": "string", // Beschreibung der Welt
    "properties": {
        "key": "value" // Key-Value Werte der Eigenschaften als 'map of string'
    }
}
```

### Welt abfragen

**GET /worlds/{id}**

Role: `USER`

Fragt eine Welt anhand ihrer ID ab. Gibt die Metadaten der Welt zurück.

### Welten auflisten

**GET /worlds**

Role: `USER`

Listet alle verfügbaren Welten auf. Optional können Filterparameter
angegeben werden, um die Ergebnisse einzuschränken.
Es werden maximal 100 Welten pro Anfrage zurückgegeben. Die Ergebnisse können
mit den Parametern `page` und `size` paginiert werden.
Maximum `size` ist 100, Standard ist 20.

### Welten aktualisieren

**PUT /worlds/{id}**

Role: `CREATOR`
Aktualisiert die Metadaten einer bestehenden Welt. Die ID der Welt muss in der URL
angegeben werden.
Die Anfrage muss die aktualisierten Metadaten im Body enthalten.

### Welten löschen

**DELETE /worlds/{id}**

Role: `CREATOR`

Löscht eine bestehende Welt. Die ID der Welt muss in der URL angegeben werden.
Die Welt wird aus der Datenbank entfernt und kann nicht wiederhergestellt werden.

## Kafka Topics

### Map Update

**Topic: `terrain-map-update`**

Dieser Topic wird verwendet, um Änderungen an der Map zu veröffentlichen.
Es wird ein update Typ 'create', 'update' oder 'delete' verwendet, um die Art der Änderung anzugeben.
Ausserdem wird die ID des Clusters und die X- und Y-Koordinaten des Clusters angegeben.

```json
{
    "type": "create|update|delete", // Typ der Änderung
    "world": "string", // ID der Welt
    "level": "int", // Ebene der Map
    "clusterX": "int", // X-Position des Clusters im Gitter
    "clusterY": "int", // Y-Position des Clusters im Gitter
    "x": int, // X-Koordinate des Feldes im Cluster (optional, für create und update)
    "y": int // Y-Koordinate des Feldes im Cluster (optional,
}
```

### Sprite Update

**Topic: `terrain-sprite-update`**

Dieser Topic wird verwendet, um Änderungen an Sprites zu veröffentlichen.
Es wird ein update Typ 'create', 'update', 'moved', 'enabled', 'disabled' oder 'delete' verwendet, 
um die Art der Änderung anzugeben. Ausserdem wird die ID des Sprites und die X- und Y-Koordinaten 
des Sprites angegeben.

```json
{
    "type": "create|update|moved|enabled|disabled|delete", // Typ der Änderung
    "world": "string", // ID der Welt
    "level": "int", // Ebene des Sprites
    "reference": "string", // NEU Referenz zu einem Item oder Lebewesen, das das Sprite repräsentiert (optional, für create und update)
    "id": "string", // ID des Sprites
    "x": int, // X-Koordinate des Sprites im Gitter (optional, für create und update)
    "y": int // Y-Koordinate des Sprites im Gitter (optional, für create und update)
}
```

### Asset Update

**Topic: `terrain-asset-update`**

Dieser Topic wird verwendet, um Änderungen an Assets zu veröffentlichen.
Es wird ein update Typ 'create', 'update' oder 'delete' verwendet, um die Art der Änderung anzugeben.
Ausserdem wird die ID des Assets und der Name des Assets angegeben.

```json
{
    "type": "create|update|delete", // Typ der Änderung
    "world": "string", // ID der Welt
    "name": "string", // Name des Assets (z.B. "tree.png", "water.wav")
}
```

### Material Update

**Topic: `terrain-material-update`**

Dieser Topic wird verwendet, um Änderungen an Materialien zu veröffentlichen.
Es wird ein update Typ 'create', 'update' oder 'delete' verwendet, um die
Art der Änderung anzugeben.
Ausserdem wird die ID des Materials angegeben.

```json
{
    "type": "create|update|delete", // Typ der Änderung
    "id": "int", // ID des Materials
    "name": "string", // Name des Materials (z.B. "grass", "water")
}
```

### Group Update

**Topic: `terrain-group-update`**

Dieser Topic wird verwendet, um Änderungen an Gruppen zu veröffentlichen.
Es wird ein update Typ 'create', 'update' oder 'delete' verwendet, um die
Art der Änderung anzugeben.
Ausserdem wird die ID der Gruppe und die ID der Welt angegeben.

```json
{
    "type": "create|update|delete", // Typ der Änderung
    "world": "string", // ID der Welt
    "id": "long", // ID der Gruppe
    "name": "string" // Name der Gruppe (z.B. "forest", "mountain")
}
```

### World Update

**Topic: `terrain-world-update`**

Dieser Topic wird verwendet, um Änderungen an Welten zu veröffentlichen.
Es wird ein update Typ 'create', 'update' oder 'delete' verwendet, um die
Art der Änderung anzugeben.
Ausserdem wird die ID der Welt angegeben.

```json
{
    "type": "create|update|delete", // Typ der Änderung
    "id": "string", // ID der Welt
    "name": "string", // Name der Welt (z.B. "Earth", "Mars")
}
```
