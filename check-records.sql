SELECT action_type, resource_type, COUNT(*) as cnt FROM user_learning_record GROUP BY action_type, resource_type ORDER BY action_type, resource_type;
SELECT '--- action_type=3 details ---';
SELECT id, user_id, course_id, action_type, resource_type, resource_id, title, created_at FROM user_learning_record WHERE action_type = 3 ORDER BY created_at DESC LIMIT 30;
SELECT '--- total records per user ---';
SELECT user_id, COUNT(*) as total, SUM(CASE WHEN action_type=3 THEN 1 ELSE 0 END) as action3_count FROM user_learning_record GROUP BY user_id;
