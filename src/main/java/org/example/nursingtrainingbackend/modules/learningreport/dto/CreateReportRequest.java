package org.example.nursingtrainingbackend.modules.learningreport.dto;

import jakarta.validation.constraints.NotNull;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;

import java.time.LocalDate;

/**
 * 创建学习报告请求。
 */
public record CreateReportRequest(

        @NotNull
        ReportType reportType,

        LocalDate periodStart,

        LocalDate periodEnd,

        Long courseId,

        Boolean forceRegenerate
) {

    public boolean shouldForceRegenerate() {
        return Boolean.TRUE.equals(forceRegenerate);
    }
}
