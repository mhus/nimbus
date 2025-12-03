/**
 * BlockType Registry
 * Loads BlockTypes on-demand from filesystem
 */

import fs from 'fs';
import path from 'path';
import { getLogger, getBlockTypeGroup, buildBlockTypeId } from '@nimbus/shared';
import type { BlockType } from '@nimbus/shared';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const logger = getLogger('BlockTypeRegistry');

export class BlockTypeRegistry {
  private blocktypesDir: string;
  private allBlockTypesCache: BlockType[] | null = null;

  constructor() {
    this.blocktypesDir = path.join(__dirname, '../../files/blocktypes');
    logger.info('BlockTypeRegistry initialized (lazy loading from filesystem)');
  }

  /**
   * Get file path for BlockType ID
   * XXX: handle as string not as int!
   */
  private getBlockTypeFilePath(id: string | number): string {
    if (typeof id === 'number') {
      const subDir = 'w'; // default group for numeric IDs
      return path.join(this.blocktypesDir, subDir.toString(), `${id}.json`);
    }
    const group = getBlockTypeGroup(id);
    const blockTypeName = id.toString().includes(':') ? id.toString().split(':')[1] : id.toString();
    return path.join(this.blocktypesDir, group, `${blockTypeName}.json`);
  }

  /**
   * Load BlockType from filesystem on-demand
   * @param id BlockType ID
   * @returns BlockType or undefined if not found
   */
  getBlockType(id: string | number): BlockType | undefined {
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
   * Get all available BlockTypes (reads manifest)
   * @returns Array of all BlockTypes
   */
  getAllBlockTypes(): BlockType[] {
    // Return cached data if available
    if (this.allBlockTypesCache !== null) {
      return this.allBlockTypesCache;
    }

    // Load from filesystem on first access
    try {
      const manifestPath = path.join(this.blocktypesDir, 'manifest.json');

      if (!fs.existsSync(manifestPath)) {
        logger.warn('Manifest not found, returning empty array');
        this.allBlockTypesCache = [];
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

      logger.info(`Loaded ${blockTypes.length} BlockTypes from manifest (cached for future use)`);

      // Cache the result
      this.allBlockTypesCache = blockTypes;
      return blockTypes;
    } catch (error) {
      logger.error('Failed to load BlockTypes from manifest', {}, error as Error);
      this.allBlockTypesCache = [];
      return [];
    }
  }

  /**
   * Search BlockTypes by query (searches in name, description, etc.)
   * @param query Search query (case insensitive)
   * @returns Array of matching BlockTypes
   */
  searchBlockTypes(query: string): BlockType[] {
    const allBlockTypes = this.getAllBlockTypes();
    const lowerQuery = query.toLowerCase();

    return allBlockTypes.filter(blockType => {
      // Search in ID (as string)
      if (blockType.id.toString().includes(lowerQuery)) {
        return true;
      }

      // Search in description if available
      if (blockType.description?.toLowerCase().includes(lowerQuery)) {
        return true;
      }

      return false;
    });
  }

  /**
   * Create a new BlockType
   * @param blockType BlockType to create
   * @returns The created BlockType or undefined if creation failed
   */
  createBlockType(blockType: BlockType): BlockType | undefined {
    try {
      const filePath = this.getBlockTypeFilePath(blockType.id);
      const dirPath = path.dirname(filePath);

      // Create directory if it doesn't exist
      if (!fs.existsSync(dirPath)) {
        fs.mkdirSync(dirPath, { recursive: true });
      }

      // Check if BlockType already exists
      if (fs.existsSync(filePath)) {
        logger.error(`BlockType ${blockType.id} already exists`);
        return undefined;
      }

      // Write BlockType to file
      fs.writeFileSync(filePath, JSON.stringify(blockType, null, 2), 'utf-8');

      // Update manifest
      this.updateManifest(blockType.id, 'add');

      logger.info(`Created BlockType ${blockType.id}`);
      return blockType;
    } catch (error) {
      logger.error(`Failed to create BlockType ${blockType.id}`, {}, error as Error);
      return undefined;
    }
  }

  /**
   * Update an existing BlockType
   * @param blockType BlockType to update
   * @returns The updated BlockType or undefined if update failed
   */
  updateBlockType(blockType: BlockType): BlockType | undefined {
    try {
      const filePath = this.getBlockTypeFilePath(blockType.id);

      // Check if BlockType exists
      if (!fs.existsSync(filePath)) {
        logger.error(`BlockType ${blockType.id} not found`);
        return undefined;
      }

      // Write updated BlockType to file
      fs.writeFileSync(filePath, JSON.stringify(blockType, null, 2), 'utf-8');

      logger.info(`Updated BlockType ${blockType.id}`);
      return blockType;
    } catch (error) {
      logger.error(`Failed to update BlockType ${blockType.id}`, {}, error as Error);
      return undefined;
    }
  }

  /**
   * Delete a BlockType
   * @param id BlockType ID to delete
   * @returns true if deleted successfully, false otherwise
   */
  deleteBlockType(id: string | number): boolean {
    try {
      const filePath = this.getBlockTypeFilePath(id);

      // Check if BlockType exists
      if (!fs.existsSync(filePath)) {
        logger.error(`BlockType ${id} not found`);
        return false;
      }

      // Delete file
      fs.unlinkSync(filePath);

      // Update manifest
      this.updateManifest(id, 'remove');

      logger.info(`Deleted BlockType ${id}`);
      return true;
    } catch (error) {
      logger.error(`Failed to delete BlockType ${id}`, {}, error as Error);
      return false;
    }
  }

  /**
   * Update manifest (add or remove entry)
   * @param id BlockType ID
   * @param action 'add' or 'remove'
   */
  private updateManifest(id: string | number, action: 'add' | 'remove'): void {
    const numericId = typeof id === 'string' ? parseInt(id, 10) : id;
    try {
      const manifestPath = path.join(this.blocktypesDir, 'manifest.json');

      let manifest: any[] = [];
      if (fs.existsSync(manifestPath)) {
        manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));
      }

      if (action === 'add') {
        // Add entry to manifest
        const subDir = Math.floor(numericId / 100);
        const entry = {
          id: numericId,
          name: `blocktype_${numericId}`, // Will be updated by the actual BlockType name if needed
          file: `${subDir}/${numericId}.json`
        };

        // Check if entry already exists
        const existingIndex = manifest.findIndex((e: any) => e.id === numericId);
        if (existingIndex === -1) {
          manifest.push(entry);
          manifest.sort((a: any, b: any) => a.id - b.id);
        }
      } else if (action === 'remove') {
        // Remove entry from manifest
        manifest = manifest.filter((e: any) => e.id !== numericId);
      }

      // Write updated manifest
      fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2), 'utf-8');

