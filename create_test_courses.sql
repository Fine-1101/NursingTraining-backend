-- 创建测试课程数据
-- 用于学员学习进度功能测试

-- 1. 先检查是否有可用的分类和讲师
SELECT '=== 现有分类 ===' AS info;
SELECT id, name FROM category WHERE status = 1 LIMIT 5;

SELECT '=== 现有讲师 ===' AS info;
SELECT id, real_name FROM user WHERE role_type = 'INSTRUCTOR' AND status = 1 LIMIT 5;

SELECT '=== 现有科室 ===' AS info;
SELECT id, department_name FROM department WHERE status = 1 LIMIT 5;

-- 2. 创建测试课程（假设 categoryId=1, instructorId=1, 请根据实际情况修改）
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
    1,  -- 请根据实际分类ID修改
    '/static/images/default-course-cover.jpg',
    1,  -- 请根据实际讲师ID修改
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
    1,
    '/static/images/default-course-cover.jpg',
    1,
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
    1,
    '/static/images/default-course-cover.jpg',
    1,
    NOW(),
    1,
    1,
    NOW(),
    NOW()
),
(
    '外科护理实践',
    '外科手术前后护理、伤口护理、引流管管理等外科专科护理技能的系统培训。',
    '掌握外科护理核心技术，熟悉围手术期护理流程，保障患者手术安全。',
    1,
    '/static/images/default-course-cover.jpg',
    1,
    NOW(),
    1,
    1,
    NOW(),
    NOW()
),
(
    '老年护理与康复',
    '针对老年人生理特点的护理方法，包括慢性病管理、康复训练、心理护理等内容。',
    '了解老年护理特点，掌握康复护理技能，提高老年人生活质量。',
    1,
    '/static/images/default-course-cover.jpg',
    1,
    NOW(),
    1,
    1,
    NOW(),
    NOW()
);

-- 3. 为课程添加章节和学习点
SELECT '=== 新创建的课程 ===' AS info;
SELECT id, title FROM course ORDER BY id DESC LIMIT 5;

-- 获取最新创建的课程ID（假设上面创建了5个课程）
SET @course1_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 4);
SET @course2_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 3);
SET @course3_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 2);
SET @course4_id = (SELECT id FROM course ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @course5_id = (SELECT id FROM course ORDER BY id DESC LIMIT 0);

-- 为课程1添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course1_id, '第一章：患者评估技术', 1, NOW()),
(@course1_id, '第二章：生命体征测量', 2, NOW()),
(@course1_id, '第三章：无菌技术操作', 3, NOW());

SET @chapter1_1 = (SELECT id FROM course_chapter WHERE course_id = @course1_id ORDER BY id DESC LIMIT 1 OFFSET 2);
SET @chapter1_2 = (SELECT id FROM course_chapter WHERE course_id = @course1_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @chapter1_3 = (SELECT id FROM course_chapter WHERE course_id = @course1_id ORDER BY id DESC LIMIT 1 OFFSET 0);

-- 为章节添加学习点
INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@chapter1_1, '患者一般情况评估', '学习如何全面评估患者的基本信息和身体状况', 1, 1, NOW()),
(@chapter1_1, '心理社会评估', '掌握患者心理状态和社会支持的评估方法', 1, 2, NOW()),
(@chapter1_2, '体温测量技术', '规范体温测量的操作流程和注意事项', 1, 1, NOW()),
(@chapter1_2, '脉搏与呼吸测量', '学习脉搏和呼吸频率的准确测量方法', 1, 2, NOW()),
(@chapter1_3, '无菌概念与原则', '理解无菌技术的基本概念和操作原则', 1, 1, NOW()),
(@chapter1_3, '无菌操作实践', '掌握常见的无菌操作技术和注意事项', 1, 2, NOW());

-- 为课程2添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course2_id, '第一章：心肺复苏技术', 1, NOW()),
(@course2_id, '第二章：创伤救护', 2, NOW());

SET @chapter2_1 = (SELECT id FROM course_chapter WHERE course_id = @course2_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @chapter2_2 = (SELECT id FROM course_chapter WHERE course_id = @course2_id ORDER BY id DESC LIMIT 1 OFFSET 0);

INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@chapter2_1, 'CPR操作流程', '学习标准的心肺复苏操作步骤', 1, 1, NOW()),
(@chapter2_1, 'AED使用方法', '掌握自动体外除颤器的使用技巧', 1, 2, NOW()),
(@chapter2_2, '止血技术', '学习各种止血方法和适用场景', 1, 1, NOW()),
(@chapter2_2, '包扎固定技术', '掌握创伤包扎和骨折固定的方法', 1, 2, NOW());

