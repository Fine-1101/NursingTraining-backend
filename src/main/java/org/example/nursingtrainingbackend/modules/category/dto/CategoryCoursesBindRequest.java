package org.example.nursingtrainingbackend.modules.category.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CategoryCoursesBindRequest(
        @NotEmpty(message = "课程ID列表不能为空")
        List<Long> courseIds
) {}
