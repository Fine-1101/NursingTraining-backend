package org.example.nursingtrainingbackend.modules.learner.profile.vo;

import lombok.Builder;

@Builder
public record LearnerDepartmentOptionVO(
        Long deptId,
        String departmentName
) {}
