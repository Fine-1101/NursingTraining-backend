-- 学员端课程学习页：测试数据初始化脚本（严格匹配 nursing7.0 表结构）

USE nursing;

-- ============================================================
-- 1. 课程
-- ============================================================
INSERT INTO course (id, title, summary, learning_objective, cover_url, scope_type, category_id,
                    completion_rule, status, created_by, published_at, created_at, updated_at)
VALUES
  (101, '静脉输液护理规范与实践',
   '学员端课程学习测试专用课程。',
   '掌握静脉输液的规范操作与并发症防治。',
   'https://oss.example.com/courses/covers/101.jpg',
   1, NULL,
   1,
   1, 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 2. 课程发布到部门 1
-- ============================================================
INSERT IGNORE INTO course_department (department_id, course_id, required, created_at)
VALUES (1, 101, 1, NOW());

-- ============================================================
-- 3. 章节
-- ============================================================
INSERT INTO course_chapter (id, course_id, title, sort, status, created_at, updated_at)
VALUES
  (201, 101, '第1章 静脉输液基础', 1, 1, NOW(), NOW()),
  (202, 101, '第2章 无菌技术操作',   2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 4. 课程点（必修 required=1）
-- ============================================================
INSERT INTO course_point (id, course_id, chapter_id, title, description, required, sort,
                          status, created_at, updated_at)
VALUES
  (10001, 101, 201, '静脉输液概述',           '讲解静脉输液的定义、目的与适应症。',           1, 1, 1, NOW(), NOW()),
  (10002, 101, 202, '无菌持物钳的使用方法', '学习无菌持物钳的正确使用方法。',               1, 1, 1, NOW(), NOW()),
  (10003, 101, 202, '无菌操作流程演示',     '完整演示无菌操作从准备到收尾的全部流程。',     1, 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 5. 课件：视频
-- ============================================================
INSERT INTO video (id, title, description, cover_url, video_url, original_name, duration, file_size,
                   allow_drag, allow_speed, status, published_at, created_by, created_at, updated_at, uploaded_at)
VALUES
  (501, '无菌持物钳使用演示',
   '演示无菌持物钳的规范操作流程。',
   'https://oss.example.com/videos/covers/501.jpg',
   'https://oss.example.com/videos/originals/501.mp4',
   '501.mp4',
   516, 32000000,
   0, 1,
   1, NOW(), 1, NOW(), NOW(), NOW()),
  (502, '静脉输液概述精讲',
   '由专家讲解静脉输液的历史、现状与临床意义。',
   'https://oss.example.com/videos/covers/502.jpg',
   'https://oss.example.com/videos/originals/502.mp4',
   '502.mp4',
   720, 45000000,
   0, 1,
   1, NOW(), 1, NOW(), NOW(), NOW()),
  (503, '无菌操作全流程演示',
   '从手消毒到器械整理，完整演示无菌操作。',
   'https://oss.example.com/videos/covers/503.jpg',
   'https://oss.example.com/videos/originals/503.mp4',
   '503.mp4',
   600, 38000000,
   0, 1,
   1, NOW(), 1, NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 6. 课件：文章
-- ============================================================
INSERT INTO article (id, title, summary, content, cover_url, attachment_name, attachment_url, allow_download,
                     status, published_at, created_by, created_at, updated_at)
VALUES
  (701, '无菌持物钳操作注意事项',
   '介绍无菌持物钳使用前、中、后的注意事项。',
   '<p>无菌持物钳使用前应检查包装完整性...</p><p>使用中注意保持钳端向下...</p><p>使用后及时归位并记录。</p>',
   NULL,
   '无菌操作规范.pdf',
   'https://oss.example.com/articles/attachments/701.pdf',
   0,
   1, NOW(), 1, NOW(), NOW()),
  (702, '静脉输液适应症与禁忌症',
   '总结静脉输液的临床适应症与严格禁忌症。',
   '<h2>一、适应症</h2><ul><li>大出血</li><li>严重感染</li></ul><h2>二、禁忌症</h2><ul><li>严重凝血障碍</li></ul>',
   NULL,
   NULL, NULL, 0,
   1, NOW(), 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 7. 课件：PPT
-- ============================================================
INSERT INTO ppt (id, title, description, cover_url, file_url, original_url, original_name, page_count, file_size,
                 allow_download, status, created_by, published_at, uploaded_at, created_at, updated_at)
VALUES
  (801, '无菌技术操作课件',
   '无菌操作流程与常见问题。',
   'https://oss.example.com/ppts/covers/801.jpg',
   'https://oss.example.com/ppts/previews/801.pdf',
   'https://oss.example.com/ppts/originals/801.pptx',
   '801.pptx',
   26, 5200000,
   0, 1, 1, NOW(), NOW(), NOW(), NOW()),
  (802, '静脉输液并发症防控',
   '总结静脉输液常见并发症与护理对策。',
   'https://oss.example.com/ppts/covers/802.jpg',
   'https://oss.example.com/ppts/previews/802.pdf',
   'https://oss.example.com/ppts/originals/802.pptx',
   '802.pptx',
   18, 3600000,
   0, 1, 1, NOW(), NOW(), NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();

-- ============================================================
-- 8. 绑定：课程点 ←→ 视频
-- ============================================================
INSERT IGNORE INTO course_point_video (course_point_id, video_id, sort, created_at) VALUES
  (10002, 501, 1, NOW()),
  (10001, 502, 1, NOW()),
  (10003, 503, 1, NOW());

-- ============================================================
-- 9. 绑定：课程点 ←→ 文章
-- ============================================================
INSERT IGNORE INTO course_point_article (course_point_id, article_id, sort, created_at) VALUES
  (10002, 701, 1, NOW()),
  (10001, 702, 1, NOW());

-- ============================================================
-- 10. 绑定：课程点 ←→ PPT
-- ============================================================
INSERT IGNORE INTO course_point_ppt (course_point_id, ppt_id, sort, created_at) VALUES
  (10002, 801, 1, NOW()),
  (10001, 802, 1, NOW());

-- ============================================================
-- 校验结果
-- ============================================================
SELECT 'Study page seed data ready.' AS status;
SELECT c.id AS course_id, c.title,
       COUNT(DISTINCT cp.id) AS point_count
FROM course c
LEFT JOIN course_point cp ON cp.course_id = c.id
WHERE c.id = 101
GROUP BY c.id, c.title;
