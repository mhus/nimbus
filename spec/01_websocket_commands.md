
# Web Socket Kommandos spezifikation

## Allgemeine Struktur eines Kommandos

Kommandos werden wie in einem Chat verwendet, um Aktionen auszuführen oder Informationen zu erhalten. Diese Spezifikation beschreibt die verfügbaren Befehle und deren Verwendung.

Jedes Kommando besteht aus einem Schlüsselwort, gefolgt von Parametern, die durch Leerzeichen getrennt sind. Die Parameter können optional sein und hängen vom jeweiligen Kommando ab.

## Client-Bridge Kommandos

### `login` - Anmeldung am Server

Usage: `login <username> <password>`

* Parameter: 
  - `username` - Der Benutzername des Clients.
  - `password` - Das Passwort des Clients.

* Rückmeldung: `login_success <JWT token>` oder `login_failure`

Meldet den Client am Server an. Bei erfolgreicher Anmeldung sendet der Server ein `login_success`-Event mit einem JWT-Token, das für die Authentifizierung bei weiteren Anfragen verwendet werden kann. Bei fehlerhafter Anmeldung sendet der Server ein `login_failure`-Event.
Das JWT-Token wird an der WebSocket-Verbindung vermerkt und bei weiteren Anfragen verwendet, um den Client zu authentifizieren.


### `logout` - Abmeldung vom Server

Usage: `logout`

* Rückmeldung: `logout_success` oder `logout_failure`

Das hinterlegte JWT-Token wird gelöscht und der Client wird vom Server abgemeldet. Bei erfolgreicher Abmeldung sendet der Server ein `logout_success`-Event. Bei fehlerhafter Abmeldung sendet der Server ein `logout_failure`-Event.

### `get_worlds` - Abrufen der verfügbaren Welten

Usage: `get_worlds <filter>`

* Parameter: 
  - `filter` - Ein optionaler Filter für die Welten. Kann leer sein, um alle Welten abzurufen, oder ein Suchbegriff, um nur bestimmte Welten zu erhalten.

* Rückmeldung: `worlds <world ids> <world names>`

### `get_world_info` - Abrufen von Informationen zu einer Welt

Usage: `get_world_info <world id>`

* Parameter: 
  - `world id` - Die ID der Welt, für die Informationen abgerufen werden sollen. Eine Welt-ID besteht aus einer UUID.

* Rückmeldung: `world_info <world id> <world name> <world description> <world owner>`

### `use_world` - Wechseln der Welt

Usage: `use_world <world id>`

* Parameter: 
  - `world id` - Die ID der Welt, zu der gewechselt werden soll. Eine Welt-ID besteht aus einer UUID.

* Rückmeldung: `world_changed <world id>` oder `world_change_failure`

Holt die Host und den Port des World-Servers für die angegebene Welt und 
baut eine WebSocket-Verbindung zu diesem Server auf. Auf die Websocket wird ein `use <world id>` Kommando gesendet, um die Welt dort festzulegen.

Es kann immer nur eine Welt gleichzeitig verwendet werden. Der Server sendet ein `world_changed`-Event, wenn der Wechsel erfolgreich war. Bei fehlerhaftem Wechsel sendet der Server ein `world_change_failure`-Event.

## World-Bridge Kommandos

### `use` - Wechseln der Welt

Usage: `use <world id>`

* Parameter: `world id` - Die ID der Welt, zu der gewechselt werden soll. Eine Welt-ID besteht aus einer UUID.

Da ein world-server mehrere Welten verwalten kann, kann der Client mit diesem Kommando die Welt wechseln. Der Server antwortet mit `world_changed` 
und sendet ab nun nur noch Events für die neue Welt. Ausserdem schickt er intern immer die id der aktuellen Welt mit, damit der Client diese nicht selbst speichern muss.

### `register_cluster_updates' - Registrierung für Cluster-Updates

Usage: `register_cluster_updates <cluster ids>`

* Parameter: `cluster ids` - Eine Liste von Cluster-IDs, die der Client abonniert. Eine Cluster ID 
  besteht aus `x`x`y`, Beispiel `3x5`. Die Liste ist kommagetrennt, Beispiel `3x5,4x6,2x1`.

Registriert den Client für Updates zu den angegebenen Clustern. Der Server sendet dann alle Änderungen an diesen Clustern an den Client.
Alle früheren Registrierungen werden dabei gelöscht. Ist die Liste der Cluster-IDs leer, werden alle Registrierungen gelöscht.

