
# World Item Spezifikation

## Einleitung

Der World Item Service verwaltet Items in der Welt und deren Kooperation.

## Zaubersprüche

Zaubersprüche werden durch Spell-Items präsentiert.

## Ablage

World Items können in der Welt abgelegt werden. Sie haben eine Position, 
die aus `x`, `y`, `z` und `level` besteht. Items können auch in Containern 
abgelegt werden, die ebenfalls World Items sind. Oder sie sind im Inventar
von Beings abgelegt, die ebenfalls World Items sind.

## World Item

Ein World Item ist ein Objekt in der Welt, das ein Item sein kann.
Es hat eine Position, die aus `x`, `y`, `z` und `level` besteht und eine `sizeX` und `sizeY`.

World Items werden durch ein Sprite repräsentiert, das die grafische Darstellung des Items ist.


