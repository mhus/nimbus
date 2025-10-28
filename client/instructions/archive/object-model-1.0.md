# Models

## Block Model

BlockShape: CUBE, CROSS, HASH, MODEL, GLASS, FLAT, SPHERE, COLUMN, ROUND_CUBE
BlockMaterial 'solid', 'water', 'lava', 'barrier', 'gas'
ToolType - Weg!
BlockOptions: 
- solid?: boolean // Gibt an, ob der Block solide ist (Kollision)
- opaque?: boolean // Gibt an, ob der Block lichtundurchlässig ist (blockiert Licht, cullt Flächen)
- transparent?: boolean // Gibt an, ob der Block transparent ist (wie Glas)
- material?: BlockMaterial // Blockmaterialtyp (z.B. 'solid', 'water', ...)
- fluid?: boolean // Gibt an, ob der Block eine Flüssigkeit ist
- fluidDensity?: number // Dichte der Flüssigkeit (für Physik)
- viscosity?: number // Viskosität der Flüssigkeit (für Physik)
- [key: string]: any // Benutzerdefinierte Eigenschaften
BlockModifier:
- shape?: BlockShape // Überschreibt die Blockform
- texture?: string | string[] // Überschreibt die Textur
- options?: BlockOptions // Überschreibt Blockoptionen
- hardness?: number // Überschreibt die Härte
- miningtime?: number // Überschreibt die Abbauzeit
- tool?: ToolType // Überschreibt das benötigte Werkzeug
- unbreakable?: boolean // Überschreibt das Unzerstörbar-Flag
- solid?: boolean // Überschreibt das Solid-Flag
- transparent?: boolean // Überschreibt das Transparent-Flag
- rotationY?: number // Rotation um die Y-Achse (0-360°), horizontal
- rotationX?: number // Rotation um die X-Achse (0-360°), vertikal
- facing?: number // Blickrichtung (0-5 für 6 Richtungen)
- rotation?: number; // (veraltet) Nutze stattdessen rotationX und rotationY
- color?: [number, number, number]; // Farb-Tint [R, G, B] (0-255)
- scale?: [number, number, number]; // Skalierung [x, y, z]
- windLeafiness?: number; // Wind-Laubigkeit (0-1): 1 für Blätter, 0 für solide Blöcke
- windStability?: number; // Windstabilität (0-1): Widerstand gegen Wind
- windLeverUp?: number; // Amplitude für Bewegung der oberen Vertices
- windLeverDown?: number; // Amplitude für Bewegung der unteren Vertices
- customProperties?: Record<string, any>; // Zusätzliche benutzerdefinierte Eigenschaften

BlockType:
- id?: number; // Numerische ID (vom Registry vergeben)
- name: string; // Eindeutiger Name (z.B. 'stone')
- displayName?: string; // Anzeigename (optional)
- shape: BlockShape; // Blockform/-modelltyp
- texture: string | string[]; // Textur(en) für den Block
- options?: BlockOptions; // Zusätzliche Blockoptionen
- hardness?: number; // Härte (beeinflusst Abbauzeit)
- miningtime?: number; // Basis-Abbauzeit in ms
- tool?: ToolType; // Benötigtes Werkzeug
- unbreakable?: boolean; // Unzerstörbar-Flag
- solid?: boolean; // Solide-Flag (Kurzform für options.solid)
- transparent?: boolean; // Transparent-Flag (Kurzform für options.transparent)
- windLeafiness?: number; // Wind-Laubigkeit (0-1)
- windStability?: number; // Windstabilität (0-1)

## Entity Model

EntityCategory:
- PLAYER = 'player' // Spieler-Entität
- PASSIVE = 'passive' // Passives Lebewesen (z.B. Tiere)
- HOSTILE = 'hostile' // Feindliches Lebewesen (Monster)
- NEUTRAL = 'neutral' // Neutrales Lebewesen (greift bei Provokation an)
- NPC = 'npc' // NPC (Dorfbewohner, Händler)
- PROJECTILE = 'projectile' // Projektil (Pfeil, Feuerball)
- ITEM = 'item' // Item-Entität (heruntergefallene Items)
- VEHICLE = 'vehicle' // Fahrzeug (Lore, Boot)
- OTHER = 'other' // Sonstiges/benutzerdefiniert

EntityAI:
- canMove?: boolean // Kann sich bewegen
- moveSpeed?: number // Bewegungsgeschwindigkeit
- canJump?: boolean // Kann springen
- jumpHeight?: number // Sprunghöhe
- canSwim?: boolean // Kann schwimmen
- canFly?: boolean // Kann fliegen
- wanders?: boolean // Streift umher
- aggroRange?: number // Aggro-Reichweite (für feindliche Mobs)
- followRange?: number // Folge-Reichweite (für passive Mobs)

EntityStats:
- maxHealth: number // Maximale Gesundheit
- attackDamage?: number // Angriffsschaden (für feindliche Mobs)
- defense?: number // Verteidigungs-/Rüstungswert
- knockbackResistance?: number // Rückstoßresistenz (0-1)

EntitySize:
- width: number // Breite (X- und Z-Achse)
- height: number // Höhe (Y-Achse)
- eyeHeight?: number // Augenhöhe (für Kamera/Sicht)

EntityType:
- id?: number // Numerische ID (vom Registry vergeben)
- name: string // Eindeutiger Name
- displayName?: string // Anzeigename
- category: EntityCategory // Entitätskategorie
- size: EntitySize // Größe/Hitbox
- stats: EntityStats // Werte (Gesundheit, Angriff, ...)
- ai?: EntityAI // KI-Verhalten
- model?: string // Modell-/Mesh-Name oder Pfad
- texture?: string // Texturpfad
- hasPhysics?: boolean // Hat Physik/Schwerkraft
- hasCollision?: boolean // Hat Kollision
- [key: string]: any // Benutzerdefinierte Eigenschaften
