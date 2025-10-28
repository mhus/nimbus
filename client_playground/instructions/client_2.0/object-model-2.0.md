# Block Model

## Block-Type

Parameters:
- id
- ?initialStatus : int (default = 0)
- status=BlockModifier

Status ist ein Integer und definiert einen status.

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
- Position
- BlockType-ID
- offsets (edgeOffset, array of 8 byte)
- faceVisibility (1 byte, 6 bit + 1 bit for fixed or auto)
- ?BlockType
- ?BlockMetadata

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
  - ?offsets (array of 8 x XYZ offsets for each corner at cubes or other offsets, max 24 bytes -127 - 127)
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
- displayName
- name
- groupId

## Shape

Parameters:
- air (0 - Block ist ein pseudoblock, keine Modifier möglich, wirklich nur für 'kein block')
- invisible (1 - nicht sichtbar, kann aber Modifier haben, 'echter' Block, der renderer macht aber nichts, z.b. effects)
- cube
- cross
- hash
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
- type : Block-Type
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

Parameters:
- block : Block
- chunkXZ
- originalBlockType : Block-Type
- customizedBlockType : Client-Block-Type
- status : String

## AnimationData

Eine Animation ist eine Abfolge von Effekten, die abgespielt werden. Der Ort wird
ausserhalb der Animation definiert. ggf wird kein Ort benötigt.

TODO: Noch nicht final definiert!

```json
{
  "name": "block_bounce", // Name der Animation
  "duration": 1000,      // Dauer in Millisekunden
  "effects": [           // Liste der Effekte
    {
      "type": "scale",   // Effekt-Typ (z.B. scale, rotate, translate, colorChange)
      "params": {        // Parameter für den Effekt
        "from": 1.0,
        "to": 1.2,
        "easing": "easeInOut"
      },
      "startTime": 0,    // Startzeit relativ zur Animation
      "endTime": 500     // Endzeit relativ zur Animation
    },
    {
      "type": "scale",
      "params": {
        "from": 1.2,
        "to": 1.0,
        "easing": "easeInOut"
      },
      "startTime": 500,
      "endTime": 1000
    }
  ]
}
```


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