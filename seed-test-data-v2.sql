-- ============================================
-- Nursing Training - Test Data Seed v2
-- ============================================

-- 1. Insert more courses (published at different times for trend testing)
INSERT INTO course (title, summary, learning_objective, scope_type, category_id, completion_rule, status, created_by, published_at, created_at)
VALUES
('ICU Monitoring Basics', 'ICU monitoring fundamentals', 'Master ICU equipment usage', 1, 101, 1, 1, 8, '2026-02-15 10:00:00', '2026-02-10 09:00:00'),
('Surgical Nursing Practice', 'Pre and post operative care', 'Learn surgical nursing skills', 1, 101, 1, 1, 8, '2026-03-10 10:00:00', '2026-03-05 09:00:00'),
('Emergency Response Training', 'Emergency response protocols', 'Master emergency procedures', 1, 102, 1, 1, 8, '2026-04-08 10:00:00', '2026-04-03 09:00:00'),
('Pediatric Care Essentials', 'Pediatric nursing basics', 'Understand child patient care', 1, 103, 1, 1, 8, '2026-05-12 10:00:00', '2026-05-07 09:00:00'),
('Vital Signs Measurement', 'Accurate vital signs taking', 'Master vital signs equipment', 1, 104, 1, 1, 8, '2026-06-05 10:00:00', '2026-06-01 09:00:00'),
('Medication Administration', 'Safe drug administration', 'Learn medication protocols', 1, 105, 1, 1, 8, '2026-06-20 10:00:00', '2026-06-15 09:00:00'),
('Cardiac Rehabilitation', 'Heart disease patient care', 'Cardiac rehab techniques', 1, 106, 1, 1, 8, '2026-07-01 10:00:00', '2026-06-28 09:00:00'),
('Wound Care Management', 'Modern wound treatment', 'Wound assessment and care', 1, 107, 1, 1, 8, '2026-07-08 10:00:00', '2026-07-05 09:00:00');

SET @c_icu = (SELECT id FROM course WHERE title='ICU Monitoring Basics' LIMIT 1);
SET @c_surg = (SELECT id FROM course WHERE title='Surgical Nursing Practice' LIMIT 1);
SET @c_emerg = (SELECT id FROM course WHERE title='Emergency Response Training' LIMIT 1);
SET @c_pedia = (SELECT id FROM course WHERE title='Pediatric Care Essentials' LIMIT 1);
SET @c_vital = (SELECT id FROM course WHERE title='Vital Signs Measurement' LIMIT 1);
SET @c_med = (SELECT id FROM course WHERE title='Medication Administration' LIMIT 1);
SET @c_card = (SELECT id FROM course WHERE title='Cardiac Rehabilitation' LIMIT 1);
SET @c_wound = (SELECT id FROM course WHERE title='Wound Care Management' LIMIT 1);

