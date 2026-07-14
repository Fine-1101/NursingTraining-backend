package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

/**
 * 学习报告内容生成编排服务。
 */
public interface LearningReportGenerationService {

    /**
     * 优先使用 AI 生成；AI 关闭或失败时使用规则报告降级。
     */
    GeneratedLearningReport generate(LearningReportSnapshot snapshot);
}
