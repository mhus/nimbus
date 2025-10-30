# Block Model

## Koordinaten-Namenskonvention

**Wichtig:** Koordinaten müssen konsistent benannt werden:
- **x, y, z** = Welt-Koordinaten (world coordinates)
- **cx, cy, cz** = Chunk-Koordinaten (chunk coordinates)
- **localX, localY, localZ** = Lokale Koordinaten innerhalb eines Chunks (vermeiden wenn möglich)

## Block-Type

Parameters:
- id
- ?initialStatus : int (default = 0)
- modifiers : Record<number, BlockModifier> (status → BlockModifier map)

Status ist ein Integer und definiert einen status. Jeder Status hat einen eigenen BlockModifier.

### Status

0 = default status
1 = open
2 = closed
3 = gesperrt
5 = zerstört

10 = winter
11 = winter/frühling (alternativ 10)
12 = frühling
13 = frühling/sommer (alternativ 12)
14 = sommer
15 = sommer/herbst (alternativ 14)
16 = herbst
17 = herbst/winter (alternativ 16)

Ab 100: Custom Status der Welt

z.b.

666 = Die Apokalypse ist eingetreten


## Block

Parameters:
- position : Vector3 (x, y, z - world coordinates)
- blockTypeId : number
- ?offsets : number[] (flexible array, shape-dependent, trailing zeros can be omitted for network optimization)
  - For cubes: 8 corners × 3 axes = up to 24 values (supports float values)
  - For other shapes: shape-specific offset data
- ?faceVisibility : number (1 byte bitfield, 6 bits for faces + 1 bit for fixed/auto mode)
  - Bit 0-5: TOP, BOTTOM, LEFT, RIGHT, FRONT, BACK
  - Bit 6: FIXED mode (1) or AUTO mode (0)
- ?status : number (current status, references modifier in BlockType.modifiers or metadata.modifiers)
- ?metadata : BlockMetadata

**Wichtig:** Block wird über Netzwerk übertragen und auf Server verwendet.
Keine cached Werte (wie blockType reference) - diese gehören in ClientBlock!

Blöcke haben möglichst wenige Parameter, deshalb werden Standard-Situationen
durch BlockTypen definiert. Diese haben die gleichen Modifier wie ein Block zusätzlich haben kann.
Zusätzlich kann ein Block auch Metadaten haben, die immer block-spezifisch sind.

### Metadaten mergen

world status ist by default = 0
Welt Status: Wenn die Welt einen Status-Switch fordert und der Status verfügbar ist, z.b. Switche alle 0 auf 666 (Apokalypse)

Metadaten werden von oben nach unten durch gemerged (first match winns)

1. Block-BlockType-status-Metadaten (Insance status)
2. Block-BlockType-ID status-Metadaten (Instance status=world status)
3. Block-BlockType-ID status-Metadaten (Base status)
4. Block-BlockType-ID status-Metadaten (Base status=world status)
5. Default Werte für Metadaten, z.b. shape=0

## BlockModifier

Parameters:
- visibility
  - ?shape : string - z.B. cube, cross, model, flat, sphere, column, round_cube, steps, stair, billboard, sprite, flame, ocean, river, water, lava, fog
  - ?effect - z.B. water, wind, flipbox, lava, fog (shader-effekte)
  - ?effectParameters (Map mit effekt-spezifischen werten)
  - ?offsets (array of 8 x XYZ offsets for each corner at cubes or other offsets, supports float values)
  - ?scalingX
  - ?scalingY
  - ?scalingZ
  - ?rotationX
  - ?rotationY
  - ?path (string - path to model file, for shape=model)
  - textures (map of texture paths with UV mapping info ':x,y,w,h'), key ist ein integer
    - path
    - uvMapping (x,y,w,h)
    - ?rotation (für jede texture(6): 0,90,180,270, flip 0,90,180,270; byte 0=0, 1=90, 2=180, 3=270, + 4 für flip -> 4,5,6,7  )
    - ?samplingMode (nearest, linear, mipmap... ; byte/enum)
    - ?transparencyMode (none, hasAlpha, getAlphaFromRGB; byte/enum)
    - ?color
