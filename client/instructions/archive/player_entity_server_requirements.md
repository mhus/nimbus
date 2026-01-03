# Player Entity - Server Requirements

This document describes the server-side requirements for handling player entities and position updates.

## Overview

The client sends position updates for the player entity approximately every 100ms. The server must:
1. Receive and validate position updates from clients
2. Broadcast relevant player positions to other connected clients (based on registered chunks)
3. Provide player entity data via REST API for entity initialization

## 1. WebSocket Message Handler: Entity Position Update (Client → Server)

### Message Type
`e.p.u` (ENTITY_POSITION_UPDATE)

### Message Format (Client → Server)

```json
{
  "t": "e.p.u",
  "d": [
    {
      "pl": "player",
      "p": {
        "x": 100.5,
        "y": 65.0,
        "z": -200.5
      },
      "r": {
        "y": 90.0,
        "p": 0.0
      },
      "v": {
        "x": 0.0,
        "y": 0.0,
        "z": 0.0
      },
      "po": 5,
      "ts": 1697045600000,
      "ta": {
        "x": 100.5,
        "y": 65.0,
        "z": -200.5,
        "ts": 1697045800000
      }
    }
  ]
}
```

### Field Descriptions

- `pl` (string): Local entity ID ("player" for the player entity, not the unique @player_uuid)
- `p` (Vector3, optional): Current position in world coordinates
- `r` (Rotation, optional): Current rotation (yaw, pitch in degrees)
- `v` (Vector3, optional): Current velocity (blocks per second)
- `po` (number, optional): Current pose ID (see ENTITY_POSES enum)
- `ts` (number): Client timestamp when update was created
- `ta` (object, optional): Predicted target position for next 200ms
  - `x`, `y`, `z`: Target position coordinates
  - `ts`: Target arrival timestamp (ts + 200ms)

### Server Processing

1. **Receive update**: Parse incoming `e.p.u` message from WebSocket
2. **Validate data**: Check that position/rotation/velocity are within reasonable bounds
3. **Store player state**: Update player's current position/rotation/velocity/pose in server state
4. **Determine relevant clients**: Find all clients that have this player's chunks registered
5. **Broadcast update**: Send position updates to relevant clients (see below)

### Change Detection

The client only sends updates when:
- Position changed by > 0.01 blocks
- Rotation changed by > 0.5 degrees
- Velocity changed by > 0.01 blocks/s
- Pose changed

The server should respect this and not expect updates every 100ms when player is stationary.

## 2. Broadcasting Position Updates (Server → Clients)

### Message Type
`e.p` (ENTITY_CHUNK_PATHWAY)

### Timing
Server should broadcast player positions every ~100ms to clients who have registered the relevant chunks.

### Message Format (Server → Client)

```json
{
  "t": "e.p",
  "d": [
    {
      "entityId": "@a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "startAt": 1697045600000,
      "waypoints": [
        {
          "timestamp": 1697045600000,
          "target": {
            "x": 100.5,
            "y": 65.0,
            "z": -200.5
          },
          "rotation": {
            "y": 90.0,
            "p": 0.0
          },
          "pose": 5
        }
      ]
    }
  ]
}
```

### Field Descriptions

- `entityId` (string): Unique player entity ID (format: `@{uuid}`)
- `startAt` (number): Server timestamp when pathway starts
- `waypoints` (array): Array of waypoints for interpolation
  - `timestamp`: When this waypoint should be reached
  - `target`: Position coordinates
  - `rotation`: Yaw and pitch in degrees
  - `pose`: Pose ID (IDLE, WALK, RUN, JUMP, etc.)

### Broadcasting Strategy

1. **Every 100ms**: Run broadcast tick
2. **For each connected client**:
   - Get list of registered chunks for that client
   - Find all players whose position is in those chunks
   - Send entity pathways for those players
3. **Use predicted position**: Use the `ta` (target arrival) from client update for smooth interpolation
4. **Batching**: Send all player updates in a single `e.p` message (array of pathways)

### Chunk Registration

Clients send `c.r` (chunk register) messages with their registered chunks:

```json
{
  "t": "c.r",
  "d": {
    "c": [
      {"x": 0, "z": 0},
      {"x": 1, "z": 0},
      {"x": 0, "z": 1}
    ]
  }
}
```

Server must track registered chunks per client to determine which player updates to send.

## 3. REST API: Get Player Entity

### Endpoint
`GET /api/worlds/{worldId}/entity/{entityId}`

### Purpose
Clients request player entity data to initialize ClientEntity objects for rendering.

