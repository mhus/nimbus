/**
 * Base API Service
 * Provides HTTP client with axios and authentication handling
 */

import axios, { type AxiosInstance, type AxiosError } from 'axios';
import { getLogger } from '@nimbus/shared';

const logger = getLogger('ApiService');

export class ApiService {
  private client: AxiosInstance;
  private apiUrl: string;

  constructor() {
    this.apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:3000';

    this.client = axios.create({
      baseURL: this.apiUrl,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor for authentication
    this.client.interceptors.request.use(
      (config) => {
        // Add authentication if needed
        const username = import.meta.env.VITE_API_USERNAME;
        const password = import.meta.env.VITE_API_PASSWORD;

        if (username && password) {
          const token = btoa(`${username}:${password}`);
          config.headers.Authorization = `Basic ${token}`;
        }

        return config;
      },
      (error) => {
        logger.error('Request error', {}, error);
        return Promise.reject(error);
      }
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        logger.error('Response error', {
          url: error.config?.url,
          status: error.response?.status,
          data: error.response?.data
        }, error);
        return Promise.reject(error);
      }
    );

    logger.info('ApiService initialized', { apiUrl: this.apiUrl });
  }

  /**
   * Get axios instance for direct use
   */
  getClient(): AxiosInstance {
    return this.client;
  }

  /**
   * Get API base URL
   */
  getBaseUrl(): string {
    return this.apiUrl;
  }

  /**
   * Generic GET request
   */
  async get<T>(url: string, params?: any): Promise<T> {
    const response = await this.client.get<T>(url, { params });
    return response.data;
  }

  /**
   * Generic POST request
   */
  async post<T>(url: string, data?: any, config?: any): Promise<T> {
    const response = await this.client.post<T>(url, data, config);
    return response.data;
  }

  /**
   * Generic PUT request
   */
  async put<T>(url: string, data?: any, config?: any): Promise<T> {
    const response = await this.client.put<T>(url, data, config);
    return response.data;
  }

  /**
   * Generic DELETE request
   */
  async delete<T>(url: string): Promise<T> {
    const response = await this.client.delete<T>(url);
    return response.data;
  }

  /**
   * Upload binary data (for assets)
   */
  async uploadBinary<T>(url: string, data: ArrayBuffer | Blob, mimeType?: string): Promise<T> {
    const response = await this.client.post<T>(url, data, {
      headers: {
        'Content-Type': mimeType || 'application/octet-stream',
      },
    });
    return response.data;
  }

  /**
   * Update binary data (for assets)
   */
  async updateBinary<T>(url: string, data: ArrayBuffer | Blob, mimeType?: string): Promise<T> {
    const response = await this.client.put<T>(url, data, {
      headers: {
        'Content-Type': mimeType || 'application/octet-stream',
      },
    });
    return response.data;
  }
}

// Singleton instance
export const apiService = new ApiService();
