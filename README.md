# DSE M&E Monitoring & Evaluation Platform

A comprehensive multi-tenant digital system designed to support effective monitoring, evaluation, and reporting across partners, facilitators, and donors in the Digital Skills for Employability (DSE) ecosystem. The platform centralizes participant data, program enrollment, training progress, employment outcomes, survey management, and analytics—ensuring transparency, accuracy, and real-time insights.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Installation & Setup](#installation--setup)
- [User Roles & Permissions](#user-roles--permissions)
- [API Endpoints](#api-endpoints)
- [Security & Authentication](#security--authentication)
- [Data Models & Entities](#data-models--entities)
- [Access Control & Restrictions](#access-control--restrictions)
- [Database Migrations](#database-migrations)
- [Swagger Documentation](#swagger-documentation)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)

---

## 🎯 Overview

The DSE M&E Platform provides role-based access control with strict data isolation:

- **ADMIN**: System administrator with full access
- **FACILITATOR**: Center-level access, manages participants in their active cohort
- **ME_OFFICER**: Partner-level access, oversees data quality and validation
- **DONOR**: Portfolio-wide access, aggregated analytics across all partners (MasterCard Foundation staff)
- **UNASSIGNED**: No access until role is approved

### Core Capabilities

- ✅ Secure JWT-based authentication with OAuth2 (Google) support
- ✅ Multi-tenant data isolation (partner-level, center-level, cohort-level)
- ✅ Participant management and verification
- ✅ Cohort enrollment with approval workflow
- ✅ Training module management
- ✅ Attendance tracking with time-based logic
- ✅ Score/grade management and validation
- ✅ Survey distribution and analytics
- ✅ Employment and internship outcome tracking
- ✅ Comprehensive reporting (CSV/PDF export)
- ✅ Automated monthly report generation
- ✅ Data consistency alerts and notifications
- ✅ Audit logging for critical actions

---

## ✨ Features

### Authentication & Authorization
- JWT token-based authentication
- Google OAuth2 integration (optional)
- Email verification
- Password reset flow
- Role-based access control (RBAC)
- Account activation/deactivation

### Participant Management
- Create and update participant profiles
- Participant verification (ME_OFFICER)
- Search and filter participants
- Participant statistics and analytics
- Enrollment status management

### Training Management
- Training module creation and management
- Attendance recording with time-based status (PRESENT, LATE, ABSENT, EXCUSED)
- Score/grade upload and tracking
- Grade analytics (high performers, need attention)
- Missing assessment tracking

### Enrollment Management
- Enroll participants into cohorts
- Bulk enrollment support
- Enrollment approval/rejection workflow (ME_OFFICER)
- Enrollment status tracking

### Survey Management
- Send surveys (BASELINE, MIDLINE, ENDLINE, TRACER)
- Survey response tracking
- Survey analytics and statistics
- Pending response reminders
- Survey detail views with pagination

### Reporting & Analytics
- Attendance trends and analytics
- Grade trends and performance metrics
- Participant progress tracking
- Cohort performance summaries
- Export reports (CSV/PDF)
- Automated monthly report generation

### Employment & Outcomes
- Record internship placements
- Track employment outcomes
- Employment status tracking
- Outcome statistics and analytics

### Notifications & Alerts
- System-generated notifications
- Data consistency alerts
- Missing attendance alerts
- Score mismatch detection
- Enrollment gap detection

---

## 🛠 Technology Stack

- **Backend Framework**: Spring Boot 3.3.5 (Java 17+)
- **Database**: PostgreSQL 14+
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security with JWT, OAuth2
- **Database Migrations**: Flyway
- **API Documentation**: Swagger/OpenAPI 3
- **Build Tool**: Maven 3.8+
- **PDF Generation**: Apache PDFBox 2.0.29
- **Email**: Spring Mail (SMTP)
- **Containerization**: Docker (optional)

---

## 📁 Project Structure

```
src/
└── main/
    └── java/
        └── com.dseme.app/
            ├── controllers/              # REST API endpoints
            │   ├── auth/                 # Authentication endpoints
            │   ├── facilitator/         # Facilitator role endpoints
            │   ├── meofficer/           # ME_OFFICER role endpoints
            │   ├── users/               # User management endpoints
            │   └── notifications/       # Notification endpoints
            ├── services/                 # Business logic layer
            │   ├── auth/                 # Authentication services
            │   ├── facilitator/         # Facilitator services
            │   └── meofficer/           # ME_OFFICER services
            ├── repositories/             # JPA repositories
            ├── models/                   # Entity models
            ├── dtos/                     # Data Transfer Objects
            ├── enums/                    # Enumeration types
            ├── configurations/           # Security & global config
            ├── exceptions/               # Custom exceptions
            ├── filters/                  # Security filters
            ├── utilities/                # Utility classes
            └── App.java                  # Main Spring Boot application
    └── resources/
        ├── db.migration/                # Flyway migration files
        ├── application.yaml             # Application configuration
        └── static/                      # Static resources
```

---

## 🚀 Installation & Setup

### Prerequisites

- Java 17 or higher
- Maven 3.8+
- PostgreSQL 14+
- Git
- Docker (optional, for containerized deployment)

### Step 1: Clone the Repository

```bash
git clone https://github.com/SheCanCODE-Capstone-Projects/DSE-M-E_capstone_bn.git
cd DSE-M-E_capstone_bn
```

### Step 2: Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE dse_me;
```

### Step 3: Environment Variables

Create a `.env` file or set environment variables:

```bash
# Database Configuration
DB_URL=jdbc:postgresql://localhost:5432/dse_me
DB_USERNAME=your_username
DB_PASSWORD=your_password

# JWT Configuration
JWT_SECRET=your_jwt_secret_key_minimum_256_bits

# Server Configuration
PORT=8088

# Email Configuration (for password reset and verification)
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_email_password
MAIL_FROM=noreply@dseme.com

# Google OAuth2 (Optional)
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:8088/login/oauth2/code/google
```

### Step 4: Install Dependencies

```bash
mvn clean install
```

### Step 5: Run the Application

```bash
./mvnw spring-boot:run
```

Or using Maven:

```bash
mvn spring-boot:run
```

### Step 6: Access the Application

- **API Base URL**: `http://localhost:8088`
- **Swagger UI**: `http://localhost:8088/swagger-ui.html`
- **Health Check**: `http://localhost:8088/health`

---

## 👥 User Roles & Permissions

### 1. FACILITATOR

**Scope**: Center-level access  
**Assignment**: Assigned to exactly ONE active cohort at a time  
**Data Access**: Only their active cohort's data

**Capabilities**:
- ✅ Create and update participant profiles
- ✅ Enroll participants into active cohort
- ✅ Create and manage training modules
- ✅ Record attendance (with time-based logic)
- ✅ Upload assessment scores
- ✅ Send surveys to participants
- ✅ View survey responses (cohort-scoped)
- ✅ View dashboard with cohort statistics
- ✅ Export data (CSV)
- ✅ Send notifications to participants
- ✅ Track participant outcomes

**Restrictions**:
- ❌ Cannot access other cohorts' data
- ❌ Cannot access other centers' data
- ❌ Cannot verify participants
- ❌ Cannot approve enrollments
- ❌ Cannot access partner-level analytics
- ❌ Cannot modify facilitator-entered attendance (read-only for ME_OFFICER)

### 2. ME_OFFICER (Partner M&E Officer)

**Scope**: Partner-level access  
**Assignment**: Assigned to exactly ONE partner  
**Data Access**: All data for their assigned partner (current + past cohorts)

**Capabilities**:
- ✅ View all participants under partner (current + past cohorts)
- ✅ Verify participant profiles (irreversible, audit logged)
- ✅ Update and archive participant profiles
- ✅ Advanced participant search and filtering
- ✅ Bulk participant operations (update, archive, reminders)
- ✅ Review pending enrollments
- ✅ Approve/reject enrollments (audit logged)
- ✅ Bulk enrollment approval/rejection
- ✅ View attendance summaries (current + past cohorts)
- ✅ Validate scores uploaded by facilitators
- ✅ Create and manage Programs (full CRUD)
- ✅ Create and manage Cohorts (full CRUD)
- ✅ Create and manage Training Modules (full CRUD)
- ✅ Assign modules to facilitators
- ✅ Create and manage Facilitator accounts
- ✅ Update facilitator profiles, activate/deactivate accounts
- ✅ Reset facilitator passwords
- ✅ View all centers with metrics
- ✅ Record internship placements (with duplicate prevention)
- ✅ Record employment outcomes (with duplicate prevention)
- ✅ Update internship/employment outcomes (can edit FACILITATOR-created records)
- ✅ Create and manage program-wide surveys
- ✅ View survey analytics with cohort breakdown
- ✅ Send bulk survey reminders
- ✅ View and manage notifications
- ✅ View audit logs with filtering and export
- ✅ Export reports (CSV/PDF)
- ✅ View data consistency alerts
- ✅ Access automated monthly reports
- ✅ Dashboard with partner-level metrics

**Restrictions**:
- ❌ Cannot access other partners' data
- ❌ Cannot modify facilitator-entered attendance
- ❌ Cannot create or edit scores directly
- ❌ Cannot access portfolio-level (DONOR) analytics
- ❌ Cannot access raw PII in survey responses (aggregated only)

### 3. DONOR (MasterCard Foundation Staff)

**Scope**: Portfolio-wide access  
**Data Access**: Aggregated data across all partners

**Capabilities**:
- ✅ Portfolio-wide dashboard with key metrics and summaries
- ✅ Partner organization management (create, view, update, activate/deactivate)
- ✅ Portfolio-level analytics:
  - Enrollment KPIs (total, growth, by partner, by program)
  - Completion & dropout metrics
  - Employment outcomes (by partner, by cohort, conversion rates)
  - Longitudinal impact tracking (baseline vs endline vs tracer)
  - Demographic & inclusion metrics (gender, disability, education)
  - Regional analytics (center, region, country)
  - Survey impact summaries (completion rates, sentiment analysis)
- ✅ Program & Cohort visibility (list and view details with metrics)
- ✅ Center visibility (list and view details with metrics)
- ✅ Notification management (view, filter, mark as read)
- ✅ Alert management (view KPI alerts, resolve alerts)
- ✅ Report exports (CSV, PDF) with scheduled generation
- ✅ Audit log visibility with filtering
- ✅ KPI anomaly detection (automated alerts for dropout spikes, low employment, enrollment stagnation)

**Restrictions**:
- ❌ Cannot access individual participant data (aggregated only)
- ❌ Cannot perform operational data entry (read-only analytics)
- ❌ Cannot modify partner data directly (only through partner management endpoints)

### 4. UNASSIGNED

**Scope**: No access until role is approved  
**Purpose**: Intermediate role for users awaiting role approval

**Capabilities**:
- ✅ Register account
- ✅ Request role assignment
- ❌ Cannot access any protected endpoints

---

## 📡 API Endpoints

### Authentication Endpoints (Public)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | Register new user | ✅ |
| POST | `/api/auth/login` | Login (returns JWT token) | ✅ |
| POST | `/api/auth/forgot-password` | Request password reset | ✅ |
| POST | `/api/auth/reset-password` | Reset password with token | ✅ |
| GET | `/api/auth/verify?token={token}` | Verify email address | ✅ |
| POST | `/api/auth/resend-verification?email={email}` | Resend verification email | ✅ |
| GET | `/api/auth/google?code={code}` | Get JWT token after Google OAuth2 login | ✅ |

### User Management Endpoints (Authenticated)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/users/request/role` | Request role approval | ✅ |
| POST | `/api/users/request/approve/{requestId}` | Approve role request | ✅ |
| POST | `/api/users/request/reject/{requestId}` | Reject role request | ✅ |

### Facilitator Endpoints (Requires `ROLE_FACILITATOR`)

#### Participant Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/participants` | Create participant | ✅ |
| PUT | `/api/facilitator/participants/{id}` | Update participant | ✅ |
| GET | `/api/facilitator/participants/{id}` | Get participant | ✅ |
| GET | `/api/facilitator/participants/list` | Get paginated participant list | ✅ |
| GET | `/api/facilitator/participants/statistics` | Get participant statistics | ✅ |
| GET | `/api/facilitator/participants/{id}/detail` | Get participant detail | ✅ |
| PUT | `/api/facilitator/participants/enrollments/{enrollmentId}/status` | Update enrollment status | ✅ |

#### Enrollment Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/enrollments` | Enroll participant | ✅ |
| POST | `/api/facilitator/enrollments/bulk` | Bulk enroll participants | ✅ |

#### Training Module Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/modules` | Create training module | ✅ |
| GET | `/api/facilitator/modules` | List training modules | ✅ |
| GET | `/api/facilitator/modules/{id}` | Get module details | ✅ |
| PUT | `/api/facilitator/modules/{id}` | Update module | ✅ |
| DELETE | `/api/facilitator/modules/{id}` | Delete module | ✅ |

#### Attendance Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/attendance` | Record attendance | ✅ |
| GET | `/api/facilitator/attendance/today/stats?moduleId={id}` | Today's attendance stats | ✅ |
| GET | `/api/facilitator/attendance/today/list?moduleId={id}` | Today's attendance list | ✅ |
| POST | `/api/facilitator/attendance/today/record` | Record today's attendance | ✅ |
| GET | `/api/facilitator/attendance/history?moduleId={id}&startDate={date}&endDate={date}` | Historical attendance | ✅ |
| PUT | `/api/facilitator/attendance/{attendanceId}` | Update attendance record | ✅ |

#### Grade/Score Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/scores` | Upload scores | ✅ |
| GET | `/api/facilitator/scores/stats?moduleId={id}` | Grade statistics | ✅ |
| GET | `/api/facilitator/scores/high-performers?moduleId={id}` | High performers list | ✅ |
| GET | `/api/facilitator/scores/need-attention?moduleId={id}` | Need attention list | ✅ |
| GET | `/api/facilitator/scores/search?moduleId={id}&name={name}` | Search participant grades | ✅ |
| GET | `/api/facilitator/scores/participants/{enrollmentId}/detail?moduleId={id}` | Participant grade detail | ✅ |

#### Survey Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/surveys/send` | Send survey | ✅ |
| GET | `/api/facilitator/surveys/{id}/detail` | Get survey detail | ✅ |
| GET | `/api/facilitator/surveys/{id}/responses` | Get survey responses | ✅ |
| GET | `/api/facilitator/surveys/responses` | Get all cohort responses | ✅ |
| GET | `/api/facilitator/surveys/responses/{id}` | Get specific response | ✅ |
| GET | `/api/facilitator/surveys/stats` | Survey statistics | ✅ |
| GET | `/api/facilitator/surveys/overview` | Survey overview | ✅ |
| GET | `/api/facilitator/surveys/pending-responses` | Pending responses | ✅ |
| POST | `/api/facilitator/surveys/send-reminders` | Send reminders | ✅ |

#### Dashboard & Reports

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/dashboard` | Get dashboard data | ✅ |
| GET | `/api/facilitator/reports/attendance-trends?startDate={date}&endDate={date}` | Attendance trends | ✅ |
| GET | `/api/facilitator/reports/grade-trends?moduleId={id}` | Grade trends | ✅ |
| GET | `/api/facilitator/reports/participant-progress?participantId={id}` | Participant progress | ✅ |
| GET | `/api/facilitator/reports/cohort-performance` | Cohort performance | ✅ |

#### Export Functionality

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/export/participants` | Export participants (CSV) | ✅ |
| GET | `/api/facilitator/export/attendance?moduleId={id}&startDate={date}&endDate={date}` | Export attendance (CSV) | ✅ |
| GET | `/api/facilitator/export/grades?moduleId={id}` | Export grades (CSV) | ✅ |
| GET | `/api/facilitator/export/outcomes` | Export outcomes (CSV) | ✅ |
| GET | `/api/facilitator/export/surveys/{surveyId}` | Export survey responses (CSV) | ✅ |

#### Notifications

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/facilitator/notifications/send` | Send notifications | ✅ |
| GET | `/api/facilitator/notifications` | Get notifications | ✅ |
| PUT | `/api/facilitator/notifications/{id}/read` | Mark as read | ✅ |

#### Participant Outcomes

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/facilitator/outcomes/stats` | Outcome statistics | ✅ |
| GET | `/api/facilitator/outcomes` | Get all outcomes | ✅ |
| POST | `/api/facilitator/outcomes` | Create/update outcome | ✅ |
| PUT | `/api/facilitator/outcomes/{outcomeId}` | Update outcome | ✅ |

### ME_OFFICER Endpoints (Requires `ROLE_ME_OFFICER`)

All ME_OFFICER endpoints enforce partner-level data isolation. ME_OFFICERs can only access data belonging to their assigned partner.

#### Participant Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/participants` | Get all participants with pagination, search, and filtering | ✅ |
| GET | `/api/me-officer/participants/search` | Advanced search with multiple criteria | ✅ |
| GET | `/api/me-officer/participants/{participantId}/profile` | Get detailed participant profile with performance history | ✅ |
| PUT | `/api/me-officer/participants/{participantId}` | Update participant profile | ✅ |
| PATCH | `/api/me-officer/participants/{participantId}/verify` | Verify participant profile (irreversible) | ✅ |
| PATCH | `/api/me-officer/participants/{participantId}/archive` | Archive participant (soft delete) | ✅ |
| POST | `/api/me-officer/participants/bulk-action` | Perform bulk actions (SEND_REMINDER, CHANGE_COHORT, EXPORT_DATA, ARCHIVE) | ✅ |
| PUT | `/api/me-officer/participants/bulk-update` | Bulk update multiple participants | ✅ |

#### Facilitator Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/facilitators` | Get all facilitators with pagination and filtering | ✅ |
| GET | `/api/me-officer/facilitators/search` | Advanced facilitator search | ✅ |
| GET | `/api/me-officer/facilitators/{facilitatorId}` | Get detailed facilitator profile with activity logs | ✅ |
| POST | `/api/me-officer/facilitators/assign` | Assign/unassign facilitators to cohorts and modules | ✅ |
| POST | `/api/me-officer/facilitators/bulk-action` | Bulk facilitator actions | ✅ |

#### Facilitator Account Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/facilitators/management/create` | Create new facilitator account with welcome email | ✅ |
| PUT | `/api/me-officer/facilitators/management/{facilitatorId}` | Update facilitator profile (excluding email/role) | ✅ |
| PATCH | `/api/me-officer/facilitators/management/{facilitatorId}/status` | Activate/deactivate facilitator account | ✅ |
| POST | `/api/me-officer/facilitators/management/{facilitatorId}/reset-password` | Reset facilitator password | ✅ |

#### Program Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/programs` | Create new program | ✅ |
| GET | `/api/me-officer/programs` | Get all programs with pagination and metrics | ✅ |
| GET | `/api/me-officer/programs/{programId}` | Get detailed program information with cohorts and modules | ✅ |
| PUT | `/api/me-officer/programs/{programId}` | Update program | ✅ |
| DELETE | `/api/me-officer/programs/{programId}` | Delete program (if no active cohorts) | ✅ |

#### Cohort Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/cohorts` | Create new cohort | ✅ |
| GET | `/api/me-officer/cohorts` | Get all cohorts with pagination and metrics | ✅ |
| GET | `/api/me-officer/cohorts/{cohortId}` | Get detailed cohort information with participant lists | ✅ |
| PUT | `/api/me-officer/cohorts/{cohortId}` | Update cohort | ✅ |
| DELETE | `/api/me-officer/cohorts/{cohortId}` | Delete cohort (if no enrollments) | ✅ |

#### Training Module Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/training-modules` | Create training module | ✅ |
| GET | `/api/me-officer/training-modules` | Get all modules for partner's programs | ✅ |
| GET | `/api/me-officer/training-modules/{moduleId}` | Get module details | ✅ |
| PUT | `/api/me-officer/training-modules/{moduleId}` | Update module | ✅ |
| DELETE | `/api/me-officer/training-modules/{moduleId}` | Delete module | ✅ |
| POST | `/api/me-officer/training-modules/{moduleId}/assign` | Assign module to facilitator | ✅ |

#### Survey Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/surveys` | Create program-wide survey | ✅ |
| GET | `/api/me-officer/surveys` | Get survey overview with analytics | ✅ |
| GET | `/api/me-officer/surveys/{surveyId}` | Get detailed survey with question analytics and cohort breakdown | ✅ |
| POST | `/api/me-officer/surveys/{surveyId}/bulk-reminder` | Send bulk reminders for survey | ✅ |
| POST | `/api/me-officer/surveys/send` | Send survey to partner participants | ✅ |

#### Center Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/centers` | Get all centers with pagination and metrics | ✅ |
| GET | `/api/me-officer/centers/{centerId}` | Get detailed center information with cohorts and facilitators | ✅ |

#### Internship Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/internships` | Create internship record (for active/completed cohorts) | ✅ |
| PUT | `/api/me-officer/internships/{internshipId}` | Update internship record (can edit FACILITATOR-created records) | ✅ |

#### Employment Outcome Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/employment-outcomes` | Create employment outcome record (for active/completed cohorts) | ✅ |
| PUT | `/api/me-officer/employment-outcomes/{employmentOutcomeId}` | Update employment outcome (can edit FACILITATOR-created records) | ✅ |

#### Enrollment Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/me-officer/enrollments/bulk-approval` | Bulk approve/reject enrollments | ✅ |

#### Notification Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/notifications` | Get all notifications with filtering (type, priority, read status) | ✅ |
| PATCH | `/api/me-officer/notifications/{notificationId}/read` | Mark notification as read | ✅ |
| PATCH | `/api/me-officer/notifications/mark-read` | Mark multiple notifications as read | ✅ |
| GET | `/api/me-officer/notifications/preferences` | Get notification preferences | ✅ |
| PUT | `/api/me-officer/notifications/preferences` | Update notification preferences | ✅ |

#### Audit Log Viewing

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/audit-logs` | Get audit logs with filtering (actor, action, entity type, date range) | ✅ |
| GET | `/api/me-officer/audit-logs/export` | Export audit logs to CSV | ✅ |

#### Reports & Alerts

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/reports` | Get all report documents | ✅ |
| GET | `/api/me-officer/reports/{reportId}` | Get report document details | ✅ |
| GET | `/api/me-officer/reports/{reportId}/download` | Download report document (CSV/PDF) | ✅ |
| GET | `/api/me-officer/alerts` | Get all system alerts | ✅ |
| PATCH | `/api/me-officer/alerts/{alertId}/acknowledge` | Acknowledge alert | ✅ |

#### Dashboard

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/me-officer/dashboard` | Get dashboard overview with key metrics | ✅ |

### DONOR Endpoints (`/api/donor/**`)

#### Dashboard

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/dashboard` | Get portfolio-wide dashboard with summary metrics | ✅ |

#### Partner Management

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| POST | `/api/donor/partners` | Create new partner organization | ✅ |
| GET | `/api/donor/partners` | Get all partners with metrics | ✅ |
| GET | `/api/donor/partners/{partnerId}` | Get partner details by ID | ✅ |
| PUT | `/api/donor/partners/{partnerId}` | Update partner information | ✅ |
| PATCH | `/api/donor/partners/{partnerId}/status` | Activate/deactivate partner | ✅ |

#### Analytics

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/analytics/enrollments` | Get enrollment KPIs (total, growth, by partner/program) | ✅ |
| GET | `/api/donor/analytics/completion` | Get completion & dropout metrics | ✅ |
| GET | `/api/donor/analytics/employment` | Get employment outcomes (by partner/cohort, conversion) | ✅ |
| GET | `/api/donor/analytics/longitudinal` | Get longitudinal impact tracking (baseline/endline/tracer) | ✅ |
| GET | `/api/donor/analytics/demographics` | Get demographic breakdowns (gender, disability, education) | ✅ |
| GET | `/api/donor/analytics/regions` | Get regional analytics (center/region/country) | ✅ |
| GET | `/api/donor/analytics/surveys` | Get survey impact summaries (completion, sentiment) | ✅ |

#### Visibility (Programs, Cohorts, Centers)

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/programs` | List all programs with filtering | ✅ |
| GET | `/api/donor/programs/{id}` | Get program details with cohort list | ✅ |
| GET | `/api/donor/cohorts` | List all cohorts with filtering | ✅ |
| GET | `/api/donor/cohorts/{id}` | Get cohort details with metrics | ✅ |
| GET | `/api/donor/centers` | List all centers with filtering | ✅ |
| GET | `/api/donor/centers/{id}` | Get center details with cohort list | ✅ |

#### Notifications

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/notifications` | Get notifications with filtering | ✅ |
| PATCH | `/api/donor/notifications/{id}/read` | Mark notification as read | ✅ |
| PATCH | `/api/donor/notifications/read-all` | Mark all notifications as read | ✅ |

#### Alerts

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/alerts` | Get KPI alerts with filtering | ✅ |
| GET | `/api/donor/alerts/{id}` | Get alert details | ✅ |
| PATCH | `/api/donor/alerts/{id}/resolve` | Resolve alert | ✅ |

#### Reports

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/reports/export` | Export reports (CSV/PDF) | ✅ |

#### Audit Logs

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/donor/audit-logs` | Get audit logs with filtering | ✅ |

### Other Endpoints

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| GET | `/api/notifications` | Get user notifications | ✅ |
| GET | `/health` | Health check | ✅ |

---

## 🔐 Security & Authentication

### Authentication Methods

1. **JWT Token Authentication** (Primary)
   - Token issued on successful login
   - Token includes: `userId`, `email`, `role`, `partnerId`
   - Token expiration: 24 hours (configurable)
   - Include token in `Authorization: Bearer {token}` header

2. **Google OAuth2** (Optional)
   - Requires `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`
   - Redirects to Google login page
   - Returns JWT token after successful OAuth2 flow

3. **Form Login** (Fallback)
   - Default Spring Security login page
   - Username/password authentication

### Authorization Filters

1. **JwtAuthenticationFilter**
   - Validates JWT tokens
   - Extracts user email from token
   - Sets authentication context

2. **FacilitatorAuthorizationFilter**
   - Loads facilitator context
   - Validates active cohort assignment
   - Stores context in request attributes

3. **MEOfficerAuthorizationFilter**
   - Loads ME_OFFICER context
   - Validates partner assignment
   - Stores context in request attributes

3. **DonorAuthorizationFilter**
   - Loads DONOR context
   - Validates DONOR role (no partner restriction)
   - Stores context in request attributes

### Security Rules

- `/api/facilitator/**` → Requires `ROLE_FACILITATOR`
- `/api/me-officer/**` → Requires `ROLE_ME_OFFICER`
- `/api/donor/**` → Requires `ROLE_DONOR`
- `/api/auth/**` → Public (except protected endpoints)
- `/health` → Public
- `/swagger-ui/**` → Public (for development)

### Account Security

- ✅ Email verification required for account activation
- ✅ Account activation (`isActive` flag) enforced
- ✅ Password reset flow with token expiration
- ✅ Only active users can perform actions
- ✅ Only active users can request/approve roles

---

## 🏗️ System Architecture

### Dual System Architecture

The platform supports two distinct but complementary systems:

1. **ME Portal System** (`/api/me/*`)
   - Uses `Course` and `MeCohort` models
   - Separate from the main M&E system
   - Managed by ADMIN and ME_OFFICER roles
   - Tables: `courses`, `me_cohorts`, `me_participants`
   - Endpoints: `/api/me/courses`, `/api/me/cohorts`, `/api/me/facilitators`, `/api/me/analytics`

2. **M&E System** (`/api/facilitator/*`, `/api/me-officer/*`)
   - Uses `Program`, `Cohort`, and `Participant` models
   - Main operational system for facilitators and ME_OFFICERs
   - Tables: `programs`, `cohorts`, `participants`, `enrollments`, `training_modules`
   - Endpoints: `/api/facilitator/*`, `/api/me-officer/*`

**Note**: These systems operate independently and do not share data. The ME Portal system is designed for course-level management, while the M&E system handles program-level operations with full partner isolation.

---

## 🗄️ Data Models & Entities

### Core Entities

1. **User**
   - Authentication and authorization
   - Role assignment (ADMIN, FACILITATOR, ME_OFFICER, DONOR, UNASSIGNED)
   - Partner/Center assignment
   - Account status (isActive, isVerified)

2. **Partner**
   - Organization information
   - Multi-tenant isolation identifier

3. **Center**
   - Training center information
   - Belongs to partner
   - On-time threshold for attendance

4. **Program**
   - Training program definition
   - Belongs to partner

5. **Cohort**
   - Program instance
   - Belongs to center and program
   - Status: `ACTIVE`, `COMPLETED`, `CANCELLED`
   - Start and end dates

6. **Participant**
   - Participant profile
   - Belongs to partner
   - Verification fields (isVerified, verifiedBy, verifiedAt)
   - Personal information (name, email, phone, DOB, gender, disability status)

7. **Enrollment**
   - Participant enrollment in cohort
   - Status: `ENROLLED`, `COMPLETED`, `DROPPED_OUT`, `WITHDRAWN`, `CANCELLED`
   - Verification flags (isVerified, verifiedBy, verifiedAt)

8. **TrainingModule**
   - Training module definition
   - Belongs to program
   - Module order and duration

9. **Attendance**
   - Attendance records
   - Belongs to enrollment and module
   - Status: `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`
   - Session date and check-in time

10. **Score**
    - Assessment scores
    - Belongs to enrollment and module
    - Assessment type: `QUIZ`, `ASSIGNMENT`, `PROJECT`, `FINAL_EXAM`
    - Validation fields (isValidated, validatedBy, validatedAt)
    - Max score and assessment date

11. **Survey**
    - Survey definition
    - Belongs to cohort
    - Types: `BASELINE`, `MIDLINE`, `ENDLINE`, `TRACER`
    - Start and end dates

12. **SurveyQuestion**
    - Survey questions
    - Belongs to survey
    - Question type: `TEXT`, `MULTIPLE_CHOICE`, `RATING_SCALE`, `YES_NO`

13. **SurveyResponse**
    - Participant survey responses
    - Belongs to survey and participant
    - Submission status and timestamp

14. **SurveyAnswer**
    - Individual question answers
    - Belongs to survey response and question

15. **Internship**
    - Internship placement records
    - Belongs to enrollment
    - Status: `PENDING`, `ACTIVE`, `COMPLETED`, `TERMINATED`

16. **EmploymentOutcome**
    - Employment outcome records
    - Belongs to enrollment
    - Employment status and type
    - Monthly amount and verification

17. **RoleRequest**
    - Role request records
    - Approval workflow
    - Status: `PENDING`, `APPROVED`, `REJECTED`

18. **Notification**
    - System notifications
    - Belongs to user
    - Type and priority

19. **AuditLog**
    - Audit trail for critical actions
    - Action type, entity type, entity ID
    - User and timestamp

20. **ReportSnapshot**
    - Generated report snapshots
    - Partner, report type, period
    - Data and format (CSV/PDF)

---

## 🚫 Access Control & Restrictions

### Data Isolation Rules

#### FACILITATOR
- ✅ **Cohort-Level Isolation**: Can only access data from their active cohort
- ✅ **Center-Level Isolation**: Can only access data from their assigned center
- ✅ **Partner-Level Isolation**: Can only access data from their partner
- ❌ **Cross-Cohort Access**: Cannot access past or future cohorts
- ❌ **Cross-Center Access**: Cannot access other centers' data
- ❌ **Cross-Partner Access**: Cannot access other partners' data

#### ME_OFFICER
- ✅ **Partner-Level Isolation**: Can access all data from their assigned partner
- ✅ **Historical Data Access**: Can access current + past cohorts
- ❌ **Cross-Partner Access**: Cannot access other partners' data
- ❌ **Portfolio-Level Access**: Cannot access aggregated data across partners

#### DONOR
- ✅ **Portfolio-Level Access**: Can access aggregated data across all partners
- ❌ **Individual Data Access**: Cannot access individual participant data
- ❌ **Partner-Specific Details**: Cannot access partner-specific operational details

### Operation Restrictions

#### FACILITATOR Cannot:
- ❌ Verify participants (ME_OFFICER only)
- ❌ Approve/reject enrollments (ME_OFFICER only)
- ❌ Validate scores (ME_OFFICER only)
- ❌ Access partner-level analytics
- ❌ Access other cohorts' data
- ❌ Modify other facilitators' data

#### ME_OFFICER Cannot:
- ❌ Modify facilitator-entered attendance
- ❌ Create or edit scores directly (can only validate)
- ❌ Access other partners' data
- ❌ Access portfolio-level analytics
- ❌ Access raw PII in survey responses (aggregated only)

#### DONOR Cannot:
- ❌ Perform operational data entry
- ❌ Access individual participant data
- ❌ Access partner-specific operational details

### Validation Rules

#### Participant Verification
- ✅ Only ME_OFFICER can verify participants
- ✅ Verification is irreversible
- ✅ Creates audit log entry
- ✅ Participant must belong to ME_OFFICER's partner

#### Enrollment Approval
- ✅ Only ME_OFFICER can approve/reject enrollments
- ✅ Enrollment must belong to ME_OFFICER's partner
- ✅ Creates audit log entry
- ✅ Rejection sets status to WITHDRAWN

#### Score Validation
- ✅ Only ME_OFFICER can validate scores
- ✅ Score must belong to ME_OFFICER's partner
- ✅ Validation does not modify score values
- ✅ Creates audit log entry

---

## 📊 Database Migrations

The application uses Flyway for database migrations. All migration files are located in `src/main/resources/db.migration/`.

### Key Migrations

- `V1__initial_migration.sql` - Initial schema
- `V2__changing_role_type_to_varchar.sql` - Role type change
- `V3__create_notification_table.sql` - Notifications table
- `V4__create_role_requests_table.sql` - Role requests table
- `V5__add_is_verified_column.sql` - User verification
- `V6__altering_table_users.sql` - User table updates
- `V7__add_on_time_threshold_to_centers.sql` - Attendance threshold
- `V8__programs_table.sql` - Programs table
- `V9__cohorts_table.sql` - Cohorts table
- `V10__participants_table.sql` - Participants table
- `V11__enrollments_table.sql` - Enrollments table
- `V12__training_modules_table.sql` - Training modules table
- `V13__attendance_table.sql` - Attendance table
- `V14__scores_table.sql` - Scores table
- `V15__add_created_by_to_participants_and_enrollments.sql` - Audit fields
- `V16__add_unique_constraint_enrollments.sql` - Unique constraints
- `V17__add_created_by_to_training_modules.sql` - Module audit fields
- `V18__add_unique_constraint_attendance.sql` - Attendance constraints
- `V19__surveys_tables.sql` - Survey tables
- `V20__add_assessment_name_to_scores.sql` - Assessment name
- `V21__add_dates_to_surveys.sql` - Survey dates
- `V22__make_survey_response_submitted_at_nullable.sql` - Nullable submission
- `V23__update_role_enum_constraints_and_backfill.sql` - Role enum migration (PARTNER → DONOR, ADMIN added)
- `V24__add_max_score_and_assessment_date_to_scores.sql` - Score enhancements
- `V25__create_audit_logs_table.sql` - Audit logs table
- `V26__create_internships_table.sql` - Internships table
- `V27__create_employment_outcomes_table.sql` - Employment outcomes table
- `V28__update_employment_status_enum.sql` - Employment status enum
- `V29__add_monthly_amount_to_employment_outcomes.sql` - Monthly amount
- `V30__add_verification_to_participants.sql` - Participant verification
- `V31__add_validation_to_scores.sql` - Score validation
- `V32__create_report_snapshots_table.sql` - Report snapshots table
- `V33__create_module_assignments_table.sql` - Module assignments table
- `V34__add_module_id_to_enrollments.sql` - Module ID in enrollments
- `V35__create_alerts_table.sql` - System alerts table
- `V36__add_created_by_to_employment_outcomes.sql` - Created by field in employment outcomes
- `V37__drop_otp_column.sql` - Remove OTP column from forgotpassword table
- `V38__fix_notifications_table.sql` - Fix notifications table constraints

---

## 📚 Swagger Documentation

All endpoints are documented with Swagger/OpenAPI annotations. Access the Swagger UI at:

```
http://localhost:8088/swagger-ui.html
```

### Documentation Features

- ✅ Endpoint descriptions
- ✅ Request/response schemas
- ✅ Parameter documentation
- ✅ Response code documentation
- ✅ Authentication requirements
- ✅ Example requests/responses

---

## 🧪 Testing

### Run Tests

```bash
mvn test
```

### Test Coverage

- Unit tests for services
- Integration tests for controllers
- Repository tests
- Security filter tests

---

## 🚢 Deployment

### Build Production JAR

```bash
mvn clean package
```

The JAR file will be created in `target/dse-backend-0.0.1-SNAPSHOT.jar`

### Run Production JAR

```bash
java -jar target/dse-backend-0.0.1-SNAPSHOT.jar
```

### Docker Deployment

```bash
docker build -t dse-me-platform .
docker run -p 8088:8088 dse-me-platform
```

### Environment Variables for Production

Ensure all required environment variables are set:
- Database credentials
- JWT secret (use a strong secret in production)
- Email configuration
- OAuth2 credentials (if using Google login)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style

- Follow Java naming conventions
- Use meaningful variable and method names
- Add Javadoc comments for public methods
- Ensure all endpoints have Swagger documentation
- Write unit tests for new features

---

## 📝 License

This project is part of the SheCanCODE Capstone Projects.

---

## 📞 Support

For issues, questions, or contributions, please open an issue on GitHub.

---

## 🎯 Roadmap

### Completed ✅
- ✅ Facilitator role implementation
- ✅ ME_OFFICER role implementation
- ✅ Authentication and authorization
- ✅ Participant management
- ✅ Enrollment management
- ✅ Attendance tracking
- ✅ Score management
- ✅ Survey management
- ✅ Reporting and exports
- ✅ Data consistency alerts

### Planned ⚠️
- ⚠️ DONOR role implementation (portfolio-wide dashboard)
- ⚠️ Advanced analytics
- ⚠️ Real-time notifications
- ⚠️ Mobile app support

---

**Last Updated**: 2025-01-XX  
**Version**: 2.0.0  
**Status**: Production Ready
