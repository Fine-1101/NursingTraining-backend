package org.example.nursingtrainingbackend.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * AI 个性化学习报告配置。

 * 对应 application.yml 中的：
 * app.learning-report
 */
@Validated
@ConfigurationProperties(prefix = "app.learning-report")
public record LearningReportProperties(

        /**
         * 学习报告模块是否启用。
         */
        boolean enabled,

        /**
         * 报告生成条件配置。
         */
        @Valid Eligibility eligibility,

        /**
         * 报告生成规则配置。
         */
        @Valid Generation generation,

        /**
         * AI 接口配置。
         */
        @Valid Ai ai,

        /**
         * AI 调用失败后的降级配置。
         */
        @Valid Fallback fallback,

        /**
         * 异步任务恢复配置。
         */
        @Valid Recovery recovery
) {

    /**
     * 报告生成条件。
     */
    public record Eligibility(

            /**
             * 注册多少天以内算新用户。
             */
            @Min(1)
            int onboardingDays,

            /**
             * 生成完整报告所需的最低数据分数。
             */
            @Min(0)
            @Max(100)
            int fullReportMinDataScore,

            /**
             * 生成报告所需的最少有效学习事件数量。
             */
            @Min(0)
            int minimumLearningEvents,

            /**
             * 分析知识掌握度所需的最少答题数。
             */
            @Min(1)
            int minimumMasteryQuestions
    ) {
    }

    /**
     * 报告生成规则。
     */
    public record Generation(

            /**
             * 每个用户每天最多生成报告的次数。
             */
            @Min(1)
            int dailyLimit,

            /**
             * 同一报告每天最多重新生成的次数。
             */
            @Min(0)
            int regenerateDailyLimit,

            /**
             * 报告最多展示多少个优势知识点。
             */
            @Min(1)
            @Max(10)
            int maxStrengths,

            /**
             * 报告最多展示多少个薄弱知识点。
             */
            @Min(1)
            @Max(10)
            int maxWeaknesses,

            /**
             * 学习计划最多包含多少项。
             */
            @Min(1)
            @Max(30)
            int maxPlanItems,

            /**
             * 学习数据没有变化时是否复用已有报告。
             */
            boolean reuseUnchangedSnapshot
    ) {
    }

    /**
     * 外部 AI 接口配置。
     */
    public record Ai(

            /**
             * 是否启用 AI 调用。
             */
            boolean enabled,

            /**
             * AI 供应商标识。
             */
            @NotBlank
            String provider,

            /**
             * AI 接口基础地址。
             */
            @NotBlank
            String baseUrl,

            /**
             * AI 接口密钥。
             */
            @NotBlank
            String apiKey,

            /**
             * 模型名称。
             */
            @NotBlank
            String model,

            /**
             * 对话补全接口路径。
             */
            @NotBlank
            String chatPath,

            /**
             * 建立 HTTP 连接的超时时间。
             */
            Duration connectTimeout,

            /**
             * 等待 AI 返回结果的超时时间。
             */
            Duration readTimeout,

            /**
             * AI 请求失败时的最大重试次数。
             */
            @Min(0)
            @Max(5)
            int maxRetries,

            /**
             * 模型生成随机度。
             */
            BigDecimal temperature,

            /**
             * AI 最大输出 Token 数。
             */
            @Min(100)
            int maxOutputTokens,

            /**
             * Prompt 版本。
             */
            @NotBlank
            String promptVersion,

            /**
             * AI 输出结构版本。
             */
            @NotBlank
            String schemaVersion
    ) {
    }

    /**
     * AI 调用失败后的降级策略。
     */
    public record Fallback(

            /**
             * 是否启用规则版报告。
             */
            boolean enabled
    ) {
    }

    /**
     * 异步报告任务恢复配置。
     */
    public record Recovery(

            /**
             * 是否启用卡住任务的恢复扫描。
             */
            boolean enabled,

            /**
             * 恢复任务扫描间隔。
             */
            Duration scanInterval,

            /**
             * PENDING 状态允许持续的最长时间。
             */
            Duration pendingTimeout,

            /**
             * GENERATING 状态允许持续的最长时间。
             */
            Duration generatingTimeout
    ) {
    }
}
