# Input System Architecture

Das Input System des Nimbus Clients folgt einer klaren Trennung zwischen Input-Empfang, Binding und Logik.

## Architektur-Überblick

```
AppContext
  └── InputService
        ├── HandlerRegistry (Map<string, InputHandler>)
        │     ├── 'click' → ClickInputHandler
        │     ├── 'shortcut' → ShortcutInputHandler
        │     ├── (weitere zentrale Handler...)
        │     └── ...
        └── Controller (WebInputController / GamePadController / TouchController)
              └── bindet Handler an konkrete Inputs
```

## Komponenten

### 1. InputService

**Verantwortlichkeiten:**
- Zentrale Verwaltung aller InputHandler
- Bereitstellung von Handlers für InputController
- Update-Loop für aktive Handler

**API:**
```typescript
class InputService {
  // Handler-Zugriff
  getHandler(key: string): InputHandler | undefined;

  // Controller-Management
  setController(controller: InputController): void;

  // Update-Loop (wird jedes Frame aufgerufen)
  update(deltaTime: number): void;
}
```

**Verwendung:**
```typescript
// In NimbusClient oder Bootstrap-Code
const inputService = new InputService(appContext, playerService);
appContext.services.input = inputService;

// Controller setzen
const webController = new WebInputController(canvas, playerService, appContext);
inputService.setController(webController);
```

### 2. InputController

**Verantwortlichkeiten:**
- Empfangen von Input-Events (Keyboard, Maus, GamePad, Touch)
- Binding: Welcher Input aktiviert welchen Handler
- Keine Business-Logik!

**Interface:**
```typescript
interface InputController {
  initialize(): void;
  dispose(): void;
  getHandlers(): InputHandler[];
}
```

**Implementierungen:**
- `WebInputController` - Browser-Input (Keyboard, Maus)
- `GamePadController` - GamePad/Controller-Input (zukünftig)
- `TouchController` - Touch-Screen-Input (zukünftig)

**Beispiel: WebInputController**
```typescript
class WebInputController implements InputController {
  private clickHandler?: InputHandler;
  private shortcutHandler?: InputHandler;

  initialize(): void {
    // Handler aus InputService holen
    const inputService = this.appContext.services.input;
    this.clickHandler = inputService.getHandler('click');
    this.shortcutHandler = inputService.getHandler('shortcut');

    // Event-Listener registrieren
    this.canvas.addEventListener('mousedown', this.onMouseDown);
    window.addEventListener('keydown', this.onKeyDown);
  }

  private onMouseDown = (event: MouseEvent) => {
    // Binding: Mausklick → ClickHandler
    if (this.clickHandler) {
      this.clickHandler.activate(event.button); // 0=left, 1=middle, 2=right
    }
  };

  private onKeyDown = (event: KeyboardEvent) => {
    // Binding: Zahlentaste → ShortcutHandler
    if (event.key >= '0' && event.key <= '9') {
      const shortcutNr = event.key === '0' ? 10 : parseInt(event.key, 10);
      if (this.shortcutHandler) {
        this.shortcutHandler.activate(shortcutNr);
      }
    }
  };
}
```

### 3. InputHandler

**Verantwortlichkeiten:**
- Enthält die gesamte Business-Logik für eine Aktion
- Zugriff auf Services (PlayerService, NetworkService, SelectService, etc.)
- Unterstützt verschiedene Input-Typen (diskret/kontinuierlich)

**Basis-Klasse:**
```typescript
abstract class InputHandler {
  protected playerService: PlayerService;
  protected appContext?: AppContext;
  protected state: InputState = { active: false, value: 0 };

  // Lifecycle-Methoden (public)
  activate(value?: number): void;
  deactivate(): void;
  update(deltaTime: number): void;
  isActive(): boolean;

  // Template-Methoden (protected, von Subklassen implementiert)
  protected abstract onActivate(value: number): void;
  protected abstract onDeactivate(): void;
  protected abstract onUpdate(deltaTime: number, value: number): void;
}
```

**Handler-Typen:**

#### A. Diskrete Handler (Keys, Buttons)
Führen eine Aktion sofort bei Aktivierung aus.

```typescript
class JumpHandler extends InputHandler {
  protected onActivate(value: number): void {
    this.playerService.jump();
  }

  protected onDeactivate(): void { /* no-op */ }
  protected onUpdate(deltaTime: number, value: number): void { /* no-op */ }
}
```