-- 2. Chapters and points for each new course
INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_icu, 'Ch1 ICU Overview', 1, 1, '2026-02-10 10:00:00'),
(@c_icu, 'Ch2 Monitoring Equipment', 2, 1, '2026-02-10 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_icu AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_icu, @ch, 'ICU Intro Video', 1, 1, 1, '2026-02-10 10:00:00'),
(@c_icu, @ch, 'ICU Standards Article', 1, 2, 1, '2026-02-10 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_icu AND sort=2 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_icu, @ch, 'Monitor Guide', 1, 1, 1, '2026-02-10 10:00:00'),
(@c_icu, @ch, 'Alarm Settings PPT', 1, 2, 1, '2026-02-10 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_surg, 'Ch1 Pre-op Care', 1, 1, '2026-03-05 10:00:00'),
(@c_surg, 'Ch2 Post-op Care', 2, 1, '2026-03-05 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_surg AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_surg, @ch, 'Pre-op Checklist Video', 1, 1, 1, '2026-03-05 10:00:00'),
(@c_surg, @ch, 'Patient Assessment', 1, 2, 1, '2026-03-05 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_surg AND sort=2 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_surg, @ch, 'Post-op Monitoring', 1, 1, 1, '2026-03-05 10:00:00'),
(@c_surg, @ch, 'Pain Management PPT', 1, 2, 1, '2026-03-05 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_emerg, 'Ch1 CPR Basics', 1, 1, '2026-04-03 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_emerg AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_emerg, @ch, 'CPR Demo Video', 1, 1, 1, '2026-04-03 10:00:00'),
(@c_emerg, @ch, 'AED Guide', 1, 2, 1, '2026-04-03 10:00:00'),
(@c_emerg, @ch, 'Emergency Protocol', 1, 3, 1, '2026-04-03 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_pedia, 'Ch1 Child Assessment', 1, 1, '2026-05-07 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_pedia AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_pedia, @ch, 'Pediatric Assessment Video', 1, 1, 1, '2026-05-07 10:00:00'),
(@c_pedia, @ch, 'Growth Milestones', 1, 2, 1, '2026-05-07 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_vital, 'Ch1 BP Measurement', 1, 1, '2026-06-01 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_vital AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_vital, @ch, 'BP Technique Video', 1, 1, 1, '2026-06-01 10:00:00'),
(@c_vital, @ch, 'Temperature Guide', 1, 2, 1, '2026-06-01 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_med, 'Ch1 Drug Safety', 1, 1, '2026-06-15 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_med AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_med, @ch, 'Drug Classification PPT', 1, 1, 1, '2026-06-15 10:00:00'),
(@c_med, @ch, 'IV Administration Video', 1, 2, 1, '2026-06-15 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_card, 'Ch1 Heart Assessment', 1, 1, '2026-06-28 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_card AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_card, @ch, 'ECG Basics Video', 1, 1, 1, '2026-06-28 10:00:00'),
(@c_card, @ch, 'Cardiac Markers Article', 1, 2, 1, '2026-06-28 10:00:00');

INSERT INTO course_chapter (course_id, title, sort, status, created_at) VALUES
(@c_wound, 'Ch1 Wound Types', 1, 1, '2026-07-05 10:00:00');
SET @ch = (SELECT id FROM course_chapter WHERE course_id=@c_wound AND sort=1 LIMIT 1);
INSERT INTO course_point (course_id, chapter_id, title, required, sort, status, created_at) VALUES
(@c_wound, @ch, 'Wound Classification PPT', 1, 1, 1, '2026-07-05 10:00:00'),
(@c_wound, @ch, 'Dressing Techniques Video', 1, 2, 1, '2026-07-05 10:00:00');

-- 3. Course-Tag associations
INSERT IGNORE INTO course_tag (course_id, tag_id) VALUES
(5, 100), (5, 101),
(@c_icu, 103), (@c_icu, 104),
(@c_surg, 100), (@c_surg, 103),
(@c_emerg, 103), (@c_emerg, 101),
(@c_pedia, 100), (@c_pedia, 105),
(@c_vital, 102), (@c_vital, 100),
(@c_med, 101), (@c_med, 100),
(@c_card, 103), (@c_card, 100),
(@c_wound, 104), (@c_wound, 100);

-- 4. Student enrollments spread across months
-- Feb 2026
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_icu, 2, 100.00, '2026-02-16 09:00:00', '2026-02-20 15:00:00', '2026-02-16 09:00:00'),
(6, @c_icu, 2, 100.00, '2026-02-17 10:00:00', '2026-02-22 14:00:00', '2026-02-17 10:00:00'),
(9, @c_icu, 1, 50.00, '2026-02-18 08:00:00', NULL, '2026-02-18 08:00:00'),
(10, @c_icu, 2, 100.00, '2026-02-19 09:30:00', '2026-02-25 16:00:00', '2026-02-19 09:30:00'),
(11, @c_icu, 1, 75.00, '2026-02-20 11:00:00', NULL, '2026-02-20 11:00:00');

-- Mar 2026
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_surg, 2, 100.00, '2026-03-11 09:00:00', '2026-03-18 15:00:00', '2026-03-11 09:00:00'),
(6, @c_surg, 2, 100.00, '2026-03-12 10:00:00', '2026-03-19 14:00:00', '2026-03-12 10:00:00'),
(12, @c_surg, 1, 60.00, '2026-03-13 08:00:00', NULL, '2026-03-13 08:00:00'),
(13, @c_surg, 2, 100.00, '2026-03-14 09:00:00', '2026-03-20 16:00:00', '2026-03-14 09:00:00'),
(14, @c_surg, 1, 30.00, '2026-03-15 10:00:00', NULL, '2026-03-15 10:00:00'),
(15, @c_surg, 2, 100.00, '2026-03-11 14:00:00', '2026-03-17 11:00:00', '2026-03-11 14:00:00');

-- Apr 2026
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_emerg, 2, 100.00, '2026-04-09 09:00:00', '2026-04-15 15:00:00', '2026-04-09 09:00:00'),
(7, @c_emerg, 2, 100.00, '2026-04-10 10:00:00', '2026-04-16 14:00:00', '2026-04-10 10:00:00'),
(9, @c_emerg, 2, 100.00, '2026-04-11 08:00:00', '2026-04-17 16:00:00', '2026-04-11 08:00:00'),
(10, @c_emerg, 1, 40.00, '2026-04-12 09:30:00', NULL, '2026-04-12 09:30:00'),
(16, @c_emerg, 2, 100.00, '2026-04-13 11:00:00', '2026-04-18 15:00:00', '2026-04-13 11:00:00'),
(17, @c_emerg, 1, 55.00, '2026-04-14 09:00:00', NULL, '2026-04-14 09:00:00'),
(18, @c_emerg, 2, 100.00, '2026-04-15 10:00:00', '2026-04-20 12:00:00', '2026-04-15 10:00:00');

-- May 2026
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_pedia, 2, 100.00, '2026-05-13 09:00:00', '2026-05-20 15:00:00', '2026-05-13 09:00:00'),
(6, @c_pedia, 1, 50.00, '2026-05-14 10:00:00', NULL, '2026-05-14 10:00:00'),
(11, @c_pedia, 2, 100.00, '2026-05-15 08:00:00', '2026-05-22 14:00:00', '2026-05-15 08:00:00'),
(12, @c_pedia, 2, 100.00, '2026-05-16 09:00:00', '2026-05-23 16:00:00', '2026-05-16 09:00:00'),
(19, @c_pedia, 1, 25.00, '2026-05-17 10:00:00', NULL, '2026-05-17 10:00:00'),
(20, @c_pedia, 2, 100.00, '2026-05-18 11:00:00', '2026-05-25 15:00:00', '2026-05-18 11:00:00');

-- Jun 2026
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_vital, 2, 100.00, '2026-06-06 09:00:00', '2026-06-12 15:00:00', '2026-06-06 09:00:00'),
(6, @c_vital, 2, 100.00, '2026-06-07 10:00:00', '2026-06-13 14:00:00', '2026-06-07 10:00:00'),
(9, @c_vital, 2, 100.00, '2026-06-08 08:00:00', '2026-06-14 16:00:00', '2026-06-08 08:00:00'),
(13, @c_vital, 1, 60.00, '2026-06-09 09:30:00', NULL, '2026-06-09 09:30:00'),
(14, @c_vital, 2, 100.00, '2026-06-10 11:00:00', '2026-06-16 15:00:00', '2026-06-10 11:00:00'),
(15, @c_vital, 2, 100.00, '2026-06-11 09:00:00', '2026-06-17 12:00:00', '2026-06-11 09:00:00'),
(16, @c_vital, 1, 45.00, '2026-06-12 10:00:00', NULL, '2026-06-12 10:00:00'),
(5, @c_med, 2, 100.00, '2026-06-21 09:00:00', '2026-06-28 15:00:00', '2026-06-21 09:00:00'),
(6, @c_med, 1, 70.00, '2026-06-22 10:00:00', NULL, '2026-06-22 10:00:00'),
(10, @c_med, 2, 100.00, '2026-06-23 08:00:00', '2026-06-30 14:00:00', '2026-06-23 08:00:00'),
(17, @c_med, 2, 100.00, '2026-06-24 09:00:00', '2026-07-01 16:00:00', '2026-06-24 09:00:00'),
(18, @c_med, 1, 35.00, '2026-06-25 10:00:00', NULL, '2026-06-25 10:00:00');

