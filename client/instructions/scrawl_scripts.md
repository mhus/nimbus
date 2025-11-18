
# Scrawl Framework

Die Scrawl Engine ist ein Framework um Animationen und Effekte (Sound,Visuell) in der 3D Welt anzuzeigen. Sie ist
modular aufgebaut und erlaubt es Effekte zu Codieren und zu hinterlegen und in einem Script zu einer Effektkette
zu verbinden.


## Konzept / Idee

Nicht relevant fuer die direkte Umsetzung, nur als Hintergrundinformation, oder Beispiel:

### Effekt Script Definition

effect-script.schema.json
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "$id": "https://example.com/effect-script.schema.json",
  "title": "EffectScript",
  "type": "object",
  "required": [
    "id",
    "root"
  ],
  "additionalProperties": false,
  "properties": {
    "schemaVersion": {
      "type": "integer",
      "minimum": 1,
      "default": 1
    },
    "id": {
      "type": "string",
      "minLength": 1
    },
    "description": {
      "type": "string"
    },
    "root": {
      "$ref": "#/definitions/Step"
    }
  },
  "definitions": {
    "Seconds": {
      "type": "number",
      "minimum": 0
    },
    "Step": {
      "type": "object",
      "oneOf": [
        {
          "$ref": "#/definitions/Play"
        },
        {
          "$ref": "#/definitions/Wait"
        },
        {
          "$ref": "#/definitions/Parallel"
        },
        {
          "$ref": "#/definitions/Sequence"
        },
        {
          "$ref": "#/definitions/Repeat"
        },
        {
          "$ref": "#/definitions/If"
        },
        {
          "$ref": "#/definitions/EmitEvent"
        },
        {
          "$ref": "#/definitions/WaitEvent"
        },
        {
          "$ref": "#/definitions/SetVar"
        },
        {
          "$ref": "#/definitions/Call"
        }
      ]
    },
    "Play": {
      "type": "object",
      "required": [
        "kind",
        "effectId"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Play"
        },
        "effectId": {
          "type": "string",
          "minLength": 1
        },
        "ctx": {
          "type": [
            "object",
            "null"
          ],
          "additionalProperties": true
        }
      }
    },
    "Wait": {
      "type": "object",
      "required": [
        "kind",
        "seconds"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Wait"
        },
        "seconds": {
          "$ref": "#/definitions/Seconds"
        }
      }
    },
    "Parallel": {
      "type": "object",
      "required": [
        "kind",
        "steps"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Parallel"
        },
        "steps": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Step"
          },
          "minItems": 1
        }
      }
    },
    "Sequence": {
      "type": "object",
      "required": [
        "kind",
        "steps"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Sequence"
        },
        "steps": {
          "type": "array",
          "items": {
            "$ref": "#/definitions/Step"
          },
          "minItems": 1
        }
      }
    },
    "Repeat": {
      "type": "object",
      "required": [
        "kind",
        "step"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Repeat"
        },
        "times": {
          "type": [
            "integer",
            "null"
          ],
          "minimum": 1
        },
        "untilEvent": {
          "type": [
            "string",
            "null"
          ]
        },
        "step": {
          "$ref": "#/definitions/Step"
        }
      },
      "anyOf": [
        {
          "required": [
            "times"
          ]
        },
        {
          "required": [
            "untilEvent"
          ]
        }
      ]
    },
    "If": {
      "type": "object",
      "required": [
        "kind",
        "cond",
        "then"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "If"
        },
        "cond": {
          "$ref": "#/definitions/Cond"
        },
        "then": {
          "$ref": "#/definitions/Step"
        },
        "else": {
          "$ref": "#/definitions/Step"
        }
      }
    },
    "EmitEvent": {
      "type": "object",
      "required": [
        "kind",
        "name"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "EmitEvent"
        },
        "name": {
          "type": "string",
          "minLength": 1
        },
        "payload": {
          "type": [
            "object",
            "null"
          ],
          "additionalProperties": true
        }
      }
    },
    "WaitEvent": {
      "type": "object",
      "required": [
        "kind",
        "name"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "WaitEvent"
        },
        "name": {
          "type": "string",
          "minLength": 1
        },
        "timeout": {
          "$ref": "#/definitions/Seconds"
        }
      }
    },
    "SetVar": {
      "type": "object",
      "required": [
        "kind",
        "name"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "SetVar"
        },
        "name": {
          "type": "string",
          "minLength": 1
        },
        "value": {}
      }
    },
    "Call": {
      "type": "object",
      "required": [
        "kind",
        "scriptId"
      ],
      "additionalProperties": false,
      "properties": {
        "kind": {
          "const": "Call"
        },
        "scriptId": {
          "type": "string",
          "minLength": 1
        },
        "args": {
          "type": [
            "object",
            "null"
          ],
          "additionalProperties": true
        }
      }
    },
    "Cond": {
      "type": "object",
      "oneOf": [
        {
          "properties": {
            "kind": {
              "const": "VarEquals"
            },
            "name": {
              "type": "string"
            },
            "value": {}
          },
          "required": [
            "kind",
            "name"
          ]
        },
        {
          "properties": {
            "kind": {
              "const": "Chance"
            },
            "p": {
              "type": "number",
              "minimum": 0,
              "maximum": 1
            }
          },
          "required": [
            "kind",
            "p"
          ]
        },
        {
          "properties": {
            "kind": {
              "const": "HasTargets"
            },
            "min": {
              "type": "integer",
              "minimum": 0
            }
          },
          "required": [
            "kind"
          ]
        }
      ],
      "additionalProperties": false
    }
  }
}
```

### Validierung

```ts
import Ajv from "ajv";
import schema from "./schema/effect-script.schema.json";
const ajv = new Ajv({ allErrors: true });
const validate = ajv.compile(schema);
const valid = validate(scriptJson);
if (!valid) console.error(validate.errors);
```

### Effekt Script Ausführung

scrawl/EffectSystem.ts Example:

```ts
export type EffectContext = Record<string, any>;

