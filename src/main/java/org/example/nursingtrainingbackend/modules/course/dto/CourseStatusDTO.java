package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
}
