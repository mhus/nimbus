/**
 * Network message types
 *
 * Shortened names to reduce network traffic
 */

export enum MessageType {
  // Authentication
  LOGIN = 'login',
  LOGIN_RESPONSE = 'loginResponse',
  LOGOUT = 'logout',

  // Connection
  PING = 'p',

  // World
  WORLD_STATUS_UPDATE = 'w.su',

  // Chunks
  CHUNK_REGISTER = 'c.r',
  CHUNK_QUERY = 'c.q',
  CHUNK_UPDATE = 'c.u',

  // Blocks
  BLOCK_UPDATE = 'b.u',
  BLOCK_CLIENT_UPDATE = 'b.cu',
  BLOCK_STATUS_UPDATE = 'b.s.u',

  // Entities
  ENTITY_UPDATE = 'e.u',

  // Animation
  ANIMATION_START = 'a.s',

  // User/Player
  USER_MOVEMENT = 'u.m',
  PLAYER_TELEPORT = 'p.t',

  // Interaction
  INTERACTION_REQUEST = 'int.r',
  INTERACTION_RESPONSE = 'int.rs',

  // NPC Dialog
  NPC_OPEN = 'npc.o',
  NPC_SELECT = 'npc.se',
  NPC_UPDATE = 'npc.u',
  NPC_CLOSE = 'npc.c',

  // Notifications
  NOTIFICATION = 'n',

  // Commands (Client -> Server)
  CMD = 'cmd',
  CMD_MESSAGE = 'cmd.msg',
  CMD_RESULT = 'cmd.rs',

  // Server Commands (Server -> Client)
  SCMD = 'scmd',
  SCMD_RESULT = 'scmd.rs',
}

/**
 * Client type identifier
 */
export enum ClientType {
  WEB = 'web',
  XBOX = 'xbox',
  MOBILE = 'mobile',
  DESKTOP = 'desktop',
}

/**
 * Notification type (numeric for network efficiency)
 */
export enum NotificationType {
  SYSTEM = 0,
  CHAT = 1,
  WARNING = 2,
  ERROR = 3,
  INFO = 4,
}

/**
 * Dialog option severity
 */
export enum DialogSeverity {
  INFO = 'info',
  WARNING = 'warning',
  DANGER = 'danger',
  NEUTRAL = 'neutral',
  SUCCESS = 'success',
}
