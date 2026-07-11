package org.example.nursingtrainingbackend.modules.courseware.ppt.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PptCoursesBindRequest(
        @NotEmpty(message = "课程ID列表不能为空")
        List<Long> courseIds
) {}
