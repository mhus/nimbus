## Block-Model

Frage: Die definition wie ein Block ist soll eigentlich individuell fuer jeden Block definiert werden.
Macht es sinn Block Typ zu entfernen und immer alle Parameter an der Block Instanz zu haben?
```text
 Aktuelles System (BlockType + BlockInstance):

  Vorteile:
  - ✅ Speicher-Effizient: Millionen von Grass-Blöcken teilen sich eine BlockType-Definition
    - Pro Block: nur 2 bytes (Uint16 ID)
    - Chunk mit 32×256×32 = 262,144 Blöcken = ~512 KB
  - ✅ Netzwerk-Effizient: Registry wird einmal synchronisiert, dann nur IDs übertragen
  - ✅ Standard in Voxel-Games: Minecraft, Minetest nutzen dieses Pattern
  - ✅ Einfache Updates: BlockType-Änderung betrifft alle Instanzen
  - ✅ Performance: Schneller Lookup, weniger Daten zu verarbeiten

  Nachteile:
  - ❌ Keine individuellen Properties: Jeder Grass-Block ist identisch
  - ❌ Limitierte Flexibilität: Spezielle Blöcke brauchen Workarounds

  Alternative: Alle Parameter pro Instanz:

  Vorteile:
  - ✅ Maximale Flexibilität: Jeder Block kann unique sein
  - ✅ Keine Type-ID nötig: Direkte Property-Definition

  Nachteile:
  - ❌ Speicher-Explosion:
    - Aktuell: 2 bytes/Block
    - Neu: ~50-100+ bytes/Block (shape, texture, material, options...)
    - Chunk: von 512 KB → 13-26 MB (25-50x mehr!)
  - ❌ Netzwerk-Overhead: Jeder Block = vollständige Definition übertragen
  - ❌ Render-Performance: Mehr Daten zu parsen beim Mesh-Building
  - ❌ Komplexität: Schwerer zu optimieren und zu cachen

  Empfohlener Hybrid-Ansatz:

  // BlockType bleibt für Standard-Properties
  interface BlockType {
    id: number;
    name: string;
    shape: number;
    texture: string | string[];
    // ... standard properties
  }

  // BlockInstance mit optionalen Override-Properties
  interface BlockInstance {
    typeId: number;              // Standard: Referenz zu BlockType
    metadata?: {                 // Optional: Individuelle Properties
      customTexture?: string;
      rotation?: number;
      customColor?: [r, g, b];
      nbt?: Record<string, any>; // Arbitrary data (Minecraft-style)
    };
  }

  Vorteile des Hybrid-Ansatzes:
  - Standard-Blöcke: Nur 2 bytes (99% der Blöcke)
  - Spezielle Blöcke: Metadata für Customization (Chests, Signs, etc.)
  - Chunk-Daten: Haupt-Array (Uint16) + Sparse Metadata-Map
  - Speicher: Effizient für normale Blöcke, flexibel für spezielle

  Aktuelle Code-Stelle für Metadata:
  // ChunkData hat bereits metadata!
  interface ChunkData {
    data: Uint16Array;          // Block IDs
    metadata?: Uint16Array;     // ← Schon vorhanden, aber wenig genutzt!
  }

  Meine Empfehlung:

  Behalte BlockType + BlockInstance, aber:
  1. Nutze metadata für individuelle Properties
  2. Erweitere ChunkData.metadata für flexible Block-Daten
  3. Implementiere Metadata-Editor im Block Editor

  Das gibt dir Flexibilität wo nötig ohne Performance zu opfern.

  Soll ich das Metadata-System ausbauen statt BlockTypes zu entfernen?
```
Frage: Wenn ich Block-Instanz erweitere um ein Modfierer-Objekt, dann kann ich einzelne Eigenschaften
fuer jeden Block individuell setzen. Wie kann ich das machen?
---
Ich moechte an der Block-Instanz die gleichen oder mehr Parameter wie am Block-Type definieren koennen.
- An der Block Instanz ein Modfierer Objekt anhaengen das auch null sein kann.
- Jeder Parameter am Modfierer Objekt kann individuell gesetzt werden.
- Die Daten des Modifier Objekts muessen serialisierbar sein und per Netzwerk übertragen werden.
- Beim Laden im Clienten soll an jeder Block Instanz das einsprechende Block-Typ Objekt angehaengt werden, dieses wird
  überall benutzt (kein separaten lookup des Block-Typ Objekts).
