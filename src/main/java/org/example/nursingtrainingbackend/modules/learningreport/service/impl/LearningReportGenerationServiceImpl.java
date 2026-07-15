package org.example.nursingtrainingbackend.modules.learningreport.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportClient;
import org.example.nursingtrainingbackend.modules.learningreport.ai.AiReportResult;
import org.example.nursingtrainingbackend.modules.learningreport.ai.ReportGenerationOptions;
import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;
import org.example.nursingtrainingbackend.modules.learningreport.service.LearningReportGenerationService;
import org.example.nursingtrainingbackend.modules.learningreport.service.RuleBasedReportService;
import org.springframework.stereotype.Service;

/**
 * 学习报告内容生成编排实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningReportGenerationServiceImpl
        implements LearningReportGenerationService {

    private final LearningReportProperties properties;
    private final AiReportClient aiReportClient;
    private final RuleBasedReportService ruleBasedReportService;

    @Override
    public GeneratedLearningReport generate(
            LearningReportSnapshot snapshot
    ) {
        if (snapshot == null) {
            throw new IllegalArgumentException("学习报告快照不能为空");
        }

        if (!properties.ai().enabled()) {
            return ruleBasedReportService.generate(snapshot);
        }

        try {
            ReportGenerationOptions options =
                    ReportGenerationOptions.from(properties);

            AiReportResult result = aiReportClient.generate(snapshot, options);
            return result.report();
        } catch (RuntimeException exception) {
            if (!properties.fallback().enabled()) {
                throw exception;
            }

            log.warn(
                    "AI学习报告生成失败，已降级为规则报告：{}",
                    exception.getMessage()
            );

            return ruleBasedReportService.generate(snapshot);
        }
    }
}
