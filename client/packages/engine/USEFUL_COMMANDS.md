

## Items im Server setzten

1. Items mit Position anlegen (bereits vorhanden, verbessert)
   - /item add <x> <y> <z> <displayName> <itemType> [texturePath]
   - Erkennt automatisch ob Koordinaten angegeben sind
2. Items ohne Position anlegen (neu)
   - /item add <displayName> <itemType> [texturePath]
   - Legt Item ohne Position an (nur in Registry, nicht in der Welt)
3. Items auf Position setzen (neu)
   - /item place <itemId> <x> <y> <z>
   - Setzt ein Item ohne Position auf eine Position
   - Prüft ob Item bereits Position hat
   - Prüft ob Zielposition bereits belegt ist
   - Sendet Update an Clients
4. Items von Position entfernen (bereits vorhanden)
   - /item remove <x> <y> <z>
   - Sendet __deleted__ Marker an Clients (bereits implementiert in Zeile 193)

