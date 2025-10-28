
# Client 2.0

Der client soll aus der aktuelle playground 1.0 phase in eine neue version 2.0 überführt werden.

## Focus

Der Server wird in Typescript mitgeführt, aber nur als Referenz implementiert. Der Fokus liegt auf dem Client.
Es wird später einen eigenständigen Server geben der in Java implementiert wird.

## Ziel des Gesamtsystems

Das Ziel ist es, eine 3D Engine für eine voxel-basierte Welt zu erstellen, später kann zu verschiedenen Servern
verbunden werden. Der Server macht das Spiel, der Client rendert die Welt und stellt die Interaktion sicher.

In dem Spiel werden keine Blöcke von Spielern direkt verändert. Das Prinzip ist eher wie in WoW, Fallout,
TESO, oder anderen MMORPGs. Nicht wie in Minecraft.

Der Server überlässt dem Client weitgehen die Darstellung, Bewegung incl. Collision, Gravitation und Interaktion. 
Der Server prüft nur die wichtigen Aspekte, z.b. bereiche die nur mit bestimmten items betreten werden können, oder ob
ein Spieler in einem Bereich Schaden nimmt. Attacken auf andere Spieler werden auch vom Server geprüft.

Beispiel Attacken:
- Spieler A attackiert Spieler B mit einem Zauberstab
- Der Client startet die hinterlegte Animation für den Angriff
- Der Client von Spieler A sendet eine Attacken-Nachricht an den Server
- Der Server prüft ob Spieler B in Reichweite ist und ob der Angriff erfolgreich ist
- Der Server sendet eine Nachricht an den Client von Spieler B, dass er getroffen wurde
- Der Client von Spieler B startet die Treffer-Animation und reduziert die Lebenspunkte

Beispiel Interaktion mit NPCs:
- Spieler A nähert sich einem NPC und klickt auf ihn, InteractionRequest wird an den Server gesendet
- Der Server prüft ob der NPC existiert und ob der Spieler in Reichweite ist
- Der Server sendet eine NPC Open Dialog Nachricht an den Client von Spieler A (Dialog geht auf)
- Der Client von Spieler A zeigt den Dialog an und ermöglicht Interaktionen
- Der Spieler wählt eine Option im Dialog, die an den Server gesendet wird
- Der Server verarbeitet die Auswahl und sendet ggf. eine Antwort zurück
  - z.B. Quest angenommen, Item erhalten, Dialog aktualisiert
- Der Client aktualisiert den Dialog basierend auf der Serverantwort
- Der Spieler schließt den Dialog, der Client sendet eine Dialog Close Nachricht an den Server
- Der Server bestätigt den Dialogabschluss und aktualisiert ggf. den NPC-Status

## Darstellung Voxels und mehr

Die Darstellung und logic des Spiels basiert auf Voxels (Blöcken). Die Darstellung
geht aber über voxels hinaus. Die Voxels können auch als Modelle, Columns, Spheres oder anderes dargestellt werden.
Die Ecken der Voxels sind durch offsets anpassbar, so dass auch unregelmäßige, organische Formen möglich sind.
Das Prinzip bleibt aber voxel-basiert. Eine Entity bewegt sich z.b. immer von voxel zu voxel, auch wenn die voxels
selbst als Kugeln oder andere Formen dargestellt werden. Collision management bleibt voxel-basiert.

## Wichtige Features

- 
