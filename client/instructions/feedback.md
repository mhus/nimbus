
# Feedback Client -> Server

## Block Collision

[x] Am BlockModifier soll es einen parameter unter Physics geben, der anweisst, das bei collision ein event an den server geschickt wird.
- collisionEvent: true/false
- Im BlockEditor unter Physics hinzufügen
- Im PhysicsService auf das Feld reagieren und ein event via Network verschicken "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'collision'

## Block Interaction

[x] Wenn im SelectorService der Modus Interaction ist, soll bei einem Klick (right/left/middle) auf einen Block ein event an den server geschickt werden.
- Im SelectorService auf den Klick reagieren und ein event via Network verschicken "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'click'
- Data: {clickType: 'right'/'left/middle'}

## Entity Interaction

[x] Erweitere im SelectionService im MODUS INTERACTION die Klick-Events auf Entities, die das flag 'interactive' auf true haben.
- Bei einem Klick (right/left/middle) auf eine Entity soll ein event an den server geschickt werden.
- Im SelectorService auf den Klick reagieren und ein event via Network verschicken "Entity Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'click'
- Data: {clickType: 'right'/'left/middle'}

## Items 

[x] Es soll ein Item system geben. Items sind Billboard Bloecke, die aber dynamischer verwaltet werden muessen. Deshalb werden sie als separate liste bei den chunks mit uebergeben.
Da sie billboards sind, habe sie sepaarte meshes die jeder zeit hinzu/ entfernt werden koennen.
- Items sind immer billboards (Y-Axis locked)
- Items haben immer eine unique ID
- Items haben immer eine ALPHA_TEST Material
- Items sind immer vom type 1 haben am block ein BlockModifier mit texture
- Es wird ein shape Type Item geben, der immer ein Billboard ist, aber speziell an die anforderungen von Item angepasst mit moeglichst wenigen parametern aber annderen default werten.

Chunk Erweiterung:
- Im Chunk Data Model wird es eine neue optionale liste 'i' geben.
- Im ChunkService wird diese liste in die liste der blocks eingefuegt. Nur an Air positionen duerfen items hinzugefuegt werden. Alle anderen werden nicht angezeigt

Network Erweiterung:
- Neues Command "Item Block Update (Server -> Client)" in client/instructions/general/network-model-2.0.md
- Die neuen Items werder im chunk UI aktualisiert
  - Items mit type=0 (AIR) werden entfernt, aber nur wenn ein Block type=1 an der position ist
  - Items mit type=1 werden hinzugefuegt/aktualisiert, aber nur wenn an der position ein Block type=0 (AIR) oder Item type=1 ist.
  - Alle anderen werden ignoriert

Erweiterung BlockMetadata:
- Es wird eine ID benoetigt
- Es wird ein Name benoetigt (z.b. hacke, schwert, etc)

[x] Im server ein register von Items anlegen und diese beim ausliefern der chunks in 'i' mitliefern.
- Ein Template fuer Items im Blocks ist unter client/packages/test_server/templates/item_block.json
- Wenn die liste von items geandert wird, hinzufuegen/loeschen/verschieben, dann muss der server ein "Item Block Update (Server -> Client)" command an alle clients schicken, die diese chunks aboniert haben.
- Erstelle im server ein Command mit dem man die items in der liste manipulieren kann (add, remove)

## Fights - Attack Palette - shortcuts

[x] werden die Tasten 1,2,3,4,5,6,7,8,9,0 gedrueckt, es wird ein shortcut event im system ausgelost.
- das event wird von SelectionService gefangen und die Aktion 'fireShortcut(nr)' im SelectionService ausgefuehrt.
- Die methode sucht ein selektierten block oder entity und senden ein event an den server via Network "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'fireShortcut'
- Data: {shortcutNr: nr}
- Optional gefundener Block oder Entity ID mit senden und die entfernung zum player. und die position dees blocks oder entity.
- Ausserdem wird die direction und pitch des players mit gesendet, damit der server weiss in welche richtung der player schaut.
- Position des players wird auch mit gesendet.
- der radius fuer den selector soll aus der PlayerInfo kommen. d.h. im player kann configuriert werden welche range er maximal gegner befeuern kann. selectionRadius

