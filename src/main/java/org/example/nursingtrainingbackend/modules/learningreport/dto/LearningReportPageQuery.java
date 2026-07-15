package org.example.nursingtrainingbackend.modules.learningreport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStatus;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;

public record LearningReportPageQuery(
        @Min(1) Integer page,
        @Min(1) @Max(50) Integer size,
        ReportType reportType,
        ReportStatus status
) {
    public long pageValue() { return page == null ? 1L : page; }
    public long sizeValue() { return size == null ? 10L : size; }
}
