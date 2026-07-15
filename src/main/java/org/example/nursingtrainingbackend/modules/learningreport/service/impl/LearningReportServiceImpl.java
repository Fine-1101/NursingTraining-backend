package org.example.nursingtrainingbackend.modules.learningreport.service.impl;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.dto.CreateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningOverviewRow;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportFeedbackRequest;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportPageQuery;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.dto.RegenerateReportRequest;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReportFeedback;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStatus;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportMapper;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportFeedbackMapper;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportSnapshotMapper;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.LearningSnapshotMapper;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportRedisService;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportService;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningSnapshotService;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportTransactionService;
import org.example.nursingtrainingbackend.modules.learningreport.task.LearningReportTaskService;
import org.example.nursingtrainingbackend.modules.learningreport.vo.CreateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportDetailVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.LearningReportListItemVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.RegenerateReportVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.ReportEligibilityVO;
import org.example.nursingtrainingbackend.modules.learningreport.vo.SubmitReportFeedbackVO;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import tools.jackson.databind.ObjectMapper;

/**
 * 学习报告任务创建协调服务。
 */
@Service
@RequiredArgsConstructor
public class LearningReportServiceImpl implements LearningReportService {

    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 15;

    private final AiLearningReportMapper reportMapper;
    private final AiLearningReportSnapshotMapper reportSnapshotMapper;
    private final AiLearningReportFeedbackMapper feedbackMapper;
    private final LearningSnapshotMapper learningSnapshotMapper;
    private final UserMapper userMapper;
    private final LearningReportTransactionService transactionService;
    private final LearningReportRedisService redisService;
    private final LearningReportTaskService taskService;
    private final LearningSnapshotService snapshotService;
    private final LearningReportProperties properties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public CreateReportVO createReport(
            CreateReportRequest request,
            String idempotencyKey
    ) {
        Long userId = SecurityUtils.currentUserId();

        validateRequest(request);
        validateIdempotencyKey(idempotencyKey);

        CreateReportVO cachedResult = findIdempotentResult(userId, idempotencyKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        ReportPeriod period = calculatePeriod(request);
        String periodKey = buildPeriodKey(period.start().toLocalDate());
        String lockKey = buildLockKey(userId, request.reportType(), periodKey);
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;

        try {
            acquired = lock.tryLock(
                    LOCK_WAIT_SECONDS,
                    LOCK_LEASE_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!acquired) {
                throw new BusinessException(
                        ErrorCode.LEARNING_REPORT_GENERATING,
                        "相同周期报告正在创建，请稍后查询"
                );
            }

            cachedResult = findIdempotentResult(userId, idempotencyKey);
            if (cachedResult != null) {
                return cachedResult;
            }

            redisService.checkDailyLimit(
                    userId,
                    properties.generation().dailyLimit()
            );

            LearningReportTransactionService.CreatePendingResult createResult =
                    transactionService.findOrCreatePendingReport(
                            userId,
                            request,
                            period.start(),
                            period.end()
                    );

            AiLearningReport report = createResult.report();
            redisService.saveIdempotentResult(
                    userId,
                    idempotencyKey,
                    report.getId()
            );

            if (createResult.created()
                    && ReportStatus.PENDING.name().equals(report.getStatus())) {
                taskService.generateAsync(report.getId());
            }

            return toCreateVO(report, !createResult.created());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "创建学习报告任务被中断",
                    exception
            );
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public ReportEligibilityVO getEligibility(
            ReportType reportType,
            Long courseId
    ) {
        validateSupportedType(reportType, courseId);
        Long userId = SecurityUtils.currentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.LEARNER_NOT_FOUND);
        }

        LearningReportSnapshot snapshot = snapshotService.buildWeeklySnapshot(
                userId,
                null,
                null
        );
        LearningOverviewRow row = learningSnapshotMapper.selectLearningOverview(
                userId,
                snapshot.period().start(),
                snapshot.period().end()
        );
        long registeredDays = ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(),
                LocalDate.now()
        ) + 1;
        long eventCount = row == null ? 0 : row.validLearningEventCount();
        boolean eligible = eventCount > 0;
        boolean mastery = row != null
                && row.answeredQuestionCount()
                >= properties.eligibility().minimumMasteryQuestions();
        boolean trend = registeredDays >= 14
                && row != null
                && row.activeDays() >= 5;
        List<String> limitations = snapshot.limitations() == null
                ? List.of()
                : snapshot.limitations();

