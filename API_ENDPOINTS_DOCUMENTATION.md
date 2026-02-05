# DSE M&E Platform - API Endpoints Documentation

## Base URL
```
http://localhost:8088/api
```

## Authentication Flow

### 1. User Registration

**Endpoint:** `POST /auth/register`

**Description:** Register a new user account. New users are assigned the `UNASSIGNED` role by default.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Validation Rules:**
- Email: Required, valid email format
- Password: Required, 5-20 characters
- First Name: Optional
- Last Name: Optional

**Response:**
```json
"User registered successfully. Please check your email for verification."
```

**Status Codes:**
- `200 OK` - Registration successful
- `400 Bad Request` - Validation errors or email already exists

---

### 2. Email Verification

**Endpoint:** `GET /auth/verify?token={verification_token}`

**Description:** Verify user email address using the token sent via email.

**Parameters:**
- `token` (query parameter): Email verification token

**Response:**
```json
"Email verified successfully"
```

**Status Codes:**
- `200 OK` - Email verified successfully
- `400 Bad Request` - Invalid or expired token

---

### 3. Resend Verification Email

**Endpoint:** `POST /auth/resend-verification?email={user_email}`

**Description:** Resend email verification link.

**Parameters:**
- `email` (query parameter): User's email address

**Response:**
```json
"Verification email sent"
```

---

### 4. User Login

**Endpoint:** `POST /auth/login`

