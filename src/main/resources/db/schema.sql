CREATE DATABASE IF NOT EXISTS nursing_training DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nursing_training;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '登录名',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要',
    nickname VARCHAR(64) NOT NULL COMMENT '显示名称',
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT' COMMENT 'ADMIN或STUDENT',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    CONSTRAINT chk_sys_user_role CHECK (role IN ('ADMIN', 'STUDENT')),
    CONSTRAINT chk_sys_user_status CHECK (status IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- 请先生成 BCrypt 密码摘要，再手工执行下面的初始化语句；不要保存明文密码。
-- INSERT INTO sys_user(username, password, nickname, role, status)
-- VALUES ('admin', '$2a$10$REPLACE_WITH_A_REAL_BCRYPT_HASH', '系统管理员', 'ADMIN', 1);

CREATE TABLE IF NOT EXISTS user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '工号/登录账号',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    dept_id BIGINT DEFAULT NULL COMMENT '科室ID',
    role_type INT NOT NULL DEFAULT 1 COMMENT '角色：1-学员 5-总管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS article (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    content LONGTEXT NOT NULL COMMENT '文章正文（HTML富文本）',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    attachment_url VARCHAR(500) DEFAULT NULL COMMENT '文章附件OSS地址',
    attachment_name VARCHAR(255) DEFAULT NULL COMMENT '附件原始文件名',
    attachment_size BIGINT DEFAULT NULL COMMENT '附件字节数',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    read_count INT NOT NULL DEFAULT 0 COMMENT '阅读完成人数',
    allow_download TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许下载附件：0-否 1-是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已下架',
    published_at DATETIME DEFAULT NULL COMMENT '最近一次发布时间',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_article_status (status),
    KEY idx_article_created_at (created_at),
    KEY idx_article_published_at (published_at),
    KEY idx_article_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训文章表';

CREATE TABLE IF NOT EXISTS article_stat_snapshot (
    stat_date DATE NOT NULL COMMENT '统计日期',
    total_articles BIGINT NOT NULL DEFAULT 0 COMMENT '文章总数',
    published_articles BIGINT NOT NULL DEFAULT 0 COMMENT '已发布文章数',
    draft_articles BIGINT NOT NULL DEFAULT 0 COMMENT '草稿文章数',
    monthly_views BIGINT NOT NULL DEFAULT 0 COMMENT '当月累计浏览量',
    PRIMARY KEY (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章管理统计快照';
