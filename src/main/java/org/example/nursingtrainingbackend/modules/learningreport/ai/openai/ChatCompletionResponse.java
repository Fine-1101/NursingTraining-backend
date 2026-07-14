package org.example.nursingtrainingbackend.modules.learningreport.ai.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenAI兼容对话补全响应。
 *
 * 忽略供应商返回但当前业务不需要的字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(

        /**
         * 供应商生成的请求或响应ID。
         */
        String id,

        /**
         * 模型输出候选项。
         */
        List<Choice> choices,

        /**
         * Token使用量。
         */
        Usage usage
) {

    /**
     * 获取第一条AI回复内容。
     *
     * @return 第一条回复内容；响应不完整时返回null
     */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return null;
        }

        Choice firstChoice = choices.getFirst();

        if (firstChoice == null
                || firstChoice.message() == null) {
            return null;
        }

        return firstChoice.message().content();
    }

    /**
     * 模型输出候选项。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(

            /**
             * 候选项序号。
             */
            Integer index,

            /**
             * AI回复消息。
             */
            Message message,

            /**
             * 模型停止生成的原因。
             */
            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    /**
     * AI回复消息。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String role,
            String content
    ) {
    }

    /**
     * Token使用情况。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(

            @JsonProperty("prompt_tokens")
            Integer promptTokens,

            @JsonProperty("completion_tokens")
            Integer completionTokens,

            @JsonProperty("total_tokens")
            Integer totalTokens
    ) {

        /**
         * 供应商不返回Token时按0处理。
         */
        public int safePromptTokens() {
            return promptTokens == null
                    ? 0
                    : promptTokens;
        }

        public int safeCompletionTokens() {
            return completionTokens == null
                    ? 0
                    : completionTokens;
        }

        public int safeTotalTokens() {
            if (totalTokens != null) {
                return totalTokens;
            }

            return safePromptTokens()
                    + safeCompletionTokens();
        }
    }
}