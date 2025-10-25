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
- ?shape
- ?assets (string, z.B.texture)
- ?offsets
- ?scalingXYZ
- ?rotationXY
- ?facing (für die texture)
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
// behavior
- ?solid (true/false) - Collision
- ?sound
  - ?walk
  - ?walkVolume
  - ?permanent
  - ?permanentVolume
  - ?changeStatus - Wenn der status wechselt? oder als Effekt?
  - ?changeStatusVolume
- ?resistance (laufwiederstand)
- ?climbable (int fuer kletter-wiederstand?)
- ?autoMoveXYZ (?) - Bewegt dich automatisch nach, mit geschwindigkeit X wenn du darauf stehst
- ?interactive
- ?forceEgoView
- ?sky
  - intensity
  - color
  - wind ... weater?



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
