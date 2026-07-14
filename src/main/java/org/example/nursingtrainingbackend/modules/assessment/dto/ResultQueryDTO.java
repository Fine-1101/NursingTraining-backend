package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResultQueryDTO {

    private Long assessmentId;

    private Long courseId;

    private Long categoryId;

    private Long departmentId;

    /** 姓名、工号或考核名称 */
    private String keyword;

    private Boolean passed;

    private LocalDateTime submittedFrom;

    private LocalDateTime submittedTo;

    @Min(1)
    private Long page = 1L;

    @Min(1)
    @Max(100)
    private Long size = 10L;
}
