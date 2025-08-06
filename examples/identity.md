# Identity Service API Examples

This document contains curl examples for all Identity Service endpoints.

## Create User

Creates a new user in the system.

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "id": "john_doe",
    "name": "John Doe",
    "nickname": "Johnny",
    "email": "john.doe@example.com",
    "password": "securePassword123",
    "roles": ["USER"]
  }'
```

Response:
```json
{
  "id": "john_doe",
  "name": "John Doe",
  "nickname": "Johnny",
  "email": "john.doe@example.com",
  "roles": ["USER"],
  "createdAt": 1641024000,
  "updatedAt": 1641024000
}
```

## Login

Authenticates a user and returns a JWT token.

```bash
curl -X POST http://localhost:8081/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "john_doe",
    "password": "securePassword123"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": 1641031200,
  "issuedAt": 1641024000
}
```

## Get User

Retrieves user information by ID.

```bash
# User accessing their own data
curl -X GET http://localhost:8081/users/john_doe \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

# Admin accessing any user data
curl -X GET http://localhost:8081/users/jane_doe \
  -H "Authorization: Bearer <admin_token>"
```

Response:
```json
{
  "id": "john_doe",
  "name": "John Doe",
  "nickname": "Johnny",
  "email": "john.doe@example.com",
  "roles": ["USER"],
  "createdAt": 1641024000,
  "updatedAt": 1641024000
}
```

## Update User

Updates user information.

```bash
curl -X PUT http://localhost:8081/users/john_doe \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "id": "john_doe",
    "name": "John Smith",
    "nickname": "Johnny",
    "email": "john.smith@example.com",
    "roles": ["USER", "MODERATOR"]
  }'
```

Response:
```json
{
  "id": "john_doe",
  "name": "John Smith",
  "nickname": "Johnny",
  "email": "john.smith@example.com",
  "roles": ["USER", "MODERATOR"],
  "createdAt": 1641024000,
  "updatedAt": 1641027600
}
```

## Get All Users

Retrieves all users (admin only).

```bash
curl -X GET http://localhost:8081/users \
  -H "Authorization: Bearer <admin_token>"
```

Response:
```json
[
  {
    "id": "john_doe",
    "name": "John Smith",
    "nickname": "Johnny",
    "email": "john.smith@example.com",
    "roles": ["USER", "MODERATOR"],
    "createdAt": 1641024000,
    "updatedAt": 1641027600
  },
  {
    "id": "admin",
    "name": "Administrator",
    "nickname": "Admin",
    "email": "admin@example.com",
    "roles": ["ADMIN"],
    "createdAt": 1641020000,
    "updatedAt": 1641020000
  }
]
```

## Change Password

Changes a user's password.

```bash
curl -X POST http://localhost:8081/users/john_doe/change-password \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "oldPassword": "securePassword123",
    "newPassword": "newSecurePassword456"
  }'
```

Response: 200 OK (no body)

## Renew Token

Renews an existing JWT token.

```bash
curl -X POST http://localhost:8081/token/renew \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresAt": 1641038400,
  "issuedAt": 1641031200
}
```

## Delete User

Deletes a user (admin only).

```bash
curl -X DELETE http://localhost:8081/users/john_doe \
  -H "Authorization: Bearer <admin_token>"
```

Response: 204 No Content

## Health Check

Checks if the service is running.

```bash
curl -X GET http://localhost:8081/health
```

Response:
```
Identity Service is running
```

## Error Examples

### Invalid Credentials

```bash
curl -X POST http://localhost:8081/login \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "john_doe",
    "password": "wrongPassword"
  }'
```

Response: 401 Unauthorized

### Unauthorized Access

```bash
# User trying to access another user's data
curl -X GET http://localhost:8081/users/admin \
  -H "Authorization: Bearer <user_token>"
```

Response: 403 Forbidden

### User Not Found

```bash
curl -X GET http://localhost:8081/users/nonexistent \
  -H "Authorization: Bearer <admin_token>"
```

Response: 404 Not Found

### Duplicate User Creation

```bash
curl -X POST http://localhost:8081/users \
  -H "Content-Type: application/json" \
  -d '{
    "id": "john_doe",
    "name": "Another John",
    "nickname": "Johnny2",
    "email": "john2@example.com",
    "password": "password123",
    "roles": ["USER"]
  }'
```

Response: 400 Bad Request
