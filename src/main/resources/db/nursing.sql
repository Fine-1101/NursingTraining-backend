-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: nursing
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `article`
--

DROP TABLE IF EXISTS `article`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title` varchar(200) NOT NULL COMMENT '文章标题',
  `summary` varchar(500) DEFAULT NULL COMMENT '文章摘要',
  `content` longtext NOT NULL COMMENT '文章正文（HTML富文本）',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '文章附件OSS地址',
  `attachment_name` varchar(255) DEFAULT NULL COMMENT '附件原始文件名',
  `attachment_size` bigint DEFAULT NULL COMMENT '附件字节数',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `read_count` int DEFAULT '0' COMMENT '阅读完成人数',
  `allow_download` tinyint DEFAULT '0' COMMENT '是否允许下载附件：0-否 1-是',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-草稿 1-已发布 2-已下架',
  `published_at` datetime DEFAULT NULL COMMENT '最近一次发布时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_article_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训文章表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `article_stat_snapshot`
--

DROP TABLE IF EXISTS `article_stat_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `article_stat_snapshot` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_articles` bigint NOT NULL DEFAULT '0',
  `published_articles` bigint NOT NULL DEFAULT '0',
  `draft_articles` bigint NOT NULL DEFAULT '0',
  `monthly_views` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文章管理统计快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `category`
--

DROP TABLE IF EXISTS `category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(100) NOT NULL COMMENT '分类名称',
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '父分类ID，0表示顶级分类',
  `level` tinyint NOT NULL DEFAULT '1' COMMENT '层级：1-顶级 2-二级 3-三级',
  `sort` int DEFAULT '0' COMMENT '同级排序号，越小越靠前',
  `icon` varchar(200) DEFAULT NULL COMMENT '图标',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训分类表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '科室ID',
  `name` varchar(100) NOT NULL COMMENT '科室名称',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科室表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ppt`
--

DROP TABLE IF EXISTS `ppt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ppt` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'PPT ID',
  `title` varchar(200) NOT NULL COMMENT 'PPT标题',
  `description` text COMMENT 'PPT简介',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图URL（自动截取第一页）',
  `file_url` varchar(500) DEFAULT NULL COMMENT '预览文件URL，基础版暂不使用',
  `original_url` varchar(500) DEFAULT NULL COMMENT '原始上传文件URL（ppt/pptx）',
  `original_name` varchar(255) DEFAULT NULL COMMENT '原始上传文件名',
  `page_count` int DEFAULT NULL COMMENT '总页数',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `complete_count` int DEFAULT '0' COMMENT '浏览完成人数',
  `allow_download` tinyint DEFAULT '0' COMMENT '是否允许下载原始文件：0-否 1-是',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态：0-草稿 1-已发布 2-已下架',
  `created_by` bigint DEFAULT NULL COMMENT '上传人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  `uploaded_at` datetime DEFAULT NULL COMMENT '原文件上传完成时间',
  `published_at` datetime DEFAULT NULL COMMENT '最近一次发布时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_ppt_uploaded_at` (`uploaded_at`),
  KEY `idx_ppt_published_at` (`published_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训PPT表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ppt_stat_snapshot`
--

DROP TABLE IF EXISTS `ppt_stat_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ppt_stat_snapshot` (
  `stat_date` date NOT NULL COMMENT '统计日期',
  `total_ppts` bigint NOT NULL DEFAULT '0' COMMENT '未删除PPT总数',
  `published_ppts` bigint NOT NULL DEFAULT '0' COMMENT '已发布PPT数',
  `draft_ppts` bigint NOT NULL DEFAULT '0' COMMENT '草稿PPT数',
  `monthly_added` bigint NOT NULL DEFAULT '0' COMMENT '当月截至当天累计新增PPT数',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='PPT管理统计快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tag`
--

DROP TABLE IF EXISTS `tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `color` varchar(20) DEFAULT '#1890ff' COMMENT '标签颜色（十六进制色值）',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标签表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '工号/登录账号',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` varchar(50) NOT NULL COMMENT '真实姓名',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `dept_id` bigint DEFAULT NULL COMMENT '科室ID（关联department表）',
  `role_type` tinyint NOT NULL COMMENT '角色：1-学员 5-总管理员',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-停用 1-启用',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_role_type` (`role_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `video`
--

DROP TABLE IF EXISTS `video`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `title` varchar(200) NOT NULL COMMENT '视频标题',
  `description` text COMMENT '视频简介',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '视频封面图URL',
  `video_url` varchar(500) NOT NULL COMMENT '视频文件URL（转码后）',
  `original_url` varchar(500) DEFAULT NULL COMMENT '原始上传文件URL',
  `vod_video_id` varchar(64) DEFAULT NULL COMMENT '阿里云VOD视频ID',
  `transcode_status` tinyint NOT NULL DEFAULT '0' COMMENT '转码状态：0-等待 1-转码中 2-成功 3-失败',
  `transcode_progress` tinyint NOT NULL DEFAULT '0' COMMENT '转码进度：0-100',
  `duration` int DEFAULT NULL COMMENT '视频时长（秒）',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `storage_size` bigint NOT NULL DEFAULT '0' COMMENT '源文件及转码产物总存储字节数',
  `allow_drag` tinyint DEFAULT '0' COMMENT '是否允许拖拽进度条：0-否 1-是',
  `allow_speed` tinyint DEFAULT '1' COMMENT '是否允许倍速播放：0-否 1-是',
  `view_count` int DEFAULT '0' COMMENT '播放量',
  `watch_count` int DEFAULT '0' COMMENT '观看完成人数',
  `allow_cache` tinyint DEFAULT '1' COMMENT '是否允许缓存到本地：0-否 1-是',
  `publish_status` tinyint NOT NULL DEFAULT '0' COMMENT '业务状态：0-草稿 1-已发布',
  `created_by` bigint DEFAULT NULL COMMENT '上传人ID',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted_at` datetime DEFAULT NULL COMMENT '软删除时间',
  `transcode_error` varchar(500) DEFAULT NULL COMMENT '最近一次转码失败原因',
  `uploaded_at` datetime DEFAULT NULL COMMENT '源文件上传完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_video_vod_video_id` (`vod_video_id`),
  KEY `idx_status` (`publish_status`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_deleted_at` (`deleted_at`),
  KEY `idx_video_publish_status` (`publish_status`),
  KEY `idx_video_transcode_status` (`transcode_status`),
  KEY `idx_video_uploaded_at` (`uploaded_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训视频表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `video_stat_snapshot`
--

DROP TABLE IF EXISTS `video_stat_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `video_stat_snapshot` (
  `stat_date` date NOT NULL,
  `total_videos` bigint NOT NULL DEFAULT '0',
  `storage_bytes` bigint NOT NULL DEFAULT '0',
  `published_videos` bigint NOT NULL DEFAULT '0',
  `draft_videos` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='视频管理统计快照';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vod_callback_event`
--

DROP TABLE IF EXISTS `vod_callback_event`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vod_callback_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_key` varchar(255) NOT NULL COMMENT 'VideoId+EventType+EventTime的摘要',
  `vod_video_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `event_time` datetime NOT NULL,
  `processed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vod_callback_event_key` (`event_key`),
  KEY `idx_vod_callback_video_id` (`vod_video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阿里云VOD回调幂等记录';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-07 16:59:37
