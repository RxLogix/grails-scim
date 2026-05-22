# Available SCIM 2.0 Compatible API Endpoints

Base URL: `{baseUrl}/scim/v2`

All requests that include a body must set `Content-Type: application/scim+json`.  
All requests must include an `Authorization: Bearer {token}` header.  
All responses are returned as `application/scim+json`.

---

## Users

### List / Search Users

```
GET /Users
GET /Users?filter=userName eq "john.doe@example.com"
GET /Users?count=10&startIndex=1
GET /Users?attributes=userName,emails
GET /Users?excludedAttributes=emails
```

### Get User by ID

```
GET /Users/{id}
```

### Create User

```
POST /Users
```

**Request body:**
```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:custom:2.0:User"
  ],
  "userName": "john.doe@example.com",
  "displayName": "John Doe",
  "active": true,
  "locale": "en-US",
  "timezone": "America/Los_Angeles",
  "emails": [
    {
      "value": "john.doe@example.com",
      "type": "work",
      "primary": true
    }
  ],
  "urn:ietf:params:scim:schemas:extension:custom:2.0:User": {
    "tenants": ["tenant-001", "DEFAULT"]
  }
}
```

**Response:** `201 Created` with the created user resource.  
The `id` assigned by the server is returned in the response body.

### Update User (full replace)

```
PUT /Users/{id}
```

The `id` field in the request body **must match** the `{id}` in the URL — a mismatch returns `400 Bad Request`.

**Request body:**
```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:User",
    "urn:ietf:params:scim:schemas:extension:custom:2.0:User"
  ],
  "id": "{id}",
  "userName": "john.doe@example.com",
  "displayName": "Johnathan Doe",
  "active": true,
  "emails": [
    {
      "value": "john.doe@example.com",
      "type": "work",
      "primary": true
    }
  ],
  "urn:ietf:params:scim:schemas:extension:custom:2.0:User": {
    "tenants": ["tenant-003"]
  }
}
```

**Response:** `200 OK` with the updated user resource.

### Patch User (partial update)

```
PATCH /Users/{id}
```

**Deactivate a user:**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "Replace",
      "path": "active",
      "value": false
    }
  ]
}
```

**Add a tenant (custom extension):**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "Add",
      "path": "urn:ietf:params:scim:schemas:extension:custom:2.0:User.tenants",
      "value": "tenant-004,tenant-005"
    }
  ]
}
```

**Response:** `204 No Content` on success.

### Delete User

```
DELETE /Users/{id}
```

**Response:** `204 No Content` on success.

---

## Groups

### List / Search Groups

```
GET /Groups
GET /Groups?filter=displayName eq "Finance Team"
GET /Groups?count=10&startIndex=1
```

### Get Group by ID

```
GET /Groups/{id}
```

### Create Group

```
POST /Groups
```

**Request body:**
```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group",
    "urn:ietf:params:scim:schemas:extension:custom:2.0:Group"
  ],
  "displayName": "Finance Team",
  "members": [
    {
      "value": "{userId}",
      "type": "User"
    }
  ],
  "urn:ietf:params:scim:schemas:extension:custom:2.0:Group": {
    "tenant": "tenant-001"
  }
}
```

**Response:** `201 Created` with the created group resource.

### Update Group (full replace)

```
PUT /Groups/{id}
```

The `id` field in the request body **must match** the `{id}` in the URL — a mismatch returns `400 Bad Request`.

**Request body:**
```json
{
  "schemas": [
    "urn:ietf:params:scim:schemas:core:2.0:Group",
    "urn:ietf:params:scim:schemas:extension:custom:2.0:Group"
  ],
  "id": "{id}",
  "displayName": "Finance Team Updated",
  "members": [
    {
      "value": "{userId}",
      "type": "User"
    }
  ],
  "urn:ietf:params:scim:schemas:extension:custom:2.0:Group": {
    "tenant": "tenant-001"
  }
}
```

**Response:** `200 OK` with the updated group resource.

### Patch Group (partial update)

```
PATCH /Groups/{id}
```

**Add a member:**
```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"],
  "Operations": [
    {
      "op": "Add",
      "path": "members",
      "value": [
        {
          "value": "{userId}",
          "type": "User"
        }
      ]
    }
  ]
}
```

**Response:** `204 No Content` on success.

### Delete Group

```
DELETE /Groups/{id}
```

**Response:** `204 No Content` on success.

---

## Discovery Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /Schemas` | All supported SCIM schemas |
| `GET /Schemas/{id}` | A specific schema by URN |
| `GET /ResourceTypes` | Supported resource types |
| `GET /ResourceTypes/{id}` | A specific resource type |
| `GET /ServiceProviderConfig` | Server capabilities and feature support |

---

## Error Responses

All errors return a SCIM-compliant body:

```json
{
  "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
  "detail": "Description of the error",
  "status": "400"
}
```

| Status | Meaning |
|--------|---------|
| `400 Bad Request` | Invalid request data, or `id` in body does not match URI `id` |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Resource already exists (on create) |
| `500 Internal Server Error` | Unexpected server error |
| `501 Not Implemented` | Operation not supported (User delete) |

---

## Custom Extension Schemas

### User Extension — `urn:ietf:params:scim:schemas:extension:custom:2.0:User`

| Attribute | Type | Description |
|-----------|------|-------------|
| `tenants` | Array of String | *(Optional)* Tenant identifiers the user belongs to |

### Group Extension — `urn:ietf:params:scim:schemas:extension:custom:2.0:Group`

| Attribute | Type | Description |
|-----------|------|-------------|
| `tenant` | String | *(Optional)* Tenant identifier the group belongs to |