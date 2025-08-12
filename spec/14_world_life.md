
# World Life Service

## Einführung

Der World Life Service ermöglicht die Verwaltung
und Simulation von Lebewesen und deren Interaktionen
in der Spielwelt. Dies umfasst sowohl NPCs (Nicht-Spieler-Charaktere)
als auch Tiere, Pflanzen und andere Lebewesen.

Auch PCs (Spieler-Charaktere) des Users werden hier persistiert 
und verwaltet.

## Allgemeine Eigenschaften

Lebewesen können attakieren und attackiert werden.

Sie können auch sterben, und haben
einen Lebenszyklus, der Geburt, Wachstum,
Fortpflanzung und Tod umfasst.

Lebewesen können durch ein Chat-System
kommunizieren, und haben oft auch
eine Art von Intelligenz oder Bewusstsein.

Lebewesen können sich in der Welt bewegen,
interagieren und auf Ereignisse reagieren.

Bewegung und Kampf werden durch den
World Physics Service unterstützt.

## Bewegung

Alle aktiven Lebewesen werden im World Terrain Service
als dynamische Sprites verwaltet. Beim starten
werden Sie im World Terrain Service registriert falls
sie nicht bereits existieren.

Bewegungen werden über den World Physics Service
verwaltet, der Kollisionsabfragen und physikalische
Simulationen durchführt.

## Lifecycle

Lebenszyklen werden durch verscheidene Module
im World Life Service verwaltet. Welches Modul
für welches Lebewesen zuständig ist, hängt
von der Art des Lebewesens ab.

Lebenszyklen werden durch Cronjobs oder Ereignisse
gesteuert.

## Aktive Bewegung

Tiere und NPCs können sich aktiv in der Welt bewegen.
Die Bewegungen werden durch Threads gesteuert, jedes
Bewegliche Lebewesen hat einen eigenen virtual Thread,
der die Bewegungen und Interaktionen steuert.

Mehrere Instanzen des Services müssen sich coordineren, 
damit die Threads nicht in Konflikt geraten.

Dies wird über die Datenbank organisiert. Im Feld `instance`
der JPA-Entity wird die ID der Instanz gespeichert. 
Die Instanz muss regelmäßig das Feld `instanceTimeout` aktualisieren,
um anzuzeigen, dass sie noch aktiv ist. 

Wenn eine Instanz das Feld `instanceTimeout` nicht mehr aktualisiert wird, 
wird die Being als inaktiv betrachtet und ist verweist.

Alle instancen prüfen regelmäßig, ob sie
verweiste Instanzen finden, und übernehmen
dann die Verantwortung für die Lebewesen dieser Instanz.

Lebewesen können auc abgegeben werden, z.B. wenn die
Verteilung nicht ausgeglichen ist. Dann wird das Feld `instance` 
und `instanceTimeout` auf null gesetzt, und die Lebewesen
werden von einer anderen Instanz übernommen.

Was wird benötigt:

* Es wird ein BeingsRegistrationService benötigt, der die
  Lebewesen verwaltet. Der Service sollte Lebewesen registrieren und unregistrieren können.
  * Es registriert die Lebewesen im BeingsManagementService.
  * Ein Job der regelmäßig prüft ob verweiste Instanzen
    gefunden werden, und diese dann übernimmt. Nur enabled Lebewesen werden registriert.
  * Ein Job der regelmäßig prüft, ob Lebewesen die von der aktuellen
    Instanz verwaltet werden, von einer anderen Instanz übernommen
    werden können, und diese dann abgibt. Der Wert in `instanceTimeout` wird
    auf null gesetzt. Das wird nur gemacht, wenn andere Instanzen weniger als 3/4 der eigenen 
    Lebewesen haben (neue Instanzen). PC werden nicht abgegeben.
  * Werden mehr als eine definierte Anzahl (application properties, default 500) von Lebewesen
    verwaltet, wird jede 20 Minuten ein nicht PC oder NPC Lebewesen
    abgegeben, bzw. die gesamte Gruppe, um die Last zu verteilen.
  * Bei der Übergabe von Lebewesen werden immer alle Lebewesen
    mit der gleichen Gruppen-ID aufgenommen oder abgegeben. 
    Wenn die Gruppen-ID gesetzt ist. 
  * Prüfen ob bei verwalteten Gruppen ein neues Lebewesen
    hinzugefügt wurde. Dieses Registerieren.
  * Prüƒen ob ein verwaltetes Lebewesen entfernt oder disabled wurde. Dieses Unregisterieren.
