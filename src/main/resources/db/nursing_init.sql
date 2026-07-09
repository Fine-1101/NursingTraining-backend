CREATE DATABASE IF NOT EXISTS nursing DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nursing;

-- 科室表
CREATE TABLE IF NOT EXISTS department (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '科室名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='科室信息表';

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '工号/登录账号',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    dept_id BIGINT DEFAULT NULL COMMENT '科室ID',
    role_type INT NOT NULL DEFAULT 1 COMMENT '角色：1-学员 5-总管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_dept_id (dept_id),
    KEY idx_user_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 培训分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0表示顶级',
    level TINYINT NOT NULL DEFAULT 1 COMMENT '层级：1-顶级 2-二级 3-三级',
    sort INT NOT NULL DEFAULT 0 COMMENT '同级排序号，越小越靠前',
    icon VARCHAR(255) DEFAULT NULL COMMENT '图标',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_category_parent_name (parent_id, name),
    KEY idx_category_parent_id (parent_id),
    KEY idx_category_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训分类表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    color VARCHAR(20) NOT NULL DEFAULT '#1890FF' COMMENT '标签颜色（十六进制色值）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-启用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tag_name (name),
    KEY idx_tag_status (status),
    KEY idx_tag_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训标签表';

-- 课程-标签关联表
CREATE TABLE IF NOT EXISTS course_tag (
    course_id BIGINT NOT NULL COMMENT '课程ID',
    tag_id BIGINT NOT NULL COMMENT '标签ID',
    PRIMARY KEY (course_id, tag_id),
    KEY idx_course_tag_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程-标签多对多关联表';

-- 培训文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '文章标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
    content MEDIUMTEXT DEFAULT NULL COMMENT '文章正文（HTML富文本）',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    attachment_url VARCHAR(500) DEFAULT NULL COMMENT '文章附件OSS地址',
    attachment_name VARCHAR(255) DEFAULT NULL COMMENT '附件原始文件名',
    attachment_size BIGINT DEFAULT NULL COMMENT '附件字节数',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    read_count INT NOT NULL DEFAULT 0 COMMENT '阅读完成人数',
    allow_download TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许下载附件：0-否 1-是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已下架',
    published_at DATETIME DEFAULT NULL COMMENT '最近一次发布时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_article_status (status),
    KEY idx_article_created_by (created_by),
    KEY idx_article_deleted (deleted_at),
    KEY idx_article_published_at (published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训文章表';

-- 文章统计快照表
CREATE TABLE IF NOT EXISTS article_stat_snapshot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    view_count INT NOT NULL DEFAULT 0,
    read_count INT NOT NULL DEFAULT 0,
    snapshot_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_article_stat_date (article_id, snapshot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章统计日快照表';

-- 培训视频表
CREATE TABLE IF NOT EXISTS video (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '视频标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT '视频简介',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '视频封面图URL',
    video_url VARCHAR(500) DEFAULT NULL COMMENT '视频文件URL（转码后）',
    original_url VARCHAR(500) DEFAULT NULL COMMENT '原始上传文件URL',
    duration INT DEFAULT NULL COMMENT '视频时长（秒）',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    allow_drag TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许拖拽进度条：0-否 1-是',
    allow_speed TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许倍速播放：0-否 1-是',
    view_count INT NOT NULL DEFAULT 0 COMMENT '播放量',
    watch_count INT NOT NULL DEFAULT 0 COMMENT '观看完成人数',
    allow_cache TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许缓存到本地：0-否 1-是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已下架',
    published_at DATETIME DEFAULT NULL COMMENT '最近一次发布时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_video_status (status),
    KEY idx_video_created_by (created_by),
    KEY idx_video_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训视频表';

-- 培训PPT表
CREATE TABLE IF NOT EXISTS ppt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT 'PPT标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT 'PPT简介',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    file_url VARCHAR(500) DEFAULT NULL COMMENT '预览文件URL（转PDF后存储）',
    original_url VARCHAR(500) DEFAULT NULL COMMENT '原始上传文件URL（ppt/pptx）',
    original_name VARCHAR(255) DEFAULT NULL COMMENT '原始上传文件名',
    page_count INT DEFAULT NULL COMMENT '总页数',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览量',
    complete_count INT NOT NULL DEFAULT 0 COMMENT '浏览完成人数',
    allow_download TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许下载原始文件：0-否 1-是',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已下架',
    published_at DATETIME DEFAULT NULL COMMENT '最近一次发布时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除时间',
    PRIMARY KEY (id),
    KEY idx_ppt_status (status),
    KEY idx_ppt_created_by (created_by),
    KEY idx_ppt_deleted (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='培训PPT课件表';

-- 初始化一个管理员用户（BCrypt of "admin123456"）
INSERT IGNORE INTO user (id, username, password, real_name, phone, dept_id, role_type, status)
VALUES (1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '系统管理员', NULL, NULL, 5, 1);

-- 初始化部分科室
INSERT IGNORE INTO department (id, name, status) VALUES
(1, '内科', 1),
(2, '外科', 1),
(3, '儿科', 1),
(4, '妇产科', 1),
(5, 'ICU', 1),
(6, '门急诊', 1),
(7, '手术室', 1),
(8, '护理部', 1);
