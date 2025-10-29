/**
 * Application Context
 * Central context containing all services, configuration, and state
 */

import type { ClientConfig } from './config/ClientConfig';
import type { ClientService } from './services/ClientService';
import type { TextureService } from './services/TextureService';
import type { NetworkService } from './services/NetworkService';
import type { ChunkService } from './services/ChunkService';
import type { WorldInfo } from '@nimbus/shared';

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

  /** Texture service for texture loading and caching */
  texture?: TextureService;

  /** Network service for WebSocket connection and message routing */
  network?: NetworkService;

  /** Chunk service for chunk management and loading */
  chunk?: ChunkService;

  // Future services will be added here:
  // engine?: EngineService;
  // render?: RenderService;
  // blockType?: BlockTypeService;
  // shader?: ShaderService;
  // input?: InputService;
  // camera?: CameraControl;
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
  };
}
