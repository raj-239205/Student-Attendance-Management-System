# College Attendance Management System

A college attendance management system built with Core Java as a college Advanced Java project.

## Technology Stack

| Technology        | Version  | Purpose                   |
|-------------------|----------|---------------------------|
| Java              | 21 LTS   | Core language             |
| Swing / AWT       | —        | GUI framework             |
| JDBC              | —        | Database connectivity     |
| MySQL             | 8.4 LTS  | Database server           |
| MySQL Connector/J | 8.4.0    | JDBC driver               |
| jBCrypt           | 0.4      | Salted password hashing   |
| Maven             | 3.9.x    | Build & dependency mgmt   |

## Current Stage

**Stage 10 — Final Polish, Integration & Production Readiness (COMPLETE)**

- [x] Maven project and JDBC connectivity (`college_attendance_db`)
- [x] Hardened relational database schema (4 tables with PK, FK, CHECK, and UNIQUE constraints)
- [x] Dedicated local MySQL application user (`attendance_app` with least-privilege grants)
- [x] Salted BCrypt password hashing (`PasswordUtil.java`)
- [x] User Data Access Object (`UserDAO.java`) with `PreparedStatement`
- [x] Three supported roles: `ADMIN`, `FACULTY`, `STUDENT`
- [x] Explicit `student_id` foreign key link between `users` and `students` table (`fk_users_student`)
- [x] Account linkage validation enforcing that `STUDENT` users must link to a valid student record
- [x] Thread-safe in-memory session management (`Session.java`) supporting `studentId`
- [x] Modern, portfolio-inspired Swing Login Window (`LoginWindow.java`)
- [x] Modern Application Shell (`MainWindow.java`) with responsive layout and `CardLayout` navigation
- [x] Centralized Design System (`AppColors`, `AppFonts`, `RoundedPanel`, `SidebarNavButton`, `StatCard`, `Badge`)
- [x] Non-bypassable authorization service (`AuthorizationService.java`) enforcing strict role permissions
- [x] Administrator Dashboard (`AdminDashboardView.java`) with live counts for students, subjects, attendance, and defaulters
- [x] Faculty Dashboard (`FacultyDashboardView.java`) with academic metrics and teaching quick actions
- [x] Student Dashboard (`StudentDashboardView.java`) with profile banner, personal attendance %, defaulter warning (< 75.0%), and course breakdown
- [x] Dedicated Student Profile View (`StudentProfileView.java`) for `NavigationItem.MY_PROFILE` displaying student enrollment details directly from `StudentService`
- [x] Complete Student Management Module (`StudentManagementView.java`) replacing the placeholder
- [x] Student Data Access Object (`StudentDAO.java`) with parameterized CRUD, multi-field search, and uniqueness checks
- [x] Business & Validation Service (`StudentService.java`) with field integrity rules and non-bypassable RBAC
- [x] Student Table Model (`StudentTableModel.java`) with custom dark table renderers
- [x] Modal Enrollment & Edit Dialog (`StudentFormDialog.java`) with inline validation error banners
- [x] Read-Only Profile Inspection Dialog (`StudentDetailsDialog.java`)
- [x] Complete Subject Management Module (`SubjectManagementView.java`) replacing the placeholder
- [x] Subject Data Access Object (`SubjectDAO.java`) with parameterized queries, search, and referential checks
- [x] Subject Business & Validation Service (`SubjectService.java`) with input integrity rules and programmatic RBAC
- [x] Subject Table Model (`SubjectTableModel.java`) with custom dark table renderers
- [x] Modal Course Add & Edit Dialog (`SubjectFormDialog.java`) with inline validation error banners
- [x] Read-Only Course Inspection Dialog (`SubjectDetailsDialog.java`)
- [x] Complete Attendance Management Module (`AttendanceManagementView.java`) with two cards: "Mark Attendance" & "Attendance History"
- [x] Attendance Data Access Object (`AttendanceDAO.java`) supporting transactional batch execution, duplicate upserts, and filtered history queries
- [x] Attendance Business & Security Service (`AttendanceService.java`) managing transaction boundaries, validations, student isolation, and percentage math
- [x] Interactive Marking Table Model (`AttendanceMarkingTableModel.java`) supporting bulk actions ("Mark All Present", "Mark All Absent") and per-row toggling
- [x] History Log Table Model (`AttendanceTableModel.java`) displaying joined student, course, and status badges
- [x] Dedicated Student Attendance View (`StudentAttendanceView.java`) for `NavigationItem.MY_ATTENDANCE` strictly bound to `Session.getStudentId()`
- [x] Automatic duplicate prevention via database unique constraint (`uq_attendance_record`) and graceful re-marking support
- [x] Role-Guarded Controls: ADMIN (full management & delete), FACULTY (marking & updates), STUDENT (read-only personal access)
- [x] Complete Automatic Defaulter Management Module (`DefaulterManagementView.java`) for `NavigationItem.DEFAULTERS`
- [x] Defaulter Record model (`DefaulterRecord.java`) and Aggregate Summary model (`DefaulterSummary.java`)
- [x] Dynamic Defaulter Data Access Object (`DefaulterDAO.java`) with zero schema changes, computing live aggregations from attendance records
- [x] Defaulter Service (`DefaulterService.java`) enforcing strict `<` comparison threshold, deficit math, and programmatic RBAC
- [x] Defaulter Table Model (`DefaulterTableModel.java`) displaying sort order (lowest attendance first) and status badges
- [x] Configurable Threshold input supporting dynamic re-calculation (default: `75.0%` defined in `AppConstants`)
- [x] Real-time KPI summary cards: Total Evaluated, Defaulters Count, Safe Students Count, Defaulter Rate %
- [x] Complete Reports & Analytics Module (`ReportsAnalyticsView.java`) for `NavigationItem.REPORTS`
- [x] Report Models / DTOs (`OverallAttendanceSummary`, `StudentAttendanceReportItem`, `SubjectAttendanceReportItem`, `AttendanceTrendItem`, `AttendanceDistribution`)
- [x] Report Data Access Object (`ReportDAO.java`) with efficient SQL aggregations and multi-criteria filters (subject, date range, threshold, status)
- [x] Report Business & Security Service (`ReportService.java`) managing date normalizations, single source of truth percentage consistency, and programmatic RBAC
- [x] Pure Java 2D Vector Charts (`DonutChartPanel`, `BarChartPanel`, `LineTrendChartPanel`) seamlessly matching dark portfolio aesthetic
- [x] Table Models: `StudentReportTableModel`, `SubjectReportTableModel`, `TrendReportTableModel`
- [x] RFC 4180 Compliant CSV Export Utility (`CsvExportUtil.java`) with destination file dialog selection
- [x] Stage 10 System-wide Polish & Hardening: date normalization, threshold clamping [0.0, 100.0], destructive action confirmation, empty-dataset resilience
- [x] Master automated test runner (`AllTestsSuite.java`) executing all 8 regression suites with 100% pass rate
- [x] 15-point automated authentication test suite (`AuthenticationTest.java`)
- [x] 57-point automated dashboard and authorization test suite (`DashboardAuthorizationTest.java`)
- [x] 33-point automated student management test suite (`StudentManagementTest.java`)
- [x] 34-point automated subject management test suite (`SubjectManagementTest.java`)
- [x] 34-point automated attendance management test suite (`AttendanceManagementTest.java`)
- [x] 42-point automated defaulter management test suite (`DefaulterManagementTest.java`)
- [x] 67-point automated reports and analytics test suite (`ReportsAnalyticsTest.java`)
- [x] 62-point automated final polish and integration test suite (`Stage10SystemPolishTest.java`)
- [x] Total automated test suite: 344 passed, 0 failed (100% pass rate)

