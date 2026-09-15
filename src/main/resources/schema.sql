-- ============================================================
-- College Attendance Management System — Database Schema
-- ============================================================
-- Safe to run on initial setup.
-- Uses CREATE TABLE IF NOT EXISTS — will not destroy existing data.
-- DO NOT add DROP TABLE / DROP DATABASE / TRUNCATE statements.
-- ============================================================

-- -----------------------------------------------------------
-- 1. USERS (admin / faculty accounts)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    role        ENUM('ADMIN', 'FACULTY') NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 2. STUDENTS
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    roll_no     VARCHAR(20)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    branch      VARCHAR(50)  NOT NULL,
    semester    INT          NOT NULL,
    section     VARCHAR(10)  NOT NULL,
    email       VARCHAR(100) DEFAULT NULL,
    phone       VARCHAR(15)  DEFAULT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_students_roll_no UNIQUE (roll_no),
    CONSTRAINT uq_students_email   UNIQUE (email),

    INDEX idx_students_branch   (branch),
    INDEX idx_students_semester (semester),
    INDEX idx_students_section  (section)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 3. SUBJECTS
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS subjects (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    subject_code  VARCHAR(20)  NOT NULL,
    subject_name  VARCHAR(100) NOT NULL,
    branch        VARCHAR(50)  NOT NULL,
    semester      INT          NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_subjects_code UNIQUE (subject_code),

    INDEX idx_subjects_branch   (branch),
    INDEX idx_subjects_semester (semester)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------
-- 4. ATTENDANCE
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS attendance (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    student_id      INT          NOT NULL,
    subject_id      INT          NOT NULL,
    attendance_date DATE         NOT NULL,
    status          ENUM('PRESENT', 'ABSENT') NOT NULL,
    marked_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate attendance for same student + subject + date
    CONSTRAINT uq_attendance_record UNIQUE (student_id, subject_id, attendance_date),

    -- Foreign keys
    CONSTRAINT fk_attendance_student
        FOREIGN KEY (student_id) REFERENCES students(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,

    INDEX idx_attendance_date (attendance_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
