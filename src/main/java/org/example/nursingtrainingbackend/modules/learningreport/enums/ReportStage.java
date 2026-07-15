package org.example.nursingtrainingbackend.modules.learningreport.enums;

/**
 * 学习报告生成阶段。
 */
public enum ReportStage {

    /**
     * 等待异步线程处理。
     */
    QUEUED,

    /**
     * 正在查询并聚合用户学习数据。
     */
    DATA_AGGREGATION,

    /**
     * 正在计算掌握度、薄弱项和推荐内容。
     */
    RULE_ANALYSIS,

    /**
     * 正在请求AI接口。
     */
    AI_GENERATION,

    /**
     * 正在校验AI返回结果。
     */
    VALIDATION,

    /**
     * 正在将最终报告保存到数据库。
     */
    PERSISTING
}