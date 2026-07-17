package org.example.nursingtrainingbackend.modules.learner.profile.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LearnerProfileVO(
        Long userId,
        String username,
        String realName,
        String phone,
        Long deptId,
        String departmentName,
        Integer roleType,
        Integer status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime lastLoginAt,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        LocalDateTime updatedAt
) {}
