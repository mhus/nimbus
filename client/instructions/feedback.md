
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

[?] Shortcuts
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

[ ] Erweiterung Click und Shortcut Events
- Sende auch den aktuellen MovementStatus aus dem PlayerService mit
- Sende daten aus dem shortcut mit an den server: shortcutType, shortcutItemId
