/**
 * Server configuration
 * Loads configuration from environment variables
 */

import { getLogger } from '@nimbus/shared';
import dotenv from 'dotenv';

// Load .env file
dotenv.config();

const logger = getLogger('ServerConfig');

/**
 * Server configuration interface
 */
export interface ServerConfig {
  /** Server port */
  port: number;

  /** Server host */
  host: string;

  /** Enable CORS */
  cors: boolean;

  /** Allowed origins for CORS */
  corsOrigins: string[];

  /** Basic auth username (optional, for testing) */
  authUsername?: string;

  /** Basic auth password (optional, for testing) */
  authPassword?: string;

  /** Ping interval in seconds */
  pingInterval: number;

  /** Enable debug logging */
  debug: boolean;
}

/**
 * Load server configuration from environment variables
 * @returns Server configuration
 */
export function loadServerConfig(): ServerConfig {
  logger.debug('Loading server configuration from environment');

  const config: ServerConfig = {
    port: parseInt(process.env.PORT || '3011', 10),
    host: process.env.HOST || '0.0.0.0',
    cors: process.env.CORS_ENABLED !== 'false',
    corsOrigins: process.env.CORS_ORIGINS
      ? process.env.CORS_ORIGINS.split(',')
      : ['http://localhost:5173', 'http://localhost:5174'],
    authUsername: process.env.AUTH_USERNAME,
    authPassword: process.env.AUTH_PASSWORD,
    pingInterval: parseInt(process.env.PING_INTERVAL || '30', 10),
    debug: process.env.DEBUG === 'true' || process.env.NODE_ENV !== 'production',
  };

  logger.info('Server configuration loaded', {
    port: config.port,
    host: config.host,
    cors: config.cors,
    pingInterval: config.pingInterval,
    debug: config.debug,
  });

  return config;
}
