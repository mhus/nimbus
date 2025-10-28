## Block-Editor

Das Editor-Fenster soll ausgebaut werden. Der Editor braucht zwei Modi. Einen Anzeigemodus und einen Bearbeitungsmodus.
Beide werden durch zwei Tabs umgesetzt.
Im Block-Info-Tab werden immer alle Eigenschaften des selektierten Blocks angezeigt. Druecke ich die Taste '.' und der editor ist aktiv,
wird der Bearbeitungsmodus aktiviert, d.h. der Editor springt in den Block-Editor Tab, das Formular wird aktiv und übernimmt die Daten 
des gerde selektierten Blocks.

Der Block-Editor Tab zeigt ein Formular in dem der die Eigenschaften des Block bearbeitet werden koennen. Eine Änderung soll sofort in
der Welt angezeigt werden. Blöcke die angepasst wurden (neu, bearbeitet, gelöscht) werden in einer Liste vermerkt. Ein Apply-All Button im Editor
schickt alle geänderten Blöcke an den Server. Ein Revert-All Button macht alle Änderungen rückgängig (betroffene chunk werden neu vomserver geladen). 
Ein Revert-Block Button macht Änderungen am aktuellen Block rückgängig.

Der New-Block Button erstellt einen neuen Block. Wenn der Editor aktiv ist kann das auch mit dem Key ',' gemacht werden.
Der Neue Block wird dann sofort im Block-Editor Tab angezeigt.

Aufbau des Editor-Fensters:
- Toolbar
  - Revert-Block Button
  - (Spacer)
  - Cancel-Editor-Mode Button - Formulare werden inaktiv, sprung in den Block-Info Tab
  - Apply-All Button
  - Revert-All Button
  - Revert-Block Button
  - (Spacer)
  - New Block Button
  - (Spacer)
  - Delete Block Button
- Tabs (Block-Info, Block-Editor, Block-Liste)
- Block-Info
  - Listet wie bisher alle Block-Eigenschaften auf.
- Block-Liste (Block-ID, Block-Name, Block-Shape, Block-Material)
- Block-Editor
  - Block-Position (read only)
  - Block-ID
  - Block-Shape
  - Block-Material
  - edge offsets
  - (alle anderen Eigenschaften des Blocks)
```text
  1. Zuerst die grundlegende Tab-Struktur und Toolbar erstellen
  2. Dann den Edit-Modus und Change-Tracking hinzufügen
  3. Keyboard-Shortcuts (. und ,) implementieren
  4. Server-Kommunikation für Apply/Revert
```
---
Wenn der Editor nicht aktiv ist und es wird der Key '.' drueckt, soll der Editor und select mode aktiviert werden.
---
Kurze Frage zum wording: Ich glaube das das Wort Block mehrfache Bedeutung hat. Es gibt eine Block Entitaet und ein Block Typ. Ausserdem gibt es Materialien. Wie sollen wir diese dinge nennen damit keine verwechlung auftritt?
```text
Aktuelle Konzepte im Code:

  1. BlockType (in @nimbus-client/core):
    - Ist die Definition/Template eines Blocks
    - Enthält: id, name, shape, texture, material, properties
    - Beispiel: "grass", "stone", "water"
    - Wird im Registry gespeichert
    - Das ist der Typ/die Art des Blocks
  2. Block Instance (in der Welt):
    - Eine konkrete Block-Instanz an einer Position (x,y,z)
    - Hat nur eine BlockType-ID (referenziert den BlockType)
    - Wird im Chunk-Daten-Array gespeichert
    - Das ist ein konkreter Block in der Welt
  3. Material:
    - Kann zwei Bedeutungen haben:
        - Rendering-Material (Babylon.js Material für Textur/Shader)
      - Block-Material-Eigenschaft (z.B. "stone", "wood", "glass" als logische Eigenschaft)

  Vorschlag für klare Begriffe:

  BlockType         -> "BlockTyp" oder "Blockart" (Definition)
  Block Instance    -> "Block" oder "Blockinstanz" (konkret in der Welt)
  Material (logic)  -> "Material" (logische Eigenschaft des BlockTyps)
  Material (render) -> "Rendering-Material" oder "Shader-Material"

  Im UI könnte man zeigen:

  Block-Info Tab:
  - Position: (x, y, z) → Die konkrete Block-Instanz
  - Blocktyp: "grass" → Der BlockType name
  - Blocktyp-ID: 2 → Die BlockType id
  - Material: "grass" → Die Material-Eigenschaft

  Block-Editor Tab:
  - "Blocktyp-ID ändern" statt "Block ID"
  - Damit wird klar: Man ändert welcher Typ von Block an dieser Position ist
```
Dann nennen wir das folgendermassen:
- BlockType oder Type: Block-Definition
- BlockInstance oder Instance: Block-Instanz
- Material: Block-Material-Eigenschaft (logical)
- RenderingMaterial: Shader (Babylon.js Material)
Schreibe das in die CLAUD.md Datei.
Ergänze noch informationen, z.b. wie und wo das dann im code definiert ist.
---
Nenne Block-Liste im Editor in Block-Type Liste um.
---
Es wird ein weiterer Tab im Editor benötigt, hier werden alle geänderten Block-Instanzen aufgelistet
die noch nicht zum Server gesendet wurden.
---
> Block-Editor Formular: Das Feld shape soll eine drop-down box werden. Alle Shape-Typen werden in der drop-down box angezeigt.
---

## Editor Stylen

Das Aussehen des Editor soll verbessert werden. Ich moechte, dass
die Bedienelemente im Editor eleganter aussehen. Alle in der gleiche
Farbe, Hell, Icons? runde Ecken?.
