package org.example.nursingtrainingbackend.modules.learningreport.validator;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

/**
 * AI 学习报告响应校验器。
 */
public interface AiReportResponseValidator {

    void validate(
            GeneratedLearningReport report,
            LearningReportSnapshot snapshot
    );
}
