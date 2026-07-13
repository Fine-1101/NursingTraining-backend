package org.example.nursingtrainingbackend.modules.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class StudentQueryDTO {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(50)
    private Integer size = 10;

    private String keyword;

    private Long departmentId;
}
