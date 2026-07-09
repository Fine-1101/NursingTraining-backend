package org.example.nursingtrainingbackend.modules.category.dto;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(
        @NotNull Integer status,
        Boolean cascade
) {}
