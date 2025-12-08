# 1. DSE M&E Monitoring & Evaluation Platform

The DSE M&E Platform is a multi-tenant digital system designed to support effective monitoring, evaluation and reporting across partners, facilitators and donors in the Digital Skills for Employability (DSE) ecosystem. 
The platform centralizes participant data, program enrollment, training progress, employment outcomes, survey management and analytics—ensuring transparency, accuracy, and real-time insights.

The system differentiates access for three user groups: Facilitators, Partner M&E Officers and Donors (MasterCard Foundation), each with clearly defined permissions and roles.

## Features
### Core Functionality

- Secure Login & Access Control: Role-based authentication for Facilitators, Partner M&E Officers, and Donors.

- Multi-Tenant Data Isolation: Partners see only their own data. Donors see aggregated performance across all partners.

- Participant Management: Create, update, and verify participant profiles.

- Cohort Enrollment: Facilitators enroll participants; Partner M&E Officers approve.

- Training Tracking: Manage training modules, attendance, assessments, and performance summaries.

- Internship & Employment Tracking: Record placements and job outcomes to measure program impact.

- Survey Management: Send baseline, midline, endline, and tracer surveys.

- Dashboard Analytics: Real-time insights for partners and donors with KPIs.

- Alerts & Notifications: Data inconsistencies, missing attendance, incomplete profiles, and KPI warnings.

- Reports: Exportable and automated (CSV/PDF), plus scheduled weekly/monthly reports.

###User Experience

- Fast Data Access: Optimized backend queries for partner and donor dashboards.

- Secure Storage: All sensitive data protected via hashed authentication and RBAC.

- Multi-Level Data Views:

     - Facilitators → Center-level
     - Partner M&E → Partner-level
     - Donors → Portfolio-wide

## Technologies

- Spring Boot (Java 17+) — Backend framework

- PostgreSQL — Relational database

- Spring Security — Authentication & RBAC

- Flyway — For reproducible database queries

- Swagger/OpenAPI — API documentation

- Maven — A Java Application Build tool

- Docker — For containerized deployment

## Project Structure
src/
└── main/        
    └── java/
        └── com.dseme.app/
            ├── controllers/              # REST endpoints
            ├── services/                 # Business logic
            ├── repositories/             # JPA repositories
            ├── models/                   # Entities
            ├── configurations/           # Security & global config
            ├── enums/                    # Enums
            ├── dtos/                     # DTOs
            ├── exceptions/               # Global & Custom Exceptions
            ├── filters/                  # Filters (Jwt Filter and Others)
            ├── utilities/                # Utilities
            └── App                       # The main spring class
    └── resources/
        ├── db.migration                  # The migration files
        ├── application.yaml              # The main configuration file
        └── static/...

## Key Modules

- Auth Module: Login, password reset, JWT management

- Partner Management: Create partner organizations and user accounts

- Participant Module: Profiles, cohorts, attendance, scores

- Survey Module: Sending + tracking survey submissions

- Analytics Module: Dashboards for partner and donor views

- Reporting Module: Data export and automated reports

## Installation
### Prerequisites

- Java 17+ 

- Maven 3.8+

- PostgreSQL 14+

- Git

- Docker / Docker Compose

## Steps
- 1. Clone the repository
       git clone https://github.com/your-org/DSE-ME-Platform.git
       cd DSE-ME-Platform

- 2. Configure database

    Create a PostgreSQL database:
    
    CREATE DATABASE dse_me;

- 3. Set environment variables

    Update application.properties or create an .env file:
    
    spring.datasource.url=jdbc:postgresql://localhost:5432/dse_me
    spring.datasource.username=your_username
    spring.datasource.password=your_password

    jwt.secret=your_jwt_secret
    server.port=8088

- 4. Install dependencies
   mvn clean install

- 5. Start the development server
   ./mvnw spring-boot:run

- 6. Open browser

    Navigate to:
    
     http://localhost:8088

## Scripts
Command	                        Description
./mvnw spring-boot:run	        Start development server
./mvnw test	                    Run tests
mvn clean package	            Build production JAR
docker-compose up	            Run system with Docker (if enabled)

## Key Features Implementation
### Authentication & Authorization

- Secure login (JWT)

- Password reset

- Role-based views:

- - Facilitator: Center-level participant & training management

- - Partner M&E Officer: Approvals, partner-wide data, reporting

- - Donor: Aggregated KPIs, cross-partner dashboards

### Routing & API Endpoints

Examples:

Endpoint	                                      Method	            Description
/api/auth/register	                              POST	                Register
/api/auth/login	                                  POST	                Login
/api/users/request/role	                          POST	                Request Role
/api/users/request/approve/{requestId}            POST	                Approve Request Role
/api/users/request/reject/{requestId}             POST	                Reject Request Role
/api/notifications                                GET	                Get notifications by user id

/api/partners	                                  POST	                Create partner
/api/users/assign-role/{id}	                      POST	                Assigning roles to each new users
/api/users	                                      POST	                Create facilitator/mentor
/api/participants	                              POST	                Create/update profile
/api/enrollment	                                  POST	                Enroll in cohort
/api/attendance	                                  POST	                Record attendance
/api/dashboard/donor	                          GET	                Donor analytics
/api/reports/export	                              GET	                Download CSV/PDF

## Dashboards

### Facilitator Dashboard

- Attendance

- Cohort performance

- Incomplete profiles

- Scores overview

### Partner M&E Dashboard

- Cohort comparisons

- Attendance trends

- Assessment performance

- Internship & employment data

### Donor Dashboard

- Portfolio-wide KPIs

- Partner comparison views

- Demographics (gender, disability, region)

- Outcome analytics

## Reporting & Alerts

- Export CSV/PDF

- Scheduled weekly/monthly reports

- - Real-time alerts:

- - Missing attendance

- - Data inconsistencies

- - KPI warnings (dropouts, low employment)

## Design Features

- Data privacy enforced per user role

- Multi-tenant isolation

- Clean, structured APIs

- Fast database querying

- Portable deployment with Docker

📸 Screenshots

(Will be added later)

## Partner dashboard

## Donor dashboard

## Participant management view

## Enrollment page

## Attendance / scores input forms

## Live Demo

(To be added when deployed)

## Login Functionality

Secure JWT-Based Login:
Users log in with their email and password via the /api/auth/login endpoint.

JWT Token Issued:
On successful login, the system returns a JWT token that must be included in the Authorization header for accessing protected endpoints.

Role-Based Access:
The login token enforces role-based access control:

Facilitator → Center-level participant & training management

Partner M&E Officer → Approvals, partner-wide data, reporting

Donor → Aggregated KPIs, cross-partner dashboards

Password Security: Passwords are securely hashed; the system supports password reset functionality.

Example API Request:

POST /api/auth/loginin
Content-Type: application/json

{
"email": "user@example.com",
"password": "userpassword"
}
