package org.example.nursingtrainingbackend.modules.course.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepartmentDTO {
    @NotNull(message = "部门ID不能为空")
    private Long departmentId;
    private Boolean required;
}
