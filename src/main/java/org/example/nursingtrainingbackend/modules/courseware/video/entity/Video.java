package org.example.nursingtrainingbackend.modules.courseware.video.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训视频实体类
 */
@Data
@TableName("video")
public class Video {

    /**
     * 视频ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频简介
     */
    private String description;

    /**
     * 视频封面图URL
     */
    private String coverUrl;

    /**
     * 视频文件URL（转码后）
     */
    private String videoUrl;

    /**
     * 原始上传文件URL
     */
    private String originalUrl;

    /**
     * 视频时长（秒）
     */
    private Integer duration;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 是否允许拖拽进度条：0-否 1-是
     */
    private Integer allowDrag;

    /**
     * 是否允许倍速播放：0-否 1-是
     */
    private Integer allowSpeed;

    /**
     * 播放量
     */
    private Integer viewCount;

    /**
     * 观看完成人数
     */
    private Integer watchCount;

    /**
     * 是否允许缓存到本地：0-否 1-是
     */
    private Integer allowCache;

    /**
     * 状态：0-转码中 1-已发布 2-已下架
     */
    private Integer status;

    /**
     * 上传人ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}
