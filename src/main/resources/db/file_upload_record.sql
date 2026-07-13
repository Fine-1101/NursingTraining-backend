-- 文件上传记录表（用于追踪孤立文件）
CREATE TABLE IF NOT EXISTS file_upload_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    object_key VARCHAR(500) NOT NULL COMMENT 'OSS对象Key',
    url VARCHAR(1000) NOT NULL COMMENT '文件访问URL',
    upload_type VARCHAR(50) DEFAULT NULL COMMENT '上传类型：DIRECT-直传, POLICY-策略上传',
    original_file_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    content_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
    biz_type VARCHAR(50) DEFAULT NULL COMMENT '业务类型：COURSE_COVER, VIDEO, ARTICLE, PPT, AVATAR等',
    biz_id BIGINT DEFAULT NULL COMMENT '业务数据ID',
    used TINYINT NOT NULL DEFAULT 0 COMMENT '是否已绑定业务：0-未使用，1-已使用',
    created_by BIGINT DEFAULT NULL COMMENT '上传人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    used_at DATETIME DEFAULT NULL COMMENT '绑定业务时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_object_key (object_key),
    KEY idx_biz (biz_type, biz_id),
    KEY idx_unused_created (used, created_at),
    KEY idx_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';
