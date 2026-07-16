USE nursing;

-- 给 course_student_message 增加考核提醒关联字段
ALTER TABLE course_student_message
    ADD COLUMN assessment_id BIGINT DEFAULT NULL COMMENT '考核ID（考核提醒时使用）' AFTER course_title,
    ADD COLUMN batch_id VARCHAR(64) DEFAULT NULL COMMENT '批量发送批次号' AFTER assessment_id,
    ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '消息类型：GENERAL / ASSESSMENT_REMINDER' AFTER batch_id;

-- 建议索引
CREATE INDEX idx_message_assessment_sent
    ON course_student_message (assessment_id, created_at);

CREATE INDEX idx_message_batch
    ON course_student_message (batch_id);
