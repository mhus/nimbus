/**
 * Nimbus constants
 * Central place for all magic numbers and configuration values
 */

/**
 * Chunk configuration constants
 */
export const ChunkConstants = {
  /** Default chunk size (blocks per side) */
  SIZE_DEFAULT: 16,

  /** Supported chunk sizes (must be power of 2) */
  SUPPORTED_SIZES: [8, 16, 32, 64] as const,

  /** Maximum chunk size */
  SIZE_MAX: 128,

  /** Minimum chunk size */
  SIZE_MIN: 8,
} as const;

/**
 * World configuration constants
 */
export const WorldConstants = {
  /** Default world height (Y-axis) */
  HEIGHT_DEFAULT: 256,

  /** Maximum world height */
  HEIGHT_MAX: 512,

  /** Minimum world height */
  HEIGHT_MIN: 64,

  /** Default sea level */
  SEA_LEVEL_DEFAULT: 62,

  /** Default ground level */
  GROUND_LEVEL_DEFAULT: 64,
} as const;

/**
 * Block constants
 */
export const BlockConstants = {
  /** Air block ID (empty/no block) */
  AIR_BLOCK_ID: 0,

  /** Maximum block type ID (Uint16 max) */
  MAX_BLOCK_TYPE_ID: 65535,

  /** Minimum block type ID */
  MIN_BLOCK_TYPE_ID: 0,

  /** Default block status */
  DEFAULT_STATUS: 0,

  /** Maximum status value (1 byte) */
  MAX_STATUS: 255,

  /** Maximum offset value (signed byte) */
  MAX_OFFSET: 127,

  /** Minimum offset value (signed byte) */
  MIN_OFFSET: -127,

  /** Maximum face visibility value (7 bits) */
  MAX_FACE_VISIBILITY: 127,
} as const;

/**
 * Network constants
 */
export const NetworkConstants = {
  /** Ping timeout buffer (milliseconds) */
  PING_TIMEOUT_BUFFER_MS: 10000,

  /** Default ping interval (seconds) */
  PING_INTERVAL_DEFAULT: 30,

  /** Maximum message size (10 MB) */
  MAX_MESSAGE_SIZE: 10 * 1024 * 1024,

  /** WebSocket reconnect delay (milliseconds) */
  RECONNECT_DELAY_MS: 5000,

  /** Maximum reconnect attempts */
  MAX_RECONNECT_ATTEMPTS: 5,

  /** Message ID max length */
  MESSAGE_ID_MAX_LENGTH: 100,
} as const;

/**
 * Entity constants
 */
export const EntityConstants = {
  /** Default player health */
  PLAYER_HEALTH_DEFAULT: 20,

  /** Default player max health */
  PLAYER_MAX_HEALTH_DEFAULT: 20,

  /** Default player walk speed (blocks/second) */
  PLAYER_WALK_SPEED: 4.3,

  /** Default player sprint speed (blocks/second) */
  PLAYER_SPRINT_SPEED: 5.6,

  /** Default player crouch speed (blocks/second) */
  PLAYER_CROUCH_SPEED: 1.3,

  /** Default player jump height (blocks) */
  PLAYER_JUMP_HEIGHT: 1.25,

  /** Entity ID max length */
  ENTITY_ID_MAX_LENGTH: 100,

  /** Display name max length */
  DISPLAY_NAME_MAX_LENGTH: 100,

  /** Username max length */
  USERNAME_MAX_LENGTH: 50,
} as const;

/**
 * Rendering constants
 */
export const RenderConstants = {
  /** Default render distance (chunks) */
  RENDER_DISTANCE_DEFAULT: 8,

  /** Maximum render distance (chunks) */
  RENDER_DISTANCE_MAX: 32,

  /** Default target FPS */
  TARGET_FPS: 60,

  /** Maximum chunks rendered per frame */
  MAX_CHUNKS_PER_FRAME: 3,

  /** LOD distance thresholds (as fraction of max distance) */
  LOD_THRESHOLDS: [0.25, 0.5, 0.75] as const,

  /** Maximum vertices per chunk mesh */
  MAX_VERTICES_PER_CHUNK: 100000,
} as const;

/**
 * Animation constants
 */
export const AnimationConstants = {
  /** Maximum animation duration (milliseconds) */
  MAX_DURATION: 60000,

  /** Maximum effects per animation */
  MAX_EFFECTS: 50,

  /** Maximum placeholder count */
  MAX_PLACEHOLDERS: 10,

  /** Default easing type */
  DEFAULT_EASING: 'easeInOut' as const,
} as const;

/**
 * Physics constants
 */
export const PhysicsConstants = {
  /** Gravity (blocks per second squared) */
  GRAVITY: 20,

  /** Terminal velocity (blocks per second) */
  TERMINAL_VELOCITY: 50,

  /** Air drag coefficient */
  AIR_DRAG: 0.98,

  /** Ground friction */
  GROUND_FRICTION: 0.6,

  /** Water drag */
  WATER_DRAG: 0.8,
} as const;

/**
 * Camera constants
 */
export const CameraConstants = {
  /** Default field of view (degrees) */
  FOV_DEFAULT: 70,

  /** Minimum FOV */
  FOV_MIN: 30,

  /** Maximum FOV */
  FOV_MAX: 110,

  /** Default mouse sensitivity */
  SENSITIVITY_DEFAULT: 0.1,

  /** Maximum pitch angle (degrees) */
  PITCH_MAX: 89,

  /** Minimum pitch angle (degrees) */
  PITCH_MIN: -89,

  /** Near clipping plane */
  NEAR_PLANE: 0.1,

  /** Far clipping plane */
  FAR_PLANE: 1000,
} as const;

/**
 * Collection size limits
 */
export const LimitConstants = {
  /** Maximum blocks in update message */
  MAX_BLOCKS_PER_MESSAGE: 10000,

  /** Maximum entities in update message */
  MAX_ENTITIES_PER_MESSAGE: 1000,

  /** Maximum chunk coordinates in registration */
  MAX_CHUNK_COORDINATES: 1000,

  /** Maximum notification queue size */
  MAX_NOTIFICATION_QUEUE: 100,

  /** Maximum animation queue size */
  MAX_ANIMATION_QUEUE: 50,
} as const;

/**
 * All constants combined
 */
export const Constants = {
  Chunk: ChunkConstants,
  World: WorldConstants,
  Block: BlockConstants,
  Network: NetworkConstants,
  Entity: EntityConstants,
  Render: RenderConstants,
  Animation: AnimationConstants,
  Physics: PhysicsConstants,
  Camera: CameraConstants,
  Limits: LimitConstants,
} as const;