- Wurde ein Modifier an der Instanz angehaengt, wird nicht das originale Block-Typ Objekt benutzt, sondern ein neues Objekt
  das aus dem Block-Typ Objekt kopiert wurde und die Modifier Eigenschaften ueberschreibt.
- Der Generator muss aktuell noch keine Modified Block Instanzen generieren.
- Der Editor muss angepasst werden, dass er Modifier Objekte an Block-Instanzen bearbeiten kann.
---
Beim speichern des Chunks muss das Modfierer Objekt auch gespeichert werden.
---
Das Formular des Block-Editor muss neu erstellt werden:
- Block ID Typ Änderbar (drop down)
- edge offsets: Aufklappbar, wenn zu geklappt wird ist edgeOffset nicht gesetzt 
  - Für jede Ecke ein Offset (x, y, z) von -127 bis 128
- Modifier: Aufklappbar, wenn zu geklappt wird ist modifier nicht gesetzt
  - displayName
  - shape (drop down von BlockShape)
  - texture (drop down + freie eingabe)
  - solid (checkbox)
  - transparent (checkbox)
  - unbreakable (checkbox) 
  - rotation (drop down)
  - facing (drop down)
  - color (erstmal nur als hex eingabe)
  - scale (x, y, z) float werte
  - Block Options: Aufklappbar, wenn zu geklappt wird ist blockOptions nicht gesetzt
    - solid (checkbox)
    - opaque (checkbox)   
    - transparent (checkbox) 
    - material (drop down von BlockMaterial)
    - fluid (checkbox)
    - fluidDensity (float)
    - viscosity (float)
---
Die Felder fuer den edgeOffset können jeweils für jede Ecke X Y Z in eine Zeile gelegt werden.
---
Im Editor die Felder fuer modifyer scale X Y Z koennen auch in eine Zeile gelegt werden.
---
Die Funktion 'Neuer Block' funktioniert nicht. Die funktion muss noch ueberarbeitet werden.
Wenn der Button 'Neuer Block' gedrueckt wird, soll der 'Selekt for new' modus aktiv werden.
Dabei koennen nur freie (air) Positionen selektiert werden. Die stelle wird entsprechend
wie bei select mit einem rahmen markiert. Wenn nun 'Cancel' gedrueckt wird, wird wieder in den
normalen Select Modus gewechselt. Wird der Editor mit dem Key '.' aktiviert und ein Air Block
ist ausgewählt, wird automatisch an dieser stelle ein Block erstellt.

Die umsetzung der beiden selekt modus ueberlasse ich dir.
---
Die suche soll direkt vor der kamera im abstand 1 einen air block selektieren, wenn dort ein Block bereits ist (nicht air), verschwindet wird die selektor umrandung rot, weil dort kein neuer block erstellt werden kann.
---
Der Abstand fuer einen Air Block sollte doch lieber 3 bis 1 sein. Das heisst es wird ein Freier Block bei 3,dann 2 dann 1 gesucht.
---
Nenne die Funktion Cancel im Editor um in 'Accept'. Wenn im Editor Modul ESC Key gedrueckt wird, soll die gleiche Funtkion wie bei Accept ausgefuehrt werden.
---
Programmatischer Swtch in den Pointer Lock funktioniert nicht, deshalb ausbauen bei Accept und New Block.
Bei New Block, Apply All, Revert All, Delete bitte auch ausbauen.
---
Reject All muss vermutlich den chink neu vom Server laden und rendern.
---
'Apply All' soll per Netzwerk die geänderten Bloecke (new, modify, delete) an den Server senden. Der Server baut das dann in den chunk ein.
- Netzwerk protokoll pruefen und erweitern, liste von Block-Instanzen senden
- Im Client diese funktion nutzen und die Changed-Block liste clearen.
- Im Server die Bloecke einbauen in den Chunk (wird automatisch gespeichert) und neu an alle clients schicken.


