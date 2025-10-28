# DropdownMenu Component

Eine wiederverwendbare Dropdown-Komponente für Babylon.js GUI, basierend auf dem MenuOptions Beispiel.

## Features

- **Vollständige Dropdown-Funktionalität**: Zeigt eine aufklappbare Liste von Optionen
- **Z-Index Management**: Automatisches Layering für korrekte Anzeige über anderen Elementen
- **Event-Handling**: onChange Callback für Wertänderungen
- **Flexible Styling**: Anpassbare Farben, Größen und Ausrichtung
- **Clean API**: Einfache Verwendung mit `setOptions()` und `onChange()`

## Verwendung im BlockEditor

### Beispiel 1: Block-Type Auswahl

```typescript
this.addLabel('Block Type:');
const allBlocks = this.registry.getAllBlocks();
const blockOptions = allBlocks.map(b => `${b.id}: ${b.name}`);
const currentSelection = `${blockType.id}: ${blockType.name}`;

this.addDropdown(blockOptions, currentSelection, (newValue) => {
  const newBlockId = parseInt(newValue.split(':')[0]);
  if (!isNaN(newBlockId)) {
    this.trackBlockModification(newBlockId);
  }
});
```

### Beispiel 2: Shape Auswahl

```typescript
this.addLabel('Shape:');
const shapeOptions = ['CUBE', 'CROSS', 'HASH', 'MODEL', 'GLASS', 'FLAT', 'SPHERE', 'COLUMN', 'ROUND_CUBE'];
const currentShape = this.currentModifier?.shape !== undefined
  ? this.getShapeName(this.currentModifier.shape)
  : this.getShapeName(blockType.shape);

this.addDropdown(shapeOptions, currentShape, (newValue) => {
  const shape = this.parseShapeName(newValue);
  this.updateModifier('shape', shape);
});
```

### Beispiel 3: Material Auswahl

```typescript
this.addLabel('Material:');
const materialOptions = ['solid', 'water', 'lava', 'barrier', 'gas'];
const currentMaterial = currentOptions.material || blockType.options?.material || 'solid';

this.addDropdown(materialOptions, currentMaterial, (newValue) => {
  const newOptions = { ...currentOptions, material: newValue as any };
  this.updateModifier('options', newOptions);
});
```

## API Reference

### Constructor

```typescript
constructor(parentPanel: Container, options?: DropdownOptions)
```

**DropdownOptions:**
- `width?: number` - Breite in Pixeln (default: 180)
- `height?: number` - Höhe in Pixeln (default: 40)
- `color?: string` - Textfarbe (default: 'white')
- `background?: string` - Hintergrundfarbe (default: '#333333')
- `align?: number` - Vertikale Ausrichtung (Control.VERTICAL_ALIGNMENT_TOP)
- `valign?: number` - Horizontale Ausrichtung (Control.HORIZONTAL_ALIGNMENT_LEFT)

### Methoden

#### `setText(text: string): void`
Setzt den Text des Hauptbuttons.

#### `getValue(): string`
Gibt den aktuell ausgewählten Wert zurück.

#### `setValue(value: string): void`
Setzt den ausgewählten Wert und aktualisiert den Button-Text.

#### `onChange(callback: (value: string) => void): void`
Registriert einen Callback für Wertänderungen.

#### `addOption(text: string, callback?: () => void): void`
Fügt eine einzelne Option hinzu.

#### `clearOptions(): void`
Entfernt alle Optionen.

#### `setOptions(optionsList: string[], selectedValue?: string): void`
Setzt alle Optionen aus einem Array und optional einen vorausgewählten Wert.

#### `dispose(): void`
Gibt alle Ressourcen frei.

### Properties

- `top: string | number` - Top-Position des Containers
- `left: string | number` - Left-Position des Containers

## Internes Verhalten

### Z-Index Management

Die Komponente verwendet automatisches Z-Index Management:
- **onPointerEnter**: `zIndex = 555` (bringt Dropdown nach vorne)
- **onPointerOut**: `zIndex = 0` und schließt die Options-Liste

### Event Flow

1. **Button Click**: Öffnet/schließt die Options-Liste (toggle)
2. **Option Click**:
   - Schließt die Options-Liste
   - Aktualisiert den ausgewählten Wert
   - Ruft den optionalen Callback auf
   - Ruft den onChange-Callback auf

## Implementation Details

Die Komponente basiert auf dem MenuOptions Beispiel von Babylon.js:
- Verwendet einen `Container` als Hauptelement
- `Button` für die Anzeige des ausgewählten Wertes
- `StackPanel` für die vertikale Liste der Optionen
- Automatisches Schließen bei Mouse-Out für bessere UX

## Integration im BlockEditor

Die Dropdown-Komponente wird über die `addDropdown()` Methode im BlockEditor verwendet:

```typescript
private addDropdown(options: string[], selectedValue: string, onChange: (newValue: string) => void): void {
  // Create dropdown menu
  const dropdown = new DropdownMenu(this.contentPanel, {
    width: 400,
    height: 30,
    color: '#FFFFFF',
    background: '#333333'
  });

  // Set selected value
  dropdown.setValue(selectedValue);

  // Add all options
  dropdown.setOptions(options, selectedValue);

  // Set change handler
  dropdown.onChange(onChange);
}
```

Diese Methode wird vom BlockEditor an verschiedenen Stellen genutzt:
- Block Type Auswahl (alle 95+ Block-Types)
- Shape Auswahl (9 verschiedene Shapes)
- Material Auswahl (5 Material-Typen)
- Rotation Auswahl (8 Rotation-Werte)
- Facing Auswahl (6 Richtungen)

## Vorteile gegenüber der alten Lösung

Die alte Implementierung zeigte nur die ersten 3 Optionen als Buttons:

```typescript
// ALT (entfernt):
for (const option of options.slice(0, 3)) { // Nur 3 Optionen!
  const btn = Button.CreateSimpleButton('opt_' + option, option);
  // ...
}
```

Die neue DropdownMenu-Komponente:
- ✅ Zeigt **alle** Optionen (nicht nur 3)
- ✅ Scrollbar-Support für lange Listen
- ✅ Besseres UX mit aufklappbarer Liste
- ✅ Z-Index Management für korrekte Layering
- ✅ Wiederverwendbar in anderen Komponenten
- ✅ Cleaner Code durch Kapselung

## Testing

Die Komponente kann getestet werden durch:
1. Starten des Clients: `npm run dev`
2. Verbinden zum Server
3. Block auswählen mit "."
4. Im Block Editor Tab:
   - Block Type Dropdown testen (alle 95+ Blocks sollten sichtbar sein)
   - Shape Dropdown testen (alle 9 Shapes)
   - Material Dropdown testen (alle 5 Materialien)