## User Roles & Data Isolation Architecture

| Role | Purpose & Scope | `student_id` Linkage |
|---|---|---|
| **`ADMIN`** | System administration, user accounts, master academic management. | `NULL` |
| **`FACULTY`** | Marking daily attendance, course management, viewing defaulter lists. | `NULL` |
| **`STUDENT`** | Viewing their own attendance percentage and personal records. | Required (`students.id` FK) |

### Student Data Isolation
- A student user account is linked to exactly one `students.id` via the `users.student_id` foreign key.
- Future student attendance queries are strictly anchored to `Session.getStudentId()` rather than user-entered parameters, guaranteeing robust data isolation.

### Credentials & Least Privilege
The application connects via a dedicated non-root MySQL user:
- **User**: `attendance_app`@`localhost`
- **Privileges**: Strictly `SELECT`, `INSERT`, `UPDATE`, `DELETE` on `college_attendance_db.*` (no administrative or DDL privileges).

### Password Hashing
- Stored in `users.password` as standard 60-character BCrypt hashes (work factor = 12).
- Plain-text passwords are never logged, never stored, and memory buffers (`char[]`) are immediately cleared.

### Environment Variables & Local Config
Database configuration is resolved in the following priority order:
1. Environment variables: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
2. JVM System properties: `-DDB_USER=...`, `-DDB_PASSWORD=...`
3. Local configuration file (`.env` or `local.properties`), which is strictly excluded from Git.

See `.env.example` for a setup template. Never commit real credentials to Git.

## Database Design

### Tables

