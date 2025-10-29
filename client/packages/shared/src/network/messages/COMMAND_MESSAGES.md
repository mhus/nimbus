# Command Messages

This document describes the command message system for bidirectional command execution between client and server.

## Overview

The command system allows the client to send commands to the server for execution (e.g., console commands, editor commands) and receive feedback and results from the server.

## Message Types

Three message types are defined for command execution:

1. **`cmd`** - Command execution request (Client → Server)
2. **`cmd.msg`** - Command messages during execution (Server → Client)
3. **`cmd.rs`** - Command result/status (Server → Client)

## Message Flow

```
Client                                  Server
  |                                      |
  |  CMD (command request)               |
  |------------------------------------->|
  |                                      | (executing...)
  |  CMD_MESSAGE (status update)         |
  |<-------------------------------------|
  |                                      |
  |  CMD_MESSAGE (progress info)         |
  |<-------------------------------------|
  |                                      |
  |  CMD_RESULT (final result)           |
  |<-------------------------------------|
  |                                      |
```

## Data Structures

### CommandData (CMD)

Command execution request from client to server.

```typescript
interface CommandData {
  /** Command string to execute */
  cmd: string;

  /** Command arguments */
  args?: string[];

  /** Additional context data */
  ctx?: Record<string, any>;
}
```

**Example:**
```typescript
const message: CommandMessage = {
  i: 'cmd-123',
  t: MessageType.CMD,
  d: {
    cmd: 'setblock',
    args: ['10', '64', '20', 'stone'],
    ctx: {
      playerId: 'player-1',
    },
  },
};
```

### CommandMessageData (CMD_MESSAGE)

Informational messages sent by server during command execution.

```typescript
interface CommandMessageData {
  /** Message text */
  msg: string;

  /** Message severity */
  severity?: CommandSeverity;

  /** Additional data */
  data?: any;
}

enum CommandSeverity {
  INFO = 'info',
  SUCCESS = 'success',
  WARNING = 'warning',
  ERROR = 'error',
}
```

**Example:**
```typescript
const message: CommandMessageMessage = {
  r: 'cmd-123',  // References command request
  t: MessageType.CMD_MESSAGE,
  d: {
    msg: 'Setting block at (10, 64, 20)...',
    severity: CommandSeverity.INFO,
  },
};
```

### CommandResultData (CMD_RESULT)

Final result of command execution sent by server.

```typescript
interface CommandResultData {
  /** Execution status */
  status: CommandStatus;

  /** Result message */
  msg?: string;

  /** Result data */
  result?: any;

  /** Error information (if status is ERROR) */
  error?: {
    message: string;
    code?: string;
    details?: any;
  };

  /** Execution time in milliseconds */
  executionTime?: number;
}

enum CommandStatus {
  PENDING = 'pending',
  EXECUTING = 'executing',
  SUCCESS = 'success',
  ERROR = 'error',
  CANCELLED = 'cancelled',
}
```

**Example (Success):**
```typescript
const message: CommandResultMessage = {
  r: 'cmd-123',  // References command request
  t: MessageType.CMD_RESULT,
  d: {
    status: CommandStatus.SUCCESS,
    msg: 'Block set successfully',
    result: {
      blockId: 1,
      position: { x: 10, y: 64, z: 20 },
    },
    executionTime: 42,
  },
};
```

**Example (Error):**
```typescript
const message: CommandResultMessage = {
  r: 'cmd-123',  // References command request
  t: MessageType.CMD_RESULT,
  d: {
    status: CommandStatus.ERROR,
    msg: 'Failed to set block',
    error: {
      message: 'Invalid block type',
      code: 'INVALID_BLOCK_TYPE',
      details: { blockType: 'invalid_block' },
    },
    executionTime: 10,
  },
};
```

## Usage Examples

### Client: Send Command

```typescript
import { MessageType } from '@nimbus/shared';
import type { CommandMessage, CommandData } from '@nimbus/shared';

// Create command message
const commandData: CommandData = {
  cmd: 'tp',
  args: ['player1', '100', '64', '200'],
  ctx: {
    source: 'console',
  },
};

const message: CommandMessage = {
  i: generateMessageId(),
  t: MessageType.CMD,
  d: commandData,
};

// Send to server
networkService.send(message);
```

### Server: Handle Command

```typescript
import { MessageType, CommandStatus, CommandSeverity } from '@nimbus/shared';
import type { CommandMessage, CommandMessageMessage, CommandResultMessage } from '@nimbus/shared';

// Handle command message
function handleCommand(message: CommandMessage) {
  const { cmd, args, ctx } = message.d;
  const requestId = message.i!;

  // Send progress message
  sendCommandMessage(requestId, 'Executing teleport...', CommandSeverity.INFO);

  try {
    // Execute command
    const result = executeCommand(cmd, args, ctx);

    // Send success result
    sendCommandResult(requestId, {
      status: CommandStatus.SUCCESS,
      msg: 'Command executed successfully',
      result,
      executionTime: Date.now() - startTime,
    });
  } catch (error) {
    // Send error result
    sendCommandResult(requestId, {
      status: CommandStatus.ERROR,
      msg: 'Command execution failed',
      error: {
        message: error.message,
        code: error.code,
      },
      executionTime: Date.now() - startTime,
    });
  }
}

function sendCommandMessage(requestId: string, msg: string, severity: CommandSeverity) {
  const message: CommandMessageMessage = {
    r: requestId,
    t: MessageType.CMD_MESSAGE,
    d: { msg, severity },
  };
  connection.send(message);
}

function sendCommandResult(requestId: string, data: CommandResultData) {
  const message: CommandResultMessage = {
    r: requestId,
    t: MessageType.CMD_RESULT,
    d: data,
  };
  connection.send(message);
}
```