-- 为课程3添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course3_id, '第一章：呼吸系统疾病护理', 1, NOW()),
(@course3_id, '第二章：循环系统疾病护理', 2, NOW());

SET @chapter3_1 = (SELECT id FROM course_chapter WHERE course_id = @course3_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @chapter3_2 = (SELECT id FROM course_chapter WHERE course_id = @course3_id ORDER BY id DESC LIMIT 1 OFFSET 0);

INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@chapter3_1, '肺炎患者护理', '了解肺炎患者的护理要点和健康教育', 1, 1, NOW()),
(@chapter3_1, '慢阻肺患者护理', '掌握慢性阻塞性肺疾病的护理方法', 1, 2, NOW()),
(@chapter3_2, '高血压患者护理', '学习高血压患者的护理和健康管理', 1, 1, NOW()),
(@chapter3_2, '冠心病患者护理', '掌握冠心病患者的护理要点', 1, 2, NOW());

-- 为课程4添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course4_id, '第一章：围手术期护理', 1, NOW()),
(@course4_id, '第二章：伤口护理技术', 2, NOW());

SET @chapter4_1 = (SELECT id FROM course_chapter WHERE course_id = @course4_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @chapter4_2 = (SELECT id FROM course_chapter WHERE course_id = @course4_id ORDER BY id DESC LIMIT 1 OFFSET 0);

INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@chapter4_1, '术前准备与护理', '学习手术前患者的准备工作', 1, 1, NOW()),
(@chapter4_1, '术后观察与护理', '掌握手术后患者的观察要点', 1, 2, NOW()),
(@chapter4_2, '伤口评估与换药', '学习伤口评估方法和换药技术', 1, 1, NOW()),
(@chapter4_2, '引流管护理', '掌握各类引流管的护理要点', 1, 2, NOW());

-- 为课程5添加章节
INSERT INTO course_chapter (course_id, title, sort, created_at) VALUES
(@course5_id, '第一章：老年人生活护理', 1, NOW()),
(@course5_id, '第二章：康复护理技术', 2, NOW());

SET @chapter5_1 = (SELECT id FROM course_chapter WHERE course_id = @course5_id ORDER BY id DESC LIMIT 1 OFFSET 1);
SET @chapter5_2 = (SELECT id FROM course_chapter WHERE course_id = @course5_id ORDER BY id DESC LIMIT 1 OFFSET 0);

INSERT INTO course_point (chapter_id, title, description, required, sort, created_at) VALUES
(@chapter5_1, '日常生活活动训练', '帮助老年人进行日常生活活动训练', 1, 1, NOW()),
(@chapter5_1, '营养与饮食护理', '学习老年人的营养需求和饮食护理', 1, 2, NOW()),
(@chapter5_2, '运动康复训练', '掌握老年人运动康复的方法和注意事项', 1, 1, NOW()),
(@chapter5_2, '认知功能训练', '学习认知功能评估和训练方法', 1, 2, NOW());

-- 4. 验证创建结果
SELECT '=== 课程统计 ===' AS info;
SELECT 
    COUNT(*) AS '课程总数',
    SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS '已发布课程',
    SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS '草稿课程'
FROM course;

SELECT '=== 章节统计 ===' AS info;
SELECT 
    c.title AS '课程名称',
    COUNT(ch.id) AS '章节数'
FROM course c
LEFT JOIN course_chapter ch ON c.id = ch.course_id
WHERE c.id >= @course1_id
GROUP BY c.id, c.title;

SELECT '=== 学习点统计 ===' AS info;
SELECT 
    c.title AS '课程名称',
    COUNT(p.id) AS '学习点数'
FROM course c
LEFT JOIN course_chapter ch ON c.id = ch.course_id
LEFT JOIN course_point p ON ch.id = p.chapter_id
WHERE c.id >= @course1_id
GROUP BY c.id, c.title;

SELECT '=== 创建完成！===' AS result;
SELECT '现在可以在学员管理页面中看到课程进度图表了' AS message;
