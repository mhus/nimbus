/**
 * WebSocket Client (migrated from tmp/voxelsrv/src/socket.ts)
 */

export type MessageHandler = (data: any) => void;

/**
 * WebSocket client for multiplayer connection
 */
export class WebSocketClient {
  private socket?: WebSocket;
  private listeners: { [type: string]: MessageHandler[] } = {};
  private connected = false;
  public server: string = '';

  constructor() {}

  /**
   * Connect to server (with automatic retry on timeout)
   */
  async connect(serverUrl: string, retryCount: number = 0): Promise<void> {
    const maxRetries = 1; // Retry once if first attempt times out

    return new Promise((resolve, reject) => {
      const attemptNumber = retryCount + 1;
      console.log(`[WebSocket] 🔍 [1] Connection attempt ${attemptNumber}/${maxRetries + 1} to`, serverUrl, 'at', new Date().toISOString());
      this.server = serverUrl;
      this.socket = new WebSocket(serverUrl);
      console.log('[WebSocket] 🔍 [2] WebSocket object created, readyState:', this.socket.readyState);

      this.socket.binaryType = 'arraybuffer';

      let resolved = false;
      let rejected = false;

      // Timeout after 10 seconds
      console.log('[WebSocket] 🔍 [3] Setting 10 second timeout at', new Date().toISOString());
      const timeoutId = setTimeout(async () => {
        console.log('[WebSocket] 🔍 [TIMEOUT] Timeout fired at', new Date().toISOString());
        console.log('[WebSocket] 🔍 [TIMEOUT] this.connected =', this.connected);
        console.log('[WebSocket] 🔍 [TIMEOUT] resolved=', resolved, 'rejected=', rejected);
        console.log('[WebSocket] 🔍 [TIMEOUT] socket readyState:', this.socket?.readyState);

        if (!this.connected && !resolved && !rejected) {
          // Mark this attempt as done first to prevent race conditions
          rejected = true;

          // Close the stuck socket
          if (this.socket) {
            this.socket.close();
          }

          // Retry if this is the first attempt
          if (retryCount < maxRetries) {
            console.log(`[WebSocket] 🔍 [TIMEOUT] Retrying connection in 1 second (attempt ${attemptNumber}/${maxRetries + 1} timed out)...`);

            // Wait 1 second before retrying to let browser/network stack reset
            setTimeout(async () => {
              try {
                await this.connect(serverUrl, retryCount + 1);
                resolve(); // Forward success from retry
              } catch (error) {
                reject(error); // Forward failure from retry
              }
            }, 1000);
          } else {
            console.log('[WebSocket] 🔍 [TIMEOUT] All retry attempts exhausted, rejecting...');
            reject(new Error('Connection timeout after retries'));
          }
        } else {
          console.log('[WebSocket] 🔍 [TIMEOUT] Already connected or settled, ignoring timeout');
        }
      }, 10000);

      this.socket.onopen = () => {
        console.log('[WebSocket] 🔍 [4] onopen fired at', new Date().toISOString(), 'readyState:', this.socket?.readyState);
        console.log('[WebSocket] 🔍 [4a] Before setting connected, this.connected =', this.connected);
        this.connected = true;
        console.log('[WebSocket] 🔍 [4b] After setting connected, this.connected =', this.connected);

        // Clear timeout since connection succeeded
        clearTimeout(timeoutId);
        console.log('[WebSocket] 🔍 [4c] Timeout cleared, setting 50ms delay before resolve...');

        setTimeout(() => {
          console.log('[WebSocket] 🔍 [5] 50ms delay complete, emitting connection event');
          this.emit('connection', {});
          console.log('[WebSocket] 🔍 [6] About to resolve promise, resolved=', resolved, 'rejected=', rejected);
          if (!resolved && !rejected) {
            resolved = true;
            resolve();
            console.log('[WebSocket] 🔍 [7] Promise resolved successfully');
          } else {
            console.log('[WebSocket] 🔍 [7] Promise already settled! resolved=', resolved, 'rejected=', rejected);
          }
        }, 50);
      };

      this.socket.onerror = (error) => {
        console.error('[WebSocket] 🔍 [ERROR] onerror fired at', new Date().toISOString(), 'readyState:', this.socket?.readyState);
        console.error('[WebSocket] 🔍 [ERROR] Error details:', error);
        console.error('[WebSocket] 🔍 [ERROR] resolved=', resolved, 'rejected=', rejected, 'connected=', this.connected);

        // Clear timeout since connection failed
        clearTimeout(timeoutId);
        console.error('[WebSocket] 🔍 [ERROR] Timeout cleared');

        setTimeout(() => {
          this.emit('PlayerKick', { reason: `Can't connect to ${serverUrl}` });
          if (!resolved && !rejected) {
            rejected = true;
            reject(error);
            console.error('[WebSocket] 🔍 [ERROR] Promise rejected');
          } else {
            console.error('[WebSocket] 🔍 [ERROR] Promise already settled!');
          }
        }, 500);
      };

      this.socket.onclose = () => {
        console.log('[WebSocket] 🔍 [CLOSE] onclose fired at', new Date().toISOString(), 'readyState:', this.socket?.readyState);
        console.log('[WebSocket] 🔍 [CLOSE] resolved=', resolved, 'rejected=', rejected, 'connected=', this.connected);
        this.connected = false;

        // Clear timeout on close
        clearTimeout(timeoutId);
        console.log('[WebSocket] 🔍 [CLOSE] Timeout cleared');

        setTimeout(() => {
          this.emit('PlayerKick', { reason: 'Connection closed!' });
        }, 500);
      };

      this.socket.onmessage = async (event) => {
        try {
          // For now, use JSON (will upgrade to protobuf later)
          const message = JSON.parse(event.data);
          if (message && message.type) {
            this.emit(message.type, message);
          }
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error);
        }
      };
    });
  }

  /**
   * Send message to server
   */
  async send(type: string, data: any = {}): Promise<void> {
    if (!this.socket || !this.connected) {
      console.warn('[WebSocket] Cannot send - not connected');
      return;
    }

    const message = { type, ...data };
    this.socket.send(JSON.stringify(message));
  }

  /**
   * Register event handler
   */
  on(type: string, handler: MessageHandler): void {
    if (!this.listeners[type]) {
      this.listeners[type] = [];
    }
    this.listeners[type].push(handler);
  }

  /**
   * Emit event to handlers
   */
  private emit(type: string, data: any): void {
    if (this.listeners[type]) {
      this.listeners[type].forEach((handler) => {
        handler(data);
      });
    }
  }

  /**
   * Close connection
   */
  close(): void {
    if (this.socket) {
      this.socket.close();
      this.socket = undefined;
    }
    this.connected = false;
    this.listeners = {};
  }

  /**
   * Check if connected
   */
  isConnected(): boolean {
    return this.connected;
  }
}