export interface EffectSystem {
  /** Startet einen konkreten Effekt (z. B. Partikel+Sound+Licht) mit Kontext. */
  trigger(effectId: string, ctx?: EffectContext): void;

  /** Optional: Sync/Event-Routing (für WaitEvent/EmitEvent) */
  on?(event: string, handler: (payload?: any) => void): () => void;
  emit?(event: string, payload?: any): void;
}
```

scrawl/EffectSequencer.ts Example:

```ts
// --- Types (entsprechen dem JSON-Schema) ---
export type Step =
  | { kind: "Play"; effectId: string; ctx?: any }
  | { kind: "Wait"; seconds: number }
  | { kind: "Parallel"; steps: Step[] }
  | { kind: "Sequence"; steps: Step[] }
  | { kind: "Repeat"; times?: number | null; untilEvent?: string | null; step: Step }
  | { kind: "If"; cond: Cond; then: Step; else?: Step }
  | { kind: "EmitEvent"; name: string; payload?: any }
  | { kind: "WaitEvent"; name: string; timeout?: number }
  | { kind: "SetVar"; name: string; value: any }
  | { kind: "Call"; scriptId: string; args?: Record<string, any> };

export type Cond =
  | { kind: "VarEquals"; name: string; value: any }
  | { kind: "Chance"; p: number }
  | { kind: "HasTargets"; min?: number };

export type EffectScript = {
  schemaVersion?: number;
  id: string;
  description?: string;
  root: Step;
};

export interface ScriptLibrary {
  get(id: string): EffectScript | undefined;
}

type Clock = () => number; // Sekunden (z. B. () => performance.now()/1000)

