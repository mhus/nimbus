/**
 * Client configuration
 * Loads configuration from environment variables
 */

import { getLogger } from '@nimbus/shared';

const logger = getLogger('ClientConfig');

/**
 * Client configuration interface
 */
export interface ClientConfig {
  /** Username for authentication */
  username: string;

  /** Password for authentication */
  password: string;

  /** WebSocket server URL */
  websocketUrl: string;

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
   * Default: '/login'
   */
  exitUrl: string;
}

/**
 * Load client configuration from environment variables
 * @returns Client configuration
 * @throws Error if required environment variables are missing
 */
export function loadClientConfig(): ClientConfig {
  logger.debug('Loading client configuration from environment');

  // Get environment based on build tool
  const env = getEnvironment();

  // Load required variables
  const username = env.CLIENT_USERNAME;
  const password = env.CLIENT_PASSWORD;
  const websocketUrl = env.SERVER_WEBSOCKET_URL;
  const apiUrl = env.SERVER_API_URL;
  const worldId = env.WORLD_ID || 'main'; // Default to 'main' if not specified

  // Validate required fields
  const missing: string[] = [];
  if (!username) missing.push('CLIENT_USERNAME');
  if (!password) missing.push('CLIENT_PASSWORD');
  if (!websocketUrl) missing.push('SERVER_WEBSOCKET_URL');
  if (!apiUrl) missing.push('SERVER_API_URL');

  if (missing.length > 0) {
    const error = `Missing required environment variables: ${missing.join(', ')}`;
    logger.error(error);
    throw new Error(error);
  }

  // Load optional variables
  const logToConsole = env.LOG_TO_CONSOLE === 'true';
  const exitUrl = env.EXIT_URL || '/login'; // Default to '/login' if not specified

  const config: ClientConfig = {
    username: username!,
    password: password!,
    websocketUrl: websocketUrl!,
    apiUrl: apiUrl!,
    worldId,
    logToConsole,
    exitUrl,
  };

  logger.debug('Client configuration loaded', {
    username,
    websocketUrl,
    apiUrl,
    worldId,
    logToConsole,
    exitUrl,
  });

  return config;
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
      WORLD_ID: env.VITE_WORLD_ID,
      LOG_TO_CONSOLE: env.VITE_LOG_TO_CONSOLE,
      EXIT_URL: env.VITE_EXIT_URL,
    };
  }

  // Fallback: empty environment
  logger.warn('Unable to detect environment, using empty config');
  return {};
}
