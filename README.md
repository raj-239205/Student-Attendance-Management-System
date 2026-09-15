# College Attendance Management System

A college attendance management system built with Core Java as a college Advanced Java project.

## Technology Stack

| Technology       | Version  | Purpose                   |
|------------------|----------|---------------------------|
| Java             | 21 LTS   | Core language             |
| Swing / AWT      | —        | GUI framework             |
| JDBC             | —        | Database connectivity     |
| MySQL            | 8.4 LTS  | Database server           |
| MySQL Connector/J| 8.4.0    | JDBC driver               |
| Maven            | 3.9.x    | Build & dependency mgmt   |

## Current Stage

**Stage 1 — Project Foundation & Database Connection**

- [x] Maven project created
- [x] MySQL database configured (`college_attendance_db`)
- [x] JDBC connection implemented with environment-variable-based config
- [x] Swing smoke-test window implemented

## Prerequisites

1. **Java 21 LTS** — [Download](https://www.oracle.com/java/technologies/downloads/#java21)
2. **Apache Maven 3.9.x** — [Download](https://maven.apache.org/download.cgi)
3. **MySQL 8.4 LTS** — [Download](https://dev.mysql.com/downloads/mysql/)

Ensure `java`, `mvn`, and `mysql` are on your system `PATH`.

## Database Setup

### 1. Create the database

Open MySQL Workbench (or the `mysql` CLI) and run:

```sql
CREATE DATABASE IF NOT EXISTS college_attendance_db;
```

### 2. Set environment variables

The application reads database credentials from environment variables so that passwords are never committed to source control.

**PowerShell (current session):**

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "your_mysql_password_here"
```

**Command Prompt (current session):**

```cmd
set DB_USER=root
set DB_PASSWORD=your_mysql_password_here
```

**To set permanently** (Windows → System Properties → Environment Variables), add:

| Variable      | Value                      |
|---------------|----------------------------|
| `DB_HOST`     | `localhost` *(optional)*   |
| `DB_PORT`     | `3306` *(optional)*        |
| `DB_NAME`     | `college_attendance_db` *(optional)* |
| `DB_USER`     | `root`                     |
| `DB_PASSWORD` | *(your MySQL password)*    |

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
1. Load the database configuration from environment variables.
2. Test the JDBC connection to MySQL.
3. Open a small Swing window showing the connection status.

## Project Structure

```
college-attendance-management-system/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/college/attendance/
    │   │       ├── Main.java
    │   │       ├── config/
    │   │       │   └── DatabaseConfig.java
    │   │       ├── database/
    │   │       │   └── DatabaseConnection.java
    │   │       ├── model/
    │   │       ├── dao/
    │   │       ├── service/
    │   │       ├── ui/
    │   │       │   └── SmokeTestWindow.java
    │   │       └── util/
    │   └── resources/
    └── test/
        └── java/
```

## License

College academic project — not for redistribution.
