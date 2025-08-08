# Registry Service API Examples

This document contains curl examples for all Registry Service endpoints.

## Prerequisites

All endpoints require authentication via JWT token. First, obtain a token from the Identity Service:

```bash
# Login to get JWT token
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "id": "admin",
    "password": "admin123"
  }'

# Response will contain the token
export JWT_TOKEN="your_jwt_token_here"
```

## Create World

Creates a new world. Requires CREATOR role.

```bash
curl -X POST http://localhost:8082/worlds \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "My Test World",
    "description": "A beautiful test world for development",
    "accessUrl": "ws://localhost:8090/world/ws",
    "properties": {
      "theme": "fantasy",
      "maxPlayers": "100",
      "difficulty": "easy"
    }
  }'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Test World",
  "description": "A beautiful test world for development",
  "createdAt": 1672531200000,
  "updatedAt": 1672531200000,
  "ownerId": "admin",
  "enabled": true,
  "accessUrl": "ws://localhost:8090/world/ws",
  "properties": {
    "theme": "fantasy",
    "maxPlayers": "100",
    "difficulty": "easy"
  }
}
```

## Get World by ID

Retrieves a specific world by its ID. Requires USER role.

```bash
curl -X GET http://localhost:8082/worlds/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Test World",
  "description": "A beautiful test world for development",
  "createdAt": 1672531200000,
  "updatedAt": 1672531200000,
  "ownerId": "admin",
  "enabled": true,
  "accessUrl": "ws://localhost:8090/world/ws",
  "properties": {
    "theme": "fantasy",
    "maxPlayers": "100",
    "difficulty": "easy"
  }
}
```

## List Worlds

Lists all worlds with optional filtering and pagination. Requires USER role.

### Basic listing

```bash
curl -X GET http://localhost:8082/worlds \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### With pagination

```bash
curl -X GET "http://localhost:8082/worlds?page=0&size=10" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

### With filters

```bash
# Filter by name
curl -X GET "http://localhost:8082/worlds?name=test" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Filter by owner
curl -X GET "http://localhost:8082/worlds?ownerId=admin" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Filter by enabled status
curl -X GET "http://localhost:8082/worlds?enabled=true" \
  -H "Authorization: Bearer $JWT_TOKEN"

# Combined filters
curl -X GET "http://localhost:8082/worlds?name=test&enabled=true&page=0&size=5" \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Response:
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "My Test World",
      "description": "A beautiful test world for development",
      "createdAt": 1672531200000,
      "updatedAt": 1672531200000,
      "ownerId": "admin",
      "enabled": true,
      "accessUrl": "ws://localhost:8090/world/ws",
      "properties": {
        "theme": "fantasy",
        "maxPlayers": "100",
        "difficulty": "easy"
      }
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false,
      "empty": false
    },
    "pageNumber": 0,
    "pageSize": 20,
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "numberOfElements": 1,
  "size": 20,
  "number": 0,
  "sort": {
    "sorted": true,
    "unsorted": false,
    "empty": false
  },
  "empty": false
}
```

## Update World

Updates an existing world. Requires ADMIN role or ownership of the world.

```bash
curl -X PUT http://localhost:8082/worlds/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "Updated Test World",
    "description": "An updated description for the test world",
    "accessUrl": "ws://localhost:8091/world/ws",
    "properties": {
      "theme": "sci-fi",
      "maxPlayers": "200",
      "difficulty": "hard",
      "version": "2.0"
    }
  }'
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Updated Test World",
  "description": "An updated description for the test world",
  "createdAt": 1672531200000,
  "updatedAt": 1672531800000,
  "ownerId": "admin",
  "enabled": true,
  "accessUrl": "ws://localhost:8091/world/ws",
  "properties": {
    "theme": "sci-fi",
    "maxPlayers": "200",
    "difficulty": "hard",
    "version": "2.0"
  }
}
```

## Delete World

Deletes a world permanently. Requires ADMIN role or ownership of the world.

```bash
curl -X DELETE http://localhost:8082/worlds/550e8400-e29b-41d4-a716-446655440000 \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Success response: HTTP 204 No Content

## Enable World

Enables a world (sets enabled status to true). Requires ADMIN role or ownership of the world.

```bash
curl -X POST http://localhost:8082/worlds/550e8400-e29b-41d4-a716-446655440000/enable \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Test World",
  "description": "A beautiful test world for development",
  "createdAt": 1672531200000,
  "updatedAt": 1672531900000,
  "ownerId": "admin",
  "enabled": true,
  "accessUrl": "ws://localhost:8090/world/ws",
  "properties": {
    "theme": "fantasy",
    "maxPlayers": "100",
    "difficulty": "easy"
  }
}
```

## Disable World

Disables a world (sets enabled status to false). Requires ADMIN role or ownership of the world.

```bash
curl -X POST http://localhost:8082/worlds/550e8400-e29b-41d4-a716-446655440000/disable \
  -H "Authorization: Bearer $JWT_TOKEN"
```

Response:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Test World",
  "description": "A beautiful test world for development",
  "createdAt": 1672531200000,
  "updatedAt": 1672532000000,
  "ownerId": "admin",
  "enabled": false,
  "accessUrl": "ws://localhost:8090/world/ws",
  "properties": {
    "theme": "fantasy",
    "maxPlayers": "100",
    "difficulty": "easy"
  }
}
```

## Error Responses

### 401 Unauthorized
When no valid JWT token is provided:
```json
{
  "timestamp": "2023-01-01T12:00:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "path": "/worlds"
}
```

### 403 Forbidden
When user doesn't have required role:
```json
{
  "timestamp": "2023-01-01T12:00:00.000+00:00",
  "status": 403,
  "error": "Forbidden",
  "path": "/worlds"
}
```

### 404 Not Found
When world doesn't exist:
```json
{
  "timestamp": "2023-01-01T12:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "path": "/worlds/nonexistent-id"
}
```
