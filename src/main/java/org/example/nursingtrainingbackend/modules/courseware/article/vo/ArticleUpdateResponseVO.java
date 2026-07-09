package org.example.nursingtrainingbackend.modules.courseware.article.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章编辑响应VO
 */
@Data
public class ArticleUpdateResponseVO {
    
    /**
     * 文章ID
     */
    private Long id;
    
    /**
     * 修改后的标题
     */
    private String title;
    
    /**
     * 状态不由编辑接口改变
     */
    private String status;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
