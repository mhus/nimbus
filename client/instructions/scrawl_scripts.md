
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

## Umsetzung Scrawl Engine

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

[x] Erstelle einen Effekt, der ein Commando im CommandService ausfuehrt:
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

[-] Es wird ein weiterer Kind 'while' in Scrawl Scripts benoetigt.
While hat zwei use cases:
1. Solange laufen bis ein anderer paralleter task beendet wird (z.b. warten auf das ende einer animation und in der zeit einen sound im loop abspielen) - Hier wird vermutlich auch der Effekt diese Eigenschaft unterstuetzen muessen.
2. Solange laufen bis ein Event kommt, z.b. mouse wieder losgelassen wird. 
   - Dabei muss auch ein target Vektor anpassbar sein, z.b. solange die maus gedrueckt wird, bewege das ziel auf die maus position. Diese Anforderung ist beschränkt auf die source und target positionen. 

- Beide sollen durch den Kind 'While' abgedeckt werden koennen.
- Nicht jeder Effekt muss das unterstuetzen. ggf geben wir diesen effekten spezielle namen, z.b. 'sound:loop_while' oder 'move:towards_while'
- Effekte sollen immer sauber aufgereaumt werden, es soll immer ein dispose() geben, das zuverlaessig aufgerufen wird, wenn der effekt beendet wird.
- Effekte sollen nie ewig laufen koennen, es muss immer einen timeout geben, der kann by default hoch sein, aber es muss ihn geben.
- Welche erweiertung ist im Effekt system notwendig, um den use case 1 zu unterstuetzen?
  - z.b. ein stop() oder disposal() methode im EffectHandler?

> Reden wir ueber zwei Kinds: Ich wuerde die 'While' und 'Until' nennen. While fuer LoopWhileTask, und until fuer LoopWhileEvent (Until Finished Event)

[?] While Kind implementieren

StepWhile - Loop während Parallel-Task läuft

- Zweck: Effekt wiederholen während ein anderer Task aktiv ist
- Terminierung: Automatisch wenn referenzierter Task endet
- Use Case: Sound loopen während Animation läuft
- Besonderheit: Keine Parameter-Updates nötig, einfache Task-Synchronisation

[?] Until Kind implementieren

StepUntil - Loop bis Event eintritt

- Zweck: Effekt wiederholen bis ein Event eintritt
- Terminierung: Event-basiert (z.B. mouse:up, custom events)
- Use Case: Beam folgt Maus, Charging-Effekt während Taste gedrückt
- Besonderheit: Unterstützt dynamische Parameter-Updates

[?] Default variablen im ScrawlExecutor implementieren
- $source - das source subject (actor)
- $target - das target subject (patient[0])
- $targets - array aller targets (patients)
- $item - das item, das den effekt ausgeloest hat (wenn vorhanden)
- $itemId - die id des items (wenn vorhanden)
- $itemTexture - die texture des items (wenn vorhanden)
- $itemName - der name des items (wenn vorhanden)

Wenn ein Script con einem Item ausgefuehrt wird (Shortcuts), werden diese variablen automatisch gesetzt.

[x] Erstelle in client/packages/engine/src/scrawl/effects eien Dokumentation, wie Effekte gebaut werden, damit spaeter neue Effekte problemlos hinzugefuegt werden koennen.
```text
  Speicherort: client/packages/engine/src/scrawl/effects/EFFECT_DEVELOPMENT.md
```

## Embedding Scripts

[?] Erweitere Items (ItemData.ts) so, das ScriptActionDefinition als onUseEffekt Effekt definiert werden kann.
- Wenn ein Item per shortcut ausgefuehrt wird, wird der ScrawlService genutzt um das Script zu starten - wenn vorhanden.

[?] Erweiterung von Shotcuts:
- Es muss auch das ende eines shortcut events geben, z.b. wenn die maus/key losgelassen wird.
- Es muss moeglich sein, während des gedrückt haltens eines shortcuts, die position des source / targets neu zu setzen.
Brauchen wir einen ShortcutService der Shortcuts managed? Aktuell wird das alles im PlayerServie und ??? verarbeitet.
- Beim Start eines effekts muss das gestartete Script an den Shortcut gebunden werden, damit 


[ ] Jetzt alles zusammen: 
- Ich brauche ein script in client/packages/test_server/files/assets/scrawl das einen  particleBeam fuer 3 sekunden von 
  source zu target zeigt. nenne es 'beam1.scrawl.json'.
- Ich brauche einen ItemType der dieses script als onUseEffect nutzt.


## Editor

[x] Erstelle einen Scrawl Script Editor in 'controls' mit dem Scripte in den assets erstellt und bearbeitet werden koennen.
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

[x] Erstelle exemplarisch eine Scrawl Script Bibliothek in den assets in client/packages/test_server/files/assets/scrawl/effects.json

[x] Erweitere die Server REST API um eine liste von items zu suchen
- Siehe "Item Suchen" in client/instructions/general/server_rest_api.md

[x] Erstelle einen Item Editor in 'controls' der es erlaubt Items (ItemData) zu bearbeiten
- Liste von Items mit Suche
- Item anlegen, loeschen
- Item bearbeiten
- onUseEffect soll mit dem Scrawl Script Editor bearbeitet werden koennen



## Effects

[x] Erstelle einen Shader, der eine Fläche anzeigt (VFX Circle Marker) Die Fläche wird innerhalb der gegebenen spredDuration von 0 auf radius skaliert. Dann
Bleibt die Fläche fuer stayDuration sekunden sichtbar und fadet dann aus.
- Parameter: radius, color, stayDuration, spreadDuration, alpha 
- Der Effekt soll in der EffectFactory registriert werden.
- Ausserdem soll sich die flaeche um das zentrum rotieren. Es soll eine roatationsgeschwindigkeit mitgegeben werden
Erstelle einen Effekt der den Shader aus dem ShaderService laed und eine Texure mit dem MaterialService (?) und den Effekt ausfuehrt
- Zusaetzlich zu den Shader parametern wird die Texture mitgegeben
- Die textur soll kurz oberhalb des Blocks angezeigt werden (y + 0.05)
- ggf parameter zu transparnz
Es soll erreicht werden, das sich eine flaeche aufbaut um eine stelle herum (z.b. aoe effekt) und dann wieder verschwindet.
Die Darstellung soll moeglichst konfigurierbar sein.
- Dem effekt soll einee coordinate (x,y,z) uebergeben werden - source oder target position
- Dem effekt soll eine radius uebergeben werden
- Dem effekt soll eine farbe uebergeben werden
- Dem effekt soll eine dauer fuer den aufbau (spreadDuration) und eine dauer fuer das verbleiben (stayDuration) uebergeben werden
- Dem effekt soll eine rotationSpeed uebergeben werden
- Dem effekt soll eine alpha (transparenz) uebergeben werden
- Dem effekt soll eine texture uebergeben werden (asset pfad)
Erstelle in client/packages/test_server/files/assets/scrawl/effects.json ein preset um diese effekt zu benutzen.

```text
doScrawlStart({
    id: "aoe_at_position",
    root: {
      kind: "Sequence",
      steps: [
        {
          kind: "Play",
          effectId: "circleMarker",
          ctx: {
            position: { x: -1, y: 70, z: 18 },
            radius: 5,
            color: "#ff6600",
            spreadDuration: 0.5,
            stayDuration: 2.0,
            rotationSpeed: 2.0,
            alpha: 0.7,
            shape: "circle",
            texture: "textures/block/basic/redstone_ore.png"
          }
        },
        {
          kind: "Wait",
          seconds: 2.5
        },
        {
          kind: "Cmd",
          cmd: "notification",
          parameters: [20, "null", "BOOM!"]
        }
      ]
    }
  })
```


