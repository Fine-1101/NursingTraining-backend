package org.example.nursingtrainingbackend.modules.learningreport.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.exception.BusinessException;
import org.example.nursingtrainingbackend.common.result.ErrorCode;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportClient;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportResult;
import org.example.nursingtrainingbackend.modules.learningreport.ai.ReportGenerationOptions;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiInvocationLog;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.entity.AiLearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportMode;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStage;
import org.example.nursingtrainingbackend.modules.learningreport.enums.ReportStatus;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiInvocationLogMapper;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportMapper;
import org.example.nursingtrainingbackend.modules.learningreport.mapper.AiLearningReportSnapshotMapper;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningSnapshotService;
import org.example.nursingtrainingbackend.modules.learningreport.service.RuleBasedReportService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 学习报告异步生成任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningReportTaskService {

    private final AiLearningReportMapper reportMapper;
    private final AiLearningReportSnapshotMapper snapshotMapper;
    private final AiInvocationLogMapper invocationLogMapper;

    private final LearningSnapshotService learningSnapshotService;
    private final RuleBasedReportService ruleBasedReportService;
    private final AiReportClient aiReportClient;

    private final LearningReportProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * 异步生成报告。
     */
    @Async("learningReportExecutor")
    public void generateAsync(Long reportId) {
        try {
            /*
             * 1. 查询任务。
             */
            AiLearningReport task =
                    findTask(reportId);

            /*
             * 2. 更新为数据聚合阶段。
             */
            markGenerating(
                    task,
                    ReportStage.DATA_AGGREGATION,
                    20
            );

            /*
             * 3. 查询并生成学习数据快照。
             */
            LearningReportSnapshot snapshot =
                    learningSnapshotService
                            .buildWeeklySnapshot(
                                    task.getUserId(),
                                    task.getPeriodStart(),
                                    task.getPeriodEnd()
                            );

            /*
             * 4. 保存快照。
             */
            saveSnapshot(task, snapshot);

            /*
             * 5. 保存最终报告模式。
             */
            task.setReportMode(
                    snapshot.reportMode().name()
            );
            task.setUpdatedAt(
                    LocalDateTime.now()
            );
            reportMapper.updateById(task);

            /*
             * 6. 数据不足时不调用AI。
             */
            if (snapshot.reportMode()
                    == ReportMode.GUIDANCE_ONLY) {
                GeneratedLearningReport guidanceReport =
                        ruleBasedReportService.generate(
                                snapshot
                        );

                completeReport(
                        task,
                        guidanceReport,
                        false,
                        null
                );

                return;
            }

            /*
             * 7. AI未启用时直接生成规则报告。
             */
            if (properties.ai() == null || !properties.ai().enabled()) {
                GeneratedLearningReport ruleReport =
                        ruleBasedReportService.generate(
                                snapshot
                        );

                completeReport(
                        task,
                        ruleReport,
                        false,
                        null
                );

                return;
            }

            /*
             * 8. 进入AI生成阶段。
             */
            markGenerating(
                    task,
                    ReportStage.AI_GENERATION,
                    60
            );

            GeneratedLearningReport generatedReport;
            boolean generatedByAi;
            AiReportResult aiResult = null;

            try {
                ReportGenerationOptions options =
                        ReportGenerationOptions.from(
                                properties
                        );

                /*
                 * OpenAiCompatibleReportClient内部已经：
                 * 解析JSON、恢复overview并校验内容。
                 */
                aiResult = aiReportClient.generate(
                        snapshot,
                        options
                );

                markGenerating(
                        task,
                        ReportStage.VALIDATION,
                        80
                );

                generatedReport =
                        aiResult.report();

                generatedByAi = true;

                saveInvocationSuccess(
                        task,
                        aiResult
                );
            } catch (RuntimeException exception) {
                log.warn(
                        "AI报告生成失败，reportId={}，使用规则报告降级：{}",
                        reportId,
                        exception.getMessage()
                );

                generatedReport =
                        ruleBasedReportService.generate(
                                snapshot
                        );

                generatedByAi = false;

                saveInvocationFailure(
                        task,
                        exception
                );
            }

            /*
             * 9. 保存最终报告。
             */
            completeReport(
                    task,
                    generatedReport,
                    generatedByAi,
                    aiResult
            );
        } catch (Exception exception) {
            log.error(
                    "学习报告异步任务失败，reportId={}",
                    reportId,
                    exception
            );

            markFailed(
                    reportId,
                    exception
            );
        }
    }

    /**
     * 查询报告任务。
     */
    private AiLearningReport findTask(
            Long reportId
    ) {
        if (reportId == null) {
            throw new IllegalArgumentException(
                    "报告ID不能为空"
            );
        }

        AiLearningReport task =
                reportMapper.selectById(reportId);

        if (task == null) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_NOT_FOUND
            );
        }

        if (!ReportStatus.PENDING.name()
                .equals(task.getStatus())
                && !ReportStatus.GENERATING.name()
                .equals(task.getStatus())) {
            throw new BusinessException(
                    ErrorCode.LEARNING_REPORT_GENERATING,
                    "当前报告状态不允许重新执行生成任务"
            );
        }

        return task;
    }

    /**
     * 更新生成阶段和进度。
     */
    private void markGenerating(
            AiLearningReport task,
            ReportStage stage,
            int progress
    ) {
        task.setStatus(
                ReportStatus.GENERATING.name()
        );
        task.setStage(stage.name());
        task.setProgress(progress);

        if (task.getStartedAt() == null) {
            task.setStartedAt(
                    LocalDateTime.now()
            );
        }

        task.setUpdatedAt(
                LocalDateTime.now()
        );

        int affected =
                reportMapper.updateById(task);

        if (affected != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "更新报告生成状态失败"
            );
        }
    }

    /**
     * 保存学习数据快照。
     */
    private void saveSnapshot(
            AiLearningReport task,
            LearningReportSnapshot snapshot
    ) {
        try {
            String snapshotContent =
                    objectMapper.writeValueAsString(
                            snapshot
                    );

            String snapshotHash =
                    calculateSha256(
                            snapshotContent
                    );

            AiLearningReportSnapshot entity =
                    new AiLearningReportSnapshot();

            entity.setUserId(
                    task.getUserId()
            );
            entity.setSnapshotVersion(
                    snapshot.snapshotVersion()
            );
            entity.setSnapshotHash(
                    snapshotHash
            );
            entity.setSnapshotContent(
                    snapshotContent
            );
            entity.setCreatedAt(
                    LocalDateTime.now()
            );

            int affected =
                    snapshotMapper.insert(entity);

            if (affected != 1
                    || entity.getId() == null) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存学习数据快照失败"
                );
            }

            task.setSnapshotId(
                    entity.getId()
            );
            task.setSnapshotHash(
                    snapshotHash
            );
            task.setUpdatedAt(
                    LocalDateTime.now()
            );

            reportMapper.updateById(task);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "学习数据快照序列化失败",
                    exception
            );
        }
    }

    /**
     * 保存AI调用成功日志。
     */
    private void saveInvocationSuccess(
            AiLearningReport task,
            AiReportResult result
    ) {
        AiInvocationLog logEntity =
                new AiInvocationLog();

        logEntity.setReportId(task.getId());
        logEntity.setUserId(task.getUserId());
        logEntity.setRequestId(
                result.requestId()
        );
        logEntity.setProvider(
                result.provider()
        );
        logEntity.setModelName(
                result.model()
        );
        logEntity.setPromptVersion(
                properties.ai().promptVersion()
        );
        logEntity.setStatus("SUCCESS");

        long latency =
                Math.min(
                        result.latencyMs(),
                        Integer.MAX_VALUE
                );

        logEntity.setLatencyMs(
                (int) latency
        );
        logEntity.setInputTokens(
                result.inputTokens()
        );
        logEntity.setOutputTokens(
                result.outputTokens()
        );
        logEntity.setRetryCount(0);
        logEntity.setCreatedAt(
                LocalDateTime.now()
        );

        invocationLogMapper.insert(logEntity);
    }

    /**
     * 保存AI调用失败日志。
     */
    private void saveInvocationFailure(
            AiLearningReport task,
            RuntimeException exception
    ) {
        AiInvocationLog logEntity =
                new AiInvocationLog();

        logEntity.setReportId(task.getId());
        logEntity.setUserId(task.getUserId());
        logEntity.setRequestId(
                UUID.randomUUID().toString()
        );
        logEntity.setProvider(
                properties.ai().provider()
        );
        logEntity.setModelName(
                properties.ai().model()
        );
        logEntity.setPromptVersion(
                properties.ai().promptVersion()
        );
        logEntity.setStatus("FAILED");
        logEntity.setRetryCount(
                properties.ai().maxRetries()
        );
        logEntity.setErrorCode(
                resolveErrorCode(exception)
        );
        logEntity.setCreatedAt(
                LocalDateTime.now()
        );

        invocationLogMapper.insert(logEntity);
    }

    /**
     * 保存最终报告。
     */
    private void completeReport(
            AiLearningReport task,
            GeneratedLearningReport generated,
            boolean generatedByAi,
            AiReportResult aiResult
    ) {
        try {
            task.setStage(
                    ReportStage.PERSISTING.name()
            );
            task.setProgress(90);
            task.setUpdatedAt(
                    LocalDateTime.now()
            );

            reportMapper.updateById(task);

            String reportContent =
                    objectMapper.writeValueAsString(
                            generated
                    );

            LocalDateTime now =
                    LocalDateTime.now();

            task.setTitle(
                    generated.title()
            );
            task.setSummary(
                    generated.summary()
            );
            task.setReportContent(
                    reportContent
            );
            task.setGeneratedByAi(
                    generatedByAi ? 1 : 0
            );

            if (aiResult != null) {
                task.setProvider(
                        aiResult.provider()
                );
                task.setModelName(
                        aiResult.model()
                );
                task.setPromptVersion(
                        properties.ai().promptVersion()
                );
                task.setSchemaVersion(
                        properties.ai().schemaVersion()
                );
                task.setInputTokens(
                        aiResult.inputTokens()
                );
                task.setOutputTokens(
                        aiResult.outputTokens()
                );
            }

            task.setStatus(
                    ReportStatus.SUCCESS.name()
            );
            task.setStage(
                    ReportStage.PERSISTING.name()
            );
            task.setProgress(100);
            task.setErrorCode(null);
            task.setErrorMessage(null);
            task.setGeneratedAt(now);
            task.setUpdatedAt(now);

            int affected =
                    reportMapper.updateById(task);

            if (affected != 1) {
                throw new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存最终学习报告失败"
                );
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "学习报告序列化失败",
                    exception
            );
        }
    }

    /**
     * 标记任务失败。
     *
     * 该方法必须自行处理错误，不能再次向外抛出导致状态未保存。
     */
    private void markFailed(
            Long reportId,
            Exception exception
    ) {
        try {
            AiLearningReport task =
                    reportMapper.selectById(reportId);

            if (task == null) {
                log.error(
                        "无法标记报告失败，报告不存在，reportId={}",
                        reportId
                );
                return;
            }

            LocalDateTime now =
                    LocalDateTime.now();

            task.setStatus(
                    ReportStatus.FAILED.name()
            );
            task.setProgress(100);
            task.setErrorCode(
                    resolveErrorCode(exception)
            );
            task.setErrorMessage(
                    safeErrorMessage(exception)
            );
            task.setUpdatedAt(now);

            reportMapper.updateById(task);
        } catch (Exception updateException) {
            log.error(
                    "更新报告失败状态时再次发生异常，reportId={}",
                    reportId,
                    updateException
            );
        }
    }

    /**
     * 生成SHA-256快照哈希。
     */
    private String calculateSha256(
            String content
    ) {
        try {
            byte[] digest =
                    MessageDigest
                            .getInstance("SHA-256")
                            .digest(
                                    content.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            );

            return HexFormat.of()
                    .formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "当前JDK不支持SHA-256",
                    exception
            );
        }
    }

    /**
     * 获取安全错误码。
     */
    private String resolveErrorCode(
            Throwable exception
    ) {
        if (exception instanceof BusinessException businessException
                && businessException.getErrorCode() != null) {
            return String.valueOf(
                    businessException
                            .getErrorCode()
                            .getCode()
            );
        }

        return "INTERNAL_ERROR";
    }

    /**
     * 保存安全的错误信息。
     *
     * 不保存API Key、Prompt和供应商完整响应。
     */
    private String safeErrorMessage(
            Throwable exception
    ) {
        String message =
                exception.getMessage();

        if (message == null || message.isBlank()) {
            return "报告生成失败";
        }

        if (message.length() > 500) {
            return message.substring(0, 500);
        }

        return message;
    }
}