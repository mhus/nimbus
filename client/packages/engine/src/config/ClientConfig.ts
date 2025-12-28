/**
 * Client configuration
 * Loads configuration from runtime config and environment variables
 */

import { getLogger } from '@nimbus/shared';
import { runtimeConfigService } from './RuntimeConfig';

const logger = getLogger('ClientConfig');

/**
 * Client configuration interface
 */
export interface ClientConfig {
  /** WebSocket server URL (will be set after loading server config) */
  websocketUrl?: string;

  /**
   * REST API server URL
   *
   * @important This property should ONLY be used by NetworkService.
   * All other services must access API URLs through NetworkService methods:
   * - getAssetUrl(assetPath)
   * - getEntityModelUrl(entityTypeId)
   * - getBackdropUrl(backdropTypeId)
   * - getEntityUrl(entityId)
   * - getBlockTypesRangeUrl(from, to)
   * - getItemUrl(itemId)
   * - getItemDataUrl(itemId)
   *
   * This ensures centralized URL management for Governor compatibility.
   */
  apiUrl: string;

  /** World ID to connect to */
  worldId: string;

  /** Enable console logging */
  logToConsole: boolean;

  /**
   * Exit URL to redirect to when connection fails permanently
   * (will be set after loading server config)
   */
  exitUrl?: string;
}

/**
 * Load initial client configuration (before server config is loaded)
 * Only loads apiUrl and worldId - websocketUrl and exitUrl will come from server
 * @returns Initial client configuration
 * @throws Error if required parameters are missing
 */
export async function loadClientConfig(): Promise<ClientConfig> {
  logger.debug('Loading initial client configuration');

  // Load runtime config (from /config.json or fallback to .env)
  const runtimeConfig = await runtimeConfigService.loadConfig();

  // Get environment based on build tool (for optional settings)
  const env = getEnvironment();

  // Get URL parameters
  const urlParams = new URLSearchParams(window.location.search);
  const usernameFromUrl = urlParams.get('username');

  // Get worldId from URL parameter (required)
  const worldId = urlParams.get('worldId');

  if (usernameFromUrl) {
    logger.info('Username overridden by URL query parameter', { username: usernameFromUrl });
  }

  // Load required variables (from runtime config)
  const apiUrl = runtimeConfig.apiUrl;

  // Validate required fields
  const missing: string[] = [];
  if (!apiUrl) missing.push('apiUrl (config.json or .env)');
  if (!worldId) missing.push('worldId (URL parameter)');

  if (missing.length > 0) {
    const error = `Missing required configuration: ${missing.join(', ')}`;
    logger.error(error);
    throw new Error(error);
  }

  // Load optional variables
  const logToConsole = env.LOG_TO_CONSOLE === 'true';

  const config: ClientConfig = {
    // websocketUrl and exitUrl will be set after loading server config
    apiUrl: apiUrl!,
    worldId: worldId!,
    logToConsole,
  };

  logger.info('Initial client configuration loaded', {
    apiUrl,
    worldId,
    logToConsole,
    note: 'websocketUrl and exitUrl will be loaded from server config',
  });

  return config;
}

/**
 * Update client config with server-provided connection info
 * Called after EngineConfiguration is loaded from server
 */
export function updateConfigFromServer(config: ClientConfig, websocketUrl: string, exitUrl: string): void {
  config.websocketUrl = websocketUrl + "/world/" + config.worldId;
  config.exitUrl = exitUrl;

  logger.info('Client configuration updated with server info', {
    websocketUrl: config.websocketUrl,
    exitUrl,
  });
}

/**
 * Get environment variables based on runtime environment
 */
function getEnvironment(): Record<string, string | undefined> {
  // Check if we're in Node.js environment (tests)
  if (typeof process !== 'undefined' && process.env) {
    return process.env as Record<string, string | undefined>;
  }

  // Check if we're in Vite environment (browser)
  if (typeof import.meta !== 'undefined' && import.meta.env) {
    // Vite prefixes env vars with VITE_
    const env = import.meta.env as Record<string, string | undefined>;
    return {
      CLIENT_USERNAME: env.VITE_CLIENT_USERNAME,
      CLIENT_PASSWORD: env.VITE_CLIENT_PASSWORD,
      SERVER_WEBSOCKET_URL: env.VITE_SERVER_WEBSOCKET_URL,
      SERVER_API_URL: env.VITE_SERVER_API_URL,
      LOG_TO_CONSOLE: env.VITE_LOG_TO_CONSOLE,
      EXIT_URL: env.VITE_EXIT_URL,
    };
  }

  // Fallback: empty environment
  logger.warn('Unable to detect environment, using empty config');
  return {};
}
