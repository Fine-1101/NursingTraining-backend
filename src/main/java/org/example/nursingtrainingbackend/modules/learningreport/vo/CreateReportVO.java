package org.example.nursingtrainingbackend.modules.learningreport.vo;

import java.time.LocalDateTime;

/**
 * 创建报告任务响应。
 */
public record CreateReportVO(
        Long reportId,
        String status,
        String stage,
        Integer progress,
        String reportType,
        String reportMode,
        LocalDateTime actualPeriodStart,
        LocalDateTime actualPeriodEnd,
        Integer estimatedWaitSeconds,
        boolean reused
) {
}
