
# Access

## Konzept

- EntryPoints werden später über HexGrid umgesetzt. Aktell wird der Default-Wert in WorldInfo genutzt.
- Es gibt Rollen in denen der User ist und es gibt eine ActorRolle die der User gerade ausfüllt.
- Rollen: SECTOR_ADMIN, REGION_ADMIN, WORLD_OWNER, WORLD_EDITOR, WORLD_PLAYER, WORLD_SUPPORT
- Actor(Rolle): SUPPORT, EDITOR, PLAYER (Welt-spezifisch)
- Die ActorRolle wird beim Login gesetzt.
- Es gibt zwei arten von AccessToken:
  - Session basierter AccessToken (via WebSocket Login)
    - Gültigkeit: 24 Stunden
    - Enthält: sessionId, worldId, userId, characterId, role, actor
    - Wird in Redis gespeichert und bei jedem Zugriff geprüft.
  - Agent AccessToken (via DevLogin)
    - Gültigkeit: 1 Stunde
    - Enthält: worldId, userId, role
    - Kein session basierter Zugriff, kann nicht auf session basierende Funktionen zugreifen.

- Rest endpoints in world-control
- AccessService in world-shared
- vue html componente in ../client/packages/controls für DevLogin

## Aufräumen

[ ] Es gibt bereits eine implementierung in world-region die muss vorher geprueft werden.
[ ] Timeout an Webscoket auf 24 Stunden setzen (aktuell 12 Stunden)
[m] Rollen und AccessGruppen in Typen umsetzen
[m] Alte Filter löschen (nicht universe)

## Develpment

Login:
- agent: boolean (optional, default false) - es wird ein agent access token erstellt hier sit characterId und role obsolate
- worldId: string
- userId: string
- characterId - nur bei agent false
- actor: string - nur bei agent false

1. Auwahl: Welt suchen / auswahlen
2. Anzeige tabs: session / agent - default session

3. Session:
   1. Character auswahl
   2. Actor auswahl (PLAYER, EDITOR, SUPPORT)
4. Agent:
   1. User auswahl
5. Login button

- Benutze JwtService um tokens zu erstellen. ggf um generische funktionen erweitern

[?] Erstelle den AccessService in world-shared
- mit den methoden:
  - getWorlds (Punkt 1) - WWorldService nutzen
  - getCharactersForUserInWorld(userId, worldId) (Punkt 3.1)
  - getRolesForCharacterInWorld(characterId, worldId) (Punkt 3.2)
- Erstelle benoetigte DTOs in world-shared / types

[?] Erstelle in AccessService eine 
- methode 'devSessionLogin(...)
  -  Rückgabe: AccessToken, cookieUrls, jumpUrl, SessionId, PlayerId
- methode 'devAgentLogin(...)
  - Rückgabe: AccessToken, cookieUrls, jumpUrl
- Erstelle benoetigte DTOs in world-shared / types


[?] Erstelle einen REST Endpunkt im world-control /api/aaa/devlogin 
- POST methode: fuehrt dev login aus
- GET methode gibt gesucht daten zurueck fuer den devLogin control

AccessToken:
- JWT Token wird mit region erstellt, nutze JwtService aus shared
- Inhalt:
  - agent: boolean
  - worldId
  - userId
  - characterId  - bei agent false 
  - role - bei agent false
  - sessionId - bei agent false
  - expiration: 5 minuten

[?] Erstelle in ../client/packages/controls einen neuen dev-login.html editor
- Suchfeld fuer user
- Suchfeld fuer world
- Liste alle Eintraege auf bei click auf einen eintrag wird der dev login ausgefuehrt
- Abfrage agebt oder session type
- Wenn login erfolgreich wird auf allen cookieUrls der pfad /api/aaa/authorize mit dem access token aufgerufen
- Danach wird auf die jump url weitergeleitet

## Access

[?] Erstelle in world-shared einen REST endpunkt /api/aaa/authorize
- Query Parameter:
  - accessToken: string
- retour:
  - httpOnly session Cookie: erstelle ein httpOnly Cookie in dem der sessionToken gespeichert wird
  - es wird ein session cookie erstellt auf das javascript zugriff hat 
  - Beide Cookies:
    - sessionId aus dem token
    - worldId aus dem token
    - userId aus dem token
    - characterId aus dem token
    - role aus dem token
- Das accessToken wird geprüft
  - agent true:
    - der user wird geladen und geprueft ob er sich an der welt als agent anmelden darf
  - agent false:
  - es wird geprüft ob die sessionId im redis existiert und im Status WAITING ist. 
    Und auch role, worldId und playerId stimmen überein.
- Wenn alles ok ist, wird ein sessionToken erstellt und ein httpOnly Cookie erstellt mit dem sessionToken
- sessionToken
  - 24 Stunden gueltig bei agent false (wird durch ende der session auch invalidiert), 1 Stunde bei agent true
  - agent: boolean
  - worldId
  - userId
  - role: string
  - sessionId
  - characterId

## Access Filters

[ ] Erstelle in world-shared einen AccessFilterBase
- Der Filter soll aktuell nur den sessionToken aus dem httpOnly Cookie lesen und prüfen
- Falls es ein SessionToken ist die session pruefen
- Ergebnisse an den HttpRequest anhaengen
- Ausgabe im Log
- noch nicht Zugriff verhindern
In world-player und world-control entsprechende Ableitungen PlayerAccessFilter und ControlAccessFilter.


---





## World Info Overwrite

Bei der Rückgabe der WorldInfo muessen vorher Daten überschrieben werden.
- Entrypoint
- World Status (character spezifisch?!)
- WorldId bei fallback (instance / branch)

---

Es werden zwei arten von access benoetigt
- session orientierter access, wird durch login via WebSocket aufrecht erhalten. max 24 Stunden. session wird in redis gespeichert und bei jedem zugriff geprueft.
- agent access ohne session. Kann nicht auf session basierende funktionen nutzen. z.b. fuer bots oder server zugriffe. 
  - laufzeit 1 Stunde