export class EffectSequencer {
  constructor(
    private effects: import("./EffectSystem").EffectSystem,
    private scripts: ScriptLibrary,
    private now: Clock = () => performance.now() / 1000
  ) {}

  run(scriptIdOrObj: string | EffectScript, ctx: Record<string, any> = {}): ScriptHandle {
    const script = typeof scriptIdOrObj === "string" ? this.scripts.get(scriptIdOrObj) : scriptIdOrObj;
    if (!script) throw new Error(`Script not found`);
    const h = new ScriptHandle(this, script, ctx);
    h.start(); // fire-and-forget; du kannst auch eine Promise-API bevorzugen
    return h;
  }

  // intern:
  async execStep(h: ScriptHandle, step: Step): Promise<void> {
    if (h.cancelled) return;

    switch (step.kind) {
      case "Play":
        this.effects.trigger(step.effectId, { ...h.ctx, ...(step.ctx || {}) });
        return;

      case "Wait":
        await h.sleep(step.seconds);
        return;

      case "Parallel": {
        await Promise.all(step.steps.map(s => this.execStep(h.fork(), s)));
        return;
      }

      case "Sequence": {
        for (const s of step.steps) {
          await this.execStep(h, s);
          if (h.cancelled) break;
        }
        return;
      }

      case "Repeat": {
        if (step.times != null) {
          for (let i = 0; i < step.times && !h.cancelled; i++) {
            await this.execStep(h, step.step);
          }
        } else if (step.untilEvent) {
          while (!h.cancelled) {
            const done = await h.waitEvent(step.untilEvent, 0);
            if (done) break;
            await this.execStep(h, step.step);
          }
        }
        return;
      }

      case "If": {
        const pass = h.evalCond(step.cond);
        if (pass) await this.execStep(h, step.then);
        else if (step.else) await this.execStep(h, step.else);
        return;
      }

      case "EmitEvent":
        h.emit(step.name, step.payload);
        return;

      case "WaitEvent":
        await h.waitEvent(step.name, step.timeout ?? 0);
        return;

      case "SetVar":
        h.vars.set(step.name, step.value);
        return;

      case "Call": {
        const sub = this.scripts.get(step.scriptId);
        if (!sub) return;
        const subHandle = new ScriptHandle(this, sub, { ...h.ctx, ...(step.args || {}) });
        await subHandle.start();
        return;
      }
    }
  }
}

export class ScriptHandle {
  cancelled = false;
  paused = false;
  vars = new Map<string, any>();

  constructor(
    private seq: EffectSequencer,
    public script: EffectScript,
    public ctx: Record<string, any>
  ) {}

  cancel() { this.cancelled = true; }
  pause() { this.paused = true; }
  resume() { this.paused = false; }

  async start() { await this.seq.execStep(this, this.script.root); }

  fork(): ScriptHandle {
    const f = new ScriptHandle(this.seq, this.script, { ...this.ctx });
    f.vars = new Map(this.vars);
    return f;
    // Hinweis: Fork teilt Events; passe an, wenn du isolieren willst.
  }

  sleep(seconds: number): Promise<void> {
    const target = this.seq["now"]() + seconds;
    return new Promise<void>(res => {
      const tick = () => {
        if (this.cancelled) return res();
        if (this.paused) return requestAnimationFrame(tick);
        if (this.seq["now"]() >= target) return res();
        requestAnimationFrame(tick);
      };
      tick();
    });
  }

  emit(name: string, payload?: any) {
    if (this.seq["effects"].emit) this.seq["effects"].emit(name, payload);
    else document.dispatchEvent(new CustomEvent(name, { detail: payload }));
  }

