package org.example.nursingtrainingbackend.modules.courseware.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文章状态修改请求DTO
 */
@Data
public class ArticleStatusUpdateRequest {
    
    /**
     * 状态：DRAFT、PUBLISHED或OFFLINE
     */
    @NotBlank(message = "状态不能为空")
    private String status;
}