      logger.debug(`Updated manifest: ${action} BlockType ${numericId}`);
    } catch (error) {
      logger.error(`Failed to update manifest for BlockType ${numericId}`, {}, error as Error);
    }
  }

  /**
   * Get next available BlockType ID
   * @returns Next available ID
   */
  getNextAvailableId(): number {
    try {
      const manifestPath = path.join(this.blocktypesDir, 'manifest.json');

      if (!fs.existsSync(manifestPath)) {
        return 100; // Start at 100 if no manifest exists
      }

      const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf-8'));

      if (manifest.length === 0) {
        return 100;
      }

      // Find highest ID and add 1
      const maxId = Math.max(...manifest.map((e: any) => e.id));
      return maxId + 1;
    } catch (error) {
      logger.error('Failed to get next available ID', {}, error as Error);
      return 100;
    }
  }

  /**
   * Get all BlockTypes in a specific group
   * Loads from: files/blocktypes/{groupName}/*.json
   *
   * @param groupName The group name (e.g., 'core', 'w', 'custom')
   * @returns Array of BlockTypes in the group
   */
  getBlockTypesByGroup(groupName: string): BlockType[] {
    try {
      const groupDir = path.join(this.blocktypesDir, groupName);

      logger.info(`Loading BlockTypes from group directory: ${groupDir}`);

      // Check if group directory exists
      if (!fs.existsSync(groupDir)) {
        logger.warn(`BlockType group directory not found: ${groupName} (path: ${groupDir})`);
        return [];
      }

      // Read all files in the group directory
      const files = fs.readdirSync(groupDir);
      logger.info(`Found ${files.length} files in group '${groupName}': ${files.join(', ')}`);

      const jsonFiles = files.filter(f => f.endsWith('.json'));
      logger.info(`Found ${jsonFiles.length} JSON files in group '${groupName}': ${jsonFiles.join(', ')}`);

      const blockTypes: BlockType[] = [];

      for (const file of jsonFiles) {
        try {
          const filePath = path.join(groupDir, file);
          logger.debug(`Loading BlockType from file: ${filePath}`);

          const data = fs.readFileSync(filePath, 'utf-8');
          const blockType = JSON.parse(data) as BlockType;

          // Ensure the BlockType ID is properly formatted with the group
          const blockTypeName = path.basename(file, '.json');
          const idAsString = String(blockType.id);

          // If ID doesn't contain ':', add the group prefix
          if (!idAsString.includes(':')) {
            blockType.id = buildBlockTypeId(groupName, blockTypeName);
          }

          logger.debug(`Loaded BlockType: ${blockType.id} (from file: ${file})`);
          blockTypes.push(blockType);
        } catch (error) {
          logger.error(`Failed to load BlockType from file: ${file}`, {}, error as Error);
        }
      }

      logger.info(`Successfully loaded ${blockTypes.length} BlockTypes from group '${groupName}'`);
      return blockTypes;
    } catch (error) {
      logger.error(`Failed to load BlockTypes for group '${groupName}'`, {}, error as Error);
      return [];
    }
  }
}