#### B. Kontinuierliche Handler (Movement)
Werden jedes Frame aktualisiert während sie aktiv sind.

```typescript
class MoveForwardHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Initial aktivieren
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Jedes Frame: Bewegung anwenden
    this.playerService.move('forward', value); // value = 0.0-1.0
  }

  protected onDeactivate(): void {
    // Bewegung stoppen
  }
}
```

#### C. Komplexe Handler (Click, Shortcut)
Interagieren mit mehreren Services und senden Daten an Server.

```typescript
class ClickInputHandler extends InputHandler {
  protected onActivate(buttonNumber: number): void {
    // 1. SelectService: Entity/Block unter Cursor finden
    // 2. PlayerService: Position, Rotation, MovementStatus holen
    // 3. PlayerInfo: Shortcut-Konfiguration lesen
    // 4. NetworkService: Interaktion an Server senden

    const selectService = this.appContext.services.select;
    const selectedEntity = selectService.getCurrentSelectedEntity();

    if (selectedEntity) {
      const movementStatus = this.playerService.getMovementState();
      const shortcut = playerInfo.shortcuts?.[`click${buttonNumber}`];

      this.appContext.services.network.sendEntityInteraction(
        selectedEntity.id,
        'click',
        buttonNumber,
        {
          movementStatus,
          shortcutType: shortcut?.type,
          shortcutItemId: shortcut?.itemId,
          // ... weitere Kontext-Daten
        }
      );
    }
  }
}
```

## Input-Typen und ihre Unterstützung

Handler sollten flexibel verschiedene Input-Wege unterstützen:

### 1. Diskrete Inputs (Binary: An/Aus)
- **Beispiele:** Tasten, Mausklicks, GamePad-Buttons
- **Handler:** `activate()` wird mit Standardwert (1.0) aufgerufen
- **Adaptierung:** Handler nutzt internen Standardwert (z.B. Turn-Speed)

```typescript
// Keyboard
handler.activate(); // value=1.0 (Standard)

// GamePad Button
handler.activate(triggerPressure); // value=0.0-1.0 (optional)
```

### 2. Kontinuierliche Inputs (Analog: 0.0-1.0)
- **Beispiele:** Maus-Movement, Analog-Stick, Trigger-Druck
- **Handler:** `activate(value)` wird mit normalisierten Wert aufgerufen
- **Adaptierung:** Handler nutzt den übergebenen Wert direkt

```typescript
// Mouse Movement
handler.activate(deltaX / sensitivity); // Normalisiert

// Analog Stick
handler.activate(stickValue); // -1.0 bis +1.0
```

### 3. Beispiel: RotateHandler
Unterstützt beide Wege:

```typescript
class RotateHandler extends InputHandler {
  private deltaX: number = 0;
  private deltaY: number = 0;
  private defaultTurnSpeed: number = 0.003; // Standardgeschwindigkeit

  // Für Maus: Direkter Delta-Wert
  setDelta(deltaX: number, deltaY: number): void {
    this.deltaX = deltaX;
    this.deltaY = deltaY;
  }

  // Für GamePad: Kontinuierlicher Wert
  protected onUpdate(deltaTime: number, value: number): void {
    if (this.deltaX !== 0 || this.deltaY !== 0) {
      // Maus: Nutze Delta direkt
      this.playerService.rotate(this.deltaX, this.deltaY);
      this.deltaX = 0;
      this.deltaY = 0;
    } else if (value !== 0) {
      // GamePad: Nutze value + Standardgeschwindigkeit
      const rotationAmount = value * this.defaultTurnSpeed * deltaTime;
      this.playerService.rotate(rotationAmount, 0);
    }
  }
}
```

## Handler registrieren

### Zentrale Handler (im InputService)
Werden einmal erstellt und von allen Controllern geteilt:

```typescript
// In InputService.registerCentralHandlers()
this.handlerRegistry.set('click', new ClickInputHandler(playerService, appContext));
this.handlerRegistry.set('shortcut', new ShortcutInputHandler(playerService, appContext));
```

### Controller-spezifische Handler
Werden vom Controller selbst erstellt und verwaltet:

```typescript
// In WebInputController.constructor()
this.moveForwardHandler = new MoveForwardHandler(playerService);
this.rotateHandler = new RotateHandler(playerService);
```