### Client: Handle Command Messages and Results

```typescript
import { MessageType } from '@nimbus/shared';
import type { CommandMessageMessage, CommandResultMessage } from '@nimbus/shared';

// Register handlers
networkService.on(MessageType.CMD_MESSAGE, handleCommandMessage);
networkService.on(MessageType.CMD_RESULT, handleCommandResult);

function handleCommandMessage(message: CommandMessageMessage) {
  const { msg, severity, data } = message.d;

  // Display message to user
  console.log(`[${severity}] ${msg}`, data);

  // Update UI with progress
  updateCommandProgress(message.r!, msg, severity);
}

function handleCommandResult(message: CommandResultMessage) {
  const { status, msg, result, error, executionTime } = message.d;

  if (status === CommandStatus.SUCCESS) {
    console.log(`✓ ${msg}`, result);
    console.log(`Execution time: ${executionTime}ms`);
  } else if (status === CommandStatus.ERROR) {
    console.error(`✗ ${msg}`, error);
  }

  // Complete command execution
  completeCommand(message.r!, status, result || error);
}
```

## Command Types

### Console Commands

Example commands that can be executed:

- **Block Manipulation:**
  - `setblock <x> <y> <z> <blockType>` - Set block at position
  - `fill <x1> <y1> <z1> <x2> <y2> <z2> <blockType>` - Fill region with blocks
  - `replace <x1> <y1> <z1> <x2> <y2> <z2> <oldBlock> <newBlock>` - Replace blocks

- **Player Commands:**
  - `tp <player> <x> <y> <z>` - Teleport player
  - `give <player> <item> [count]` - Give item to player
  - `gamemode <mode> [player]` - Change game mode

- **World Commands:**
  - `time set <value>` - Set world time
  - `weather <type>` - Change weather
  - `save` - Save world

- **Editor Commands:**
  - `select <x1> <y1> <z1> <x2> <y2> <z2>` - Select region
  - `copy` - Copy selection
  - `paste` - Paste clipboard
  - `undo` - Undo last operation
  - `redo` - Redo last undone operation

## Best Practices

1. **Always provide message IDs** - Use `i` field in command requests for proper correlation
2. **Send progress updates** - Use CMD_MESSAGE for long-running commands
3. **Include execution time** - Helps with performance monitoring
4. **Use appropriate severity levels** - INFO for progress, WARNING for issues, ERROR for failures
5. **Provide detailed error information** - Include error code and details for debugging
6. **Include context** - Use `ctx` field to pass relevant context (player, editor state, etc.)

## Security Considerations

1. **Validate commands** - Server must validate command names and arguments
2. **Check permissions** - Verify user has permission to execute command
3. **Sanitize input** - Prevent command injection attacks
4. **Rate limiting** - Prevent command spam
5. **Audit logging** - Log all command executions for security audit

## Performance

- **Network efficiency**: Short message type names (`cmd`, `cmd.msg`, `cmd.rs`) reduce bandwidth
- **Async execution**: Commands execute asynchronously without blocking
- **Progress updates**: Optional progress messages for user feedback
- **Execution time tracking**: Monitor command performance

## Error Handling

All errors should be reported via CMD_RESULT with status ERROR:

```typescript
{
  status: CommandStatus.ERROR,
  msg: 'Human-readable error message',
  error: {
    message: 'Detailed error message',
    code: 'ERROR_CODE',  // e.g., 'INVALID_ARGS', 'PERMISSION_DENIED'
    details: { /* additional context */ }
  }
}
```

Common error codes:
- `INVALID_COMMAND` - Unknown command
- `INVALID_ARGS` - Invalid arguments
- `PERMISSION_DENIED` - User lacks permission
- `EXECUTION_FAILED` - Command execution failed
- `TIMEOUT` - Command execution timed out
- `CANCELLED` - Command was cancelled

## Testing

Example test cases:

```typescript
describe('CommandMessage', () => {
  it('should validate command data', () => {
    const data: CommandData = {
      cmd: 'test',
      args: ['arg1', 'arg2'],
    };
    expect(validateCommandData(data)).toBe(true);
  });

  it('should handle command execution flow', async () => {
    const commandId = 'cmd-123';

    // Send command
    await sendCommand({ cmd: 'test', args: [] });

    // Expect progress message
    const progressMsg = await waitForMessage(MessageType.CMD_MESSAGE, commandId);
    expect(progressMsg.d.severity).toBe(CommandSeverity.INFO);

    // Expect result
    const result = await waitForMessage(MessageType.CMD_RESULT, commandId);
    expect(result.d.status).toBe(CommandStatus.SUCCESS);
  });
});
```

## Future Enhancements

Possible future additions:

1. **Command history** - Track executed commands
2. **Command aliases** - Support command shortcuts
3. **Auto-completion** - Suggest commands and arguments
4. **Batch execution** - Execute multiple commands in sequence
5. **Scheduled commands** - Schedule commands for later execution
6. **Command macros** - Define reusable command sequences
