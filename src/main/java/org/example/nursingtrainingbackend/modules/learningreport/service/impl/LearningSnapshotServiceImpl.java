package org.example.nursingtrainingbackend.modules.learningreport.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningOverviewRow;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.DataQualityLevel;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportType;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.LearningSnapshotMapper;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningSnapshotService;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习报告快照服务实现。
 */
@Service
@RequiredArgsConstructor
public class LearningSnapshotServiceImpl
        implements LearningSnapshotService {

    private static final int WEEKLY_EXPECTED_DAYS = 7;

    private final UserMapper userMapper;
    private final LearningSnapshotMapper learningSnapshotMapper;

    @Override
    public LearningReportSnapshot buildWeeklySnapshot(
            Long userId,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd
    ) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        /*
         * 1. 查询用户。
         */
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.LEARNER_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();

        /*
         * 2. 没有传开始时间时，默认使用本周一零点。
         */
        LocalDateTime defaultStart = LocalDate.now()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay();

        LocalDateTime requestedStartValue =
                requestedStart == null
                        ? defaultStart
                        : requestedStart;

        /*
         * 3. 没有传结束时间时，默认使用当前时间。
         */
        LocalDateTime requestedEndValue =
                requestedEnd == null
                        ? now
                        : requestedEnd;

        /*
         * 4. 真实开始时间不能早于注册时间。
         */
        LocalDateTime actualStart =
                requestedStartValue.isAfter(user.getCreatedAt())
                        ? requestedStartValue
                        : user.getCreatedAt();

        /*
         * 5. 真实结束时间不能晚于当前时间。
         */
        LocalDateTime actualEnd =
                requestedEndValue.isBefore(now)
                        ? requestedEndValue
                        : now;

        if (actualStart.isAfter(actualEnd)) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_PERIOD_INVALID
            );
        }

        /*
         * 6. 查询统计数据。
         */
        LearningOverviewRow overviewRow =
                learningSnapshotMapper.selectLearningOverview(
                        userId,
                        actualStart,
                        actualEnd
                );

        if (overviewRow == null) {
            overviewRow = emptyOverview();
        }

        /*
         * 7. 计算注册天数。
         *
         * 注册当天算第1天，所以最后加1。
         */
        long registeredDays = ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(),
                now.toLocalDate()
        ) + 1;

        /*
         * 8. 计算实际统计天数。
         */
        int actualDays = Math.toIntExact(
                ChronoUnit.DAYS.between(
                        actualStart.toLocalDate(),
                        actualEnd.toLocalDate()
                ) + 1
        );

        /*
         * 9. 计算数据充足度。
         */
        int dataScore = calculateDataScore(overviewRow);

        DataQualityLevel dataQualityLevel =
                resolveDataQualityLevel(dataScore);

        /*
         * 10. 决定报告模式。
         */
        ReportMode reportMode = resolveReportMode(
                overviewRow.validLearningEventCount(),
                registeredDays,
                dataScore
        );

        /*
         * 11. 生成数据限制说明。
         */
        List<String> limitations = buildLimitations(
                registeredDays,
                overviewRow,
                dataQualityLevel
        );

        /*
         * 12. 组装统计周期。
         */
        LearningReportSnapshot.ReportPeriod period =
                new LearningReportSnapshot.ReportPeriod(
                        actualStart,
                        actualEnd,
                        actualDays,
                        WEEKLY_EXPECTED_DAYS
                );

        /*
         * 13. 组装数据质量。
         */
        LearningReportSnapshot.ReportDataQuality dataQuality =
                new LearningReportSnapshot.ReportDataQuality(
                        dataQualityLevel,
                        dataScore
                );

        /*
         * 14. 组装学习概览。
         */
        LearningReportSnapshot.ReportOverview overview =
                new LearningReportSnapshot.ReportOverview(
                        overviewRow.activeDays(),
                        overviewRow.studyMinutes(),
                        overviewRow.completedPoints(),
                        overviewRow.assessmentCount(),
                        overviewRow.averageScore()
                );

        /*
         * 第一版暂时不计算知识点优势、薄弱项和候选课程，
         * 因此先传空集合。
         */
        return new LearningReportSnapshot(
                "1.0",
                ReportType.WEEKLY,
                reportMode,
                period,
                dataQuality,
                overview,
                List.of(),
                List.of(),
                List.of(),
                limitations
        );
    }

    /**
     * SQL没有返回结果时使用空数据。
     */
    private LearningOverviewRow emptyOverview() {
        return new LearningOverviewRow(
                0,
                0,
                0,
                0,
                0,
                0,
                null
        );
    }

    /**
     * 计算数据充足度分数。
     */
    private int calculateDataScore(
            LearningOverviewRow row
    ) {
        double activeDaysPart =
                Math.min(row.activeDays() / 5.0, 1.0) * 25;

        double studyMinutesPart =
                Math.min(row.studyMinutes() / 120.0, 1.0) * 20;

        double completedPointsPart =
                Math.min(row.completedPoints() / 8.0, 1.0) * 20;

        double answeredQuestionsPart =
                Math.min(
                        row.answeredQuestionCount() / 10.0,
                        1.0
                ) * 25;

        double assessmentsPart =
                Math.min(row.assessmentCount() / 2.0, 1.0) * 10;

        return (int) Math.round(
                activeDaysPart
                        + studyMinutesPart
                        + completedPointsPart
                        + answeredQuestionsPart
                        + assessmentsPart
        );
    }

    /**
     * 根据分数确定数据质量等级。
     */
    private DataQualityLevel resolveDataQualityLevel(
            int dataScore
    ) {
        if (dataScore < 20) {
            return DataQualityLevel.INSUFFICIENT;
        }

        if (dataScore < 50) {
            return DataQualityLevel.LOW;
        }

        if (dataScore < 75) {
            return DataQualityLevel.MEDIUM;
        }

        return DataQualityLevel.HIGH;
    }

    /**
     * 决定使用哪种报告模式。
     */
    private ReportMode resolveReportMode(
            long validLearningEventCount,
            long registeredDays,
            int dataScore
    ) {
        if (validLearningEventCount == 0) {
            return ReportMode.GUIDANCE_ONLY;
        }

        if (registeredDays < 7 || dataScore < 50) {
            return ReportMode.ONBOARDING;
        }

        return ReportMode.FULL;
    }

    /**
     * 生成报告限制说明。
     */
    private List<String> buildLimitations(
            long registeredDays,
            LearningOverviewRow row,
            DataQualityLevel dataQualityLevel
    ) {
        List<String> limitations = new ArrayList<>();

        if (registeredDays < 7) {
            limitations.add("注册不足7天");
        }

        if (row.assessmentCount() == 0) {
            limitations.add("当前没有已完成的考核数据");
        } else if (row.answeredQuestionCount() < 5) {
            limitations.add("考核样本较少");
        }

        if (dataQualityLevel == DataQualityLevel.INSUFFICIENT) {
            limitations.add("当前学习数据不足");
        }

        return List.copyOf(limitations);
    }
}
