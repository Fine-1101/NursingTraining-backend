package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章状态修改响应VO
 */
@Data
public class ArticleStatusUpdateResponseVO {
    
    /**
     * 文章ID
     */
    private Long id;
    
    /**
     * 修改后状态
     */
    private String status;
    
    /**
     * 发布时更新；草稿时为空，下架时保留最近发布时间
     */
    private LocalDateTime publishedAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
