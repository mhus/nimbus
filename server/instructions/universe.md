
# Universe Structure and Management

## Overview

Universe <- Region <- Solar System <- Planet <- World

Universe -> Universe / Governor Server
Region -> Ueber meherere Regionen verteilt (Regionen sind einheiten mit fester Größe, Regionen können wachsen)

Universe -> Universe Server - Only one Universe!
Galaxy -> Universe Server (Meta Struktur)
Region -> Region Server
Region -> Universe Server (Meta Struktur)
Solar System -> Universe Server (Meta Struktur)
Planet -> Universe Server (Meta Struktur)
World -> World Server

ggf. Universe Region zum durchqueren des Universums

- Universe managed: User, Echtwährung (Governor), Regionen Registry, Short Links zu Welten
  - Management von geografischer Lage der Regionen, Regionen, Systems, Planeten/Ebenen und Welten, EntryPoints
  - Welten und EntryPoints werden auf Planeten auf einer Map dargestellt - diese wird von Region an Universe gemeldet
  - Universe verwaltet bekannt Entry Points zu Welten, werden von Region an Universe gemeldet
  - Können auch als Invite gemeldet werden von Owner einer Welt
- Region managed: Metastruktur (Solar Systeme, Planeten), Welten Registry, Character Daten
  - Character Daten: Inventar, Rucksack, Alt-Waehrung, Vortschritt, Skills, Einstellungen
  - Eigene Asset (Texture, Model) und item Verwaltung uebergreifend
  - (Shop)
  - Character Access Points zu Welten (Bekannte Reisepunkte) - Werden von Welt an Region gemeldet, Startpunkte fuer besucher uder Einladungen
- World managed: Welt Daten, Block Daten, In-Game Aktionen

- Transfer zwischen den Platformen wird spaeter moeglich sein
- Items sollen eine signatur des erstellers und des systems ahben, bei dem es erstellt wurde und erlaubt ist

- Universe hat einen Signatur Key fuer:
  - Access Tokens 
- Region hat einen Signatur Key fuer:
  - Character Data Tokens

- Auf einem Planeten gibt es eine Map und weitere Ebenen
  - Auf der Map koenne Welten in 6Eck Raster definiert werden, Pro Raster ein EntryPoint möglich (optional)
  - Ausserdem können weitere Ebenen defineirt werden (jeweils eine Welt)

Access:
- Unverse Access Token
- Region Access Token ? -> Unverse Access Token + Region Info Claim
- World Access Token ? -> Unverse Access Token + Region Info Claim + World Info Claim
- Visum - World Access Token ? - One Time Access to a single point in the World

Word Access:
- Login beim Universe -> universe access token
- Auswahl des Regionen -> Region abfrage von Access Points (Worlds) (mit such funktion nach Solar System, Panet, Name - Liste der SolarSystems und Planeten) -> Region Access Point + Region Access Token
  - Oder Short Link zu World -> Region Access Point + Region Access Token
  - Hier auch Rolle des Characters abfragen (owner, editor, moderator, player) und angeben mit welcher rolle eingelogt werden soll
- Access Points auf der Welt anzeigen (Name, Beschreibung, Bild, Rolle) -> Auswahl der Welt -> Visum wird vom Region Server ausgestellt -> World Access Token + Visum

Visum:
- Der User/Character darf nicht einfach so in einer Welt irgendwohin reisen, er braucht ein visum, das einen Teleport an eine bestimmte stelle erlaubt
- Wann visum: 
  - Beim Betreten, wird vom Region Server ausgestellt
  - Beim Teleport innerhalb der Welt, wird vom World Server ausgestellt ????
  - Beim Teleport mit einem Teleporter, auch in eine andere Welt, wird vom Region Server ausgestellt ????
- Visum beinhaltet:
  - World Access Token
  - Ziel Position
  - Zeitliche Begrenzung des Logins (nicht des aufenthalts in der Welt)

## Datenstrukturen

UniverseInfo:
  - userId

UniverseCoins:
  - amount

RegionInfo:
  - RegionId: string
  - ApiUrl: string
  - characters

RegionCharacterInfo:
- userId: string
  - characterId: string
  - characterName: string

RegionCharacterInventory:
  > Wird im Region verwaltet und hier mit Backpack ausgetauscht, wird nicht vom client abgerufen
  - (items: Item[])

RegionChest:
  - chestId: string
  - worldId: string
  - chestSecretHash: string
  - (items: Item[])

CharacterSkills:
  > Werden nur vom World Server geladen
  - skills: Map<skillName, skillLevel:number>
    - intelligence: number
    - charisma: number
    - speed: number
    - experience: Map<skillName, experiencePoints:number>
    - skillsNextLevel: Map<skillName, experiencePointsForNextLevel:number> // evtl. virtual vom server, nicht im storage
    - level: number
    - maxVitals: Map<vitalName, vitalLevel:number>
      - health: number
      - mana: number
      - stamina: number
      - strength: number
      - dexterity: number
    - altCoins: Map<currencyName, amount:number>

SettingsSet:
  - settings: Map<settingName, settingValue:any>
    - controlSettings: Map<actionName, keyBinding>
        (settingName: z.b. PC, XBOX, IPad, Mobile)

