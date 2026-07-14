USE nursing;

-- 考核题库：目前仅支持单选题和判断题。
CREATE TABLE IF NOT EXISTS assessment_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL COMMENT '所属课程类别ID',
    question_type TINYINT NOT NULL COMMENT '题型：1-单选题 2-判断题',
    stem TEXT NOT NULL COMMENT '题干',
    analysis TEXT DEFAULT NULL COMMENT '答案解析',
    difficulty TINYINT NOT NULL DEFAULT 2 COMMENT '难度：1-简单 2-中等 3-困难',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_question_category_type (category_id, question_type, status),
    KEY idx_question_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核题库';

CREATE TABLE IF NOT EXISTS assessment_question_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question_id BIGINT NOT NULL COMMENT '题目ID',
    option_key VARCHAR(8) NOT NULL COMMENT '选项标识，如A/B或TRUE/FALSE',
    content VARCHAR(1000) NOT NULL COMMENT '选项内容',
    is_correct TINYINT NOT NULL DEFAULT 0 COMMENT '是否正确答案：0-否 1-是',
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_option_key (question_id, option_key),
    KEY idx_option_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核题目选项';

-- 没有记录表示类别下所有课程可用；存在记录时，仅表内指定课程可用。
CREATE TABLE IF NOT EXISTS assessment_question_course (
    question_id BIGINT NOT NULL COMMENT '题目ID',
    course_id BIGINT NOT NULL COMMENT '指定可用课程ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (question_id, course_id),
    KEY idx_question_course_course (course_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目指定课程范围';

CREATE TABLE IF NOT EXISTS assessment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL COMMENT '所属课程ID',
    category_id BIGINT NOT NULL COMMENT '发布时课程类别快照',
    title VARCHAR(200) NOT NULL COMMENT '考核名称',
    description VARCHAR(1000) DEFAULT NULL COMMENT '考核说明',
    start_at DATETIME NOT NULL COMMENT '开考时间',
    end_at DATETIME DEFAULT NULL COMMENT '最晚可开考时间，为空表示不限制',
    duration_minutes INT NOT NULL COMMENT '答题时长（分钟）',
    total_score DECIMAL(6,2) NOT NULL COMMENT '总分',
    pass_score DECIMAL(6,2) NOT NULL COMMENT '及格分',
    max_attempts INT NOT NULL DEFAULT 1 COMMENT '最多考试次数',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已关闭',
    published_at DATETIME DEFAULT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_assessment_course_status (course_id, status),
    KEY idx_assessment_time (start_at, end_at),
    KEY idx_assessment_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程考核';

CREATE TABLE IF NOT EXISTS assessment_draw_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT NOT NULL COMMENT '考核ID',
    question_type TINYINT NOT NULL COMMENT '题型：1-单选题 2-判断题',
    question_count INT NOT NULL COMMENT '随机抽题数量',
    score_per_question DECIMAL(6,2) NOT NULL COMMENT '每题分值',
    PRIMARY KEY (id),
    UNIQUE KEY uk_assessment_question_type (assessment_id, question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随机组卷规则';

CREATE TABLE IF NOT EXISTS assessment_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attempt_no INT NOT NULL DEFAULT 1 COMMENT '第几次考试',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-答题中 2-已交卷 3-已超时',
    started_at DATETIME NOT NULL,
    deadline_at DATETIME NOT NULL COMMENT '本次考试服务端截止时间',
    submitted_at DATETIME DEFAULT NULL,
    score DECIMAL(6,2) DEFAULT NULL,
    passed TINYINT DEFAULT NULL COMMENT '是否通过：0-否 1-是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_assessment_user_attempt (assessment_id, user_id, attempt_no),
    KEY idx_attempt_user_status (user_id, status),
    KEY idx_attempt_assessment (assessment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员考核记录';

-- 开考时随机抽题并保存完整快照；后续题库修改不影响本次试卷和成绩复核。
CREATE TABLE IF NOT EXISTS assessment_attempt_question (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    source_question_id BIGINT NOT NULL COMMENT '来源题目ID，仅用于追溯',
    question_type TINYINT NOT NULL,
    stem_snapshot TEXT NOT NULL,
    options_snapshot JSON NOT NULL COMMENT '选项快照，不向答题接口返回正确标记',
    correct_option_key VARCHAR(8) NOT NULL COMMENT '正确答案快照',
    analysis_snapshot TEXT DEFAULT NULL,
    score DECIMAL(6,2) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_source_question (attempt_id, source_question_id),
    UNIQUE KEY uk_attempt_sort (attempt_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考核试卷题目快照';

CREATE TABLE IF NOT EXISTS assessment_answer (
    id BIGINT NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT NOT NULL,
    attempt_question_id BIGINT NOT NULL,
    selected_option_key VARCHAR(8) DEFAULT NULL COMMENT '学员选择的答案',
    is_correct TINYINT DEFAULT NULL,
    score DECIMAL(6,2) NOT NULL DEFAULT 0,
    answered_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempt_question_answer (attempt_id, attempt_question_id),
    KEY idx_answer_attempt (attempt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员答题记录';
