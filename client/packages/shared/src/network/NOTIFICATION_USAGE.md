# Notification System Usage

Server notifications with numeric type codes for network efficiency.

## NotificationType Enum

```typescript
enum NotificationType {
  SYSTEM = 0,   // System messages
  CHAT = 1,     // Chat messages
  WARNING = 2,  // Warning messages
  ERROR = 3,    // Error messages
  INFO = 4,     // Info messages
}
```

## Message Structure

```typescript
interface NotificationData {
  t: NotificationType;  // Numeric type (0-4)
  f?: string;           // From (optional, for chat)
  m: string;            // Message text
  ts: number;           // UTC timestamp
}
```

## Usage Examples

### 1. System Notification

```typescript
// Server sends system message
const systemNotification: NotificationMessage = {
  t: MessageType.NOTIFICATION,
  d: {
    t: NotificationType.SYSTEM,
    m: 'Welcome to the server!',
    ts: Date.now()
  }
};

ws.send(JSON.stringify(systemNotification));
```

**Network (JSON):**
```json
{"t": "n", "d": {"t": 0, "m": "Welcome to the server!", "ts": 1234567890}}
```

### 2. Chat Message

```typescript
// Server sends chat message
const chatNotification: NotificationMessage = {
  t: MessageType.NOTIFICATION,
  d: {
    t: NotificationType.CHAT,
    f: 'PlayerName',  // Who sent the message
    m: 'Hello everyone!',
    ts: Date.now()
  }
};
```

**Network (JSON):**
```json
{"t": "n", "d": {"t": 1, "f": "PlayerName", "m": "Hello everyone!", "ts": 1234567890}}
```

### 3. Warning Message

```typescript
const warningNotification: NotificationMessage = {
  t: MessageType.NOTIFICATION,
  d: {
    t: NotificationType.WARNING,
    m: 'Server will restart in 5 minutes',
    ts: Date.now()
  }
};
```

**Network (JSON):**
```json
{"t": "n", "d": {"t": 2, "m": "Server will restart in 5 minutes", "ts": 1234567890}}
```

### 4. Error Message

```typescript
const errorNotification: NotificationMessage = {
  t: MessageType.NOTIFICATION,
  d: {
    t: NotificationType.ERROR,
    m: 'Failed to load chunk data',
    ts: Date.now()
  }
};
```

**Network (JSON):**
```json
{"t": "n", "d": {"t": 3, "m": "Failed to load chunk data", "ts": 1234567890}}
```

### 5. Info Message

```typescript
const infoNotification: NotificationMessage = {
  t: MessageType.NOTIFICATION,
  d: {
    t: NotificationType.INFO,
    m: 'New player joined the world',
    ts: Date.now()
  }
};
```

**Network (JSON):**
```json
{"t": "n", "d": {"t": 4, "m": "New player joined the world", "ts": 1234567890}}
```

## Client-Side Handling

### Basic Handler

```typescript
import { NotificationType, getNotificationTypeName } from '@nimbus/shared';

function onNotification(notification: NotificationData) {
  const typeName = getNotificationTypeName(notification.t);
  const timestamp = new Date(notification.ts);

  console.log(`[${typeName}] ${notification.m}`);

  // Display based on type
  switch (notification.t) {
    case NotificationType.SYSTEM:
      showSystemMessage(notification.m);
      break;

    case NotificationType.CHAT:
      showChatMessage(notification.f ?? 'Unknown', notification.m);
      break;

    case NotificationType.WARNING:
      showWarningMessage(notification.m);
      break;

    case NotificationType.ERROR:
      showErrorMessage(notification.m);
      break;

    case NotificationType.INFO:
      showInfoMessage(notification.m);
      break;
  }
}
```

### UI Display with Styling

```typescript
function displayNotification(notification: NotificationData) {
  const typeName = getNotificationTypeName(notification.t);

  // Get color/style based on type
  const config = {
    [NotificationType.SYSTEM]: { color: '#888', icon: 'ℹ️' },
    [NotificationType.CHAT]: { color: '#fff', icon: '💬' },
    [NotificationType.WARNING]: { color: '#ff9', icon: '⚠️' },
    [NotificationType.ERROR]: { color: '#f44', icon: '❌' },
    [NotificationType.INFO]: { color: '#4af', icon: 'ℹ️' },
  }[notification.t];

  // Show in UI
  const element = document.createElement('div');
  element.className = `notification notification-${typeName}`;
  element.style.color = config.color;
  element.innerHTML = `
    <span class="icon">${config.icon}</span>
    ${notification.f ? `<strong>${notification.f}:</strong> ` : ''}
    <span class="message">${notification.m}</span>
  `;

  document.getElementById('notifications').appendChild(element);

  // Auto-remove after 5 seconds
  setTimeout(() => element.remove(), 5000);
}
```

### Notification Queue