# Next Steps

===

## Entity

Es sollen zusaetzlich zu den Bloecken Entities (Entity) hinzugefuegt werden. Entities sind mobil koennen jederzeit auftauchen, sich bewegen und verschwinden.

Darstellung und Volumen:
- Entities haben ein Model, das gerendert wird. Eine .babylon odel .glb Datei kann aus den assets geladen werden.
- Entities haben eine Position und eine Rotation.
- Entities werden von einem Block-Raster umgeben, belegen also mehrere Block-Positionen. 
  - Diese ändert sich wenn sich das Object bewegt/rotiert.
  - Dieses Object-Block-Raster sind die physicalischen grenzen in der collision detection
- Entities haben Animationen definiert, die aktiviert und abgespielt werden (z.B. laufen)

Netzwerk:
- Ueber das Netzwerk registriert sich der Client an chunks. Immer mit einem befehl an einer liste von chunks. Er ist dann nur an diesen registriert, damit entfällt eine deregistrierung.
  Es kann eine Liste von Objekten gesendet werden (batch).
- Der Server schickt dem Client wenn ein Entity auftaucht (incl. aktuelle animation, rotation und position), der client laed das modell aus den assets und zeigt das Object an.
- Der Server schickt dem Client wenn sich ein Entity ändert (aktuelle animation, rotation und position). Es kann eine Liste von Entities gesendet werden (batch).
- Der Server schickt dem Client wenn ein Entity verschwindet. Es kann eine Liste Entities Objekten gesendet werden (batch). Das Entity wird im Client nicht mehr angezeigt. Ein Cache-System hält 
  das Entity noch einige zeit bevor das model verworfen wird.
- Dr Server schickt dem Client wohin sich ein Entity bewegen soll (route). Der Client laeuft die route ab. 
!!! Geht nicht, weil viele clients: Bei collision oder ende der route wird ein event an den server geschickt.

Server Demo:
- Der Server soll eine Liste von seagulf.glb ueber die landschaft laufen lassen. alle in der naehe von punkt (0/0) mit radius 100 Blocks.
- Der Server berechnet eine route schickt diese an clients, die sich registriert haben. 



> - Sprites sind auf einer Block Position
> - Haben eine Ausdehnung in Bloecken, dass kann eine Beliebeige, nicht sichtbare Anordnung von Bloecken sein, innen drin ist das Sprite
> - Die Ausdehnung in Bloecken kann Collidieren
> - Sind animiert
> - Haben vermutlich ein Mesh Model
> - Vom server koennen Bewegungs-Routen geschickt werden, die werden dann abgelaufen,
    >   - bei Collision abbruch, event an Server schicken
>   - bei ende der bewegung event an server schicken
>   - Routen koennen in einer queue gestackt werden, jeweils am ende ein event, dann kann der server schon das die naechste route schicken

## Code Optimieren

[x] Baue ein TransferObjekt das du add*Block() uebergibst, in dem Objekt sind alle wichtigen Referenzen und das Obejkt kann leicht erweitert werden
[x] Erstelle aus ChunkRenderner.ts eine class ShapeRenderer und fuer jeden shape Typ eine Ableitung. Jedes in eine eigene ts datei. 
~~[ ] Datei ChunkRenderner.ts sinnvoll weitere teile auslagern~~
[x] Datei BlockEditor.ts sinnvoll teile auslagern und so komplexitaet reduzieren.

# Backlog

## Gruppen

> Ein Block soll in einer Gruppe sein können, UUID/String
> Alle in einer Gruppe können gemeinsam verschoben werden
> Modell in JSON erstellen
> Bei Interaktion leuchten alle in der Gruppe

