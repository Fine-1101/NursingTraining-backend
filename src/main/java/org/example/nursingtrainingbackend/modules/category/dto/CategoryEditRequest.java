package org.example.nursingtrainingbackend.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryEditRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Long parentId,
        @Size(max = 200) String icon,
        @NotNull Integer status,
        Boolean cascade
) {}