package org.example.nursingtrainingbackend.modules.learningreport.enums;

/**
 * 学习报告数据充足度等级。
 */
public enum DataQualityLevel {

    /**
     * 数据不足，无法生成有效分析。
     */
    INSUFFICIENT,

    /**
     * 数据较少，只能形成初步判断。
     */
    LOW,

    /**
     * 数据基本充足，可以分析部分趋势。
     */
    MEDIUM,

    /**
     * 数据充足，可以生成完整报告。
     */
    HIGH
}