package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

/**
 * 批量删除文章响应VO
 */
@Data
public class ArticleBatchDeleteResponseVO {
    
    /**
     * 请求删除数量
     */
    private Integer requestedCount;
    
    /**
     * 实际软删除数量
     */
    private Integer deletedCount;
}
