/**
 * Logger implementation
 */

import { LogLevel, LogLevelNames } from './LogLevel';
import type { LogEntry, LogFormatter, LogTransport } from './LogEntry';

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
    this.config = {
      minLevel: LogLevel.INFO,
      includeTimestamp: true,
      includeStack: true,
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

    // Use custom formatter or default
    const formatted = this.config.formatter
      ? this.config.formatter(entry)
      : this.defaultFormatter(entry);

    // Use custom transports or console
    if (this.config.transports && this.config.transports.length > 0) {
      this.config.transports.forEach((transport) => {
        try {
          transport(entry);
        } catch (error) {
          // Transport errors should not break logging
          console.error('[Logger] Transport error:', error);
        }
      });
    } else {
      this.consoleTransport(entry, formatted);
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

  /**
   * Default formatter
   */
  private defaultFormatter(entry: LogEntry): string {
    const level = LogLevelNames[entry.level].padEnd(5);
    const timestamp = this.config.includeTimestamp
      ? `[${new Date(entry.timestamp).toISOString()}] `
      : '';

    let message = `${timestamp}[${level}] [${entry.name}] ${entry.message}`;

    if (entry.data !== undefined) {
      try {
        message += `\n  Data: ${JSON.stringify(entry.data, null, 2)}`;
      } catch (error) {
        // Handle circular references
        message += `\n  Data: [Circular or non-serializable]`;
      }
    }

    if (entry.error) {
      message += `\n  Error: ${entry.error.message}`;
      if (entry.stack && this.config.includeStack) {
        message += `\n  Stack: ${entry.stack}`;
      }
    }

    return message;
  }

  /**
   * Console transport with colored output
   */
  private consoleTransport(entry: LogEntry, formatted: string): void {
    switch (entry.level) {
      case LogLevel.FATAL:
      case LogLevel.ERROR:
        console.error(formatted);
        break;
      case LogLevel.WARN:
        console.warn(formatted);
        break;
      case LogLevel.INFO:
        console.info(formatted);
        break;
      case LogLevel.DEBUG:
      case LogLevel.TRACE:
        console.debug(formatted);
        break;
      default:
        console.log(formatted);
    }
  }
}
