/**
 * Logger factory for managing logger instances
 */

import { Logger, type LoggerConfig } from './Logger';
import { LogLevel, parseLogLevel } from './LogLevel';
import { ExceptionHandler } from '../errors/ExceptionHandler';

/**
 * Global logger configuration
 */
interface GlobalLoggerConfig extends Partial<LoggerConfig> {
  /** Default log level for all loggers */
  defaultLevel?: LogLevel;

  /** Per-logger level overrides (name → level) */
  loggerLevels?: Record<string, LogLevel>;
}

/**
 * Logger factory singleton
 */
class LoggerFactoryImpl {
  private loggers = new Map<string, Logger>();
  private config: GlobalLoggerConfig = {
    defaultLevel: LogLevel.INFO,
    includeTimestamp: true,
    includeStack: true,
  };

  /**
   * Get or create logger instance
   */
  getLogger(name: string): Logger {
    let logger = this.loggers.get(name);

    if (!logger) {
      const levelOverride = this.config.loggerLevels?.[name];
      const level = levelOverride ?? this.config.defaultLevel ?? LogLevel.INFO;

      logger = new Logger(name, {
        minLevel: level,
        formatter: this.config.formatter,
        transports: this.config.transports,
        includeTimestamp: this.config.includeTimestamp,
        includeStack: this.config.includeStack,
      });

      this.loggers.set(name, logger);
    }

    return logger;
  }

  /**
   * Configure global logger settings
   */
  configure(config: GlobalLoggerConfig): void {
    this.config = { ...this.config, ...config };

    // Update existing loggers
    this.loggers.forEach((logger, name) => {
      const levelOverride = this.config.loggerLevels?.[name];
      const level = levelOverride ?? this.config.defaultLevel ?? LogLevel.INFO;
      logger.setLevel(level);
    });
  }

  /**
   * Set log level for specific logger
   */
  setLoggerLevel(name: string, level: LogLevel): void {
    if (!this.config.loggerLevels) {
      this.config.loggerLevels = {};
    }
    this.config.loggerLevels[name] = level;

    const logger = this.loggers.get(name);
    if (logger) {
      logger.setLevel(level);
    }
  }

  /**
   * Set default log level
   */
  setDefaultLevel(level: LogLevel): void {
    this.config.defaultLevel = level;

    // Update all loggers without specific overrides
    this.loggers.forEach((logger, name) => {
      if (!this.config.loggerLevels?.[name]) {
        logger.setLevel(level);
      }
    });
  }

  /**
   * Configure from environment
   */
  configureFromEnv(): void {
    try {
      // Check environment variable for log level
      let envLevel: string | undefined;

      // Node.js environment
      if (typeof process !== 'undefined' && process.env) {
        envLevel = process.env.LOG_LEVEL;
      }

      // Vite environment (browser)
      if (!envLevel && typeof import.meta !== 'undefined') {
        const meta = import.meta as any;
        if (meta.env) {
          envLevel = meta.env.VITE_LOG_LEVEL;
        }
      }

      if (envLevel) {
        const level = parseLogLevel(envLevel);
        this.setDefaultLevel(level);
      }

      // Check for per-logger levels (format: "NetworkService=DEBUG,ChunkService=TRACE")
      let envLoggers: string | undefined;

      // Node.js environment
      if (typeof process !== 'undefined' && process.env) {
        envLoggers = process.env.LOG_LOGGERS;
      }

      // Vite environment (browser)
      if (!envLoggers && typeof import.meta !== 'undefined') {
        const meta = import.meta as any;
        if (meta.env) {
          envLoggers = meta.env.VITE_LOG_LOGGERS;
        }
      }

      if (envLoggers) {
        const parts = envLoggers.split(',');
        parts.forEach((part: string) => {
          const [name, levelStr] = part.split('=');
          if (name && levelStr) {
            const level = parseLogLevel(levelStr.trim());
            this.setLoggerLevel(name.trim(), level);
          }
        });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'LoggerFactory.configureFromEnv');
      // Don't rethrow - configuration errors should not break initialization
    }
  }

  /**
   * Get all logger names
   */
  getLoggerNames(): string[] {
    return Array.from(this.loggers.keys());
  }

  /**
   * Clear all loggers (for testing)
   */
  clear(): void {
    this.loggers.clear();
  }
}

/**
 * Global logger factory instance
 */
export const LoggerFactory = new LoggerFactoryImpl();

/**
 * Convenience function to get logger
 */
export function getLogger(name: string): Logger {
  return LoggerFactory.getLogger(name);
}