[?] Erweitern des Click events im Interaction Mode
- Wenn im SelectorService der Modus Interaction ist, wird bei einem Klick (right/left/middle) auf einen Block oder Entity ein event an den server geschickt.
- stelle clickType um auf die nummer der Maustaste, alle maustasten sollen unterstuetzt werden. Start bei 0.
- Wie bei den shortcut tasten sollen weitere informationen mit gesendet werden:
    - Optional gefundener Block oder Entity ID mit senden und die entfernung zum player. und die position dees blocks oder entity.
    - Ausserdem wird die direction und pitch des players mit gesendet, damit der server weiss in welche richtung der player schaut.
    - Position des players wird auch mit gesendet.
    - der radius fuer den selector soll aus der PlayerInfo kommen. d.h. im player kann configuriert werden welche range er maximal gegner befeuern kann. selectionRadius

[x] Shortcuts
Fuer die Shortcuts koennen dinge hinterlegt werden. Lege im PlayerInfo eine Map shortcuts an, die folgende Struktur hat:
- key: string (key0...key9, click1, click2, click3, slot0...slotN)
- value: ShortcutDefintiton
ShortcutDefintiton:
- type: 'block', 'attack', 'use'
- itemId: id des Items, das benutzt werden soll - bei block, attacke oder use
- pose: Pose, die aktiviert werden soll (optional)
- wait: wie lange vor der aktivierung gewartet werden soll (in ms)
- duration: wie lange die aktion dauern soll (in ms, optional) - in dieser zeit sind keine anderen aktionen moeglich

Default Aktion ist NONE

Erstelle ein command in engine mit dem ich die shortcuts setzen kann.

[x] Erweiterung Click und Shortcut Events
- Sende auch den aktuellen MovementStatus aus dem PlayerService mit
- Sende daten aus dem shortcut mit an den server: shortcutType, shortcutItemId

[?] Item erweitern
Erstelle in shared types eine ItemData definition, die folgende felder hat:
- block: block definition (Block) - wird aktuell als item verwaltet und versendet vom server
- parameters: map<string, any> - optionale parameter fuer das item
- description - neu

- Benutze im server zum verwalten der items diese definition. Die aktuellen Blocks sind in der neuen struktur in block wieder vorhanden.
- Passe das commando im server an

[?] Items aus server laden, einen REST Endpoint im server erstellen, der die items zurueckgibt.
GET /api/world/{worldid}/item/{itemid}
Gibt die Item definition zurueck.

[?] Darstellung der shortcuts im UI umsetzen
Im NotificationService wird es ein neues UI Element geben, das die aktuellen shortcuts anzeigt.
- Es wird ein ItemService benoetigt, der items voms erver laed und cached.
- Im NotificationService wird das UI Element erstellt und die shortcuts aus der PlayerInfo geladen.
- Items werden vom ItemService geladen.
- Das UI Element wird im unteren bereich angezeigt und zeigt nebeneinander die shortcuts icons/texturen in den items an.
- Die texturen koennen vom server via assets rute genutzt werden. Der ItemService gibt die url zurueck.
  - Leere slots werden leer angezeigt
  - Es gibt die slots 1...0 fuer die shortcut tasten Insgesamt 10 slots. die anzeige kann umgestellt werden, das andere slots angezeigt werden (click1, click2, click3, slot0...slotN)
  - Die umstellung erfolgt ueber einen parameter in NotificationService: keys, clicks, slots0 (0-9), slots1 (10-19), ....
  - Das element wird nru angezeigt, wenn ein parameter showShortcuts auf true gesetzt ist im NotificationService, by default ist das off
- Bei hover werden die details des shortcuts angezeigt (name, beschreibung, etc)
- Erstelle einen key shortcut auf 'T' um das UI element ein und auszublenden bzw zu rotieren keys, clicks, slots0, slots10, aus - wobei die slots nur angezeigt werden, wenn welche in diesem range definiert sind.
- Es reicht wenn der Shortcut im NotificationService ein toggleShowShortcuts() oder aehnlich aufruft und der NotificationService macht die darstellung 

[?] Im ItemService sollen die events fuer shortcuts abgefangen werden und die hinterlegte pose aktiviert werden.
- Wenn eine pose hinterlegt ist, wird diese aktiviert
- im PlayerService die currentPose overrulen dieser pose, solange die aktion dauert (dauer ist auch im item duration hinterlegt)

