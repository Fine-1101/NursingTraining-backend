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