  waitEvent(name: string, timeoutSec = 0): Promise<boolean> {
    return new Promise<boolean>(res => {
      let to: any = null;
      const handler = () => { cleanup(); res(true); };
      const cleanup = () => {
        if (this.seq["effects"].on) off?.();
        else document.removeEventListener(name, handler);
        if (to) clearTimeout(to);
      };

      let off: (() => void) | undefined;
      if (this.seq["effects"].on) off = this.seq["effects"].on!(name, handler);
      else document.addEventListener(name, handler, { once: true });

      if (timeoutSec > 0) {
        to = setTimeout(() => { cleanup(); res(false); }, timeoutSec * 1000);
      }
    });
  }

  evalCond(cond: Cond): boolean {
    switch (cond.kind) {
      case "VarEquals": return this.vars.get(cond.name) === cond.value;
      case "Chance": return Math.random() < cond.p;
      case "HasTargets": {
        const n = Array.isArray(this.ctx?.patients) ? this.ctx.patients.length : 0;
        const min = cond.min ?? 1;
        return n >= min;
      }
    }
  }
}
```

### Beispiel Effekt Script

```ts
import { EffectSequencer } from "./runtime/EffectSequencer";
import { EffectSystem } from "./runtime/EffectSystem";

const scripts = new Map<string, any>();
scripts.set("warn_then_boom", {
  id: "warn_then_boom",
  root: {
    kind: "Sequence",
    steps: [
      { kind: "Play", effectId: "aoe_marker", ctx: { radius: 3, color: "#ff0" } },
      { kind: "Wait", seconds: 1.5 },
      { kind: "Play", effectId: "explosion_big", ctx: { damage: 50 } }
    ]
  }
});

// Effekte sollen von einer Factory on demand immer neu erzeugt werden. - hier nur beispielhaft:
const effectSystem: EffectSystem = {
  trigger: (id, ctx) => console.log("[Effect]", id, ctx),
  on: (event, handler) => {
    const h = (e: any) => handler(e?.detail);
    document.addEventListener(event, h);
    return () => document.removeEventListener(event, h);
  },
  emit: (event, payload) => document.dispatchEvent(new CustomEvent(event, { detail: payload }))
};

const seq = new EffectSequencer(effectSystem, { get: (id) => scripts.get(id) });
seq.run("warn_then_boom", { actor: "A1", patients: ["B1"] });
```

### Player Registry und Factory

scarwl/EffectPlayerRegistry.ts Beispiel:
```ts
// =====================
// Effekt-Basis / Contracts
// =====================

export type Deps = {
  // Engine-/Game-abhängige Abhängigkeiten hier injizieren:
  log?: (...args: any[]) => void;
  now?: () => number;
  // z.B. scene: BABYLON.Scene, audio: BABYLON.AudioEngine, ...
};

export abstract class EffectHandler<O = unknown> {
  protected started = false;
  constructor(protected readonly deps: Deps, protected readonly options: O) {}
  /** Starte den Effekt (Render/Audio/Licht etc.). */
  abstract start(): void | Promise<void>;
  /** Optional: Stop/cleanup (falls nötig). */
  stop?(): void | Promise<void>;
}

/** Konstruktor-Typ jeder Effekt-Klasse */
export interface EffectHandlerCtor<O, H extends EffectHandler<O> = EffectHandler<O>> {
  new (deps: Deps, options: O): H;
}

// =====================
// Registry: Key -> Handler-Klasse
// =====================

export class EffectRegistry {
  private map = new Map<string, EffectHandlerCtor<any>>();

  register<O>(key: string, ctor: EffectHandlerCtor<O>): this {
    if (this.map.has(key)) {
      throw new Error(`Effect key already registered: ${key}`);
    }
    this.map.set(key, ctor);
    return this;
  }

  get<O>(key: string): EffectHandlerCtor<O> {
    const ctor = this.map.get(key);
    if (!ctor) throw new Error(`Effect key not found: ${key}`);
    return ctor;
  }

  has(key: string) { return this.map.has(key); }
}

// =====================
// Factory: erstellt IMMER neue Instanzen
// =====================

export class EffectFactory {
  constructor(private readonly registry: EffectRegistry, private readonly deps: Deps) {}

