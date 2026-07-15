package org.example.nursingtrainingbackend.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class LearnerResultHistoryQuery {

    @Size(max = 100)
    private String keyword;

    @Positive
    private Long courseId;

    private Boolean passed;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime submittedFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime submittedTo;

    @Min(1)
    private Long page = 1L;

    @Min(1)
    @Max(50)
    private Long size = 10L;
}
