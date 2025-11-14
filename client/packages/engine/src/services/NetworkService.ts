/**
 * NetworkService - WebSocket connection and message routing
 *
 * Manages WebSocket connection to server, handles automatic login,
 * routes messages to handlers, and manages reconnection.
 */

import {
  BaseMessage,
  RequestMessage,
  MessageType,
  ClientType,
  LoginRequestData,
  getLogger,
  ExceptionHandler,
} from '@nimbus/shared';
import type { AppContext } from '../AppContext';
import type { MessageHandler } from '../network/MessageHandler';

const logger = getLogger('NetworkService');

/**
 * Connection state
 */
export enum ConnectionState {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
}

/**
 * Pending request for request/response correlation
 */
interface PendingRequest {
  resolve: (data: any) => void;
  reject: (error: Error) => void;
  timeout: NodeJS.Timeout;
}

/**
 * Event listener
 */
type EventListener = (...args: any[]) => void;

/**
 * NetworkService - Manages WebSocket connection and message routing
 *
 * Features:
 * - WebSocket connection management
 * - Automatic login after connect
 * - Message ID generation
 * - Handler registration and message routing
 * - Request/response correlation
 * - Automatic reconnection with exponential backoff
 * - Ping/pong keep

alive handled by PingMessageHandler
 */
export class NetworkService {
  private ws?: WebSocket;
  private websocketUrl: string;
  private apiUrl: string;
  private connectionState: ConnectionState = ConnectionState.DISCONNECTED;

  private messageIdCounter: number = 0;
  private handlers: Map<MessageType, MessageHandler[]> = new Map();
  private pendingRequests: Map<string, PendingRequest> = new Map();
  private eventListeners: Map<string, EventListener[]> = new Map();

  private lastLoginMessage?: RequestMessage<LoginRequestData>;
  private reconnectAttempt: number = 0;
  private maxReconnectAttempts: number = 5;
  private shouldReconnect: boolean = true;

  constructor(private appContext: AppContext) {
    this.websocketUrl = appContext.config.websocketUrl;
    this.apiUrl = appContext.config.apiUrl;

    logger.info('NetworkService initialized', {
      websocketUrl: this.websocketUrl,
      apiUrl: this.apiUrl,
    });
  }

  /**
   * Connect to WebSocket server and automatically send login
   */
  async connect(): Promise<void> {
    if (this.connectionState !== ConnectionState.DISCONNECTED) {
      logger.warn('Already connected or connecting');
      return;
    }

    try {
      this.connectionState = ConnectionState.CONNECTING;
      logger.info('Connecting to WebSocket server', { url: this.websocketUrl });

      this.ws = new WebSocket(this.websocketUrl);

      this.ws.onopen = () => this.onOpen();
      this.ws.onmessage = (event) => this.onMessage(event);
      this.ws.onerror = (error) => this.onError(error);
      this.ws.onclose = () => this.onClose();

      // Wait for connection
      await new Promise<void>((resolve, reject) => {
        const timeout = setTimeout(() => {
          reject(new Error('Connection timeout'));
        }, 10000);

        this.once('connected', () => {
          clearTimeout(timeout);
          resolve();
        });

        this.once('error', (error) => {
          clearTimeout(timeout);
          reject(error);
        });
      });
    } catch (error) {
      this.connectionState = ConnectionState.DISCONNECTED;
      throw ExceptionHandler.handleAndRethrow(error, 'NetworkService.connect');
    }
  }

  /**
   * Disconnect from WebSocket server
   */
  async disconnect(): Promise<void> {
    this.shouldReconnect = false;

    if (this.ws) {
      this.ws.close();
      this.ws = undefined;
    }

    this.connectionState = ConnectionState.DISCONNECTED;
    logger.info('Disconnected from WebSocket server');
  }

