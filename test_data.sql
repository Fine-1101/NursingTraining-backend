-- 1. 创建管理员账号 (如果不存在)
INSERT IGNORE INTO user (username, password, real_name, role_type, status, dept_id, created_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '系统管理员', 0, 1, NULL, NOW());

-- 2. 创建学员 (BCrypt encoded '123456')
INSERT INTO user (username, password, real_name, role_type, status, dept_id, phone, created_at) VALUES
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '张三', 1, 1, 1, '13800001001', NOW()),
('lisi',     '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '李四', 1, 1, 2, '13800001002', NOW()),
('wangwu',   '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '王五', 1, 1, 3, '13800001003', NOW()),
('zhaoliu',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '赵六', 1, 1, 5, '13800001004', NOW()),
('sunqi',    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '孙七', 1, 1, 7, '13800001005', NOW());

-- 3. 课程关联科室 (所有已发布课程关联到所有科室)
INSERT IGNORE INTO course_department (course_id, department_id, required) VALUES
(2,1,1),(2,2,1),(2,3,1),(2,4,1),(2,5,1),(2,6,1),(2,7,1),(2,8,1),
(4,1,1),(4,2,1),(4,3,1),(4,4,1),(4,5,1),(4,6,1),(4,7,1),(4,8,1),
(5,1,1),(5,2,1),(5,3,1),(5,4,1),(5,5,1),(5,6,1),(5,7,1),(5,8,1),
(6,1,1),(6,2,1),(6,3,1),(6,4,1),(6,5,1),(6,6,1),(6,7,1),(6,8,1);

-- 4. 为学员添加课程进度
INSERT IGNORE INTO user_course_progress (user_id, course_id, progress_percent, status, started_at, completed_at, created_at) VALUES
(2, 4, 100.00, 2, NOW(), NOW(), NOW()),
(2, 5, 75.00,  1, NOW(), NULL,    NOW()),
(2, 6, 50.00,  1, NOW(), NULL,    NOW()),
(3, 4, 100.00, 2, NOW(), NOW(), NOW()),
(3, 5, 100.00, 2, NOW(), NOW(), NOW()),
(3, 6, 30.00,  1, NOW(), NULL,    NOW()),
(4, 4, 60.00,  1, NOW(), NULL,    NOW()),
(5, 5, 100.00, 2, NOW(), NOW(), NOW()),
(6, 6, 85.00,  1, NOW(), NULL,    NOW());
