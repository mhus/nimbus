
# Feedback Client -> Server

## Block Collision

Am BlockModifier soll es einen parameter unter Physics geben, der anweisst, das bei collision ein event an den server geschickt wird.
- collisionEvent: true/false
- Im BlockEditor unter Physics hinzufügen
- Im PhysicsService auf das Feld reagieren und ein event via Network verschicken "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'collision'

## Block Interaction

Wenn im SelectorService der Modus Interaction ist, soll bei einem Klick (right/left/middle) auf einen Block ein event an den server geschickt werden.
- Im SelectorService auf den Klick reagieren und ein event via Network verschicken "Block Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'click'
- Data: {clickType: 'right'/'left/middle'}

## Entity Interaction

- Erweitere im SelectionService im MODUS INTERACTION die Klick-Events auf Entities, die das flag 'interactive' auf true haben.
- Bei einem Klick (right/left/middle) auf eine Entity soll ein event an den server geschickt werden.
- Im SelectorService auf den Klick reagieren und ein event via Network verschicken "Entity Interaction (Client -> Server)" in client/instructions/general/network-model-2.0.md
- Action: 'click'
- Data: {clickType: 'right'/'left/middle'}

## Items 

[?] Es soll ein Item system geben. Items sind Billboard Bloecke, die aber dynamischer verwaltet werden muessen. Deshalb werden sie als separate liste bei den chunks mit uebergeben.
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

[?] Im server ein register von Items anlegen und diese beim ausliefern der chunks in 'i' mitliefern.
- Ein Template fuer Items im Blocks ist unter client/packages/test_server/templates/item_block.json
- Wenn die liste von items geandert wird, hinzufuegen/loeschen/verschieben, dann muss der server ein "Item Block Update (Server -> Client)" command an alle clients schicken, die diese chunks aboniert haben.
- Erstelle im server ein Command mit dem man die items in der liste manipulieren kann (add, remove)

## Fights - Attack Palette

## Groups

