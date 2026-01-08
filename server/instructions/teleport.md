
# Teleport

Zum teleportieren werden noch weitere Daten benoetigt, bevor teleport funktioniert.

## BlockMetadata erweitern

> Neues Feld 'confirm:string' - Für interaktion confirm dialog -> wird in metadata.client gespeichert!
> Neues Feld 'server:Record<string,string>' - Steuern von Block eigenschaften, wird nicht zum client gesendet!
> Neues Feld 'client:Record<string,string>' - Steuern von Block eigenschaften, wird nicht zum client gesendet!

[x] In BlockMetadata gibt es neue Felder
- groupId : string - Gruppe zu der der Block gehoert - wurde von number auf string geaendert
- server: Record<string,string> - Steuerung von Block eigenschaften, wird nicht zum client gesendet!
- client: Record<string,string> - Steuerung von Block eigenschaften, wird nicht zum client
Passe den Block Editor unter ../client/packages/controls an um diese Felder zu bearbeiten.

## WChunk - info

In WChunk werden die Daten für den Server gespeichert. Zusaetzlich sollen noch folgende Felder hinzugefuegt werden:

infoServer: Map<String,Map<String,String>>  - hier werden die parameter aus BlockMetadata.server gespeichert
- Key ist die coordinate des Blocks "x,y,z"
- Value sind die daten aus dem BlockMetadata.server

Wenn in WDirtyChunkService die Chunks erstellt werden, wird geprueft ob ein block metadaten.server hat. ist das so,
dann wird der eintrag in WChunk.infoServer hinzugefuegt. Am Block wird metadaten.server auf null gesetzt. 
Diese prüfung immer machen: Hat block keine metadaten (alles null), wird metadaten auf null gesetzt.

## Interactive Confirm

[?] Wird in engine Space gedrückt und es ist ein interaktiver Block im fokus (Selected), dann wird ein
event zum server gesendet (bereits implementiert).
- Nun soll geprüft werden ob der block in metadata.client ein feld confirm hat.
- Ist das feld gesetzt, wird ein confirm dialog angezeigt mit dem text aus confirm.
- Wenn der user bestätigt, wird das interactive event gesendet. sonst nicht.
- Ist das Feld nicht gesetzt, wird das event sofort gesendet (wie bisher).

## Teleport Gameplay

[ ] Wird im GameplayService ein event von einer interaktion mit einem block empfangen,
dann wird der WChunk geladen und dort geprueft ob in infoServer für den Block ein eintrag existiert.
- Erstelle heirfür in WChunk eine methode getServerInfoForBlock(x:int,y:int,z:int):Map<String,String>
Wenn ja prüfe ob ein eintrag teleportation existiert.
- Wenn ja, dann soll eine Teleportation durchgefuehrt werden.
- Erstelle dazu eine methode teleportPlayer(playerId:string, target:string) im PlayerService.
- Format teleportation string: <worldId>#<position> worldId#grid:q,r oder worldId - dann normaler WordlId entry point
  - worldId kann auch leer sein, dann nur position: #<position> oder #grid:q,r
  - worldId kann eine volle WorldId (Klasse WorldId) sein, aber Ohne Instance teil.
- Prüfe ob die worldId existiert, wenn nicht, dann abbrechen.
- Es wird in der session setTeleportation mit dem teleportation target gesetzt.
- Dann wird umgeleitet auf die /teleport URL (wie logout).

[ ] Erweiterung:
- Bevor weitergeleitet wird, muss die aktuelle Session im mongoDb w_player_sessions gespeichert werden. WPlayerSessionServcice

## Teleport Login

[ ] Erstelle einen neue teleport-login.html komponente unter ../client/packages/controls
- Bei aufruf wird sofort ein neuer REST Controller aufgerufen /control/player/teleport-login 
  - der die sessionId aus dem cookie liest - wird bereits im AccessFilter gemacht.
  - auslesen der session, diese kann bereits CLOSED sein.
    - Prüfen ob teleportation gesetzt ist in der session, wenn nicht, fehler.
    - Warten bis Closed ist, in einer schleife mit timeout (5s) - Konfigurierbar
    - Jetzt eine neue w_player_sessions für die neue weltId aus teleportation anlegen.
    - Eine neue WSession erstellen mit der sich der Player verbinden kann
    - Position im WSession hinterlegen (aus teleportation)
    - Rückgabe an teleport-login.html die Urls jumpUrl, cookieUrls. - Dieser Teil ist dann exakt wie in dev-login.html
    - cookieUrls aufrufen und dann zu jumpUrl weiterleiten.
- Orientiere dich an dev-login.html und den entsprechenden REST Controller
