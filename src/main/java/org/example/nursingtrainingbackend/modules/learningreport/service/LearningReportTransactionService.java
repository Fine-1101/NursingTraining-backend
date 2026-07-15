package org.example.nursingtrainingbackend.modules.learningreport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStage;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStatus;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 在独立短事务内查询并创建学习报告任务。
 */
@Service
@RequiredArgsConstructor
public class LearningReportTransactionService {

    private final AiLearningReportMapper reportMapper;

    @Transactional
    public CreatePendingResult findOrCreatePendingReport(
            Long userId,
            CreateReportRequest request,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        AiLearningReport existing = reportMapper.selectOne(
                new LambdaQueryWrapper<AiLearningReport>()
                        .eq(AiLearningReport::getUserId, userId)
                        .eq(AiLearningReport::getReportType, request.reportType().name())
                        .eq(AiLearningReport::getPeriodStart, periodStart)
                        .in(
                                AiLearningReport::getStatus,
                                ReportStatus.PENDING.name(),
                                ReportStatus.GENERATING.name()
                        )
                        .orderByDesc(AiLearningReport::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (existing != null) {
            return new CreatePendingResult(existing, false);
        }

        LocalDateTime now = LocalDateTime.now();
        AiLearningReport report = new AiLearningReport();
        report.setUserId(userId);
        report.setReportType(request.reportType().name());
        report.setReportMode(null);
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setStatus(ReportStatus.PENDING.name());
        report.setStage(ReportStage.QUEUED.name());
        report.setProgress(0);
        report.setGeneratedByAi(0);
        report.setRetryCount(0);
        report.setReportVersion(1);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        int affected = reportMapper.insert(report);
        if (affected != 1 || report.getId() == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "创建学习报告任务失败"
            );
        }

        return new CreatePendingResult(report, true);
    }

    public record CreatePendingResult(
            AiLearningReport report,
            boolean created
    ) {
    }

    @Transactional
    public AiLearningReport createRegeneratedReport(
            AiLearningReport previous
    ) {
        AiLearningReport report = new AiLearningReport();
        LocalDateTime now = LocalDateTime.now();
        report.setUserId(previous.getUserId());
        report.setReportType(previous.getReportType());
        report.setReportMode(null);
        report.setPeriodStart(previous.getPeriodStart());
        report.setPeriodEnd(previous.getPeriodEnd());
        report.setStatus(ReportStatus.PENDING.name());
        report.setStage(ReportStage.QUEUED.name());
        report.setProgress(0);
        report.setGeneratedByAi(0);
        report.setRetryCount(0);
        report.setReportVersion(
                previous.getReportVersion() == null
                        ? 2
                        : previous.getReportVersion() + 1
        );
        report.setPreviousReportId(previous.getId());
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        int affected = reportMapper.insert(report);
        if (affected != 1 || report.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建重新生成任务失败");
        }
        return report;
    }
}