[x] Erstelle einen Effect 'projectile' der einen projectile fliegen lässt.
Das projektile soll von einer start position zu einer ziel position fliegen.
- Parameter: startPosition - Vector3
- Parameter: targetPosition - Vector3
- Parameter: projektileWidh // full size of the projektile
- Parameter: projectileHeadWidth // site of the first part - head - of the projektile where it becomes a smaller size
- Parameter: projektileRadius
- Parameter: projectileTexture
- Parameter: speed
- Parameter: rotationSpeed
Das Projektile soll sich auf dem weg zum ziel drehen (rotationSpeed)
Wenn das projektile das ziel erreicht hat verschwindet es. (dispose)

```text
 doScrawlStart({
    id: "fireball",
    root: {
      kind: "Play",
      effectId: "projectile",
      ctx: {
        startPosition: { x: 0, y: 65, z: 0 },
        targetPosition: { x: 10, y: 65, z: 10 },
        projectileWidth: 0.8,
        projectileHeadWidth: 0.2,
        projectileRadius: 0.3,
        projectileTexture: "effects/fireball.png",
        speed: 15,
        rotationSpeed: 8,
        shape: "bullet",
        color: "#ff6600"
      }
    }
  })
```

[?] Erstelle einen effekt 'particleBeam' der einen beam zwischen zwei positionen anzeigt.
Es soll eine interaktion mit einem Zauberstab simulieren.
- Parameter: startPosition - Vector3
- Parameter: endPosition - Vector3
- Parameter: color1
- Parameter: color2
- Parameter: color3
- Parameter: duration sekunden
- Parameter: thickness
- Parameter: setupDuration - In dieser zeit baut sich der Beam vom start zum end auf, default 0.2 sekunden
- Parameter: fadeDuration - In dieser zeit fadet der Beam am ende aus, default 0.2 sekunden
Es soll keine texture benutzt werden, sondern partikel oder ein shader.
Es werden drei Farben uebergeben, die den beam farblich gestalten. ggf gibt es drei strahlen, die sich umeinander winden.
Zur bewegung der particle sollen random zahlen benutzt werden.

```text
doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleBeam",
      "ctx": {
        "startPosition": {"x": 0, "y": 65, "z": 0},
        "endPosition": {"x": 10, "y": 70, "z": 10},
        "color1": "#ff0000",
        "color2": "#00ff00",
        "color3": "#0000ff",
        "duration": 2.0,
        "thickness": 0.15,
        "speed": 1.5,
        "alpha": 0.5  // 50% transparent
      }
    }
  })
  
  1. Ausfransung (Fraying) - HIGHLIGHT 🌟

  - fraying (0-1) - Stärke des Ausfransens am Beam-Ende
  - frayingDistance (0-1) - Ab wo das Ausfransen beginnt (default: 0.5 = Mitte)
  - frayingNoise - Zufälligkeit des Ausfransens (default: 1.0)

  2. Spiral-Kontrolle:

  - spiralSpeed - Rotationsgeschwindigkeit (default: 4.0)
  - spiralRadius - Radius-Multiplikator (default: 2.0)
  - spiralPattern - 'helix' | 'twist' | 'wave' | 'none'
    - helix: Gleichmäßige Spirale
    - twist: Sich beschleunigende Spirale
    - wave: Sinusförmige Wellenbewegung

  3. Beam-Verhalten:

  - beamStyle - 'continuous' | 'pulsing' | 'flickering' | 'lightning'
    - continuous: Durchgehender Beam
    - pulsing: Pulsierender Beam
    - flickering: Flackernder Beam
    - lightning: Blitz-Effekt (Flicker + Turbulenz)
  - pulseFrequency - Pulsfrequenz in Hz (default: 2.0)
  - turbulence (0-1) - Zufällige Abweichungen für Lightning-Effekte

  4. Partikel-Eigenschaften:

  - particleCount - Anzahl Partikel pro Strang (default: 2000)
  - emitRate - Emission-Rate Override
  - particleLifetime - Lebensdauer in Sekunden (default: 0.2)

  5. Beam-Geometrie:

  - beamTaper - 'none' | 'start' | 'end' | 'both'
    - Beam verjüngt sich an den Enden
  - strandSeparation - Abstand zwischen Strängen (default: 1.0)

  6. Fortgeschrittene Effekte:

  - glow - Glow-Effekt aktivieren (boolean)
  - glowIntensity - Glow-Stärke (default: 1.0)
  - coreColor - Separate Kernfarbe für Mittenstrang (hex)

  7. Visuals:

  - blend - 'add' | 'alpha' | 'multiply' (default: 'add')

  📝 Drei detaillierte Usage-Beispiele:

  1. Basic magical beam - Einfacher magischer Beam
  2. Frayed lightning beam - Ausgefranster Blitz mit Turbulenz
  3. Pulsing beam with glow - Pulsierender Beam mit Glow-Effekt

  🎨 Features Highlights:

  - ✅ Fraying-Effekt: Beam franst am Ende natürlich aus
  - ✅ Verschiedene Spiral-Muster: Helix, Twist, Wave
  - ✅ Beam-Styles: Continuous, Pulsing, Flickering, Lightning
  - ✅ Turbulenz: Für realistische Lightning-Effekte
  - ✅ Beam-Taper: Verjüngung an Start/Ende/Beiden
  - ✅ Glow-System: Separates Partikelsystem für Glow
  - ✅ Flexible Blending-Modi: Add, Alpha, Multiply
  - ✅ Core-Color Support: Separate Farbe für den Mittenstrang  
```

[x] Gewichtung fuer die drei Farben mit Veränderung der Intensität auf Zeit je Farbe
[ ] Fluktuation Richtung Target: Partikel können den Beam verlassen und wieder zurueckkehren
[x] Umbennennen von Effekt 'beam' zu 'particleBeam'

[x] Erstelle einen Effekt 'particleExplosion' der an einer stelle eine Partikel Explosion ausloest.
- Parameter: position - Vector3
- Parameter: color1 - string
- Parameter: color2 - string
- Parameter: color3 - string
- Parameter: color1Weight - number (0-1) - default: 1
- Parameter: color2Weight - number (0-1) - default: 1
- Parameter: color3Weight - number (0-1) - default: 1
- Parameter: initialRadius - number
- Parameter: spreadRadius - number
- Parameter: particleCount - number
- Parameter: particleSize - number
- Parameter: duration - number (sekunden)
- Parameter: speed - number
- Parameter: alpha - number

