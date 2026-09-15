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
| Maven             | 3.9.x    | Build & dependency mgmt   |

## Current Stage

**Stage 2 — Database Design & Setup**

- [x] Maven project created
- [x] MySQL database configured (`college_attendance_db`)
- [x] JDBC connection implemented with environment-variable-based config
- [x] Database schema designed and created (4 tables)
- [x] Foreign keys and unique constraints enforced
- [x] Attendance percentage calculation validated
- [x] Defaulter identification logic verified (75% threshold)
- [x] Swing smoke-test window updated

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

- `attendance.student_id` → `students.id` (FK, ON DELETE RESTRICT)
- `attendance.subject_id` → `subjects.id` (FK, ON DELETE RESTRICT)
- Duplicate attendance for (student + subject + date) is prevented via UNIQUE constraint.

### Attendance Percentage Formula

```
attendance_percentage = (PRESENT count / total classes) × 100
```

**Default threshold:** A student is a **defaulter** when their attendance percentage falls below **75%**.

The threshold is defined in `AppConstants.java` — not hard-coded across the codebase.

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
