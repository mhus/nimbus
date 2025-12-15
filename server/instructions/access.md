
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

[x] Erstelle in world-shared einen AccessFilterBase
- Der Filter soll aktuell nur den sessionToken aus dem httpOnly Cookie lesen und prüfen
- Falls es ein SessionToken ist die session pruefen
- Ergebnisse an den HttpRequest anhaengen
- Ausgabe im Log
- noch nicht Zugriff verhindern
In world-player und world-control entsprechende Ableitungen PlayerAccessFilter und ControlAccessFilter.

[x] In world-control soll der ControlAccessFilter den Zugriff verweigern wenn das cookie nicht gueltig ist.
- Ausnahmen fuer /control/aaa/authorize und /control/aaa/devlogin und /control/aaa/status und /control/aaa/logout
- Bei verweigerung 401 zurueckgeben
- Gib eine info-seite aus mit einem Link auf den login (AccessProperties.loginUrl).
- Die freigaben sollen durch eine abstrakte methode in AccessFilterBase abgefragt werden.

[x] In world-player soll der PlayerAccessFilter den Zugriff verweigern wenn das cookie nicht gueltig ist oder kein session cookie ist.
- Ausnahmen fuer /player/aaa/authorize
- Bei verweigerung 401 zurueckgeben
- Gib eine info-seite aus mit einem Link auf den login (AccessProperties.loginUrl).
- Die freigaben sollen durch eine abstrakte methode in AccessFilterBase abgefragt werden.

[?] Zugriff auf rest endpunkte mit Annotations. 
Da die AccessToken daten im request sind, koennen sie aus dem Request geholt werden.
Es soll erstmal ein AccessUtil geben mit methoden wie:
- hasRole(Request request, SectorRoles role): boolean
- hasRole(Request request, WorldRoles role): boolean
- isWorldOwner(Request request): boolean
- isWorldEditor(Request request): boolean
- isWorldPlayer(Request request): boolean
- isWorldInstance(Request request): boolean (WordlId.isInstance)
- isWorldZone(Request request): boolean (WorldId.isZone)
- isWorldBranch(Request request): boolean (WorldId.isBranch)
- isWorldCollection(Request request): boolean (WorldId.isCollection)
- isWorldSet(Request request): boolean
- getWorldId(Request request): WorldId
- getUserId(Request request): UserId
- getCharacterId(Request request): CharacterId
- getPlayerId(Request request): PlayerId
- isAgent(Request request): boolean
- getSessionId(Request request): String
- hasSession(Request request): boolean

Darauf aufbauend koennen dann Annotations erstellt werden wie:
- @RequireWorldRole(WorldRoles role)
- @RequireSectorRole(SectorRoles role)
- @RequireAgent
- @RequireSession
- @RequireWorldInNotInstance // spezial for this case
Die annotations sollen an Rest endpoints angebracht werden koennen.

```text
  Zusammenfassung: Access Control Annotations

  Ich habe erfolgreich ein vollständiges System für Zugriffskontrolle via Annotations auf REST-Endpunkten in world-shared erstellt:

  1. AccessUtil (world-shared/src/main/java/de/mhus/nimbus/world/shared/access/AccessUtil.java)

  Utility-Klasse mit Methoden zum Prüfen von:
  - Authentication Status: isAuthenticated(), isAgent(), hasSession()
  - User Information: getUserId(), getCharacterId(), getPlayerId(), getSessionId()
  - World Information: getWorldId(), isWorldInstance(), isWorldZone(), isWorldBranch(), isWorldCollection(), isWorldSet()
  - Role Checks: hasSectorRole(), hasWorldRole(), isWorldOwner(), isWorldEditor(), isWorldPlayer()

  Alle Daten werden aus den Request-Attributen gelesen, die vom AccessFilterBase gesetzt wurden.

  2. Annotations

  5 Annotations für deklarative Zugriffskontrolle:

  - @RequireWorldRole(WorldRoles role) - Erfordert eine bestimmte World-Rolle (PLAYER, EDITOR, OWNER, SUPPORT)
  - @RequireSectorRole(SectorRoles role) - Erfordert eine bestimmte Sector-Rolle (USER, ADMIN)
  - @RequireAgent - Erfordert Agent-Authentifizierung (nicht-session-basiert)
  - @RequireSession - Erfordert Session-Authentifizierung (mit Character)
  - @RequireWorldIsNotInstance - Erlaubt Zugriff nur auf Nicht-Instance-Welten

  3. AccessControlAspect (world-shared/src/main/java/de/mhus/nimbus/world/shared/access/AccessControlAspect.java)

  Spring AOP Aspect der:
  - Alle REST-Endpunkte (@GetMapping, @PostMapping, etc.) abfängt
  - Class-Level und Method-Level Annotations prüft
  - Bei fehlender Berechtigung HTTP 403 Forbidden zurückgibt
  - Detaillierte Logs für Debugging erstellt

  4. Dependency: Spring Boot Starter AOP hinzugefügt in pom.xml

  Verwendungsbeispiele:

  // Nur für World-Editoren
  @RequireWorldRole(WorldRoles.EDITOR)
  @GetMapping("/world/config")
  public ResponseEntity<?> getConfig() {
      // ...
  }

  // Nur für Sector-Admins
  @RequireSectorRole(SectorRoles.ADMIN)
  @PostMapping("/sector/settings")
  public ResponseEntity<?> updateSettings() {
      // ...
  }

  // Nur für Session-basierte Authentifizierung
  @RequireSession
  @PostMapping("/player/action")
  public ResponseEntity<?> playerAction() {
      // ...
  }

  // Nicht auf Instance-Welten erlaubt
  @RequireWorldIsNotInstance
  @PutMapping("/world/structure")
  public ResponseEntity<?> updateStructure() {
      // ...
  }

  // Kombiniert: Class-Level + Method-Level
  @RestController
  @RequireSession  // Gilt für alle Methoden
  public class PlayerController {

      @RequireWorldRole(WorldRoles.PLAYER)  // Zusätzlich für diese Methode
      @PostMapping("/move")
      public ResponseEntity<?> move() {
          // ...
      }
  }
```
Erstelle das in world-shared im package access.