> Verschiedene Arten von Modellen: 
> - Block Model: Liste von Blöcken und deren Anordnung und Konfiguration
> - Mesh Model: Liste von Meshes und deren Anordnung und Konfiguration

## Start und Login

> - Automatisch beim Server anmelden, Abfrage User/PW, Anzeige der moeglichen Eintrittspunkte
> - Automatischer Sprung in eine andere Welt, Weltraum Modus?

> Frage Wording: Planet -> Welt ? oder Planet (Welt) -> Kontinent/Ebene ? World -> Ebene (Plane)
> Frage Technik: Geht alles ueber EINEN Einstiegspunkt (volle Kontrolle) oder verbindet sich der Client direkt zu den Servern (mehr flexibel), ggf. Eine Zentrale Resourcenverwaltung (User, Items, Geld, Energie, ...)? 
> Frage Konzept: Energie als steuerndes Mittel einführen? Energie als gegepol zu Geld?

## Wasser

> Beim laden eines Chinks wird fuer jede XZ Column geprueft ob dort Wasser ist. Es wird sich die Ebenen (Y) und die Farbe gemerkt fuer diese Column
> Ist die Camera unter Wasser wird ein Wasser-Sphere um die Kamera gelegt in der Farbe des Wassers, simuliert wasser
> Im Walk modus und unter Wasser gibt es keine Gravitation mehr / Gravitation ist sehr gering / Ist die navigation wie im Flug-Modus (tauchen)

## Klettern

> Lauft man im Walk Modus an einen Block der 'Kletterbar' als Eigenschaft hat, bewegt man sich beim weiter laufen automatisch nach oben.
> Die Gravitation ist deaktiviert, solange man an einem Kletter-Block ist.
> - Pruefen ob das nur fuer seitliche Blocks gild. 
> - Wie ist das, wenn das der letzte Block, oben ist?

## Server Networking verbessern

[ ] Kann das senden der Chunks nicht kompakter geschehen, 
- indem daten die default sind weggelassen werden
- indem namen kuerzer oder durch ids ersetzt werden
- indem mit einer einfachen komprimierung gearbeitet wird

[ ] Die BlockTypen muessen vom Server geladen werden
- Ueber den gleichen weg wie die Assets aber dynamisch?

## Strukturierung von Block Eigenschaften

> Keine Block Optiosn mehr
> In Block Modifiers Dinge, die das Aussehen beeinflussen
> In BlockMetadata Dinge die das Verhalten beeinflussen
> BlockModifers sind: Material Eigenschaften, Positions Eigenschaften, Animations Eigenschaften
> BlockType
> Alle BlockMetadata sind auch im BlockTyp definiert

## Block Editor Metadata

[x] Aus BlockMetadata kann 'rotationAxis', 'rotationStep',' 'facing', 'state' entfernt werden
[x] Name soll von BlockModifier in BlockMetadata verschoben werden
[x] BlockMetadata soll auch im Editor als separate Aufklappbox "Metadata" bearbeitbar sein. displayName ist dann dort im formular.
[x] Group Id (string) soll in BlockMetadata angelegt werden, im editor bearbeitbar sein.
[x] Block-Info erweitern um BlockMetadata, BlockModifier, BlockType informationen, zu welchem chunk der block gehört
[ ] Facing Dropdown zeigt im Button immer ... anstelle des wertes an, aber das Element ist breit genug um den text anzuzeigen
[ ] Die Wind Parameter Leafiness und Stability sollen in eine Zeile gelegt werden.
[ ] Die Wind Parameter Lever Up und Lever Down sollen auch in eine Zeile gelegt werden.
[ ] Das Scrolling im Editor und auch Dropdown ist zu schnell, kann man das verbessern?
---
[ ] Erstelle am Editor einen Close Button (evtl ein Cross Icon) mit dem der Editor deaktiviert und select modus ausgeschalten wird.
