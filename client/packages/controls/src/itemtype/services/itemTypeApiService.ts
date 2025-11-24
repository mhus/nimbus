/**
 * ItemType API Service
 *
 * Handles all REST API calls for ItemTypes
 */

import type { ItemType } from '@nimbus/shared';

const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:3000';
const WORLD_ID = 'main'; // Default world ID

/**
 * Search ItemTypes
 */
export async function searchItemTypes(query?: string): Promise<ItemType[]> {
  const url = query
    ? `${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes?query=${encodeURIComponent(query)}`
    : `${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes`;

  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to search ItemTypes: ${response.statusText}`);
  }

  const data = await response.json();
  return data.itemTypes;
}

/**
 * Get ItemType by ID
 */
export async function getItemType(itemTypeId: string): Promise<ItemType> {
  const response = await fetch(
    `${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes/${itemTypeId}`
  );

  if (!response.ok) {
    throw new Error(`Failed to load ItemType: ${response.statusText}`);
  }

  return response.json();
}

/**
 * Create new ItemType
 */
export async function createItemType(itemType: ItemType): Promise<ItemType> {
  const response = await fetch(`${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(itemType),
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || `Failed to create ItemType: ${response.statusText}`);
  }

  return response.json();
}

/**
 * Update ItemType
 */
export async function updateItemType(
  itemTypeId: string,
  updates: Partial<ItemType>
): Promise<ItemType> {
  const response = await fetch(
    `${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes/${itemTypeId}`,
    {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(updates),
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || `Failed to update ItemType: ${response.statusText}`);
  }

  return response.json();
}

/**
 * Delete ItemType
 */
export async function deleteItemType(itemTypeId: string): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/worlds/${WORLD_ID}/itemtypes/${itemTypeId}`,
    {
      method: 'DELETE',
    }
  );

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error || `Failed to delete ItemType: ${response.statusText}`);
  }
}
