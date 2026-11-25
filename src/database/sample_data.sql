/*
 * Demo data for the Student Counselling System (all people are fictional).
 * Import AFTER schema.sql.  Dates are relative to today, so the demo always has upcoming appointments.
 *
 * Demo logins:
 *   admin@example.com     Admin@123
 *   grace@example.com     Counsel@123   (also samuel@ and fatuma@example.com)
 *   amina@example.com     Student@123   (also brian@, chloe@, david@example.com)
 */

USE student_counselling_db;

-- users (password hashes are PBKDF2, see utils/PasswordUtil.java)
INSERT INTO users (user_id, full_name, email, phone, password_hash, role) VALUES
 (1, 'System Administrator', 'admin@example.com',  '+254700000000', 'PBKDF2$120000$jih+NAWS+WtCCd8pQlijSA==$Lzq4CbyQhQJBMD5220ycrZ0CC6iImXYx+iftieu8Gew=', 'ADMIN'),
 (2, 'Grace Njeri',          'grace@example.com',  '+254711000001', 'PBKDF2$120000$pyJgmYsJSZkE/n90FtlGiA==$QcMdqK6RLc3cisB1Lf8ZebPwYT+GczJvrMBdMOwVaNA=', 'COUNSELLOR'),
 (3, 'Samuel Kiptoo',        'samuel@example.com', '+254711000002', 'PBKDF2$120000$pyJgmYsJSZkE/n90FtlGiA==$QcMdqK6RLc3cisB1Lf8ZebPwYT+GczJvrMBdMOwVaNA=', 'COUNSELLOR'),
 (4, 'Fatuma Hassan',        'fatuma@example.com', '+254711000003', 'PBKDF2$120000$pyJgmYsJSZkE/n90FtlGiA==$QcMdqK6RLc3cisB1Lf8ZebPwYT+GczJvrMBdMOwVaNA=', 'COUNSELLOR'),
 (5, 'Amina Wanjiru',        'amina@example.com',  '+254700000001', 'PBKDF2$120000$Pt6EOSAuhTmxftMr6EavEg==$UTV3Im63Oh8F00BrzoAM3pUV5FYVCxx1YRSPM7+dvS8=', 'STUDENT'),
 (6, 'Brian Otieno',         'brian@example.com',  '+254700000002', 'PBKDF2$120000$Pt6EOSAuhTmxftMr6EavEg==$UTV3Im63Oh8F00BrzoAM3pUV5FYVCxx1YRSPM7+dvS8=', 'STUDENT'),
 (7, 'Chloe Mutua',          'chloe@example.com',  '+254700000003', 'PBKDF2$120000$Pt6EOSAuhTmxftMr6EavEg==$UTV3Im63Oh8F00BrzoAM3pUV5FYVCxx1YRSPM7+dvS8=', 'STUDENT'),
 (8, 'David Kamau',          'david@example.com',  '+254700000004', 'PBKDF2$120000$Pt6EOSAuhTmxftMr6EavEg==$UTV3Im63Oh8F00BrzoAM3pUV5FYVCxx1YRSPM7+dvS8=', 'STUDENT');

INSERT INTO counsellors (counsellor_id, user_id, specialization, qualification, gender, religion) VALUES
 (1, 2, 'Academic stress and study skills',  'MSc Counselling Psychology', 'Female', 'Christian'),
 (2, 3, 'Anxiety and depression',            'MA Clinical Psychology',     'Male',   'Christian'),
 (3, 4, 'Relationships and life transitions','MSc Counselling Psychology', 'Female', 'Muslim');

INSERT INTO students (student_id, user_id, registration_number, course, year_of_study) VALUES
 (1, 5, '650001', 'BSc Information Systems', 3),
 (2, 6, '650002', 'BA Psychology',           2),
 (3, 7, '650003', 'BSc Applied Statistics',  1),
 (4, 8, '650004', 'BBA Finance',             4);

-- appointments: past ones are completed, upcoming ones are pending or approved
INSERT INTO appointments (appointment_id, student_id, counsellor_id, appointment_date, appointment_time, reason, status) VALUES
 (1, 1, 1, (SELECT d - INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 1 WHEN 6 THEN 2 ELSE 0 END) DAY FROM (SELECT CURDATE() - INTERVAL 9 DAY AS d) x),  '10:00:00', 'Exam stress',                'COMPLETED'),
 (2, 2, 2, (SELECT d - INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 1 WHEN 6 THEN 2 ELSE 0 END) DAY FROM (SELECT CURDATE() - INTERVAL 6 DAY AS d) x),  '14:00:00', 'Adjusting to campus life',   'COMPLETED'),
 (3, 3, 2, (SELECT d - INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 1 WHEN 6 THEN 2 ELSE 0 END) DAY FROM (SELECT CURDATE() - INTERVAL 3 DAY AS d) x),  '09:00:00', 'Feeling overwhelmed',        'CANCELLED'),
 (4, 1, 1, (SELECT d + INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 2 WHEN 6 THEN 1 ELSE 0 END) DAY FROM (SELECT CURDATE() + INTERVAL 1 DAY AS d) x),   '09:00:00', 'Follow-up on study plan',    'APPROVED'),
 (5, 3, 1, (SELECT d + INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 2 WHEN 6 THEN 1 ELSE 0 END) DAY FROM (SELECT CURDATE() + INTERVAL 2 DAY AS d) x),   '11:00:00', 'Time management',            'PENDING'),
 (6, 4, 3, (SELECT d + INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 2 WHEN 6 THEN 1 ELSE 0 END) DAY FROM (SELECT CURDATE() + INTERVAL 2 DAY AS d) x),   '15:00:00', 'Relationship concerns',      'PENDING'),
 (7, 2, 2, (SELECT d + INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 2 WHEN 6 THEN 1 ELSE 0 END) DAY FROM (SELECT CURDATE() + INTERVAL 3 DAY AS d) x),   '10:00:00', 'Sleep and anxiety',          'APPROVED');

INSERT INTO counselling_sessions (appointment_id, presenting_concern, counselling_goals, session_notes, action_plan, duration_minutes, follow_up_date) VALUES
 (1, 'Anxiety before exams', 'Build a weekly revision plan', 'Discussed the exam timetable and short study blocks with breaks.', 'Follow the plan and track sleep', 45, (SELECT d + INTERVAL (CASE WEEKDAY(d) WHEN 5 THEN 2 WHEN 6 THEN 1 ELSE 0 END) DAY FROM (SELECT CURDATE() + INTERVAL 1 DAY AS d) x)),
 (2, 'Settling into campus', 'Build a support network',      'Talked about joining clubs and meeting classmates.',            'Attend two club meetings',      40, NULL);

INSERT INTO feedback (appointment_id, rating, comments) VALUES
 (1, 5, 'Felt heard and left with a clear plan.'),
 (2, 4, 'Helpful, I would like longer sessions.');
