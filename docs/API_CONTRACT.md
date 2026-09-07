# BankPoker API Contract & Realtime Protocol

This document defines the REST API contract and Socket.IO realtime event specifications for BankPoker.

---

## 1. Authentication & Identity

### `POST /api/auth/register`
Creates a permanent user account.
- **Request Body:**
  ```json
  {
    "username": "danakarimi",
    "password": "mypassword123",
    "display_name": "Dana Karimi",
    "avatar_id": "avatar_4"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "message": "User registered successfully",
    "token": "<JWT_TOKEN>",
    "user": {
      "id": "uuid",
      "username": "danakarimi",
      "display_name": "Dana Karimi",
      "avatar_id": "avatar_4",
      "role": "SUPER_ADMIN", // First user or env match is SUPER_ADMIN, else USER
      "is_guest": false,
      "created_at": 1788416000000,
      "updated_at": 1788416000000
    }
  }
  ```

### `POST /api/auth/guest`
Creates an anonymous guest user account with a display name.
- **Request Body:**
  ```json
  {
    "display_name": "John Doe"
  }
  ```
- **Response (201 Created):**
  ```json
  {
    "message": "Guest session created",
    "token": "<JWT_TOKEN>",
    "user": {
      "id": "uuid",
      "username": "guest_a7f92b41",
      "display_name": "John Doe",
      "avatar_id": "avatar_12",
      "role": "USER",
      "is_guest": true,
      "created_at": 1788416000000,
      "updated_at": 1788416000000
    }
  }
  ```

### `POST /api/auth/login`
Logs in an existing user with password.
- **Request Body:**
  ```json
  {
    "username": "danakarimi",
    "password": "mypassword123"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "token": "<JWT_TOKEN>",
    "user": { ... }
  }
  ```

### `POST /api/auth/activate`
Converts an existing guest session into a permanent full account.
- **Headers:** `Authorization: Bearer <GUEST_TOKEN>`
- **Request Body:**
  ```json
  {
    "username": "johndoe",
    "password": "newpassword123",
    "display_name": "John Doe",
    "avatar_id": "avatar_8"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "message": "Account activated successfully",
    "token": "<NEW_JWT_TOKEN>",
    "user": { ... }
  }
  ```

### `PUT /api/auth/profile`
Updates current user display name, username (uniqueness enforced), and avatar.
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Request Body:**
  ```json
  {
    "display_name": "Dana K.",
    "username": "danak",
    "avatar_id": "avatar_7"
  }
  ```
- **Response (200 OK):**
  ```json
  {
    "message": "Profile updated successfully",
    "token": "<NEW_JWT_TOKEN_IF_USERNAME_CHANGED>",
    "user": { ... }
  }
  ```

### `GET /api/auth/me`
Fetches the current user profile and notification preferences.
- **Headers:** `Authorization: Bearer <TOKEN>`
- **Response (200 OK):**
  ```json
  {
    "user": { ... },
    "settings": {
      "notif_prefs": {
        "table_created": true,
        "member_joined": true,
        "claim": false,
        "request_to_me": true,
        "settlement": true
      }
    }
  }
  ```

---

## 2. Smart Code Lookup

### `GET /api/lookup/:code`
Unified entrypoint that inspects whether a code is a Group invite code or a Table code.
- **Response (200 OK for Group):**
  ```json
  {
    "type": "GROUP",
    "group": {
      "id": "uuid",
      "name": "Friday Poker",
      "invite_code": "ABC123",
      "owner_user_id": "uuid",
      "owner": {
        "id": "uuid",
        "username": "danakarimi",
        "display_name": "Dana Karimi",
        "avatar_id": "avatar_1"
      },
      "member_count": 8,
      "table_count": 3,
      "created_at": 1788416000000
    }
  }
  ```
- **Response (200 OK for Table):**
  ```json
  {
    "type": "TABLE",
    "table": {
      "id": "uuid",
      "groupId": null, // or uuid if table belongs to group
      "groupName": null,
      "name": "Quick Cash Game",
      "code": "XYZ789",
      "status": "ACTIVE",
      "isQuickTable": true,
      "chip_value": 100,
      "has_entry_fee": false,
      "entry_fee": null,
      "player_count": 5,
      "creator": { ... },
      "created_at": 1788416000000,
      "published_at": 1788416000000
    }
  }
  ```
- **Response (404 Not Found):**
  ```json
  {
    "error": "No group or table found with code \"FOOBAR\""
  }
  ```

---

## 3. Groups & Claim Rules

### `POST /api/groups/create`
Creates a new group. Any user (including guest) can create a group; creator becomes group ADMIN.
- **Request Body:** `{ "name": "Friday Night Poker" }`

### `POST /api/groups/publish` & `POST /api/groups/import`
Publishes or re-links a local group to the server.
- **Re-link Rule:** Match by `server_id` first $\to$ update; else match by `invite_code` $\to$ update; else create new. NEVER create duplicate groups.
- **Tables & Players:** Match by `server_id` or `code`/`name` $\to$ update without duplicate rows.

### `POST /api/groups/:id/claim-player`
Claims an unclaimed player identity or safely switches identity (re-claim).
- **Request Body:**
  ```json
  {
    "playerName": "Alice",
    "newDisplayName": "Alice Cooper" // optional rename chooser
  }
  ```
