package org.example.nursingtrainingbackend.modules.learningreport.service;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;
import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

/**
 * 规则版学习报告生成服务。
 *
 * 不调用外部AI，只根据后端学习快照生成报告。
 */
public interface RuleBasedReportService {

    /**
     * 根据学习快照生成规则版报告。
     *
     * @param snapshot 学习数据快照
     * @return 已生成的规则版报告
     */
    GeneratedLearningReport generate(
            LearningReportSnapshot snapshot
    );
}
