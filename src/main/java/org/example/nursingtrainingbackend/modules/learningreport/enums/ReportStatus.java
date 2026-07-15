package org.example.nursingtrainingbackend.modules.learningreport.enums;

/**
 * 学习报告生成状态。
 */
public enum ReportStatus {

    /**
     * 已创建任务，等待执行。
     */
    PENDING,

    /**
     * 正在聚合数据或调用AI。
     */
    GENERATING,

    /**
     * 报告生成成功。
     */
    SUCCESS,

    /**
     * 报告生成失败。
     */
    FAILED,

    /**
     * 报告数据已过期。
     */
    EXPIRED
}