/**
 * Chunk validation functions
 */

import type { ChunkData } from '../types/ChunkData';
import type { ChunkDataTransferObject, ChunkCoordinate } from '../network/messages/ChunkMessage';
import type { ValidationResult } from './BlockValidator';
import { BlockValidator } from './BlockValidator';

/**
 * Chunk validators
 */
export namespace ChunkValidator {
  /**
   * Validate chunk coordinate
   * @param cx Chunk X coordinate
   * @param cz Chunk Z coordinate
   * @returns True if valid
   */
  export function isValidChunkCoordinate(cx: number, cz: number): boolean {
    return (
      Number.isInteger(cx) &&
      Number.isInteger(cz) &&
      Math.abs(cx) < 1000000 &&
      Math.abs(cz) < 1000000
    );
  }

  /**
   * Validate chunk size
   * @param size Chunk size
   * @returns True if valid
   */
  export function isValidChunkSize(size: number): boolean {
    // Must be power of 2: 8, 16, 32, 64
    return (
      Number.isInteger(size) &&
      size > 0 &&
      size <= 128 &&
      (size & (size - 1)) === 0
    );
  }

  /**
   * Validate ChunkData
   * @param chunk Chunk data to validate
   * @param worldHeight Expected world height
   * @returns Validation result
   */
  export function validateChunkData(
    chunk: ChunkData,
    worldHeight: number = 256
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate coordinates
    if (!isValidChunkCoordinate(chunk.cx, chunk.cz)) {
      errors.push(`Invalid chunk coordinates: (${chunk.cx}, ${chunk.cz})`);
    }

    // Validate size
    if (!isValidChunkSize(chunk.size)) {
      errors.push(
        `Invalid chunk size: ${chunk.size} (must be power of 2: 8, 16, 32, 64)`
      );
    }

    // Validate blocks array
    if (!chunk.blocks || !(chunk.blocks instanceof Uint16Array)) {
      errors.push('Blocks must be a Uint16Array');
    } else {
      const expectedLength = chunk.size * worldHeight * chunk.size;
      if (chunk.blocks.length !== expectedLength) {
        errors.push(
          `Invalid blocks array length: ${chunk.blocks.length} (expected: ${expectedLength})`
        );
      }

      // Check for invalid block IDs
      let invalidCount = 0;
      for (let i = 0; i < chunk.blocks.length; i++) {
        const blockId = chunk.blocks[i];
        if (!BlockValidator.isValidBlockTypeId(blockId)) {
          invalidCount++;
        }
      }

      if (invalidCount > 0) {
        errors.push(`Found ${invalidCount} invalid block type IDs`);
      }
    }

    // Validate height data if present
    if (chunk.heightData) {
      if (chunk.heightData.length % 4 !== 0) {
        errors.push(
          `Invalid height data length: ${chunk.heightData.length} (must be multiple of 4)`
        );
      }

      const expectedHeightDataLength = chunk.size * chunk.size * 4;
      if (chunk.heightData.length !== expectedHeightDataLength) {
        warnings.push(
          `Height data length mismatch: ${chunk.heightData.length} (expected: ${expectedHeightDataLength})`
        );
      }
    }

    // Validate sparse blocks if present
    if (chunk.sparseBlocks) {
      chunk.sparseBlocks.forEach((blockId, index) => {
        if (!BlockValidator.isValidBlockTypeId(blockId)) {
          errors.push(
            `Invalid block ID in sparse blocks at index ${index}: ${blockId}`
          );
        }
      });
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate ChunkDataTransferObject
   * @param transferObj Transfer object to validate
   * @returns Validation result
   */
  export function validateChunkTransferObject(
    transferObj: ChunkDataTransferObject
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    // Validate coordinates
    if (!isValidChunkCoordinate(transferObj.cx, transferObj.cz)) {
      errors.push(
        `Invalid chunk coordinates: (${transferObj.cx}, ${transferObj.cz})`
      );
    }

    // Validate blocks array
    if (!Array.isArray(transferObj.b)) {
      errors.push('Blocks (b) must be an array');
    } else {
      const blockValidation = BlockValidator.validateBlockArray(
        transferObj.b,
        100000
      );
      errors.push(...blockValidation.errors);
      if (blockValidation.warnings) {
        warnings.push(...blockValidation.warnings);
      }
    }

    // Validate height data
    if (!Array.isArray(transferObj.h)) {
      errors.push('Height data (h) must be an array');
    } else {
      transferObj.h.forEach((heightData, index) => {
        if (!Array.isArray(heightData) || heightData.length !== 4) {
          errors.push(
            `Height data[${index}] must be array of 4 values: [maxHeight, minHeight, groundLevel, waterHeight]`
          );
        } else {
          const [maxHeight, minHeight, groundLevel, waterHeight] = heightData;

          if (minHeight > maxHeight) {
            errors.push(
              `Height data[${index}]: minHeight (${minHeight}) > maxHeight (${maxHeight})`
            );
          }

          if (groundLevel < minHeight || groundLevel > maxHeight) {
            errors.push(
              `Height data[${index}]: groundLevel (${groundLevel}) out of range [${minHeight}, ${maxHeight}]`
            );
          }

          if (waterHeight < 0 || waterHeight > 512) {
            warnings.push(
              `Height data[${index}]: unusual waterHeight: ${waterHeight}`
            );
          }
        }
      });
    }

    // Validate areas if present
    if (transferObj.a) {
      if (!Array.isArray(transferObj.a)) {
        errors.push('Area data (a) must be an array');
      }
    }

    // Validate entities if present
    if (transferObj.e) {
      if (!Array.isArray(transferObj.e)) {
        errors.push('Entity data (e) must be an array');
      }
    }

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Validate chunk coordinate array
   * @param coordinates Array of chunk coordinates
   * @param maxCount Maximum allowed coordinates
   * @returns Validation result
   */
  export function validateChunkCoordinates(
    coordinates: ChunkCoordinate[],
    maxCount: number = 1000
  ): ValidationResult {
    const errors: string[] = [];
    const warnings: string[] = [];

    if (!Array.isArray(coordinates)) {
      errors.push('Coordinates must be an array');
      return { valid: false, errors, warnings };
    }

    if (coordinates.length > maxCount) {
      errors.push(
        `Too many coordinates: ${coordinates.length} (max: ${maxCount})`
      );
    }

    coordinates.forEach((coord, index) => {
      if (!coord.cx === undefined || coord.cz === undefined) {
        errors.push(`Coordinate[${index}] missing cx or cz`);
      } else if (!isValidChunkCoordinate(coord.cx, coord.cz)) {
        errors.push(
          `Coordinate[${index}] invalid: (${coord.cx}, ${coord.cz})`
        );
      }
    });

    // Check for duplicates
    const seen = new Set<string>();
    coordinates.forEach((coord, index) => {
      const key = `${coord.cx},${coord.cz}`;
      if (seen.has(key)) {
        warnings.push(`Duplicate coordinate at index ${index}: ${key}`);
      }
      seen.add(key);
    });

    return { valid: errors.length === 0, errors, warnings };
  }

  /**
   * Quick validation (only critical checks)
   * @param chunk Chunk data
   * @returns True if valid
   */
  export function isValid(chunk: ChunkData): boolean {
    return (
      isValidChunkCoordinate(chunk.cx, chunk.cz) &&
      isValidChunkSize(chunk.size) &&
      chunk.blocks instanceof Uint16Array
    );
  }
}
