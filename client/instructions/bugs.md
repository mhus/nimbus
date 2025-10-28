## Bug

[x] Beim Login bricht die erste Verbindung ab, beim zweiten Mal funktioniert es.
---
[x] Wenn Zwei Blöcke uebereinander sind und man steht direkt davor (wie vor einer mauer), dann schaut man durch den
oberen Block durch. Vermutlich muss die Cam ein kleines stueck weiter zurueck, oder man sollte etwas eher stehen
bleiben. Wichtig ist, das man auf dem Voxel steht, der vor der Mauer ist.
```text
Das ist ein klassisches Kamera-Problem in Voxel-Engines: Die Kamera ist zu nah am Spieler, sodass die Near-Clipping-Plane den oberen Block abschneidet. Lass mich die Kamera-Einstellungen überprüfen:
```
---
[x] Wenn ich im Editor auf den Tab 'Block-Type-Liste' oder 'Änderungen' gehe, geht die Maus noch in den Pointer-Lock Modus.
---
~~[ ] Im Block-Editor funktioniert der Button 'Apply All' nicht mehr. Vermutlich ein Server Problem.~~
[x] Wenn ich im editor von Sphere auf etwas anderes wechsle, wird die Sphere nicht geloescht sondern ist zusaetzlich noch da.
[ ] Der eine teil des CROSS ist nur von einer Seite sichtbar.

[x] Die felder fuer scale (XYZ) im Block-Editor lassen sich nicht richtig bedienen, es lassen sich keine kommazahlen
eingeben, nicht richtig loeschen. Das Feld windLeverUp funktioniert da sehr gut, kann das genauso konfiguriert werden?
> change erst bei ENTER oder Focus lost