  /** Neue Instanz erzeugen (jedes Mal frisch). */
  create<O, H extends EffectHandler<O> = EffectHandler<O>>(key: string, options: O): H {
    const Ctor = this.registry.get<O>(key) as EffectHandlerCtor<O, H>;
    return new Ctor(this.deps, options);
  }

  /** Bequemer Helper: erstellen + starten. */
  play<O>(key: string, options: O): EffectHandler<O> {
    const h = this.create<O>(key, options);
    const started = h.start();
    if (started instanceof Promise) started.catch(err => this.deps.log?.("[Effect start error]", err));
    return h;
  }
}

// =====================
// Beispiel-Handler (VFX / SFX / Light)
// =====================

type VfxOptions = { template: string; size?: number; color?: string; duration?: number };
export class VfxHandler extends EffectHandler<VfxOptions> {
  start() {
    this.started = true;
    this.deps.log?.("[VFX] start", this.options);
    // z.B. Partikel-Emitter erstellen, Uniforms setzen, Timer für Auto-Despawn ...
    if (this.options.duration) {
      setTimeout(() => this.stop?.(), this.options.duration * 1000);
    }
  }
  stop() {
    if (!this.started) return;
    this.deps.log?.("[VFX] stop");
    this.started = false;
    // Partikel entfernen / in Pool zurückgeben etc.
  }
}

type SfxOptions = { clip: string; volume?: number; pitch?: number; pos3D?: [number, number, number] };
export class SfxHandler extends EffectHandler<SfxOptions> {
  start() {
    this.started = true;
    this.deps.log?.("[SFX] play", this.options);
    // Audio laden/abspielen; bei 3D: Position & Rolloff setzen
  }
}

type LightOptions = { kind: "point" | "spot"; intensity: number; color?: string; time?: number };
export class LightFlashHandler extends EffectHandler<LightOptions> {
  start() {
    this.started = true;
    this.deps.log?.("[LIGHT] flash", this.options);
    // Licht erzeugen, Intensitätskurve anwenden, ggf. nach 'time' wieder entfernen
    if (this.options.time) {
      setTimeout(() => this.stop?.(), this.options.time * 1000);
    }
  }
  stop() {
    if (!this.started) return;
    this.deps.log?.("[LIGHT] off");
    this.started = false;
    // Licht entfernen
  }
}

// =====================
// Setup: Registry füllen, Factory nutzen
// =====================

const registry = new EffectRegistry()
  .register<VfxOptions>("vfx.spark", VfxHandler)
  .register<SfxOptions>("sfx.beep", SfxHandler)
  .register<LightOptions>("light.flash", LightFlashHandler);

const factory = new EffectFactory(registry, {
  log: console.log,
  now: () => performance.now() / 1000,
  // scene: ..., audio: ...
});

// =====================
// Verwendung
// =====================

// 1) Instanz erzeugen und selber steuern:
const spark = factory.create<VfxOptions>("vfx.spark", { template: "spark_small", color: "#6af", duration: 0.5 });
spark.start(); // später:
spark.stop?.();

// 2) Bequem: direkt abspielen (fire & forget):
factory.play<SfxOptions>("sfx.beep", { clip: "ui/beep.ogg", volume: 0.8 });

