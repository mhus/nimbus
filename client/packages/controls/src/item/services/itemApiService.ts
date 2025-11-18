/**
 * Item API Service
 * Handles all HTTP requests to the item REST API
 */

import type { ItemData } from '@nimbus/shared';
import { ApiService } from '../../services/ApiService';

export interface ItemSearchResult {
  itemId: string;
  name: string;
  texture?: string;
}

export class ItemApiService {
  private static apiService = new ApiService();

  private static getWorldId(): string {
    return this.apiService.getCurrentWorldId();
  }

  /**
   * Search for items
   */
  static async searchItems(query: string = ''): Promise<ItemSearchResult[]> {
    const worldId = this.getWorldId();
    const queryParam = query ? `?query=${encodeURIComponent(query)}` : '';
    const url = `/api/worlds/${worldId}/items${queryParam}`;

    const response = await this.apiService.get<{ items: ItemSearchResult[] }>(url);
    return response.items || [];
  }

  /**
   * Get item data by ID
   */
  static async getItem(itemId: string): Promise<ItemData | null> {
    const worldId = this.getWorldId();
    const url = `/api/worlds/${worldId}/item/${encodeURIComponent(itemId)}`;

    try {
      return await this.apiService.get<ItemData>(url);
    } catch (error: any) {
      if (error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  }

  /**
   * Create a new item
   */
  static async createItem(itemId: string, itemData: ItemData): Promise<void> {
    const worldId = this.getWorldId();
    const url = `/api/worlds/${worldId}/items`;

    await this.apiService.post(url, { itemId, itemData });
  }

  /**
   * Update an existing item
   */
  static async updateItem(itemId: string, itemData: ItemData): Promise<void> {
    const worldId = this.getWorldId();
    const url = `/api/worlds/${worldId}/item/${encodeURIComponent(itemId)}`;

    await this.apiService.put(url, itemData);
  }

  /**
   * Delete an item
   */
  static async deleteItem(itemId: string): Promise<void> {
    const worldId = this.getWorldId();
    const url = `/api/worlds/${worldId}/item/${encodeURIComponent(itemId)}`;

    try {
      await this.apiService.delete(url);
    } catch (error: any) {
      // Ignore 404 errors
      if (error.response?.status !== 404) {
        throw error;
      }
    }
  }
}
