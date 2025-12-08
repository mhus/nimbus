
[ ] Teams
- Clients koennen Teams beitreten. Teams haben einen eindeutigen namen (uuid) und werden in redis gespeichert.
- Teams sind nicht world spezifisch
- Ein Team hat mitglieder (player namen), Team Player: src/main/java/de/mhus/nimbus/generated/types/TeamMember.java
  - Einladungen im redis speichern, max 15 min haltbar
- Ein Player muss keine Position haben, z.b. wenn er in einer anderen Welt ist.
- Um Teams zu Managen, erstellen/löschen/mitglied hinzuzufügen/zu entfernen soll es REST Endpunkte im world-control server geben.
    - Route /api/teams
- Es werden per redis events an alle world-player server die team änderungen geschickt.
    - Wird ein event mit einer leeren Mitglieder liste gesendet, wurde das team verlassen.
    - Wird detected, dass ein Spieler in mehreren Teams ist, wird er automatisch aus dem alten Team entfernt. (Es kommt ein Team event herein, aber der name Passt nicht zum aktuellen, wird er entfernt aus dem alten)
    - Ein spieler kann nur in einem Team gleichzeitig sein.
    - Nachrichten werden an ae world-player server geschickt, hier wird auf die sesssion verteilt, die player aus der liste sind.
- Die message 't.d' "## Team Data (Server -> Client)" muss zum client gesendet werden, wenn ein spieler einem team beitritt oder es verlaesst.
    - In der WebSocket Session sollte gespeichert werden in welchem team der spieler ist und welche mitglieder (List fo String) noch darin sind.
    - Der aktuelle speiler wird beim senden an den client nicht mitgesendet.
- Die Message 't.s' "## Team Status (Server -> Client)" muss zum clint gesendet werden wenn sich der status eines spielers ändert
    - Position kann direkt aus den "e.p" "## Entity Chunk Pathway (Server -> Client)" messages genommen werden.
- Andere status änderungen werden per redis an alle world-player server geschickt.
- Siehe instructions/general/network-model-2.0.md

[ ] Teams Management
- Neue Gruppe anlegen
- Mitglied einladen
- Einladung annehmen/ablehnen
- Mitglied entfernen
- Gruppe verlassen
- Gruppe löschen (nur wenn keine mitglieder mehr drin sind)
- Team info abfragen (name, mitglieder)

- team-view.html
  - Anzeige aktuelles Team (redis)
  - Einladung versenden (redis storage)
  - Einladungen anzeigen (versendete, sehen alle)
  - Einladungen anzeigen, wenn man eingeladen wurde
  - Annehemen/ablehnen