```typescript
class NotificationQueue {
  private queue: NotificationData[] = [];
  private maxSize = 100;

  add(notification: NotificationData) {
    this.queue.push(notification);

    // Limit queue size
    if (this.queue.length > this.maxSize) {
      this.queue.shift();
    }

    this.display(notification);
  }

  getRecent(count = 10): NotificationData[] {
    return this.queue.slice(-count);
  }

  getByType(type: NotificationType): NotificationData[] {
    return this.queue.filter(n => n.t === type);
  }

  clear() {
    this.queue = [];
  }

  private display(notification: NotificationData) {
    displayNotification(notification);
  }
}
```

## Server-Side Broadcasting

### Broadcast to All Players

```typescript
function broadcastNotification(
  type: NotificationType,
  message: string,
  from?: string
) {
  const notification: NotificationMessage = {
    t: MessageType.NOTIFICATION,
    d: {
      t: type,
      f: from,
      m: message,
      ts: Date.now()
    }
  };

  // Send to all connected players
  players.forEach(player => {
    player.send(JSON.stringify(notification));
  });
}

// Usage
broadcastNotification(NotificationType.SYSTEM, 'Server starting...');
broadcastNotification(NotificationType.CHAT, 'Hello!', 'AdminBot');
```

### Targeted Notifications

```typescript
function sendNotificationToPlayer(
  playerId: string,
  type: NotificationType,
  message: string
) {
  const player = players.get(playerId);
  if (!player) return;

  const notification: NotificationMessage = {
    t: MessageType.NOTIFICATION,
    d: {
      t: type,
      m: message,
      ts: Date.now()
    }
  };

  player.send(JSON.stringify(notification));
}

// Usage
sendNotificationToPlayer(
  'player123',
  NotificationType.WARNING,
  'You are entering a dangerous area'
);
```

## Type Name Helper

The `getNotificationTypeName()` helper converts numeric type to string:

```typescript
import { getNotificationTypeName, NotificationType } from '@nimbus/shared';

console.log(getNotificationTypeName(NotificationType.SYSTEM));   // 'system'
console.log(getNotificationTypeName(NotificationType.CHAT));     // 'chat'
console.log(getNotificationTypeName(NotificationType.WARNING));  // 'warning'
console.log(getNotificationTypeName(NotificationType.ERROR));    // 'error'
console.log(getNotificationTypeName(NotificationType.INFO));     // 'info'
console.log(getNotificationTypeName(99));                        // 'unknown'
```

## Chat System Example

```typescript
class ChatSystem {
  sendChatMessage(message: string, playerId: string) {
    const player = players.get(playerId);
    if (!player) return;

    // Broadcast to all players
    broadcastNotification(
      NotificationType.CHAT,
      message,
      player.displayName
    );
  }

  sendPrivateMessage(message: string, from: string, to: string) {
    sendNotificationToPlayer(to, NotificationType.CHAT, message);
  }

  sendSystemMessage(message: string) {
    broadcastNotification(NotificationType.SYSTEM, message);
  }
}

// Usage
chatSystem.sendChatMessage('Hi everyone!', 'player123');
chatSystem.sendSystemMessage('World saved successfully');
```

## Network Efficiency

Using numeric types instead of strings:

**String-based (inefficient):**
```json
{"t": "n", "d": {"t": "system", "m": "Hello", "ts": 1234567890}}
```
Size: ~61 bytes

**Numeric-based (efficient):**
```json
{"t": "n", "d": {"t": 0, "m": "Hello", "ts": 1234567890}}
```
Size: ~54 bytes

**Savings:** ~12% reduction per message

For 1000 notifications: ~7 KB saved

## Best Practices

### ✅ DO
- Use NotificationType enum (not magic numbers)
- Include timestamp for all notifications
- Set 'from' field for chat messages
- Use appropriate type for message severity
- Use helper function `getNotificationTypeName()` for display

### ❌ DON'T
- Don't use magic numbers directly (`t: 0`)
- Don't forget timestamp
- Don't mix notification types
- Don't send sensitive data in notifications
- Don't spam notifications (rate limit)

## Examples by Type

### System Messages
```typescript
// Server start/stop
NotificationType.SYSTEM: "Server starting..."
NotificationType.SYSTEM: "World saved"
NotificationType.SYSTEM: "Maintenance in 10 minutes"
```

### Chat Messages
```typescript
// Always include 'from' field
NotificationType.CHAT: "Hello!" (from: "PlayerName")
NotificationType.CHAT: "GG" (from: "Player2")
```

### Warnings
```typescript
NotificationType.WARNING: "Low health!"
NotificationType.WARNING: "PvP zone ahead"
NotificationType.WARNING: "Connection unstable"
```

### Errors
```typescript
NotificationType.ERROR: "Failed to save world"
NotificationType.ERROR: "Connection lost"
NotificationType.ERROR: "Invalid command"
```

### Info
```typescript
NotificationType.INFO: "Player joined"
NotificationType.INFO: "Achievement unlocked"
NotificationType.INFO: "Quest completed"
```

## Summary

- **Numeric types** (0-4) for network efficiency
- **Helper function** for type-to-name conversion
- **Type-safe** with TypeScript enum
- **Flexible** 'from' field for chat
- **Timestamped** for chronological ordering