// 3) Kombi:
factory.play<LightOptions>("light.flash", { kind: "point", intensity: 8, color: "#fff6aa", time: 0.2 });
```

## Konzeption

### Subjekte

Es gibt bis zu zwei arten von Subjekten:
- Actor: Der Ausführende des Effekts (z. B. der Spieler, der eine Fähigkeit einsetzt)
- Patients: Die Empfänger/Ziele des Effekts (z. B. Gegner, die Schaden nehmen)

Wenn ein Effekt ausgeführt wird, kann der bis zu zwei Subjekte bei der Ausfuehrung erhalten:
source und target sind Subject Objekte:
- Wetter ändern: kein Subjekt nötig
- Explosion: Ein Subjekt (Patienten) - die getroffenen Einheiten
- Schuss abfeuern: Zwei Subjekte - der Schütze (Actor) und die getroffenen Einheiten (Patienten)

Subject:
- position: Vector3 (Position der Entity oder des Blocks - Bei Block immer interger Werte)
- entityId: string (optional)
- blockId: string (optional)

So kann es beim aufruf eines Effekts immer angegebn werden:
- source="$actor"
- target="$patient[0]"

Es wird ein ForEach type benotigt, in dem z.b. fuer alle patienten ein Effekt ausgefuehrt wird.
- ForEach over $patients in $patient
  - Play effect "hit_effect" with source="$actor" and target="$patient"

### Parameter

Bei der Ausführung eines Effekts können Parameter übergeben werden, die den Effekt beeinflussen.
Beispiel:
- Play effect "explosion" with radius="$radius" and color="$color" and duration="$duration"

```json
{
  "kind": "Play",
  "effect": "vfx:aoe_marker",
  "target": "$patient",
  "ctx": {
    "radius": "$radius",
    "color": "$color",
    "duration": "$duration"
  }
}
```

### Templates

Es soll Templates geben, die vordefinierte Effekte mit Parametern bereitstellen.

```json
{
  "id": "vfx:aoe_marker",
  "description": "Zeigt einen AOE Marker an der Zielposition an.",
  "parameters": {
    "radius": { "type": "number", "default": 2 },
    "color": { "type": "string", "default": "#ff0000" },
    "duration": { "type": "number", "default": 1.0 }
  },
  "steps": {
    "kind": "Play",
    "effect": "vfx:circle_marker",
    "ctx": {
      "radius": "$radius",
      "color": "$color",
      "duration": "$duration"
    }
  }
}
```

und der Aufruf:

```json
{
  "kind": "Play",
  "template": "vfx:aoe_marker",
  "target": "$patient",
  "ctx": {
    "radius": 3,
  }
}
```

### LOD (Level of Detail)

Das Script kann mit LOD Stufen arbeiten, um Effekte je nach Entfernung oder Performance-Einstellungen anzupassen.
Dafuer gibt es einen StepLodSwitch der verschiedene Sub-Steps fuer verschiedene LOD Stufen enthaelt.

```json
{
  "kind": "LodSwitch",
  "levels": {
    "high": { "kind": "Play", "effect": "vfx:explosion_detailed" },
    "medium": { "kind": "Play", "effect": "vfx:explosion_simple" },
    "low": { "kind": "Play", "effect": "vfx:explosion_basic" }
  }
}
```

Was genau die LOD Stufen sind und wie sie bestimmt werden, ist Implementation-abhängig und nicht Teil des Scrawl Scripts.

### Vita Modifier

Zusaetzlich zu den Audio/Visual Effekten koennen Effekte auch die Vita eines Subjekts veraendern (Schaden, Heilung, Buffs).
Diese Dfinition wird separat von den Scrawl Scripts behandelt, da sie im Server ausgefuehrt werden muss.
Die Definition ist simpler als die der Scrawl Scripts und wird in einem eigenen VitaEffect System verarbeitet.

TBD - Ist stark vom Vita System abhaengig, ist noch nicht final definiert.

## Model

### ScrawlSubject


### ScrawlExecContext - Execution Context

- actor: ScrawlSubject
- patients: ScrawlSubject[]

### Scrawl Klassen

- ScrawlEffectFactory - Factory fuer Effekt-Handler-Instanzen, Registry fuer Effekt-Handler-Klassen in der factory
- ScrawlEffectHandler - Basisklasse fuer Effekt-Handler
- (Obsolate: ScrawlEffectSystem - Schnittstelle fuer das Triggern von Effekten - wird direkt in einer methode in ScrawlExecutor integriert) 
- ScrawlScript - Envelop Objekt fuer ein Effekt-Script, haelt den root und current step ... (daten, status)
- ScrawlStep (basis): StepPlay, StepWait, StepSequence ... - Typen fuer Effekt-Script Definition
- ScrawlExecutor - Executor instance eines scrawl scripts (EffectSequencer) - fuehrt die steps aus (mechanik)
- ScrawlService - Zentrale Verwaltung aller laufenden ScrawlExecutors, EffectFactory

### Imports

- Scripte keonnen templates/sub sequences importieren, diese werden wie assets geladen und in den script store eingefuegt, sie werden im ScrawlServcie verwaltet, geladen, gecached.
- sub sequences koennen auch gecallt werden
- Ein Import ist genau wie ein script aufgebaut, wird aber nicht ausgefuehrt, es fehlt der main teil.
- Scrawl Scripte haben immer die Endung .scrawl.json
- Wenn die Endung beim laden nicht dran ist, wird die automatisch angehaengt.

### Bedingungen

- LodSwitch
- If ... Conditions, wird vor allem im Zusamenhang mit parametern und LOD genutzt
- Conditions:
  - VarEquals 
  - VarExists
  - HasTargets
  - HasSource
  - IsVarTrue - exists and true, default value kann definiert werden, wenn nicht existiert
  - IsVarFalse - not exists or false, default value kann definiert werden, wenn nicht existiert

### Parameter definition

Im Script gibt es eine Parameter defintiiton section, in der die benoetigten parameter definiert werden koennen.

- name: string
- type: string (number, string, boolean, color, vector3)
- default: any (optional)
- description: string (optional)

Source und targets koennen immer als parameter genutzt werden, muessen nicht definiert werden.

### Script Teile

- Imports
- Parameter definitions
- Templates
- Sequences

Die sequence mit dem namen main ist der haupt entry point eines scripts.

### ScriptActionDefinition

Die Action Definition wird ueberall anghaemngt, wo ein Scrawl Script ausgefuehrt werden soll:

ScriptActionDefinition:
- scriptId: string
- parameters: Record<string, any> (optional)
- script: ScrawlScript (optional, inline definition)

## Umsetzung

[x] Erstelle die Model interfaces und basis klassen in shared/src/scrawl/ 
- ScrawlScript
- ScrawlStep Typen: 
  - StepPlay
  - StepWait 
  - StepSequence
  - StepParallel 
  - StepRepeat
  - StepForEach
  - StepLodSwitch
  - StepCall - fuehrt eine sub sequence aus
  - StepIf + Conditions
- ScriptActionDefinition

[x] Erstelle die implementierung in engine/src/scrawl/
- ScrawlEffectFactory
- ScrawlEffectHandler - execute(ctx: ScrawlExecContext, step: ScrawlStep): void
- ScrawlExecutor
- ScrawlService - in AppContext referenzieren
- ScrawlExecContext - Hier eine Referenz auf AppContext, auf ScrawlExecutor

[x] Implementiere die Logic in ScrawlExecutor um die steps auszufuehren

[x] Erstelle Unit Tests die die funktionen im ScrawlExecutor testen

[x] Erstelle commandos um mit ScrawlService scripte zu listen, zu starten, stoppen

[x] Erstelle ein effect LogEffect der einen text im log ausgibt (zum testen), in die EffectFactory registrieren
- Gib mir ein beispiel, wie ich mit dem command einen log effekt starten kann.
```text
doScrawlStart('{
    "id": "test-log",
    "root": {
      "kind": "Sequence",
      "steps": [
        {
          "kind": "Play",
          "effectId": "log",
          "ctx": {
            "message": "Starting in 10 seconds...",
            "level": "info"
          }
        },
        {
          "kind": "Wait",
          "seconds": 10
        },
        {
          "kind": "Play",
          "effectId": "log",
          "ctx": {
            "message": "Hello from Scrawl!",
            "level": "info"
          }
        }
      ]
    }
  }')
