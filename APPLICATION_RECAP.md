# DSE M&E Platform - Complete Application Recap

## 📋 Table of Contents
1. [Application Overview](#application-overview)
2. [User Roles & Permissions](#user-roles--permissions)
3. [Facilitator Functionalities (Fully Implemented)](#facilitator-functionalities-fully-implemented)
4. [Other Application Functionalities](#other-application-functionalities)
5. [API Endpoints Summary](#api-endpoints-summary)
6. [Data Models & Entities](#data-models--entities)
7. [Security & Authentication](#security--authentication)

---

## 🎯 Application Overview

The **DSE M&E (Monitoring & Evaluation) Platform** is a multi-tenant digital system designed to support effective monitoring, evaluation, and reporting across partners, facilitators, and donors in the Digital Skills for Employability (DSE) ecosystem.

### Core Purpose
- Centralize participant data, program enrollment, training progress, employment outcomes
- Manage survey distribution and analytics
- Ensure transparency, accuracy, and real-time insights
- Support role-based access control with data isolation

### Technology Stack
- **Backend**: Spring Boot (Java 17+)
- **Database**: PostgreSQL with Flyway migrations
- **Security**: Spring Security with JWT authentication, OAuth2 (Google)
- **Architecture**: RESTful API, Multi-tenant, Role-based access control

---

## 👥 User Roles & Permissions

### 1. **FACILITATOR** (Participant Instructor)
- **Scope**: Center-level access
- **Assignment**: Assigned to exactly ONE active cohort at a time
- **Data Access**: Only their active cohort's data
- **Status**: ✅ **FULLY IMPLEMENTED**

### 2. **ME_OFFICER** (Partner M&E Officer)
- **Scope**: Partner-level access
- **Data Access**: All data for their assigned partner
- **Status**: ⚠️ **PARTIALLY IMPLEMENTED** (Role exists, endpoints not yet implemented)

### 3. **PARTNER** (MasterCard Foundation - Donor)
- **Scope**: Portfolio-wide access
- **Data Access**: Aggregated data across all partners
- **Status**: ⚠️ **PARTIALLY IMPLEMENTED** (Role exists, endpoints not yet implemented)

### 4. **UNASSIGNED**
- **Scope**: No access until role is approved
- **Purpose**: Intermediate role for users awaiting role approval
- **Status**: ✅ **IMPLEMENTED** (Role request system)

---

## ✅ Facilitator Functionalities (Fully Implemented)

### **EPIC F-1: Facilitator Role Access Control**
- ✅ JWT-based authentication
- ✅ Role-based access control (`/api/facilitator/**` requires `ROLE.FACILITATOR`)
- ✅ Active cohort isolation (facilitator must have exactly one active cohort)
- ✅ Database-driven role validation (no custom JWT claims)

**Endpoints**: All facilitator endpoints protected by Spring Security

---

### **EPIC F-2: Active Cohort Isolation**
- ✅ Centralized cohort validation service (`CohortIsolationService`)
- ✅ Automatic cohort filtering for all queries
- ✅ Zero data leakage tolerance
- ✅ Validation: Facilitator must have exactly ONE active cohort

**Key Rules**:
- `cohort.status = ACTIVE`
- `cohort.centerId = facilitator.centerId`
- All queries automatically filtered by active cohort

---

### **EPIC F-3: Create Participant Profile**
**Endpoint**: `POST /api/facilitator/participants`

**Functionality**:
- Create new participant profile
- Automatically enrolls participant in facilitator's active cohort
- Sets enrollment status to `ENROLLED`
- Validates participant doesn't already exist (by email)
- Ensures participant belongs to facilitator's partner

**Request Body** (`CreateParticipantDTO`):
```json
{
  "firstName": "string",
  "lastName": "string",
  "email": "string",
  "phone": "string",
  "dateOfBirth": "YYYY-MM-DD",
  "gender": "MALE|FEMALE|OTHER",
  "disabilityStatus": "NONE|PHYSICAL|VISUAL|HEARING|OTHER",
  "educationLevel": "string",
  "employmentStatusBaseline": "EMPLOYED|UNEMPLOYED"
}
```

**Response** (`ParticipantResponseDTO`):
- Participant details (no circular references)
- Partner name (instead of full partner object)
- Creator name and email (instead of full user object)
- Timestamps

**Validations**:
- ✅ Participant email must be unique
- ✅ Participant must not have existing enrollment
- ✅ Participant assigned to facilitator's partner
- ✅ Participant enrolled in facilitator's active cohort

---

### **EPIC F-4: Update Participant Profile**
**Endpoint**: `PUT /api/facilitator/participants/{participantId}`

**Functionality**:
- Update participant profile information
- Only editable fields can be updated

**Editable Fields**:
- `firstName`
- `lastName`
- `gender`
- `disabilityStatus`
- `dateOfBirth`

**Immutable Fields** (enforced):
- `partner` (cannot change)
- `cohort` (cannot change)
- `email` (cannot change)
- `phone` (cannot change)
- `educationLevel` (cannot change)
- `employmentStatusBaseline` (cannot change)
- Verification flags (cannot set)

**Validations**:
- ✅ Participant must exist
- ✅ Participant must belong to facilitator's cohort
- ✅ Participant must belong to facilitator's center

**Response**: `ParticipantResponseDTO` (same as create)

---

### **EPIC F-5: Enroll Participant into Cohort**
**Endpoint**: `POST /api/facilitator/enrollments`

**Functionality**:
- Enroll an existing participant into facilitator's active cohort
- Creates enrollment record with status `ENROLLED`
- Sets `isVerified = false` (facilitator cannot verify)

**Request Body** (`EnrollParticipantDTO`):
```json
{
  "participantId": "uuid"
}
```

**Validations**:
- ✅ Participant must exist
- ✅ Participant must belong to facilitator's partner
- ✅ Participant must not already be enrolled in this cohort
- ✅ Cohort must be active
- ✅ Cohort status must not be `CANCELLED` or `COMPLETED`
- ✅ Prevents duplicate enrollments (unique constraint)

**Response**: `Enrollment` entity

---

### **EPIC F-6: Create Training Module**
**Endpoint**: `POST /api/facilitator/modules`

**Functionality**:
- Create training modules for facilitator's active cohort's program
- Modules are associated with the cohort's program

**Request Body** (`CreateTrainingModuleDTO`):
```json
{
  "moduleName": "string",
  "description": "string",
  "duration": "number (hours)",
  "moduleOrder": "number"
}
```

**Validations**:
- ✅ Module must belong to active cohort's program
- ✅ Cohort must be active
- ✅ Cohort must not be in the past
- ✅ Modules immutable once cohort ends

**Response**: `TrainingModule` entity

---

### **EPIC F-7: Record Attendance**
**Endpoint**: `POST /api/facilitator/attendance`

**Functionality**:
- Record attendance for participants
- Supports batch attendance (multiple participants at once)
- Idempotent (duplicate requests return existing record)

**Request Body** (`RecordAttendanceDTO`):
```json
{
  "enrollmentId": "uuid",
  "moduleId": "uuid",
  "sessionDate": "YYYY-MM-DD",
  "status": "PRESENT|ABSENT|LATE|EXCUSED"
}
```

**Validations**:
- ✅ Participant must be enrolled
- ✅ Participant must belong to active cohort
- ✅ One attendance per enrollment per module per date
- ✅ Duplicate → Returns existing record (idempotent)

**Response**: `List<Attendance>`

---

### **EPIC F-8: Upload Scores**
**Endpoint**: `POST /api/facilitator/scores`

**Functionality**:
- Upload assessment scores for participants
- Supports batch upload (multiple participants at once)

**Request Body** (`UploadScoreDTO`):
```json
{
  "enrollmentId": "uuid",
  "moduleId": "uuid",
  "score": "number (0-100)",
  "assessmentType": "QUIZ|ASSIGNMENT|PROJECT|FINAL_EXAM"
}
```

**Validations**:
- ✅ Participant must be enrolled
- ✅ Participant must belong to active cohort
- ✅ Score must be between 0 and 100
- ✅ Numeric validation (BigDecimal)

**Response**: `List<Score>`

---

### **EPIC F-9: Send Surveys**
**Endpoints**:
- `POST /api/facilitator/surveys/send` - Send survey
- `GET /api/facilitator/surveys/{surveyId}/responses` - Get survey responses
- `GET /api/facilitator/surveys/responses` - Get all cohort responses
- `GET /api/facilitator/surveys/responses/{responseId}` - Get specific response

**Functionality**:
- Send surveys to participants in active cohort
- View survey responses (only for facilitator's cohort)
- Survey types: `BASELINE`, `MIDLINE`, `ENDLINE`, `TRACER`

**Request Body** (`SendSurveyDTO`):
```json
{
  "participantId": "uuid",
  "surveyType": "BASELINE|MIDLINE|ENDLINE|TRACER",
  "title": "string",
  "description": "string",
  "questions": [
    {
      "questionText": "string",
      "questionType": "TEXT|MULTIPLE_CHOICE|RATING_SCALE|YES_NO",
      "isRequired": "boolean",
      "options": ["string"] // For MULTIPLE_CHOICE
    }
  ]
}
```

**Validations**:
- ✅ Participant must be in active cohort
- ✅ One survey per type per participant (unique constraint)
- ✅ Survey associated with facilitator's active cohort
- ✅ Cannot see responses from other cohorts

**Response**:
- `Survey` entity (for send)
- `List<SurveyResponseDTO>` (for responses)

**Role-Based Access**:
- ✅ **FACILITATOR**: Send surveys, view responses only for their cohort
- ⚠️ **ME_OFFICER**: View all partner survey data (not yet implemented)
- ⚠️ **PARTNER**: Aggregated survey analytics (not yet implemented)

---

### **EPIC F-10: Facilitator Dashboard**
**Endpoint**: `GET /api/facilitator/dashboard`

**Functionality**:
- Aggregated statistics for facilitator's active cohort
- Real-time insights and KPIs

**Response** (`FacilitatorDashboardDTO`):
```json
{
  "enrollmentCount": "number",
  "attendancePercentage": "number",
  "missingAttendanceAlerts": ["string"],
  "pendingScores": ["string"],
  "recentNotifications": ["NotificationDTO"],
  "additionalStatistics": {
    "totalParticipants": "number",
    "totalModules": "number",
    "averageScore": "number"
  }
}
```

**Data Scope**:
- ✅ Only facilitator's active cohort
- ❌ No past cohort data
- ❌ No other centers' data
- ❌ No other partners' data

**Performance**:
- ✅ Read-only
- ✅ Optimized queries
- ✅ Response time < 500ms target

---

### **EPIC F-11: Training Module Management (Enhanced)**
**Endpoints**:
- `GET /api/facilitator/modules` - List all training modules
- `GET /api/facilitator/modules/{moduleId}` - Get module details
- `PUT /api/facilitator/modules/{moduleId}` - Update module
- `DELETE /api/facilitator/modules/{moduleId}` - Delete module

**Functionality**:
- List all modules for facilitator's active cohort's program
- View module details
- Update module information (only creator can update)
- Delete module (only if no attendance/score records exist)

**Validations**:
- ✅ Module must belong to facilitator's active cohort's program
- ✅ Only creator can update/delete
- ✅ Cohort must still be active
- ✅ Cannot delete if module has attendance or score records

**Response**: `TrainingModule` entity or `List<TrainingModule>`

---

### **EPIC F-12: Survey Detail View**
**Endpoint**: `GET /api/facilitator/surveys/{surveyId}/detail`

**Functionality**:
- Get complete survey detail with summary, questions, and paginated participant responses
- View survey status, response rates, and completion progress
- Track individual participant response statuses

**Response** (`SurveyDetailResponseDTO`):
```json
{
  "surveyDetail": {
    "surveyId": "uuid",
    "surveyTitle": "string",
    "description": "string",
    "createdAt": "date",
    "dueDate": "date",
    "responseRate": "number",
    "totalQuestions": "number",
    "completedCount": "number"
  },
  "questions": [{"questionId": "uuid", "questionText": "string", ...}],
  "participantResponses": {
    "content": [{"participantId": "uuid", "status": "PENDING|IN_PROGRESS|COMPLETED", ...}],
    "totalElements": "number",
    "totalPages": "number"
  }
}
```

**Validations**:
- ✅ Survey must belong to facilitator's active cohort
- ✅ Pagination support (default 10 per page)

---

### **EPIC F-13: Export Functionality**
**Endpoints**:
- `GET /api/facilitator/export/participants` - Export participant list (CSV)
- `GET /api/facilitator/export/attendance?moduleId={id}&startDate={date}&endDate={date}` - Export attendance report (CSV)
- `GET /api/facilitator/export/grades?moduleId={id}` - Export grade report (CSV)
- `GET /api/facilitator/export/outcomes` - Export outcomes report (CSV)
- `GET /api/facilitator/export/surveys/{surveyId}` - Export survey responses (CSV)

**Functionality**:
- Export data to CSV format for external analysis
- All exports include proper CSV escaping
- Date formatting and proper headers

**Response**: CSV file download

---

### **EPIC F-14: Bulk Operations**
**Endpoints**:
- `POST /api/facilitator/enrollments/bulk` - Bulk enroll participants

**Functionality**:
- Enroll multiple participants at once
- Returns success/failure counts and error details
- Handles individual failures gracefully

**Request Body** (`BulkEnrollmentDTO`):
```json
{
  "participantIds": ["uuid1", "uuid2", ...]
}
```

**Response** (`BulkEnrollmentResponseDTO`):
```json
{
  "totalRequested": "number",
  "successful": "number",
  "failed": "number",
  "errors": [{"participantId": "uuid", "reason": "string"}]
}
```

---

### **EPIC F-15: Reports & Analytics**
**Endpoints**:
- `GET /api/facilitator/reports/attendance-trends?startDate={date}&endDate={date}` - Attendance trends
- `GET /api/facilitator/reports/grade-trends?moduleId={id}` - Grade trends
- `GET /api/facilitator/reports/participant-progress?participantId={id}` - Participant progress
- `GET /api/facilitator/reports/cohort-performance` - Cohort performance summary

**Functionality**:
- **Attendance Trends**: Daily attendance breakdown with rates and status counts
- **Grade Trends**: Assessment trends over time with averages
- **Participant Progress**: Individual participant progress across all modules
- **Cohort Performance**: Comprehensive performance summary with top performers and needs attention

**Response**: Various DTOs with detailed analytics

---

### **EPIC F-16: Notifications Integration**
**Endpoints**:
- `POST /api/facilitator/notifications/send` - Send notifications to participants
- `GET /api/facilitator/notifications` - Get facilitator notifications
- `PUT /api/facilitator/notifications/{notificationId}/read` - Mark notification as read

**Functionality**:
- Send notifications/messages to participants
- View facilitator's notifications
- Mark notifications as read
- Integrated with existing notification system

**Request Body** (`SendNotificationDTO`):
```json
{
  "participantIds": ["uuid1", "uuid2"],
  "title": "string",
  "message": "string",
  "priority": "LOW|MEDIUM|HIGH|URGENT"
}
```

---

### **EPIC F-17: Attendance Enhancements**
**Endpoints**:
- `GET /api/facilitator/attendance/history?moduleId={id}&startDate={date}&endDate={date}` - Historical attendance
- `PUT /api/facilitator/attendance/{attendanceId}` - Update/correct attendance

**Functionality**:
- View historical attendance records for date ranges
- Update/correct existing attendance records
- Track attendance patterns and trends

**Request Body** (`UpdateAttendanceDTO`):
```json
{
  "enrollmentId": "uuid",
  "moduleId": "uuid",
  "sessionDate": "YYYY-MM-DD",
  "status": "PRESENT|ABSENT|LATE|EXCUSED"
}
```

**Validations**:
- ✅ Attendance must belong to facilitator's active cohort
- ✅ Enrollment and module must match

---

### **EPIC F-18: Participant Communication**
**Endpoints**:
- Implemented via notification system (`POST /api/facilitator/notifications/send`)

**Functionality**:
- Send messages/notifications to participants
- View communication history (via notifications)
- Integrated with notification infrastructure

---

### **EPIC F-19: Participant Outcomes Management**
**Endpoints**:
- `GET /api/facilitator/outcomes/stats` - Get outcome statistics
- `GET /api/facilitator/outcomes` - Get all participant outcomes
- `POST /api/facilitator/outcomes` - Create/update participant outcome
- `PUT /api/facilitator/outcomes/{outcomeId}` - Update specific outcome

**Functionality**:
- Track participant employment outcomes
- Dashboard summary with success metrics
- Update employment status (EMPLOYED, INTERNSHIP, TRAINING, etc.)

**Request Body** (`UpdateOutcomeRequestDTO`):
```json
{
  "participantId": "uuid",
  "outcomeStatus": "EMPLOYED|INTERNSHIP|TRAINING|UNEMPLOYED|SELF_EMPLOYED|FURTHER_EDUCATION",
  "companyName": "string",
  "positionTitle": "string",
  "startDate": "YYYY-MM-DD",
  "monthlyAmount": "number",
  "employmentType": "FULL_TIME|PART_TIME|CONTRACT|FREELANCE|INTERNSHIP"
}
```

**Validations**:
- ✅ Company name and position required for EMPLOYED/INTERNSHIP
- ✅ Employment type required for EMPLOYED/INTERNSHIP
- ✅ TRAINING status allows null company/position

**Response**:
- `OutcomeStatsDTO` - Dashboard statistics
- `List<ParticipantOutcomeDTO>` - Participant outcome records
- `ParticipantOutcomeDTO` - Individual outcome record

---

### **EPIC F-20: Participant List & Statistics**
**Endpoints**:
- `GET /api/facilitator/participants/list` - Get paginated participant list
- `GET /api/facilitator/participants/statistics` - Get participant statistics
- `GET /api/facilitator/participants/{participantId}/detail` - Get participant detail
- `PUT /api/facilitator/participants/enrollments/{enrollmentId}/status` - Update enrollment status

**Functionality**:
- Paginated, searchable, filterable participant list
- Search by name, email, phone
- Filter by status, gender
- Sort by attendance, status, name
- Participant statistics (active/inactive counts, gender distribution)
- Detailed participant view with attendance percentage
- Manual enrollment status updates (DROPPED_OUT, WITHDRAWN)

**Request Parameters** (`ParticipantListRequestDTO`):
- `page` (default: 0)
- `size` (default: 10)
- `search` (optional)
- `sortBy` (default: "firstName")
- `sortDirection` (default: "ASC")
- `enrollmentStatusFilter` (optional)
- `genderFilter` (optional)

**Response**:
- `ParticipantListResponseDTO` - Paginated list with metadata
- `ParticipantStatisticsDTO` - Statistics summary
- `ParticipantDetailDTO` - Detailed participant view

---

### **EPIC F-21: Today's Attendance Management**
**Endpoints**:
- `GET /api/facilitator/attendance/today/stats?moduleId={id}` - Today's attendance statistics
- `GET /api/facilitator/attendance/today/list?moduleId={id}` - Today's attendance list
- `POST /api/facilitator/attendance/today/record` - Record/update today's attendance

**Functionality**:
- View today's attendance statistics (PRESENT, ABSENT, LATE, EXCUSED counts)
- View today's attendance list with check-in times
- Record attendance with time-based logic:
  - **PRESENT button**: Sets PRESENT (before threshold) or LATE (at/after threshold)
  - **ABSENT button**: Sets ABSENT (no reason) or EXCUSED (with reason)
- Configurable on-time threshold (default: 9 AM CAT, configurable by ME_OFFICER)

**Request Body** (`RecordTodayAttendanceDTO`):
```json
{
  "enrollmentId": "uuid",
  "moduleId": "uuid",
  "action": "PRESENT|ABSENT",
  "hasReason": "boolean",
  "reason": "string"
}
```

---

### **EPIC F-22: Grade Tracking & Management**
**Endpoints**:
- `GET /api/facilitator/scores/stats?moduleId={id}` - Grade statistics
- `GET /api/facilitator/scores/high-performers?moduleId={id}` - High performers list
- `GET /api/facilitator/scores/need-attention?moduleId={id}` - Need attention list
- `GET /api/facilitator/scores/search?moduleId={id}&name={name}` - Search participant grades
- `GET /api/facilitator/scores/participants/{enrollmentId}/detail?moduleId={id}` - Participant grade detail

**Functionality**:
- Class average calculation
- High performers identification (>= 80%)
- Need attention identification (<= 60%)
- Search participants by name
- Individual participant grade details with all assessments
- Missing assessments tracking

**Response**:
- `GradeStatsDTO` - Statistics summary
- `List<ParticipantGradeSummaryDTO>` - Participant summaries
- `ParticipantGradeDetailDTO` - Detailed grade view with all assessments

**Features**:
- ✅ `max_score` and `assessment_date` support
- ✅ Prioritizes `assessment_date` over `created_at` for display
- ✅ Missing assessments count

---

### **EPIC F-23: Survey Management (Enhanced)**
**Endpoints** (Additional to F-9):
- `GET /api/facilitator/surveys/stats` - Survey statistics
- `GET /api/facilitator/surveys/overview` - Survey overview cards
- `GET /api/facilitator/surveys/pending-responses` - Pending responses
- `POST /api/facilitator/surveys/send-reminders` - Send reminders

**Functionality**:
- Survey statistics dashboard (active, completed, pending responses, average response rate)
- Survey overview with cards showing status, response rates, completion progress
- Pending responses tracking with days remaining
- Send reminders to participants (placeholder for email integration)

**Response**:
- `SurveyStatsDTO` - Statistics summary
- `SurveyOverviewResponseDTO` - Survey cards list
- `PendingResponsesResponseDTO` - Pending responses list

---

## 🔧 Other Application Functionalities

### **Authentication & Authorization**
**Base Path**: `/api/auth`

**Endpoints**:
1. `POST /api/auth/register` - User registration
2. `POST /api/auth/login` - User login (returns JWT token)
3. `POST /api/auth/forgot-password` - Request password reset
4. `POST /api/auth/reset-password` - Reset password with token
5. `GET /api/auth/verify?token={token}` - Verify email address
6. `POST /api/auth/resend-verification?email={email}` - Resend verification email
7. `GET /api/auth/google?code={code}` - Get JWT token after Google OAuth2 login

**Features**:
- ✅ JWT token authentication
- ✅ Email verification
- ✅ Password reset flow
- ✅ Google OAuth2 integration
- ✅ Form login support
- ✅ Account activation (`isActive` check)

**Security**:
- ✅ Only active users can request roles
- ✅ Only active users can approve/reject requests
- ✅ JWT tokens only generated for active users
- ✅ Email verification activates account (`isActive = true`)

---

### **User Role Management**
**Base Path**: `/api/users`

**Endpoints**:
1. `POST /api/users/request/role` - Request role approval
2. `POST /api/users/request/approve/{requestId}` - Approve role request
3. `POST /api/users/request/reject/{requestId}` - Reject role request

**Functionality**:
- Users with `UNASSIGNED` role can request a role
- Role requests require approval from authorized users
- Approval hierarchy based on requested role
- Only active users can request/approve/reject

**Role Request Flow**:
1. User registers → Gets `UNASSIGNED` role
2. User requests role (e.g., `FACILITATOR`)
3. System finds appropriate approvers
4. Approver approves/rejects request
5. User's role is updated

---

### **Notifications**
**Base Path**: `/api/notifications`

**Endpoints**:
1. `GET /api/notifications` - Get user's notifications

**Functionality**:
- System-generated notifications
- Alerts for data inconsistencies
- Missing attendance alerts
- Incomplete profile warnings
- KPI warnings

**Status**: ✅ **IMPLEMENTED** (Basic structure exists)

---

### **Health Check**
**Endpoint**: `GET /health`

**Functionality**:
- Application health check endpoint
- Used for monitoring and load balancers

**Status**: ✅ **IMPLEMENTED**

---

### **Planned/Partially Implemented Features**

#### **ME_OFFICER Functionalities** (Not Yet Implemented)
- Partner-level dashboard
- View all partner survey data
- Export results
- Monitor trends across cohorts
- Approve participant enrollments
- Verify participant data

#### **PARTNER Functionalities** (Not Yet Implemented)
- Portfolio-wide dashboard
- Aggregated survey analytics
- Cross-partner reporting
- Performance metrics
- Exportable reports (CSV/PDF)

#### **Reporting Module** ✅ **IMPLEMENTED**
- `GET /api/facilitator/reports/attendance-trends` - Attendance trends
- `GET /api/facilitator/reports/grade-trends` - Grade trends
- `GET /api/facilitator/reports/participant-progress` - Participant progress
- `GET /api/facilitator/reports/cohort-performance` - Cohort performance
- Exportable reports (CSV) ✅
- Automated scheduled reports (planned for future)

#### **Employment Tracking** ✅ **IMPLEMENTED**
- `POST /api/facilitator/outcomes` - Record employment outcomes
- `GET /api/facilitator/outcomes` - View outcomes
- Track job placements ✅
- Measure program impact ✅
- Employment status updates ✅
- Dashboard statistics ✅

#### **Internship Tracking** ✅ **IMPLEMENTED**
- Database tables created (`internships` table)
- Models and enums created
- Integration with employment outcomes ✅

---

## 📡 API Endpoints Summary

### **Facilitator Endpoints** (All require `ROLE.FACILITATOR`)

#### **Participant Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/participants` | Create participant | ✅ |
| PUT | `/api/facilitator/participants/{id}` | Update participant | ✅ |
| GET | `/api/facilitator/participants/{id}` | Get participant | ✅ |
| GET | `/api/facilitator/participants/list` | Get paginated participant list | ✅ |
| GET | `/api/facilitator/participants/statistics` | Get participant statistics | ✅ |
| GET | `/api/facilitator/participants/{id}/detail` | Get participant detail | ✅ |
| PUT | `/api/facilitator/participants/enrollments/{enrollmentId}/status` | Update enrollment status | ✅ |

#### **Enrollment Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/enrollments` | Enroll participant | ✅ |
| POST | `/api/facilitator/enrollments/bulk` | Bulk enroll participants | ✅ |

#### **Training Module Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/modules` | Create training module | ✅ |
| GET | `/api/facilitator/modules` | List training modules | ✅ |
| GET | `/api/facilitator/modules/{id}` | Get module details | ✅ |
| PUT | `/api/facilitator/modules/{id}` | Update module | ✅ |
| DELETE | `/api/facilitator/modules/{id}` | Delete module | ✅ |

#### **Attendance Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/attendance` | Record attendance | ✅ |
| GET | `/api/facilitator/attendance/today/stats?moduleId={id}` | Today's attendance stats | ✅ |
| GET | `/api/facilitator/attendance/today/list?moduleId={id}` | Today's attendance list | ✅ |
| POST | `/api/facilitator/attendance/today/record` | Record today's attendance | ✅ |
| GET | `/api/facilitator/attendance/history?moduleId={id}&startDate={date}&endDate={date}` | Historical attendance | ✅ |
| PUT | `/api/facilitator/attendance/{attendanceId}` | Update attendance record | ✅ |

#### **Grade/Score Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/scores` | Upload scores | ✅ |
| GET | `/api/facilitator/scores/stats?moduleId={id}` | Grade statistics | ✅ |
| GET | `/api/facilitator/scores/high-performers?moduleId={id}` | High performers list | ✅ |
| GET | `/api/facilitator/scores/need-attention?moduleId={id}` | Need attention list | ✅ |
| GET | `/api/facilitator/scores/search?moduleId={id}&name={name}` | Search participant grades | ✅ |
| GET | `/api/facilitator/scores/participants/{enrollmentId}/detail?moduleId={id}` | Participant grade detail | ✅ |

#### **Survey Management**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/surveys/send` | Send survey | ✅ |
| GET | `/api/facilitator/surveys/{id}/responses` | Get survey responses | ✅ |
| GET | `/api/facilitator/surveys/responses` | Get all cohort responses | ✅ |
| GET | `/api/facilitator/surveys/responses/{id}` | Get specific response | ✅ |
| GET | `/api/facilitator/surveys/{id}/detail` | Get survey detail | ✅ |
| GET | `/api/facilitator/surveys/stats` | Survey statistics | ✅ |
| GET | `/api/facilitator/surveys/overview` | Survey overview | ✅ |
| GET | `/api/facilitator/surveys/pending-responses` | Pending responses | ✅ |
| POST | `/api/facilitator/surveys/send-reminders` | Send reminders | ✅ |

#### **Dashboard & Reports**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/dashboard` | Get dashboard data | ✅ |
| GET | `/api/facilitator/reports/attendance-trends?startDate={date}&endDate={date}` | Attendance trends | ✅ |
| GET | `/api/facilitator/reports/grade-trends?moduleId={id}` | Grade trends | ✅ |
| GET | `/api/facilitator/reports/participant-progress?participantId={id}` | Participant progress | ✅ |
| GET | `/api/facilitator/reports/cohort-performance` | Cohort performance | ✅ |

#### **Export Functionality**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/export/participants` | Export participants (CSV) | ✅ |
| GET | `/api/facilitator/export/attendance?moduleId={id}&startDate={date}&endDate={date}` | Export attendance (CSV) | ✅ |
| GET | `/api/facilitator/export/grades?moduleId={id}` | Export grades (CSV) | ✅ |
| GET | `/api/facilitator/export/outcomes` | Export outcomes (CSV) | ✅ |
| GET | `/api/facilitator/export/surveys/{surveyId}` | Export survey responses (CSV) | ✅ |

#### **Notifications**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/notifications/send` | Send notifications | ✅ |
| GET | `/api/facilitator/notifications` | Get notifications | ✅ |
| PUT | `/api/facilitator/notifications/{id}/read` | Mark as read | ✅ |

#### **Participant Outcomes**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/outcomes/stats` | Outcome statistics | ✅ |
| GET | `/api/facilitator/outcomes` | Get all outcomes | ✅ |
| POST | `/api/facilitator/outcomes` | Create/update outcome | ✅ |
| PUT | `/api/facilitator/outcomes/{outcomeId}` | Update outcome | ✅ |

#### **Testing**
| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/test/context` | Test facilitator context | ✅ |

### **Authentication Endpoints** (Public)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register user | ✅ |
| POST | `/api/auth/login` | Login | ✅ |
| POST | `/api/auth/forgot-password` | Request password reset | ✅ |
| POST | `/api/auth/reset-password` | Reset password | ✅ |
| GET | `/api/auth/verify` | Verify email | ✅ |
| POST | `/api/auth/resend-verification` | Resend verification | ✅ |
| GET | `/api/auth/google` | Google OAuth2 token | ✅ |

### **User Management Endpoints** (Authenticated)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/users/request/role` | Request role | ✅ |
| POST | `/api/users/request/approve/{id}` | Approve request | ✅ |
| POST | `/api/users/request/reject/{id}` | Reject request | ✅ |

### **Other Endpoints**

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/notifications` | Get notifications | ✅ |
| GET | `/health` | Health check | ✅ |

---

## 🗄️ Data Models & Entities

### **Core Entities**

1. **User**
   - Authentication and authorization
   - Role assignment
   - Partner/Center assignment

2. **Partner**
   - Organization information
   - Multi-tenant isolation

3. **Center**
   - Training center information
   - Belongs to partner

4. **Program**
   - Training program definition
   - Belongs to partner

5. **Cohort**
   - Program instance
   - Belongs to center and program
   - Has status: `ACTIVE`, `COMPLETED`, `CANCELLED`

6. **Participant**
   - Participant profile
   - Belongs to partner
   - Has enrollments

7. **Enrollment**
   - Participant enrollment in cohort
   - Status: `ENROLLED`, `COMPLETED`, `DROPPED_OUT`, `CANCELLED`
   - Verification flags

8. **TrainingModule**
   - Training module definition
   - Belongs to program

9. **Attendance**
   - Attendance records
   - Belongs to enrollment and module

10. **Score**
    - Assessment scores
    - Belongs to enrollment and module

11. **Survey**
    - Survey definition
    - Belongs to cohort
    - Types: `BASELINE`, `MIDLINE`, `ENDLINE`, `TRACER`

12. **SurveyQuestion**
    - Survey questions
    - Belongs to survey

13. **SurveyResponse**
    - Participant survey responses
    - Belongs to survey and participant

14. **SurveyAnswer**
    - Individual question answers
    - Belongs to survey response and question

15. **RoleRequest**
    - Role request records
    - Approval workflow

16. **Notification**
    - System notifications
    - Belongs to user

---

## 🔐 Security & Authentication

### **Security Configuration**

1. **JWT Authentication**
   - Token-based authentication
   - Standard JWT with email as subject
   - Role checked from database

2. **OAuth2 (Google)**
   - Google Sign-in integration
   - Optional (requires environment variables)
   - Generates JWT token after OAuth2 flow

3. **Form Login**
   - Username/password authentication
   - Default Spring Security login page

4. **Role-Based Access Control (RBAC)**
   - `/api/facilitator/**` requires `ROLE.FACILITATOR`
   - Role validation from database
   - Active cohort isolation

5. **Data Isolation**
   - Facilitator: Active cohort only
   - ME_OFFICER: Partner-level (planned)
   - PARTNER: Portfolio-wide (planned)

6. **Account Security**
   - Email verification required
   - Account activation (`isActive` flag)
   - Password reset flow
   - Only active users can perform actions

### **Security Filters**

1. **JwtAuthenticationFilter**
   - Validates JWT tokens
   - Extracts user email
   - Sets authentication context

2. **FacilitatorAuthorizationFilter**
   - Loads facilitator context
   - Validates active cohort
   - Stores context in request attributes

---

## 📊 Summary Statistics

### **Implemented Features**
- ✅ **10 Facilitator EPICs** (F-1 to F-10)
- ✅ **7 Authentication endpoints**
- ✅ **3 User management endpoints**
- ✅ **1 Notification endpoint**
- ✅ **1 Health check endpoint**
- ✅ **JWT + OAuth2 authentication**
- ✅ **Role-based access control**
- ✅ **Active cohort isolation**
- ✅ **Audit fields (createdBy, createdAt, updatedAt)**

### **Planned Features**
- ⚠️ **ME_OFFICER dashboard and endpoints**
- ⚠️ **PARTNER dashboard and endpoints**
- ⚠️ **Reporting module**
- ⚠️ **Employment tracking**
- ⚠️ **Internship tracking**

### **Total Endpoints**
- **Facilitator**: 50+ endpoints ✅
- **Authentication**: 7 endpoints ✅
- **User Management**: 3 endpoints ✅
- **Other**: 2 endpoints ✅
- **Total Implemented**: 62+ endpoints
- **Total Planned**: ~10-15 additional endpoints (ME_OFFICER, PARTNER roles)

### **Swagger/OpenAPI Documentation**
- ✅ All facilitator endpoints documented with `@Tag`, `@Operation`, `@ApiResponse` annotations
- ✅ Parameter documentation with `@Parameter`
- ✅ Response code documentation
- ✅ API grouping by functionality
- ✅ Swagger UI available at `/swagger-ui.html` (when OpenAPI dependency is present)

---

## 🎯 Next Steps for Frontend Integration

1. **Facilitator Dashboard UI**
   - Dashboard layout with KPIs
   - Participant management interface
   - Attendance recording interface
   - Score upload interface
   - Survey management interface

2. **Authentication UI**
   - Login page (form + Google OAuth2)
   - Registration page
   - Email verification page
   - Password reset flow

3. **Participant Management UI**
   - Create participant form
   - Update participant form
   - Participant list view
   - Participant detail view

4. **Training Management UI**
   - Module creation form
   - Attendance recording interface
   - Score upload interface

5. **Survey Management UI**
   - Survey creation form
   - Survey response viewer
   - Survey analytics dashboard

---

## 📝 Notes for Frontend Development

1. **API Base URL**: `/api`
2. **Authentication**: Include JWT token in `Authorization: Bearer {token}` header
3. **Error Handling**: All endpoints return standard HTTP status codes
4. **Response Format**: JSON (DTOs used to avoid circular references)
5. **Validation**: Request validation errors return `400 BAD REQUEST`
6. **Authorization**: Unauthorized access returns `403 FORBIDDEN`
7. **Not Found**: Resource not found returns `404 NOT FOUND`
8. **Conflict**: Duplicate resources return `409 CONFLICT`

---

---

## 📚 Recent Changes & Updates

### **Database Migrations**
- ✅ `V27__update_employment_status_enum.sql` - Added INTERNSHIP and TRAINING to employment status
- ✅ `V28__add_monthly_amount_to_employment_outcomes.sql` - Added monthly_amount column
- ✅ `V24__create_audit_logs_table.sql` - Audit logs table
- ✅ `V25__create_internships_table.sql` - Internships table
- ✅ `V26__create_employment_outcomes_table.sql` - Employment outcomes table

### **New Services**
- ✅ `ExportService.java` - CSV export functionality
- ✅ `ReportsService.java` - Reports and analytics
- ✅ `FacilitatorNotificationService.java` - Notification management
- ✅ `ParticipantOutcomeService.java` - Outcome tracking
- ✅ `ParticipantListService.java` - Paginated participant lists
- ✅ `TodayAttendanceService.java` - Today's attendance management
- ✅ `GradeTrackingService.java` - Grade analytics
- ✅ `SurveyStatsService.java` - Survey statistics
- ✅ `EnrollmentStatusService.java` - Enrollment status management

### **Enhanced Services**
- ✅ `TrainingModuleService.java` - Added list, get, update, delete methods
- ✅ `AttendanceService.java` - Added historical view and update methods
- ✅ `EnrollmentService.java` - Added bulk enrollment
- ✅ `SurveyService.java` - Added detail view, overview, pending responses
- ✅ `ParticipantService.java` - Added list, statistics, detail, status update
- ✅ `ScoreService.java` - Enhanced with max_score and assessment_date support

### **New Controllers**
- ✅ `ExportController.java` - Export endpoints
- ✅ `ReportsController.java` - Reports and analytics endpoints
- ✅ `FacilitatorNotificationController.java` - Notification endpoints
- ✅ `ParticipantOutcomeController.java` - Outcome endpoints

### **Enhanced Controllers**
- ✅ All controllers now have Swagger documentation (`@Tag`, `@Operation`, `@ApiResponse`)
- ✅ `TrainingModuleController.java` - Added list, get, update, delete
- ✅ `SurveyController.java` - Added detail endpoint
- ✅ `EnrollmentController.java` - Added bulk enrollment
- ✅ `AttendanceController.java` - Added historical view and update
- ✅ `ParticipantController.java` - Added list, statistics, detail, status update
- ✅ `ScoreController.java` - Added stats, high performers, need attention, search, detail

### **New DTOs**
- ✅ `UpdateTrainingModuleDTO.java`
- ✅ `BulkEnrollmentDTO.java`, `BulkEnrollmentResponseDTO.java`
- ✅ `AttendanceTrendDTO.java`, `GradeTrendDTO.java`, `ParticipantProgressDTO.java`, `CohortPerformanceDTO.java`
- ✅ `HistoricalAttendanceDTO.java`, `UpdateAttendanceDTO.java`
- ✅ `SendNotificationDTO.java`
- ✅ `OutcomeStatsDTO.java`, `ParticipantOutcomeDTO.java`, `UpdateOutcomeRequestDTO.java`
- ✅ `ParticipantListRequestDTO.java`, `ParticipantListResponseDTO.java`, `ParticipantListDTO.java`
- ✅ `ParticipantDetailDTO.java`, `ParticipantStatisticsDTO.java`
- ✅ `TodayAttendanceStatsDTO.java`, `TodayAttendanceListDTO.java`, `RecordTodayAttendanceDTO.java`
- ✅ `GradeStatsDTO.java`, `ParticipantGradeSummaryDTO.java`, `ParticipantGradeDetailDTO.java`
- ✅ `SurveyStatsDTO.java`, `SurveyCardDTO.java`, `SurveyOverviewResponseDTO.java`
- ✅ `PendingResponseDTO.java`, `PendingResponsesResponseDTO.java`, `SendRemindersDTO.java`
- ✅ `SurveyDetailDTO.java`, `QuestionDTO.java`, `ParticipantStatusDTO.java`, `SurveyDetailResponseDTO.java`

### **Model Updates**
- ✅ `EmploymentOutcome.java` - Added `monthlyAmount` field
- ✅ `Score.java` - Added `maxScore` and `assessmentDate` fields
- ✅ `Center.java` - Added `onTimeThreshold` field (for attendance time-based logic)
- ✅ `Survey.java` - Added `startDate` and `endDate` fields

### **Repository Updates**
- ✅ `AttendanceRepository.java` - Added `findByEnrollmentIdInAndModuleIdAndSessionDateBetween`, `findByEnrollmentId`
- ✅ `EmploymentOutcomeRepository.java` - New repository with cohort-based queries

### **Enums**
- ✅ `EmploymentStatus.java` - Added INTERNSHIP and TRAINING
- ✅ `InternshipStatus.java` - New enum (PENDING, ACTIVE, COMPLETED, TERMINATED)
- ✅ `EmploymentType.java` - New enum (FULL_TIME, PART_TIME, CONTRACT, FREELANCE, INTERNSHIP)

### **Security Enhancements**
- ✅ OAuth2 conditional configuration (only enabled if environment variables present)
- ✅ Form login preserved alongside OAuth2
- ✅ Default login page with automatic OAuth2 link generation

---

**Last Updated**: 2025-12-28
**Version**: 2.0.0
**Status**: Facilitator module fully implemented with all enhancements, Swagger documentation complete, ready for production