```text
 doScrawlStart({
    id: "fireball",
    root:  {
    "kind": "Play",
    "effectId": "particleExplosion",
    "ctx": {
      "position": {"x": -1, "y": 70, "z": 18},
      "color1": "#ff0000",
      "color2": "#ff9900",
      "color3": "#ffff00",
      "color1Weight": 1.0,
      "color2Weight": 0.7,
      "color3Weight": 0.3,
      "initialRadius": 0.1,
      "spreadRadius": 5.0,
      "particleCount": 200,
      "particleSize": 0.2,
      "duration": 1.5,
      "speed": 1.0,
      "alpha": 0.8
    }
  }
})

Zusammenfassung der Implementierung

  ✅ Alle erweiterten Parameter implementiert:

  Richtung & Kegel:

  - direction - Hauptrichtung der Explosion (Vector3)
  - directionStrength - Wie stark die Richtung beeinflusst (0-1)
  - coneAngle - Kegelwinkel in Grad (0-180°)

  Physik:

  - gravity - Schwerkraftvektor (z.B. {x: 0, y: -9.81, z: 0})
  - drag - Luftwiderstand (0-1)
  - rotation - Partikelrotation aktivieren (boolean)
  - angularVelocity - Rotationsgeschwindigkeit (Radiant/Sekunde)

  Größenanimation:

  - minParticleSize / maxParticleSize - Präzise Größenkontrolle
  - sizeOverLifetime - 'constant' | 'grow' | 'shrink' | 'pulse'

  Emissionsmuster:

  - emissionPattern - 'sphere' | 'hemisphere' | 'cone' | 'ring' | 'disc'
  - burstCount - Anzahl separater Bursts
  - burstDelay - Verzögerung zwischen Bursts

  Visuelle Effekte:

  - fadeInDuration - Fade-in Zeit in Sekunden
  - fadeOutDuration - Fade-out Zeit in Sekunden
  - blend - 'add' | 'alpha' | 'multiply'

  📝 Drei Usage-Beispiele in der Dokumentation:

  1. Basic radial explosion - Einfache Explosion in alle Richtungen
  2. Directional explosion with gravity - Gerichtete Explosion mit Schwerkraft und Kegel
  3. Ring explosion with multiple bursts - Ring-Pattern mit mehreren Bursts und Rotation

  🚀 Features:

  - ✅ Alle Parameter sind optional mit sinnvollen Defaults
  - ✅ Multiple Bursts mit Verzögerung möglich
  - ✅ Verschiedene Emissionsmuster (Kugel, Halbkugel, Kegel, Ring, Disc)
  - ✅ Physik-Simulation mit Schwerkraft und Drag
  - ✅ Flexible Größenanimationen über Lifetime
  - ✅ Gerichtete Explosionen mit einstellbarer Stärke
  - ✅ Partikelrotation mit konfigurierbarer Geschwindigkeit
  - ✅ Verschiedene Blending-Modi
  - ✅ Flexible Fade-Zeiten
```

[x] Erstelle einen Effekt 'particleFire' der an einer position ein Feuer mit Partikeln simuliert.
- Parameter: position - Vector3
Weitere sinnvolle parameter fuer ein feuer partikel system
```text
 5 Partikelsystem-Layer:

  1. Core Layer - Heiße weiße/gelbe Basis
  2. Inner Flame Layer - Innere gelbe Flammen
  3. Mid Flame Layer - Mittlere orange Flammen
  4. Outer Flame Layer - Äußere orange/rote Flammen
  5. Smoke Layer - Aufsteigender grauer Rauch
  6. Spark Layer - Leuchtende Funken (optional)

  6 Feuer-Stil-Presets:

  - campfire: Kleines gemütliches Lagerfeuer mit Rauch
  - torch: Fackel - schmal und hoch
  - bonfire: Großes intensives Feuer mit vielen Funken
  - magical: Magisches lila/pink Feuer
  - blue: Blaues heißes Feuer
  - green: Grünes magisches Feuer
  - custom: Vollständig anpassbar

  Haupt-Features:

  - ✅ Endlos oder zeitlich begrenzt: duration: Infinity für endloses Feuer
  - ✅ Steady Effect Support: Kann manuell gestoppt werden
  - ✅ Rauch-System: Aktivierbar mit Menge und Höhe
  - ✅ Funken-System: Leuchtende aufsteigende Funken
  - ✅ Turbulenz & Flackern: Realistisches Flackern
  - ✅ Wind-Unterstützung: Feuer reagiert auf Wind
  - ✅ Fade In/Out: Smooth Ein- und Ausblenden
  - ✅ Color & Size Gradients: Partikel ändern Farbe und Größe über Lifetime
  - ✅ Vollständig anpassbar: Alle Farben, Größen und Verhaltensweisen konfigurierbar

  Parameter-Übersicht (30+):

  - Position, Größe, Intensität, Höhe, Dauer
  - 6 Farbparameter (Core, Flames, Smoke, Sparks)
  - Turbulenz, Flicker-Speed, Spread, Wind
  - Rauch-Einstellungen (aktivieren, Menge, Höhe)
  - Funken-Einstellungen (aktivieren, Anzahl, Intensität)
  - Timing (Fade-in, Fade-out)
  - Partikel-Eigenschaften

  4 Usage-Beispiele in JSDoc:

  1. Endloses Campfire
  2. Fackel für 30 Sekunden
  3. Magisches Feuer mit Turbulenz
  4. Custom blaues Feuer ohne Rauch

  🔥 Das Feuer ist jetzt einsatzbereit!

  Du kannst es testen mit:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleFire",
      "ctx": {
        "position": {"x": -1, "y": 70, "z": 18},
        "fireStyle": "campfire"
      }
    }
  })

  Oder für ein zeitlich begrenztes Feuer:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleFire",
      "ctx": {
        "position": {"x": -1, "y": 70, "z": 18},
        "fireStyle": "bonfire",
        "duration": 10
      }
    }
  })
  
doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleFireSteady",
      "ctx": {
        "position": {"x": 0, "y": 65, "z": 0},
        "fireStyle": "bonfire"
      }
    }
  })  
```

[x] Erstelle einen Effekt 'particlePositionFlash' der an einer position einen hellen Partikel Blitz simuliert.
Der Blitz kommt von oben auf das Ziel zu und verschwindet wieder.
- Parameter: position - Vector3
Weitere sinnvolle parameter fuer ein feuer partikel system
```text
5 Partikelsystem-Komponenten:

  1. Main Lightning Bolt - Haupt-Blitzstrahl mit Zickzack-Pfad
  2. Branch Systems (3-5x) - Seitliche elektrische Verzweigungen
  3. Glow Effect - Blaues/weißes Leuchten entlang des Blitzes
  4. Impact Flash - Radiale Explosion am Auftreffpunkt
  5. Afterglow - Nachleuchtender Effekt

  5 Lightning-Style-Presets:

  - natural: Realistischer weiß/blauer Blitz
  - magical: Magischer lila/pink Blitz
  - blue: Blaue Elektrizität
  - red: Roter Energie-Blitz
  - purple: Lila Magie-Blitz

  Haupt-Features:

  - ✅ Von oben nach unten: Blitz schlägt von einstellbarer Höhe ein
  - ✅ Zickzack-Pfad: Realistischer zackiger Verlauf (konfigurierbar)
  - ✅ Verzweigungen: Seitliche elektrische Bögen
  - ✅ Glow & Afterglow: Leuchteffekte mit Nachglühen
  - ✅ Impact Flash: Explosion am Auftreffpunkt
  - ✅ Phasen-System: Strike → Impact → Glow
  - ✅ Sehr schnell: Blitz erscheint fast instant (konfigurierbare Speed)

  Parameter (20+):

  - Basis: position, startHeight, intensity, duration
  - Lightning: thickness, branches, branchIntensity, zigzag
  - Farben: coreColor, glowColor, impactColor
  - Timing: strikeSpeed, flashDuration, glowDuration, impactDuration
  - Effekte: glow, impact, afterglow (alle aktivierbar)
  - Stil: lightningStyle (5 Presets)
  - Partikel: particleCount

  3 Usage-Beispiele in JSDoc:

  1. Standard Lightning Strike
  2. Intense Lightning mit vielen Branches
  3. Magical Purple Lightning

  ⚡ Spezielle Implementierungen:

  1. Zickzack-Algorithmus: Berechnet realistischen Blitzpfad mit 20 Segmenten
  2. Branch-Point-System: Zufällige Verzweigungen entlang des Hauptpfads
  3. Pfad-basierte Partikel: Custom Update positioniert Partikel entlang des Blitzpfads
  4. Phasen-Timing: Koordiniert Main Bolt, Impact und Glow
  5. Impact-System: Radiale Explosion am Auftreffpunkt

  🎯 Der Blitz ist jetzt einsatzbereit!

  Teste es mit:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particlePositionFlash",
      "ctx": {
        "position": {"x": -1, "y": 70, "z": 18}
      }
    }
  })

  Oder für einen intensiven Blitz:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particlePositionFlash",
      "ctx": {
        "position": {"x": -1, "y": 70, "z": 18},
        "intensity": 2.0,
        "branches": 5,
        "thickness": 0.4,
        "zigzag": 0.9
      }
  }})
```

