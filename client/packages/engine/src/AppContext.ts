/**
 * Application Context
 * Central context containing all services, configuration, and state
 */

import type { ClientConfig } from './config/ClientConfig';
import type { ClientService } from './services/ClientService';
import type { NetworkService } from './services/NetworkService';
import type { ChunkService } from './services/ChunkService';
import type { BlockTypeService } from './services/BlockTypeService';
import type { ShaderService } from './services/ShaderService';
import type { SpriteService } from './services/SpriteService';
import type { ThinInstancesService } from './services/ThinInstancesService';
import type { EngineService } from './services/EngineService';
import type { SelectService } from './services/SelectService';
import type { ModalService } from './services/ModalService';
import type { NotificationService } from './services/NotificationService';
import type { CommandService } from './services/CommandService';
import type { CameraService } from './services/CameraService';
import type { EnvironmentService } from './services/EnvironmentService';
import type { PlayerService } from './services/PlayerService';
import type { PhysicsService } from './services/PhysicsService';
import type { CompassService } from './services/CompassService';
import type { ModifierService } from './services/ModifierService';
import type { EntityService } from './services/EntityService';
import type { WorldInfo, PlayerInfo } from '@nimbus/shared';

/**
 * Server information (received after login)
 */
export interface ServerInfo {
  /** Server version */
  version?: string;

  /** Server name */
  name?: string;

  /** Additional server metadata */
  [key: string]: any;
}

/**
 * Service registry
 * Contains all singleton services used by the application
 */
export interface Services {
  /** Client service for platform detection and configuration */
  client: ClientService;

  /** Network service for WebSocket connection and message routing */
  network?: NetworkService;

  /** Chunk service for chunk management and loading */
  chunk?: ChunkService;

  /** Block type service for block type registry */
  blockType?: BlockTypeService;

  /** Shader service for shader effect management */
  shader?: ShaderService;

  /** Sprite service for sprite rendering and animation */
  sprite?: SpriteService;

  /** Thin instances service for high-performance grass/foliage rendering */
  thinInstances?: ThinInstancesService;

  /** Engine service for 3D rendering */
  engine?: EngineService;

  /** Select service for block selection in front of player */
  select?: SelectService;

  /** Modal service for displaying IFrame modals */
  modal?: ModalService;

  /** Notification service for displaying toast notifications */
  notification?: NotificationService;

  /** Command service for command registration and execution */
  command?: CommandService;

  /** Camera service for camera control and underwater effects */
  camera?: CameraService;

  /** Environment service for lighting, wind, and environmental effects */
  environment?: EnvironmentService;

  /** Player service for player state and movement */
  player?: PlayerService;

  /** Physics service for entity physics simulation */
  physics?: PhysicsService;

  /** Compass service for compass bar and directional markers */
  compass?: CompassService;

  /** Modifier service for managing modifier stacks */
  modifier?: ModifierService;

  /** Entity service for entity and entity model management */
  entity?: EntityService;

  // Future services will be added here:
  // render?: RenderService;
  // input?: InputService;
}

/**
 * Application Context
 * Central context passed to all services and components
 */
export interface AppContext {
  /** Service registry */
  services: Services;

  /** Client configuration from environment */
  config: ClientConfig;

  /** Server information (after login) */
  serverInfo: ServerInfo | null;

  /** Current world information (after world selection) */
  worldInfo: WorldInfo | null;

  /** Player information and properties (dynamically updatable) */
  playerInfo: PlayerInfo | null;

  /** Session ID (after login) */
  sessionId: string | null;
}

/**
 * Create initial application context
 * @param config Client configuration
 * @param clientService Client service instance
 * @returns Initial app context
 */
export function createAppContext(
  config: ClientConfig,
  clientService: ClientService
): AppContext {
  return {
    services: {
      client: clientService,
    },
    config,
    serverInfo: null,
    worldInfo: null,
    playerInfo: null, // Will be initialized later with DEFAULT_PLAYER_INFO
    sessionId: null,
  };
}
