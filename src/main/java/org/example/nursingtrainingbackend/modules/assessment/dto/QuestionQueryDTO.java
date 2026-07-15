package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuestionQueryDTO {

    private String keyword;

    private Long categoryId;

    private Long courseId;

    private Integer questionType;

    private Integer difficulty;

    private Integer status;

    /** CATEGORY_ALL 或 SPECIFIED_COURSE */
    private String scope;

    @Min(1)
    private Long page = 1L;

    @Min(1)
    @Max(100)
    private Long size = 10L;
}
