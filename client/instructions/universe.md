
# Universe Structure and Management

## Overview

Universe <- Quadrant <- Solar System <- Planet <- World

Universe -> Universe / Governor Server

Quadrant -> Quadrant Server
Solar System -> Quadrant Server (Meta Struktur)
Planet -> Quadrant Server (Meta Struktur)

World -> World Server

ggf. Universe Quadrant zum durchqueren des Universums

- Universe managed: User, Echtwährung (Governor), Quadranten Registry, Short Links zu Welten
- Quadrant managed: Metastruktur (Solar Systeme, Planeten), Welten Registry, Character Daten
  - Character Daten: Inventar, Rucksack, Alt-Waehrung, Vortschritt, Skills, Einstellungen
  - Eigene Asset (Texture, Model) und item Verwaltung uebergreifend
  - (Shop)
  - Character Access Points zu Welten (Bekannte Reisepunkte) - Werden von Welt an Quadrant gemeldet, Startpunkte fuer besucher uder Einladungen
- World managed: Welt Daten, Block Daten, In-Game Aktionen

- Transfer zwischen den Platformen wird spaeter moeglich sein
- Items sollen eine signatur des erstellers und des systems ahben, bei dem es erstellt wurde und erlaubt ist

- Universe hat einen Signatur Key fuer:
  - Access Tokens 
- Quadrant hat einen Signatur Key fuer:
  - Character Data Tokens

Access:
- Unverse Access Token
- Quadrant Access Token ? -> Unverse Access Token + Quadrant Info Claim
- World Access Token ? -> Unverse Access Token + Quadrant Info Claim + World Info Claim
- Visum - World Access Token ? - One Time Access to a single point in the World

Word Access:
- Login beim Universe -> universe access token
- Auswahl des Quadranten -> Quadrant abfrage von Access Points (Worlds) (mit such funktion nach Solar System, Panet, Name - Liste der SolarSystems und Planeten) -> Quadrant Access Point + Quadrant Access Token
  - Oder Short Link zu World -> Quadrant Access Point + Quadrant Access Token
  - Hier auch Rolle des Characters abfragen (owner, editor, moderator, player) und angeben mit welcher rolle eingelogt werden soll
- Access Points auf der Welt anzeigen (Name, Beschreibung, Bild, Rolle) -> Auswahl der Welt -> Visum wird vom Quadrant Server ausgestellt -> World Access Token + Visum

Visum:
- Der User/Character darf nicht einfach so in einer Welt irgendwohin reisen, er braucht ein visum, das einen Teleport an eine bestimmte stelle erlaubt
- Wann visum: 
  - Beim Betreten, wird vom Quadrant Server ausgestellt
  - Beim Teleport innerhalb der Welt, wird vom World Server ausgestellt ????
  - Beim Teleport mit einem Teleporter, auch in eine andere Welt, wird vom Quadrant Server ausgestellt ????
- Visum beinhaltet:
  - World Access Token
  - Ziel Position
  - Zeitliche Begrenzung des Logins (nicht des aufenthalts in der Welt)

## Datenstrukturen

UniverseInfo:
  - userId

UniverseCoins:
  - amount

QuadrantInfo:
  - QuadrantId: string
  - ApiUrl: string
  - characters

QuadrantCharacterInfo:
- userId: string
  - characterId: string
  - characterName: string

QuadrantCharacterInventory:
  > Wird im Quadrant verwaltet und hier mit Backpack ausgetauscht, wird nicht vom client abgerufen
  - (items: Item[])

QuadrantChest:
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
