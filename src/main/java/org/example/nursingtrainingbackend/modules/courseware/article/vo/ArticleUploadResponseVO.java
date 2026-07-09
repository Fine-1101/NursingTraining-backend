package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章上传响应VO
 */
@Data
public class ArticleUploadResponseVO {
    
    /**
     * 新文章ID
     */
    private Long id;
    
    /**
     * 标题
     */
    private String title;
    
    /**
     * 当前管理员ID
     */
    private Long uploaderId;
    
    /**
     * 当前管理员姓名
     */
    private String uploaderName;
    
    /**
     * 草稿或已发布
     */
    private String status;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadedAt;
    
    /**
     * 直接发布时等于当前时间，草稿时为空
     */
    private LocalDateTime publishedAt;
}
