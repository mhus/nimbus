
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

[?] Wird im GameplayService ein event von einer interaktion mit einem block empfangen,
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

[ ] Erweiterung im GamePlay:
- Bevor weitergeleitet wird, muss die aktuelle Session im mongoDb w_player_sessions gespeichert werden. WPlayerSessionServcice
- Die absprung URL ist die gerade erstellte teleport-login.html Seite. Dies muss dynamisch aus der logutUrl erstellt werden 
  z.b. logaut url ist http://foo.bar/controls/dev-login.html dann ist teleport url http://foo.bar/controls/teleport-login.html

[?] Ich merke gerade die Weiterleitung ist nicht so einfach moeglich. Da ja im Backend die Weiterleitung beschlossen wird.
Wie können wir das lösen? Es kann im Client ein Commando ausgeführt werden vie Wenbsocket Message (gibt es schon)
das die Weiterleitung im Client initiiert.
- Neues Commando im Client: RedirectCommand - Feld url:string
- Auslöesen des commandos im GameplayService nach dem setzen der teleportation.

[?] Erweiterung in PlayerService
- Wenn neuer w_player_sessions angelegt ist eine mergeFunktion aufrufen die daten vom alten in den neuen einträgt.
  - aktuell ist die funktion leer, bitte implementieren. (Hier sollen mal parameter wie health, mana, stamina, effekte etc. übernommen werden.)

## Teleport Login

[?] Erstelle einen neue teleport-login.html komponente unter ../client/packages/controls
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
- teleport-login hat keine aktiven Eingabeelemente 

[?] In der neuen w_player_sessions muss es Felder geben in der die alte worldId und position gespeichert werden.
Beides kann aus der alten w_player_sessions ausgelesen werden.
- Neue Felder in w_player_sessions anlegen
- Beim erstellen der neuen w_player_sessions die alten werte übernehmen.

## Return 

[ ] Wenn in teleportation nur 'return' steht, dann soll zurück zur alten welt teleportiert werden.
- Im PlayerService im teleportPlayer prüfen ob target 'return' ist.
- Wenn ja Prüfn ob im w_player_sessions die werte previousWorldId, previousPosition, previousRotation gesetzt sind.
- Wenn ja, dann denn teleportation aus diesen werten zusammen bauen. 
  - Erweiterung von entryPoint: um position: 'last|grid:q,r|world|position:x,y,z'
  - Erweiterung muss im WorldConfigController gemacht werden.
- Jetzt den w_player_sessions eintrag für das Ziel mergen (aktuell leere funktion, bitte einbauen), d.h. parameter wie health, mana, stamina, effekte etc. uebernehmen.
  - Es kann die mereits erstellte Funktion im PlayerService benutzt werden.
- Und jetzt die aktuelle w_player_sessions löschen. - Da wir aktiv zurück gehen sind die daten obsolate.
- Jetzt wie bisher weiter verfahren, wie wenn die teleportation normal gesetzt wurde