-- Jul 2026 (current month)
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(5, @c_card, 1, 60.00, '2026-07-02 09:00:00', NULL, '2026-07-02 09:00:00'),
(6, @c_card, 2, 100.00, '2026-07-03 10:00:00', '2026-07-10 14:00:00', '2026-07-03 10:00:00'),
(9, @c_card, 1, 30.00, '2026-07-05 08:00:00', NULL, '2026-07-05 08:00:00'),
(11, @c_card, 2, 100.00, '2026-07-06 09:00:00', '2026-07-12 16:00:00', '2026-07-06 09:00:00'),
(12, @c_card, 1, 45.00, '2026-07-07 10:00:00', NULL, '2026-07-07 10:00:00'),
(5, @c_wound, 1, 40.00, '2026-07-09 09:00:00', NULL, '2026-07-09 09:00:00'),
(6, @c_wound, 1, 25.00, '2026-07-10 10:00:00', NULL, '2026-07-10 10:00:00'),
(13, @c_wound, 2, 100.00, '2026-07-08 08:00:00', '2026-07-13 15:00:00', '2026-07-08 08:00:00'),
(14, @c_wound, 1, 50.00, '2026-07-11 09:30:00', NULL, '2026-07-11 09:30:00'),
(15, @c_wound, 1, 20.00, '2026-07-12 11:00:00', NULL, '2026-07-12 11:00:00');