[?] Erstelle einen Effekt 'particleWandFlash' der wie aus enem Zauberstab ein Partikel Blitz von der Quelle zur Ziel Position simuliert.
- Parameter: source - Vector3
- Parameter: target - Vector3
Weitere sinnvolle parameter fuer ein feuer partikel system
```text
 1. particleWandFlash (One-Shot)

  - isSteadyEffect(): false
  - duration: Default 1.0 Sekunde
  - Endet automatisch nach duration
  - Mit Fade-In und Fade-Out
  - Kein isRunning() nötig

  Usage:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleWandFlash",
      "ctx": {
        "source": {"x": -1, "y": 70, "z": 18},
        "target": {"x": -1, "y": 75, "z": 23},
        "wandStyle": "powerful",
        "duration": 2.0
      }
    }
  })

  2. particleWandFlashSteady (Steady)

  - isSteadyEffect(): true
  - Kein duration parameter (endlos)
  - Läuft bis stop() aufgerufen wird
  - Nur Fade-In, kein Fade-Out
  - isRunning() implementiert

  Usage:
  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleWandFlashSteady",
      "ctx": {
        "source": {"x": -1, "y": 70, "z": 18},
        "target": {"x": -1, "y": 75, "z": 23},
        "wandStyle": "healing",
        "animationMode": "pulsing"
      }
    }
  })

  🎯 Vollständige Effekt-Liste (5 neue Partikel-Effekte):

  1. ✅ particleExplosion - Radiale Explosionen (One-Shot)
  2. ✅ particleFire - Feuer-Simulation (Steady/One-Shot hybrid)
  3. ✅ particlePositionFlash - Blitzschlag von oben (One-Shot)
  4. ✅ particleWandFlash - Zauberstab-Strahl zeitlich begrenzt (One-Shot)
  5. ✅ particleWandFlashSteady - Zauberstab-Strahl endlos (Steady)

  📊 Vorteile der Trennung:

  - ✅ Klare Semantik: Nutzer wissen sofort welchen Effekt sie brauchen
  - ✅ Einfacherer Code: Keine komplexen Modus-Checks
  - ✅ Bessere Wartbarkeit: Jede Klasse hat eine klare Verantwortung
  - ✅ Folgt Best Practices: Ähnlich wie sound:loop vs normale Sound-Effekte


 1. Alpha-Transparenz:

  - alpha?: number (0-1, default: 1.0) - Gesamt-Transparenz für alle Layer

  2. Blending-Modus:

  - blend?: 'add' | 'alpha' | 'multiply' (default: 'add')
    - add: Additive Blending (hell, leuchtend)
    - alpha: Standard Alpha-Blending
    - multiply: Multiplikatives Blending (dunkel)

  3. Spark-Farbe:

  - sparkColor?: string (default: same as coreColor) - Separate Farbe für Funken

  4. Source Glow:

  - sourceGlow?: boolean (default: false) - Leuchten an der Zauberstab-Spitze
  - sourceGlowRadius?: number (default: 0.3) - Größe des Quell-Leuchtens

  5. Beam-Struktur:

  - strandCount?: number (1-5, default: 2) - Anzahl der Beam-Stränge
    - 1 Strand: Nur Core
    - 2 Stränge: Core + Outer (default)
    - 3+ Stränge: Zusätzliche Layer für dickere Beams

  6. Partikel-Rotation:

  - particleRotation?: boolean (default: false) - Partikel rotieren aktivieren
  - angularVelocity?: number (default: 0) - Rotationsgeschwindigkeit in rad/s

  📊 Gesamt-Parameter beider Effekte:

  particleWandFlash (One-Shot): 30+ Parameter
  particleWandFlashSteadyEffect (Steady): 29+ Parameter (kein duration/fadeOut)

  🎨 Beispiel mit neuen Parametern:

  doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleWandFlash",
      "ctx": {
        "source": {"x": -1, "y": 70, "z": 18},
        "target": {"x": -1, "y": 75, "z": 23},
        "wandStyle": "custom",
        "thickness": 0.3,
        "alpha": 0.8,
        "blend": "add",
        "strandCount": 3,
        "sourceGlow": true,
        "sourceGlowRadius": 0.5,
        "sparkColor": "#ffff00",
        "particleRotation": true,
        "angularVelocity": 3.14,
        "duration": 2.0
      }
    }
  })  
```

[?] Die Effekte 'particleWandFlash' und 'particleWandFlashSteady' sollen Anpassungen
die mit onParameterChanged() gemacht werden unterstuetzen.
- Es muessen nur die beiden parameter 'source' und 'target' unterstuetzt werden damit die beiden enden des flash angepasst 
  werden koennen.

[x] Erstelle einen Effekt 'playSoundLoop' der einen Sound in einer Schleife abspielt, bis der Effekt beendet wird.
- Parameter: soundClip - string (asset pfad)
- Parameter: volume - number (0-1)
- Parameter: position - Vector3 (optional, wenn 3D sound)
- Parameter: stream - boolean (ob der sound gestreamt werden soll, default false)
- Benutze den AudioService um den sound abzuspielen.
- Der Effekt ist isSteadyEffect() == true und beendet bei stop() die wiedergabe des sounds.
- Dispose den sound bei stop() wieder
```text
 2D Beispiel (Ambiente):
doScrawlStart({
  "root": {
    "kind": "Play",
    "effectId": "playSoundLoop",
    "ctx": {
      "soundClip": "audio/ambiente/TremLoadingloopl.ogg",
      "volume": 0.8,
      "stream": true
    }
  }
})  

  3D Beispiel (räumlicher Sound):
  
doScrawlStart({
  "root":   {
    "kind": "Play",
    "effectId": "playSoundLoop",
    "ctx": {
      "soundClip": "audio/ambiente/TremLoadingloopl.ogg",
      "volume": 1.0,
      "position": {"x": 100, "y": 65, "z": 200},
      "stream": true
    }
  }
})  
```

[x] Erstelle einen Effekt 'playSound' der einen Sound einmal abspielt.
- Parameter: soundClip - string (asset pfad)
- Parameter: volume - number (0-1)
- Parameter: position - Vector3 (optional, wenn 3D sound)
- Parameter: stream - boolean (ob der sound gestreamt werden soll, default false)
- Benutze den AudioService um den sound abzuspielen.
- Dispose den sound bei stop() wieder

Implementierung: `PlaySoundEffect` in `/client/packages/engine/src/scrawl/effects/PlaySoundEffect.ts`
- Registriert als 'playSound'
- Kein Steady Effect (`isSteadyEffect()` = false) - endet automatisch nach Playback
- Unterstützt 2D (non-spatial) und 3D (spatial) Sound
- 2D: Verwendet CreateSoundAsync() direkt (nicht gecacht), disposed automatisch nach Playback via onEndedObservable
- 3D: Verwendet AudioService.playSoundAtPosition() mit Pool-System (auto-released)
- Sound wird automatisch disposed wenn Playback endet oder stop() aufgerufen wird

Beispiel 2D:
```text
doScrawlStart({
  "root":  {
  "kind": "Play",
  "effectId": "playSound",
  "ctx": {
    "soundClip": "audio/effects/explosion.wav",
    "volume": 1.0
  }
}
})  
```

Beispiel 3D:
```text
doScrawlStart({
  "root":  {
  "kind": "Play",
  "effectId": "playSound",
  "ctx": {
    "soundClip": "audio/effects/door_open.ogg",
    "volume": 0.8,
    "position": {"x": 100, "y": 65, "z": 200}
  }
}
})  
```

[x] Erstelle einen 'positionFlash' Effekt der einen Lichtblitz erzeugt.
Der Blitz kommt von oben auf das Ziel zu und verschwindet wieder.

