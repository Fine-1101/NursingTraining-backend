package org.example.nursingtrainingbackend.modules.learningreport.enums;

/**
 * 学习报告分析模式。
 */
public enum ReportMode {

    /**
     * 没有足够学习数据，不调用AI，只提供学习引导。
     */
    GUIDANCE_ONLY,

    /**
     * 新用户或数据较少时生成入门报告。
     */
    ONBOARDING,

    /**
     * 数据充足时生成完整个性化报告。
     */
    FULL
}