CharacterInfo:
  > Werden vom Client und World Server geladen
  - characterSettings: Map<settingName, settingValue>
  - model: string
  - modelParams: Map<paramName, paramValue>
  - wearing: Map<enum, Item>
  - backpack: Item[]
  - shortcuts: Map<slot:string, Item>


@Deprecated
 
- GovenorInfo
  - Url zur API
  - public key
- PlayerInfo
  - UserId 
  - Waehrung
  - CharacterInfo
    - Ausstattung + Inventar
    - Rucksack
    - Waehrung der Platform
    - CharacterSettings - koennen vom Player 
      - model / modelParams
    - system:CharacterSkills
      - max leben, max mana, speed, strength, intelligence, dexterity, charisma ....
  - PlayerSettings - Controls settings
- WorldInfo
  - WorldId
  - API Path
  - Websocket Path
  - Controls Path
  - dimensions
  - public key

Neue Pfade

Assets:
- @G:/assets/{assetId} - geht nach Governor
- @S:/assets/{assetId} - geht nach Shop
- assets/{assetId} - geht nach World



Was alles gespeichert werden muss:

- Shortcuts des Characters
- Ausstattung des Characters (was hat er wo (slot) an) -> Item Eigenschaft: wo (welcher slot) kann man das anziehen
  - Ausstattung mappt teilweise mit Shortcuts
- Welche Arten von Vortbewegung sind erlaubt in der Welt
- Wo sind die EntryPoints der Welt: key:area, default ist 'main' EntryPoint
- Default backboard in der WeltInfo
- SplashScreen Image links(s) in der WeltInfo
- DeathAmbiente Sound in der WeltInfo
- SplashScreenSound in der WeltInfo (?)


## Ids und pfade

- Ids: \[a-zA-Z0-9_-\]{1,64}
- Pfade: /[a-zA-Z0-9_-\]{1,64}(/[a-zA-Z0-9_-]{1,64})*
- WorldCoordiante Kurz: W:regionId/worldId\[$subWorld\]\[:branch\]
- WorldCoordiante Lang: U:galaxyId/regionId/starId/celestialBodyId/worldId\[$subWorld\]\[:branch\]
- starId: galaxy coordinate: komma getrennte werte einer funktion, die den stern im galaxie gitter beschreibt
- planetId: planetNr von innen\[,mond Nr von Innen\]

## Tokens und Access

- Login mit credentials am Universe Server: Access Token (15 Minuten gültig) - nur im Memory
- Im Universe Server ein Refresh Token holen (30 Tage gültig) und in Session Cookie speichern
- Mit Refresh Token kann neues Access Token erstellt werden und neues Access Token
- Auswahl der Region/Welt/EntryPoint -> Universe lässt vom Region Server ein Visum Token ausstellen (5 Minuten gültig) - nur im Memory
- Mit dem Visum wird im World eine Session (Status prepared) erstellt und im Memory gespeichert - Session ist im Redis gespeichert wird nach 5 Minuten abgelaufen
- Sobald sich der Websochet an die session hängt wird diese als aktiv markiert und wird geloescht, wenn die Websocket disconnetced
- Session Id ist der access token kann als Bearer oder via url query parameter übergeben werden
  - Bearer sessionId:<sessionId>
  - sessionId=<sessionId> 
- Im Region Server kann sich auch mit der sessionId validiert werden, wird vom WorldServer
  abgefragt und alle 5 minuten aktualisiert

- SessionId kann jedes mal im redis direkt abgefragt werden
- SessionId: <regionId>:<worldId>:<expireTimestamp>:<uuid><uuid>
- expire ist 24 Stunden
- uuid ohne '-'

Ist die Session weg, muss am Unvierse Server ein neues Access Token erstellt werden
und ein neues Visum geholt werden. Danach Session ersellen.

## World EntryPoints und Visum

Beim erstellen eines Visums gibt es meherer Wege:

Vorher: 
- User login
- Asuwahl Region
- Auswahl Character

Visum erstellen:
- Universe fragt Region, Region hat gespeichert welche EntryPoints der Character schon hat
- Visum dafuer erstellen

Im Visum (wird alles an der Session gemerkt):
- Player
- Character
- WorldId
- EntryPoint
- ggf Ruckkehr-Punkt
- Signieryt von Region Server

Oder World Server stellt Visum direkt aus (z.b. bei World Wechsel / teleport)
- Mit dem Visum wird eine neue Session erstellt und im Redis gespeichert
- Bei Rückkehr zur main-Welt muss auch wieder ein Visum erstellt werden

## Zusammenfassung

- World Server domaene ist User und World und deren Location im Universe
- Region Server domaene ist Charaktere und alles was damit zu tun hat 
  (Backpack, Inventar) und deshalb auch alle Items (ohne position) und 
  welche Main Welten es gibt um diese zu verwalten
- World Server hat die domaene der Welt Zonen, Branches und Welt Daten (Blocks, Positionen der Items, 
  Entity), Welt Editing
- Region Server kennt User nur, um Rollen in der entsprechenden Region zu definieren
- World Server kennt keine User, Charakter+UserDaten sind als Player bekannt, aber nicht in der DB