* Ein BeingsManagementService wird benötigt.
  * Registrierte Lebewesen werden hier verwaltet. Für jedes Lebewesen
    wird ein Thread gestartet, der die Bewegungen und Interaktionen steuert.
  * Er nuztzt den LifecycleService um einen `Step` zu erzeugen, der die
    Bewegungen und Interaktionen steuert.
  * Zwischen den Steps wird eine Pause eingelegt,
    um die CPU nicht zu überlasten. Die Pause ist in den Properties des Lebewesens oder in der Species definiert. 
    Default ist 200ms.
* LifecycleTypen werden als Spring Services
  implementiert. Es wird ein LifecycleService benötigt,
  * Ein interface für den lifecycleType, der die verschiedenen
    Lebenszyklen verwaltet. Z.B. für Pflanzen, Tiere
  * Ein einfacher LifecycleService für Pflanzen wird benötigt,
    der die verschiedenen Pflanzenarten verwaltet. Er macht nichts mit den pflanzen.
  * Ein einfacher LifecycleService für einzelgänger Tiere wird benötigt,
    der die verschiedenen Tierarten verwaltet. Er lässt die Tiere langsam in ihrem definierten
    `area` herum laufen. Falls das Tier stirbt wird es an einer zufälligen Stelle in der `area` wiederbelebt.
  * Ein einfacher LifecycleService für Gruppen Tiere wird benötigt,
    der die verschiedenen Tierarten verwaltet. In den properties der Species ist definiert
    wieviele Tiere in einer Gruppe sind. Default ist zwischen 5 und 15.
    Er lässt die Tiere langsam in ihrem definierten `area` herum laufen. Manchmal wird geprüft ob ein Tier
    hinzukommen sollte, dann wird ein neues Tier in der Gruppe erzeugt. Falls ein Tier stirbt, wird dadurch 
    automatisch ein neues Tier in der Gruppe erzeugt.
  * Eine einfache LifecycleService für NPCs wird benötigt,
    der die verschiedenen NPC-Arten verwaltet. Er lässt die NPCs langsam in ihrem definierten
    `area` herumlaufen.
  * Ein einfacher LifecycleService für PCs wird benötigt,
    der die verschiedenen PC-Arten verwaltet. PCs werden von aussen gesteuert.

## Arten (Species)

Lebewesen können in verschiedene Arten
unterteilt werden, die jeweils
spezifische Eigenschaften und Verhaltensweisen haben.

### Species-Datenmodell

```json
{
  "id": "string",
  "name": "string",
  "type": "string", // z.B. "animal", "plant", "npc", "pc"
  "description": "string",
  "lifecycleType": "string", // z.B. "seasonal", "annual", "perennial" - wird durch verschiedene Module verwaltet
  "properties": {
    "key": "value" // z.B. "intelligence": "high", "aggression": "low"
  }
}
```

### Species-JPA-Entity

```text
"world": String,
"name": String,
"type": String // z.B. "animal", "plant", "npc", "pc"
```

## Beings

Beings sind eine allgemeine Kategorie von Lebewesen,
die alle oben genannten Arten umfassen.

### Beings-Datenmodell