| Table      | Purpose                          |
|------------|----------------------------------|
| `users`    | Admin and faculty accounts       |
| `students` | Enrolled student records         |
| `subjects` | Courses/subjects per branch/sem  |
| `attendance` | Daily attendance records       |

### Relationships

```
students ──────< attendance >────── subjects
```

### Hardened Constraints & Checks

- **Role Check**: `users.role` ENUM restricted to `('ADMIN', 'FACULTY')` with explicit `chk_users_role` constraint.
- **Timestamp Tracking**: `users` and `students` include `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`.
- **Semester Check**: `chk_students_semester` and `chk_subjects_semester` enforce `semester > 0`.
- **Attendance Status Check**: `chk_attendance_status` strictly restricts `status` to `('PRESENT', 'ABSENT')`.
- **Duplicate Prevention**: `uq_attendance_record` on `(student_id, subject_id, attendance_date)` strictly blocks duplicate entries.

### Attendance Percentage Formula

```
attendance_percentage = (PRESENT count / total classes) × 100
```

**Default threshold:** A student is a **defaulter** when their attendance percentage falls below **75.0%** (`attendance_percentage < 75.0`).
Students with exactly 75.0% attendance are **not** defaulters.

The threshold is centralized in `AppConstants.DEFAULT_DEFAULTER_THRESHOLD = 75.0`.

### Sample Data Seeding

Sample data (`sample_data.sql`) is **not** executed automatically on startup to prevent unintended data duplication. It can be explicitly seeded when desired:
```bash
mvn exec:java -Dexec.args="--seed-sample-data"
```

### SQL Validation Query

```sql
SELECT
    s.roll_no, s.name, sub.subject_code,
    SUM(a.status = 'PRESENT') AS present_count,
    COUNT(*)                  AS total_classes,
    ROUND((SUM(a.status = 'PRESENT') / COUNT(*)) * 100, 2) AS attendance_pct
FROM attendance a
JOIN students s   ON a.student_id = s.id
JOIN subjects sub ON a.subject_id = sub.id
GROUP BY s.id, sub.id
ORDER BY attendance_pct ASC;
```

## Prerequisites

1. **Java 21 LTS** — [Download](https://www.oracle.com/java/technologies/downloads/#java21)
2. **Apache Maven 3.9.x** — [Download](https://maven.apache.org/download.cgi)
3. **MySQL 8.4 LTS** — [Download](https://dev.mysql.com/downloads/mysql/)

Ensure `java`, `mvn`, and `mysql` are on your system `PATH`.

## Database Setup

### 1. Create the database

```sql
CREATE DATABASE IF NOT EXISTS college_attendance_db;
```

### 2. Initialize the schema

The schema is automatically created when the application runs.
It uses `CREATE TABLE IF NOT EXISTS` — safe to re-run.

Alternatively, run the script manually:

```bash
mysql -u root college_attendance_db < src/main/resources/schema.sql
```

### 3. (Optional) Load sample data

```bash
mysql -u root college_attendance_db < src/main/resources/sample_data.sql
```

### 4. Set environment variables

**PowerShell (current session):**

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "your_mysql_password_here"
```

**To set permanently:** Windows → System Properties → Environment Variables

| Variable      | Value                              |
|---------------|------------------------------------|
| `DB_HOST`     | `localhost` *(optional)*           |
| `DB_PORT`     | `3306` *(optional)*                |
| `DB_NAME`     | `college_attendance_db` *(optional)* |
| `DB_USER`     | `root`                             |
| `DB_PASSWORD` | *(your MySQL password)*            |

> **⚠️ Never commit real passwords to Git.**

## Build & Run

### Compile

```bash
mvn clean compile
```

### Run the application

```bash
mvn exec:java
```

This will:
1. Connect to MySQL via JDBC.
2. Create/verify the database schema.
3. Validate tables, constraints, and indexes.
4. Test the attendance percentage calculation.
5. Open the Swing status window.

## Project Structure

```
Java_Project/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/college/attendance/
    │   │       ├── Main.java
    │   │       ├── config/
    │   │       │   ├── AppConstants.java
    │   │       │   └── DatabaseConfig.java
    │   │       ├── database/
    │   │       │   ├── DatabaseConnection.java
    │   │       │   ├── SchemaInitializer.java
    │   │       │   └── SchemaValidator.java
    │   │       ├── model/
    │   │       │   ├── Attendance.java
    │   │       │   ├── Student.java
    │   │       │   ├── Subject.java
    │   │       │   └── User.java
    │   │       ├── dao/
    │   │       ├── service/
    │   │       ├── ui/
    │   │       │   └── SmokeTestWindow.java
    │   │       └── util/
    │   └── resources/
    │       ├── schema.sql
    │       └── sample_data.sql
    └── test/
        └── java/
```

## License

College academic project — not for redistribution.
