## Bedienung

[x] Mit den Tasten Z und X soll man sich hoch und runter Bewegen wenn man im Flugmodus ist.
Im Walk Modus wird der Kammera-Winkel nach oben und unten gedreht.

[x] Wenn der Block_Editor aktiv werden soll und 'Cannot activate edit mode: No block selected' erscheint, dann soll der 'New Block' Modus aktiv werden.

[x] Im Block-Editor für color den ColorPicker von BabylonJS GUI verwenden.

[ ] Dropdown soll eine option an der componente haben, die ein suchfeld anzeigt. wird etwas gesucht, dann reduzieren sich die options.
[ ] Dropdown soll eine option an der componente haben, die es ermoeglicht in eine textfeld oben den Wert frei einzugeben. Bei Enter wird der Wret uebernommen.

[ ] Im Flugmodus soll mit Grossbuchstaben (kein CAPSLOCK) die Kamera um einen Punkt bewegen werden. Der Punkt soll zwei von der Kamera entfernt sein. Dieser Abstand soll in der Console mit einem command änderbar sein.
- Q und E: Roteiren linkls und Rechts um den Punkt herum
- Z und X Rotieren hoch und runter um den Punkt herum
 ```
Orbit Camera Controls (Flight Mode):
  - Shift+Q: Rotate left around focus point
  - Shift+E: Rotate right around focus point
  - Shift+Space: Rotate up around focus point
  - Shift+xx: Rotate down around focus point

  Console Commands:
  - orbitdist - Show current orbit distance (default: 4 blocks)
  - orbitdist 5 - Set orbit distance to 5 blocks
  - orbitdist 10.5 - Set orbit distance to 10.5 blocks
 ```

[x] Wenn ich im editor in einem Textfeld bin und '.' oder '/' druecke, dann wird der Shortkey aktiv, an der stelle soll aber das textfeld den key konsumieren.
[x] Wenn ich im Editor anfange einen bestehenden Block zu editieren, springen immer alle parameter auf 'default' anstelle die daten des block zu kopieren.
[x] Im Block Editor ist oben, unter den Tab-Buttons bis zum Tab-Content viel Platz leer gelassen


[x] Copy/Paste: Wenn ein Block selektiert ist, brauche ich im Editor einen Button 'Copy'. Das merkt sich alle Einstellungen des Selektierten Blocks in einem Puffer
Wenn ich im Block-Editor modus bin brauche ich einen Button 'Paste'. Wenn ich auf 'Paste' druecke, dann wird der Puffer in den Editor kopiert.
- Der 'Copy' Button ist nur im Block-Info sichtbar
- Der Paste Button ist nur im Block-Editor sichtbar
- Die beiden Button koennen auch der gleiche sein, nur der titel wechselt, wenn der Tab wechselt, es reicht wenn dort drin steht 'C' copy, 'P' Paste und '-' wenn nicht moeglich (nichts im CopyPaste Puffer)
- Paste kann auch im Neuer-Block-Select modus durch drücken des Key ',' aktiviert werden. d.h. der block an die neue stelle kopiert, der Neuer-Block-Select modus bleibt an
- Wenn ein Block im Copy-Puffer ist, wird im Info-Editor oben eine kurze info angezeigt
