package org.example.nursingtrainingbackend.modules.learningreport.vo;

public record RegenerateReportVO(
        Long reportId,
        Long previousReportId,
        String status,
        Integer reportVersion,
        Integer estimatedWaitSeconds
) {
}
