package org.example.nursingtrainingbackend.modules.courseware.ppt.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训PPT实体类
 */
@Data
@TableName("ppt")
public class Ppt {

    /**
     * PPT ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * PPT标题
     */
    private String title;

    /**
     * PPT简介
     */
    private String description;

    /**
     * 封面图URL（自动截取第一页）
     */
    private String coverUrl;

    /**
     * 预览文件URL（转PDF后存储）
     */
    private String fileUrl;

    /**
     * 原始上传文件URL（ppt/pptx）
     */
    private String originalUrl;

    /**
     * 总页数
     */
    private Integer pageCount;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 浏览量
     */
    private Integer viewCount;

    /**
     * 浏览完成人数
     */
    private Integer completeCount;

    /**
     * 是否允许下载原始文件：0-否 1-是
     */
    private Integer allowDownload;

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