### Request Parameters

- `worldId` (path): World ID
- `entityId` (path): Player entity ID (format: `@{uuid}`)

### Response Format

```json
{
  "id": "@testuser_session123abc",
  "name": "Player Display Name",
  "model": "farmer1",
  "modelModifier": {},
  "movementType": "dynamic",
  "solid": false,
  "interactive": false,
  "clientPhysics": false,
  "position": {
    "x": 0,
    "y": 64,
    "z": 0
  },
  "rotation": {
    "y": 0,
    "p": 0
  },
  "pose": 0
}
```

### Field Descriptions

- `id` (string): Player entity ID (must match `entityId` from request)
- `name` (string): Player display name (from PlayerInfo)
- `model` (string): Entity model ID (e.g., "farmer1")
- `modelModifier` (object): Optional model modifiers (color, scale, etc.)
- `movementType` (string): "dynamic" (moves via pathways)
- `solid` (boolean): false (players don't block other entities)
- `interactive` (boolean): false (no click interactions on players)
- `clientPhysics` (boolean): false (client-side physics disabled - player uses server pathways)
- `position` (Vector3): Current position (can be approximate, updated via pathways)
- `rotation` (Rotation): Current rotation (yaw, pitch)
- `pose` (number): Current pose ID (0 = IDLE by default)

### Error Responses

**404 Not Found**: Entity ID not found or player not connected
```json
{
  "error": "Entity not found",
  "entityId": "@a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

**400 Bad Request**: Invalid entity ID format
```json
{
  "error": "Invalid entity ID format",
  "entityId": "invalid-id"
}
```

## 4. Player Entity ID Format

Player entity IDs **must** start with `@` to distinguish them from regular entities:

- Format: `@{username}_{sessionId}`
- Example: `@testuser_session123abc`
- Generated after login from username and sessionId (not UUID)
- Username comes from login request (userId field)
- SessionId comes from login response

## 5. Pose IDs (ENTITY_POSES)

```typescript
enum ENTITY_POSES {
  IDLE = 0,
  WALK = 1,
  RUN = 2,
  JUMP = 3,
  CROUCH = 4,
  SWIM = 5,
  CLIMB = 6,
  FLY = 7,
  SIT = 8,
  ATTACK = 9,
  DEFEND = 10,
  DIE = 11,
  CUSTOM1 = 20,
  CUSTOM2 = 21,
  CUSTOM3 = 22,
  CUSTOM4 = 23,
  CUSTOM5 = 24
}
```

## 6. Implementation Checklist

### Server WebSocket Handler
- [ ] Register handler for `e.p.u` message type
- [ ] Parse and validate position update data
- [ ] Update player state in server memory
- [ ] Track player's current chunk position

### Server Broadcast System
- [ ] Create 100ms interval timer for broadcasts
- [ ] Maintain map of client → registered chunks
- [ ] Maintain map of chunk → players in that chunk
- [ ] Generate pathways from stored player states
- [ ] Send `e.p` messages to relevant clients

### Server REST API
- [ ] Implement `GET /api/worlds/{worldId}/entity/{entityId}` endpoint
- [ ] Validate entity ID format (must start with `@`)
- [ ] Look up player by entity ID
- [ ] Return player entity JSON (see template in `files/entity/player_entity.json`)
- [ ] Handle 404 for disconnected/unknown players

### Testing
- [ ] Test position updates from multiple clients
- [ ] Verify broadcasting only to clients with registered chunks
- [ ] Test REST endpoint with valid/invalid entity IDs
- [ ] Verify smooth interpolation of player movement
- [ ] Test with players in different chunks

## 7. Performance Considerations

- **Batching**: Send all player updates in single message per client
- **Change detection**: Don't broadcast if player hasn't moved
- **Chunk filtering**: Only send updates for players in registered chunks
- **Interpolation**: Use client's predicted position (ta) for smoother movement
- **Rate limiting**: Cap update rate at 100ms (10 updates/second per client)

## 8. Template Files

### Player Entity Template
See: `files/entity/player_entity.json`

This template should be used by the REST API endpoint. Replace:
- `@PLAYER_ID` → actual player entity ID (format: `@{uuid}`)
- `PLAYER_NAME` → player display name from PlayerInfo
- Position/rotation → current player state

## References

- Network Protocol: `client/instructions/general/network-model-2.0.md`
- Entity Position Update: Line 264-299 in network-model-2.0.md
- Entity Pathway: Line 251-263 in network-model-2.0.md
- Client Implementation: `client/packages/engine/src/services/PlayerService.ts`
