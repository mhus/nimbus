
# World Physics Service

## Einführung

Der World Physics Service ermöglicht die Verwaltung
und Simulation von physikalischen Eigenschaften in
der Spielwelt. 

Vor allem Kollisonen, Schwerkraft und andere
physikalische Effekte werden hier behandelt.

TODO: Kampf, Parameter, Währung, Wetter, Tag/Nacht
TODO: Optionen für Spiel-Mechaniken

## Positionen

Die Positionen von Objekten in der Welt werden
in einem auf `level` basierenden Koordinatensystem
verwaltet. Level 0 ist die unterste Ebene, auf der
sich die Spieler befinden. Höhere Level repräsentieren
höhere Ebenen, wie z.B. Dächer oder Berge oder Wolken.
Tiefere Level repräsentieren unterirdische Bereiche
wie Höhlen oder U-Bahn-Schächte oder Unterwasserwelten.

Auf der Ebene kann es noch Erhebungen geben, die `z`-Achse
genannt wird. Diese repräsentiert aber nur eine Erhöhung. Damit können
kleine Hügel oder Treppen dargestellt werden.

## Positionen Caches

Positionen werdem im World Terrain Service verwaltet und
werden in einem Redis-Cache gespeichert. Dieser Cache
wird verwendet, um die Positionen schnell zu laden und
zu speichern.

Der Service ist Consumer von World Terrain Service Topics
und invalidiert die Caches, wenn sich etwas ändert.

## WorldObject

Ein WorldObject ist ein Objekt in der Welt, das ein Item oder ein Being sein kann.
Es hat eine Position, die aus `x`, `y`, `z` und `level` besteht und eine `sizeX` und `sizeY`.

Felder sind auch WorldObjects, werden aber direkt im World Terrain Service verwaltet, da sie
nicht bewegt werden können.

## Bewegung

Bewegungen können nur auf eine Koordinate in der Welt
durchgeführt werden. Bewegt wird entweder von nichts
oder von einem anderen Feld auf ein neues.

Bewegungen haben verschiedene Gründe. Wird das WorldObject
erstellt, wird es auf eine Startposition gesetzt.

Bewegt sich das Being selbst, kann es je nach Eigenschaften
* nicht bewegt werden, 
* auf ein angrenzendes Feld oder 
* auf ein beliebiges Feld bewegt werden.

Items werden in der Regel platziert oder aufgehoben.

Items und Beings können auch von externen Quellen
bewegt werden, z.B. bei einer Teleportation oder einer Kollision oder
wenn eine Explosion ein Being oder Item an eine andere Position
bewegt. Oder durch Zauberei.

### Unmögliche Orte/Felder

Es gibt Felder, an die ein WorldObject nicht bewegt
werden können. Das kann durch Eigenschaften des WorldObjects
geändert werden.

Manche Felder oder WorldObjects durchdringbar, wenn bestimmte
Eigenschaften gesetzt sind. Z.B. können manche Items
durch Wände hindurch bewegt werden, wenn sie
eine bestimmte Eigenschaft haben. Oder Beings können
durch Wände hindurch gehen, wenn sie eine bestimmte
Eigenschaft haben, z.B. wenn sie fliegen können oder
wenn sie bestimmte Zauber wirken oder bestimmte
Items im Inventar haben.

## Bewegung und Kollision

Es können nicht zwei Beings an der gleichen Position
stehen oder sich überlappen. Wenn ein Being sich bewegt, wird geprüft,
ob die neue Position frei ist. Wenn nicht, wird die
Bewegung verweigert/kollidiert.

Items können an der gleichen Position wie ein Being
liegen, aber nicht zwei Items an der gleichen Position.

Manche WorldObjects lösen effekte bei einer Kollision aus.

## WorldItem-Datenmodell

```json
{
  "id": "string",
  "type": "string",
  "x": "int",
  "y": "int",
  "z": "int",
  "level": "int",
  "sizeX": "int",
  "sizeY": "int",
  "sprite": {
    "id": "string"
  },
  "properties": {
    "key": "value" // hier können beliebige Eigenschaften gespeichert werden
  }
}
```

## API Endpunkte

### Bewegung 

** POST /world/move**

Bewegt ein WorldObject auf eine neue Position.

```json
{
  "id": "string", // ID des WorldObjects
  "x": "int", // neue x-Position
  "y": "int", // neue y-Position
  "z": "int", // neue z-Position
  "level": "int" // neues Level
}
```

