package org.example.nursingtrainingbackend.modules.category.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchDeleteRequest(
        @NotEmpty @Size(max = 100) List<Long> ids
) {}
