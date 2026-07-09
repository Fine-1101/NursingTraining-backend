package org.example.nursingtrainingbackend.modules.courseware.article.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量操作文章请求DTO
 */
@Data
public class ArticleBatchRequest {
    
    /**
     * 文章ID数组，1～100条，不允许重复
     */
    @NotEmpty(message = "文章ID列表不能为空")
    @Size(min = 1, max = 100, message = "文章ID数量必须在1-100之间")
    private List<Long> ids;
}