- ?wind
  - leafiness
  - stability
  - leverUp
  - leverDown
- ?spriteCount
- ?alpha (?)
- ?illumination (?)
  - color
  - strength
- ?physics
  - ?solid (true/false) - Collision
  - ?resistance (laufwiederstand)
  - ?climbable (int fuer kletter-wiederstand?)
  - ?autoMoveXYZ (?) - Bewegt dich automatisch nach, mit geschwindigkeit X wenn du darauf stehst
  - ?interactive
  - ?gateFromDirection (byte, z.B. north,south,east,west,up,down) - Ermöglicht das durchgehen von einer Seite
- ?effects
  - ?forceEgoView
  - ?sky
    - intensity
    - color
    - wind ... weater?
- ?sound
  - ?walk
  - ?walkVolume
  - ?permanent
  - ?permanentVolume
  - ?changeStatus - Wenn der status wechselt? oder als Effekt? TODO
  - ?changeStatusVolume

Alle Parameter und sub trukturen sind Optional um die Menge an übertragenen und gespeicherten Daten
zu minimieren. Die Parameter-Namen werden ggf. gekürzt oder durch Zahlen ersetzt,
um die Datenmenge zu reduzieren.

Regel für das Rendern: Offsets → Scale → Rotation
??? TODO Oder  Scale → Rotation -> Offsets

### Texture Keys

0 = alle seiten
1 = top
2 = bottom
3 = left
4 = right
5 = front
6 = back
7 = side
8 = diffuse
9 = distortion
10 = opacity

Ab 100: ggf. für bestimmte shape typen spezielle texturen.

## BlockMetadata

Parameters:
- ?groupId : number
- ?modifiers : Record<number, BlockModifier> (optional instance-specific modifier overrides)

Metadata können eigene Modifier enthalten, die die BlockType Modifier für diese spezifische Block-Instanz überschreiben.
Use case: Eine spezielle Tür, die anders aussieht als der Standard-Türtyp.

## Shape

Parameters:
- invisible (0 - nicht sichtbar, kann aber Modifier haben, 'echter' Block, der renderer macht aber nichts, z.b. effects)
- cube (1 - normaler Würfel)
- cross (2 - Kreuzpflanze)
- hash (3 - Hash-Pflanze)
- model
- glass (a cube of glass)
- glass_flat (a flat glass)
- flat
- sphere
- column
- round_cube
- steps
- stair
- billboard
- sprite
- flame
- ocean (water of ocean, flat)
- ocean_coast
- ocean_mahlstrom
- river (water of river, flat, needs a direction)
- river_waterfall
- river_waterfall_whirlpool
- water (a cube of water)
- lava
- fog

# Server Block Model

Parameters:
- block
- chunk : Chunk
- ...?

# Client-Block-Type

Wichtige Eigenschaften als feste werte aus Block-Type raus holen, 
fuer schnellen Zugriff. Werte werden aus Block-Type, Block und status
dynamisch zusammengebaut. Beic ändern des Status wird aktualisiert.
Keine optional Werte, wenn nicht explizit sinnvoll.

Parameters:
- type : Block-Type (0 - AIR)
- attributes : Map
- shape
- assets : array of string
- assetTextures : array of textures
- offsets
- scalingXYZ
- rotationXY
- facing
- color
...


# Client Block Model

ClientBlock ist die client-seitige Repräsentation mit aufgelösten Referenzen und Caches.
Wird NICHT über Netzwerk übertragen!

Parameters:
- block : Block (original network data)
- chunk : { cx: number, cz: number } (chunk coordinates)
- blockType : BlockType (cached, resolved from block.blockTypeId)
- ?metadata : BlockMetadata (cached)
- currentModifier : BlockModifier (cached, resolved from status)
- clientBlockType : ClientBlockType (optimized for rendering)
- ?statusName : string (debug string, e.g., "OPEN", "WINTER")
- ?isVisible : boolean (culling flag)
- ?lastUpdate : number (timestamp)
- ?isDirty : boolean (needs re-render)

