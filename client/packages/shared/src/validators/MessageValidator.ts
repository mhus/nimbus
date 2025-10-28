/**
 * Network message validation functions
 */

import type { BaseMessage } from '../network/BaseMessage';
import { MessageType } from '../network/MessageTypes';
import type { ValidationResult } from './BlockValidator';

/**
 * Message validators
 */
export namespace MessageValidator {
  /**
   * Validate message ID format
   * @param id Message ID
   * @returns True if valid
   */
  export function isValidMessageId(id: string): boolean {
    return typeof id === 'string' && id.length > 0 && id.length <= 100;
  }

  /**
   * Validate message type
   * @param type Message type
   * @returns True if valid
   */
  export function isValidMessageType(type: string): boolean {
    return Object.values(MessageType).includes(type as MessageType);
  }

  /**
   * Validate base message structure
   * @param message Message to validate
   * @returns Validation result
   */
  export function validateBaseMessage(message: BaseMessage): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate type
    if (!message.t) {
      errors.push('Message type (t) is required');
    } else if (!isValidMessageType(message.t)) {
      errors.push(`Invalid message type: ${message.t}`);
    }

    // Validate message ID if present
    if (message.i !== undefined && !isValidMessageId(message.i)) {
      errors.push(`Invalid message ID: ${message.i}`);
    }

    // Validate response ID if present
    if (message.r !== undefined && !isValidMessageId(message.r)) {
      errors.push(`Invalid response ID: ${message.r}`);
    }

    // Check mutual exclusivity of i and r
    if (message.i && message.r) {
      warnings.push('Message has both i and r (unusual)');
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate JSON message string
   * @param jsonStr JSON message string
   * @returns Validation result with parsed message
   */
  export function validateMessageJSON(
    jsonStr: string
  ): ValidationResult & { message?: BaseMessage } {
    const errors: string[] = [];
    const warnings: string[] = [];

    let message: BaseMessage;

    // Try to parse JSON
    try {
      message = JSON.parse(jsonStr);
    } catch (e) {
      errors.push(`Invalid JSON: ${e instanceof Error ? e.message : 'Parse error'}`);
      return { valid: false, errors, warnings };
    }

    // Validate base message
    const baseValidation = validateBaseMessage(message);
    errors.push(...baseValidation.errors);
    if (baseValidation.warnings) {
      warnings.push(...baseValidation.warnings);
    }

    // Check message size
    if (jsonStr.length > 10 * 1024 * 1024) {
      // 10 MB
      warnings.push(
        `Message is very large: ${(jsonStr.length / 1024 / 1024).toFixed(2)} MB`
      );
    }

    return { valid: errors.length === 0, errors, warnings, message };
  }

  /**
   * Quick validation (only critical checks)
   * @param message Message to validate
   * @returns True if valid
   */
  export function isValid(message: BaseMessage): boolean {
    return (
      message &&
      typeof message === 'object' &&
      isValidMessageType(message.t)
    );
  }
}
