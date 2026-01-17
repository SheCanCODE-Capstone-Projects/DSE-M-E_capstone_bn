# DSE M&E Portal - Role-Based Access Control (RBAC) System

## Overview
The DSE M&E Portal implements a comprehensive role-based access control system with four distinct roles and a secure approval workflow for role assignments.

## User Roles

### 1. 👑 ADMIN
**Full System Authority**
- **Access**: Complete system access
- **Responsibilities**:
  - Approve/reject all role requests
  - Create and manage facilitators
  - Manage courses and cohorts
  - View all analytics and reports
  - Change any user's password
  - System configuration

**Auto-Created on Startup**:
- Email: `admin@dseme.com`
- Password: `Admin@123`
- ⚠️ **Change default password after first login**

### 2. 🎓 FACILITATOR
**Center-Level Access**
- **Access**: Limited to their assigned center
- **Responsibilities**:
  - Create and update participant data (own center only)
  - Manage attendance and scores for current cohort
  - Deploy surveys to participants
  - View center-specific dashboards
  - Manage enrollments for current cohort

### 3. 📊 ME_OFFICER (Partner M&E Officer)
**Partner-Level Access**
- **Access**: Partner-wide data access
- **Responsibilities**:
  - Read and verify participant data across partner
  - Approve enrollments
  - Validate attendance and scores
  - Manage facilitators within partner
  - Create and analyze surveys
  - View partner-wide dashboards and reports

### 4. 💰 DONOR (MasterCard Foundation)
**Read-Only Aggregated Access**
- **Access**: Anonymized and aggregated data only
- **Responsibilities**:
  - View dashboards across all partners
  - Access aggregated reports
  - Review employment outcomes
  - Monitor program performance
  - View survey results (anonymized)

### 5. ❓ UNASSIGNED (Default Role)
**Limited Access - Pending Approval**
- **Access**: Minimal system access
- **Capabilities**:
  - Register and login
  - View own profile
  - Request role assignment
- **Restrictions**:
  - Cannot access courses
  - Cannot access participants
  - Cannot access analytics
  - Cannot perform privileged actions

## Role Assignment Workflow

### 1️⃣ User Registration
```http
POST /api/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```
**Result**: User created with `UNASSIGNED` role

### 2️⃣ User Login (Limited Access)
```http
POST /api/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```
**Result**: JWT token with `role=UNASSIGNED` (limited access)

### 3️⃣ Request Role Assignment
```http
POST /api/users/request/role
Authorization: Bearer <unassigned_user_token>
{
  "requestedRole": "FACILITATOR",
  "reason": "I will be facilitating the Web Development cohort"
}
```
**Result**: Access request created with `PENDING` status

### 4️⃣ Admin Reviews Requests
```http
# View pending requests
GET /api/access-requests/pending
Authorization: Bearer <admin_token>

# Approve request
POST /api/access-requests/{requestId}/approve
Authorization: Bearer <admin_token>

# Reject request
POST /api/access-requests/{requestId}/reject
Authorization: Bearer <admin_token>
```

### 5️⃣ Role Assignment Complete
- User role updated in database
- User must re-login to get new JWT with updated role
- Full access granted based on assigned role

## API Endpoints by Role