-- Additional course 5 enrollments
INSERT INTO user_course_progress (user_id, course_id, status, progress_percent, started_at, completed_at, created_at) VALUES
(11, 5, 2, 100.00, '2026-03-01 09:00:00', '2026-03-10 15:00:00', '2026-03-01 09:00:00'),
(12, 5, 1, 50.00, '2026-04-05 10:00:00', NULL, '2026-04-05 10:00:00'),
(13, 5, 2, 100.00, '2026-05-10 08:00:00', '2026-05-18 14:00:00', '2026-05-10 08:00:00'),
(14, 5, 1, 30.00, '2026-06-15 09:00:00', NULL, '2026-06-15 09:00:00'),
(15, 5, 2, 100.00, '2026-07-01 10:00:00', '2026-07-08 16:00:00', '2026-07-01 10:00:00');

-- 5. Learning records across time
SET @p1 = (SELECT id FROM course_point WHERE course_id=5 AND sort=1 LIMIT 1);
SET @p2 = (SELECT id FROM course_point WHERE course_id=5 AND sort=2 LIMIT 1);
SET @p3 = (SELECT id FROM course_point WHERE course_id=5 AND sort=3 LIMIT 1);

INSERT INTO user_learning_record (user_id, course_id, course_point_id, resource_type, action_type, title, description, created_at) VALUES
(5, 5, @p1, 1, 1, 'Watched video', 'Completed basics video', '2026-02-15 10:30:00'),
(6, 5, @p1, 1, 1, 'Watched video', 'Completed basics video', '2026-02-16 11:00:00'),
(9, 5, @p2, 2, 1, 'Read article', 'Finished standards article', '2026-03-10 14:00:00'),
(5, 5, @p2, 2, 1, 'Read article', 'Finished standards article', '2026-03-12 09:30:00'),
(10, 5, @p3, 3, 1, 'Viewed PPT', 'Reviewed emergency drug PPT', '2026-04-08 15:00:00'),
(6, 5, @p1, 1, 1, 'Watched video', 'Re-watched basics', '2026-04-20 10:00:00'),
(11, 5, @p1, 1, 1, 'Watched video', 'Started nursing basics', '2026-05-05 09:00:00'),
(12, 5, @p2, 2, 1, 'Read article', 'Standards review', '2026-05-15 14:30:00'),
(13, 5, @p1, 1, 1, 'Watched video', 'Completed video module', '2026-06-02 10:00:00'),
(14, 5, @p3, 3, 1, 'Viewed PPT', 'PPT review session', '2026-06-18 11:00:00'),
(5, 5, @p3, 3, 1, 'Viewed PPT', 'Reviewed emergency PPT', '2026-06-25 15:00:00'),
(15, 5, @p1, 1, 1, 'Watched video', 'Video completed', '2026-07-01 09:30:00'),
(6, 5, @p2, 2, 1, 'Read article', 'Article review', '2026-07-05 10:00:00'),
(9, 5, @p1, 1, 1, 'Watched video', 'Video session', '2026-07-08 14:00:00'),
(10, 5, @p2, 2, 1, 'Read article', 'Reading completion', '2026-07-10 11:30:00'),
(5, 5, @p1, 1, 1, 'Watched video', 'Quick review', '2026-07-12 09:00:00'),
(13, 5, @p3, 3, 1, 'Viewed PPT', 'PPT study session', '2026-07-13 10:00:00');

-- 6. Update course 5 category
UPDATE course SET category_id = 100 WHERE id = 5;

-- Summary
SELECT '=== Test Data Summary ===' AS info;
SELECT COUNT(*) AS total_published_courses FROM course WHERE status = 1;
SELECT COUNT(*) AS total_enrollments FROM user_course_progress;
SELECT COUNT(*) AS total_learning_records FROM user_learning_record;
SELECT COUNT(*) AS total_students FROM user WHERE role_type = 1;
