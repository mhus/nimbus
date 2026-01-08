
# Player Session Persistence

Der Status einer Player Session soll gespeichert werden können. Wenn der Player
sich wieder einloggt, dann kann der Status wiederhergestellt werden.

Wichtig für:
- Weiter spielen
- Wenn in eine Zone ud Zurück wechseln wird. - Hier muss ein Merge gemacht werden.

Speichern in bestimmten Situatuionen:
- WSession close / logout
- Immer mal wieder zwischendurch (Timer)
- Wenn in den Combat Modus gewechselt wird
- Wenn in eine Zone gewechselt wird

Persistiert wird:
- PlayerId, WorldId (Key)
- Position, Rotation
- Aktuelle Effekte
- Aktuelle Health / Mana / Stamina Werte

> Das ist viel GamePlay, aber GamePlay ist noch nicht umgesetzt
> Später erweitern um Gameplay Funktionalität.

## Entity

[ ] Erstelle eine Entity PlayerSession in world-shared mit den folgenden Feldern:
- wordlId
- playerId
- position : Vector3
- rotation : Rotation (?)
- returnWorldId : String (für Zonen wechsel)
- returnPosition : Vector3
- createdAt
- modifiedAt
- unique Index auf worldId + playerId
Und das PlayerSessionRepository mit den üblichen CRUD Methoden. Erstelle einen PlayerSessionService der die 
PlayerSessions verwaltet.


