-- 创建测试课程数据（简化版）

-- 1. 查看现有数据
SELECT '=== 现有分类 ===' AS info;
SELECT id, name FROM category WHERE status = 1;

SELECT '=== 现有科室 ===' AS info;
SELECT id, name FROM department WHERE status = 1;

-- 2. 创建测试课程
INSERT INTO course (
    title, 
    summary, 
    learning_objective, 
    category_id, 
    cover_url, 
    instructor_id, 
    start_at, 
    status, 
    created_by, 
    created_at, 
    updated_at
) VALUES 
(
    '护理基础技能培训',
    '本课程系统讲解护理工作中的基础技能，包括患者评估、生命体征测量、无菌技术等核心内容。',
    '掌握护理基础操作规范，提升临床护理能力，确保护理安全。',
    3,  -- 临床护理分类
    '/uploads/nursing-training/courses/covers/default.jpg',
    NULL,
    NOW(),
    1,  -- 状态：1-已发布
    1,  -- 创建人ID
    NOW(),
    NOW()
),
(
    '急救护理技术',
    '针对急诊和危重患者的护理技术，包括心肺复苏、创伤救护、急性中毒处理等紧急情况的应对方法。',
    '熟练掌握急救护理流程，提高应急处理能力，为患者争取宝贵的抢救时间。',
    3,
    '/uploads/nursing-training/courses/covers/default.jpg',
    NULL,
    NOW(),
    1,
    1,
    NOW(),
    NOW()
),
(
    '内科护理学',
    '涵盖呼吸系统、循环系统、消化系统等常见内科疾病的护理要点和健康教育知识。',
    '理解内科疾病护理原则，能够制定个性化护理方案，提供全面的内科护理服务。',
    3,
    '/uploads/nursing-training/courses/covers/default.jpg',
    NULL,
    NOW(),
    1,
    1,
    NOW(),
    NOW()
);

-- 3. 获取新创建的课程ID
SET @course1_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 2);
SET @course2_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @course3_id = (SELECT id FROM course ORDER BY id DESC LIMIT 0);

-- 4. 为课程添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course1_id, '第一章：患者评估', 1, NOW()),
(@course1_id, '第二章：生命体征', 2, NOW()),
(@course2_id, '第一章：心肺复苏', 1, NOW()),
(@course2_id, '第二章：创伤救护', 2, NOW()),
(@course3_id, '第一章：呼吸系统', 1, NOW()),
(@course3_id, '第二章：循环系统', 2, NOW());

-- 5. 为章节添加学习点
SET @ch1_1 = (SELECT id FROM course_chapter WHERE course_id = @course1_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @ch1_2 = (SELECT id FROM course_chapter WHERE course_id = @course1_id ORDER BY id DESC LIMIT 1 OFFSET 0);
SET @ch2_1 = (SELECT id FROM course_chapter WHERE course_id = @course2_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @ch2_2 = (SELECT id FROM course_chapter WHERE course_id = @course2_id ORDER BY id DESC LIMIT 1 OFFSET 0);
SET @ch3_1 = (SELECT id FROM course_chapter WHERE course_id = @course3_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @ch3_2 = (SELECT id FROM course_chapter WHERE course_id = @course3_id ORDER BY id DESC LIMIT 1 OFFSET 0);

INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@ch1_1, '患者一般情况评估', '学习如何全面评估患者的基本信息和身体状况', 1, 1, NOW()),
(@ch1_1, '心理社会评估', '掌握患者心理状态和社会支持的评估方法', 1, 2, NOW()),
(@ch1_2, '体温测量技术', '规范体温测量的操作流程和注意事项', 1, 1, NOW()),
(@ch1_2, '脉搏与呼吸测量', '学习脉搏和呼吸频率的准确测量方法', 1, 2, NOW()),
(@ch2_1, 'CPR操作流程', '学习标准的心肺复苏操作步骤', 1, 1, NOW()),
(@ch2_1, 'AED使用方法', '掌握自动体外除颤器的使用技巧', 1, 2, NOW()),
(@ch2_2, '止血技术', '学习各种止血方法和适用场景', 1, 1, NOW()),
(@ch2_2, '包扎固定技术', '掌握创伤包扎和骨折固定的方法', 1, 2, NOW()),
(@ch3_1, '肺炎患者护理', '了解肺炎患者的护理要点和健康教育', 1, 1, NOW()),
(@ch3_1, '慢阻肺患者护理', '掌握慢性阻塞性肺疾病的护理方法', 1, 2, NOW()),
(@ch3_2, '高血压患者护理', '学习高血压患者的护理和健康管理', 1, 1, NOW()),
(@ch3_2, '冠心病患者护理', '掌握冠心病患者的护理要点', 1, 2, NOW());

-- 6. 为课程分配科室（让学员可以学习这些课程）
INSERT INTO course_department (course_id, department_id, required) VALUES
(@course1_id, 1, 1),  -- 内科必学
(@course1_id, 2, 1),  -- 外科必学
(@course2_id, 1, 1),
(@course2_id, 2, 1),
(@course3_id, 1, 1);  -- 内科护理主要给内科

-- 7. 验证结果
SELECT '=== 创建的课程 ===' AS info;
SELECT id, title, status FROM course WHERE id IN (@course1_id, @course2_id, @course3_id);

SELECT '=== 章节统计 ===' AS info;
SELECT c.title AS '课程名称', COUNT(ch.id) AS '章节数'
FROM course c
LEFT JOIN course_chapter ch ON c.id = ch.course_id
WHERE c.id IN (@course1_id, @course2_id, @course3_id)
GROUP BY c.id, c.title;

SELECT '=== 学习点统计 ===' AS info;
SELECT c.title AS '课程名称', COUNT(p.id) AS '学习点数'
FROM course c
LEFT JOIN course_chapter ch ON c.id = ch.course_id
LEFT JOIN course_point p ON ch.id = p.chapter_id
WHERE c.id IN (@course1_id, @course2_id, @course3_id)
GROUP BY c.id, c.title;

SELECT '=== 完成！===' AS result;
SELECT '现在刷新前端页面，选择学员后应该能看到课程进度图表了' AS message;