## AnimationData

Timeline-basiertes Animation-System. Animationen sind Abfolgen von Effekten, die parallel
oder sequenziell ausgeführt werden. Effekte können positions-basiert sein, mit Support für
mehrere Positionen (z.B. Projektil von A nach B).

**Use Cases:**
1. Server-definiert: Server sendet komplette Animation mit festen Positionen
2. Client-definiert: Client hat Template, füllt Positionen aus, sendet zurück an Server für Broadcast

**Beispiel:** Pfeilschuss von Spieler auf NPC:
- Effekt 1: Projektil fliegt von A nach B (parallel, 0-1000ms)
- Effekt 2: Himmel wird dunkel (parallel, 0-500ms)
- Effekt 3: Explosion bei B (sequential, 1000-1300ms)
- Effekt 4: Himmel wird hell (sequential, 1300-2000ms)

```json
{
  "id": "arrow_shot_123",
  "name": "arrow_shot",
  "duration": 2000,
  "placeholders": ["shooter", "target", "impact"],  // To be filled by client
  "effects": [
    {
      "id": "projectile",
      "type": "projectile",
      "positions": [
        {"type": "placeholder", "name": "shooter"},
        {"type": "placeholder", "name": "target"}
      ],
      "params": {
        "projectileModel": "/models/arrow.babylon",
        "speed": 50,
        "trajectory": "arc"
      },
      "startTime": 0,
      "duration": 1000,
      "blocking": true
    },
    {
      "id": "sky_darken",
      "type": "skyChange",
      "positions": [],
      "params": {
        "color": "#333333",
        "lightIntensity": 0.3,
        "easing": "easeIn"
      },
      "startTime": 0,
      "duration": 500
    },
    {
      "id": "explosion",
      "type": "explosion",
      "positions": [
        {"type": "fixed", "position": {"x": 10, "y": 65, "z": 10}}
      ],
      "params": {
        "radius": 5,
        "explosionIntensity": 1.0
      },
      "startTime": 1000,
      "duration": 300
    },
    {
      "id": "sky_brighten",
      "type": "skyChange",
      "positions": [],
      "params": {
        "color": "#87CEEB",
        "lightIntensity": 1.0,
        "easing": "easeOut"
      },
      "startTime": 1300,
      "duration": 700
    }
  ],
  "source": {
    "type": "client",
    "playerId": "player123"
  }
}
```

### Effect Types
- Transform: scale, rotate, translate
- Visual: colorChange, fade, flash
- Particle/Object: projectile, explosion, particles, spawnEntity
- Environment: skyChange, lightChange, cameraShake
- Sound: playSound
- Block: blockBreak, blockPlace, blockChange

### Position References
- Fixed: `{"type": "fixed", "position": {"x": 10, "y": 64, "z": 5}}`
- Placeholder: `{"type": "placeholder", "name": "player"}` (filled by client)

### Timeline
- **Parallel**: Effects mit gleicher startTime laufen gleichzeitig
- **Sequential**: blocking=true wartet auf Abschluss vor nächstem Effekt
- **Duration**: Wird aus effects berechnet wenn nicht gesetzt


## BlockData

Siehe Block

TODO?

## AreaData

```json
{
  "a": {"x": 0, "y": 0, "z": 0}, // start
  "b": {"x": 15, "y": 255, "z": 15}, // stop
  "e": [ // effects
    EffectData,
    ...
  ]
}
```

## EffectData

```json
{
  "n": "rain", // name
  "p": { // parameters
    "intensity": 0.5,
    "color": "#aabbff"
  }
}
```

## EntityData

TODO

- id
- type: string (npc, player)
- visibility
  - modelPath

- position
- rotation
- walkToPosition