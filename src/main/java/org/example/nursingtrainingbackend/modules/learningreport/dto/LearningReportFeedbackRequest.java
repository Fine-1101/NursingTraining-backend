package org.example.nursingtrainingbackend.modules.learningreport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LearningReportFeedbackRequest(
        @NotNull Boolean helpful,
        @Size(max = 5) List<String> reasonCodes,
        @Size(max = 500) String comment
) {
}
