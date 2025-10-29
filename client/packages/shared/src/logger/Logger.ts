/**
 * Logger implementation
 */

import { LogLevel, LogLevelNames } from './LogLevel';
import type { LogEntry, LogFormatter, LogTransport } from './LogEntry';
import { createConsoleTransport } from './transports/ConsoleTransport';

/**
 * Logger configuration
 */
export interface LoggerConfig {
  /** Minimum log level to process */
  minLevel: LogLevel;

  /** Custom formatter (optional) */
  formatter?: LogFormatter;

  /** Custom transports (optional) */
  transports?: LogTransport[];

  /** Enable timestamp in logs */
  includeTimestamp?: boolean;

  /** Enable stack traces for errors */
  includeStack?: boolean;
}

/**
 * Logger instance
 */
export class Logger {
  private name: string;
  private config: LoggerConfig;

  constructor(name: string, config: Partial<LoggerConfig> = {}) {
    this.name = name;

    // Use ConsoleTransport as default if no transports provided
    const defaultTransports = [
      createConsoleTransport({
        includeTimestamp: config.includeTimestamp ?? true,
        includeStack: config.includeStack ?? true,
      }),
    ];

    this.config = {
      minLevel: LogLevel.INFO,
      includeTimestamp: true,
      includeStack: true,
      transports: defaultTransports,
      ...config,
    };
  }

  /**
   * Set minimum log level
   */
  setLevel(level: LogLevel): void {
    this.config.minLevel = level;
  }

  /**
   * Get current log level
   */
  getLevel(): LogLevel {
    return this.config.minLevel;
  }

  /**
   * Check if level is enabled
   */
  isLevelEnabled(level: LogLevel): boolean {
    return level <= this.config.minLevel;
  }

  /**
   * Log at specified level
   */
  log(level: LogLevel, message: string, data?: any, error?: Error): void {
    if (!this.isLevelEnabled(level)) {
      return;
    }

    const entry: LogEntry = {
      level,
      name: this.name,
      message,
      timestamp: Date.now(),
      data,
      error,
    };

    // Add stack trace for errors
    if (error && this.config.includeStack) {
      entry.stack = error.stack;
    }

    // Use transports to output log entry
    if (this.config.transports && this.config.transports.length > 0) {
      this.config.transports.forEach((transport) => {
        try {
          transport(entry);
        } catch (error) {
          // Transport errors should not break logging
          console.error('[Logger] Transport error:', error);
        }
      });
    }
  }

  /**
   * Fatal error (system must stop)
   */
  fatal(message: string, data?: any, error?: Error): void {
    this.log(LogLevel.FATAL, message, data, error);
  }

  /**
   * Error message
   */
  error(message: string, data?: any, error?: Error): void {
    this.log(LogLevel.ERROR, message, data, error);
  }

  /**
   * Warning message
   */
  warn(message: string, data?: any): void {
    this.log(LogLevel.WARN, message, data);
  }

  /**
   * Info message
   */
  info(message: string, data?: any): void {
    this.log(LogLevel.INFO, message, data);
  }

  /**
   * Debug message
   */
  debug(message: string, data?: any): void {
    this.log(LogLevel.DEBUG, message, data);
  }

  /**
   * Trace message
   */
  trace(message: string, data?: any): void {
    this.log(LogLevel.TRACE, message, data);
  }
}
