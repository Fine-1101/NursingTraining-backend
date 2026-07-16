-- Existing databases must run this once before deploying the difficulty-aware draw rules.
-- Legacy rows retain NULL difficulty and continue drawing from all difficulty levels.
ALTER TABLE assessment_draw_rule
    DROP INDEX uk_assessment_question_type,
    ADD COLUMN difficulty TINYINT NULL COMMENT '难度：1-简单 2-中等 3-困难；NULL表示不限难度' AFTER question_type,
    ADD UNIQUE KEY uk_assessment_question_type_difficulty (assessment_id, question_type, difficulty);

ALTER TABLE assessment
    ADD COLUMN difficulty_level TINYINT NOT NULL DEFAULT 2 COMMENT '考核难度：1-简单 2-中等 3-困难' AFTER max_attempts;