## Inner Server Comunikation

[?] Wenn dei server untereinander kommunizieren via rest requests, dann muss der access token als bearer token im authorization header mitgegeben werden.
- Dazu haelt jeder Server ein eigenes worldToken das vollen Zugriff erlaubt (keine worldId oder userId, characterId enthalten, dafuer der name des Services (LocaltionService.applicationServiceName)).
- In AccessService wird ein solches bearer Token erzeugt und gehalten. Wenn es ablauft, wird automatisch ein neues geholt (on request).
- Will ein Service auf einen anderen world service zugreifen, holt er sich das token aus dem AccessService und fuegt es dem request header hinzu.
- Im AccessFilterBase wird geprueft ob ein bearer token im authorization header ist. Wenn ja, wird es geprueft und der zugriff erlaubt.

[?] Der AccessService hält ein token mit dem die services untereinander kommunizieren koennen.
- Im WorldClientService werden rest aufrufe ausgefuehrt. Da muss immer der token als Bearer Token mitgegeben werden.

## Cookie & CORS Configuration

**WICHTIG für Cookie-basierte Authentication:**

1. **CORS mit Credentials**:
   - `Access-Control-Allow-Credentials: true` MUSS gesetzt sein
   - `Access-Control-Allow-Origin` darf NICHT `*` sein - muss spezifische Origin sein
   - Konfiguration in `CorsConfig.java` (world-control, world-player)
   - Default: `http://localhost:*` (Pattern für alle localhost Ports)
   - Production: Spezifische Origins in `application.yml` konfigurieren

2. **Cookie Einstellungen** (AccessService.java):
   - **sessionToken Cookie**: httpOnly=true, SameSite=Lax (HTTP) oder SameSite=None (HTTPS)
   - **sessionData Cookie**: httpOnly=false (JS-accessible), Base64-encoded JSON
   - MaxAge wird aus JWT expiresAt berechnet (UTC-korrekt)
   - Path: `/`
   - Secure: true nur für HTTPS (Production)

3. **Bekannte Probleme & Lösungen**:
   - ❌ Cookie-Wert mit ungültigen Zeichen → ✅ Base64-Encoding für JSON-Daten
   - ❌ Timezone-Problem bei MaxAge → ✅ Berechnung aus `expiresAt.getEpochSecond() - Instant.now().getEpochSecond()`
   - ❌ CORS Wildcard mit Credentials → ✅ Spezifische Origins verwenden
   - ❌ Tomcat verwirft Cookies → ✅ Manuelles Cookie-Header Parsing als Fallback in AccessFilterBase



## Logout

[?] Fuege in world-control /control/aaa/status POST methode hinzu, die gibt nur die aktuellen daten aus dem sessionToken zurueck und die urls und die logoutUrl (AccessProperties.logoutUrl)
- Siehe dazu auch devLogin
[?] Fuege in die bestehende routen /control/aaa/authorize und /player/aaa/authorize eine DELETE methode hinzu. Sie soll die sessionId aus dem httpOnly entfernen.
- In /player/aaa/logout: Falls es ein SessionToken ist, soll die ession im redis mit SessionService auf CLOSED gesetzt werden.

[?] Fuege in ../client/packages/controls einen logout.html control hinzu. Der holt sich die urls, macht den DELETE auf alle urls 
und leitet auf die login seite weiter.

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