Implementierung: `PositionFlashEffect` in `/client/packages/engine/src/scrawl/effects/PositionFlashEffect.ts`
- Registriert als 'positionFlash'
- Kein Steady Effect - endet automatisch nach duration
- Verwendet Billboard-Mesh mit Texturen (vertikal gestretcht von Himmel zu Boden)
- Unterstützt optionale Textur-Frames für animierte Blitz-Effekte
- Optionales dynamisches Licht (Point oder Spot Light)
- Fade-in/Hold/Fade-out Animation

**Optimierte Parameter:**
- `position`: Vector3 - Zielposition am Boden ✅
- `duration`: number - Gesamtdauer in Sekunden (default: 0.5) ✅
- `color`: string - Farbe des Blitzes (default: "#ffffff") ✅
- `intensity`: number - Helligkeit 0-1+ (default: 1.0) ✅
- `height`: number - Höhe über Boden wo Blitz startet (default: 20) ✅
- `width`: number - Breite/Dicke des Blitzes (default: 0.5) ✅
- `light`: boolean - Dynamisches Licht aktivieren (default: true) ✅
- `lightType`: "point" | "spot" - Lichttyp (default: "point") ✅
- `lightIntensity`: number - Licht-Stärke Multiplikator (default: 10) ✅
- `lightRange`: number - Licht-Reichweite (default: 10) ✅
- `textureFrames`: string[] - Optional: Textur-Pfade für Animation ✅
- `frameRate`: number - Bildrate für Textur-Animation in FPS (default: 30) ✅

**Entfernt/Angepasst:**
- ❌ `time` - Redundant mit `duration`, wurde entfernt
- ❌ `kind` - Wurde zu `lightType` und ist nun optional (nur relevant wenn `light: true`)

Beispiel (ohne Texturen, procedural):
```text
doScrawlStart({
  "root": {
    "kind": "Play",
    "effectId": "positionFlash",
    "ctx": {
      "position": {"x": 100, "y": 65, "z": 200},
      "duration": 0.5,
      "color": "#aaccff",
      "intensity": 1.5,
      "height": 25,
      "width": 0.8,
      "light": true,
      "lightType": "point"
    }
  }
})
```

Beispiel (mit animierten Texturen):
```text
doScrawlStart({
  "root": {
    "kind": "Play",
    "effectId": "positionFlash",
    "ctx": {
      "position": {"x": 100, "y": 65, "z": 200},
      "duration": 0.6,
      "color": "#ffffff",
      "height": 30,
      "textureFrames": [
        "textures/effects/blitz1.png"
      ],
      "frameRate": 20,
      "light": true
    }
  }
})
```

## Dynamic Fire ohne direktes Ziel

Aktuell wird Ein input event an den ShortcutService geschickt, dort wird der SelectionService gefragt, ob etwas 
selektiert ist, und wenn ja wird die action ausgefuehrt und interaction events richtung server.

Dieses Verhalten soll so mit den interaction events auch bleiben, nur die Darstellung, d.h. ausfuehren von Pose und Effekten sollen unabhaengig von
selektierten Zielen werden. Der transport der darstellung (ef.p.u 'client -> server') muss sich dazu anpassen.

- Logik mit Select und Events Richtung Server bleibt wie bisher
- Am ItemModifier brauchen wir einen neuen parameter 'actionTargeting?', 
- BOTH: bleibt alles wie bisher.
- BLOCK: Nur bloecke werden als Ziel akzeptiert.
- ENTITY: Nur entitys werden als Ziel akzeptiert.
- GROUND: der SelectionService mit dem Select TYPE BLOCK wird aufgerufen, wenn ein Block gefunden wurde wird dieser als Ziel benutzt.
- ALL: der SelectionService mit dem Select TYPE ALL wird aufgerufen, wenn ein Ziel gefunden wurde wird dieses benutzt.
  - Das Ziel kann auch ein AIR Block sein. Aber es geht um die position.
- Default ist ALL
- itemModifier.actionTargeting: ENTITY, BLOCK, BOTH, GROUND, ALL
- Wie schicken wir die events zum server
  - 'ef.p.u' events um die angepassten targets zu anderen clients zu senden
  - Block interaction / Entity interaction

