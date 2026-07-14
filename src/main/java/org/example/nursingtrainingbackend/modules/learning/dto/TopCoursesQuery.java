package org.example.nursingtrainingbackend.modules.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TopCoursesQuery {

    private String range;

    @Min(value = 1, message = "页码必须大于等于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页条数必须大于等于1")
    @Max(value = 50, message = "每页条数不能超过50")
    private Integer size = 10;
}