```json
{
  "id": "string",
  "name": "string",
  "type": "string", // z.B. "animal", "plant", "npc", "pc", redundant mit Species
  "description": "string",
  "owner": "string", // ID des Besitzers (z.B. Spieler) bei PCs
  "attributes": {
    "key": "value" // z.B. "intelligence": "high", "aggression": "low"
  },
  "properties": {
    "key": "value" // z.B. "isHostile": "true", "canReproduce": "true"
  },
  "health": {
    "current": 100,
    "max": 100
  },
  "age": 0, // Alter in steps
  "createdAt": "2023-10-01T00:00:00Z", // Zeitpunkt der Erstellung
  "updatedAt": "2023-10-01T00:00:00Z", // Zeitpunkt der letzten Aktualisierung
  "speciesId": "string", // ID der Species, zu der das Being gehört
  "sprite_reference": "string", // Sprite ID im Terrain Service
  "group": "string", // z.B. ID "pack", "herd", "swarm"
  "position": { // Nächste Start-Position in der Welt
    "level": 0,
    "x": 0,
    "y": 0,
    "z": 0
  },
  "area" : { // Bereich in der Welt, in dem sich das Being bewegen kann
    "level": 0, // z.B. 0 für die Grundebene
    "x": 0,
    "y": 0,
    "width": 10,
    "height": 10
  },
}
```

### Beings-JPA-Entity

```text
"world": String,
"id": String,
"type": String, // z.B. "animal", "plant", "npc", "pc"
"name": String,
"group": String, // z.B. ID von "pack", "herd", "swarm"
"enabled": boolean, // ob das Beings aktiv ist
"owner": String, // ID des Besitzers (z.B. Spieler)
"instance" : String, // ID der Instanz (z.B. Server-Instanz)
"instanceTimeout": Date, // Zeitpunkt, bis zu dem die Instanz dieses feld aktualisieren muss
```

## Pflanzen

Pflanzen sind statische Objekte, die in der Welt
ihren Ort selten wechseln. Sie können sich
jedoch wachsen. Bzw. haben einen Lifecycle, der
von der Jahreszeit und dem Alter abhängt.

Pflanzen können auch von NPCs oder PCs
geerntet oder gepflegt werden.

Pflanzen können auch attackiert werden, oder selbst
attackieren, wenn sie z.B. giftig sind.

## Tiere

Tiere sind dynamische Objekte, die sich in der Welt
bewegen und interagieren können. Sie haben
einen Lebenszyklus, der Geburt, Wachstum,
Fortpflanzung und Tod umfasst.

Tiere können auch von NPCs oder PCs gejagt
oder gezähmt werden.

Tiere können auch in Gruppen leben
und soziale Strukturen haben, wie Rudel oder Herden.

## NPCs (Nicht-Spieler-Charaktere)

NPCs sind Charaktere, die von der Spiel-Engine
kontrolliert werden und nicht von Spielern gesteuert werden.
Sie können in der Welt interagieren, kommunizieren
und auf Ereignisse reagieren.
NPCs können auch Quests oder Aufgaben vergeben,
die Spieler erfüllen können.

## PCs (Spieler-Charaktere)

PCs sind Charaktere, die von Spielern gesteuert werden.
Sie haben keinen Lebenszyklus.

Sie können in der Welt interagieren, kommunizieren
und auf Ereignisse reagieren. 

## API Endpunkte

### Species anlegen

**POST /world/life/species**

Rolle: `CREATOR`

Anlegen einer neuen Species.

### Species aktualisieren

**PUT /world/life/species/{id}**

Rolle: `CREATOR`

Aktualisieren einer bestehenden Species.

### Species löschen

**DELETE /world/life/species/{id}**

Rolle: `CREATOR`

Löschen einer bestehenden Species.

### Species abrufen

**GET /world/life/species/{id}**

Rolle: `USER`

Abrufen einer bestehenden Species.

### Alle Species abrufen

**GET /world/life/species**

Rolle: `USER`

