package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章列表项VO
 */
@Data
public class ArticleListItemVO {
    
    /**
     * 文章ID
     */
    private Long id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 摘要
     */
    private String summary;
    
    /**
     * 上传者用户ID
     */
    private Long uploaderId;
    
    /**
     * 上传者姓名
     */
    private String uploaderName;
    
    /**
     * 上传时间，即created_at
     */
    private LocalDateTime uploadedAt;
    
    /**
     * 最近发布时间
     */
    private LocalDateTime publishedAt;
    
    /**
     * 浏览量
     */
    private Integer viewCount;
    
    /**
     * 文章状态：DRAFT、PUBLISHED、OFFLINE
     */
    private String status;
    
    /**
     * 是否存在附件
     */
    private Boolean hasAttachment;
}
