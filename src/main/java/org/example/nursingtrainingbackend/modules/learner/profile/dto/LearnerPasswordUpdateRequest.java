package org.example.nursingtrainingbackend.modules.learner.profile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LearnerPasswordUpdateRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 20, message = "新密码长度需为8-20位")
        String newPassword,

        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {}
