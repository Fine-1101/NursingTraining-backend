package org.example.nursingtrainingbackend.modules.courseware.video.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 视频管理统计快照实体类
 */
@Data
@TableName("video_stat_snapshot")
public class VideoStatSnapshot {

    /**
     * 统计日期
     */
    @TableId
    private LocalDate statDate;

    /**
     * 视频总数
     */
    private Long totalVideos;

    /**
     * 存储字节数
     */
    private Long storageBytes;

    /**
     * 已发布视频数
     */
    private Long publishedVideos;

    /**
     * 草稿视频数
     */
    private Long draftVideos;
}