# Block Model

Block-Type
- id
- BlockModifiers
- ?status=BlockModifier

Block
- Position
- BlockType-ID
- offsets (edgeOffset, array of 8 byte)
- faceVisibility (1 byte, 6 bit + 1 bit for fixed or auto)
- ?BlockModifier
- ?status=BlockModifier
- ?BlockMetadata

BlockModifier
// visibility - Alles als Map hinterlegen, meist keine default werte
- vision
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
  - texture[0..x] (array of texture paths with UV mapping info ':x,y,w,h')
    - name (string: top, bottom, left, right, front, back, side, all, diffuse, distortion, opacity
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
- behavior
  - ?solid (true/false) - Collision
  - ?resistance (laufwiederstand)
  - ?climbable (int fuer kletter-wiederstand?)
  - ?autoMoveXYZ (?) - Bewegt dich automatisch nach, mit geschwindigkeit X wenn du darauf stehst
  - ?interactive
  - ?gateFromDirection (byte, z.B. north,south,east,west,up,down) - Ermöglicht das durchgehen von einer Seite
- effects
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
  - ?changeStatus - Wenn der status wechselt? oder als Effekt?
  - ?changeStatusVolume


BlockMetadata
- displayName
- name
- groupId

Shape
- air (0 - Block is invisible)
- cube
- cross
- hash
- model
- glass (a cube of glass)
- flat
- sphere
- column
- round_cube
- steps
- stair
- billboard
- sprite
- flame
- ocean (water of ocean)
- river (water of river)
- water (a cube of water)
- lava
- fog

# Server Block Model
- block
- chunk : Chunk
- ...?

# Client-Block-Type

Wichtige Eigenschaften als feste werte aus Block-Type raus holen, 
fuer schnellen Zugriff. Werte werden aus Block-Type, Block und status
dynamisch zusammengebaut. Beic ändern des Status wird aktualisiert.
Keine optional Werte, wenn nicht explizit sinnvoll.

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