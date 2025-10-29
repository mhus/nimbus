/**
 * Block validation functions
 */

import type { Block, Offsets } from '../types/Block';
import type { BlockType } from '../types/BlockType';
import type { BlockMetadata } from '../types/BlockMetadata';
import { BlockStatus } from '../types/BlockType';

/**
 * Validation result
 */
export interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings?: string[];
}

/**
 * Block validators
 */
export namespace BlockValidator {
  /**
   * Validate block type ID
   * @param id Block type ID
   * @returns True if valid
   */
  export function isValidBlockTypeId(id: number): boolean {
    return Number.isInteger(id) && id >= 0 && id <= 65535;
  }

  /**
   * Validate block status
   * @param status Status value
   * @returns True if valid
   */
  export function isValidStatus(status: number): boolean {
    return Number.isInteger(status) && status >= 0 && status <= 255;
  }

  /**
   * Validate offsets array
   * @param offsets Offsets array
   * @returns Validation result
   */
  export function validateOffsets(offsets: Offsets): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!Array.isArray(offsets)) {
      errors.push('Offsets must be an array');
      return { valid: false, errors, warnings };
    }

    // Check each value
    offsets.forEach((value, index) => {
      if (!Number.isFinite(value)) {
        errors.push(`Offset[${index}] is not a finite number: ${value}`);
      }

      if (value < -127 || value > 127) {
        errors.push(`Offset[${index}] out of range (-127 to 127): ${value}`);
      }

      if (!Number.isInteger(value)) {
        warnings.push(`Offset[${index}] should be integer: ${value}`);
      }
    });

    // Warn if unusual length
    if (offsets.length > 0 && offsets.length !== 24 && offsets.length % 3 !== 0) {
      warnings.push(
        `Unusual offset count: ${offsets.length} (expected multiple of 3 for XYZ)`
      );
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate face visibility value
   * @param value Face visibility bitfield
   * @returns True if valid
   */
  export function isValidFaceVisibility(value: number): boolean {
    // Should be 7-bit value (6 faces + 1 fixed flag)
    return Number.isInteger(value) && value >= 0 && value <= 127;
  }

  /**
   * Validate block instance
   * @param block Block to validate
   * @returns Validation result
   */
  export function validateBlock(block: Block): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate position
    if (!block.position) {
      errors.push('Block position is required');
    } else {
      if (!Number.isFinite(block.position.x)) {
        errors.push('Position.x is not a finite number');
      }
      if (!Number.isFinite(block.position.y)) {
        errors.push('Position.y is not a finite number');
      }
      if (!Number.isFinite(block.position.z)) {
        errors.push('Position.z is not a finite number');
      }

      // Warn about unusual positions
      if (Math.abs(block.position.y) > 512) {
        warnings.push(`Unusual Y position: ${block.position.y}`);
      }
    }

    // Validate block type ID
    if (!isValidBlockTypeId(block.blockTypeId)) {
      errors.push(
        `Invalid block type ID: ${block.blockTypeId} (must be 0-65535)`
      );
    }

    // Validate offsets if present
    if (block.offsets) {
      const offsetValidation = validateOffsets(block.offsets);
      errors.push(...offsetValidation.errors);
      if (offsetValidation.warnings) {
        warnings.push(...offsetValidation.warnings);
      }
    }

    // Validate face visibility if present
    if (block.faceVisibility) {
      if (!isValidFaceVisibility(block.faceVisibility.value)) {
        errors.push(
          `Invalid face visibility: ${block.faceVisibility.value} (must be 0-127)`
        );
      }
    }

    // Validate status if present
    if (block.status !== undefined && !isValidStatus(block.status)) {
      errors.push(`Invalid status: ${block.status} (must be 0-255)`);
    }

    // Validate metadata if present
    if (block.metadata) {
      const metadataValidation = validateBlockMetadata(block.metadata);
      errors.push(...metadataValidation.errors);
      if (metadataValidation.warnings) {
        warnings.push(...metadataValidation.warnings);
      }
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate block type
   * @param blockType Block type to validate
   * @returns Validation result
   */
  export function validateBlockType(blockType: BlockType): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate ID
    if (!isValidBlockTypeId(blockType.id)) {
      errors.push(`Invalid ID: ${blockType.id}`);
    }

    // Validate initial status
    if (
      blockType.initialStatus !== undefined &&
      !isValidStatus(blockType.initialStatus)
    ) {
      errors.push(`Invalid initial status: ${blockType.initialStatus}`);
    }

    // Validate modifiers
    if (!blockType.modifiers) {
      errors.push('Modifiers are required');
    } else {
      // Check for default modifier (status 0)
      if (!blockType.modifiers[BlockStatus.DEFAULT]) {
        errors.push('Default modifier (status 0) is required');
      }

      // Validate each modifier's status key
      Object.keys(blockType.modifiers).forEach((statusStr) => {
        const status = parseInt(statusStr, 10);
        if (!isValidStatus(status)) {
          errors.push(`Invalid modifier status key: ${statusStr}`);
        }
      });

      // Check if initial status has corresponding modifier
      if (
        blockType.initialStatus !== undefined &&
        !blockType.modifiers[blockType.initialStatus]
      ) {
        warnings.push(
          `Initial status ${blockType.initialStatus} has no corresponding modifier`
        );
      }
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate block metadata
   * @param metadata Block metadata
   * @returns Validation result
   */
  export function validateBlockMetadata(
    metadata: BlockMetadata
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate group ID
    if (metadata.groupId !== undefined) {
      if (!Number.isInteger(metadata.groupId) || metadata.groupId < 0) {
        errors.push(`Invalid group ID: ${metadata.groupId}`);
      }
    }

    // Validate modifiers if present
    if (metadata.modifiers) {
      Object.keys(metadata.modifiers).forEach((statusStr) => {
        const status = parseInt(statusStr, 10);
        if (!isValidStatus(status)) {
          errors.push(`Invalid metadata modifier status: ${statusStr}`);
        }
      });
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate block array (for network messages)
   * @param blocks Array of blocks
   * @param maxCount Maximum allowed blocks
   * @returns Validation result
   */
  export function validateBlockArray(
    blocks: Block[],
    maxCount: number = 10000
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!Array.isArray(blocks)) {
      errors.push('Blocks must be an array');
      return { valid: false, errors, warnings };
    }

    if (blocks.length > maxCount) {
      errors.push(`Too many blocks: ${blocks.length} (max: ${maxCount})`);
    }

    // Validate each block
    blocks.forEach((block, index) => {
      const result = validateBlock(block);
      if (!result.valid) {
        errors.push(`Block[${index}]: ${result.errors.join(', ')}`);
      }
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Quick validation (only critical checks)
   * @param block Block to validate
   * @returns True if valid
   */
  export function isValid(block: Block): boolean {
    return (
      block.position !== undefined &&
      Number.isFinite(block.position.x) &&
      Number.isFinite(block.position.y) &&
      Number.isFinite(block.position.z) &&
      isValidBlockTypeId(block.blockTypeId)
    );
  }

  /**
   * Sanitize block (fix invalid values)
   * @param block Block to sanitize
   * @returns Sanitized block
   */
  export function sanitize(block: Block): Block {
    const sanitized: Block = {
      position: {
        x: Number.isFinite(block.position?.x) ? block.position.x : 0,
        y: Number.isFinite(block.position?.y) ? block.position.y : 0,
        z: Number.isFinite(block.position?.z) ? block.position.z : 0,
      },
      blockTypeId: isValidBlockTypeId(block.blockTypeId)
        ? block.blockTypeId
        : 0,
    };

    // Copy optional fields if valid
    if (block.offsets) {
      const offsetValidation = validateOffsets(block.offsets);
      if (offsetValidation.valid) {
        sanitized.offsets = block.offsets;
      }
    }

    if (
      block.faceVisibility &&
      isValidFaceVisibility(block.faceVisibility.value)
    ) {
      sanitized.faceVisibility = block.faceVisibility;
    }

    if (block.status !== undefined && isValidStatus(block.status)) {
      sanitized.status = block.status;
    }

    if (block.metadata) {
      const metadataValidation = validateBlockMetadata(block.metadata);
      if (metadataValidation.valid) {
        sanitized.metadata = block.metadata;
      }
    }

    return sanitized;
  }
}
