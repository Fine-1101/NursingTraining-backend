package org.example.nursingtrainingbackend.modules.learner.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LearnerProfileUpdateRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 50, message = "姓名不能超过50个字符")
        String realName,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不合法")
        String phone,

        @NotNull(message = "科室不能为空")
        Long deptId
) {}