```

[?] Erstelle einen Effekt, der ein Commando im CommandService ausfuehrt:
- cmd: string
- parameter0: any
- parameter1: any
- ... 
- parameter10: any

```text
doScrawlStart({
    id: "chat-notification",
    root: {
      kind: "Play",
      effectId: "command",
      ctx: {
        cmd: "notification",
        parameter0: 11,
        parameter1: "ScrawlBot",
        parameter2: "Script executed successfully!"
      }
    }
  })

  // Overlay-Nachricht (type 20)
  doScrawlStart({
    id: "overlay-notification",
    root: {
      kind: "Play",
      effectId: "command",
      ctx: {
        cmd: "notification",
        parameter0: 20,
        parameter1: "null",
        parameter2: "LEVEL UP!"
      }
    }
  })
```

[x] Ich moechte command staerker in Scrawl Scripts nutzen koennen, erstelle einen StepCommand:
{
  kind: "Cmd",
  cmd: "notification",
  parameters: [ 20, "null", "LEVEL UP!"]
  }
}
```text
 doScrawlStart({
    id: "multi-command",
    root: {
      kind: "Sequence",
      steps: [
        {
          kind: "Cmd",
          cmd: "notification",
          parameters: [11, "Game", "Quest started!"]
        },
        {
          kind: "Wait",
          seconds: 2
        },
        {
          kind: "Cmd",
          cmd: "teleport",
          parameters: [100, 64, 200]
        },
        {
          kind: "Cmd",
          cmd: "notification",
          parameters: [0, "System", "Teleported to quest location"]
        }
      ]
    }
  })