[x] Player haben mehrere status effekte die gleichzeitig aktiv sein koennen.
- Erstelle im PlayerService eine Liste von Status Effekten, Status Effekte sind im hintergrund Items, d.h. sie haben eine Item definition und koennen vom ItemService abgerufen werden.
- Erstelle in der engine ein command um Status Effekte hinzuzufuegen und zu entfernen.
- Im NotificationService sllen genauso wie bei den shortcuts die aktiven status effekte angezeigt werden. Die Darstellung ist die gleiche wie bei den shortcuts, nur das die effekte horizontal ueber dem shortcut element angezeigt werden.
- Dier Hintergrund von status effekten ist dunkelrot. Es werden nur die angezeigt, die aktiv sind. (nicht 10 wie bei shortcuts)
- Sind keine Effekte aktiv, wird die StatusEffekte Leiste nicht angezeigt.
- Bei hover werden die details des status effektes angezeigt (name, beschreibung, etc)
- Status Effekte koennen eine dauer haben, nach der sie automatisch entfernt werden. Die Dauer ist im item in duration hinterlegt. Die verwaltung uebernimmt der PlayerService.
- Kommt ein neuer effekt hinzu/geloescht/update wird ein event im PlayerService ausgelost, das der NotificationService abonniert und die darstellung aktualisiert.

[x] Player haben mehrere vitals (z.b. hunger, durst, stamina, leben, mana, etc)
- Erstelle in shared ein VitalsData type, er hat folgende felder:
  - type: string (z.b. hunger, durst, stamina, leben, mana, etc)
  - current: number
  - max: number
  - extended: number (optional, kommt auf max drauf)
  - extendExpiry: number (timestamp, optional, wann die erweiterung ablaeuft, dann wird die auf 0 gestellt)
  - regenRate: number (pro sekunde)
  - degenRate: number (pro sekunde)
  - color: string (hex color code fuer die anzeige im UI)
  - name: string (display name des vitals)
  - order: number (reihenfolge der anzeige im UI)
- Erstelle im PlayerService eine liste von VitalsData, key=type
- Erstelle im NotificationService eine UI darstellung der vitals
  - Die vitals werden vertikal am rechten bildschirmrand angezeigt
  - Jedes vital hat eine leiste, die den aktuellen wert anzeigt (current/max+extended)
  - Jedes vital ist eine horizontale leiste mit einer hoehe von 10px und einer breite von 200px bei extend wird die höhe auf den % wert der max in px erhoeht, maximal 50px - somit ist eine leise maximal 250px hoch
  - Die farbe der leiste wird aus dem color feld genommen
  - Die vitals werden nach order sortiert angezeigt
  - Bei hover werden die details des vitals angezeigt (name, current, max, extended, regenRate, degenRate)
  - Es gibt einen showVitals parameter im NotificationService, der die anzeige ein und ausschaltet, by default ist das on
  - Vitals werden dennoch angezeigt, wenn der current wert kleiner als max ist. d.h. wenn 'voll', werden die vitals nicht angezeigt
  - Die psoition der vitals bleibt beim ausblenden gleich, damit sich die anderen vitals nicht verschieben
- Erstelle ein command im server showVitals()
- Erstelle ein command im server um vitals zu updaten (add, remove, update)

[x] Wenn die shortcut Keys 1-0 gedrueckt werden (ShortcutInputHandler), soll im NotificationService geprueft werden, ob showShortcuts aktiv (nicht das overlay bei highlight, sondern wirklich showShortcuts) ist.
- Wenn ja, soll geprueft werden, welche shortcuts angezeigt werden (keys, clicks, slots0, slots10, etc)
  - es wird der angezeigt shotcut fuer 0-9 geholt und ausgefuehrt anstelle des normalen shortcut verhaltens
- Als event zum Server muss eine andere shortcutNr gesendt werden, z.b. 'slot0', 'slot1', etc anstelle von 0-9

[x] Die Erkennung von entity im SelectionService ist sehr eng, ist es moeglich entities mit etws kullanz zu erkennen? Auch wenn es nicht genau vor der Kammera ist?

[ ] Erstelle im NotificationService eine Methode um ein Bild zu flashen.
- flashImage(assetPath: string, duration: number, opacity: number)
- Das bild wird in der mitte des bildschirms angezeigt, startet klein und skaliert auf volle hoehe des bildschirms in der duration zeit.
- als asstPath dueren nur values mit der endung .png genutzt werden. Alle anderen werden ignoriert.
- Das bild wird mit der angegebenen opacity angezeigt.
- Nach der duration animation verschwindet das bild wieder.
- Baue ein Command in engine um das flashImage aufzurufen.