- **Rules:**
  1. Cannot claim an identity already linked to another user (`400 Bad Request`).
  2. **10-minute re-claim rule:** If user previously claimed an identity in this group, re-claiming another identity is allowed **only within 10 minutes** of their `user_linked_at`. If expired: `400 "Re-claim window has expired (10 minutes limit)"`.
  3. Re-claim unlinks the old identity (`user_id = NULL, user_linked_at = NULL`) returning it to the claimable pool, and links the new identity.

---

## 4. Tables & Quick Tables

### `POST /api/tables/quick`
Creates a Quick Table (`group_id = null`). Table `code` is NULL and hidden until first published.

### `POST /api/tables/:id/publish`
Assigns unique 6-character code to the table and sets `published_at = now`. Emits `table_published`.

### `POST /api/tables/create`
Creates a table inside a group.
- **Request Body:**
  ```json
  {
    "groupId": "uuid",
    "name": "Table 1",
    "chipValue": 100,
    "entryFee": 50,
    "memberPlayerIds": ["player_uuid_1"],
    "newPlayerNames": ["Charlie"]
  }
  ```

### `DELETE /api/tables/:tableId/players/:playerId`
Deletes a player from a table.
- **Permission:** Table creator, group owner, or SUPER_ADMIN.
- **Constraint:** Player must have **ZERO buy-ins**.
- **Error if buy-ins > 0:** `400 "This player has buy-ins and cannot be deleted"`.

### `POST /api/tables/:id/buy-ins` & `POST /api/tables/:id/exits`
Manual buy-in or exit recorded by table manager.

---

## 5. Unified Requests Engine

### `POST /api/tables/:tableId/requests`
Player requests a Buy-In or Exit.
- **Request Body:** `{ "type": "BUY_IN", "amount": 200 }` (or `"EXIT"`)
- Emits `request_created` and sends `request_to_me` notification to table manager.

### `GET /api/tables/:tableId/requests`
List all requests for a table with requesting user details (avatar, display name, username).

### `POST /api/tables/:tableId/requests/:requestId/approve`
Approves request. Automatically inserts `buy_ins` or `exit_records` and emits `request_resolved` + `buyin_recorded`/`exit_recorded`.

### `POST /api/tables/:tableId/requests/:requestId/reject`
Rejects request and emits `request_resolved`.

---

## 6. Realtime Socket.IO Protocol

### Connection
- **URL:** `ws://<host>/` (with polling fallback)
- **Handshake Auth:** `{ "auth": { "token": "<JWT_TOKEN>" } }` or `?token=<JWT_TOKEN>`

### Client Emit Events
- `join_group`: `(groupId: string) => void`
- `leave_group`: `(groupId: string) => void`
- `join_table`: `(tableId: string) => void`
- `leave_table`: `(tableId: string) => void`

### Server Broadcast Events
| Event Name | Room Target | Payload Summary |
|------------|-------------|-----------------|
| `table_created` | `group:<groupId>` | Table details with initial playerCount |
| `table_published` | `table:<tableId>`, `group:<groupId>` | `{ tableId, code, publishedAt }` |
| `table_closed` | `table:<tableId>`, `group:<groupId>` | `{ tableId, closedAt }` |
| `player_added` | `table:<tableId>`, `group:<groupId>` | Player object |
| `player_deleted` | `table:<tableId>`, `group:<groupId>` | `{ tableId, playerId, playerName }` |
| `buyin_recorded` | `table:<tableId>`, `group:<groupId>` | `{ buyInId, tableId, playerId, playerName, amount, timestamp }` |
| `exit_recorded` | `table:<tableId>`, `group:<groupId>` | `{ exitId, tableId, playerId, playerName, amount, timestamp }` |
| `request_created` | `table:<tableId>`, `group:<groupId>` | Request object |
| `request_resolved` | `table:<tableId>`, `group:<groupId>` | `{ requestId, tableId, status, updatedAt }` |
| `settlement_done` | `group:<groupId>` | Settlement update payload |
| `group_updated` | `group:<groupId>` | `{ groupId, name, inviteCode }` |
| `claim_done` | `group:<groupId>` | `{ groupId, userId, playerName, userLinkedAt }` |
| `notification` | `user:<userId>` | Notification object |

---

## 7. Notifications Engine

### User Preference Defaults
| Preference Key | Default | Description |
|----------------|---------|-------------|
| `table_created` | `true` | When a new table is opened in a member's group |
| `member_joined` | `true` | When a player joins a group via invite code |
| `claim` | `false` | When an identity is claimed in a group |
| `request_to_me` | `true` | When a player requests buy-in/exit or request is resolved |
| `settlement` | `true` | When a settlement is completed |

### Anti-Spam Deduplication
If a notification with the same `(user_id, type, targetId)` arrives within 60 seconds, the server collapses it into the existing row, incrementing `count++` and setting `read = 0`.

---

## 8. Super Admin Management

Accessible only to users with `role: "SUPER_ADMIN"`.
- `GET /api/admin/overview`: System statistics.
- `GET /api/admin/users`: List all users with guest flags and group counts.
- `DELETE /api/admin/users/:id`: Safe cascading user deletion.
- `GET /api/admin/groups`: List all groups.
- `DELETE /api/admin/groups/:id`: Safe cascading group deletion.
- `GET /api/admin/tables`: List all tables (including quick tables).
- `DELETE /api/admin/tables/:id`: Safe cascading table deletion.
- `GET /api/admin/tables/:tableId/players`: Inspect table players with balance breakdown.
- `PUT /api/admin/tables/:tableId/players/:playerId`: Modify player name, link, or record balance adjustments.