        ReportEligibilityVO.DataQuality quality = new ReportEligibilityVO.DataQuality(
                snapshot.dataQuality().level(),
                snapshot.dataQuality().score(),
                row == null ? 0 : row.activeDays(),
                row == null ? 0 : row.studyMinutes(),
                row == null ? 0 : row.completedPoints(),
                row == null ? 0 : row.answeredQuestionCount(),
                row == null ? 0 : row.assessmentCount(),
                limitations
        );

        return new ReportEligibilityVO(
                eligible,
                snapshot.reportMode(),
                registeredDays,
                eventCount,
                quality,
                new ReportEligibilityVO.Capabilities(
                        trend,
                        mastery,
                        registeredDays >= 14,
                        row != null && row.assessmentCount() >= 2
                ),
                user.getCreatedAt().plusDays(7),
                eligible ? null : "NO_LEARNING_DATA",
                eligible ? null : "完成至少一次有效学习后即可生成入门报告"
        );
    }

    @Override
    public LearningReportDetailVO getCurrent(
            ReportType reportType,
            Long courseId
    ) {
        validateSupportedType(reportType, courseId);
        ReportType actualReportType = reportType == null
                ? ReportType.WEEKLY
                : reportType;
        Long userId = SecurityUtils.currentUserId();
        LocalDateTime weekStart = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        AiLearningReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<AiLearningReport>()
                        .eq(AiLearningReport::getUserId, userId)
                        .eq(AiLearningReport::getReportType, actualReportType.name())
                        .ge(AiLearningReport::getPeriodStart, weekStart)
                        .last("ORDER BY CASE status "
                                + "WHEN 'GENERATING' THEN 0 "
                                + "WHEN 'PENDING' THEN 1 "
                                + "WHEN 'SUCCESS' THEN 2 "
                                + "ELSE 3 END, created_at DESC LIMIT 1")
        );
        return report == null ? null : toDetailVO(report);
    }

    @Override
    public LearningReportDetailVO getDetail(Long reportId) {
        Long userId = SecurityUtils.currentUserId();
        AiLearningReport report = findOwnedReport(reportId, userId);

        String cached = redisService.getCachedReportDetail(reportId);
        if (cached != null && ReportStatus.SUCCESS.name().equals(report.getStatus())) {
            try {
                return objectMapper.readValue(cached, LearningReportDetailVO.class);
            } catch (RuntimeException exception) {
                redisService.evictReportDetail(reportId);
            }
        }

        LearningReportDetailVO detail = toDetailVO(report);
        if (ReportStatus.SUCCESS.name().equals(report.getStatus())) {
            try {
                redisService.cacheReportDetail(
                        reportId,
                        objectMapper.writeValueAsString(detail)
                );
            } catch (RuntimeException ignored) {
                // 缓存失败不影响主查询。
            }
        }
        return detail;
    }

    @Override
    public PageResult<LearningReportListItemVO> list(
            LearningReportPageQuery query
    ) {
        Long userId = SecurityUtils.currentUserId();
        LearningReportPageQuery safeQuery = query == null
                ? new LearningReportPageQuery(1, 10, null, null)
                : query;

        LambdaQueryWrapper<AiLearningReport> wrapper =
                new LambdaQueryWrapper<AiLearningReport>()
                        .eq(AiLearningReport::getUserId, userId)
                        .eq(
                                safeQuery.reportType() != null,
                                AiLearningReport::getReportType,
                                safeQuery.reportType() == null
                                        ? null
                                        : safeQuery.reportType().name()
                        )
                        .eq(
                                safeQuery.status() != null,
                                AiLearningReport::getStatus,
                                safeQuery.status() == null
                                        ? null
                                        : safeQuery.status().name()
                        )
                        .orderByDesc(AiLearningReport::getCreatedAt);

        Page<AiLearningReport> page = reportMapper.selectPage(
                new Page<>(safeQuery.pageValue(), safeQuery.sizeValue()),
                wrapper
        );
        List<LearningReportListItemVO> records = page.getRecords().stream()
                .map(this::toListItemVO)
                .toList();

        return new PageResult<>(
                records,
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                page.getPages()
        );
    }

    @Override
    public RegenerateReportVO regenerate(
            Long reportId,
            RegenerateReportRequest request,
            String idempotencyKey
    ) {
        Long userId = SecurityUtils.currentUserId();
        validateIdempotencyKey(idempotencyKey);
        AiLearningReport previous = findOwnedReport(reportId, userId);
        if (!ReportStatus.SUCCESS.name().equals(previous.getStatus())
                && !ReportStatus.FAILED.name().equals(previous.getStatus())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_GENERATING,
                    "当前报告状态不允许重新生成"
            );
        }

        Optional<Long> cachedId = redisService.findIdempotentReportId(
                userId,
                idempotencyKey
        );
        if (cachedId.isPresent()) {
            AiLearningReport cached = findOwnedReport(cachedId.get(), userId);
            return toRegenerateVO(cached);
        }

        String lockKey = "ai:report:lock:"
                + userId + ":REGENERATE:" + reportId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(
                    LOCK_WAIT_SECONDS,
                    LOCK_LEASE_SECONDS,
                    TimeUnit.SECONDS
            );
            if (!acquired) {
                throw new BusinessException(
                        ErrorCode.LEARNING_REPORT_GENERATING,
                        "该报告正在重新生成"
                );
            }

            redisService.checkRegenerateLimit(
                    userId,
                    reportId,
                    properties.generation().regenerateDailyLimit()
            );
            AiLearningReport report = transactionService
                    .createRegeneratedReport(previous);
            redisService.saveIdempotentResult(userId, idempotencyKey, report.getId());
            taskService.generateAsync(report.getId());
            return toRegenerateVO(report);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "重新生成任务被中断", exception);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public SubmitReportFeedbackVO submitFeedback(
            Long reportId,
            LearningReportFeedbackRequest request
    ) {
        Long userId = SecurityUtils.currentUserId();
        AiLearningReport report = findOwnedReport(reportId, userId);
        if (!ReportStatus.SUCCESS.name().equals(report.getStatus())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_FEEDBACK_INVALID,
                    "只有生成成功的报告可以提交反馈"
            );
        }
        validateFeedback(request);

        AiLearningReportFeedback feedback = feedbackMapper.selectOne(
                new LambdaQueryWrapper<AiLearningReportFeedback>()
                        .eq(AiLearningReportFeedback::getReportId, reportId)
                        .eq(AiLearningReportFeedback::getUserId, userId)
                        .last("LIMIT 1")
        );
        LocalDateTime now = LocalDateTime.now();
        if (feedback == null) {
            feedback = new AiLearningReportFeedback();
            feedback.setReportId(reportId);
            feedback.setUserId(userId);
            feedback.setCreatedAt(now);
        }
        feedback.setHelpful(Boolean.TRUE.equals(request.helpful()) ? 1 : 0);
        feedback.setReasonCodes(objectMapper.writeValueAsString(
                request.reasonCodes() == null ? List.of() : request.reasonCodes()
        ));
        feedback.setComment(request.comment() == null ? null : request.comment().trim());
        feedback.setUpdatedAt(now);

        if (feedback.getId() == null) {
            feedbackMapper.insert(feedback);
        } else {
            feedbackMapper.updateById(feedback);
        }
        return new SubmitReportFeedbackVO(reportId, true);
    }

    private void validateSupportedType(ReportType reportType, Long courseId) {
        ReportType actual = reportType == null ? ReportType.WEEKLY : reportType;
        if (actual == ReportType.COURSE && courseId == null) {
            throw new BusinessException(ErrorCode.LEARNING_REPORT_COURSE_REQUIRED);
        }
        if (actual != ReportType.WEEKLY) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_TYPE_INVALID,
                    "当前版本只支持WEEKLY周学习报告"
            );
        }
    }

    private AiLearningReport findOwnedReport(Long reportId, Long userId) {
        if (reportId == null || reportId <= 0) {
            throw new BusinessException(ErrorCode.LEARNING_REPORT_NOT_FOUND);
        }
        AiLearningReport report = reportMapper.selectOne(
                new LambdaQueryWrapper<AiLearningReport>()
                        .eq(AiLearningReport::getId, reportId)
                        .eq(AiLearningReport::getUserId, userId)
                        .last("LIMIT 1")
        );
        if (report == null) {
            throw new BusinessException(ErrorCode.LEARNING_REPORT_NOT_FOUND);
        }
        return report;
    }

    private LearningReportDetailVO toDetailVO(AiLearningReport report) {
        AiLearningReportSnapshot snapshotEntity = report.getSnapshotId() == null
                ? null
                : reportSnapshotMapper.selectById(report.getSnapshotId());
        LearningReportSnapshot snapshot = parseSnapshot(snapshotEntity);
        GeneratedLearningReport content = parseGeneratedReport(report.getReportContent());

        LearningReportSnapshot.ReportPeriod period = snapshot == null
                ? new LearningReportSnapshot.ReportPeriod(
                        report.getPeriodStart(),
                        report.getPeriodEnd(),
                        Math.toIntExact(ChronoUnit.DAYS.between(
                                report.getPeriodStart().toLocalDate(),
                                report.getPeriodEnd().toLocalDate()
                        ) + 1),
                        7
                )
                : snapshot.period();
        LearningReportSnapshot.ReportDataQuality quality = snapshot == null
                ? null
                : snapshot.dataQuality();
        boolean full = ReportMode.FULL.name().equals(report.getReportMode());
        boolean mastery = quality != null
                && quality.level() != DataQualityLevel.INSUFFICIENT;
        ReportEligibilityVO.Capabilities capabilities =
                new ReportEligibilityVO.Capabilities(full, mastery, full, full);

        LearningReportDetailVO.Failure failure =
                ReportStatus.FAILED.name().equals(report.getStatus())
                        ? new LearningReportDetailVO.Failure(
                                report.getErrorCode(),
                                report.getErrorMessage() == null
                                        ? "报告生成失败，请稍后重试"
                                        : report.getErrorMessage(),
                                true
                        )
                        : null;
        User user = userMapper.selectById(report.getUserId());

        return new LearningReportDetailVO(
                report.getId(),
                report.getStatus(),
                report.getStage(),
                report.getProgress(),
                report.getReportType(),
                report.getReportMode(),
                report.getTitle(),
                period,
                quality,
                capabilities,
                content == null
                        ? (snapshot == null ? null : snapshot.overview())
                        : content.overview(),
                report.getSummary(),
                content == null ? null : content.performanceLevel(),
                content == null ? List.of() : safeList(content.highlights()),
                content == null ? List.of() : safeList(content.strengths()),
                content == null ? List.of() : safeList(content.weaknesses()),
                content == null ? List.of() : safeList(content.studyPlan()),
                content == null ? null : content.encouragement(),
                content == null ? null : content.disclaimer(),
                Integer.valueOf(1).equals(report.getGeneratedByAi()),
                report.getGeneratedAt(),
                snapshotEntity == null ? null : snapshotEntity.getCreatedAt(),
                report.getPromptVersion(),
                user == null || user.getCreatedAt() == null
                        ? null
                        : user.getCreatedAt().plusDays(7),
                failure,
                ReportStatus.PENDING.name().equals(report.getStatus())
                        || ReportStatus.GENERATING.name().equals(report.getStatus())
                        ? 3
                        : null,
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }

    private LearningReportListItemVO toListItemVO(AiLearningReport report) {
        LearningReportSnapshot snapshot = report.getSnapshotId() == null
                ? null
                : parseSnapshot(reportSnapshotMapper.selectById(report.getSnapshotId()));
        return new LearningReportListItemVO(
                report.getId(),
                report.getReportType(),
                report.getReportMode(),
                report.getStatus(),
                report.getTitle(),
                report.getSummary(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                snapshot == null || snapshot.dataQuality() == null
                        ? null
                        : snapshot.dataQuality().level().name(),
                report.getGeneratedAt()
        );
    }

    private RegenerateReportVO toRegenerateVO(AiLearningReport report) {
        return new RegenerateReportVO(
                report.getId(),
                report.getPreviousReportId(),
                report.getStatus(),
                report.getReportVersion(),
                20
        );
    }

    private LearningReportSnapshot parseSnapshot(AiLearningReportSnapshot entity) {
        if (entity == null || entity.getSnapshotContent() == null) {
            return null;
        }
        try {
            return objectMapper.readValue(
                    entity.getSnapshotContent(),
                    LearningReportSnapshot.class
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private GeneratedLearningReport parseGeneratedReport(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, GeneratedLearningReport.class);
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "学习报告内容解析失败",
                    exception
            );
        }
    }

    private void validateFeedback(LearningReportFeedbackRequest request) {
        if (request == null || request.helpful() == null) {
            throw new BusinessException(ErrorCode.LEARNING_REPORT_FEEDBACK_INVALID);
        }
        Set<String> allowed = Set.of(
                "ANALYSIS_ACCURATE",
                "PLAN_ACTIONABLE",
                "COURSE_RECOMMENDATION_USEFUL",
                "ANALYSIS_INACCURATE",
                "PLAN_NOT_PRACTICAL",
                "CONTENT_TOO_GENERIC",
                "OTHER"
        );
        if (request.reasonCodes() != null
                && request.reasonCodes().stream().anyMatch(code -> !allowed.contains(code))) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_FEEDBACK_INVALID,
                    "反馈原因编码不合法"
            );
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private CreateReportVO findIdempotentResult(
            Long userId,
            String idempotencyKey
    ) {
        Optional<Long> reportId = redisService.findIdempotentReportId(
                userId,
                idempotencyKey
        );

        if (reportId.isEmpty()) {
            return null;
        }

        AiLearningReport report = reportMapper.selectById(reportId.get());
        if (report == null || !userId.equals(report.getUserId())) {
            redisService.deleteIdempotentResult(userId, idempotencyKey);
            return null;
        }

        return toCreateVO(report, true);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "缺少Idempotency-Key请求头"
            );
        }

        if (idempotencyKey.length() > 100) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Idempotency-Key长度不能超过100"
            );
        }

        try {
            UUID.fromString(idempotencyKey);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "Idempotency-Key必须是合法UUID"
            );
        }
    }

    private void validateRequest(CreateReportRequest request) {
        if (request == null || request.reportType() == null) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_TYPE_INVALID
            );
        }

        if (request.reportType() != ReportType.WEEKLY) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_TYPE_INVALID,
                    "当前版本只支持WEEKLY周学习报告"
            );
        }

        if (request.periodStart() != null
                && request.periodEnd() != null
                && request.periodStart().isAfter(request.periodEnd())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_PERIOD_INVALID,
                    "报告开始日期不能晚于结束日期"
            );
        }

        if (request.periodEnd() != null
                && request.periodEnd().isAfter(LocalDate.now())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_PERIOD_INVALID,
                    "报告结束日期不能晚于当前日期"
            );
        }
    }

    private ReportPeriod calculatePeriod(CreateReportRequest request) {
        LocalDate today = LocalDate.now();
        LocalDate defaultStart = today.with(DayOfWeek.MONDAY);
        LocalDate startDate = request.periodStart() == null
                ? defaultStart
                : request.periodStart();
        LocalDate endDate = request.periodEnd() == null
                ? today
                : request.periodEnd();

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.equals(today)
                ? LocalDateTime.now()
                : endDate.atTime(LocalTime.MAX);

        return new ReportPeriod(start, end);
    }

    private String buildPeriodKey(LocalDate periodDate) {
        WeekFields fields = WeekFields.ISO;
        int year = periodDate.get(fields.weekBasedYear());
        int week = periodDate.get(fields.weekOfWeekBasedYear());
        return "%d-W%02d".formatted(year, week);
    }

    private String buildLockKey(
            Long userId,
            ReportType reportType,
            String periodKey
    ) {
        return "ai:report:lock:"
                + userId + ":" + reportType.name() + ":" + periodKey;
    }

    private CreateReportVO toCreateVO(
            AiLearningReport report,
            boolean reused
    ) {
        return new CreateReportVO(
                report.getId(),
                report.getStatus(),
                report.getStage(),
                report.getProgress(),
                report.getReportType(),
                report.getReportMode(),
                report.getPeriodStart(),
                report.getPeriodEnd(),
                ReportStatus.SUCCESS.name().equals(report.getStatus()) ? 0 : 20,
                reused
        );
    }

    private record ReportPeriod(
            LocalDateTime start,
            LocalDateTime end
    ) {
    }
}
