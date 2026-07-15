package org.example.nursingtrainingbackend.modules.learningreport.dto;

import jakarta.validation.constraints.Size;

public record RegenerateReportRequest(
        @Size(max = 50) String reason
) {
}