Abrufen aller bestehenden Species. 
Paging: Es werden maximal 100 Species pro Seite zurückgegeben. Durch den Parameter `page` kann die Seite ausgewählt werden.

### Beings anlegen

**POST /world/life/beings**

Rolle: `CREATOR` für Tiere und NPCs, `USER` für PCs

Anlegen eines neuen Beings.

### Beings aktualisieren

**PUT /world/life/beings/{id}**

Rolle: `CREATOR` für Tiere und NPCs, `USER` für PCs wenn owner des PCs

Aktualisieren eines bestehenden Beings.

### Beings löschen

**DELETE /world/life/beings/{id}**

Rolle: `CREATOR` für Tiere und NPCs, `USER` für PCs wenn owner des PCs

Löschen eines bestehenden Beings.

### Beings abrufen

**GET /world/life/beings/{id}**

Rolle: `USER`

Abrufen eines bestehenden Beings.

### Alle Beings abrufen

**GET /world/life/beings**

Rolle: `USER`

Abrufen aller bestehenden Beings.
Paging: Es werden maximal 100 Beings pro Seite zurückgegeben. Durch den Parameter `page` 
kann die Seite ausgewählt werden.

### Beings für eine Species abrufen

**GET /world/life/species/{speciesId}/beings**

Rolle: `USER`

Abrufen aller Beings für eine bestimmte Species.
Paging: Es werden maximal 100 Beings pro Seite zurückgegeben. Durch den Parameter `page`
kann die Seite ausgewählt werden.

### Beings für eine Gruppe abrufen

**GET /world/life/groups/{groupId}/beings**

Rolle: `USER`

Abrufen aller Beings für eine bestimmte Gruppe.
Paging: Es werden maximal 100 Beings pro Seite zurückgegeben. Durch den Parameter `page` 
kann die Seite ausgewählt werden.

### Beings eines Users abrufen

**GET /world/life/users/{userId}/beings**

Rolle: `USER`

Abrufen aller Beings für einen bestimmten User.
Paging: Es werden maximal 100 Beings pro Seite zurückgegeben. Durch den Parameter `page` 
kann die Seite ausgewählt werden.

### Beings registrieren

**POST /world/life/beings/registration/{id}**

Rolle: `ADMIN`

Registrieren eines Beings im BeingsManagementService.

### Beings deregistrieren

**DELETE /world/life/beings/registration/{id}**

Rolle: `ADMIN`

Deregistrieren eines Beings im BeingsManagementService.

### Beings-Registrierung abrufen

**GET /world/life/beings/registration/{id}**

Rolle: `ADMIN`

Abrufen der Registrierung eines Beings im BeingsManagementService.

### Alle registrierten Beings abrufen

**GET /world/life/beings/registration**

Rolle: `ADMIN`

Abrufen aller registrierten Beings im BeingsManagementService.

### Beings-Registrierung für eine Species abrufen

**GET /world/life/species/{speciesId}/beings/registration**

Rolle: `ADMIN`

Abrufen aller registrierten Beings für eine bestimmte Species im BeingsManagementService.

### Beings-Registrierung für eine Gruppe abrufen

**GET /world/life/groups/{groupId}/beings/registration**

Rolle: `ADMIN`

Abrufen aller registrierten Beings für eine bestimmte Gruppe im BeingsManagementService.

## Kafka Topics

### Beings-Registrierung

**Topic: `terrain-life-beings-registration`**

Veröffentlicht Ereignisse, wenn ein Being registriert oder deregistriert wird.

### Species-Änderungen

**Topic: `terrain-life-species-changes`**

Veröffentlicht Ereignisse, wenn eine Species angelegt, aktualisiert oder gelöscht wird.

### Beings-Änderungen

**Topic: `terrain-life-beings-changes`**

Veröffentlicht Ereignisse, wenn ein Being angelegt, aktualisiert, gelöscht, enabled oder disabled wird.
Topic wird nicht bei Änderungen der Position verwendet.