**Description:** Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "UNASSIGNED",
    "emailVerified": true
  }
}
```

**Status Codes:**
- `200 OK` - Login successful
- `401 Unauthorized` - Invalid credentials
- `403 Forbidden` - Email not verified

---

## Role Management Flow

### 5. Request Role Assignment

**Endpoint:** `POST /users/request/role`

**Description:** Request a specific role assignment (only for UNASSIGNED users).

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Request Body:**
```json
{
  "requestedRole": "FACILITATOR",
  "reason": "I am a trainer at XYZ center and need access to manage participants"
}
```

**Available Roles:**
- `FACILITATOR` - Participant Instructor
- `ME_OFFICER` - Partner M&E Officers  
- `DONOR` - MasterCard Foundation

**Response:**
```json
{
  "id": "request-uuid",
  "requestedRole": "FACILITATOR",
  "reason": "I am a trainer at XYZ center...",
  "status": "PENDING",
  "requestedAt": "2024-01-15T10:30:00Z",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

**Status Codes:**
- `201 Created` - Request submitted successfully
- `403 Forbidden` - User already has assigned role
- `400 Bad Request` - Invalid role or validation errors

---

## Admin Operations

### 6. View All Access Requests

**Endpoint:** `GET /access-requests`

**Description:** List all role requests (ADMIN only).

**Headers:**
```
Authorization: Bearer {admin_jwt_token}
```

**Query Parameters:**
- `page` (optional): Page number (default: 0)
- `size` (optional): Page size (default: 20)
- `sort` (optional): Sort field and direction (e.g., "requestedAt,desc")

**Response:**
```json
{
  "content": [
    {
      "id": "request-uuid",
      "requestedRole": "FACILITATOR",
      "reason": "I am a trainer...",
      "status": "PENDING",
      "requestedAt": "2024-01-15T10:30:00Z",
      "user": {
        "id": "user-uuid",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe"
      }
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

---

### 7. View Pending Access Requests

**Endpoint:** `GET /access-requests/pending`

**Description:** List only pending role requests (ADMIN only).

**Headers:**
```
Authorization: Bearer {admin_jwt_token}
```

**Response:** Same format as above, filtered for pending requests only.

---

### 8. Approve Access Request

**Endpoint:** `POST /access-requests/{id}/approve`

**Description:** Approve a role request and assign the role to the user (ADMIN only).

**Headers:**
```
Authorization: Bearer {admin_jwt_token}
```

**Path Parameters:**
- `id`: Access request UUID

**Response:**
```json
{
  "id": "request-uuid",
  "requestedRole": "FACILITATOR",
  "reason": "I am a trainer...",
  "status": "APPROVED",
  "requestedAt": "2024-01-15T10:30:00Z",
  "approvedAt": "2024-01-15T14:30:00Z",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "FACILITATOR"
  }
}
```

---

### 9. Reject Access Request

**Endpoint:** `POST /access-requests/{id}/reject`

**Description:** Reject a role request (ADMIN only).

**Headers:**
```
Authorization: Bearer {admin_jwt_token}
```

**Path Parameters:**
- `id`: Access request UUID

**Response:**
```json
{
  "id": "request-uuid",
  "requestedRole": "FACILITATOR",
  "reason": "I am a trainer...",
  "status": "REJECTED",
  "requestedAt": "2024-01-15T10:30:00Z",
  "rejectedAt": "2024-01-15T14:30:00Z",
  "user": {
    "id": "user-uuid",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "UNASSIGNED"
  }
}
```

---

## Password Management

### 10. Forgot Password

**Endpoint:** `POST /auth/forgot-password`

**Description:** Request password reset link.

**Request Body:**
```json
{
  "email": "user@example.com"
}
```

**Response:**
```json
"Password reset email sent"
```

---

### 11. Reset Password

**Endpoint:** `POST /auth/reset-password`

**Description:** Reset password using the token from email.

**Request Body:**
```json
{
  "token": "reset_token_from_email",
  "newPassword": "newpassword123"
}
```

**Response:**
```json
"Password reset successfully"
```

---

## Notifications

### 12. Get User Notifications

**Endpoint:** `GET /notifications`

**Description:** Get notifications for the authenticated user.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response:**
```json
[
  {
    "id": "notification-uuid",
    "title": "Role Request Approved",
    "message": "Your request for FACILITATOR role has been approved",
    "type": "ROLE_APPROVED",
    "read": false,
    "createdAt": "2024-01-15T14:30:00Z"
  }
]
```

---

## Google OAuth2 Authentication

### 13. Google OAuth2 Login

**Endpoint:** `GET /auth/google`

**Description:** Handle Google OAuth2 authentication callback.

**Query Parameters:**
- `code` (optional): OAuth2 authorization code
- `message` (optional): Success message from OAuth flow
- `error` (optional): Error message from OAuth flow

**Response (Success):**
```json
{
  "token": "jwt_token_here",
  "message": "Google authentication successful"
}
```

**Response (Error):**
```json
{
  "error": "Email not verified",
  "message": "Please verify your email before logging in"
}
```

---

## System Health

### 14. Health Check

**Endpoint:** `GET /health`

**Description:** Check system health status.

**Response:**
```json
{
  "status": "UP",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## Complete Registration to Role Assignment Flow

### Step-by-Step Process:

1. **Register** → `POST /auth/register`
2. **Verify Email** → `GET /auth/verify?token={token}`
3. **Login** → `POST /auth/login` (receives JWT token)
4. **Request Role** → `POST /users/request/role` (with JWT token)
5. **Admin Reviews** → `GET /access-requests/pending` (admin checks requests)
6. **Admin Approves** → `POST /access-requests/{id}/approve` (admin approves)
7. **User Gets Notification** → `GET /notifications` (user sees approval)
8. **User Can Access Role-Specific Features** → Use JWT token for protected endpoints

### Authentication Headers

For all protected endpoints, include:
```
Authorization: Bearer {jwt_token}
```

### Error Response Format

All endpoints return errors in this format:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/auth/register"
}
```

### Environment Configuration

The system uses these environment variables from `.env`:
- `DB_URL`: Database connection URL
- `JWT_SECRET`: JWT signing secret
- `MAIL_HOST`: SMTP server host
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID
- `PORT`: Server port (default: 8088)

### Testing with cURL

**Register a new user:**
```bash
curl -X POST http://localhost:8088/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Request role (with JWT token):**
```bash
curl -X POST http://localhost:8088/api/users/request/role \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "requestedRole": "FACILITATOR",
    "reason": "I need access to manage participants"
  }'
```