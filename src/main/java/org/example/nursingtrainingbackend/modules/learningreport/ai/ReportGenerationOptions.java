package org.example.nursingtrainingbackend.modules.learningreport.ai;

import org.example.nursingtrainingbackend.config.properties.LearningReportProperties;

import java.math.BigDecimal;

/**
 * 单次AI报告生成参数。
 *
 * 这些参数表示“本次调用”应该如何生成报告。
 */

public record ReportGenerationOptions(

        /**
         * Prompt版本。
         */
        String promptVersion,

        /**
         * AI输出JSON结构版本。
         */
        String schemaVersion,

        /**
         * 最大输出Token数量。
         */
        int maxOutputTokens,

        /**
         * 模型生成随机度。
         *
         * 学习报告建议使用较低值，例如0.3。
         */
        BigDecimal temperature
) {

    /**
     * 构造参数校验。
     */
    public ReportGenerationOptions {
        if (promptVersion == null
                || promptVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "promptVersion不能为空"
            );
        }

        if (schemaVersion == null
                || schemaVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "schemaVersion不能为空"
            );
        }

        if (maxOutputTokens < 100) {
            throw new IllegalArgumentException(
                    "maxOutputTokens不能小于100"
            );
        }

        if (temperature == null) {
            throw new IllegalArgumentException(
                    "temperature不能为空"
            );
        }

        if (temperature.compareTo(BigDecimal.ZERO) < 0
                || temperature.compareTo(
                new BigDecimal("2")
        ) > 0) {
            throw new IllegalArgumentException(
                    "temperature必须在0到2之间"
            );
        }
    }
    /**
     * 从系统配置创建本次AI调用参数。
     */
    public static ReportGenerationOptions from(
            LearningReportProperties properties
    ) {
        if (properties == null || properties.ai() == null) {
            throw new IllegalArgumentException(
                    "AI配置不能为空"
            );
        }

        LearningReportProperties.Ai ai =
                properties.ai();

        return new ReportGenerationOptions(
                ai.promptVersion(),
                ai.schemaVersion(),
                ai.maxOutputTokens(),
                ai.temperature()
        );
    }
}