```

[ ] Erweitere Items (ItemData.ts) so, das ScriptActionDefinition als onUseEffekt Effekt definiert werden kann.
- Wenn ein Item per shortcut ausgefuehrt wird, wird der ScrawlService genutzt um das Script zu starten - wenn vorhanden.

[ ] Erstelle einen Scrawl Script Editor in 'controls' mit dem Scripte in den assets erstellt und bearbeitet werden koennen.
- Liste von .scrawl.json Scripten und suche. Nur anzeige von Dateinamen. (AssetPrview control benutzen)
- Mit Add new Script wird der Editor mit einem leeren Script geoeffnet.
- Auf den gefunden elementen kann ein Icon 'Duplicate' geklickt werden, um das Script zu kopieren und im Editor zu oeffnen.
- Bei Click auf einen Script wird der Editor geoeffnet.
- Im Editor gibt es oben ein Input fuer die Script Id, die bei neuen Scripten editierbar ist, bei bestehenden nicht.
- Die Scripte sollen in einem Visuellen Editor bearbeitet werden koennen, die darstellung ist frei waehlbar
  - es sollen neue Steps hinzugefuegt werden koennen
  - bestehende Steps sollen editiert werden koennen
  - Steps koennen verschoben und geloescht werden
  - Je nach step gibt es unter slots
  - Die sub sequence ist wahlbar, es wird dann nur die sub sequence angezeigt, es koennen neue erstellt und welche geloescht werden
- Es soll einen Button Source geben, der das JSON des Scripts anzeigt und editierbar macht.
- Eine Liste von moeglichen Effekten/Commands (Kind:Cmd) soll aus einer asset-Datei unter 'scrawl/effects.json' geladen werden und im Editor als presets angezeigt werden.

[ ] Erstelle einen Shader, der eine runde Fläche anzeigt (VFX Circle Marker) Die Fläche wird innerhalb der gegebenen spredDuration von 0 auf radius skaliert. Dann
Bleibt die Fläche fuer stayDuration sekunden sichtbar und fadet dann aus.
- Parameter: radius, color, stayDuration, spreadDuration, alpha 
- Der Effekt soll in der EffectFactory registriert werden.

