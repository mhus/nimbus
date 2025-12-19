/**
 * Anything Service
 * Manages WAnythingEntity CRUD operations
 */

import { ApiService } from './ApiService';
import type { WAnythingEntity } from '@shared/generated/entities/WAnythingEntity';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('AnythingService');

export interface ListAnythingParams {
  collection: string;
  regionId?: string;
  worldId?: string;
  type?: string;
  enabledOnly?: boolean;
  offset?: number;
  limit?: number;
}

export interface ListAnythingResponse {
  entities: WAnythingEntity[];
  count: number;
  limit: number;
  offset: number;
}

export interface CreateAnythingRequest {
  regionId?: string;
  worldId?: string;
  collection: string;
  name: string;
  description?: string;
  type?: string;
  data?: any;
}

export interface UpdateAnythingRequest {
  description?: string;
  type?: string;
  data?: any;
  enabled?: boolean;
}

export class AnythingService extends ApiService {
  /**
   * List entities with flexible filtering
   */
  async list(params: ListAnythingParams): Promise<ListAnythingResponse> {
    logger.debug('Listing entities', { params });

    const queryParams = new URLSearchParams();
    queryParams.append('collection', params.collection);

    if (params.regionId) queryParams.append('regionId', params.regionId);
    if (params.worldId) queryParams.append('worldId', params.worldId);
    if (params.type) queryParams.append('type', params.type);
    if (params.enabledOnly !== undefined) queryParams.append('enabledOnly', String(params.enabledOnly));
    if (params.offset !== undefined) queryParams.append('offset', String(params.offset));
    if (params.limit !== undefined) queryParams.append('limit', String(params.limit));

    const response = await this.get<ListAnythingResponse>(`/control/anything/list?${queryParams.toString()}`);
    logger.debug('Listed entities', { count: response.count });
    return response;
  }

  /**
   * Get entity by collection and name
   */
  async getByCollection(collection: string, name: string): Promise<WAnythingEntity> {
    logger.debug('Getting entity by collection', { collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    const response = await this.get<WAnythingEntity>(`/control/anything/by-collection?${queryParams.toString()}`);
    logger.debug('Got entity', { id: response.id });
    return response;
  }

  /**
   * Get entity by world, collection, and name
   */
  async getByWorld(worldId: string, collection: string, name: string): Promise<WAnythingEntity> {
    logger.debug('Getting entity by world', { worldId, collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('worldId', worldId);
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    const response = await this.get<WAnythingEntity>(`/control/anything/by-world?${queryParams.toString()}`);
    logger.debug('Got entity', { id: response.id });
    return response;
  }

  /**
   * Get entity by region, collection, and name
   */
  async getByRegion(regionId: string, collection: string, name: string): Promise<WAnythingEntity> {
    logger.debug('Getting entity by region', { regionId, collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('regionId', regionId);
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    const response = await this.get<WAnythingEntity>(`/control/anything/by-region?${queryParams.toString()}`);
    logger.debug('Got entity', { id: response.id });
    return response;
  }

  /**
   * Create new entity
   */
  async create(request: CreateAnythingRequest): Promise<WAnythingEntity> {
    logger.debug('Creating entity', { request });

    const response = await this.post<WAnythingEntity>('/control/anything', request);
    logger.info('Created entity', { id: response.id, collection: response.collection, name: response.name });
    return response;
  }

  /**
   * Update entity by ID
   */
  async update(id: string, request: UpdateAnythingRequest): Promise<WAnythingEntity> {
    logger.debug('Updating entity', { id, request });

    const response = await this.put<WAnythingEntity>(`/control/anything/${id}`, request);
    logger.info('Updated entity', { id: response.id });
    return response;
  }

  /**
   * Delete entity by collection and name
   */
  async deleteByCollection(collection: string, name: string): Promise<void> {
    logger.debug('Deleting entity by collection', { collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    await this.delete(`/control/anything/by-collection?${queryParams.toString()}`);
    logger.info('Deleted entity', { collection, name });
  }

  /**
   * Delete entity by world, collection, and name
   */
  async deleteByWorld(worldId: string, collection: string, name: string): Promise<void> {
    logger.debug('Deleting entity by world', { worldId, collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('worldId', worldId);
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    await this.delete(`/control/anything/by-world?${queryParams.toString()}`);
    logger.info('Deleted entity', { worldId, collection, name });
  }

  /**
   * Delete entity by region, collection, and name
   */
  async deleteByRegion(regionId: string, collection: string, name: string): Promise<void> {
    logger.debug('Deleting entity by region', { regionId, collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('regionId', regionId);
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    await this.delete(`/control/anything/by-region?${queryParams.toString()}`);
    logger.info('Deleted entity', { regionId, collection, name });
  }

  /**
   * Delete entity by region, world, collection, and name
   */
  async deleteByRegionAndWorld(regionId: string, worldId: string, collection: string, name: string): Promise<void> {
    logger.debug('Deleting entity by region and world', { regionId, worldId, collection, name });

    const queryParams = new URLSearchParams();
    queryParams.append('regionId', regionId);
    queryParams.append('worldId', worldId);
    queryParams.append('collection', collection);
    queryParams.append('name', name);

    await this.delete(`/control/anything/by-region?${queryParams.toString()}`);
    logger.info('Deleted entity', { regionId, worldId, collection, name });
  }
}

// Export singleton instance
export const anythingService = new AnythingService();
