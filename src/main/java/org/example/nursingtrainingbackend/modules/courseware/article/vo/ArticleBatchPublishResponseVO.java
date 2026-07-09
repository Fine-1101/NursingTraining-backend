package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 批量发布文章响应VO
 */
@Data
public class ArticleBatchPublishResponseVO {
    
    /**
     * 请求发布数量
     */
    private Integer requestedCount;
    
    /**
     * 实际发布数量
     */
    private Integer publishedCount;
    
    /**
     * 本批次统一发布时间
     */
    private LocalDateTime publishedAt;
}
