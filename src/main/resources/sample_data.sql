-- ============================================================
-- College Attendance Management System — Sample Test Data
-- ============================================================
-- Small dataset for development verification ONLY.
-- DO NOT use in production.
-- ============================================================

-- NOTE: The users table is left empty intentionally.
-- Proper password hashing will be implemented in the login stage.
-- Inserting plaintext passwords here would be bad practice.

-- -----------------------------------------------------------
-- Sample Students
-- -----------------------------------------------------------
INSERT IGNORE INTO students (roll_no, name, branch, semester, section, email, phone)
VALUES
    ('CS2024001', 'Aarav Sharma',  'CSE', 4, 'A', 'aarav.sharma@college.edu',  '9876543210'),
    ('CS2024002', 'Priya Patel',   'CSE', 4, 'A', 'priya.patel@college.edu',   '9876543211'),
    ('CS2024003', 'Rahul Verma',   'CSE', 4, 'B', 'rahul.verma@college.edu',   '9876543212');

-- -----------------------------------------------------------
-- Sample Subjects
-- -----------------------------------------------------------
INSERT IGNORE INTO subjects (subject_code, subject_name, branch, semester)
VALUES
    ('CS401', 'Advanced Java',       'CSE', 4),
    ('CS402', 'Database Management', 'CSE', 4);

-- -----------------------------------------------------------
-- Sample Attendance Records
-- -----------------------------------------------------------
-- Aarav Sharma (CS2024001) — 4/5 present in Advanced Java = 80% (above threshold)
-- Priya Patel  (CS2024002) — 2/5 present in Advanced Java = 40% (DEFAULTER)
-- Rahul Verma  (CS2024003) — 5/5 present in Advanced Java = 100% (above threshold)

INSERT IGNORE INTO attendance (student_id, subject_id, attendance_date, status)
VALUES
    -- Aarav (student_id=1) — Advanced Java (subject_id=1)
    ((SELECT id FROM students WHERE roll_no='CS2024001'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-01', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024001'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-02', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024001'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-03', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024001'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-04', 'ABSENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024001'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-05', 'PRESENT'),

    -- Priya (student_id=2) — Advanced Java (subject_id=1)
    ((SELECT id FROM students WHERE roll_no='CS2024002'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-01', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024002'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-02', 'ABSENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024002'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-03', 'ABSENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024002'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-04', 'ABSENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024002'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-05', 'PRESENT'),

    -- Rahul (student_id=3) — Advanced Java (subject_id=1)
    ((SELECT id FROM students WHERE roll_no='CS2024003'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-01', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024003'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-02', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024003'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-03', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024003'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-04', 'PRESENT'),
    ((SELECT id FROM students WHERE roll_no='CS2024003'), (SELECT id FROM subjects WHERE subject_code='CS401'), '2026-09-05', 'PRESENT');
