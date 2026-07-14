package org.example.nursingtrainingbackend.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 64) String username,
        @NotBlank @Size(max = 100) String password,
        @NotBlank @Size(max = 50) String realName,
        @NotNull Long deptId,
        @NotNull Integer roleType
) {}
