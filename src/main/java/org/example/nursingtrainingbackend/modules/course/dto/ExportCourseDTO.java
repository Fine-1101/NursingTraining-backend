package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 课程导出查询参数（不含分页）
 */
@Data
public class ExportCourseDTO {
    @Length(min = 1, max = 100, message = "关键词去除首尾空格后长度必须在1-100之间")
    private String keyword;

    private Long categoryId;

    @Pattern(regexp = "^(DRAFT|PUBLISHED|OFFLINE)?$", message = "状态仅支持 DRAFT、PUBLISHED、OFFLINE")
    private String status;
}
