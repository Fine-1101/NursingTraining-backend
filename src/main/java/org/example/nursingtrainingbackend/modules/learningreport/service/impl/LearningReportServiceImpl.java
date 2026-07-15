package org.example.nursingtrainingbackend.modules.learningreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStage;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStatus;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportMapper;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportService;
import org.example.nursingtrainingbackend.modules.learningreport.task.LearningReportTaskService;
import org.example.nursingtrainingbackend.modules.learningreport.vo.CreateReportVO;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 学习报告业务服务实现。
 */
@Service
@RequiredArgsConstructor
public class LearningReportServiceImpl
        implements LearningReportService {

    private static final String DAILY_LIMIT_PREFIX =
            "nursing:learning-report:daily-limit:";

    private final AiLearningReportMapper reportMapper;
    private final LearningReportTaskService taskService;
    private final LearningReportProperties properties;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public CreateReportVO createReport(
            CreateReportRequest request
    ) {
        Long userId =
                SecurityUtils.currentUserId();

        validateRequest(request);

        ReportPeriod period =
                calculatePeriod(request);

        checkRateLimit(userId);

        checkNoGeneratingTask(
                userId,
                request.reportType(),
                period
        );

        AiLearningReport report =
                createPendingReport(
                        userId,
                        request,
                        period
                );

        /*
         * 事务成功提交后再启动异步任务。
         *
         * 如果直接在insert之后启动异步线程，
         * 异步线程可能在当前事务提交前查询报告，
         * 从而查询不到刚插入的数据。
         */
        TransactionSynchronizationManager
                .registerSynchronization(
                        new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                taskService.generateAsync(
                                        report.getId()
                                );
                            }
                        }
                );

        return toCreateVO(report);
    }

    /**
     * 校验创建请求。
     */
    private void validateRequest(
            CreateReportRequest request
    ) {
        if (request == null) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "创建报告请求不能为空"
            );
        }

        if (request.reportType() == null) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_TYPE_INVALID
            );
        }

        /*
         * 第一版只实现周报。
         */
        if (request.reportType() != ReportType.WEEKLY) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_TYPE_INVALID,
                    "当前版本只支持WEEKLY周学习报告"
            );
        }

        if (request.periodStart() != null
                && request.periodEnd() != null
                && request.periodStart()
                .isAfter(request.periodEnd())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_PERIOD_INVALID,
                    "报告开始日期不能晚于结束日期"
            );
        }

        if (request.periodEnd() != null
                && request.periodEnd()
                .isAfter(LocalDate.now())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_PERIOD_INVALID,
                    "报告结束日期不能晚于当前日期"
            );
        }
    }

    /**
     * 计算请求统计周期。
     *
     * 用户注册时间限制会在快照服务中再次处理。
     */
    private ReportPeriod calculatePeriod(
            CreateReportRequest request
    ) {
        LocalDate today =
                LocalDate.now();

        LocalDate defaultStart =
                today.minusDays(6);

        LocalDate startDate =
                request.periodStart() == null
                        ? defaultStart
                        : request.periodStart();

        LocalDate endDate =
                request.periodEnd() == null
                        ? today
                        : request.periodEnd();

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                endDate.equals(today)
                        ? LocalDateTime.now()
                        : endDate.atTime(LocalTime.MAX);

        return new ReportPeriod(start, end);
    }

    /**
     * 每个用户每天生成次数限制。
     */
    private void checkRateLimit(Long userId) {
        String key =
                DAILY_LIMIT_PREFIX
                        + userId
                        + ":"
                        + LocalDate.now();

        Long count =
                redisTemplate.opsForValue()
                        .increment(key);

        if (count != null && count == 1L) {
            LocalDateTime now =
                    LocalDateTime.now();

            LocalDateTime tomorrow =
                    LocalDate.now()
                            .plusDays(1)
                            .atStartOfDay();

            redisTemplate.expire(
                    key,
                    Duration.between(
                            now,
                            tomorrow
                    )
            );
        }

        int dailyLimit =
                properties.generation()
                        .dailyLimit();

        if (count != null && count > dailyLimit) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_RATE_LIMITED,
                    "今日报告生成次数已达到上限"
            );
        }
    }

    /**
     * 检查相同用户、类型和周期是否已有生成中任务。
     */
    private void checkNoGeneratingTask(
            Long userId,
            ReportType reportType,
            ReportPeriod period
    ) {
        Long count = reportMapper.selectCount(
                new LambdaQueryWrapper<AiLearningReport>()
                        .eq(
                                AiLearningReport::getUserId,
                                userId
                        )
                        .eq(
                                AiLearningReport::getReportType,
                                reportType.name()
                        )
                        .eq(
                                AiLearningReport::getPeriodStart,
                                period.start()
                        )
                        .eq(
                                AiLearningReport::getPeriodEnd,
                                period.end()
                        )
                        .in(
                                AiLearningReport::getStatus,
                                ReportStatus.PENDING.name(),
                                ReportStatus.GENERATING.name()
                        )
        );

        if (count != null && count > 0) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_GENERATING
            );
        }
    }

    /**
     * 创建PENDING任务。
     */
    private AiLearningReport createPendingReport(
            Long userId,
            CreateReportRequest request,
            ReportPeriod period
    ) {
        LocalDateTime now =
                LocalDateTime.now();

        AiLearningReport report =
                new AiLearningReport();

        report.setUserId(userId);
        report.setReportType(
                request.reportType().name()
        );

        /*
         * 此时还没有生成快照，所以暂时不知道最终模式。
         * 等异步任务生成快照后再设置。
         */
        report.setReportMode(null);

        report.setPeriodStart(period.start());
        report.setPeriodEnd(period.end());
        report.setStatus(
                ReportStatus.PENDING.name()
        );
        report.setStage(
                ReportStage.QUEUED.name()
        );
        report.setProgress(0);
        report.setGeneratedByAi(0);
        report.setRetryCount(0);
        report.setReportVersion(1);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);

        int affected =
                reportMapper.insert(report);

        if (affected != 1 || report.getId() == null) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "创建学习报告任务失败"
            );
        }

        return report;
    }

    /**
     * 转换创建任务响应。
     */
    private CreateReportVO toCreateVO(
            AiLearningReport report
    ) {
        return new CreateReportVO(
                report.getId(),
                report.getStatus(),
                report.getStage(),
                report.getProgress(),
                report.getReportType(),
                report.getReportMode(),
                report.getPeriodStart(),
                report.getPeriodEnd()
        );
    }

    /**
     * 内部统计周期。
     */
    private record ReportPeriod(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}