```text
 Implementation Plan: Dynamic Fire ohne direktes Ziel

 Clean Architecture with Extended ef.p.u

 Executive Summary

 This plan implements the actionTargeting feature to decouple visual effects (poses/effects) from server interaction targeting. We introduce a new TargetingService for clean separation of concerns and extend ef.p.u messages to transport full
 targeting context for multiplayer synchronization.

 Key Architectural Decisions

 1. New TargetingService: Centralizes all targeting logic using Strategy Pattern
 2. Separation of Concerns: Visual targeting vs Server interaction targeting are distinct
 3. Extended ef.p.u Protocol: Adds targeting metadata for remote clients
 4. Type-Safe Design: TypeScript discriminated unions ensure compile-time safety

 ---
 Architecture Overview

 Current Flow (Problem)

 ShortcutService.fireShortcut()
   → Resolves target from SelectService (tightly coupled)
   → Sends interaction to server
   → Emits event with target
   → ItemService executes pose + effects (requires target)

 Issues:
 - Selection and server interaction are tightly coupled
 - Visual effects depend on selection state
 - Can't have effects without selected targets

 New Flow (Solution)

 ShortcutService.fireShortcut()
   → TargetingService.resolveTarget(mode='BOTH')     [For server interaction]
   → Sends interaction to server IF target matches mode
   → TargetingService.resolveTarget(mode=item.actionTargeting) [For visual effects]
   → Emits event with visual target
   → ItemService executes pose + effects (always works)

 Benefits:
 - Server interaction uses strict targeting (BOTH)
 - Visual effects use flexible targeting (ENTITY/BLOCK/GROUND/ALL)
 - Complete decoupling of visual and gameplay concerns

 ---
 Implementation Steps

 Phase 1: Type Definitions

 1.1 Add actionTargeting to ItemModifier

 File: packages/shared/src/types/ItemModifier.ts

 export type ActionTargetingMode = 'ENTITY' | 'BLOCK' | 'BOTH' | 'GROUND' | 'ALL';

 export interface ItemModifier {
   texture: string;
   // ... existing fields ...

   /**
    * Targeting mode for visual effects (pose, onUseEffect)
    *
    * - 'ENTITY': Only execute when entity is targeted
    * - 'BLOCK': Only execute when block is targeted
    * - 'BOTH': Execute when entity OR block is targeted
    * - 'GROUND': Always execute with ground position from camera ray
    * - 'ALL': Always execute (entity, block, or ground position)
    *
    * Default: 'ALL'
    *
    * Note: Server interactions always use 'BOTH' mode
    */
   actionTargeting?: ActionTargetingMode;
 }

 1.2 Create Targeting Types

 New File: packages/shared/src/types/TargetingTypes.ts

 import type { Vector3 } from '@babylonjs/core';

 export type TargetingMode = 'ENTITY' | 'BLOCK' | 'BOTH' | 'GROUND' | 'ALL';

 /**
  * Discriminated union for type-safe target resolution
  */
 export type ResolvedTarget =
   | { type: 'entity'; entity: ClientEntity; position: Vector3 }
   | { type: 'block'; block: ClientBlock; position: Vector3 }
   | { type: 'ground'; position: Vector3 }
   | { type: 'none' };

 export interface ClientEntity {
   id: string;
   currentPosition: { x: number; y: number; z: number };
   position: { x: number; y: number; z: number };
   // ... other entity fields
 }

 export interface ClientBlock {
   block: any; // BlockData
   blockType: any; // BlockType
   position: { x: number; y: number; z: number };
 }

 /**
  * Targeting context for network synchronization
  */
 export interface TargetingContext {
   mode: TargetingMode;
   target: ResolvedTarget;
 }

 1.3 Extend ef.p.u Message

 File: packages/shared/src/network/messages/EffectParameterUpdateMessage.ts

 import type { TargetingContext } from '../../types/TargetingTypes';

 export interface EffectParameterUpdateData {
   effectId: string;
   paramName: string;
   value?: any; // Now optional
   chunks?: ChunkCoordinate[];

   /**
    * NEW: Targeting context for position-based parameters
    * When paramName is 'targetPos', this provides full targeting context
    * so remote clients can properly resolve/track targets
    */
   targeting?: TargetingContext;
 }

 ---
 Phase 2: TargetingService Implementation

 New File: packages/engine/src/services/TargetingService.ts

 import { getLogger } from '@nimbus/shared';
 import type { AppContext } from '../AppContext';
 import type { TargetingMode, ResolvedTarget, ClientEntity, ClientBlock } from '@nimbus/shared';
 import { Vector3 } from '@babylonjs/core';
 import type { SelectService } from './SelectService';

 const logger = getLogger('TargetingService');

 /**
  * TargetingService - Centralized target resolution using Strategy Pattern
  *
  * Provides clean separation between:
  * - Visual targeting (for effects/poses)
  * - Interaction targeting (for server events)
  */
 export class TargetingService {
   constructor(private readonly appContext: AppContext) {
     logger.info('TargetingService initialized');
   }

   /**
    * Resolve target based on targeting mode
    *
    * @param mode Targeting mode
    * @returns Resolved target or none
    */
   resolveTarget(mode: TargetingMode): ResolvedTarget {
     const selectService = this.appContext.services.select;
     if (!selectService) {
       logger.warn('SelectService not available');
       return { type: 'none' };
     }

     // Get current selections
     const selectedEntity = selectService.getCurrentSelectedEntity();
     const selectedBlock = selectService.getCurrentSelectedBlock();

     return this.resolveTargetFromSelections(mode, selectedEntity, selectedBlock);
   }

   /**
    * Resolve target from explicit selections
    *
    * @param mode Targeting mode
    * @param entity Selected entity (or null)
    * @param block Selected block (or null)
    * @returns Resolved target
    */
   private resolveTargetFromSelections(
     mode: TargetingMode,
     entity: ClientEntity | null,
     block: ClientBlock | null
   ): ResolvedTarget {
     switch (mode) {
       case 'ENTITY':
         return this.resolveEntity(entity);

       case 'BLOCK':
         return this.resolveBlock(block);

       case 'BOTH':
         return this.resolveEntityOrBlock(entity, block);

       case 'GROUND':
         return this.resolveGround();

       case 'ALL':
         return this.resolveAll(entity, block);

       default:
         logger.warn('Unknown targeting mode, defaulting to ALL', { mode });
         return this.resolveAll(entity, block);
     }
   }

   /**
    * ENTITY strategy: Only resolve entity
    */
   private resolveEntity(entity: ClientEntity | null): ResolvedTarget {
     if (!entity) {
       return { type: 'none' };
     }

     return {
       type: 'entity',
       entity,
       position: new Vector3(
         entity.currentPosition.x,
         entity.currentPosition.y,
         entity.currentPosition.z
       ),
     };
   }

   /**
    * BLOCK strategy: Only resolve block
    */
   private resolveBlock(block: ClientBlock | null): ResolvedTarget {
     if (!block) {
       return { type: 'none' };
     }

     const pos = block.block.position;
     return {
       type: 'block',
       block,
       position: new Vector3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5),
     };
   }

   /**
    * BOTH strategy: Entity priority, fallback to block
    */
   private resolveEntityOrBlock(
     entity: ClientEntity | null,
     block: ClientBlock | null
   ): ResolvedTarget {
     if (entity) {
       return this.resolveEntity(entity);
     }
     if (block) {
       return this.resolveBlock(block);
     }
     return { type: 'none' };
   }

   /**
    * GROUND strategy: Raycast to ground plane
    */
   private resolveGround(): ResolvedTarget {
     const playerService = this.appContext.services.player;
     const cameraService = this.appContext.services.camera;

     if (!playerService || !cameraService) {
       return { type: 'none' };
     }

     const playerPos = playerService.getPosition();
     const rotation = cameraService.getRotation();

     // Calculate ray direction from camera rotation
     const pitch = rotation.x;
     const yaw = rotation.y;

     const dirX = Math.cos(pitch) * Math.sin(yaw);
     const dirY = -Math.sin(pitch);
     const dirZ = Math.cos(pitch) * Math.cos(yaw);

     // Ground plane intersection (y=0)
     if (dirY >= 0) {
       // Looking up, no ground intersection
       return { type: 'none' };
     }

     const t = -playerPos.y / dirY;
     const groundPos = new Vector3(
       playerPos.x + dirX * t,
       0,
       playerPos.z + dirZ * t
     );

     return {
       type: 'ground',
       position: groundPos,
     };
   }

   /**
    * ALL strategy: Try entity, then block, then ground
    */
   private resolveAll(
     entity: ClientEntity | null,
     block: ClientBlock | null
   ): ResolvedTarget {
     if (entity) {
       return this.resolveEntity(entity);
     }
     if (block) {
       return this.resolveBlock(block);
     }
     return this.resolveGround();
   }

   /**
    * Check if target should trigger server interaction
    *
    * @param mode Targeting mode
    * @param target Resolved target
    * @returns true if interaction should be sent
    */
   shouldSendInteraction(mode: TargetingMode, target: ResolvedTarget): boolean {
     switch (mode) {
       case 'ENTITY':
         return target.type === 'entity';
       case 'BLOCK':
         return target.type === 'block';
       case 'BOTH':
         return target.type === 'entity' || target.type === 'block';
       case 'GROUND':
       case 'ALL':
         return true; // Always send
       default:
         return true;
     }
   }

   /**
    * Convert ResolvedTarget to legacy format for backward compatibility
    */
   toLegacyTarget(target: ResolvedTarget): {
     target?: any;
     targetPosition?: { x: number; y: number; z: number };
   } {
     switch (target.type) {
       case 'entity':
         return {
           target: target.entity,
           targetPosition: {
             x: target.position.x,
             y: target.position.y,
             z: target.position.z,
           },
         };
       case 'block':
         return {
           target: {
             position: {
               x: target.position.x,
               y: target.position.y,
               z: target.position.z,
             },
             block: target.block.block,
             blockType: target.block.blockType,
           },
           targetPosition: {
             x: target.position.x,
             y: target.position.y,
             z: target.position.z,
           },
         };
       case 'ground':
         return {
           targetPosition: {
             x: target.position.x,
             y: target.position.y,
             z: target.position.z,
           },
         };
       case 'none':
         return {};
     }
   }
 }

 ---
 Phase 3: ShortcutService Integration

 File: packages/engine/src/services/ShortcutService.ts

 Changes:

 1. Import TargetingService:
 import type { TargetingService } from './TargetingService';

 2. Modify fireShortcut() (lines 71-201):

 async fireShortcut(shortcutNr: number, shortcutKey: string): Promise<void> {
   try {
     // Check if blocked
     if (this.isShortcutBlocked(shortcutNr)) {
       logger.debug('Shortcut blocked', { shortcutNr });
       return;
     }

     const playerService = this.appContext.services.player;
     const networkService = this.appContext.services.network;
     const itemService = this.appContext.services.item;
     const targetingService = this.appContext.services.targeting;

     if (!playerService || !networkService || !targetingService) {
       logger.warn('Required services not available');
       return;
     }

     // Get player info and shortcut definition
     const playerInfo = playerService.getPlayerEntity().playerInfo;
     const shortcutDef = playerInfo.shortcuts?.[shortcutKey];

     if (!shortcutDef) {
       logger.debug('No shortcut definition', { shortcutKey });
       return;
     }

     // Get player state
     const playerPosition = playerService.getPosition();
     const cameraService = this.appContext.services.camera;
     const rotation = cameraService?.getRotation() || { x: 0, y: 0, z: 0 };
     const movementStatus = playerService.getMovementState();

     // --- NEW: Dual Targeting Resolution ---

     // 1. SERVER INTERACTION: Always use BOTH mode (entity OR block required)
     const interactionTarget = targetingService.resolveTarget('BOTH');
     const shouldSendInteraction = targetingService.shouldSendInteraction('BOTH', interactionTarget);

     // 2. VISUAL EFFECTS: Use item's actionTargeting mode
     let visualTargetMode: TargetingMode = 'ALL'; // Default
     if (shortcutDef.itemId && itemService) {
       const item = await itemService.getItem(shortcutDef.itemId);
       if (item) {
         const mergedModifier = await itemService.getMergedModifier(item);
         visualTargetMode = mergedModifier?.actionTargeting ?? 'ALL';
       }
     }
     const visualTarget = targetingService.resolveTarget(visualTargetMode);

     logger.info('Shortcut targeting resolved', {
       shortcutNr,
       interactionMode: 'BOTH',
       interactionTarget: interactionTarget.type,
       willSendInteraction: shouldSendInteraction,
       visualMode: visualTargetMode,
       visualTarget: visualTarget.type,
     });

     // Build interaction params
     const params: any = {
       shortcutNr,
       playerPosition: { x: playerPosition.x, y: playerPosition.y, z: playerPosition.z },
       playerRotation: { yaw: rotation.y, pitch: rotation.x },
       selectionRadius: 5,
       movementStatus,
       shortcutType: shortcutDef.type,
       shortcutItemId: shortcutDef.itemId,
     };

     // Add distance and target position if interaction target exists
     if (interactionTarget.type !== 'none') {
       const distance = Math.sqrt(
         Math.pow(interactionTarget.position.x - playerPosition.x, 2) +
         Math.pow(interactionTarget.position.y - playerPosition.y, 2) +
         Math.pow(interactionTarget.position.z - playerPosition.z, 2)
       );
       params.distance = parseFloat(distance.toFixed(2));
       params.targetPosition = {
         x: interactionTarget.position.x,
         y: interactionTarget.position.y,
         z: interactionTarget.position.z,
       };
     }

     // Send interaction to server (only if target matches BOTH mode)
     if (shouldSendInteraction) {
       if (interactionTarget.type === 'entity') {
         networkService.sendEntityInteraction(
           interactionTarget.entity.id,
           'fireShortcut',
           undefined,
           params
         );
       } else if (interactionTarget.type === 'block') {
         const pos = interactionTarget.block.block.position;
         networkService.sendBlockInteraction(
           pos.x,
           pos.y,
           pos.z,
           'fireShortcut',
           params,
           interactionTarget.block.block.metadata?.id,
           interactionTarget.block.block.metadata?.groupId
         );
       }
     } else {
       logger.debug('Skipping server interaction (no valid target for BOTH mode)');
     }

     // Emit PlayerService event with VISUAL target (always fires)
     const legacyVisualTarget = targetingService.toLegacyTarget(visualTarget);
     playerService.emitShortcutActivated(
       shortcutKey,
       shortcutDef.itemId,
       legacyVisualTarget.target,
       legacyVisualTarget.targetPosition
     );

     logger.debug('Shortcut fired', {
       shortcutNr,
       shortcutKey,
       sentInteraction: shouldSendInteraction,
       hasVisualTarget: visualTarget.type !== 'none',
     });
   } catch (error) {
     logger.error('Failed to fire shortcut', { shortcutNr, shortcutKey }, error as Error);
   }
 }

 3. Modify sendActiveShortcutUpdatesToServer() (lines 413-477):

 private sendActiveShortcutUpdatesToServer(): void {
   if (this.activeShortcuts.size === 0) {
     return;
   }

   const networkService = this.appContext.services.network;
   const scrawlService = this.appContext.services.scrawl;
   const targetingService = this.appContext.services.targeting;

   if (!networkService || !scrawlService || !targetingService) {
     logger.warn('Required services not available for server updates');
     return;
   }

   let sentCount = 0;
   let skippedCount = 0;

   for (const shortcut of this.activeShortcuts.values()) {
     // Get effectId for this executor
     const effectId = scrawlService.getEffectIdForExecutor(shortcut.executorId);
     if (!effectId) {
       skippedCount++;
       continue;
     }

     // Resolve current target (use item's actionTargeting mode if available)
     // For now, use ALL mode for continuous updates
     const currentTarget = targetingService.resolveTarget('ALL');

     if (currentTarget.type === 'none') {
       logger.info('Skipping shortcut update - no target position', {
         shortcutNr: shortcut.shortcutNr,
       });
       skippedCount++;
       continue;
     }

     try {
       // Send parameter update with targeting context
       networkService.sendEffectParameterUpdate(
         effectId,
         'targetPos',
         {
           x: currentTarget.position.x,
           y: currentTarget.position.y,
           z: currentTarget.position.z,
         },
         {
           mode: 'ALL',
           target: currentTarget,
         }
       );

       logger.info('Shortcut position update sent to server', {
         shortcutNr: shortcut.shortcutNr,
         effectId,
         targetType: currentTarget.type,
       });
       sentCount++;
     } catch (error) {
       logger.warn('Failed to send shortcut update to server', {
         error: (error as Error).message,
         shortcutNr: shortcut.shortcutNr,
       });
     }
   }

   if (sentCount > 0 || skippedCount > 0) {
     logger.info('Server update batch complete', { sentCount, skippedCount });
   }
 }

 ---
 Phase 4: NetworkService Extension

 File: packages/engine/src/services/NetworkService.ts

 Modify sendEffectParameterUpdate() (lines 799-832):

 import type { TargetingContext } from '@nimbus/shared';

 sendEffectParameterUpdate(
   effectId: string,
   paramName: string,
   value: any,
   targeting?: TargetingContext
 ): void {
   if (!this.socket || !this.isConnected) {
     logger.warn('Cannot send effect parameter update: not connected');
     return;
   }

   // Serialize Vector3 to plain object
   let serializedValue = value;
   if (value && typeof value === 'object' && 'x' in value && 'y' in value && 'z' in value) {
     serializedValue = { x: value.x, y: value.y, z: value.z };
   }

   const data: EffectParameterUpdateData = {
     effectId,
     paramName,
     value: serializedValue,
     targeting, // NEW: Include targeting context
   };

   this.socket.emit(MessageType.EFFECT_PARAMETER_UPDATE, data);

   logger.debug('Effect parameter update sent', {
     effectId,
     paramName,
     hasTargeting: !!targeting,
     targetingMode: targeting?.mode,
   });
 }

 ---
 Phase 5: ScrawlExecutor Extension

 File: packages/engine/src/scrawl/ScrawlExecutor.ts

 Modify updateParameter() (lines 779-829):

 updateParameter(paramName: string, value: any, targeting?: TargetingContext): void {
   const ctx = this.context;

   logger.info('Parameter update received', {
     executorId: this.id,
     paramName,
     hasValue: value !== undefined,
     hasTargeting: !!targeting,
     targetingMode: targeting?.mode,
   });

   // Special handling for targetPos with targeting context
   if (paramName === 'targetPos') {
     if (targeting) {
       // Use targeting context to reconstruct full target
       ctx.vars = ctx.vars || {};

       switch (targeting.target.type) {
         case 'entity':
           ctx.vars.target = {
             ...targeting.target.entity,
             position: value,
             currentPosition: value,
           };
           break;
         case 'block':
           ctx.vars.target = {
             ...targeting.target.block,
             position: value,
           };
           break;
         case 'ground':
           ctx.vars.target = {
             position: value,
             currentPosition: value,
           };
           break;
         case 'none':
           ctx.vars.target = undefined;
           break;
       }
     } else {
       // Legacy: just position
       ctx.vars = ctx.vars || {};
       ctx.vars.target = {
         currentPosition: value,
         position: value,
       };
     }
   } else if (paramName === '__stop__') {
     this.emit('stop_event');
     return; // Don't notify effects for stop event
   } else {
     // Generic parameter
     ctx.vars = ctx.vars || {};
     ctx.vars[paramName] = value;
   }

   // Notify all running effects
   for (const [effectId, effect] of this.runningEffects) {
     if (effect.onParameterChanged) {
       try {
         effect.onParameterChanged(paramName, value, ctx);
       } catch (error) {
         logger.warn('Effect parameter change handler failed', {
           effectId,
           paramName,
           error: (error as Error).message,
         });
       }
     }
   }
 }

 ---
 Phase 6: AppContext Registration

 File: packages/engine/src/NimbusClient.ts or packages/engine/src/AppContext.ts

 import { TargetingService } from './services/TargetingService';

 // In initialization:
 const targetingService = new TargetingService(appContext);
 appContext.services.targeting = targetingService;

 File: packages/engine/src/AppContext.ts

 export interface Services {
   // ... existing services ...
   targeting?: TargetingService;
 }

 ---
 Testing Strategy

 Test Case 1: ENTITY Targeting

 Item:
 {
   "texture": "items/heal.png",
   "actionTargeting": "ENTITY",
   "pose": "cast",
   "onUseEffect": { "kind": "Play", "effectId": "heal_beam" }
 }

 Expected Behavior:
 - Server interaction: Only sent when entity selected
 - Visual effects: Only execute when entity targeted
 - Pose: Only activates with entity target
 - ef.p.u: Only sends when entity is tracked

 Test Case 2: GROUND Targeting

 Item:
 {
   "texture": "items/fireball.png",
   "actionTargeting": "GROUND",
   "pose": "cast",
   "onUseEffect": { "kind": "Play", "effectId": "fireball" }
 }

 Expected Behavior:
 - Server interaction: NEVER sent (no entity/block requirement)
 - Visual effects: ALWAYS execute at ground position
 - Pose: ALWAYS activates
 - ef.p.u: Sends ground position continuously

 Test Case 3: ALL (Default)

 Item:
 {
   "texture": "items/wand.png",
   "pose": "use"
 }

 Expected Behavior:
 - Backwards compatible with current behavior
 - Works with entity, block, or ground
 - Server interaction uses BOTH mode
 - Visual effects use ALL mode

 ---
 Critical Files Summary

 Type Definitions (New/Modified)

 1. packages/shared/src/types/ItemModifier.ts - Add actionTargeting field
 2. packages/shared/src/types/TargetingTypes.ts - NEW: Target resolution types
 3. packages/shared/src/network/messages/EffectParameterUpdateMessage.ts - Extend with targeting field

 Core Services (New/Modified)

 4. packages/engine/src/services/TargetingService.ts - NEW: Central targeting logic
 5. packages/engine/src/services/ShortcutService.ts - Dual targeting resolution
 6. packages/engine/src/services/NetworkService.ts - Extended sendEffectParameterUpdate()
 7. packages/engine/src/scrawl/ScrawlExecutor.ts - Handle targeting context in updateParameter()

 Integration (Modified)

 8. packages/engine/src/NimbusClient.ts - Register TargetingService
 9. packages/engine/src/AppContext.ts - Add targeting to Services interface

 Server-Side (Modified)

 10. packages/test_server/src/NimbusServer.ts - Handle extended ef.p.u messages (pass through)

 ---
 Implementation Order

 1. Type definitions - Enable TypeScript support
 2. TargetingService - Core targeting logic
 3. ShortcutService integration - Dual targeting resolution
 4. NetworkService extension - ef.p.u with targeting
 5. ScrawlExecutor extension - Handle targeting context
 6. AppContext registration - Wire up service
 7. Testing - Verify all targeting modes

 ---
 Benefits of This Design

 1. Clean Separation: Visual vs Interaction targeting are explicit
 2. Extensible: Easy to add new targeting modes (RADIUS, CONE, etc.)
 3. Type-Safe: Discriminated unions catch errors at compile-time
 4. Testable: TargetingService can be unit tested independently
 5. Multiplayer: Full targeting context in ef.p.u enables proper remote tracking
 6. Backwards Compatible: Default 'ALL' mode preserves current behavior

 ---
 Migration Notes

 - Existing items without actionTargeting default to 'ALL'
 - Server interactions always use 'BOTH' mode (entity OR block required)
 - Visual effects use item's actionTargeting mode
 - No changes needed for existing item configurations
```

