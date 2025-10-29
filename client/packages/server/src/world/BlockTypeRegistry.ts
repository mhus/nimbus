/**
 * BlockType Registry
 * Loads BlockTypes on-demand from filesystem
 */

import fs from 'fs';
import path from 'path';
import { getLogger } from '@nimbus/shared';
import type { BlockType } from '@nimbus/shared';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const logger = getLogger('BlockTypeRegistry');

export class BlockTypeRegistry {
  private blocktypesDir: string;

  constructor() {
    this.blocktypesDir = path.join(__dirname, '../../files/blocktypes');
    logger.info('BlockTypeRegistry initialized (lazy loading from filesystem)');
  }

  /**
   * Get file path for BlockType ID
   * Schema: (id / 100)/id.json
   */
  private getBlockTypeFilePath(id: number): string {
    const subDir = Math.floor(id / 100);
    return path.join(this.blocktypesDir, subDir.toString(), `${id}.json`);
  }

  /**
   * Load BlockType from filesystem on-demand
   * @param id BlockType ID
   * @returns BlockType or undefined if not found
   */
  getBlockType(id: number): BlockType | undefined {
    try {
      const filePath = this.getBlockTypeFilePath(id);

      if (!fs.existsSync(filePath)) {
        logger.debug(`BlockType ${id} not found at ${filePath}`);
        return undefined;
      }

      const data = fs.readFileSync(filePath, 'utf-8');
      const blockType = JSON.parse(data) as BlockType;

      logger.debug(`Loaded BlockType ${id} from filesystem`);
      return blockType;
    } catch (error) {
      logger.error(`Failed to load BlockType ${id}`, {}, error as Error);
      return undefined;
    }
  }

  /**
   * Get range of BlockTypes
   * @param from Start ID (inclusive)
   * @param to End ID (inclusive)
   * @returns Array of BlockTypes
   */
  getBlockTypeRange(from: number, to: number): BlockType[] {
    const result: BlockType[] = [];

    for (let id = from; id <= to; id++) {
      const blockType = this.getBlockType(id);
      if (blockType) {
        result.push(blockType);
      }
    }

    logger.debug(`Loaded ${result.length} BlockTypes from range ${from}-${to}`);
    return result;
  }

  /**
   * Get all available BlockTypes (reads manifest)
   * @returns Array of all BlockTypes
   */
  getAllBlockTypes(): BlockType[] {
    try {
      const manifestPath = path.join(this.blocktypesDir, 'manifest.json');

      if (!fs.existsSync(manifestPath)) {
        logger.warn('Manifest not found, returning empty array');
        return [];
      }

      const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
      const blockTypes: BlockType[] = [];

      manifest.forEach((entry: any) => {
        const blockType = this.getBlockType(entry.id);
        if (blockType) {
          blockTypes.push(blockType);
        }
      });

      logger.info(`Loaded ${blockTypes.length} BlockTypes from manifest`);
      return blockTypes;
    } catch (error) {
      logger.error('Failed to load BlockTypes from manifest', {}, error as Error);
      return [];
    }
  }
}