  /**
   * Send a message to the server
   */
  send<T>(message: BaseMessage<T>): void {
    if (!this.isConnected()) {
      throw new Error('Not connected to server');
    }

    try {
      const json = JSON.stringify(message);
      this.ws!.send(json);

      logger.debug('Sent message', { type: message.t, id: message.i });
    } catch (error) {
      throw ExceptionHandler.handleAndRethrow(error, 'NetworkService.send', { message });
    }
  }

  /**
   * Send a request and wait for response
   */
  async request<T, R>(message: RequestMessage<T>): Promise<R> {
    if (!message.i) {
      throw new Error('Request message must have an ID');
    }

    return new Promise<R>((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingRequests.delete(message.i!);
        reject(new Error(`Request timeout: ${message.t}`));
      }, 30000); // 30 second timeout

      this.pendingRequests.set(message.i, {
        resolve,
        reject,
        timeout,
      });

      this.send(message);
    });
  }

  /**
   * Register a message handler
   */
  registerHandler(handler: MessageHandler): void {
    const handlers = this.handlers.get(handler.messageType) || [];
    handlers.push(handler);
    this.handlers.set(handler.messageType, handlers);

    logger.debug('Registered handler', { messageType: handler.messageType });
  }

  /**
   * Check if connected to server
   */
  isConnected(): boolean {
    return this.connectionState === ConnectionState.CONNECTED && this.ws?.readyState === WebSocket.OPEN;
  }

  /**
   * Get current connection state
   */
  getConnectionState(): ConnectionState {
    return this.connectionState;
  }

  /**
   * Generate unique message ID
   */
  generateMessageId(): string {
    return `msg_${Date.now()}_${++this.messageIdCounter}`;
  }

  /**
   * Add event listener
   */
  on(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event) || [];
    listeners.push(listener);
    this.eventListeners.set(event, listeners);
  }

  /**
   * Add one-time event listener
   */
  once(event: string, listener: EventListener): void {
    const onceListener = (...args: any[]) => {
      this.off(event, onceListener);
      listener(...args);
    };
    this.on(event, onceListener);
  }

  /**
   * Remove event listener
   */
  off(event: string, listener: EventListener): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      const index = listeners.indexOf(listener);
      if (index !== -1) {
        listeners.splice(index, 1);
      }
    }
  }

  /**
   * Emit event
   */
  emit(event: string, ...args: any[]): void {
    const listeners = this.eventListeners.get(event);
    if (listeners) {
      listeners.forEach(listener => {
        try {
          listener(...args);
        } catch (error) {
          ExceptionHandler.handle(error, 'NetworkService.emit', { event });
        }
      });
    }
  }

  /**
   * WebSocket opened
   */
  private onOpen(): void {
    const isReconnect = this.reconnectAttempt > 0;

    this.connectionState = ConnectionState.CONNECTED;
    this.reconnectAttempt = 0;

    if (isReconnect) {
      logger.info('Reconnected to WebSocket server');
      this.emit('reconnected');

      // Resend login
      if (this.lastLoginMessage) {
        logger.info('Resending login after reconnect');
        this.send(this.lastLoginMessage);
      }
    } else {
      logger.info('Connected to WebSocket server');
      this.emit('connected');

      // Send initial login
      this.sendLogin();
    }
  }

  /**
   * WebSocket message received
   */
  private onMessage(event: MessageEvent): void {
    try {
      const message: BaseMessage = JSON.parse(event.data);

      // Log ALL incoming messages (especially b.u for debugging)
      if (message.t === 'b.u') {
        logger.info('🔵 INCOMING WebSocket Message: b.u', {
          type: message.t,
          dataLength: message.d?.length,
          rawData: event.data,
        });
      } else {
        logger.debug('Received message', { type: message.t, responseId: message.r });
      }

      // Handle response to pending request
      if (message.r) {
        const pending = this.pendingRequests.get(message.r);
        if (pending) {
          clearTimeout(pending.timeout);
          this.pendingRequests.delete(message.r);
          pending.resolve(message.d);
          return;
        }
      }

      // Route to handlers
      const handlers = this.handlers.get(message.t);
      if (handlers && handlers.length > 0) {
        if (message.t === 'b.u') {
          logger.info(`🔵 Found ${handlers.length} handler(s) for b.u, routing...`);
        }
        handlers.forEach(handler => {
          try {
            // Support both sync and async handlers
            const result = handler.handle(message);
            if (result instanceof Promise) {
              result.catch(error => {
                ExceptionHandler.handle(error, 'NetworkService.onMessage.handler.async', {
                  messageType: message.t,
                });
              });
            }
          } catch (error) {
            ExceptionHandler.handle(error, 'NetworkService.onMessage.handler', {
              messageType: message.t,
            });
          }
        });
      } else {
        logger.warn('🔴 NO HANDLER registered for message type', {
          type: message.t,
          registeredTypes: Array.from(this.handlers.keys()),
        });
      }
    } catch (error) {
      ExceptionHandler.handle(error, 'NetworkService.onMessage', {
        data: event.data,
      });
    }
  }

  /**
   * WebSocket error
   */
  private onError(error: Event): void {
    logger.error('WebSocket error', {}, error as any);
    this.emit('error', error);
  }

  /**
   * WebSocket closed
   */
  private onClose(): void {
    const wasConnected = this.connectionState === ConnectionState.CONNECTED;

    this.connectionState = ConnectionState.DISCONNECTED;
    logger.info('WebSocket connection closed');

    this.emit('disconnected');

    // Attempt reconnection
    if (this.shouldReconnect && wasConnected) {
      this.attemptReconnect();
    }
  }

  /**
   * Attempt to reconnect with exponential backoff
   */
  private attemptReconnect(): void {
    if (this.reconnectAttempt >= this.maxReconnectAttempts) {
      logger.error('Max reconnection attempts reached');
      this.emit('reconnect_failed');
      return;
    }

    this.reconnectAttempt++;
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempt - 1), 16000);

    logger.info('Attempting reconnect', {
      attempt: this.reconnectAttempt,
      delay,
    });

    this.connectionState = ConnectionState.RECONNECTING;
    this.emit('reconnecting', this.reconnectAttempt);

    setTimeout(() => {
      if (this.shouldReconnect) {
        this.connect().catch(error => {
          ExceptionHandler.handle(error, 'NetworkService.attemptReconnect');
        });
      }
    }, delay);
  }

  /**
   * Send login message
   */
  private sendLogin(): void {
    const loginMessage: RequestMessage<LoginRequestData> = {
      i: this.generateMessageId(),
      t: MessageType.LOGIN,
      d: {
        username: this.appContext.config.username,
        password: this.appContext.config.password,
        worldId: this.appContext.config.worldId,
        clientType: ClientType.WEB,
      },
    };

    this.lastLoginMessage = loginMessage;
    this.send(loginMessage);

    logger.info('Sent login message', { username: loginMessage.d!.username, worldId: loginMessage.d!.worldId });
  }

  /**
   * Send block interaction to server
   *
   * @param x Block X position
   * @param y Block Y position
   * @param z Block Z position
   * @param action Action type ('click', 'collision')
   * @param params Action parameters
   * @param id Block ID from metadata (optional)
   * @param gId Block group ID (optional)
   */
  sendBlockInteraction(
    x: number,
    y: number,
    z: number,
    action: string = 'click',
    params?: Record<string, any>,
    id?: string,
    gId?: number
  ): void {
    const message: RequestMessage<any> = {
      i: this.generateMessageId(),
      t: MessageType.BLOCK_INTERACTION,
      d: {
        x,
        y,
        z,
        id,
        gId,
        ac: action,
        pa: params || {},
      },
    };

    this.send(message);

    logger.debug('Sent block interaction', {
      position: { x, y, z },
      id,
      gId,
      action,
      params,
    });
  }

  /**
   * Send entity interaction to server
   *
   * @param entityId Entity ID
   * @param action Action type (e.g., 'click', 'use', 'talk', 'fireShortcut')
   * @param clickType Mouse button number (0 = left, 1 = middle, 2 = right, etc.) - only for 'click' action
   * @param additionalParams Additional parameters to merge into pa
   */
  sendEntityInteraction(
    entityId: string,
    action: string = 'click',
    clickType?: number,
    additionalParams?: Record<string, any>
  ): void {
    const params: any = { ...additionalParams };
    if (action === 'click' && clickType !== undefined) {
      params.clickType = clickType;
    }

    const message: RequestMessage<any> = {
      i: this.generateMessageId(),
      t: MessageType.ENTITY_INTERACTION,
      d: {
        entityId,
        ts: Date.now(),
        ac: action,
        pa: params,
      },
    };

    this.send(message);

    logger.debug('Sent entity interaction', {
      entityId,
      action,
      clickType,
      params: additionalParams,
    });
  }

  /**
   * Get API URL for REST calls
   * @private Use specific URL methods instead (getAssetUrl, getEntityModelUrl, etc.)
   */
  private getApiUrl(): string {
    return this.apiUrl;
  }

  /**
   * Get WebSocket URL
   */
  getWebSocketUrl(): string {
    return this.websocketUrl;
  }

  /**
   * Get asset URL for a given asset path
   *
   * Constructs full URL using apiUrl and worldInfo.assetPath
   *
   * @param assetPath - Normalized asset path (e.g., "textures/block/basic/stone.png")
   * @returns Full asset URL
   */
  getAssetUrl(assetPath: string): string {
    const worldId = this.appContext.worldInfo?.worldId || 'main';
    const worldAssetPath = this.appContext.worldInfo?.assetPath || `/api/worlds/${worldId}/assets`;

    return `${this.apiUrl}${worldAssetPath}/${assetPath}`;
  }

  /**
   * Get entity model URL
   *
   * @param entityTypeId - Entity type ID
   * @returns Full entity model URL with cache-busting timestamp
   */
  getEntityModelUrl(entityTypeId: string): string {
    const worldId = this.appContext.worldInfo?.worldId || 'main';
    const timestamp = Date.now();
    return `${this.apiUrl}/api/worlds/${worldId}/entitymodel/${entityTypeId}?t=${timestamp}`;
  }

  /**
   * Get backdrop URL
   *
   * @param backdropTypeId - Backdrop type ID
   * @returns Full backdrop URL with cache-busting timestamp
   */
  getBackdropUrl(backdropTypeId: string): string {
    const timestamp = Date.now();
    return `${this.apiUrl}/api/backdrop/${backdropTypeId}?t=${timestamp}`;
  }

  /**
   * Get entity URL
   *
   * @param entityId - Entity ID
   * @returns Full entity URL with cache-busting timestamp
   */
  getEntityUrl(entityId: string): string {
    const worldId = this.appContext.worldInfo?.worldId || 'main';
    const timestamp = Date.now();
    return `${this.apiUrl}/api/worlds/${worldId}/entity/${entityId}?t=${timestamp}`;
  }

  /**
   * Get block types range URL
   *
   * @param from - Start of range
   * @param to - End of range
   * @returns Full block types range URL with cache-busting timestamp
   */
  getBlockTypesRangeUrl(from: number, to: number): string {
    const worldId = this.appContext.worldInfo?.worldId || 'main';
    const timestamp = Date.now();
    return `${this.apiUrl}/api/worlds/${worldId}/blocktypes/${from}/${to}?t=${timestamp}`;
  }

  /**
   * Get base editor URL from world configuration
   *
   * @returns Editor base URL or null if no editor URL configured
   */
  getComponentBaseUrl(): string | null {
    const url = this.appContext.worldInfo?.editorUrl;

    if (!url) {
      logger.warn('No editor URL configured for this world');
      return null;
    }

    return url;
  }

}
