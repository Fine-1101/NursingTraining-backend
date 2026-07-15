package org.example.nursingtrainingbackend.modules.learningreport.ai;

import org.example.nursingtrainingbackend.modules.learningreport.dto.LearningReportSnapshot;

/**
 * AI学习报告客户端统一接口。
 *
 * 业务层只依赖该接口，不直接依赖具体AI供应商。
 */
public interface AiReportClient {

    /**
     * 根据学习数据快照生成个性化学习报告。
     *
     * @param snapshot 学习数据快照
     * @param options  本次报告生成参数
     * @return AI报告生成结果
     */
    AiReportResult generate(
            LearningReportSnapshot snapshot,
            ReportGenerationOptions options
    );
}
