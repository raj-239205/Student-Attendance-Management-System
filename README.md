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

**Stage 4 — Dashboard & Role-Based Navigation**

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
- [x] Strict student data isolation anchored solely to `Session.getStudentId()`
- [x] Polished module placeholders (`ModulePlaceholderView.java`) for subsequent development stages
- [x] 15-point automated authentication test suite (`AuthenticationTest.java`)
- [x] 57-point automated dashboard and authorization test suite (`DashboardAuthorizationTest.java`)

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