[x] Phase 1: Type Definitions
[x] Phase 2: TargetingService Implementation
[x] Phase 3: ShortcutService Integration
[x] Phase 4: NetworkService Extension
[x] Phase 5: ScrawlExecutor Extension
[ ] Phase 6: AppContext Registration

[ ] Visuell umgesetzt
[ ] Events werden richtig gefeuert
[ ] Effekte werden remote richtig abgespielt


[x] Ein aktueller Fall der aufgetreten ist:
"Der ClickHandler ist NICHT in der this.handlers Liste! Das ist das Problem - er wird nicht bei InputService.update() 
durchlaufen. Lass mich prüfen, ob er manuell aufgerufen werden muss oder zur Liste hinzugefügt werden sollte:"
- Kann man nicht click und key handler events zusammen fuehren im InputService, der dann das handled, ggf. wenn wir 
spaeter auch andere handler nutzt, z.b. ein virtueller joystick auf dem bildschirm.

===
# Examples tum testen

```text
doScrawlStart({
    id: "aoe_at_position",
    root: {
      kind: "Sequence",
      steps: [
        {
          kind: "Play",
          effectId: "circleMarker",
          ctx: {
            position: { x: -1, y: 70, z: 18 },
            radius: 5,
            color: "#ff6600",
            spreadDuration: 0.5,
            stayDuration: 2.0,
            rotationSpeed: 2.0,
            alpha: 0.9,
             shape: "circle",
            texture: "textures/block/basic/red_mushroom.png"
          }
        },
        {
          kind: "Wait",
          seconds: 2.5
        },
        {
          kind: "Cmd",
          cmd: "notification",
          parameters: [20, "null", "BOOM!"]
        }
      ]
    }
  })


 doScrawlStart({
    id: "fireball",
    root: {
      kind: "Play",
      effectId: "projectile",
      ctx: {
        startPosition: { x: -1, y: 70, z: 18 },
        targetPosition: { x: -1, y: 75, z: 23 },
        projectileWidth: 0.8,
        projectileHeadWidth: 0.2,
        projectileRadius: 0.3,
        projectileTexture: "textures/block/basic/redstone_ore.png",
        speed: 15,
        shape: "bullet",
        rotationSpeed: 3,
        color: "#ff6600"
      }
    }
  })



doScrawlStart({
    "root": {
      "kind": "Play",
      "effectId": "particleBeam",
      "ctx": {
        "startPosition": {"x": -1, "y": 70, "z": 18},
        "endPosition": {"x": -1, "y": 75, "z": 23},
        "color1": "#ff0000",
        "color2": "#00ff00",
        "color3": "#0000ff",
        "duration": 2.0,
        "alpha": 1,
        "thickness": 0.3
      }
    }
  })
  
doScrawlSelectedAction({
    script: {
      id: "mark_target",
      root: {
        kind: "Play",
        effectId: "circleMarker",
        ctx: {
          radius: 2.0,
          color: "#ff0000",
          spreadDuration: 0.3,
          stayDuration: 1.0,
          fadeDuration: 0.5
        }
      }
    }
  })

  
```
