

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



doSend('world','status', 666 )

doSend('world','status', 0 )

doSetShortcut('click0', 'use', {"itemId": "item_1763653693310_uv2m2pu", "wait": 100})

doSend('world','season', 0 ) // NONE
doSend('world','season', 1 ) // WINTER
doSend('world','season', 3 ) // SUMMER

doSend('world','seasonProgress', 0.5 )


## Moon

doMoonTexture(0,'textures/moon/moon1.png');
doMoonEnable(0,'true');
doMoonSize(0, 70);
doMoonPosition(0, 180);
doMoonElevation(0, 60);
doMoonDistance(0, 450);
doMoonPhase(0, 1.0);

## Clouds

# Wolke im Norden, 200 Blöcke entfernt, Höhe 180
doCloudAdd("cloud-north",0, -200, 180, 80, 50, "textures/clouds/cloud1.png", 3, 0, 0);

# Wolke im Osten, 150 Blöcke entfernt, Höhe 160
cloudAdd "cloud-east" 150 0 160 60 40 "textures/clouds/cloud2.png" 5 90 1

# Wolke direkt über der Kamera
cloudAdd "cloud-above" 0 0 200 100 60 "textures/clouds/cloud3.png" 0 0 2

