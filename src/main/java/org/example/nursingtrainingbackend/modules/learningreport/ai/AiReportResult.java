
package org.example.nursingtrainingbackend.modules.learningreport.ai;

import org.example.nursingtrainingbackend.modules.learningreport.dto.GeneratedLearningReport;

/**
 * AI学习报告调用结果。
 *
 * 除了报告正文，还包含供应商、模型、Token和耗时，
 * 用于日志、成本统计和问题排查。
 */
public record AiReportResult(

        /**
         * AI生成的结构化报告。
         */
        GeneratedLearningReport report,

        /**
         * AI供应商标识。
         *
         * 例如 openai-compatible、deepseek。
         */
        String provider,

        /**
         * 实际使用的模型名称。
         */
        String model,

        /**
         * 输入Token数量。
         */
        Integer inputTokens,

        /**
         * 输出Token数量。
         */
        Integer outputTokens,

        /**
         * AI接口调用耗时，单位为毫秒。
         */
        long latencyMs,

        /**
         * 本次AI请求唯一标识。
         */
        String requestId
) {

    /**
     * 构造参数基本校验。
     */
    public AiReportResult {
        if (report == null) {
            throw new IllegalArgumentException(
                    "AI生成报告不能为空"
            );
        }

        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException(
                    "AI供应商不能为空"
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException(
                    "AI模型名称不能为空"
            );
        }

        if (latencyMs < 0) {
            throw new IllegalArgumentException(
                    "AI调用耗时不能小于0"
            );
        }

        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException(
                    "AI请求ID不能为空"
            );
        }

        /*
         * 有些供应商可能不返回Token数据，
         * 所以inputTokens和outputTokens允许为null。
         */
        if (inputTokens != null && inputTokens < 0) {
            throw new IllegalArgumentException(
                    "输入Token数量不能小于0"
            );
        }

        if (outputTokens != null && outputTokens < 0) {
            throw new IllegalArgumentException(
                    "输出Token数量不能小于0"
            );
        }
    }

    /**
     * 获取总Token数量。
     *
     * 供应商未返回Token时按0处理。
     */
    public int totalTokens() {
        int input = inputTokens == null
                ? 0
                : inputTokens;

        int output = outputTokens == null
                ? 0
                : outputTokens;

        return input + output;
    }
}