## Neue Handler erstellen

### 1. Handler-Klasse erstellen

```typescript
// handlers/MyActionHandler.ts
export class MyActionHandler extends InputHandler {
  protected onActivate(value: number): void {
    // Logik bei Aktivierung
  }

  protected onDeactivate(): void {
    // Logik bei Deaktivierung (optional)
  }

  protected onUpdate(deltaTime: number, value: number): void {
    // Logik für kontinuierliche Updates (optional)
  }
}
```

### 2. Im InputService registrieren (für zentrale Handler)

```typescript
// InputService.registerCentralHandlers()
this.handlerRegistry.set('myAction', new MyActionHandler(playerService, appContext));
```

### 3. Im Controller binden

```typescript
// WebInputController.initialize()
this.myActionHandler = inputService.getHandler('myAction');

// Event-Listener
onKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'E') {
    this.myActionHandler?.activate();
  }
};
```

## GamePad Controller (Zukünftig)

Ein GamePad Controller würde dieselben Handler nutzen:

```typescript
class GamePadController implements InputController {
  private clickHandler?: InputHandler;
  private shortcutHandler?: InputHandler;
  private moveForwardHandler?: InputHandler;

  initialize(): void {
    // Zentrale Handler holen
    const inputService = this.appContext.services.input;
    this.clickHandler = inputService.getHandler('click');
    this.shortcutHandler = inputService.getHandler('shortcut');

    // Eigene Handler erstellen
    this.moveForwardHandler = new MoveForwardHandler(playerService);

    // GamePad-Events binden
    window.addEventListener('gamepadconnected', this.onGamePadConnected);
  }

  private updateGamePad(): void {
    const gamepads = navigator.getGamepads();
    const pad = gamepads[0];

    if (pad) {
      // Trigger → Click
      if (pad.buttons[0].pressed) {
        this.clickHandler?.activate(0); // Trigger = Button 0
      }

      // D-Pad → Shortcuts
      if (pad.buttons[12].pressed) {
        this.shortcutHandler?.activate(1); // D-Pad Up = Shortcut 1
      }

      // Analog Stick → Bewegung
      const stickY = pad.axes[1]; // Vertical axis
      if (Math.abs(stickY) > 0.1) { // Deadzone
        this.moveForwardHandler?.activate(stickY); // Kontinuierlicher Wert
      }
    }
  }
}
```

## Best Practices

### 1. Keine Logik im Controller
❌ **Falsch:**
```typescript
private onMouseDown = (event: MouseEvent) => {
  const selectedEntity = this.appContext.services.select.getCurrentSelectedEntity();
  if (selectedEntity) {
    this.appContext.services.network.sendEntityInteraction(...);
  }
};
```

✅ **Richtig:**
```typescript
private onMouseDown = (event: MouseEvent) => {
  this.clickHandler?.activate(event.button);
};
```

### 2. Handler sind zustandslos zwischen Aktivierungen
Handler sollten ihren Zustand zwischen `deactivate()` und `activate()` nicht beibehalten (außer Konfiguration).

### 3. Handler-Keys sind eindeutig
Zentrale Handler verwenden eindeutige Keys:
- `'click'` - Mausklicks/Trigger
- `'shortcut'` - Tastatur-Shortcuts/GamePad-Buttons
- `'use'` - Interact/Use-Action
- usw.

### 4. Performance
- Handler-Lookups nur bei `initialize()`, nicht bei jedem Event
- Handler-Referenzen cachen
- Keine schweren Operations in `onUpdate()` (wird jedes Frame aufgerufen)

## Zusammenfassung

**Input-Flow:**
```
User Input
  ↓
InputController (Binding)
  ↓
InputHandler.activate()
  ↓
InputHandler.onActivate() [Business-Logik]
  ↓
Services (PlayerService, NetworkService, etc.)
  ↓
Server / Game State
```

**Vorteile dieser Architektur:**
- ✅ **Wiederverwendbarkeit:** Handler funktionieren mit allen Controllern
- ✅ **Erweiterbarkeit:** Neue Controller einfach hinzufügen (GamePad, Touch)
- ✅ **Testbarkeit:** Handler können isoliert getestet werden
- ✅ **Wartbarkeit:** Klare Trennung von Input-Empfang und Logik
- ✅ **Flexibilität:** Handler unterstützen verschiedene Input-Typen