### Public Endpoints (No Authentication)
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/forgot-password
POST /api/auth/reset-password
GET  /api/auth/verify
```

### UNASSIGNED Role Endpoints
```
GET  /api/users/profile
POST /api/users/request/role
```

### ADMIN Role Endpoints
```
GET  /api/access-requests
GET  /api/access-requests/pending
POST /api/access-requests/{id}/approve
POST /api/access-requests/{id}/reject
POST /api/facilitators
PUT  /api/facilitators/{id}
DELETE /api/facilitators/{id}
POST /api/facilitators/{id}/assign-course
DELETE /api/facilitators/{id}/courses/{courseId}
```

### ME_OFFICER Role Endpoints
```
GET  /api/courses
POST /api/courses
PUT  /api/courses/{id}
DELETE /api/courses/{id}
GET  /api/courses/{id}/participants
GET  /api/analytics/overview
```

### FACILITATOR Role Endpoints
```
GET  /api/cohorts
POST /api/cohorts
GET  /api/cohorts/{id}/participants
POST /api/cohorts/{id}/participants
GET  /api/participants
PUT  /api/participants/{id}/status
GET  /api/facilitator/** (all facilitator-specific endpoints)
```

### DONOR Role Endpoints
```
GET  /api/analytics/overview
GET  /api/analytics/courses
GET  /api/analytics/facilitators
```

## Security Implementation

### 1. JWT Token-Based Authentication
```java
// JWT contains user role information
{
  "sub": "user@example.com",
  "role": "FACILITATOR",
  "exp": 1642781234,
  "iat": 1642694834
}
```

### 2. Method-Level Security
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<AccessRequestResponseDTO> approveRequest(@PathVariable UUID id) {
    // Only ADMIN can approve requests
}

@PreAuthorize("hasRole('UNASSIGNED')")
public ResponseEntity<AccessRequestResponseDTO> requestRole(@RequestBody RoleRequestDTO dto) {
    // Only UNASSIGNED users can request roles
}
```

### 3. URL-Based Security
```java
.requestMatchers("/api/access-requests/**").hasRole("ADMIN")
.requestMatchers("/api/courses/**").hasAnyRole("ADMIN", "ME_OFFICER")
.requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "ME_OFFICER", "DONOR")
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    user_id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'UNASSIGNED',
    is_active BOOLEAN DEFAULT true,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Access Requests Table
```sql
CREATE TABLE access_requests (
    request_id UUID PRIMARY KEY,
    requester_email VARCHAR(255) NOT NULL,
    requester_name VARCHAR(255) NOT NULL,
    requested_role VARCHAR(20) NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    reviewed_by UUID REFERENCES users(user_id)
);
```

## Testing the System

### 1. Admin Login (First Time)
```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@dseme.com",
    "password": "Admin@123"
  }'
```

### 2. Register New User
```bash
curl -X POST http://localhost:8088/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "facilitator@example.com",
    "password": "password123",
    "firstName": "Jane",
    "lastName": "Smith"
  }'
```

### 3. Login as UNASSIGNED User
```bash
curl -X POST http://localhost:8088/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "facilitator@example.com",
    "password": "password123"
  }'
```

### 4. Request Role (as UNASSIGNED)
```bash
curl -X POST http://localhost:8088/api/users/request/role \
  -H "Authorization: Bearer <unassigned_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "requestedRole": "FACILITATOR",
    "reason": "I will be teaching web development courses"
  }'
```

### 5. Approve Request (as ADMIN)
```bash
curl -X POST http://localhost:8088/api/access-requests/{requestId}/approve \
  -H "Authorization: Bearer <admin_token>"
```

## Error Responses

### 401 Unauthorized
```json
{
  "timestamp": "2024-01-17T10:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required. Please provide a valid JWT token.",
  "path": "/api/courses"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2024-01-17T10:15:30.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You don't have permission to access this resource.",
  "path": "/api/access-requests"
}
```

## Best Practices

### 1. Token Management
- Store JWT tokens securely (httpOnly cookies or secure storage)
- Implement token refresh mechanism
- Handle token expiration gracefully

### 2. Role Transitions
- Users must re-login after role approval to get updated JWT
- Implement real-time notifications for role changes
- Provide clear feedback on role request status

### 3. Security Monitoring
- Log all authentication attempts
- Monitor failed authorization attempts
- Track role request patterns

### 4. Data Isolation
- FACILITATOR: Center-level data isolation
- ME_OFFICER: Partner-level data isolation  
- DONOR: Aggregated/anonymized data only
- ADMIN: Full system access

This RBAC system ensures proper security, accountability, and governance while maintaining a clean and professional user experience.